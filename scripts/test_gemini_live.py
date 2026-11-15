"""Bounded, opt-in Gemini checks against synthetic meals; never writes app data.

Run from the repository root: python scripts/test_gemini_live.py --output report.json
The key is read from GEMINI_API_KEY or a hidden prompt, never saved in the report.
--prepare-wire/--replay-wire support an external HTTPS transport when local DNS is
unavailable. Replay decodes actual saved Google responses through the same SDK.
"""
import argparse
import getpass
import json
import os
import sys
import time
from contextlib import nullcontext
from copy import deepcopy
from datetime import date, timedelta
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "src"))

from bep_nha_api import app as gateway


def context():
    today = date(2026, 11, 16)
    return {
        "today": today.isoformat(), "date": today.isoformat(), "meal": "lunch",
        "timezone": "Asia/Ho_Chi_Minh", "screen": "TODAY", "people": 4,
        "lunch": "11:30", "dinner": "18:30", "favorites": [], "timer": None,
        "meals": [{"date": (today + timedelta(days=day)).isoformat(), "meal": meal,
                   "recipes": ["pork", "soup", "greens"]}
                  for day in range(7) for meal in ("lunch", "dinner")],
    }


def cases():
    swap = {"type": "swap_recipe", "date": "2026-11-18", "meal": "dinner", "index": 1, "recipe_id": "sour"}
    inputs = [
        ("pantry", "Tủ lạnh có thịt heo, trứng và rau muống. Tối mai nấu gì?", "suggestions"),
        ("swap", "Tối ngày mốt đổi món canh thành canh chua cá, giữ mặn và rau", "swap"),
        ("no_diacritics", "toi ngay mot doi mon canh thanh canh chua ca, giu man va rau", "swap"),
        ("negated", "Tối ngày mốt đừng đổi món mặn và rau, chỉ đổi canh thành canh chua cá", "swap"),
        ("pending", "À đổi sang canh cải thịt bằm", "follow_up"),
        ("last", "À đổi sang canh cải thịt bằm", "follow_up"),
        ("pantry_negation", "Tủ lạnh không còn thịt heo, trứng. Chỉ còn rau muống. Tối mai nấu gì?", "only_greens"),
        ("pantry_exclusion", "Tủ lạnh có đậu hũ, cà chua, rau muống. Tối mai gợi ý mâm không dùng thịt heo và trứng nhé", "exclude"),
        ("pantry_only", "Tủ lạnh chỉ có trứng. Tối mai nấu gì, chỉ dùng nguyên liệu đang có, không mua thêm?", "text"),
        ("family", "Đổi khẩu phần toàn gia đình thành 6 người", "people"),
        ("navigate", "Mở danh sách đi chợ bữa này", "navigate"),
        ("unknown_recipe", "Tối mai đổi món mặn thành phở bò", "text"),
        ("clarify", "Ngày mai mình muốn ăn canh chua cá", "clarify"),
        ("long_clarification", "Bữa tối nhé, giữ nguyên món mặn và món rau giúp mình", "tomorrow_soup"),
        ("outside_week", "Tối 28/11 đổi món canh thành canh chua cá", "text"),
        ("breakfast", "Sáng mai đổi món mặn thành đậu hũ sốt cà chua", "text"),
    ]
    result = []
    for name, message, expected in inputs:
        data = {"message": message, "context": context(), "history": []}
        if name in ("pending", "last"):
            data["context"][f"{name}_menu_changes"] = [swap]
        if name == "long_clarification":
            data["history"] = [{"role": "user", "text": "Ngày mai mình muốn ăn canh chua cá"},
                               {"role": "assistant", "text": gateway.MEAL_QUESTION}]
        result.append({"id": name, "turn": data, "expected": expected})
    return result


