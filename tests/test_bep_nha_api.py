import json
from pathlib import Path
from types import SimpleNamespace

import pytest

pytest.importorskip("fastapi")
from fastapi.testclient import TestClient

from bep_nha_api import app as gateway

client = TestClient(gateway.app)


def action(**kwargs):
    return gateway.Action(type="swap_recipe", date="2026-11-03", meal="dinner", index=0, recipe_id="tofu", **kwargs)


def test_catalog_matches_android():
    source = Path(__file__).parents[1] / "android/app/src/main/assets/recipes.json"
    assert json.loads(source.read_text(encoding="utf-8")) == gateway.CATALOG
    assert len(gateway.RECIPES) == 14


@pytest.mark.parametrize("change", [
    {"recipe_id": "invented"}, {"index": 2}, {"date": "2026-11-10"}, {"people": 4},
])
def test_invalid_actions_rejected(change):
    values = action().model_dump()
    values.update(change)
    with pytest.raises(ValueError):
        gateway.validate_actions([gateway.Action(**values)], {"today": "2026-11-03"})


def test_gateway_returns_proposal_without_claiming_execution(monkeypatch):
    monkeypatch.setattr(gateway, "generate", lambda _: gateway.Answer(reply="Đã đổi món", actions=[action()]))
    response = client.post("/api/assistant/turn", json={"message": "Đổi thịt kho thành đậu hũ", "context": {"today": "2026-11-03"}})
    assert response.status_code == 200
    assert "Đã" not in response.json()["reply"]
    assert response.json()["actions"][0]["recipe_id"] == "tofu"


def test_provider_errors_do_not_expose_secrets(monkeypatch):
    def fail(_):
        raise RuntimeError("secret-key-user-payload")
    monkeypatch.setattr(gateway, "generate", fail)
    response = client.post("/api/assistant/turn", json={"message": "Món nhanh", "context": {"today": "2026-11-03"}})
    assert response.status_code == 503
    assert "secret" not in response.text


def test_blank_question_and_unknown_input_rejected():
    assert client.post("/api/assistant/turn", json={"message": " ", "context": {}}).status_code == 422
    with pytest.raises(ValueError):
        gateway.Action(type="run_shell", date="2026-11-03", meal="dinner")


def test_missing_key_returns_actionable_error(monkeypatch):
    monkeypatch.delenv("GEMINI_API_KEY", raising=False)
    response = client.post("/api/assistant/turn", json={"message": "Món nhanh", "context": {"today": "2026-11-03"}})
    assert response.status_code == 503
    assert "offline" in response.json()["detail"]


@pytest.mark.parametrize("values", [{"people": 13}, {"people": True}, {"people": "4"}])
def test_people_requires_bounded_integer(values):
    with pytest.raises(ValueError):
        gateway.Action(type="set_people", date="2026-11-03", meal="dinner", **values)


def mock_gemini_call(monkeypatch, name, args):
    from google import genai
    captured = {}

    class Client:
        def __init__(self, **kwargs):
            pass
        def __enter__(self):
            return self
        def __exit__(self, *_):
            pass
        @property
        def models(self):
            return self
        def generate_content(self, **kwargs):
            captured.update(kwargs)
            return SimpleNamespace(function_calls=[SimpleNamespace(name=name, args=args)])

    monkeypatch.setattr(genai, "Client", Client)
    monkeypatch.setenv("GEMINI_API_KEY", "test-only")
    return captured


def test_sdk_uses_manual_function_calling_and_validates_tool_result(monkeypatch):
    captured = mock_gemini_call(monkeypatch, "propose_actions", {
        "reply": "Đề xuất",
        "actions": [action().model_dump(exclude_none=True)],
    })
    result = gateway.generate(gateway.Turn(message="Đổi món", context={"today": "2026-11-03"}))
    assert result.actions[0].recipe_id == "tofu"
    assert captured["config"].automatic_function_calling.disable is True
    assert captured["config"].tool_config.function_calling_config.mode == "AUTO"
    tools = {item.name: item.parameters_json_schema for item in captured["config"].tools[0].function_declarations}
    assert set(tools) == {"propose_actions", "suggest_meals"}
    assert "suggestions" not in tools["propose_actions"]["properties"]
    assert "actions" not in tools["suggest_meals"]["properties"]


