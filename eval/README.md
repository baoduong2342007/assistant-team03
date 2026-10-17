# Evaluation Benchmark Dataset

This directory contains the benchmark questions and expected answers used to systematically evaluate the assistant's response quality and rule coverage.

## Dataset: `cases.csv`

The CSV file contains the following columns:
- `question`: The input query sent to the assistant.
- `expected`: The exact expected output string from `assistant.rules.reply()`.
- `category`: Classification of the test scenario (`office_lookup`, `greeting`, `out_of_scope`, `edge_case`).

## Summary of Test Cases

- **Total cases**: 19 questions (exceeds requirement of $\ge$ 15).
- **Categories covered**:
  - `office_lookup`: Valid campus offices (Training Office, Student Affairs, Library, IT Helpdesk) queried by full name and operational inquiries (location, hours).
  - `greeting`: Common greetings supported by the assistant (`hi`, `hello`, `xin chao`).
  - `out_of_scope`: Queries unrelated to campus offices or outside currently supported capabilities.
  - `edge_case`: Boundary and tricky inputs (whitespace-only, punctuation-only, case/punctuation mixes, partial name lookup).
- **Edge cases count**: 4 cases (exceeds requirement of $\ge$ 3).

## Current Behavior & Assumptions

1. **Current Rule Matching**: The baseline rule system in `src/assistant/rules.py` uses exact substring matching `if name in text:` against lowercase office names from `data/offices.csv`.
2. **Partial Name Matching**: A query like `"where is training"` does not match the full name `"training office"` in the current baseline implementation and therefore returns the fallback unknown message. Once Issue #8 (`Backend: office lookup handles partial names`) is implemented and merged, expected answers for partial matching edge cases can be updated accordingly.
3. **Empty / Punctuation Queries**: Stripped inputs containing only whitespace or punctuation resolve to `"Please type a question."`.
