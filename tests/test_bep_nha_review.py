"""Regressions found during live Gemini and meal-flow review."""
from datetime import date

import pytest

from bep_nha_api import app as gateway
from bep_nha_api.meal_planning import menu_scope, pantry_names


def context(**fields):
    return {"today": "2026-11-16", "date": "2026-11-16", "meal": "lunch", **fields}


def swap(**fields):
    return gateway.Action(**{"type": "swap_recipe", "date": "2026-11-17", "meal": "dinner",
                             "index": 1, "recipe_id": "sour", **fields})


def test_long_clarification_keeps_tomorrow_and_original_dish(monkeypatch):
    turn = gateway.Turn(
        message="Bữa tối nhé, giữ nguyên món mặn và món rau giúp mình",
        context=context(),
        history=[gateway.Message(role="user", text="Ngày mai mình muốn ăn canh chua cá"),
                 gateway.Message(role="assistant", text=gateway.MEAL_QUESTION)],
    )
    monkeypatch.setattr(gateway, "generate", lambda _: gateway.Answer(reply="Đề xuất", actions=[swap()]))
    assert gateway.assistant_turn(turn).actions == [swap()]


@pytest.mark.parametrize("question,day,meal", [
    ("toi thu ba doi mon canh", date(2026, 11, 17), "dinner"),
    ("trua chu nhat doi mon rau", date(2026, 11, 22), "lunch"),
])
def test_weekdays_without_diacritics(question, day, meal):
    scope = menu_scope(question, context())
    assert (scope.day, scope.meal) == (day, meal)


@pytest.mark.parametrize("question,day,meal", [
    ("Đổi một món canh cho bữa này", date(2026, 11, 16), "lunch"),
    ("Tối mốt đổi món canh", date(2026, 11, 18), "dinner"),
    ("Đổi sang ngày mai bữa tối", date(2026, 11, 17), "dinner"),
    ("doi sang ngay mai bua toi", date(2026, 11, 17), "dinner"),
])
def test_accent_normalization_does_not_turn_numbers_or_verbs_into_dates(question, day, meal):
    scope = menu_scope(question, context())
    assert (scope.day, scope.meal, scope.clarification) == (day, meal, None)


def test_negated_replace_request_cannot_change_the_soup(monkeypatch):
    monkeypatch.setattr(gateway, "generate", lambda _: gateway.Answer(reply="Đề xuất", actions=[swap()]))
    with pytest.raises(gateway.HTTPException) as exc:
        gateway.assistant_turn(gateway.Turn(message="Tối mai đừng thay canh thành canh chua cá", context=context()))
    assert exc.value.status_code == 502


def test_this_dish_does_not_mean_today_when_revising_a_future_meal():
    scope = menu_scope("Đổi món này", context(pending_menu_changes=[swap().model_dump(exclude_none=True)]))
    assert (scope.day, scope.meal) == (date(2026, 11, 17), "dinner")


@pytest.mark.parametrize("bad", [swap(recipe_id="cabbage"), swap(index=0, recipe_id="tofu")])
def test_unaccented_request_still_validates_dish_and_slot(monkeypatch, bad):
    monkeypatch.setattr(gateway, "generate", lambda _: gateway.Answer(reply="Đề xuất", actions=[bad]))
    with pytest.raises(gateway.HTTPException) as exc:
        gateway.assistant_turn(gateway.Turn(message="toi mai doi mon canh thanh canh chua ca, giu man va rau", context=context()))
    assert exc.value.status_code == 502


def test_ambiguous_unaccented_request_is_clarified_before_calling_provider(monkeypatch):
    monkeypatch.setattr(gateway, "generate", lambda _: pytest.fail("Must clarify first"))
    answer = gateway.assistant_turn(gateway.Turn(message="mai doi mon do", context=context()))
    assert answer.reply == gateway.MEAL_QUESTION


@pytest.mark.parametrize("messages,terms", [
    (["Tủ lạnh không còn thịt heo, trứng. Chỉ còn rau muống."], ["thịt heo", "trứng", "rau muống"]),
    (["Hết thịt lợn, còn rau muống.", "Tủ lạnh có thịt heo."], ["thịt heo", "rau muống"]),
    (["Chỉ còn rau muống.", "Tủ lạnh có trứng."], ["trứng", "rau muống"]),
    (["Có rau muống, không có thịt heo và trứng."], ["thịt heo", "trứng", "rau muống"]),
])
def test_pantry_uses_latest_availability_and_negated_lists(messages, terms):
    assert pantry_names(gateway.RECIPES, terms, messages) == {"Rau muống"}


def test_pantry_accepts_alias_and_retains_additive_updates():
    assert pantry_names(gateway.RECIPES, ["trứng gà", "rau muống"],
                        ["Mới mua thêm rau muống", "Tủ lạnh có trứng"]) == {"Trứng gà", "Rau muống"}


