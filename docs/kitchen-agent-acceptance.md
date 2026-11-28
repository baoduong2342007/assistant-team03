# Bếp Nhà — nghiệm thu trợ lý agent cho người lớn tuổi

Nhánh `feat/kitchen-agent`. Tài liệu này liệt kê cách kiểm tra từng phần trước khi demo. Kiểm thử JVM chạy được trên mọi máy; phần có AI thật cần Firebase, App Check và quota.

## Đã có trong bản này

| Phần | Người dùng làm được gì |
|---|---|
| Agent nhiều bước | Nói tự nhiên ("tối mai nấu canh chua cá nhé"), trợ lý tự xem thực đơn, công thức, danh sách đi chợ rồi đề xuất. Sai thì tự sửa, tối đa 4 lần gọi AI mỗi lượt. |
| Mức xác nhận | Xem/mở màn hình làm ngay. Tích đồ, lưu món, hẹn giờ, nhắc nấu làm ngay và có **Hoàn tác**. Đổi món, lên thực đơn, khẩu phần, giờ ăn, hồ sơ ăn uống cần xác nhận. |
| Lên thực đơn nhiều bữa | "Lên thực đơn cả tuần" → xem trước theo từng bữa, bỏ bữa không muốn đổi, xác nhận một lần. |
| Mọi phần của app | Chọn bữa, mở sheet đổi món/nhắc nấu, chia sẻ danh sách đi chợ (bữa hoặc cả ngày), thêm vào lịch, xin quyền, đổi tên gia đình, mở "Nấu cùng tôi". |
| Giọng nói | Nút micro lớn, đọc to câu trả lời, nói "đồng ý"/"không" để xác nhận, "hoàn tác" để trả lại. Cách xưng hô và tốc độ đọc ở màn Gia đình. |
| Hồ sơ ăn uống | Ít muối, ít đường, mềm, ít dầu mỡ, chay; dị ứng; nguyên liệu tránh; thời gian và ngân sách. Áp dụng tự động cho mọi gợi ý. |
| Chủ động | Lời chào tóm tắt bữa sắp tới. "Nấu cùng tôi" đọc từng bước; bản debug có Firebase trò chuyện bằng Gemini Live (sang bước, hẹn/tắt hẹn giờ, hỏi về món đang nấu), còn lại nhận lệnh "tiếp", "đọc lại", "hẹn giờ 10 phút" trên máy. Thông báo nhắc nấu và nhắc đi chợ. |
| Lịch sử | 30 tin nhắn gần nhất được lưu trên máy (chỉ chữ, không khôi phục đề xuất có thể chạy lại). Chỉ tin trong 6 giờ gần nhất được gửi cho AI. Có nút "Xóa lịch sử trò chuyện". |

## 1. Kiểm thử tự động không tốn quota

Từ thư mục `android` (cần JDK 17 và Android SDK; đặt `ANDROID_HOME` hoặc `local.properties`):

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug compileDebugAndroidTestKotlin
```

Kết quả gần nhất trên nhánh này: 139 test JVM đạt, build, lint và biên dịch instrumentation thành công. Trên emulator `Medium_Phone_API_37.0` (API 37, không có `google-services.json`): 51 test thiết bị, 43 đạt, 8 bỏ qua vì cần AI thật hoặc kịch bản nâng cấp cài đè (`FirebaseLiveTest`, `AgentEvalTest`, `UpgradeCompatibilityTest`), 0 lỗi. Có 5 test giao diện riêng cho agent trong `AgentUiTest`: Nấu cùng tôi, hồ sơ ăn uống, lời chào và micro, hoàn tác, xóa lịch sử.

Kiểm thử giao diện trên emulator (`connectedDebugAndroidTest`) dùng dữ liệu thử, **xóa dữ liệu app và có thể gỡ APK**, làm mất debug token App Check. Chỉ chạy trên emulator riêng cho kiểm thử, không chạy trên máy demo đã đăng ký.

## 2. Kiểm thử với AI thật (tốn quota)

Cần `android/app/google-services.json` của nhóm, thiết bị đã đăng ký debug token App Check (xem [firebase-spark-demo.md](firebase-spark-demo.md)). Cài đè bằng `adb install -r` để giữ token, rồi chạy từng nhóm nhỏ:

```powershell
adb shell am instrument -w -e class vn.team03.bepnha.AgentEvalTest -e firebaseLive true -e evalGroup swap vn.team03.bepnha.test/androidx.test.runner.AndroidJUnitRunner
```

Nhóm: `swap`, `plan`, `diet`, `pantry`, `app`, `clarify` (23 câu). Dùng `-e evalCase swap-casual` để chạy một câu. Bộ thử dừng khi hết quota; thêm `-e evalPauseMs 20000` để giãn cách khi chạy cả bộ. Câu do trợ lý trên máy trả lời được ghi `not-ai`, không tính là đạt. Báo cáo nằm ở `files/agent-eval.json` của app:

```powershell
adb shell run-as vn.team03.bepnha cat files/agent-eval.json
```

### Khi AI không trả lời

- Lấy debug token: token chỉ được in **một lần**, lúc tạo, dưới tag `com.google.firebase.appcheck.debug.internal.DebugAppCheckProvider`:
  `adb logcat -d | Select-String "App Check debug token"`. Nếu Logcat đã bị xóa, đọc lại trong
  `adb shell "run-as vn.team03.bepnha sh -c 'cat shared_prefs/com.google.firebase.appcheck.debug.store*'"`.
  Không bật emulator với `-no-snapshot-save` trước khi đăng ký: lần khởi động sau quay về trạng thái cũ và mất token.
- Bản debug ghi lý do App Check từ chối vào Logcat với tag `BepNhaAI`.
- Tách lỗi theo bước (không tốn nhiều quota):
  `adb shell am instrument -w -e class vn.team03.bepnha.FirebaseLiveTest#diagnoseLatency -e firebaseDiag true [-e diagModel <tên>] vn.team03.bepnha.test/androidx.test.runner.AndroidJUnitRunner`,
  rồi xem `adb logcat -d -s BepNhaDiag`. `countTokens` đạt mà `generateContent` báo *high demand* hoặc chờ hết 60 giây là model đang quá tải phía Google, không phải lỗi cấu hình; thử lại sau.
