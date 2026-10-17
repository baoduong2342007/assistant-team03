from pathlib import Path
import re

ROOT_DIR = Path(__file__).resolve().parents[1]


def test_env_example_exists_and_safe():
    env_example = ROOT_DIR / ".env.example"
    assert env_example.exists(), ".env.example must exist at repository root"
    content = env_example.read_text(encoding="utf-8")

    # Ensure no real keys or sensitive tokens are present in .env.example
    assert not re.search(r"sk-[A-Za-z0-9]{20,}", content), "Found potential OpenAI key in .env.example"
    assert not re.search(r"gh[pousr]_[A-Za-z0-9]{20,}", content), "Found potential GitHub token in .env.example"
    assert not re.search(r"AKIA[0-9A-Z]{16}", content), "Found potential AWS key in .env.example"


def test_gitignore_covers_env():
    gitignore = ROOT_DIR / ".gitignore"
    assert gitignore.exists(), ".gitignore must exist"
    content = gitignore.read_text(encoding="utf-8")
    lines = [line.strip() for line in content.splitlines() if line.strip() and not line.startswith("#")]
    assert ".env" in lines, ".gitignore must explicitly ignore .env"
    assert "!.env.example" in lines, ".gitignore must whitelist .env.example"


def test_security_report_exists():
    report = ROOT_DIR / "docs" / "security-check.md"
    assert report.exists(), "docs/security-check.md must exist"
    content = report.read_text(encoding="utf-8")
    assert "Audit Scope" in content
    assert "Findings" in content
