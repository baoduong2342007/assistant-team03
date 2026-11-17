# Bếp Nhà Mình — demo Firebase Spark

Android gọi Firebase AI Logic bằng Gemini Developer API. APK không cần Python, backend riêng, đăng nhập người dùng hay nhập Gemini API key. Model ghim trong build là `gemini-3.8-flash`; không tự đổi model hoặc gửi lại khi lỗi. Firebase BoM: `35.2.0`.

## Dự án và cấu hình

- Project sử dụng: `bep-nha-team03-20261116-21eb9`.
- Android application ID: `vn.team03.bepnha`.
- Firebase app ID: `1:356596396940:android:02b3ab7b8fe749a5b81ca9`.
- Đã kiểm tra Cloud Billing chưa bật. Không liên kết tài khoản thanh toán để giữ Spark.
- `.firebaserc` chọn dự án; `android/app/google-services.json` nằm ngoài Git. Đây là cấu hình Firebase, không phải Gemini API key.
- Project Google Cloud ban đầu `bep-nha-team03-20261116` chưa dùng; dự án có hậu tố `-21eb9` do người dùng tạo trong Console là dự án chính thức của đợt demo này.

Thiết lập trên máy phát triển khác, từ gốc repo (Node.js 20+, Firebase account có quyền):

```powershell
npx -y firebase-tools@latest login --interactive
npx -y firebase-tools@latest projects:list
npx -y firebase-tools@latest apps:sdkconfig ANDROID 1:356596396940:android:02b3ab7b8fe749a5b81ca9 --project bep-nha-team03-20261116-21eb9 --out android/app/google-services.json
```

Đợt này đã chạy `npx -y firebase-tools@latest init ailogic --interactive` để provision dịch vụ. Nếu tạo dự án mới, đăng ký Android app trước rồi chạy bước này; chỉ tải cấu hình Android là chưa đủ. Chọn Gemini Developer API; giữ Spark. Lỗi `addFirebase` 403 có thể do thiếu quyền hoặc chưa chấp nhận điều khoản Firebase trong Console.

## App Check và thiết bị nội bộ

`BepNhaApplication` cài Debug Provider trước khi tạo AI Logic. AI dùng limited-use App Check token. Enforcement của Firebase AI Logic phải là **Enforced** trong Firebase Console → App Check → APIs. Đây là bản debug cho thiết bị nội bộ; bản release hiện dùng xử lý cục bộ và chưa chuẩn bị chứng thực cho phát hành công khai.

Mỗi máy ảo/điện thoại cần đăng ký riêng một lần:

1. Cài APK debug, mở Hỏi Bếp Nhà và gửi một câu hỏi. Thiết bị chưa đăng ký sẽ báo lỗi xác thực AI.
2. Người quản trị xem Logcat riêng trên máy phát triển, lọc `DebugAppCheckProvider` để lấy token. Không dán token vào chat, Git, ảnh chụp hoặc tài liệu bàn giao.
3. Trong Console → App Check → ứng dụng Android → Manage debug tokens, thêm token với tên thiết bị. Có thể dùng `npx -y firebase-tools@latest appcheck:debugtokens:create` theo trợ giúp CLI; không đặt giá trị token trong script được commit.
4. Chờ cấu hình có hiệu lực, gửi lại thủ công. Chỉ ghi nhận AI thành công khi nguồn trả lời là AI, không phải kết quả cục bộ.
5. Cài đè bằng APK cùng chữ ký giữ token và dữ liệu. Gỡ cài đặt/xóa dữ liệu sinh token mới; đăng ký lại và thu hồi token cũ. Thu hồi thiết bị không dùng trong Console.

Không chạy `connectedDebugAndroidTest` sau khi đăng ký thiết bị demo mà chưa chuẩn bị đăng ký lại: bộ kiểm thử dùng dữ liệu thử và có thể gỡ cài đặt APK. Kiểm thử AI thật dùng `adb install -r` và `am instrument` để giữ token qua các ca.

## Hợp đồng và xử lý trên Android

`AssistantProvider.generate` là giao diện bất đồng bộ có thể thay bằng phản hồi giả lập. `FirebaseAssistantProvider` chỉ vận chuyển yêu cầu; `AssistantService`, `MealPlanning`, `AssistantCommands` kiểm tra và tạo đề xuất. `recipes.json` trên Android là nguồn duy nhất cho catalog, giá, định lượng và lịch nấu. Backend Python cũ đã được gỡ khỏi repo.

Mỗi lượt là một vòng agent nhỏ (`AssistantService`): tối đa **4 lần gọi model** trong cùng một `Chat`, tối đa 6 tool call mỗi lần, 48 actions và hai mâm; timeout 25 giây mỗi lần gọi. App Check có yêu cầu chứng thực riêng. Tool đọc chạy ngay và kết quả được gửi lại model bằng `FunctionResponsePart`. Đề xuất sai được trả lỗi về cho model tự sửa; hết bốn lần thì trợ lý hỏi lại người dùng.