- Thời gian chờ mỗi lần gọi mặc định 25 giây; đổi khi build bằng `-PassistantTimeoutMs=60000`.

Ghi nhận ngày 17/11/2026 (emulator, project `bep-nha-team03-20261116-21eb9`): App Check debug token được chấp nhận, `countTokens` khoảng 2,3 giây; `generateContent` với `gemini-3.8-flash` lúc báo *high demand*, lúc không phản hồi sau 60 giây; `gemini-3.6-flash` cũng không phản hồi; `gemini-2.5-flash` không còn cho người dùng mới. Chưa có lượt AI thật thành công cho bản agent.

Ghi nhận ngày 18/11/2026 (cùng emulator và project, model `gemini-3.5-flash-lite`): `generateContent` khoảng 1,7 giây. `AgentEvalTest` **23/23 đạt**, mỗi câu 2–8 giây. Gói miễn phí chỉ cho 15 lượt gọi mỗi phút, mà một câu có thể tốn tới 4 lượt, nên chạy cả bộ với `-e evalPauseMs 20000` (khoảng 8 phút).

## 3. Kịch bản thủ công trên điện thoại

Dùng chữ hệ thống 200% và bật TalkBack ít nhất một lần.

1. Mở **Hỏi Bếp Nhà**: nghe lời chào, kiểm tra đúng bữa sắp tới và đồ còn thiếu.
2. Màn Gia đình: đặt "Trợ lý gọi bạn là" = bác, tốc độ Chậm, bấm **Nghe thử**. Tích "Ít muối", dị ứng "Tôm". Lưu.
3. Bấm micro, nói "Lên thực đơn tuần này". Xem trước 14 bữa, bấm **Giữ bữa này như cũ** ở một bữa, nói "đồng ý".
4. Kiểm tra Thực đơn: không có món chứa tôm, mọi món ít muối. Đi chợ và Vào bếp đã cập nhật.
5. Nói "đánh dấu đã mua gạo" (làm ngay), rồi nói "hoàn tác".
6. Vào bếp → **Nấu cùng tôi**: nói "tiếp", "đọc lại", "hẹn giờ 5 phút". Màn hình không tắt. Với Gemini Live: bấm micro, hỏi "cá chín thì nhìn thế nào?", nói "xong rồi, tiếp đi", "hẹn giờ 5 phút", "tắt hẹn giờ"; bấm **Bước tiếp theo** (Gemini đọc bước); im lặng 60 giây (phiên tự đóng); bật chế độ máy bay rồi bấm micro (báo lỗi, chuyển về lệnh trên máy).
7. Nói "gửi danh sách đi chợ cả ngày mai cho con": bảng chia sẻ của điện thoại mở ra.
8. Đóng hẳn app, mở lại: lịch sử trò chuyện còn, không có đề xuất cũ nào bấm được.
9. Bật chế độ máy bay, hỏi lại "tối mai đổi món canh thành canh chua cá": trợ lý trên máy trả lời và ghi rõ lý do.

Ghi lại chỗ người lớn tuổi bị vướng (không hiểu nút, nói không được nhận, đọc quá nhanh) để chỉnh tiếp.

## Giới hạn còn lại

- AI chỉ chạy trên bản debug với thiết bị đã đăng ký App Check; bản release dùng trợ lý trên máy.
- Mỗi bữa vẫn đúng 3 món; chưa có bữa sáng; mỗi lúc chỉ một đồng hồ hẹn giờ.
- Định lượng, giá và tag của 46 món mới cần nhóm kiểm duyệt; hồ sơ ăn uống không thay cho lời khuyên của bác sĩ.
- Nhận giọng nói phụ thuộc dịch vụ nhận dạng tiếng Việt trên máy; máy không có giọng đọc tiếng Việt thì không đọc to được.
- Gemini Live là bản Preview: model, giọng và giới hạn free tier có thể đổi. Micro tạm tắt khi Gemini nói (không ngắt lời được). Kết nối bị server cắt khoảng 10 phút một lần; app tự mở phiên mới ở bước hiện tại.
- App tự thu/phát âm thanh thay cho `startAudioConversation` của firebase-ai 18.2.0, vì hàm đó để lại một luồng chạy 100% CPU sau mỗi phiên. `LiveSession.receive()` của SDK vẫn chiếm một nhân CPU trong lúc phiên đang mở (SDK TODO b/410059569); phiên tự đóng sau 60 giây im lặng nên chi phí có giới hạn.
