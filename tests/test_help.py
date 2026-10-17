import pytest

from assistant import rules


@pytest.mark.parametrize("message", ["help", "HELP", "  HeLp?!  "])
def test_help_lists_capabilities_and_examples(message):
    answer = rules.reply(message)
    assert "Greeting: 'hi' or 'xin chao'" in answer
    assert "Location: 'where is the training office?' or 'where is training'" in answer
    assert "Opening hours: 'when does the library open?'" in answer
    assert "full office name" in answer


def test_help_does_not_require_office_data(monkeypatch):
    def unavailable():
        raise FileNotFoundError("Office data unavailable")

    monkeypatch.setattr(rules, "load_offices", unavailable)
    assert "Greeting" in rules.reply("help")


def test_helpdesk_is_still_an_office_lookup():
    assert rules.reply("IT Helpdesk") == "IT Helpdesk: room E.005, open Mon-Fri 08:00-17:00."
