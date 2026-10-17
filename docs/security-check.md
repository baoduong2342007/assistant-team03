# Security Audit Report

- **Date of Audit:** 17/10/2026
- **Auditor / Owner:** Pham Quoc Bao (@simble2k)
- **Role:** QA & Security Lead
- **Related Issue:** #15 (`Security: Add environment example and audit repository data`)

---

## 1. Audit Scope & Methodology

1. **Repository Files:** All tracked files in the current working tree across `src/`, `data/`, `tests/`, `docs/`, `scripts/`, `.github/`, and root configuration files.
2. **Git Commit History:** All commit diffs from initial commit up to `HEAD` on `main`.
3. **Configuration & Ignored Files:** Checked `.gitignore` to ensure private environment configurations and secrets cannot be accidentally staged or committed.

---

## 2. Tool Availability & Scanning Execution

* **Gitleaks Tool Check:**
  - Checked local environment with `where.exe gitleaks`.
  - Result: `gitleaks` is not installed on this workstation.
* **Alternative Automated Pattern Scan:**
  - Executed a custom Python regex scanning inspection across the entire git commit log (`git log -p --all`).
  - Scanned for common secret patterns, including:
    - Generic API keys / secrets / bearer tokens (`api_key`, `secret`, `token`, `password`).
    - GitHub Personal Access Tokens (`ghp_`, `gho_`, etc.).
    - OpenAI API keys (`sk-...`).
    - AWS Access Keys (`AKIA...`).
    - Private SSH / SSL keys (`BEGIN PRIVATE KEY`).

---

## 3. Audit Findings

1. **No Credentials or Secrets Found:**
   - No private keys, database passwords, or third-party API tokens were found in tracked files or commit history.
2. **Environment Variable Configuration:**
   - Added `.env.example` with documented dummy placeholders (`APP_ENV=development`, `APP_LOG_LEVEL=INFO`, and commented-out placeholders for future Week 4 AI API keys).
   - Currently, the virtual assistant runs on a rule-based engine and does not require external API secrets.
3. **Gitignore Rules Verified:**
   - Tested using `git check-ignore -v`:
     - `.env` $\rightarrow$ ignored by `.gitignore:17:.env`.
     - `.env.local` / `.env.production` $\rightarrow$ ignored by `.gitignore:18:.env.*`.
     - `.env.example` $\rightarrow$ explicitly permitted by `.gitignore:19:!.env.example`.
4. **Public Team Roster Emails:**
   - `docs/team.md` contains 5 student contact email addresses.
   - These are public institutional/study contact emails required by Lab 1 deliverable for team identification.
   - The team has reviewed and agreed to retain them in the roster. No personal phone numbers, physical addresses, or passwords are included.
5. **Contributing Guidelines:**
   - `CONTRIBUTING.md` already contains an explicit `## Secrets and data` section warning contributors against committing secrets, credentials, or personal data.

---

## 4. Recommendations & Follow-Up

1. **CI Automated Secret Scanning:** Consider adding a Gitleaks GitHub Action workflow step in `.github/workflows/` so PRs are automatically blocked if a secret pattern is detected.
2. **Pre-commit Hooks:** Recommend team members use a pre-commit check or local linting before committing code that may interact with APIs in Week 4.
