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
| Chủ động | Lời chào tóm tắt bữa sắp tới. "Nấu cùng tôi" đọc từng bước, nhận lệnh "tiếp", "đọc lại", "hẹn giờ 10 phút". Thông báo nhắc nấu và nhắc đi chợ. |
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

Nhóm: `swap`, `plan`, `diet`, `pantry`, `app`, `clarify` (23 câu). Dùng `-e evalCase swap-casual` để chạy một câu. Bộ thử dừng khi hết quota. Câu do trợ lý trên máy trả lời được ghi `not-ai`, không tính là đạt. Báo cáo nằm ở `files/agent-eval.json` của app:

```powershell
adb shell run-as vn.team03.bepnha cat files/agent-eval.json
```

## 3. Kịch bản thủ công trên điện thoại

Dùng chữ hệ thống 200% và bật TalkBack ít nhất một lần.

1. Mở **Hỏi Bếp Nhà**: nghe lời chào, kiểm tra đúng bữa sắp tới và đồ còn thiếu.
2. Màn Gia đình: đặt "Trợ lý gọi bạn là" = bác, tốc độ Chậm, bấm **Nghe thử**. Tích "Ít muối", dị ứng "Tôm". Lưu.
3. Bấm micro, nói "Lên thực đơn tuần này". Xem trước 14 bữa, bấm **Giữ bữa này như cũ** ở một bữa, nói "đồng ý".
4. Kiểm tra Thực đơn: không có món chứa tôm, mọi món ít muối. Đi chợ và Vào bếp đã cập nhật.
5. Nói "đánh dấu đã mua gạo" (làm ngay), rồi nói "hoàn tác".
6. Vào bếp → **Nấu cùng tôi**: nói "tiếp", "đọc lại", "hẹn giờ 5 phút". Màn hình không tắt.
7. Nói "gửi danh sách đi chợ cả ngày mai cho con": bảng chia sẻ của điện thoại mở ra.
8. Đóng hẳn app, mở lại: lịch sử trò chuyện còn, không có đề xuất cũ nào bấm được.
9. Bật chế độ máy bay, hỏi lại "tối mai đổi món canh thành canh chua cá": trợ lý trên máy trả lời và ghi rõ lý do.

Ghi lại chỗ người lớn tuổi bị vướng (không hiểu nút, nói không được nhận, đọc quá nhanh) để chỉnh tiếp.

## Giới hạn còn lại

- AI chỉ chạy trên bản debug với thiết bị đã đăng ký App Check; bản release dùng trợ lý trên máy.
- Mỗi bữa vẫn đúng 3 món; chưa có bữa sáng; mỗi lúc chỉ một đồng hồ hẹn giờ.
- Định lượng, giá và tag của 46 món mới cần nhóm kiểm duyệt; hồ sơ ăn uống không thay cho lời khuyên của bác sĩ.
- Nhận giọng nói phụ thuộc dịch vụ nhận dạng tiếng Việt trên máy; máy không có giọng đọc tiếng Việt thì không đọc to được.
