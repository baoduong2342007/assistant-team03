# Contributing

Every change starts with an issue and is delivered through a reviewed pull request to `main`.

## Before you start

1. Check that the issue has a clear goal, checkable acceptance criteria, one owner, a size, and any required labels.
2. Make sure the issue is assigned to you. Ask before taking work that is already in progress.
3. Start from the latest `main`:

   ```bash
   git switch main
   git pull --ff-only origin main
   git switch -c feat/<issue-number>-<short-description>
   ```

   Use `fix/`, `docs/`, `test/`, or `chore/` when that better describes the change.

## Implement and open a pull request

1. Keep the change focused on the issue and add or update tests when behavior changes.
2. Run the checks documented in the repository README before opening the PR.
3. Commit with a short message that describes the change and references the issue, for example:

   ```text
   feat(backend): match partial office names (refs #12)
   ```

4. Push your branch and open a PR targeting `main`.
5. Complete the PR template. Include `Closes #<issue-number>`, how you tested the change, and a clear AI-use disclosure (`None` if not used).
6. Request review from the teammate assigned by the team's review rotation. Do not approve your own PR.
7. Reply to each review comment and push follow-up commits without force-pushing during review.
8. Merge only after a teammate approves and required checks pass. Use squash merge unless the team agrees otherwise.

## Review expectations

Read the diff and relevant tests. Check correctness, edge cases, readability, security, and whether the tests cover the acceptance criteria. Make comments specific and label them `Blocking:`, `Question:`, `Suggestion:`, or `Praise:` where useful. The PR author should respond to each conversation before merge.

## Secrets and data

Never commit API keys, passwords, access tokens, private `.env` files, or personal data. Use `.env.example` with placeholder values when documenting configuration. Use public, attributable sources for campus information and record the source in `data/README.md`.
