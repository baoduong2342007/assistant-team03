# AI vs. Rule-Based Query Classification

**Author / Owner:** Phan Quỳnh Quyền ([@pqquyenn](https://github.com/pqquyenn))  
**Role:** Data & Product Lead  
**Component:** `data`, `ai`  
**Date:** 2026-10-17  

---

## 1. Overview & Motivation

In the campus assistant architecture, deciding which queries should be handled deterministically by **Rules / Database Lookup** and which require a **Large Language Model (AI)** is crucial. 
- **Rule-based systems** provide zero hallucination, zero API latency/cost, and 100% deterministic precision for factual directory lookups.
- **AI / LLM systems** excel at semantic ambiguity, synthesis across multiple facts, multi-lingual flexibility, counseling, and conversational guidance.

---

## 2. Table: 10 Queries That Do NOT Need AI (Rule / Lookup Based)

| No. | User Query | Handling Method | Rationale |
| :-: | :--- | :--- | :--- |
| 1 | "Where is the Training Office?" | Direct Key Lookup (`data/offices.csv`) | Pure factual location mapping. Exact/partial keyword match guarantees 100% accurate room number without hallucination risk. |
| 2 | "When does the library open on Saturday?" | Structured Filter (`hours` field) | Specific operating schedule lookup from verified calendar data. |
| 3 | "Where can I find Student Affairs?" | Synonym / Alias Matching | Maps canonical name `Student Affairs` to room `I.102`. Low latency, zero LLM compute cost. |
| 4 | "IT Helpdesk room number" | Attribute Extraction | Single field retrieval (`room` = `E.005`). |
| 5 | "What time does the Health Clinic close on Friday?" | Temporal Parse + Schedule Query | Factual check against `Mon-Fri 07:30-16:30`. |
| 6 | "Hello" / "Xin chao" / "Hey" | Regex / Set Membership | Standard greeting template response. Calling an LLM is wasteful and slow. |
| 7 | "help" | Hardcoded Command / Menu | Static instruction guide listing supported queries and command syntax. |
| 8 | "Is Accounting Office in building F?" | Boolean Directory Check | True/False check on room prefix `F.01`. |
| 9 | "List all departments in building B" | Database Grouping / Filter | Deterministic SQL/CSV filter: returns `Library (B.201)`, `Testing (B.02)`, `Youth Union (B.03)`. |
| 10 | "   " / "????!!!!" | String Normalization & Guardrail | Empty or punctuation-only input caught immediately by input sanitization guardrails. |

---

## 3. Table: 10 Queries That DO Need AI (LLM / Generative Reasoning)

| No. | User Query | LLM Capability Needed | Rationale |
| :-: | :--- | :--- | :--- |
| 1 | "I lost my student ID card this morning, what step-by-step procedures should I follow?" | Multi-step procedural synthesis | Requires synthesizing regulations across Student Affairs, Security, and Library, then generating a step-by-step checklist. |
| 2 | "Can you explain why my tuition fee hasn't updated after online payment?" | Semantic understanding & troubleshooting | Involves contextual banking delay explanations, verification windows, and advising when to contact the Accounting Office. |
| 3 | "I have a sudden panic attack before my calculus exam, who can I talk to on campus?" | Empathy, sentiment analysis & triage | Requires empathetic tone, identifying emotional distress, and directing to counseling / health clinic with high sensitivity. |
| 4 | "What is the difference between General Education and Specialized Electives in my credit curriculum?" | Academic advising & conceptual explanation | Needs natural language explanation of educational terminology and credit prerequisites tailored to the student's cohort. |
| 5 | "Summarize the major scientific research grants announced by HCMUS this semester." | Unstructured document summarization | Extracts key takeaways from lengthy official PDF documents and announcements. |
| 6 | "Recommend 3 student clubs for a first-year student interested in robotics and open source." | Multi-criteria recommendation | Understands student preferences and matches them with relevant club descriptions from the Youth Union. |
| 7 | "If I fail the prerequisite Math 1 course, will my subsequent Physics 1 registration be canceled?" | Complex conditional reasoning | Analyzes interconnected academic policy rules and edge-case exceptions. |
| 8 | "Could you draft a polite formal email to the Training Office requesting an official academic transcript in English?" | Language generation & formal tone transfer | Drafts an email template with appropriate salutation, student ID placeholders, and polite request phrasing. |
| 9 | "How do I appeal my midterm grade if the lecturer hasn't responded for 2 weeks?" | Policy interpretation & protocol routing | Interprets academic appeal escalation paths without a rigid keyword match. |
| 10 | "Help me plan a weekly study schedule balancing 18 credits and part-time lab research." | Personalized planning & scheduling optimization | Custom constraint satisfaction and time-management advice based on user-provided constraints. |

---

## 4. Conclusion & Recommended Architecture

For Lab 3 & Lab 4, the assistant should implement a **Router Pattern**:
1. Incoming queries are first sanitized and matched against deterministic rules (`rules.py`).
2. If the intent is directory lookup or commands, it is resolved instantly via `offices.csv`.
3. If unmatched or classified as complex/advisory, it routes to the LLM agent pipeline with appropriate context grounding (RAG).
