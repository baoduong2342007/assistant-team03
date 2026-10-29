# Bếp Nhà — ứng dụng Android

Bản Android cài bằng APK, chạy giao diện Bếp Nhà có sẵn trong WebView và bổ sung chức năng Android bằng Java. Toàn bộ giao diện, ảnh và 14 công thức được đóng gói trong APK; không tải trang Sites và không yêu cầu đăng nhập ChatGPT.

## Cài thử trên điện thoại

1. Chép `releases/BepNha-0.1.0-debug.apk` sang điện thoại Android 8.0 trở lên.
2. Mở tệp APK. Nếu Android hỏi, cho phép ứng dụng đang mở tệp cài ứng dụng từ nguồn này rồi xác nhận cài Bếp Nhà.
3. Mở Bếp Nhà, chọn thực đơn và thử các mục **Đi chợ**, **Vào bếp**. Khi bật nhắc nấu hoặc hẹn giờ, cho phép thông báo nếu Android hỏi.
4. Trong **Nhắc nấu**, chọn **Bật quyền nhắc đúng giờ** nếu muốn nhắc theo giờ đã đặt. Khi thiếu quyền này, Android có thể trì hoãn thông báo.

Đây là APK debug để thử nghiệm, chưa phải bản phát hành Google Play. Android System WebView trên máy cần được cập nhật để hỗ trợ các tính năng JavaScript và hộp thoại của giao diện.

## Đã có trong bản Android

- Thực đơn 7 ngày, đổi món trong từng bữa, công thức bằng chữ và định lượng theo số người.
- Danh sách đi chợ có ô tích, nút sao chép dùng clipboard Android.
- Lịch nấu dựa trên giờ ăn, món đã chọn và bếp hai vùng nấu.
- Nhắc giờ bắt đầu nấu và đồng hồ nấu ăn bằng `AlarmManager` cùng thông báo Android. Lịch được lưu phía Android và khôi phục sau khi khởi động lại thiết bị hoặc cập nhật app.
- Đổi món/giờ ăn cập nhật lịch nhắc tương ứng; tắt nhắc hoặc dừng đồng hồ hủy lịch đã đặt.
- Chạm thông báo để mở bữa ăn tương ứng; thao tác Back đóng hộp thoại, trở về Hôm nay hoặc thoát ứng dụng.
- Thêm lịch qua màn hình soạn sự kiện của ứng dụng lịch. Người dùng tự xác nhận lưu; Bếp Nhà không yêu cầu quyền đọc/ghi lịch.
- Nội dung chạy offline và lưu trên thiết bị. APK không khai báo quyền truy cập Internet; WebView chỉ mở tài nguyên đóng gói qua `WebViewAssetLoader`, không cấp quyền đọc file.

## Giới hạn hiện tại

Trợ lý gợi ý theo công thức có sẵn, chưa kết nối AI tạo sinh. Giờ nấu và chi phí là ước tính. Chưa có đồng bộ, tài khoản hoặc nhập dữ liệu từ bản web.

Thông báo cần quyền của người dùng; nhắc đúng giờ còn cần quyền Alarms & reminders trên Android hỗ trợ. Chế độ tiết kiệm pin hoặc hạn chế ứng dụng của nhà sản xuất có thể ảnh hưởng việc nhận thông báo. **Dừng bắt buộc** ứng dụng sẽ ngăn lịch nhắc cho đến khi bạn mở lại. Mỗi lần chỉ có một đồng hồ nấu ăn; các nhắc bữa ăn hoạt động độc lập.

Đã build APK, chạy unit test Java/JavaScript và Android Lint. Chưa chạy thử trên điện thoại thật hoặc máy ảo Android; cần kiểm tra thao tác, âm báo khi khóa màn hình, cấp/từ chối quyền và khôi phục lịch sau reboot trước khi dùng thực tế.

## Mở bằng Android Studio

Mở thư mục `android/`, chọn Gradle JDK 17 và cài SDK Platform 36 cùng Build Tools 35.0.0. Dự án dùng Android Gradle Plugin 8.13.2, Gradle Wrapper 8.13 đã ghim SHA-256. Android Studio có thể tự tạo `local.properties` cho đường dẫn SDK của bạn; tệp này không đưa vào Git.

```bash
./gradlew testDebugUnitTest assembleDebug lintDebug
```

Trên Windows dùng `gradlew.bat`. APK build nằm ở `app/build/outputs/apk/debug/app-debug.apk`.

Kiểm tra logic dùng chung:

```bash
npm --prefix ../ui/bep-nha test
```

Không cần npm install. Task `prepareWebAssets` lấy mã từ `../ui/bep-nha/dist`, bỏ liên kết font từ Internet, thêm CSS Android và tạo tài nguyên trong `app/build/generated/bepNhaAssets/web/`. Không sửa trực tiếp các tệp sinh ra trong `build/`.

## Cấu trúc

| Thành phần | Vai trò |
| --- | --- |
| `app/src/main/java/vn/team03/bepnha/MainActivity.java` | WebView, cầu nối JavaScript, quyền, lịch và Back |
| `ReminderStore.java` | Lưu, cập nhật, hủy và khôi phục lịch nhắc |
| `ReminderReceiver.java` | Hiện thông báo, mở đúng bữa ăn |
| `RestoreRemindersReceiver.java` | Khôi phục lịch khi hệ thống khởi động lại/cấp quyền |
| `app/src/main/web/native.css` | Điều chỉnh giao diện trong Android |
| `../ui/bep-nha/dist/platform.js` | Chuyển trạng thái thực đơn/đồng hồ thành lịch nhắc Android |

Tài liệu API áp dụng: [nội dung đóng gói trong WebView](https://developer.android.com/develop/ui/views/layout/webapps/load-local-content), [quyền thông báo](https://developer.android.com/develop/ui/views/notifications/notification-permission), [AlarmManager và quyền nhắc đúng giờ](https://developer.android.com/develop/background-work/services/alarms).

## AI use

Codex viết phần đóng gói Android, cầu nối JavaScript, logic lịch nhắc, kiểm tra Java/JavaScript và tài liệu. Codex chạy bộ kiểm tra và build APK. Chưa có bằng chứng review hoặc kiểm thử trên thiết bị của thành viên nhóm; không quy các phần mã/kiểm tra này cho người dùng tự viết.
