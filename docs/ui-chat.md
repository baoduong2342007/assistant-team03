# UI: Chat Page Design & Interaction States

- **Document:** Chat UI Specification & Mockups
- **Author:** Lương Nhật Minh (`@luongnhatinh`) — UI & Workflow
- **Related Issue:** Closes #12
- **Component:** UI
- **Status:** In Review

---

## 1. Overview & Objectives

This document specifies the design, layout, interaction states, and backend integration model for the Student Assistant chat interface.

### Key Requirements Addressed:
1. **Mockup:** Clear layout featuring a question input area and answer/chat history container.
2. **Backend Communication:** Explicit contract showing how user input is passed to `src/assistant/rules.py:reply()` and rendered.
3. **State Management:** Formal specifications for **Empty**, **Waiting**, **Success**, and **Error** states.
4. **Source Attribution (Bonus):** Visual representation of data sources and verification metadata with concrete examples.
5. **Framework Independence:** This specification does not mandate or introduce any external UI framework (e.g. Streamlit or Gradio) at this stage; it provides the implementation blueprint for Week 7.

---

## 2. Page Layout & Mockup

### 2.1 Wireframe Diagram

```text
+-----------------------------------------------------------------------------------+
|  HCMUS Campus Assistant                                   [● Backend: Online]     |
|  Ask questions about campus offices, locations, and operating hours               |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  [🤖 Assistant]                                                                  |
|  +-----------------------------------------------------------------------------+  |
|  | Hello! Ask me where an office is, or when it opens.                        |  |
|  +-----------------------------------------------------------------------------+  |
|                                                                                   |
|                                                                    [👤 Student]   |
|                                 +----------------------------------------------+  |
|                                 | Where is the Training Office?                |  |
|                                 +----------------------------------------------+  |
|                                                                                   |
|  [🤖 Assistant]                                                                  |
|  +-----------------------------------------------------------------------------+  |
|  | Training Office: room I.101, open 07:30 - 16:30.                            |  |
|  |                                                                             |  |
|  | ℹ️ Source: data/offices.csv | Verified: 2026-10-12                          |  |
|  +-----------------------------------------------------------------------------+  |
|                                                                                   |
+-----------------------------------------------------------------------------------+
| [ Type your question here (e.g. 'training office', 'student affairs')... ] [Send] |
+-----------------------------------------------------------------------------------+
```

### 2.2 Component Hierarchy
1. **Header Bar:**
   - Application title and brief guidance.
   - Status badge indicating backend connectivity status (`Online` / `Offline`).
2. **Conversation Stream (Message Area):**
   - Scrollable message history with auto-scroll to latest message.
   - Distinct message cards:
     - **User query bubble:** Right-aligned, primary accent background.
     - **Assistant reply bubble:** Left-aligned, neutral background with assistant avatar.
     - **Source Footnote:** Subtle badge or caption area under the message displaying origin file and verification date.
3. **Action & Input Area:**
   - Multi-line / auto-expanding text input box.
   - Submit button ("Send") triggered by click or keyboard shortcut (`Enter`).

---

## 3. Interaction States

### 3.1 Initial / Empty State (Zero State)
* **Trigger:** Initial page load or after clearing conversation history.
* **Display:**
  - Welcome greeting message displayed from assistant.
  - Quick-prompt suggestions (clickable chips) to help new students start quickly.
  - Input box is empty and automatically focused.

```text
+-----------------------------------------------------------------------------------+
| [🤖 Assistant]                                                                    |
| Welcome to the HCMUS Campus Assistant!                                           |
|                                                                                   |
| Suggestions:                                                                      |
|  [ 💬 Where is the Training Office? ]  [ 💬 Student Affairs hours ]  [ 💬 help ]  |
+-----------------------------------------------------------------------------------+
| [ Ask where an office is...                                              ] [Send] |
+-----------------------------------------------------------------------------------+
```

---

### 3.2 Waiting / In-Flight State
* **Trigger:** User clicks "Send" or presses `Enter`.
* **Behavior:**
  - User's message is immediately appended to the chat stream.
  - Input field is disabled or cleared; "Send" button is disabled to prevent duplicate submissions.
  - Assistant message area displays an animated typing indicator (`• • •` or `"Searching campus records..."`).

```text
+-----------------------------------------------------------------------------------+
|                                                                    [👤 Student]   |
|                                                Where is the International Office? |
|                                                                                   |
| [🤖 Assistant]                                                                    |
| ⌛ Assistant is checking office directory... • • •                                |
+-----------------------------------------------------------------------------------+
| [ Waiting for response...                                     (Disabled) ] [Send] |
+-----------------------------------------------------------------------------------+
```

---