def test_dish_reference_cannot_edit_an_unrelated_slot(monkeypatch):
    monkeypatch.setattr(gateway, "generate", lambda _: gateway.Answer(reply="Đề xuất", actions=[swap(index=0, recipe_id="tofu")]))
    turn = gateway.Turn(message="Đổi món đó", context=context(last_menu_changes=[swap().model_dump(exclude_none=True)]))
    with pytest.raises(gateway.HTTPException) as exc:
        gateway.assistant_turn(turn)
    assert exc.value.status_code == 502


def test_menu_request_cannot_accept_unrequested_side_effect(monkeypatch):
    # A proposal to swap the soup must not quietly include changes to family size.
    monkeypatch.setattr(gateway, "generate", lambda _: gateway.Answer(reply="Đề xuất", actions=[
        swap(), gateway.Action(type="set_people", date="2026-11-17", meal="dinner", people=12)]))
    turn = gateway.Turn(message="Tối mai đổi món canh thành canh chua cá", context=context())
    with pytest.raises(gateway.HTTPException) as exc:
        gateway.assistant_turn(turn)
    assert exc.value.status_code == 502


@pytest.mark.parametrize("message,terms,expected", [
    ("Có cà chua", ["cá", "cà chua"], {"Cà chua"}),
    ("Có bông cải xanh", ["cải xanh", "bông cải xanh"], {"Bông cải xanh"}),
    ("Trứng đã hết, còn rau muống", ["trứng", "rau muống"], {"Rau muống"}),
])
def test_pantry_does_not_confuse_overlapping_ingredient_names(message, terms, expected):
    assert pantry_names(gateway.RECIPES, terms, [message]) == expected


def test_menu_change_may_include_explicit_family_request(monkeypatch):
    actions = [swap(), gateway.Action(type="set_people", date="2026-11-17", meal="dinner", people=6)]
    monkeypatch.setattr(gateway, "generate", lambda _: gateway.Answer(reply="Đề xuất", actions=actions))
    answer = gateway.assistant_turn(gateway.Turn(
        message="Tối mai đổi canh thành canh chua cá và đổi khẩu phần cả nhà thành 6 người", context=context()))
    assert answer.actions == actions


@pytest.mark.parametrize("provider_status,expected_status", [(429, 429), (503, 503), (403, 503), (404, 503)])
def test_provider_status_is_actionable_without_secret_or_automatic_retries(monkeypatch, provider_status, expected_status):
    import httpx
    from google import genai
    from google.genai import types
    real_client = genai.Client
    requests = []

    def respond(request):
        requests.append(request)
        return httpx.Response(provider_status, json={"error": {"code": provider_status, "message": "SECRET user data"}})

    def client(**kwargs):
        options = kwargs["http_options"].model_dump(exclude_none=True)
        options["client_args"] = {"transport": httpx.MockTransport(respond)}
        return real_client(**{**kwargs, "http_options": types.HttpOptions(**options)})

    monkeypatch.setattr(genai, "Client", client)
    monkeypatch.setenv("GEMINI_API_KEY", "test-only")
    with pytest.raises(gateway.HTTPException) as exc:
        gateway.assistant_turn(gateway.Turn(message="Món nào nấu nhanh?", context=context()))
    assert exc.value.status_code == expected_status
    assert "SECRET" not in exc.value.detail
    assert len(requests) == 1
    if provider_status in (403, 404):
        assert "cấu hình" in exc.value.detail


def test_sdk_read_timeout_returns_timeout_status(monkeypatch):
    import httpx
    from google import genai
    from google.genai import types
    real_client = genai.Client

    def timeout(request):
        raise httpx.ReadTimeout("SECRET", request=request)

    def client(**kwargs):
        options = kwargs["http_options"].model_dump(exclude_none=True)
        options["client_args"] = {"transport": httpx.MockTransport(timeout)}
        return real_client(**{**kwargs, "http_options": types.HttpOptions(**options)})

    monkeypatch.setattr(genai, "Client", client)
    monkeypatch.setenv("GEMINI_API_KEY", "test-only")
    with pytest.raises(gateway.HTTPException) as exc:
        gateway.assistant_turn(gateway.Turn(message="Món nào nấu nhanh?", context=context()))
    assert exc.value.status_code == 504
    assert "SECRET" not in exc.value.detail


def test_daily_quota_uses_provider_retry_delay_instead_of_promising_seconds(monkeypatch):
    from google.genai import errors

    def exhausted(_):
        raise errors.APIError(429, {"error": {"code": 429, "message": "SECRET", "details": [
            {"@type": "type.googleapis.com/google.rpc.RetryInfo", "retryDelay": "47007s"},
        ]}})

    monkeypatch.setattr(gateway, "_generate", exhausted)
    with pytest.raises(gateway.HTTPException) as exc:
        gateway.generate(gateway.Turn(message="Món nhanh", context=context()))
    assert exc.value.status_code == 429
    assert exc.value.headers == {"Retry-After": "47007"}
    assert "SECRET" not in exc.value.detail
