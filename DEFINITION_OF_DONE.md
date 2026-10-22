# Definition of Done (team agreement)

A task is **Done** only when all of these are true:

1. Acceptance criteria in the issue are met.
2. Code is on a feature branch and merged through a pull request.
3. At least one teammate approved the PR after reading it.
4. Tests were added or updated, and CI is green.
5. No secrets, personal data or large files were committed.
6. README / docs are updated if setup or behaviour changed.
7. AI use (if any) is disclosed in the PR.
8. The issue is closed and the board card is in "Done".

## Done Verification Procedure

To ensure the 8 DoD criteria above are consistently met across all features and fixes, the team follows this verification procedure before merging any pull request:

1. **Pre-PR Self-Verification**: The author verifies that the issue criteria are satisfied, runs all local tests (`pytest`), checks lint/format (`ruff`), and confirms no secrets, tokens or temporary files are staged.
2. **Pull Request Submission**: The PR must reference its target issue via `Closes #<id>`, outline the exact changes, document validation commands/results, and explicitly disclose AI usage.
3. **Peer Review**: At least one assigned teammate reviews the PR following `REVIEW_CHECKLIST.md`. Reviewers use standard labels (`Blocking:`, `Question:`, `Suggestion:`, `Nit:`, `Praise:`). Any blocking comments must be resolved by the author with follow-up commits.
4. **CI Automated Gate**: GitHub Actions workflows (Python 3.10 and 3.12 checks) must run and pass with a green check.
5. **Merge & Completion**: Merging is performed via Squash Merge (or merge commit for conflict demonstration). The author verifies that the linked issue is automatically closed and moves the board card to "Done".

## Team Consensus & Sign-Off

This section records the formal adoption of the Definition of Done and its verification procedure across all 5 members. Each teammate confirms consensus by reviewing and approving this pull request.

> **Context note on historical tracking**: Pull Request [#18](https://github.com/baoduong2342007/assistant-team03/pull/18) previously introduced the initial draft but was merged with only one approval from team lead Dương. This update establishes explicit, full-team consensus and review across all 5 members.

| Member | Full Name | GitHub Username | Role in Team | Consensus Status | Date |
| --- | --- | --- | --- | --- | --- |
| 1 | Trương Quang Bảo Dương | `baoduong2342007` | Team Lead / Release Manager | Pending PR review & approval | — |
| 2 | Trần Quốc Huy | `tqhuy0602` | Backend & Process Lead | Pending PR review & approval | — |
| 3 | Phan Quỳnh Quyền | `pqquyenn` | CI & Data Lead | Pending PR review & approval | — |
| 4 | Lương Nhật Minh | `luongnhatinh` | UI & Workflow Lead | Agreed & Signed (PR #23 author) | 2026-10-20 |
| 5 | Phạm Quốc Bảo | `simble2k` | QA & Evidence Lead | Pending PR review & approval | — |