def planning_context(**fields):
    return {
        "today": "2026-11-03", "date": "2026-11-03", "meal": "lunch",
        "meals": [{"date": day, "meal": meal, "recipes": ["pork", "soup", "greens"]}
                  for day in ("2026-11-03", "2026-11-04", "2026-11-05") for meal in ("lunch", "dinner")],
        **fields,
    }


def soup_swap(**fields):
    return gateway.Action(**{
        "type": "swap_recipe", "date": "2026-11-05", "meal": "dinner", "index": 1, "recipe_id": "sour",
        **fields,
    })


def test_suggest_meals_ranks_catalog_matches_and_returns_no_actions(monkeypatch):
    captured = mock_gemini_call(monkeypatch, "suggest_meals", {
        "date": "2026-11-05", "meal": "dinner", "pantry_items": ["thịt lợn", "trứng", "rau muống"],
    })
    answer = gateway.assistant_turn(gateway.Turn(
        message="Tủ lạnh có thịt lợn, trứng và rau muống. Tối ngày mai nữa nấu gì?",
        context=planning_context(),
    ))
    assert not answer.actions
    assert [suggestion.id for suggestion in answer.suggestions] == [1, 2]
    for suggestion in answer.suggestions:
        assert (suggestion.date, suggestion.meal) == ("2026-11-05", "dinner")
        assert [gateway.RECIPES[recipe_id]["kind"] for recipe_id in suggestion.recipe_ids] == list(gateway.KINDS)
        assert suggestion.recipe_ids[0] == "pork"
        assert suggestion.recipe_ids[2] == "greens"
        assert {"Thịt ba chỉ|g", "Trứng gà|quả", "Rau muống|g"} <= set(suggestion.pantry_keys)
    assert json.loads(captured["contents"])["menu_scope"]["date"] == "2026-11-05"


def test_suggest_meals_rejects_wrong_day_for_relative_date(monkeypatch):
    mock_gemini_call(monkeypatch, "suggest_meals", {
        "date": "2026-11-04", "meal": "dinner", "pantry_items": ["trứng"],
    })
    with pytest.raises(gateway.HTTPException) as exc:
        gateway.assistant_turn(gateway.Turn(message="Tối ngày mai nữa nấu gì với trứng?", context=planning_context()))
    assert exc.value.status_code == 502


def test_suggest_meals_grounded_in_latest_available_ingredients(monkeypatch):
    mock_gemini_call(monkeypatch, "suggest_meals", {
        "date": "2026-11-04", "meal": "dinner", "pantry_items": ["thịt heo", "trứng", "tôm"],
    })
    answer = gateway.assistant_turn(gateway.Turn(
        message="Hết thịt heo. Tủ lạnh còn trứng. Tối mai nấu gì?",
        history=[gateway.Message(role="user", text="Tủ lạnh có thịt heo.")],
        context=planning_context(),
    ))
    assert answer.suggestions
    assert all(item.pantry_keys == ["Trứng gà|quả"] for item in answer.suggestions)


def test_pantries_with_missing_ingredients_report_gap_when_required(monkeypatch):
    mock_gemini_call(monkeypatch, "suggest_meals", {
        "date": "2026-11-04", "meal": "dinner", "pantry_items": ["trứng"], "pantry_only": True,
    })
    answer = gateway.assistant_turn(gateway.Turn(
        message="Tối mai nấu gì mà chỉ dùng trứng?",
        context=planning_context(),
    ))
    assert not answer.suggestions and not answer.actions
    assert "cần thêm" in answer.reply


@pytest.mark.parametrize("message", [
    "Tối ngày mốt đổi món canh thành canh chua cá, giữ mặn và rau",
    "toi ngay mai nua doi mon canh thanh canh chua ca",
    "Tối ngày mốt mình muốn ăn canh chua cá",
])
def test_swap_keeps_named_slots_and_targets_future_meal(monkeypatch, message):
    monkeypatch.setattr(gateway, "generate", lambda _: gateway.Answer(reply="Đề xuất", actions=[soup_swap()]))
    turn = gateway.Turn(
        message=message,
        context=planning_context(),
    )
    assert gateway.assistant_turn(turn).actions == [soup_swap()]


