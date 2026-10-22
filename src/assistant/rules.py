"""A tiny rule-based responder. No AI yet — that comes in Week 4."""
from __future__ import annotations

import csv
import re
from pathlib import Path

DATA_DIR = Path(__file__).resolve().parents[2] / "data"

GREETINGS = {"hi", "hello", "hey", "xin chao", "chao"}


def load_offices(path: Path = DATA_DIR / "offices.csv") -> dict[str, dict[str, str]]:
    """Return {office_name_lower: row} from the offices CSV."""
    with path.open(encoding="utf-8", newline="") as f:
        return {row["name"].lower(): row for row in csv.DictReader(f)}


def reply(message: str, offices: dict[str, dict[str, str]] | None = None) -> str:
    """Answer a message with simple rules."""
    text = message.strip().lower().rstrip("?!.")
    if not text:
        return "Please type a question."
    if text in GREETINGS:
        return "Hello! Welcome to the campus assistant. Type help for office locations and hours."
    if text == "help":
        return (
            "I can greet you and look up campus office locations and opening hours.\n"
            "- Greeting: 'hi' or 'xin chao'.\n"
            "- Location: 'where is the training office?' or 'where is training'.\n"
            "- Opening hours: 'when does the library open?'\n"
            "Use a full office name if a short name matches several offices."
        )
    offices = offices if offices is not None else load_offices()
    # Compare complete words so 'it' does not match inside 'university'.
    query = " " + " ".join(re.findall(r"\w+", text)) + " "
    names = {name: re.findall(r"\w+", name.lower()) for name in offices}
    matches = [
        row for name, row in offices.items()
        if names[name] and f" {' '.join(names[name])} " in query
    ]
    if not matches:
        matches = [
            row for name, row in offices.items()
            if any(f" {word} " in query for word in names[name])
        ]
    if len(matches) == 1:
        row = matches[0]
        return f"{row['name']}: room {row['room']}, open {row['hours']}."
    if len(matches) > 1:
        options = ", ".join(sorted(row["name"] for row in matches))
        return f"Which office do you mean? Please use a full name: {options}."
    return "I don't know that yet. Try asking about an office, e.g. 'training office'."
