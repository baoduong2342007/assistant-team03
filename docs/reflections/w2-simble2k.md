# Week 2 Reflection — Phạm Quốc Bảo (`simble2k`)

Sau khi tìm hiểu tài liệu hướng dẫn của GitHub về Pull Request Reviews và tài liệu nguyên lý Kanban, tôi ghi nhận các bài học và sự kiện thực tế trong tuần:

1. **Nhận xét review nhận được và điều chỉnh mã nguồn:**
   - Trên repository `assistant-team03`, tôi nhận được các nhận xét review rất chi tiết từ bạn Minh (`luongnhatinh`) trên hai PR cá nhân:
     - Tại [PR #20 review](https://github.com/baoduong2342007/assistant-team03/pull/20#pullrequestreview-5519827014), bạn Minh đã gợi ý mở rộng phương pháp đánh giá benchmark từ so khớp chuỗi tuyệt đối sang kiểm tra semantic / keyword coverage khi dự án tích hợp LLM ở Tuần 4, đồng thời góp ý về tính tương thích với PR #8 của bạn Huy và format chuỗi khoảng trắng trong `eval/cases.csv`. Tôi đã tiếp thu và phản hồi cam kết lộ trình cập nhật tại [comment phản hồi PR #20](https://github.com/baoduong2342007/assistant-team03/pull/20#issuecomment-6320722613).
     - Tại [PR #21 review](https://github.com/baoduong2342007/assistant-team03/pull/21#pullrequestreview-5519829372), bạn Minh chỉ ra sự cần thiết của việc kiểm tra wildcard `.env.*` trong suite kiểm thử bảo mật `tests/test_security.py` và đề xuất đưa Gitleaks vào CI pipeline. Tôi đã phản hồi giải pháp và thống nhất kế hoạch triển khai tại [comment phản hồi PR #21](https://github.com/baoduong2342007/assistant-team03/pull/21#issuecomment-6320735368).
   - Trong quá trình hoàn thiện các bài tập Tuần 1 và Tuần 2, tôi nhận thấy việc reviewer đặt câu hỏi và gợi ý giải pháp giúp tác giả nhìn nhận được các ca biên (edge cases) và rủi ro tiềm ẩn mà bản thân có thể bỏ sót khi viết code một mình.

2. **Bài học về Pull Request Reviews và Kanban:**
   - **PR Reviews:** Việc tuân thủ quy chuẩn phân loại nhận xét (`Praise`, `Question`, `Suggestion`, `Nit`) giúp việc giao tiếp kỹ thuật trở nên minh bạch, tôn trọng lẫn nhau và mang tính xây dựng cao. Điều này đặc biệt có ý nghĩa khi thực hiện các đợt review bổ sung để bù đắp các thiếu sót lịch sử (như các PR merge thiếu approval giai đoạn đầu).
   - **Kanban:** Việc trực quan hóa tiến độ trên board với các cột trạng thái rõ ràng (`Todo` → `In Progress` → `In Review` → `Done`) và thiết lập chính sách Definition of Done (DoD) nghiêm ngặt tại cửa ngõ `In Review` là chìa khóa để kiểm soát chất lượng, tránh tình trạng "Done ảo" và đảm bảo mọi tính năng trước khi vào nhánh `main` đều có đầy đủ test xanh và sự đồng thuận của đồng đội.