def test_request_to_eat_one_named_dish_cannot_also_change_another_slot(monkeypatch):
    changes = [soup_swap(), soup_swap(index=0, recipe_id="tofu")]
    monkeypatch.setattr(gateway, "generate", lambda _: gateway.Answer(reply="Đề xuất", actions=changes))
    turn = gateway.Turn(message="Tối ngày mốt mình muốn ăn canh chua cá", context=planning_context())
    with pytest.raises(gateway.HTTPException) as exc:
        gateway.assistant_turn(turn)
    assert exc.value.status_code == 502


@pytest.mark.parametrize("changes", [
    [soup_swap(date="2026-11-04")],
    [soup_swap(meal="lunch")],
    [soup_swap(recipe_id="cabbage")],
    [soup_swap(index=0, recipe_id="tofu")],
    [soup_swap(), soup_swap()],
])
def test_wrong_meal_recipe_or_repeated_slot_rejects_entire_batch(monkeypatch, changes):
    monkeypatch.setattr(gateway, "generate", lambda _: gateway.Answer(reply="Đề xuất", actions=changes))
    turn = gateway.Turn(
        message="Tối ngày mốt đổi món canh thành canh chua cá, giữ mặn và rau",
        context=planning_context(),
    )
    with pytest.raises(gateway.HTTPException) as exc:
        gateway.assistant_turn(turn)
    assert exc.value.status_code == 502


@pytest.mark.parametrize("focus_field", ["pending_menu_changes", "last_menu_changes"])
def test_follow_up_targets_meal_from_prior_swap(monkeypatch, focus_field):
    next_swap = soup_swap(recipe_id="soup")
    monkeypatch.setattr(gateway, "generate", lambda _: gateway.Answer(reply="Đề xuất", actions=[next_swap]))
    turn = gateway.Turn(
        message="À đổi sang canh cải thịt bằm",
        context=planning_context(**{focus_field: [soup_swap().model_dump(exclude_none=True)]}),
    )
    assert gateway.assistant_turn(turn).actions == [next_swap]


def test_clarifying_meal_and_dish_keeps_the_explicit_relative_date(monkeypatch):
    context = planning_context()
    history = []
    for message, clarification in [
        ("Mai đổi món đó", gateway.MEAL_QUESTION),
        ("Bữa tối nhé", gateway.DISH_QUESTION),
    ]:
        answer = gateway.assistant_turn(gateway.Turn(message=message, context=context, history=history))
        assert answer.reply == clarification
        assert not answer.actions and not answer.suggestions
        history.extend([gateway.Message(role="user", text=message), gateway.Message(role="assistant", text=answer.reply)])
    next_swap = soup_swap(date="2026-11-04")
    monkeypatch.setattr(gateway, "generate", lambda _: gateway.Answer(reply="Đề xuất", actions=[next_swap]))
    turn = gateway.Turn(message="Món canh", history=history, context=context)
    assert gateway.assistant_turn(turn).actions == [next_swap]


def test_outside_week_is_explained_without_a_tool_call(monkeypatch):
    monkeypatch.setattr(gateway, "generate", lambda _: pytest.fail("date outside the week reached Gemini"))
    answer = gateway.assistant_turn(gateway.Turn(
        message="Tối 28/11 đổi món canh thành canh chua cá",
        context=planning_context(),
    ))
    assert not answer.actions
    assert "ngoài thực đơn bảy ngày" in answer.reply


def test_answering_meal_question_preserves_named_dish(monkeypatch):
    question = "Ngày mai mình muốn ăn canh chua cá"
    original = gateway.assistant_turn(gateway.Turn(message=question, context=planning_context()))
    assert original.reply == gateway.MEAL_QUESTION
    wrong_dish = soup_swap(date="2026-11-04", recipe_id="cabbage")
    monkeypatch.setattr(gateway, "generate", lambda _: gateway.Answer(reply="Đề xuất", actions=[wrong_dish]))
    turn = gateway.Turn(
        message="Bữa tối", context=planning_context(),
        history=[gateway.Message(role="user", text=question), gateway.Message(role="assistant", text=original.reply)],
    )
    with pytest.raises(gateway.HTTPException) as exc:
        gateway.assistant_turn(turn)
    assert exc.value.status_code == 502
