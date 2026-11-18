# Demo Bếp Nhà offline

Cài `app-debug.apk` trên Android 8.0 trở lên. Nếu cần kiểm thử thông báo, cho phép thông báo và bật **Nhắc đúng giờ** trong **Thêm → Nhắc nấu**. Có thể bật chế độ máy bay để trình diễn các bước dưới đây.

| Luồng | Cách trình diễn | Kết quả cần thấy |
| --- | --- | --- |
| Thực đơn và ảnh | Ở **Hôm nay**, chọn **Bữa sáng** rồi chọn vài ngày trong dải tuần. Chuyển sang **Bữa tối** và đổi món mặn thành **Đậu hũ sốt cà chua**. | Mỗi món sáng có ảnh tương ứng. Ảnh lớn cập nhật khi đổi món. Mở công thức để xem định lượng và các bước nấu. |
| Tủ lạnh | Vào **Hỏi bếp → Tủ lạnh**, chọn **Trứng** và **Rau muống**, rồi xem một gợi ý. | Gợi ý cho biết món dùng nguyên liệu nào và còn cần mua gì. |
| Trợ lý trên máy | Mở **Hỏi Bếp Nhà** và nhập “Bữa tối ngày mai tôi muốn ăn đậu hũ sốt cà chua”. Chọn **Xác nhận thay đổi** nếu app hiển thị bản xem trước. | App ghi nguồn trả lời trên máy và giữ đúng ngày, bữa đích. Nếu món đó đã có, trợ lý báo thực đơn đã khớp. |
| Đi chợ | Vào **Đi chợ**, chọn trưa và tối hoặc **Cả ngày**. | Danh sách cộng định lượng nguyên liệu trùng và cho phép đánh dấu đã có. |
| Hẹn giờ | Gửi “hẹn 1 phút luộc rau” và “hẹn 1 phút nấu canh” trong hai lượt, rồi mở **Hỏi bếp → Hẹn giờ**. | Hai đồng hồ chạy song song và mỗi đồng hồ có thông báo riêng nếu đã cấp quyền. |

APK này trình diễn trợ lý cục bộ. Gemini thật sẽ được nghiệm thu sau khi nhóm chuẩn bị cấu hình Firebase và App Check. [Hướng dẫn Firebase](../../docs/firebase-spark-demo.md)