- Tool đọc: `get_meals`, `search_recipes`, `get_recipe`, `get_shopping_list` (một bữa hoặc cả ngày), `get_cooking_plan`. Mỗi lượt chỉ gửi chỉ mục catalog (ID, tên, nhóm, phút, giá) và chi tiết của bữa đang chọn; chi tiết khác lấy qua tool.
- `propose_actions(reply, actions)` có 18 loại action: 11 loại cũ cùng `select_meal`, `open_sheet`, `close_assistant`, `share_shopping_list`, `add_to_calendar`, `request_permission`, `set_family_name`. Thao tác chung (khẩu phần, tên gia đình, món yêu thích, dừng hẹn giờ) không cần ngày/bữa; thiếu ngày/bữa thì dùng bữa đang chọn.
- `plan_meals(from, to, meals, avoid_ingredients, prefer_ingredients, max_minutes, max_cost)` lên thực đơn nhiều bữa. Model chỉ gửi ràng buộc; `MealPlanner` tự chọn mâm hợp lệ, tránh lặp món mặn trong ba ngày. Nguyên liệu người dùng nói cần tránh luôn được áp dụng dù model bỏ sót.
- `suggest_meals(date, meal, pantry_items, exclude_ingredients, pantry_only)` chỉ gửi điều kiện. Kotlin đối chiếu câu người dùng, tự xếp mâm, tạo `recipe_ids` và `pantry_keys`.
- Kiểm tra theo cấu trúc: ngày trong bảy ngày, bữa trưa/tối, vị trí và nhóm món, ID món, tham số đúng kiểu. Không còn chặn theo từ khóa trong câu, nên "tối mai nấu canh chua cá nhé" vẫn đổi được món. Khi câu nói rõ "giữ nguyên", "đừng đổi" hoặc tên món cụ thể mà đề xuất trái ý, app nhắc model một lần; nếu vẫn trái ý thì hỏi lại người dùng.
- Mâm gắn với ID lượt gửi/ID tin trả lời và snapshot bữa ăn. Thay ngày, bữa, số người hoặc gửi yêu cầu mới thay điều kiện làm mâm cũ hết hiệu lực. So sánh hai mâm dùng nhóm đang hoạt động.
- Phân mức rủi ro (`AssistantRisk`): xem/mở màn hình chạy ngay; tích nguyên liệu, bước nấu, lưu món, hẹn giờ, nhắc nấu làm ngay kèm nút **Hoàn tác** (hoặc nói "hoàn tác"); đổi món, lên thực đơn, khẩu phần, giờ ăn, tên gia đình, thay đồng hồ đang chạy cần xem trước và xác nhận. Bản xem trước nhóm theo bữa, mỗi bữa có nút "Giữ bữa này như cũ". Xác nhận tiêu thụ một lần; lưu dữ liệu và cập nhật AlarmManager trước khi thông báo thành công. Hoàn tác chỉ chạy khi dữ liệu chưa bị chỉnh tay sau đó.

Không có cấu hình Firebase, mất mạng, timeout, hết quota hoặc dịch vụ tạm lỗi: xử lý cục bộ theo đúng yêu cầu và hiển thị lý do. Trợ lý cục bộ hỗ trợ tra công thức, gợi ý theo nguyên liệu, chọn mâm và đổi món có tên rõ; câu mơ hồ hỏi lại. Lỗi cấu hình/App Check và phản hồi sai cấu trúc có thông báo riêng. Không kiểm tra tồn kho theo số lượng, ngân sách hoặc chế độ ăn.

Hội thoại giữ qua xoay máy và mở công thức. Khi tiến trình kết thúc, chỉ dữ liệu đã xác nhận và bản nháp được phục hồi; không phục hồi đề xuất có thể thực thi.

## Build và kiểm thử

```powershell
cd android
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

JDK 17, Android SDK 36; emulator thử `Medium_Phone_API_37.0`. APK: `android/app/build/outputs/apk/debug/app-debug.apk`. Thiếu `google-services.json` vẫn build và dùng trợ lý cục bộ; không nghiệm thu bản đó là demo AI.

Kiểm thử thường dùng phản hồi giả lập, không tốn quota. Năm ca trong `FirebaseLiveTest` được bỏ qua nếu chưa bật cờ riêng. Sau khi cài cả APK ứng dụng và APK kiểm thử bằng `adb install -r`, chạy một ca thật:

```powershell
adb -s emulator-5554 shell am instrument -w -e class vn.team03.bepnha.FirebaseLiveTest#firebaseCanAnswerWithoutLocalFallback -e firebaseLive true vn.team03.bepnha.test/androidx.test.runner.AndroidJUnitRunner
```

Các ca riêng: `realPantryExclusionProducesGroundedSuggestions`, `realPendingRevisionOnlyChangesSoup`, `realNavigationOpensShopping`. Ca từ chối thiết bị chưa đăng ký dùng `unregisteredDeviceCannotCallAI` và cờ `firebaseUnregistered=true`, trước khi đăng ký token. Dừng kiểm thử thật nếu hết quota; không tính kết quả fallback là AI đạt.

## Chạy demo

1. “Tủ lạnh có thịt heo, trứng và rau muống. Tối mai nấu gì?”
2. Chọn **Dùng mâm 1** hoặc gửi “Lấy mâm 1”.
3. **Sửa đề xuất** → “Đổi món canh thành canh chua cá”.
4. **Xác nhận thay đổi**; kiểm tra đúng ngày/bữa, Đi chợ, Vào bếp rồi đóng/mở lại ứng dụng.
5. Ngắt mạng và lặp lại với tên món rõ; kiểm tra nhãn cục bộ và lý do chuyển chế độ.

Quota xem trong Google AI Studio/Firebase AI Logic của đúng project. Spark không tự nâng lên trả phí; hạn mức cụ thể phụ thuộc model và project. App không hứa thời điểm quota phục hồi.

Kết quả thực tế và giới hạn nghiệm thu ghi tại [firebase-spark-verification.md](firebase-spark-verification.md). Điện thoại thật chưa kiểm thử theo lựa chọn của người dùng trong đợt này.

Tài liệu chính thức: [giá](https://firebase.google.com/docs/ai-logic/pricing), [model](https://firebase.google.com/docs/ai-logic/models), [App Check](https://firebase.google.com/docs/ai-logic/app-check), [Debug Provider Android](https://firebase.google.com/docs/app-check/android/debug-provider).
