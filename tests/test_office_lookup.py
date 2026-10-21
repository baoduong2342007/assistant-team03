import pytest

from assistant.rules import reply


@pytest.mark.parametrize("message", ["training", "where is training", "  TRAINING?!  "])
def test_short_name(message):
    assert reply(message) == "Training Office: room I.101, open Mon-Fri 07:30-16:30."


@pytest.mark.parametrize(
    ("message", "office", "room"),
    [
        ("Where is the Training Office?", "Training Office", "I.101"),
        ("when does student affairs open?", "Student Affairs", "I.102"),
        ("library", "Library", "B.201"),
        ("where is the IT Helpdesk?", "IT Helpdesk", "E.005"),
    ],
)
def test_full_names(message, office, room):
    answer = reply(message)
    assert answer.startswith(f"{office}: room {room}, open ")


@pytest.mark.parametrize("message", ["admissions", "university", "retraining"])
def test_unknown_name(message):
    assert "don't know" in reply(message)


def test_ambiguous_partial_name():
    offices = {
        "training office": {"name": "Training Office", "room": "A.101", "hours": "09-17"},
        "training support": {"name": "Training Support", "room": "B.201", "hours": "08-16"},
    }
    answer = reply("where is training", offices)
    assert answer == (
        "Which office do you mean? Please use a full name: Training Office, Training Support."
    )
    assert reply("where is training", dict(reversed(list(offices.items())))) == answer
    assert reply("training office", offices).startswith("Training Office: room A.101")


def test_multiple_full_names_request_clarification():
    answer = reply("Training Office or Student Affairs?")
    assert "Which office" in answer
    assert "Training Office" in answer
    assert "Student Affairs" in answer
    assert "room" not in answer


def test_empty_offices_are_respected():
    assert "don't know" in reply("training", {})


@pytest.mark.parametrize("message", ["office", "where is office"])
@pytest.mark.parametrize("reverse", [False, True])
def test_generic_office_token_requests_clarification(message, reverse):
    offices = {
        "training office": {"name": "Training Office", "room": "A.101", "hours": "09-17"},
        "accounting office": {"name": "Accounting Office", "room": "B.201", "hours": "08-16"},
    }
    if reverse:
        offices = dict(reversed(list(offices.items())))
    assert reply(message, offices) == (
        "Which office do you mean? Please use a full name: Accounting Office, Training Office."
    )


def test_generic_office_token_with_one_candidate():
    offices = {
        "training office": {"name": "Training Office", "room": "A.101", "hours": "09-17"},
        "library": {"name": "Library", "room": "B.201", "hours": "08-16"},
    }
    assert reply("office", offices) == "Training Office: room A.101, open 09-17."


@pytest.mark.parametrize(
    ("message", "office", "room"),
    [
        ("Where is the Accounting Office?", "Accounting Office", "F.01"),
        ("when does accounting open?", "Accounting Office", "F.01"),
        ("where is the Health Clinic?", "Health Clinic", "C.12"),
        ("clinic", "Health Clinic", "C.12"),
        ("Science and Technology Office", "Science and Technology Office", "F.03"),
        ("where is Testing and Quality Assurance?", "Testing and Quality Assurance", "B.02"),
    ],
)
def test_newly_added_offices_lookup(message, office, room):
    answer = reply(message)
    assert answer.startswith(f"{office}: room {room}, open ")

