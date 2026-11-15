import re
import unicodedata
from dataclasses import dataclass
from datetime import date, timedelta
from itertools import product
from typing import Literal

Meal = Literal["lunch", "dinner"]
KINDS = ("main", "soup", "veg")
MEAL_QUESTION = "Bạn muốn áp dụng cho bữa trưa hay bữa tối?"


def normalize_text(value: str) -> str:
    """Match Vietnamese with or without accents, retaining sentence punctuation."""
    text = unicodedata.normalize("NFD", value.casefold()).replace("đ", "d")
    return " ".join("".join(c for c in text if not unicodedata.combining(c)).split())


RELATIVE_DAYS = {
    "hôm nay": 0,
    "hom nay": 0,
    "nay": 0,
    "ngày mai": 1,
    "ngay mai": 1,
    "mai": 1,
    "ngày mai nữa": 2,
    "ngay mai nua": 2,
    "mai nữa": 2,
    "mai nua": 2,
    "ngày kia": 2,
    "ngay kia": 2,
    "ngày mốt": 2,
    "ngay mot": 2,
    "mốt": 2,
    "hôm qua": -1,
    "hôm kia": -2,
}
RELATIVE_DAYS = {
    normalize_text(key): value
    for key, value in RELATIVE_DAYS.items()
    if key not in ("mốt", "nay")
}
WEEKDAYS = {"hai": 0, "ba": 1, "tu": 2, "bon": 2, "nam": 3, "sau": 4, "bay": 5}
RELATIVE_PATTERN = "|".join(sorted(RELATIVE_DAYS, key=len, reverse=True))
DATE_PATTERN = re.compile(
    rf"\b(?P<relative>{RELATIVE_PATTERN})\b|\b(?P<iso>\d{{4}}-\d{{2}}-\d{{2}})\b"
    r"|\b(?P<day>\d{1,2})/(?P<month>\d{1,2})(?:/(?P<year>\d{4}))?(?![\d/])"
    r"|\b(?P<weekday>thu (?:hai|ba|tu|bon|nam|sau|bay|[2-7])|chu nhat)(?P<next_week> tuan sau)?\b"
)
MEAL_PATTERN = re.compile(
    r"\b(trưa|trua|tối(?! (?:đa|thiểu)\b)|bua toi|toi(?= (?:nay|mai|ngay|thu)\b)"
    r"|bữa sáng|bua sang|sáng(?= (?:nay|mai|ngày|thứ)\b)|sang(?= (?:nay|mai|ngay|thu)\b))\b"
)
MEAL_VALUES = {
    "trưa": "lunch",
    "trua": "lunch",
    "tối": "dinner",
    "bua toi": "dinner",
    "toi": "dinner",
    "bữa sáng": "breakfast",
    "bua sang": "breakfast",
    "sáng": "breakfast",
    "sang": "breakfast",
}
SLOT_NUMBERS = {"man": 0, "canh": 1, "rau": 2}
SLOT = r"(?:mon )?(?:man|canh|rau)"
SLOT_LIST = rf"{SLOT}(?:(?:, *| va | cung ){SLOT})*"
FOLLOW_UP = re.compile(
    r"^(?:a|y minh la|y toi la|thoi)\b|\b(?:doi sang|doi lai|thay bang|mon do)\b"
)


@dataclass(frozen=True)
class MenuScope:
    day: date | None = None
    meal: Meal | None = None
    clarification: str | None = None


def parse_day(value: str) -> date:
    parsed = date.fromisoformat(value)
    if parsed.isoformat() != value:
        raise ValueError("Ngày phải có định dạng YYYY-MM-DD")
    return parsed


def _mentioned_days(text: str, today: date) -> set[date]:
    days = set()
    original = unicodedata.normalize("NFC", text).casefold()
    if re.search(r"\bmốt\b", original):
        days.add(today + timedelta(days=2))
    if re.search(r"\b(?:tối|toi|trưa|trua|sáng|sang) nay\b", original):
        days.add(today)
    for match in DATE_PATTERN.finditer(normalize_text(text)):
        if match["relative"]:
            days.add(today + timedelta(days=RELATIVE_DAYS[match["relative"]]))
        elif match["iso"]:
            days.add(parse_day(match["iso"]))
        elif match["weekday"]:
            name = match["weekday"].removeprefix("thu ")
            weekday = int(name) - 2 if name.isdigit() else WEEKDAYS.get(name, 6)
            offset = weekday - today.weekday()
            days.add(
                today + timedelta(days=offset + 7 if match["next_week"] else offset % 7)
            )
        else:
            day, month = int(match["day"]), int(match["month"])
            # Find a date without a year in the rolling week, including New Year.
            week = (today + timedelta(days=i) for i in range(7))
            in_week = next((d for d in week if (d.day, d.month) == (day, month)), None)
            days.add(
                in_week
                if not match["year"] and in_week
                else date(int(match["year"] or today.year), month, day)
            )
    return days


