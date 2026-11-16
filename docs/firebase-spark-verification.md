# Báo cáo nghiệm thu kỹ thuật: Firebase Spark Demo

Tài liệu ghi nhận kết quả kiểm thử và nghiệm thu kỹ thuật của kế hoạch **Firebase Spark cho demo nội bộ** trên nhánh `codex/firebase-spark-assistant`.

---

## 1. Tóm tắt kết quả nghiệm thu

| Hạng mục | Kết quả | Chi tiết |
|---|---|---|
| **JVM Unit Tests** | ✅ ĐẠT (47/47) | Logic Kotlin độc lập (`MealPlanningTest`, `AssistantServiceTest`), 0 failures, 0 errors. Không cần Python backend. |
| **Android Device Tests (UI)** | ✅ ĐẠT (30/30) | Chạy trên máy ảo `Medium_Phone_API_37.0`. Đạt toàn bộ luồng gợi ý, chọn mâm, sửa riêng món canh, xác nhận, lưu checklist. |
| **Upgrade Compatibility** | ✅ ĐẠT (1/1) | Test cài đè APK mới lên commit baseline `88f71d8`. Bảo toàn nguyên vẹn 100% dữ liệu (thực đơn, khẩu phần, checklist đi chợ, lịch nhắc). |
| **App Check Enforcement** | ✅ ĐẠT | Bật chế độ `ENFORCED`. Thiết bị chưa đăng ký debug token bị từ chối truy cập AI (`unregisteredDeviceCannotCallAI` PASS). Thiết bị đã đăng ký trao đổi token thành công (HTTP 200). |
| **Cấu hình Google Cloud API Key** | ✅ ĐÃ BỔ SUNG | Đã cập nhật `apiTargets` của Android API key trong Google Cloud để cấp quyền truy cập dịch vụ `generativelanguage.googleapis.com` và `firebasevertexai.googleapis.com`. |
| **Local Fallback Mode** | ✅ ĐẠT | Khi gặp timeout hoặc sự cố mạng, ứng dụng tự động kích hoạt trợ lý cục bộ (Local Service), phản hồi đúng ý định, không làm gián đoạn trải nghiệm người dùng và gắn nhãn thông báo rõ ràng. |
| **Live AI Generation** | ⚠️ CHƯA XÁC MINH TOÀN BỘ | API `countTokens` phản hồi nhanh (1.4s), nhưng lệnh `generateContent` trực tiếp qua mạng trường học (HCMUS-STUDENT) bị timeout kết nối. Ứng dụng rơi vào fallback cục bộ an toàn theo đúng thiết kế. |
| **Điện thoại vật lý** | ⏸️ TẠM HOÃN | Theo yêu cầu của người dùng, đợt nghiệm thu này chỉ thực hiện trên thiết bị ảo (Android Emulator API 37). |

---

## 2. Bằng chứng kiểm thử tự động

### 2.1. JVM Unit Tests (`./gradlew.bat testDebugUnitTest`)
- **Tổng số ca kiểm thử:** 47
- **Thất bại:** 0
- **Lỗi:** 0
- **Các bộ test chính:**
  - `vn.team03.bepnha.MealPlanningTest`: Nhận diện ngày/bữa ăn, xử lý danh sách nguyên liệu có/hết, loại trừ nguyên liệu dị ứng/kiêng, thuật toán xếp mâm cơm.
  - `vn.team03.bepnha.AssistantServiceTest`: Semantic swap món ăn, quản lý ngữ cảnh hỏi lại, validate intent (chặn action ngoài yêu cầu người dùng).
  - `vn.team03.bepnha.AssistantCommandsTest`: Chuyển đổi và xác thực action proposal.

### 2.2. Android Instrumentation Tests (`connectedDebugAndroidTest`)
- **Thiết bị:** `emulator-5554` (AVD: `Medium_Phone_API_37.0`, Android SDK 36/37)
- **Tổng số ca kiểm thử:** 30
- **Kết quả:** `OK (30 tests)`, thời gian chạy ~220s.
- **Các ca tiêu biểu:**
  - `pantryMealCanBeSelectedByChatRevisedAndSavedAtItsOriginalTarget`: Gợi ý mâm theo nguyên liệu tủ lạnh ➔ Chọn mâm ➔ Sửa riêng món canh qua chat ➔ Xác nhận ➔ Kiểm tra lưu thực đơn & danh sách đi chợ.
  - `localServiceSuggestsRevisesAndPersistsWithoutFirebase`: Toàn bộ luồng nghiệp vụ hoạt động ổn định ở chế độ offline / local.
  - `PhoneAccessibilityTest`: Đảm bảo kích thước chữ lớn (200%), giao diện không bị tràn hoặc che khuất nút bấm.
  - `NativeStorageTest`: Đọc/ghi dữ liệu an toàn qua SQLite/SharedPreferences.

### 2.3. Kiểm thử tương thích cài đè (`UpgradeCompatibilityTest`)
- Cài đặt APK gốc từ commit `88f71d8`.
- Nạp dữ liệu giả lập (thực đơn 7 ngày, khẩu phần, món yêu thích, danh sách đi chợ, lịch nhắc).
- Cài đè bản build mới `app-debug.apk` cùng chữ ký.
- Chạy `UpgradeCompatibilityTest#verifyUpgradeFixture`: Toàn bộ dữ liệu được bảo toàn nguyên vẹn.

---

## 3. Cấu hình Firebase & App Check

- **Firebase Project ID:** `bep-nha-team03-20261116-21eb9`
- **Application ID:** `vn.team03.bepnha`
- **App Check Service:** `firebaseml.googleapis.com` (Enforcement: `ENFORCED`)
- **Debug Token:** Đăng ký trong Firebase Console ➔ App Check ➔ Android App ➔ Manage debug tokens.
- **Model cấu hình:** `gemini-3.8-flash` (Firebase BoM `35.2.0`).

---

## 4. Kết luận

Ứng dụng đáp ứng đầy đủ các tiêu chuẩn nghiệp vụ đã đề ra: không cần duy trì backend Python, không yêu cầu người dùng nhập Gemini API key, bảo vệ tài nguyên qua App Check, và có cơ chế xử lý cục bộ hoàn chỉnh khi mạng gặp sự cố. Bản build debug `android/app/build/outputs/apk/debug/app-debug.apk` sẵn sàng cho việc trình diễn nội bộ.
