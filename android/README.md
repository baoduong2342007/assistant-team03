# Bếp Nhà — Android native (Kotlin + Jetpack Compose)

Bản 0.3.0-native là ứng dụng Android native. Các màn hình, điều hướng, ô nhập, hộp thoại và danh sách được dựng bằng Jetpack Compose; không dùng WebView, HTML hoặc JavaScript để hiển thị ứng dụng.

## Giao diện 0.3

- Tông xanh rừng, nền kem và điểm nhấn đất nung; chữ tiêu đề serif, ảnh mâm cơm và ảnh đại diện ba món có sẵn trong ảnh gốc.
- Màn hình Hôm nay có lịch chọn ngày, lựa chọn bữa rõ ràng và thẻ món với nút công thức, đổi món và lưu yêu thích.
- Đi chợ có thanh tiến độ và mỗi nguyên liệu là một checkbox duy nhất cho cả hàng; lịch nấu có tiến độ, mốc giờ và trạng thái hoàn thành.
- Giữ vị trí cuộn khi chuyển tab; tự sắp xếp lựa chọn bữa theo chiều dọc trên màn hình hẹp hoặc chữ lớn. Nội dung giới hạn rộng 680dp trên màn hình lớn.
- Dùng chung một bitmap cho ảnh đầu trang và ảnh món; ghi nhớ thực đơn/lịch nấu bằng `derivedStateOf`. Khi ứng dụng đang hiển thị, chỉ cập nhật đồng hồ mỗi giây nếu có hẹn giờ; khi không có hẹn giờ, kiểm tra ngày mới mỗi 30 giây. Android AlarmManager vẫn phụ trách nhắc khi ứng dụng đóng.

## Cài bản thử

Chép `releases/BepNha-0.3.0-native-debug.apk` sang điện thoại Android 8.0 trở lên rồi mở tệp để cài. Đây là APK debug, chưa phát hành lên Google Play. Khi Android hỏi, cấp quyền thông báo để nhận nhắc nấu; trong **Nhắc nấu** có nút mở quyền **Nhắc đúng giờ**.

APK 0.3 đã được xác minh chữ ký nhưng dùng chứng chỉ debug khác APK 0.2 được lưu trong dự án, nên không cài đè trực tiếp được lên bản đó. Cài mới trên thiết bị chưa có bản cũ, hoặc chủ động sao lưu thông tin cần giữ trước khi gỡ bản cũ; gỡ ứng dụng sẽ xóa dữ liệu cục bộ. Mã nguồn UI vẫn có thể build với khóa ký cũ nếu người giữ khóa còn lưu khóa đó.

APK không cần đăng nhập ChatGPT và không phụ thuộc Android System WebView. 14 công thức cùng ảnh minh họa có sẵn trong gói, dùng được offline. Dữ liệu của bản WebView 0.1 và bản web chưa được nhập tự động sang kho dữ liệu native mới.

## Những màn hình đã có

- **Hôm nay:** chọn ngày/bữa, xem ba món, chi phí và thời gian ước tính; đổi riêng từng món, lưu món yêu thích.
- **Thực đơn:** kế hoạch bảy ngày, mỗi bữa có món mặn, canh và rau. Lựa chọn đã lưu được giữ khi sang ngày mới.
- **Công thức:** nguyên liệu theo số người, các bước bằng chữ và mẹo nấu; mở bằng bottom sheet native.
- **Đi chợ:** gộp nguyên liệu cùng tên và đơn vị, tích đồ đã có và sao chép danh sách qua clipboard Android.
- **Vào bếp:** timeline tính ngược từ giờ ăn, canh/rau dùng lần lượt một vùng bếp; tích công việc, mở công thức và đặt đồng hồ. Đồng hồ giữ đúng bữa gốc khi bạn xem ngày khác.
- **Gia đình:** tên, số người từ 1–12 và giờ ăn bằng đồng hồ chọn giờ Android; đổi khẩu phần cập nhật định lượng.
- **Hỏi Bếp Nhà:** ô nhập native để tra món, xem lịch nấu, tìm món nhanh hoặc tiết kiệm trong công thức có sẵn. Chưa có AI tạo sinh.
- **Nhắc nấu:** lưu lịch bằng AlarmManager, gửi thông báo Android, chạm thông báo mở đúng bữa ăn; cập nhật/hủy lịch khi thay đổi thực đơn, giờ ăn hoặc đồng hồ.

Thêm sự kiện vào lịch bằng màn hình soạn sự kiện của ứng dụng lịch. Người dùng xác nhận lưu; Bếp Nhà không yêu cầu quyền đọc hoặc ghi lịch. Ứng dụng không khai báo quyền Internet.

## Giao diện chạy trên máy ảo

Ảnh chụp trực tiếp bản 0.3 trên Android 15/API 35:

![Màn hình Hôm nay](docs/native-home.png)

![Danh sách đi chợ Android native](docs/native-shopping.png)

![Lịch nấu Android native](docs/native-cook.png)

## Cấu trúc native

