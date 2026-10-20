# Week 2 Reflection — Lương Nhật Minh (`luongnhatinh`)

Sau khi đọc tài liệu của GitHub về Pull Request Reviews và tài liệu nhập môn Kanban, tôi ghi nhận các bài học và sự kiện thực tế trong tuần:

1. **Nhận xét review nhận được và điều chỉnh mã nguồn:**
   - Trong quá trình hoàn thiện bài tập Python Tuần 1, bạn Bảo (`simble2k`) đã rà soát và chỉ ra rằng hàm tách từ `word_count` trong `members/luongnhatinh/w1/text_tools.py` cần coi các dấu câu (`.,!?;:`) là ranh giới tách từ (ví dụ `"hello,world"` phải tách thành `"hello"` và `"world"` thay vì coi là một từ dính liền).
   - Tôi đã tiếp thu nhận xét, điều chỉnh hàm tách từ bằng cách chuẩn hóa các ký tự phân cách trước khi split, và kiểm tra lại toàn bộ qua suite kiểm thử hồi quy `tests/test_w1_tokenization.py`. Thay đổi này đã được đưa vào commit [`2ca353f`](https://github.com/baoduong2342007/python-bootcamp/commit/2ca353f7f0cf520ea85e332bbdca1b09b0bfa2df) (PR [#28](https://github.com/baoduong2342007/python-bootcamp/pull/28)), giúp toàn bộ tests của 5 thành viên đều đạt chuẩn hành vi chung.
   - Trước đó, trong đợt rà soát mã nguồn theo linter Ruff, tôi cũng đã tiếp thu góp ý về cảnh báo `PLC0206` (lặp qua keys của dictionary rồi tra cứu giá trị) trong `members/luongnhatinh/w1/timetable.py` và sửa thành lặp trực tiếp qua `b.values()` tại commit [`d345bcc`](https://github.com/baoduong2342007/python-bootcamp/commit/d345bcc1fa385a8a1eb29ee0682255716182c40c) (PR [#20](https://github.com/baoduong2342007/python-bootcamp/pull/20)).

2. **Bài học về Pull Request Reviews và Kanban:**
   - Việc phân loại nhận xét theo các nhãn `Blocking:`, `Question:`, `Suggestion:`, `Nit:`, `Praise:` giúp tác giả phân biệt rõ việc bắt buộc sửa trước khi merge và các ý tưởng cải tiến trong tương lai.
   - Áp dụng Kanban với cột `In review` rõ ràng và chính sách Definition of Done (DoD) minh bạch ngăn chặn việc merge vội vã khi chưa có approval (như các PR #16, #19, #22 giai đoạn đầu), đồng thời giới hạn WIP giúp nhóm tập trung giải quyết dứt điểm từng task trước khi mở task mới.
