# Ảnh kiểm chứng UI điện thoại

Ảnh được render trực tiếp bằng Compose trên máy ảo Android API 37, không phải mockup đồ họa. Các ca AI dùng dữ liệu giả lập có chủ đích; chưa gọi backend/mô hình thật.

| Ảnh | Trường hợp |
| --- | --- |
| [Hôm nay](native-home.png) | Giao diện thật, dữ liệu đóng gói, chữ hệ thống mặc định |
| [Đi chợ](native-shopping.png) | Checklist và định lượng nguyên liệu |
| [Công thức](native-recipe.png) | Nội dung các bước nấu, cuộn tới phần cách nấu |
| [Vào bếp](native-cook.png) | Timeline với khẩu phần sáu người trong ca kiểm thử |
| [Điều hướng chữ 200%](phone-navigation-320-text200.png) | Fixture khung 320 × 640dp; bốn tab và nút trợ lý |
| [Chat chữ 200% và timer](phone-chat-320-text200-timer.png) | Fixture kiểm tra vùng nhập khi ngữ cảnh/timer cuộn được |
| [Tra cứu trên máy](phone-chat-offline.png) | Gợi ý điền ô nhập; nhãn nút khi chưa cấu hình AI |
| [Đề xuất chữ 200%](phone-chat-proposal-text200.png) | Đề xuất giả lập, cuộn đến nút xác nhận |
| [Khung ngắn](phone-chat-reduced-height.png) | Fixture 320 × 300dp, chữ 150% |
| [Composer ngang](phone-chat-landscape-text200-compact.png) | ForcedSize 640 × 140dp và chữ 200%, không phải IME thật |
| [IME thật, cử chỉ](real-landscape-ime-text200.png) | Gboard, màn ngang, font scale hệ thống 2.0, nút tìm khả dụng |
| [IME thật, ba nút](real-landscape-ime-text200-threebutton.png) | Cùng trường hợp trên với thanh ba nút; banner cập nhật cỡ chữ là của Gboard |

Các ảnh khung giới hạn dùng Compose density override hoặc DeviceConfigurationOverride; không đại diện cho mọi thiết bị vật lý. Hai ảnh IME thật được chụp trực tiếp bằng Android CLI.

Lần chạy instrumentation bằng Gradle có thể gỡ app và xóa ảnh. Khi thu ảnh, dùng task installDebug và installDebugAndroidTest, chạy AndroidJUnitRunner qua adb rồi pull từng tệp trước khi gỡ app. Pull nguyên thư mục vào đích đã có sẽ tạo thêm thư mục con; không trộn bộ ảnh cũ và mới.

Xem [bàn giao backend và kiểm thử](../phone-ui-handoff.md).