| Tệp | Vai trò |
| --- | --- |
| `MainActivity.kt` | Điểm vào Compose, quyền, lịch hệ thống, clipboard và đồng hồ chọn giờ |
| `PlannerApp.kt`, `PlannerSheets.kt`, `PlannerTheme.kt`, `KitchenComponents.kt` | Giao diện, thành phần dùng chung và điều hướng Android native |
| `PlannerViewModel.kt` | Trạng thái UI, thao tác người dùng, đồng hồ và xử lý thông báo |
| `PlannerEngine.kt`, `PlannerModels.kt` | Thực đơn, đổi món, định lượng, timeline và lịch nhắc thuần Kotlin |
| `PlannerStorage.kt` | Đọc công thức và lưu trạng thái bằng SharedPreferences trên luồng IO tuần tự |
| `ReminderStore.java`, `ReminderReceiver.java` | Đặt/hủy/khôi phục lịch và hiện thông báo Android |
| `src/main/assets/recipes.json` | Dữ liệu 14 công thức được đóng gói trong APK |
| `src/main/res/drawable-nodpi/family_meal.webp` | Ảnh minh họa được đóng gói như tài nguyên Android |

Các tệp Kotlin/Java ở `app/src/main/java/vn/team03/bepnha/`. Build Android không dùng hoặc sao chép mã trong `ui/`.

## Mở và build

Mở thư mục `android/` trong Android Studio. Dùng JDK 17, SDK Platform 36 và Build Tools 35.0.0. Phiên bản đã ghim: AGP 8.13.2, Gradle 8.13, Kotlin/Compose Compiler 2.2.10 và Compose BOM 2025.08.00.

```bash
./gradlew testDebugUnitTest assembleDebug lintDebug
```

Windows dùng `gradlew.bat`. APK nằm ở `app/build/outputs/apk/debug/app-debug.apk`. Không cần Node, npm, máy chủ web hay khóa API để build/chạy bản Android.

Kiểm thử giao diện cần máy ảo riêng vì bộ kiểm thử xóa dữ liệu thử nghiệm Bếp Nhà trước mỗi ca:

```bash
./gradlew connectedDebugAndroidTest
```

## Xác minh

Đã build bản 0.3, chạy **12 unit test** và **8 kiểm thử instrumentation trên máy ảo Android 15/API 35 ARM64**, tất cả đều qua. Kiểm thử gồm: xác nhận có AndroidComposeView và không có WebView trong cây màn hình; đổi món; công thức chữ; danh sách đi chợ; khẩu phần sáu người; lưu/đọc dữ liệu; hủy PendingIntent khi tắt nhắc; khôi phục vị trí cuộn khi chuyển tab; và bật/tắt checkbox nguyên liệu qua cả hàng mà vẫn giữ trạng thái khi chuyển tab. Unit test kiểm tra timeline hai vùng bếp, định lượng, giữ thực đơn qua ngày mới, nhắc quá hạn và đồng hồ giữ bữa gốc.

Ảnh chụp native được lấy từ ứng dụng đang chạy trên máy ảo bằng ADB, không phải ảnh mockup. Android Lint có **0 lỗi, 13 cảnh báo** về phiên bản công cụ/thư viện, biểu tượng monochrome và cách dùng KTX. Đã kiểm tra trực tiếp đồng hồ đang giảm sau khi bật hẹn giờ. Chưa kiểm tra trên điện thoại thật, các chế độ ngủ/tiết kiệm pin, reboot thực tế hoặc giao thông báo đúng giờ trên từng hãng máy.

Đã kiểm tra trực quan bố cục ở chiều rộng khoảng 393dp và 320dp, bao gồm cỡ chữ 130%. [Ảnh thẻ món ăn với chữ lớn](docs/native-large-font.png). Đây là kiểm tra trên máy ảo, chưa phải đánh giá khả năng tiếp cận đầy đủ trên thiết bị thật.

## Giới hạn

Giờ nấu và giá là ước tính, cần kiểm tra độ chín thực tế. Dữ liệu lưu trên thiết bị, chưa có tài khoản hoặc đồng bộ. Chỉ có một đồng hồ đếm ngược tại một thời điểm; nhắc từng bữa hoạt động độc lập.

Thông báo cần quyền của người dùng; quyền Alarms & reminders hỗ trợ nhắc đúng giờ trên Android tương ứng. Tiết kiệm pin hoặc hạn chế ứng dụng của nhà sản xuất vẫn có thể ảnh hưởng. Dừng bắt buộc ứng dụng ngăn nhắc cho đến khi mở lại.

## AI use và tài liệu

Codex viết phần Kotlin/Compose native, logic thực đơn, persistence, unit test, instrumentation test và tài liệu; Codex thực hiện build và chạy các kiểm tra nêu trên. Trong bản 0.3, Codex chỉnh UI, các thành phần dùng chung, giữ trạng thái cuộn, cache dữ liệu hiển thị, nhịp cập nhật đồng hồ, thêm hai kiểm thử instrumentation và cập nhật tài liệu/ảnh chụp. Phần nhắc Android bằng Java được giữ lại từ bản thử trước. Ảnh mâm cơm được tái sử dụng từ ảnh imagegen của dự án, có prompt và công bố trong `../ui/bep-nha/README.md`. Chưa có bằng chứng review thủ công của thành viên nhóm.

Tham khảo: [Jetpack Compose](https://developer.android.com/develop/ui/compose), [Compose Compiler Gradle plugin](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler), [quyền thông báo](https://developer.android.com/develop/ui/views/notifications/notification-permission), [AlarmManager](https://developer.android.com/develop/background-work/services/alarms).
