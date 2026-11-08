import json
from pathlib import Path

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


def test_sdk_uses_manual_function_calling_and_validates_tool_result(monkeypatch):
    from google import genai
    from types import SimpleNamespace
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
            return SimpleNamespace(function_calls=[SimpleNamespace(name="propose_actions", args={"reply": "Đề xuất", "actions": [action().model_dump(exclude_none=True)]})])

    monkeypatch.setattr(genai, "Client", Client)
    monkeypatch.setenv("GEMINI_API_KEY", "test-only")
    result = gateway.generate(gateway.Turn(message="Đổi món", context={"today": "2026-11-03"}))
    assert result.actions[0].recipe_id == "tofu"
    assert captured["config"].automatic_function_calling.disable is True
    assert captured["config"].tools[0].function_declarations[0].parameters_json_schema
