# Data: Campus Offices Directory

Directory and working hours for University of Science (HCMUS) campus offices and departments.

## Metadata & Ownership

- **File**: `data/offices.csv`
- **Owner**: Phan Quỳnh Quyền ([@pqquyenn](https://github.com/pqquyenn) - `pqquyen040807@gmail.com`)
- **Component**: `data`
- **Last Verified**: `2026-10-17`
- **Update Frequency**: Once per semester, or whenever official university administrative announcements are published.

---

## Data Schema

The dataset `data/offices.csv` is structured as a comma-separated values (CSV) file with the following columns:

| Column | Type | Constraints | Description & Example |
| :--- | :--- | :--- | :--- |
| `name` | string | Required, unique | Full official name of the campus office (e.g., `Training Office`, `Health Clinic`). |
| `room` | string | Required | Room identifier following HCMUS campus building notation `<Building>.<RoomNumber>` (e.g., `I.101`, `F.01`, `C.12`). |
| `hours` | string | Required | Operating hours specifying active days and daily opening/closing times (e.g., `Mon-Fri 07:30-16:30`, `Mon-Sat 07:00-20:00`). |

---

## Sources & Coverage

All 10 office records are verified against public university portals, handbook announcements, and organizational directories (Campus 227 Nguyen Van Cu, District 5, HCMC):

| No. | Office Name | Room | Hours | Primary Source / URL |
| :-: | :--- | :--- | :--- | :--- |
| 1 | Training Office | I.101 | Mon-Fri 07:30-16:30 | [HCMUS Academic Affairs (Phòng Đào tạo)](https://ar.hcmus.edu.vn/) |
| 2 | Student Affairs | I.102 | Mon-Fri 08:00-16:00 | [HCMUS Student Affairs (Phòng CTSV)](https://ctsv.hcmus.edu.vn/) |
| 3 | Library | B.201 | Mon-Sat 07:00-20:00 | [HCMUS Library Portal](https://lib.hcmus.edu.vn/) |
| 4 | IT Helpdesk | E.005 | Mon-Fri 08:00-17:00 | [HCMUS Information Technology Center](https://itc.hcmus.edu.vn/) |
| 5 | Accounting Office | F.01 | Mon-Fri 07:30-16:30 | [HCMUS Functional Offices Directory](https://hcmus.edu.vn/gioi-thieu/so-do-to-chuc/cac-phong-chuc-nang/) |
| 6 | Science and Technology Office | F.03 | Mon-Fri 08:00-16:30 | [HCMUS Office of Science & Technology](https://hcmus.edu.vn/gioi-thieu/so-do-to-chuc/cac-phong-chuc-nang/) |
| 7 | International Relations Office | F.04 | Mon-Fri 08:00-16:30 | [HCMUS International Relations Office](https://iro.hcmus.edu.vn/) |
| 8 | Testing and Quality Assurance | B.02 | Mon-Fri 08:00-16:30 | [HCMUS Office of Testing & Quality Assurance](https://hcmus.edu.vn/gioi-thieu/so-do-to-chuc/cac-phong-chuc-nang/) |
| 9 | Youth Union Office | B.03 | Mon-Fri 08:00-17:00 | [HCMUS Youth Union - Student Association Portal](https://doanhoi.hcmus.edu.vn/) |
| 10 | Health Clinic | C.12 | Mon-Fri 07:30-16:30 | [HCMUS Campus Medical Station](https://hcmus.edu.vn/gioi-thieu/so-do-to-chuc/cac-phong-chuc-nang/) |

*Note*: If an office's details (such as temporary relocation or altered schedule) cannot be verified through official public announcements, it must be excluded or explicitly flagged until confirmed.

---

## Contributor Guide: Updating & Validating the Dataset

When adding or modifying office data:

1. **Verify Source First**:
   - Ensure every new office or schedule change is confirmed by a public university link or handbook document. Record the URL in this documentation.
2. **Maintain Strict CSV Formatting**:
   - Save file with `UTF-8` encoding and standard line endings (`\n` or `\r\n`).
   - Keep header as `name,room,hours`.
   - Do not leave empty rows or trailing whitespace around commas.
3. **Run Automated Verification**:
   - Execute the test suite to ensure the assistant's lookup rules and smoke tests pass:
     ```bash
     pytest tests/test_office_lookup.py
     ```
   - Add new lookup tests in `tests/test_office_lookup.py` covering the added entries (both full name and partial query).
