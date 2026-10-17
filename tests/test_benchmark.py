import csv
from pathlib import Path
import pytest
from assistant.rules import reply

EVAL_DIR = Path(__file__).resolve().parents[1] / "eval"
CASES_FILE = EVAL_DIR / "cases.csv"


def load_cases():
    assert CASES_FILE.exists(), f"Benchmark cases file not found at {CASES_FILE}"
    with CASES_FILE.open(encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f)
        assert set(reader.fieldnames) == {"question", "expected", "category"}, (
            f"Invalid columns: {reader.fieldnames}"
        )
        return list(reader)


CASES = load_cases()


def test_benchmark_dataset_size_and_categories():
    assert len(CASES) >= 15, f"Expected at least 15 benchmark questions, found {len(CASES)}"
    categories = {row["category"] for row in CASES}
    assert "office_lookup" in categories
    assert "greeting" in categories
    assert "out_of_scope" in categories
    assert "edge_case" in categories

    edge_cases = [row for row in CASES if row["category"] == "edge_case"]
    assert len(edge_cases) >= 3, f"Expected at least 3 edge cases, found {len(edge_cases)}"


@pytest.mark.parametrize("case", CASES, ids=[f"{c['category']}-{i}" for i, c in enumerate(CASES)])
def test_case_expected_reply(case):
    actual = reply(case["question"])
    assert actual == case["expected"], (
        f"Mismatch for input {case['question']!r}:\nExpected: {case['expected']!r}\nActual:   {actual!r}"
    )
