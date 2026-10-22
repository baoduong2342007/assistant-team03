# Week 2 — Dương's evidence (2026-10-21)

## H1: implementation and historical gap

Issues [#6](https://github.com/baoduong2342007/assistant-team03/issues/6) and
[#7](https://github.com/baoduong2342007/assistant-team03/issues/7) were implemented
in [PR #16](https://github.com/baoduong2342007/assistant-team03/pull/16).
Feature/bug templates, PR template, workflow and CONTRIBUTING.md are present.
The workflow installs dependencies and the editable package and runs pytest
on pushes and PRs, with Python 3.10 and 3.12 jobs.

On 2026-10-21 the GitHub API enabled and verified protection of `main`:

- A PR with at least one approval is required; stale approvals are dismissed.
- Required checks are `Python 3.10` and `Python 3.12`, with an up-to-date branch.
- Rules apply to administrators; force pushes and branch deletion are blocked.

PR #16 had no teammate approval before merge. This setting change does not
repair that historical gap. Instructor acceptance of remediation is Pending;
no instructor message or decision is claimed here. Board evidence belongs
in the team summary maintained by Bảo/Huy.

## H2: supplemental reviews posted

Both reviews are explicitly dated after merge and contain three separate
inline comments labelled Question, Suggestion and Praise:

| PR | Tested exact head | Local Python 3.12 | Review |
| --- | --- | --- | --- |
| #17 | `266e46b6ee3f55e23bea5eb9bab0342cc3677a01` | 22 passed | [Review](https://github.com/baoduong2342007/assistant-team03/pull/17#pullrequestreview-5596909986) |
| #22 | `3046e162f9ad9e55174a0e3c42017f88554341b2` | 51 passed | [Review](https://github.com/baoduong2342007/assistant-team03/pull/22#pullrequestreview-5596910478) |

For each exact head, a temporary `git archive` snapshot was tested with
`python -m pytest -q` and its `src` directory on PYTHONPATH. All tests passed
without skips. The #22 tests establish consistency, not independent factual
verification of the room/hour data. Authors' responses remain Pending.

## DoD consensus

[PR #23](https://github.com/baoduong2342007/assistant-team03/pull/23) was read and
tested at `dbef29b727f7bf375d7a600532e1ab151020f19d` (51 passed).
Dương agrees with the eight criteria and verification procedure, but
[requested changes](https://github.com/baoduong2342007/assistant-team03/pull/23#pullrequestreview-5596910906)
because its table prefilled all five signatures with 2026-10-20 before actual
approvals. Record actual dates and review links; approval remains Pending.

## Remaining collaboration gates

- Controlled conflict: prepare Dương's Dương–Huy branch from
  `3620a55` (full SHA recorded in that PR). Huy must start from the same SHA,
  change the same greeting line differently, and open his own PR. Merge A only
  after approval, then B merges `origin/main`, resolves the real conflict with
  the partner, runs tests and retains the explanatory merge commit. Do not
  squash the conflict drill. The Bảo–Dương pair follows after this pair.
- AI-PR exercise: no instructor exercise PR identified among current open
  PRs. Quyền's source/assignment confirmation and all-member review are Pending.
- H4 reflection: no received review on PR #16 was found in the review/comment
  audit. A truthful reflection needs an actual received comment, a resulting
  code change, and their links; no fictional feedback or reflection is added.
- Final checks/tag: Bootcamp PRs [#34](https://github.com/baoduong2342007/python-bootcamp/pull/34),
  [#35](https://github.com/baoduong2342007/python-bootcamp/pull/35), and
  [#36](https://github.com/baoduong2342007/python-bootcamp/pull/36) await the
  relevant peer approvals/dependencies. Do not create `py-w2-done` before all
  five members, integration tests and release conditions are complete.
- Actual hours and the LMS deadline need Dương's confirmation; neither is inferred.

## AI assistance

Codex hỗ trợ đọc diff và yêu cầu, chạy tests, soạn và đăng review theo phân
công, cấu hình bảo vệ nhánh, chuẩn bị code/tài liệu và thao tác Git/GitHub.
Nội dung/code do AI tạo được giữ lại trong các PR và được khai báo; nhận xét
này không thay thế approval hoặc phản hồi thật từ đồng đội.