### 3.3 Success / Answer State
* **Trigger:** Backend returns a valid response string.
* **Behavior:**
  - Typing indicator is replaced by the response text.
  - Source attribution tag is attached at the bottom of the assistant card.
  - Input field is re-enabled, cleared, and focused for the next question.

```text
+-----------------------------------------------------------------------------------+
| [🤖 Assistant]                                                                    |
| International Office: room B.12, open 08:00 - 16:30.                             |
| ───────────────────────────────────────────────────────────────────────────────── |
| 📁 Source: data/offices.csv | Verified: 2026-10-12                                |
+-----------------------------------------------------------------------------------+
| [ Ask where an office is...                                              ] [Send] |
+-----------------------------------------------------------------------------------+
```

---

### 3.4 Error & Edge States

#### State 3.4.a: Empty Query Submission
* **Trigger:** User sends whitespace or empty text without typing.
* **Backend output:** `"Please type a question."`
* **UI Handling:**
  - UI can perform pre-validation (disable Send if empty) or display inline feedback directly from backend output.
  - Input border momentarily highlights in amber warning border.

```text
+-----------------------------------------------------------------------------------+
| [🤖 Assistant]                                                                    |
| ⚠️ Please type a question. (Try asking e.g. "Where is room I.101?")               |
+-----------------------------------------------------------------------------------+
| [                                                                (Warning) ] [Send] |
+-----------------------------------------------------------------------------------+
```

#### State 3.4.b: Unknown Office Query (Fallback)
* **Trigger:** User asks a query not present in the office directory.
* **Backend output:** `"I don't know that yet. Try asking about an office, e.g. 'training office'."`
* **UI Handling:**
  - Rendered as an informational assistant bubble with suggested keywords.

```text
+-----------------------------------------------------------------------------------+
|                                                                    [👤 Student]   |
|                                                        Where is the cinema room?  |
|                                                                                   |
| [🤖 Assistant]                                                                    |
| I don't know that yet. Try asking about an office, e.g. 'training office'.        |
+-----------------------------------------------------------------------------------+
```

#### State 3.4.c: System / Backend Execution Error
* **Trigger:** Backend function throws an unhandled exception or connection fails.
* **UI Handling:**
  - Displays a dedicated error toast/banner: `"Unable to reach the assistant service."`
  - Provides a `[Retry]` button next to the failed query.

```text
+-----------------------------------------------------------------------------------+
| [⚠️ Connection Error]                                                             |
| Unable to process request. Please check your connection or retry.   [ 🔄 Retry ]  |
+-----------------------------------------------------------------------------------+
```

---

## 4. Source & Citation Display (Backlog Extension)

To ensure transparency and traceability for campus information, responses display metadata extracted from the dataset:

### Example 1: Office Location Query
* **User Question:** *"Where is the Training Office?"*
* **Response Content:**
  > **Training Office**: room I.101, open 07:30 - 16:30.
* **Citation Metadata Footnote:**
  > `Source: data/offices.csv (Row 1: Training Office) | Verified: 2026-10-12 | Owner: Academic Affairs`

### Example 2: General Operating Hours Query
* **User Question:** *"When is Student Affairs open?"*
* **Response Content:**
  > **Student Affairs**: room A.02, open 08:00 - 17:00.
* **Citation Metadata Footnote:**
  > `Source: data/offices.csv (Row 3: Student Affairs) | Verified: 2026-10-12 | Owner: Student Affairs Office`

---

## 5. Backend Integration Model

The UI interacts with the assistant backend via a simple request-reply invocation contract:

```
[ User Action ] ───> [ UI Event Handler ]
                             │
                             ▼
                     [ Normalize Input ]
                             │
                             ▼
                  [ assistant.rules.reply(text) ]
                             │
                             ▼
             [ Format Response + Source Footnote ]
                             │
                             ▼
                 [ Render in Message Stream ]
```

### Direct Python Invocation
When using a Python-based UI framework (such as Streamlit or Gradio), the UI directly imports the rules engine:

```python
from assistant.rules import reply

def on_send_message(user_message: str) -> str:
    # 1. Clean message
    text = user_message.strip()
    # 2. Invoke backend logic
    response = reply(text)
    # 3. Return formatted answer
    return response
```

### CLI / Process Interface Fallback
If integrated into external client applications:
```bash
python -m assistant "Where is the Training Office?"
```

---

## 6. Verification and Display Checklist

To ensure documentation quality and mockup clarity:
- [x] **Markdown Formatting:** Renders cleanly in GitHub Web View, VSCode Markdown preview, and CLI pagers.
- [x] **Completeness:** Covers question input, answer view, empty state, waiting state, and error states.
- [x] **Backend Alignment:** All sample responses match the exact outputs from `src/assistant/rules.py` and `tests/test_smoke.py`.
- [x] **No Framework Dependency:** No external dependencies added to `pyproject.toml` or `requirements.txt`.