def _mentioned_meals(text: str) -> set[str]:
    meals = set()
    for match in MEAL_PATTERN.finditer(text):
        name = match.group(1)
        if name == "sang" and re.search(
            r"\b(?:doi|chuyen|thay)(?: \w+){0,3}$",
            normalize_text(text[: match.start()]),
        ):
            continue  # "đổi sang ngày mai" uses a verb, not a breakfast request.
        meals.add(MEAL_VALUES[name])
    return meals


def _context_target(value: dict) -> tuple[date | None, Meal | None]:
    try:
        day = parse_day(value.get("date"))
    except (TypeError, ValueError):
        day = None
    meal = value.get("meal")
    return day, meal if meal in ("lunch", "dinner") else None


def _single_change_target(changes: object) -> tuple[date | None, Meal | None]:
    if (
        not isinstance(changes, list)
        or not changes
        or not all(isinstance(change, dict) for change in changes)
    ):
        return None, None
    targets = {_context_target(change) for change in changes}
    return next(iter(targets)) if len(targets) == 1 else (None, None)


def menu_scope(question: str, context: dict, previous_question: str = "") -> MenuScope:
    today = parse_day(context["today"])
    text = " ".join(unicodedata.normalize("NFC", question).casefold().split())
    try:
        days = _mentioned_days(text, today)
        meals = _mentioned_meals(text)
        if previous_question:
            prior = " ".join(
                unicodedata.normalize("NFC", previous_question).casefold().split()
            )
            days = days or _mentioned_days(prior, today)
            meals = meals or _mentioned_meals(prior)
    except ValueError:
        return MenuScope(
            clarification="Bạn cho mình ngày hợp lệ trong thực đơn bảy ngày nhé."
        )
    if "breakfast" in meals:
        return MenuScope(
            clarification="Thực đơn hiện hỗ trợ bữa trưa và bữa tối. Bạn muốn chọn bữa nào?"
        )
    if len(days) > 1 or len(meals) > 1:
        return MenuScope(
            clarification="Bạn muốn chỉnh hoặc gợi ý cho ngày và bữa nào trước?"
        )

    current_day, current_meal = _context_target(context)
    focus_day, focus_meal = _single_change_target(context.get("pending_menu_changes"))
    if focus_day is None and FOLLOW_UP.search(normalize_text(text)):
        focus_day, focus_meal = _single_change_target(context.get("last_menu_changes"))
    current_meal_requested = "bữa này" in text or "bua nay" in text
    if current_meal_requested:
        focus_day, focus_meal = current_day, current_meal
    day = next(iter(days), focus_day or current_day)
    if day and not today <= day <= today + timedelta(days=6):
        return MenuScope(
            clarification="Ngày đó nằm ngoài thực đơn bảy ngày. Bạn muốn chọn ngày nào trong tuần đang xem?"
        )
    if meals:
        return MenuScope(day, "lunch" if "lunch" in meals else "dinner")
    if focus_day == day and focus_meal:
        return MenuScope(day, focus_meal)
    if days and day != current_day and not current_meal_requested:
        return MenuScope(day, clarification=MEAL_QUESTION)
    return MenuScope(day, current_meal)


def named_slots(question: str, command: str) -> set[int]:
    pattern = rf"\b(?:{normalize_text(command)})\s+({SLOT_LIST})\b"
    return {
        SLOT_NUMBERS[slot]
        for group in re.findall(pattern, normalize_text(question))
        for slot in re.findall(r"\b(?:man|canh|rau)\b", group)
    }


def check_meal(day: date, meal: Meal, context: dict) -> None:
    today = parse_day(context["today"])
    if not today <= day <= today + timedelta(days=6):
        raise ValueError("Ngày nằm ngoài thực đơn bảy ngày")
    if "meals" in context and not any(
        isinstance(entry, dict)
        and entry.get("date") == day.isoformat()
        and entry.get("meal") == meal
        for entry in context["meals"]
    ):
        raise ValueError("Bữa ăn không có trong thực đơn")


def check_scope(scope: MenuScope, day: date, meal: Meal, context: dict) -> None:
    check_meal(day, meal, context)
    if (scope.day is not None and day != scope.day) or (
        scope.meal is not None and meal != scope.meal
    ):
        raise ValueError("Ngày hoặc bữa không khớp yêu cầu")


