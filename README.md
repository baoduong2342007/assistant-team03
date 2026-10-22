# Study Assistant — assistant-team03

A starter repository for the CSC10014 Smart Virtual Assistant project.

## Setup
Prerequisites: Python 3.10+, Git.

```bash
git clone https://github.com/baoduong2342007/assistant-team03.git
cd assistant-team03
python -m venv .venv
```

Activate the virtual environment:
- Linux / macOS / Git Bash:
  ```bash
  source .venv/bin/activate
  ```
- Windows PowerShell:
  ```powershell
  .venv\Scripts\Activate.ps1
  ```

Install pinned dependencies and package in editable mode:
```bash
pip install -r requirements.txt
pip install -e .
```

Verify your environment setup:
```bash
python scripts/check_env.py
```

## Run
Interactive chat mode:
```bash
python -m assistant
```
Single question mode:
```bash
python -m assistant "where is the training office?"
```

Show supported questions and examples:
```bash
python -m assistant help
```
The `help` command is case-insensitive and lists greetings, office locations,
and opening-hours questions. In interactive mode, type `help` at the prompt.

You can also use a short office name:
```bash
python -m assistant "where is training"
python -m assistant "when does the library open?"
```
Lookup is case-insensitive and matches complete words, so `training` matches
`Training Office`, while `retraining` does not. Full names take priority over
partial matches. If several offices match, the assistant lists their names
and asks you to use a full name. Each successful lookup includes both the room
and opening hours.

Generic words such as `office` follow the same rule: one matching office is
returned; two or more matching offices produce a clarification list sorted by
name. This list is independent of the order of records in the CSV.

## Test
```bash
pytest -q
```

## Project structure
- `src/assistant/`: core rule-based assistant package
- `data/`: CSV data files for campus offices
- `tests/`: automated smoke tests
- `docs/`: project documentation and team roster
- `ui/`: user interface roadmap (Week 7)
- `scripts/`: environment check script

## Troubleshooting
- "No module named assistant" -> run `pip install -e .` with active `.venv`
- PowerShell blocks Activate.ps1 -> run `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`
