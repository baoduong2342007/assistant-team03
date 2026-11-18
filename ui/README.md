# Bếp Nhà Mình / Mâm Cơm Nhà Mình — Frontend UI/UX

Trợ lý bữa ăn và dinh dưỡng gia đình Việt thông minh: đề xuất mâm cơm hợp bệnh lý từng thành viên, cân đối dinh dưỡng và timeline nấu nướng tính ngược.

- **Thành viên phụ trách:** Phan Quỳnh Quyền (`@pqquyenn` / `PQQuyen`)
- **Vai trò:** UI/UX Designer & Frontend Engineer chính
- **Mã nguồn gốc xây dựng trên Google AI Studio:** [Xem app trên AI Studio](https://ai.studio/apps/30c35877-a04c-4986-bb90-c26a9223d411)

---

## 1. Giới thiệu & Tính năng chính

Giao diện được thiết kế tối ưu hóa cho trải nghiệm di động (PWA-ready), thao tác một tay thuận tiện khi đang nấu nướng:

1. **Floating Navigation & Hero Landing:**
   - Điều hướng trực quan giữa các trạng thái của ứng dụng.
   - Giới thiệu giải pháp dinh dưỡng cho người nội trợ hiện đại.
2. **Onboarding Flow (Bộ 10 câu hỏi hồ sơ gia đình):**
   - Thu thập thông tin thành viên: độ tuổi, bệnh lý (huyết áp cao, tiểu đường, gout, mỡ máu...), tiền sử dị ứng thực phẩm, khẩu vị vùng miền và ngân sách bữa ăn.
   - Lưu trữ hồ sơ gia đình làm dữ liệu cơ sở cho AI tư vấn.
3. **Chatbot Interface & Nhập liệu đa phương thức:**
   - Nhập yêu cầu món ăn bằng văn bản hoặc nút thu âm giọng nói (Voice input).
   - Đề xuất câu hỏi nhanh theo từng ngữ cảnh.
4. **Mâm cơm & Phân tích dinh dưỡng:**
   - Đề xuất mâm cơm chuẩn Việt (Món mặn, Canh, Món xào/rau) phù hợp với từng bệnh lý của các thành viên.
   - Tính năng **Đổi món** linh hoạt.
   - Khung lý do y khoa và cảnh báo an toàn sức khỏe theo tài liệu Viện Dinh Dưỡng.
5. **Timeline nấu nướng thông minh:**
   - Thuật toán tính ngược mốc thời gian từ giờ ăn của gia đình (Rã đông -> Sơ chế -> Nấu -> Dọn mâm).
   - Checkbox đánh dấu hoàn thành từng bước trực quan.

---

## 2. Công nghệ sử dụng (Tech Stack)

- **Framework:** React 19, TypeScript
- **Styling:** Tailwind CSS v4, Motion (Animation)
- **Icons:** Lucide React
- **Bundler:** Vite
- **AI SDK:** `@google/genai`

---

## 3. Hướng dẫn chạy thử nghiệm cục bộ (Run Locally)

### Yêu cầu tiên quyết:
- [Node.js](https://nodejs.org/) (phiên bản 18+ hoặc 20+)

### Các bước cài đặt:
1. Di chuyển vào thư mục giao diện:
   ```bash
   cd ui
   ```
2. Cài đặt các gói phụ thuộc:
   ```bash
   npm install
   ```
3. Tạo file cấu hình môi trường `.env.local` (nếu cần gọi Gemini API trực tiếp):
   ```bash
   cp .env.example .env.local
   ```
   Điền khóa `GEMINI_API_KEY` của bạn vào file `.env.local`.
4. Khởi động môi trường phát triển:
   ```bash
   npm run dev
   ```
   Mở trình duyệt tại địa chỉ `http://localhost:3000` để trải nghiệm giao diện.
