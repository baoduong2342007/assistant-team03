# Huy's Week 2 submitted reviews

Prepared with Codex assistance from actual diffs and recorded checks, and posted
after Huy explicitly authorized these reviews in this chat. Every submission is
a COMMENT, not an approval or personal team-consensus signature. Heads were
rechecked immediately before submission. Supplemental reviews remain post-merge.

## [python-bootcamp #31](https://github.com/baoduong2342007/python-bootcamp/pull/31)

Posted review: [COMMENT](https://github.com/baoduong2342007/python-bootcamp/pull/31#pullrequestreview-5601689870).

Reviewed revision: `a272a58913a2b03c7038a89c17e0b2d754462b49`.

Local Python 3.14: official W2 3 passed, cross-tests 5 passed, package printed both sample tasks, Ruff passed.

- `members/luongnhatinh/w2/io_utils.py:27` — Question: Should line numbers refer to physical CSV lines when a quoted title spans multiple lines? enumerate(reader, start=2) counts records, so the next invalid record can report a different line from the file. Could we document the convention or use reader.line_num and add a multiline-title regression?
- `members/luongnhatinh/w2/io_utils.py:39` — Suggestion: Catch the expected conversion/validation exceptions (for example ValueError) rather than Exception. The broad catch can relabel an unrelated programming error as invalid CSV data; preserving unexpected failures makes debugging easier.
- `members/luongnhatinh/w2/__main__.py:8` — Praise: Resolving the sample from __file__ and keeping execution under the __main__ guard makes the package portable and import-safe. I verified the two sample tasks print successfully without an absolute machine path.

## [python-bootcamp #32](https://github.com/baoduong2342007/python-bootcamp/pull/32)

Posted review: [COMMENT](https://github.com/baoduong2342007/python-bootcamp/pull/32#pullrequestreview-5601690501).

Reviewed revision: `2132db8807aa46b00b71cf1404c2aa8d84c98793`.

Local Python 3.14: all five render tests and targeted Ruff checks passed. Contract PR #35 remains a merge dependency.

- `shared/study_planner/render.py:14` — Question: The shared Task contract allows arbitrary titles, including a newline. Should render preserve such a title verbatim or escape it to keep one output line per block? Please record the intended behavior rather than silently choosing a new format.
- `tests/shared_w2/test_render.py:35` — Suggestion: Add an explicit zero-hour block and a Unicode title case. Block currently permits zero hours, and render can be called directly even though allocate omits zero blocks; these cases would make the public formatting contract clearer.
- `shared/study_planner/render.py:15` — Praise: Joining formatted lines preserves input order, avoids a trailing newline, and uses :g consistently with the sample. The empty sequence and non-mutation tests cover useful caller guarantees.

## [assistant-team03 #22](https://github.com/baoduong2342007/assistant-team03/pull/22)

Posted review: [COMMENT](https://github.com/baoduong2342007/assistant-team03/pull/22#pullrequestreview-5601688568).

Reviewed revision: `3046e162f9ad9e55174a0e3c42017f88554341b2`.

Supplemental review after merge, prepared on 2026-10-21; this is not a pre-merge approval. At the exact PR head, 51 local tests passed. Source correctness is not established by passing lookup tests.

- `data/README.md:37` — Question: Could you link the specific announcement or contact page establishing F.01 and Mon-Fri 07:30-16:30 for Accounting Office, rather than only the organizational directory? Please flag room/hours as unverified if the public source does not support those fields.
- `data/README.md:21` — Suggestion: Add a dataset validation test for required columns, nonempty values and case-insensitive unique names before loading the dict. Duplicate names would otherwise overwrite a record in load_offices while ordinary lookup tests could still pass.
- `data/README.md:11` — Praise: The named owner, verification date, per-office source links and semester-based update cadence make maintenance responsibilities visible. The PR also exercises full/short lookups for the new offices; its exact head passes 51 tests locally.

## [assistant-team03 #19](https://github.com/baoduong2342007/assistant-team03/pull/19)

Posted review: [COMMENT](https://github.com/baoduong2342007/assistant-team03/pull/19#pullrequestreview-5601687789).

Reviewed revision: `c85e6ea98ea43753f863684c7a86054ccbacbf08`.

Supplemental review after merge, prepared on 2026-10-21; this is not a pre-merge approval. Read the UI diff and executed backend examples at the exact PR head.

- `docs/ui-chat.md:199` — Question: This example shows Student Affairs in A.02 with 08:00-17:00, but running the documented query at this PR head returns room I.102, open Mon-Fri 08:00-16:00. Are these intended as mock values? Please label them or align them before claiming exact backend alignment.
- `docs/ui-chat.md:187` — Suggestion: Specify a separate metadata source or a future structured response for the citation footer. The current reply() returns only a string and offices.csv has only name,room,hours, so Verified/Owner cannot currently be extracted from that backend contract. Mark the footnote examples as proposed data.
- `docs/ui-chat.md:173` — Praise: Empty, waiting, unknown-query and execution-error states are separated, and the waiting state disables Send to prevent duplicate submissions. The direct Python call and CLI fallback give a practical integration path without adding a UI framework now.

## [assistant-team03 #23](https://github.com/baoduong2342007/assistant-team03/pull/23)

Posted review: [COMMENT](https://github.com/baoduong2342007/assistant-team03/pull/23#pullrequestreview-5601689230).

Reviewed revision: `dbef29b727f7bf375d7a600532e1ab151020f19d`.

Review draft: do not approve while the document asserts unverified full-team sign-offs. This payload is COMMENT, not an approval or personal signature.

- `DEFINITION_OF_DONE.md:26` — Blocking: This paragraph and the sign-off table assert unanimous agreement and dated signatures for all five members. No confirmation of Huy's personal sign-off was established in this session. Keep unconfirmed members Pending and use actual review dates/links instead of recording consent on their behalf.
- `DEFINITION_OF_DONE.md:18` — Question: The pre-PR procedure now requires Ruff, while the current Assistant workflow installs pytest and runs Python 3.10/3.12 tests. Is Ruff a documented local-only check or a new dependency/check that should be added by a separate issue? Please make the executable verification steps explicit.
- `DEFINITION_OF_DONE.md:22` — Praise: The merge procedure distinguishes normal squash merges from merge commits used to retain conflict-drill evidence. That preserves traceability without treating historical missing approvals as if they happened before merge.

## [python-bootcamp #35](https://github.com/baoduong2342007/python-bootcamp/pull/35)

Posted review: [COMMENT](https://github.com/baoduong2342007/python-bootcamp/pull/35#pullrequestreview-5601691271).

Reviewed revision: `ad85ecd09d5ce0b32e2d47976f86912d3d863e5a`.

Contract review draft. The six contract tests pass locally, and Huy's separate prioritise implementation passes eight additional tests against these exact shared types. No personal team-consensus approval is inferred.

- `shared/contract.py:27` — Question: Block permits hours == 0 while allocate is required not to emit zero-hour blocks. Is the distinction intentional for direct construction/rendering? Please keep this distinction explicit so callers do not mistake the constructor guarantee for allocate's stronger postcondition.
- `docs/shared-w2-contract.md:25` — Suggestion: Keep contract consensus separate from implementation completion. The branch currently also includes total_effort and integration scaffolding; consider isolating Bảo's function work in its own reviewed PR as the plan requires, and do not count a skipped integration test as the final team gate.
- `shared/contract.py:17` — Praise: A shared Task class with importance and nonnegative-effort checks gives all planner functions the same data boundary. Huy's prioritise tests run against this class directly without importing a personal Task type or mutating inputs.