INGREDIENT_ALIASES = {
    "Thịt ba chỉ": ("thịt heo", "thịt lợn", "ba chỉ", "ba rọi", "thịt ba rọi"),
    "Thịt heo bằm": (
        "thịt heo",
        "thịt lợn",
        "thịt bằm",
        "thịt băm",
        "thịt heo băm",
        "thịt heo xay",
        "thịt lợn xay",
    ),
    "Trứng gà": ("trứng",),
    "Thịt gà": ("gà",),
    "Cá basa": ("cá", "cá ba sa"),
    "Đậu hũ": ("đậu phụ", "tàu hũ"),
    "Thơm": ("dứa", "khóm"),
    "Dưa leo": ("dưa chuột",),
    "Bí đỏ": ("bí ngô",),
    "Cải xanh": ("cải bẹ xanh",),
    "Bông cải xanh": ("súp lơ xanh",),
}
STAPLE_GROUP = "Gia vị & đồ khô"
RICE = {"name": "Gạo", "unit": "g", "group": STAPLE_GROUP}


def _ingredient_words(value: str) -> str:
    return " ".join(re.findall(r"[a-z0-9]+", normalize_text(value)))


def ingredient_names(recipes: dict, terms: list[str]) -> set[str]:
    terms = {_ingredient_words(item) for item in terms}
    names = {
        item["name"] for recipe in recipes.values() for item in recipe["ingredients"]
    } | {RICE["name"]}
    return {
        name
        for name in names
        if any(
            _ingredient_words(alias) in terms
            for alias in (name, *INGREDIENT_ALIASES.get(name, ()))
        )
    }


def pantry_names(recipes: dict, terms: list[str], messages: list[str]) -> set[str]:
    # Resolve canonical names before comparing newer messages: "hết thịt lợn"
    # invalidates an older "có thịt heo". Keep comma-separated negations together.
    aliases: dict[str, set[str]] = {}
    names = {
        item["name"] for recipe in recipes.values() for item in recipe["ingredients"]
    } | {RICE["name"]}
    for name in names:
        for alias in (name, *INGREDIENT_ALIASES.get(name, ())):
            aliases.setdefault(normalize_text(alias), set()).add(name)
    # Longest match avoids reading "cà chua" as "cá", or "bông cải xanh" as "cải xanh".
    pattern = re.compile(
        r"\b(?:"
        + "|".join(re.escape(a) for a in sorted(aliases, key=len, reverse=True))
        + r")\b"
    )
    remaining = ingredient_names(recipes, terms)
    grounded = set()
    for message in messages:
        source = normalize_text(message)
        availability = {}
        for mention in pattern.finditer(source):
            clause = re.split(r"[.;!?]|\bnhung\b", source[: mention.start()])[-1]
            markers = list(
                re.finditer(
                    r"\b(?:khong (?:co|con)|het|thieu|co|con|moi mua|mua them)\b",
                    clause,
                )
            )
            marker = markers[-1].group() if markers else ""
            available = marker not in ("khong co", "khong con", "het", "thieu")
            if re.match(r"\s+(?:da )?het\b", source[mention.end() :]):
                available = False
            for name in aliases[mention.group()]:
                availability[name] = available
        grounded.update(name for name in remaining if availability.get(name) is True)
        remaining -= availability.keys()
        if re.search(r"\bchi (?:co|con)\b", source):
            break  # An explicit exhaustive list replaces older pantry contents.
    return grounded


@dataclass(frozen=True)
class MealOption:
    recipe_ids: tuple[str, ...]
    pantry_keys: list[str]
    missing: list[str]


def rank_meals(recipes: dict, pantry: set[str], excluded: set[str]) -> list[MealOption]:
    groups = [
        [recipe for recipe in recipes.values() if recipe["kind"] == kind]
        for kind in KINDS
    ]
    ranked = []
    for menu in product(*groups):
        ingredients = {RICE["name"]: RICE["group"]}
        present_keys = set()
        for recipe in menu:
            for item in recipe["ingredients"]:
                ingredients[item["name"]] = item["group"]
                if item["name"] in pantry:
                    present_keys.add(f"{item['name']}|{item['unit']}")
        available = pantry & ingredients.keys()
        if not available or excluded & ingredients.keys():
            continue
        if RICE["name"] in pantry:
            present_keys.add(f"{RICE['name']}|{RICE['unit']}")
        missing = ingredients.keys() - pantry
        main_available = sum(ingredients[name] != STAPLE_GROUP for name in available)
        main_missing = sum(ingredients[name] != STAPLE_GROUP for name in missing)
        ids = tuple(recipe["id"] for recipe in menu)
        ranked.append(
            (
                (-main_available, -len(available), main_missing, len(missing), ids),
                MealOption(
                    ids,
                    sorted(present_keys),
                    sorted(
                        missing,
                        key=lambda name: (ingredients[name] == STAPLE_GROUP, name),
                    ),
                ),
            )
        )
    return [option for _, option in sorted(ranked, key=lambda row: row[0])]