def check(case, answer):
    expected = case["expected"]
    actions = [a.model_dump(exclude_none=True) for a in answer.actions]
    if expected in ("text", "clarify"):
        assert answer.reply and not actions and not answer.suggestions, "Expected text without executable proposals"
        if expected == "clarify":
            assert answer.reply == gateway.MEAL_QUESTION, "Must ask for the meal"
    elif expected in ("swap", "follow_up", "tomorrow_soup"):
        day = "2026-11-17" if expected == "tomorrow_soup" else "2026-11-18"
        recipe = "soup" if expected == "follow_up" else "sour"
        assert actions == [{"type": "swap_recipe", "date": day, "meal": "dinner", "index": 1, "recipe_id": recipe}], actions
    elif expected == "people":
        assert len(actions) == 1 and actions[0]["type"] == "set_people" and actions[0]["people"] == 6, actions
    elif expected == "navigate":
        assert len(actions) == 1 and actions[0]["type"] == "navigate" and actions[0]["screen"] == "SHOPPING", actions
    else:
        assert answer.suggestions and not actions, "Expected meal suggestions without changes"
        for suggestion in answer.suggestions:
            assert suggestion.date == "2026-11-17" and suggestion.meal == "dinner"
            if expected == "only_greens":
                assert suggestion.pantry_keys == ["Rau muống|g"], suggestion.pantry_keys
            if expected == "exclude":
                ingredients = {i["name"] for r in suggestion.recipe_ids for i in gateway.RECIPES[r]["ingredients"]}
                assert not ingredients & {"Thịt ba chỉ", "Thịt heo bằm", "Trứng gà"}, ingredients


def run_case(case):
    started = time.monotonic()
    result = {"id": case["id"], "message": case["turn"]["message"]}
    try:
        answer = gateway.assistant_turn(gateway.Turn.model_validate(deepcopy(case["turn"])))
        result["answer"] = answer.model_dump(exclude_none=True)
        check(case, answer)
        result["passed"] = True
    except gateway.HTTPException as exc:
        result.update(passed=False, status=exc.status_code, error=exc.detail)
    except AssertionError as exc:
        result.update(passed=False, error=str(exc))
    result["seconds"] = round(time.monotonic() - started, 2)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=Path(".venv/gemini-live-report.json"))
    parser.add_argument("--case", action="append", help="Run only these case IDs")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--prepare-wire", type=Path)
    mode.add_argument("--replay-wire", type=Path)
    args = parser.parse_args()
    selected = [case for case in cases() if not args.case or case["id"] in args.case]
    if not selected:
        parser.error("No matching cases")
    wire = args.prepare_wire or args.replay_wire
    if wire:
        os.environ["GEMINI_API_KEY"] = "wire-transport-placeholder"
        wire.mkdir(parents=True, exist_ok=True)
    elif not os.environ.get("GEMINI_API_KEY"):
        os.environ["GEMINI_API_KEY"] = getpass.getpass("Gemini API key (hidden, not saved): ").strip()
    import httpx
    from google import genai
    from google.genai import types
    real_client = genai.Client
    results = []
    for case in selected:
        observed = {}

        def transport(request, case=case, observed=observed):
            observed["called"] = True
            if args.prepare_wire:
                payload = {"url": str(request.url), "body": json.loads(request.content)}
                (wire / f"{case['id']}.request.json").write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")
                return httpx.Response(200, json={"candidates": [{"content": {"parts": [{"text": "Prepared only"}], "role": "model"}}]})
            original = json.loads((wire / f"{case['id']}.request.json").read_text(encoding="utf-8"))
            observed["request_matches"] = original == {"url": str(request.url), "body": json.loads(request.content)}
            if not observed["request_matches"]:
                raise ValueError("Saved provider response belongs to a different request")
            response = json.loads((wire / f"{case['id']}.response.json").read_text(encoding="utf-8"))
            observed.update({k: response[k] for k in ("status", "seconds")})
            observed["usage"] = response["body"].get("usageMetadata", {})
            return httpx.Response(response["status"], json=response["body"])

        def client(**kwargs):
            options = kwargs["http_options"].model_dump(exclude_none=True)
            options.update(client_args={"transport": httpx.MockTransport(transport)}, retry_options={"attempts": 1})
            kwargs["http_options"] = types.HttpOptions(**options)
            return real_client(**kwargs)

        with patch.object(genai, "Client", client) if wire else nullcontext():
            result = run_case(case)
        result["provider"] = observed if wire else {"transport": "direct_sdk"}
        results.append(result)
        print(json.dumps({"id": case["id"], "prepared": bool(args.prepare_wire), "passed": result["passed"],
                          "seconds": result["seconds"], "error": result.get("error")}, ensure_ascii=False), flush=True)
        if not wire and result.get("status") == 429:
            break  # Leave the remaining cases for a later run when quota is available.
    report = {"model": os.environ.get("GEMINI_MODEL", "gemini-3.6-flash"),
              "mode": "prepare" if args.prepare_wire else "real_response_replay" if wire else "direct_live",
              "passed": sum(r["passed"] for r in results), "total": len(results),
              "planned_total": len(selected), "results": results}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    return 0 if args.prepare_wire or report["passed"] == report["total"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
