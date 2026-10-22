# Submitted replies to Dương on PR #17

Prepared with Codex assistance and posted after Huy's explicit authorization. The code/test follow-up is in
commit `6c7fd12` on `feat/w2-huy-evidence` and needs its own peer review/merge.

## Reply to Question — discussion_r4334610655

Returning the only office matching a generic token is intentional. After the
directory grows, the same token must return a sorted clarification list when
there are multiple candidates. The follow-up README documents both cases,
and commit 6c7fd12 verifies single/multiple candidates and reversed input order.
This is a post-merge follow-up; it does not change the historical approval record.
Codex assisted the test/documentation change and this reply.

## Reply to Suggestion — discussion_r4334610672

Added five regression cases: `office` and `where is office` with two matching
offices in each input order, plus the single-candidate case. The tests assert
the complete sorted clarification string. Full local suite: 56 passed on
Python 3.14. These changes remain on a separate follow-up branch pending review.
Codex assisted implementation and verification.

## Posted reply links

- [Reply to 4334610655](https://github.com/baoduong2342007/assistant-team03/pull/17#discussion_r4338354156)
- [Reply to 4334610672](https://github.com/baoduong2342007/assistant-team03/pull/17#discussion_r4338354458)
