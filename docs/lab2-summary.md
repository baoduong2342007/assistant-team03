# Hồ Sơ Tổng Hợp Minh Chứng Homework Lab 2 — Team 03

- **Repository:** [baoduong2342007/assistant-team03](https://github.com/baoduong2342007/assistant-team03)
- **Thành viên nhóm:**
  1. Trương Quang Bảo Dương (`baoduong2342007`) — Team Lead / Release Manager
  2. Trần Quốc Huy (`tqhuy0602`) — Backend & Process Lead
  3. Phan Quỳnh Quyền (`pqquyenn`) — CI & Data Lead
  4. Lương Nhật Minh (`luongnhatinh`) — UI & Workflow Lead
  5. Phạm Quốc Bảo (`simble2k`) — QA & Evidence Lead (Người tổng hợp hồ sơ)
- **Ngày lập:** 2026-10-21
- **Căn cứ tài liệu:** `CSC10014_2026_Lab02_Git_Collaboration_Tutorial.pdf` và kế hoạch chi tiết `plan (3).md`.

## Cập nhật kiểm chứng — 2026-10-22

Phần bên dưới giữ snapshot ngày 2026-10-21. Bảng này thay thế các trạng thái
review/merge/conflict cũ tương ứng; không xác nhận hoàn tất toàn bộ Lab 2.

| Issue | Trạng thái được kiểm chứng | Bằng chứng hoặc phần còn thiếu |
| --- | --- | --- |
| [#24](https://github.com/baoduong2342007/assistant-team03/issues/24) | Còn mở | PR #25 đã approve, CI xanh và merge. Giảng viên chưa xác nhận cách khắc phục approval lịch sử (chủ repo xác nhận ngày 2026-10-22); tác giả #22 chưa phản hồi review bổ sung. |
| [#26](https://github.com/baoduong2342007/assistant-team03/issues/26) | Đủ tiêu chí kỹ thuật và review để đóng | PR #27/#29 đã approve trước merge; conflict thật, commit giải quyết hai parent và lịch sử merge được giữ. Xem [minh chứng conflict](conflicts/duong-huy-tqhuy0602.md). |
| [#28](https://github.com/baoduong2342007/assistant-team03/issues/28) | Các deliverable của issue đã hoàn tất | PR #30 đã approve, CI xanh và merge. Tests/docs, review/reply và file reflection đã có; chưa suy ra Huy đã đọc reflection hoặc ký đồng thuận cá nhân. |

`main` tại `f60f4a7` đạt 56/56 tests cục bộ trên Python 3.13.14 và
[CI xanh](https://github.com/baoduong2342007/assistant-team03/actions/runs/39715259528).
DoD đã có approval thật của Dương/Bảo; Huy/Quyền vẫn Pending.
Bài AI-PR của giảng viên và các xác nhận cá nhân còn thiếu tiếp tục được theo dõi.
Trạng thái board riêng tư chưa được kiểm chứng/cập nhật trong lần này; người có
quyền board cần đồng bộ các thẻ #26/#28 sau khi issue được đóng.

Codex hỗ trợ kiểm tra bằng chứng và cập nhật tài liệu/trạng thái GitHub theo yêu
cầu của chủ repo. Không thay đổi ngày review hoặc tạo chữ ký thay thành viên.

---

## 1. H1 — Bảng tổng hợp 10 Issue và PR cá nhân

Toàn bộ 10 Issue và các PR tính năng/công việc tương ứng của 5 thành viên đã được hợp nhất vào nhánh `main`:

| Thành viên | Issue | Nhiệm vụ chính | Pull Request | Trạng thái PR | Trạng thái Issue |
|---|---|---|---|---|---|
| **Dương** | [#6](https://github.com/baoduong2342007/assistant-team03/issues/6), [#7](https://github.com/baoduong2342007/assistant-team03/issues/7) | Workflow GitHub Actions & Hướng dẫn đóng góp | [PR #16](https://github.com/baoduong2342007/assistant-team03/pull/16) | Merged (`0b6e484`) | Closed |
| **Huy** | [#8](https://github.com/baoduong2342007/assistant-team03/issues/8), [#9](https://github.com/baoduong2342007/assistant-team03/issues/9) | Backend: tra cứu phòng ban rút gọn & lệnh `help` | [PR #17](https://github.com/baoduong2342007/assistant-team03/pull/17) | Merged (`266e46b`) | Closed |
| **Quyền** | [#10](https://github.com/baoduong2342007/assistant-team03/issues/10), [#11](https://github.com/baoduong2342007/assistant-team03/issues/11) | Data: bổ sung 6 cơ sở kèm tài liệu nguồn | [PR #22](https://github.com/baoduong2342007/assistant-team03/pull/22) | Merged (`3046e16`) | Closed |
| **Minh** | [#12](https://github.com/baoduong2342007/assistant-team03/issues/12), [#13](https://github.com/baoduong2342007/assistant-team03/issues/13) | Definition of Done & Thiết kế giao diện Chat UI | [PR #18](https://github.com/baoduong2342007/assistant-team03/pull/18), [PR #19](https://github.com/baoduong2342007/assistant-team03/pull/19) | Merged (`ce03d7d`, `406b29f`) | Closed |
| **Bảo** | [#14](https://github.com/baoduong2342007/assistant-team03/issues/14), [#15](https://github.com/baoduong2342007/assistant-team03/issues/15) | 19 câu benchmark test & Kiểm tra an toàn bảo mật | [PR #20](https://github.com/baoduong2342007/assistant-team03/pull/20), [PR #21](https://github.com/baoduong2342007/assistant-team03/pull/21) | Merged (`e52f311`, `14cba4d`) | Closed |

> **Ghi nhận lịch sử trung thực về điều kiện Done:**
> - Các PR ban đầu [#16](https://github.com/baoduong2342007/assistant-team03/pull/16), [#19](https://github.com/baoduong2342007/assistant-team03/pull/19), và [#22](https://github.com/baoduong2342007/assistant-team03/pull/22) đã được merge mà thiếu teammate approval trước merge; PR [#18](https://github.com/baoduong2342007/assistant-team03/pull/18) mới chỉ có duy nhất approval của Dương.
> - Các checklist acceptance criteria của Issue #14 và #15 đã được Bảo đối chiếu và cập nhật đầy đủ dấu tick `[x]` kèm ghi chú hoàn tất vào ngày 2026-10-21.

---

## 2. H2 — Review bổ sung và phản hồi sau merge

Để khắc phục thiếu sót lịch sử và đảm bảo 100% PR đều được review chéo kỹ lưỡng theo `REVIEW_CHECKLIST.md` (bao gồm đủ các phần `Question`, `Suggestion`, `Praise`), nhóm đã thực hiện vòng review bổ sung có ghi rõ ngày thực tế:

| Thành viên review | PR được phân công | Nội dung chính | Trạng thái & Link Review | Phản hồi của tác giả |
|---|---|---|---|---|
| **Dương** | PR #17, PR #22 | Review chi tiết logic backend và tính toàn vẹn dữ liệu | [Review #17](https://github.com/baoduong2342007/assistant-team03/pull/17#pullrequestreview-5596909986), [Review #22](https://github.com/baoduong2342007/assistant-team03/pull/22#pullrequestreview-5596910478) (2026-10-21) | Huy đã phản hồi và bổ sung test regression tại PR #30 |
| **Huy** | PR #22, PR #19 | Đọc diff UI và tests dữ liệu | [Review #22](https://github.com/baoduong2342007/assistant-team03/pull/22#pullrequestreview-5596910700), [Review #19](https://github.com/baoduong2342007/assistant-team03/pull/19#pullrequestreview-5596910800) (2026-10-21, ghi nhận tại PR #30) | Đã ghi nhận |
| **Quyền** | PR #19, PR #21 | Review thiết kế UI và an toàn bảo mật | Đang tổng hợp review | Đang chờ |
| **Minh** | PR #20, PR #21 | Đã review chi tiết bộ benchmark và báo cáo an toàn repo | [Review #20](https://github.com/baoduong2342007/assistant-team03/pull/20#pullrequestreview-5519827014), [Review #21](https://github.com/baoduong2342007/assistant-team03/pull/21#pullrequestreview-5519829372) (2026-10-17) | Bảo đã phản hồi đầy đủ tại [Comment #20](https://github.com/baoduong2342007/assistant-team03/pull/20#issuecomment-6320722613) và [Comment #21](https://github.com/baoduong2342007/assistant-team03/pull/21#issuecomment-6320735368) |
| **Bảo** | PR #16, PR #18 | Review cấu hình CI/workflow và quy chuẩn DoD | [Review #16](https://github.com/baoduong2342007/assistant-team03/pull/16#pullrequestreview-5596911500), [Review #18](https://github.com/baoduong2342007/assistant-team03/pull/18#pullrequestreview-5596912000) (2026-10-21) | Dương và Minh tiếp thu cho các cập nhật tiếp theo |

---

## 3. H3 — Quản lý chất lượng: Bảo vệ nhánh, Kanban Board & Đồng thuận DoD

1. **Bảo vệ nhánh `main` (Branch Protection):**
   - Đã được kích hoạt và kiểm chứng trên GitHub bởi bạn Dương vào ngày 2026-10-21.
   - Các quy tắc áp dụng:
     - Bắt buộc phải có Pull Request trước khi merge; yêu cầu tối thiểu 1 approval từ đồng đội.
     - Bắt buộc kiểm thử tự động xanh (passing) cả 2 jobs: `Python 3.10` và `Python 3.12`.
     - Nhánh phải up-to-date với `main` trước khi merge.
     - Chặn quyền Force Push và chặn xóa nhánh `main` (áp dụng cho cả Administrators).

2. **Bảng Kanban Board:**
   - Đã được thiết lập công khai cho nhóm tại: [https://github.com/users/tqhuy0602/projects/2](https://github.com/users/tqhuy0602/projects/2).
   - Cấu trúc gồm 4 cột trạng thái chuẩn: `Todo` → `In progress` → `In review` → `Done`.
   - Mỗi thẻ bài tập đều có đầy đủ thông tin phân loại `Component` và ước lượng `Size`.
   - Cả 5 thành viên (`baoduong2342007`, `tqhuy0602`, `pqquyenn`, `luongnhatinh`, `simble2k`) đều đã được cấp quyền `WRITER` để đồng bộ cập nhật thẻ.

3. **Đồng thuận Definition of Done (DoD Consensus):**
   - Minh đã mở [PR #23](https://github.com/baoduong2342007/assistant-team03/pull/23) để bổ sung 5 bước kiểm tra chất lượng chặt chẽ (**Done Verification Procedure**) và bảng ký nhận đồng thuận chính thức của cả 5 thành viên.
   - Minh (tác giả) đã ký nhận.
   - Bảo (`simble2k`) đã đọc, kiểm thử cục bộ 51 tests pass và chính thức gửi [Approval](https://github.com/baoduong2342007/assistant-team03/pull/23#pullrequestreview-5596912500) vào ngày 2026-10-21.
   - Các thành viên còn lại đang cập nhật ngày ký duyệt thực tế theo yêu cầu review của trưởng nhóm.

---

## 4. Thực hành nâng cao (Minh chứng đang hoàn thiện)

1. **Bài tập xử lý Git Conflict có kiểm soát:**
   - Phân chia thực hiện theo 3 cặp để bảo đảm cả 5 thành viên đều nắm vững kỹ năng giải quyết conflict:
     - **Cặp 1 (Dương – Huy):** Đã tạo nhánh từ cùng commit gốc `3620a55`. Dương mở [PR #27](https://github.com/baoduong2342007/assistant-team03/pull/27), Huy mở [PR #29](https://github.com/baoduong2342007/assistant-team03/pull/29) cùng sửa đổi câu chào greeting. Đang tiến hành merge PR của Dương trước, sau đó Huy merge `origin/main`, giải quyết xung đột thật, chạy tests và giữ merge commit minh chứng.
     - **Cặp 2 (Quyền – Minh):** Thực hiện nối tiếp theo lịch trình.
     - **Cặp 3 (Bảo – Dương):** Thực hiện nối tiếp ngay sau khi các cặp trước hoàn tất.
   - *Trạng thái:* `In Progress (đang thực hiện theo tiến độ từng cặp)`.

2. **Bài tập Review AI-generated PR (Cài sẵn 3 lỗi):**
   - Hiện tại chưa xác định được PR bài tập chính thức có 3 lỗi cài sẵn từ Giảng viên trên repo hay LMS.
   - Bạn Quyền đang làm đầu mối liên hệ với Giảng viên để nhận đúng link bài tập.
   - Khi có link, toàn bộ 5 thành viên sẽ cùng tham gia review, chỉ ra 3 lỗi và đề xuất cách sửa tối thiểu mà không merge PR.
   - *Trạng thái:* `Pending confirmation từ Giảng viên`.

---

## 5. H4 — Tổng hợp Báo Cáo Thu Hoạch (Reflections)

Toàn bộ các thành viên đều đã và đang hoàn thiện bài thu hoạch cá nhân (5–10 dòng), ghi nhận trung thực các nhận xét review nhận được, mã nguồn đã sửa đổi kèm link minh chứng và bài học về quy trình Kanban:

| Thành viên | File Reflection | Vị trí lưu trữ / PR liên quan | Trạng thái |
|---|---|---|---|
| **Dương** | `docs/w2-baoduong2342007-evidence.md` | [PR #25](https://github.com/baoduong2342007/assistant-team03/pull/25) | Đã hoàn thành bản draft minh chứng |
| **Huy** | `docs/reflections/w2-tqhuy0602.md` | [PR #30](https://github.com/baoduong2342007/assistant-team03/pull/30) | Đã hoàn thành bản draft minh chứng |
| **Quyền** | `docs/reflections/w2-pqquyenn.md` | Repo `assistant-team03` | Đang hoàn thiện |
| **Minh** | `docs/reflections/w2-luongnhatinh.md` | [PR #23](https://github.com/baoduong2342007/assistant-team03/pull/23) | Đã hoàn thành |
| **Bảo** | [`docs/reflections/w2-simble2k.md`](reflections/w2-simble2k.md) | Repo `assistant-team03` | Đã hoàn thành |

---

## 6. Các hạng mục Pending & Kiến nghị Giảng viên

Để đảm bảo tính trung thực học thuật cao nhất theo đúng tinh thần kế hoạch:
1. **Thiếu sót approval lịch sử:** Nhóm ghi nhận trung thực việc các PR #16, #19, #22 merge trước khi có approval. Việc khắc phục bằng các review chi tiết có ngày thực tế sau merge đang chờ ý kiến xác nhận của Giảng viên về cách chấm điểm.
2. **Đề bài AI-PR:** Đang chờ link bài tập chính thức từ Giảng viên để thực hiện phân tích 3 lỗi cài sẵn.
