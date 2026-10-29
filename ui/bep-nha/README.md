# Bếp Nhà — Mobile web cho bữa cơm gia đình Việt

Bản web hoạt động độc lập với CLI Python hiện có. HTML, CSS và JavaScript thuần, không cần cài thư viện hoặc khóa API.

Đã có thêm [dự án Android](../../android/README.md) tái sử dụng giao diện này, đóng gói nội dung offline và bổ sung thông báo nhắc nấu Android. Các giới hạn nhắc khi đóng trang bên dưới áp dụng cho bản web trong trình duyệt; xem README Android cho APK.

## Chạy thử

Tại thư mục `ui/bep-nha`:

```bash
npm run dev
```

Mở `http://127.0.0.1:4173`. Chạy kiểm tra logic bằng `npm test` (Node.js 18+).

## Các luồng đã triển khai

- **Hôm nay:** chọn ngày, chọn bữa trưa/tối; xem 3 món, chi phí ước tính và lịch nấu.
- **Thực đơn 7 ngày:** xem cả tuần; đổi riêng món mặn, canh hoặc rau, giữ đúng nhóm món.
- **Công thức bằng chữ:** 14 công thức Việt, nguyên liệu theo số người và các bước dễ đọc.
- **Đi chợ:** gộp nguyên liệu trùng tên và đơn vị; tích đồ đã có, sao chép hoặc tải danh sách dạng chữ.
- **Vào bếp:** tính lịch ngược từ giờ ăn, 15 phút sơ chế; dùng một vùng bếp cho món mặn và một vùng cho canh rồi rau. Đánh dấu việc đã xong, xem công thức, đặt một đồng hồ đếm ngược. Thời gian là gợi ý, cần điều chỉnh theo thiết bị và độ chín thực tế.
- **Nhắc nấu:** nhắc trong trang khi đang mở, tùy chọn Notification API khi trình duyệt hỗ trợ. Xuất `.ics` với cảnh báo trước 10 phút để người dùng tự nhập vào lịch điện thoại.
- **Gia đình & món yêu thích:** đổi số người, giờ ăn, lưu món; lưu trạng thái trên trình duyệt qua localStorage.
- **Hỏi Bếp Nhà:** tìm công thức, món nhanh, món tiết kiệm và lịch nấu bằng quy tắc dựa trên dữ liệu hiện có.

## Định hướng cho người nội trợ trung niên

Tiếng Việt xuyên suốt; điều hướng dưới cùng trên điện thoại; mỗi món có nút công thức và đổi món riêng; công thức có số bước, nguyên liệu hiện định lượng rõ ràng. Màu nền ấm, chữ xanh đậm và ảnh bữa cơm Việt. Hỗ trợ từ màn hình 320px, phím bàn, focus, hộp thoại có nhãn và tùy chọn giảm chuyển động của hệ điều hành.

## Giới hạn của bản hiện tại

Đây là bản thử nghiệm tương tác, chưa có tài khoản, backend, đồng bộ thiết bị hoặc AI tạo sinh. Thông báo trong trang không bảo đảm chạy khi trình duyệt đóng, trang bị hệ điều hành tạm dừng hoặc điện thoại khóa; lịch hệ thống là phương án nhắc bên ngoài trang. Người dùng cần thực sự nhập tệp sự kiện, trình duyệt không tự thêm vào lịch. Chưa kiểm tra thực địa Notification API và nhập lịch trên iOS/Android.

Giá món ăn là dữ liệu minh họa, chưa tính gạo và gia vị có sẵn. Không có tuyên bố dinh dưỡng hoặc cá nhân hóa y tế. Các số lượng phân số được làm tròn một chữ số thập phân; người nấu điều chỉnh lượng trứng, rau củ theo thực tế.

## Giai đoạn tiếp theo

1. Kết nối API phía server cho gợi ý thực đơn và hỏi cách nấu có AI; giữ khóa API ở server, trả kết quả có cấu trúc và xác thực trước khi cập nhật thực đơn.
2. Lưu hồ sơ khẩu vị, dị ứng và ngân sách; bổ sung dữ liệu món ăn được kiểm chứng.
3. Backend lưu thực đơn, Web Push/service worker và lịch nhắc bền vững; không dựa vào timer của tab cho nhắc khi đóng ứng dụng.
4. Thử sử dụng thực tế với người nội trợ Việt Nam trung niên và thiết bị Android/iPhone.

## Nguồn hình ảnh & AI use

Codex viết mã mobile web, dữ liệu công thức minh họa, kiểm tra và tài liệu trong thư mục này. Phần CLI Python hiện có được giữ nguyên. Ảnh mâm cơm `dist/assets/family-meal.webp` được tạo bằng công cụ imagegen tích hợp; ảnh mang tính minh họa.

Prompt ảnh: “Use case: photorealistic-natural. Asset type: main food photograph for a Vietnamese family meal planner web application. Primary request: authentic home cooked Vietnamese dinner, warmly appetizing editorial food photography. Scene: rustic light warm oak table, round rattan woven tray, off-white ceramic plates and bowls. Subject: a dark brown clay pot of glossy caramelized pork belly and boiled eggs (thit kho trung) prominent in lower center, bowl of light green mustard vegetable soup with minced pork upper left, plate of sauteed morning glory with garlic upper right, small bowl white rice and wooden chopsticks at right. Composition: overhead slightly angled close-up, wide landscape 3:2, food fills frame, artfully casual genuinely Vietnamese family home cooking, subtle linen napkin. Soft natural daylight, cream, leaf green and caramel tones, premium cookbook quality, realistic textures. No lettering, no logos, no people, no watermarks.”
