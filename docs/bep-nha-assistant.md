# Trợ lý Gemini cho Bếp Nhà Android

## Chạy backend

Từ gốc repo, dùng Python 3.10+:

```powershell
.\.venv\Scripts\python.exe -m pip install -e '.[api]'
$env:GEMINI_API_KEY = '<khóa Gemini của nhóm>'
$env:GEMINI_MODEL = 'gemini-3.6-flash'
.\.venv\Scripts\python.exe -m uvicorn bep_nha_api.app:app --host 127.0.0.1 --port 8000
```

`GET /health` không gọi model. Backend không tự đọc `.env`; truyền biến môi trường qua tiến trình hoặc nền tảng chạy server. Không đưa khóa vào APK hay Git. Model có thể thay qua `GEMINI_MODEL`.

Để điện thoại kết nối, đặt backend sau HTTPS reverse proxy hoặc HTTPS tunnel của nhóm. Bản này dành cho thử nghiệm nội bộ: trước khi công khai endpoint, bổ sung kiểm soát truy cập và giới hạn lưu lượng ở gateway. Chưa có tài khoản, đồng bộ hay lưu hội thoại phía server.

## Build Android với địa chỉ backend

```powershell
cd android
.\gradlew.bat '-PassistantBaseUrl=https://your-backend.example' testDebugUnitTest assembleDebug lintDebug
```

Địa chỉ không chứa khóa, query string hoặc thông tin đăng nhập. Android chỉ dùng HTTPS và không đi theo redirect. Để trống cấu hình vẫn build/chạy offline; gửi AI sẽ báo chưa cấu hình và giữ câu hỏi để tra cứu offline. APK ở `android/app/build/outputs/apk/debug/app-debug.apk` tính từ gốc repo.

## Hợp đồng API và thực thi

`POST /api/assistant/turn` nhận:

```json
{"message":"Đổi món mặn thành đậu hũ", "history":[], "context":{"today":"2026-11-03","date":"2026-11-03","meal":"dinner","screen":"TODAY","people":4,"meals":[]}}
```

Android gửi snapshot đầy đủ từ PlannerEngine: thực đơn bảy ngày, giá/định lượng/lịch nấu, checklist, giờ ăn, món yêu thích, đồng hồ và món đang hỏi. Không gửi tên gia đình. `history` tối đa 20 tin, câu hỏi tối đa 1.000 ký tự. Backend đóng gói 14 công thức và dùng Gemini function calling, không chạy lệnh thay người dùng.

Phản hồi gồm `reply` và `actions`. Mỗi action luôn có `type`, `date` ISO và `meal` (`lunch`/`dinner`):

| type | Tham số bổ sung |
|---|---|
| navigate | screen: TODAY, PLAN, SHOPPING, COOK, FAMILY, FAVORITES |
| open_recipe | recipe_id |
| swap_recipe | index: 0 mặn, 1 canh, 2 rau; recipe_id cùng nhóm |
| set_people | people: 1–12, áp dụng toàn gia đình |
| set_meal_time | time: HH:mm, áp dụng mọi ngày cho loại bữa đó |
| set_reminder | enabled: boolean, bật/tắt nhắc theo timeline |
| start_timer | minutes: 1–180; label, thay đồng hồ duy nhất sau xác nhận |
| cancel_timer | không có |
| set_ingredient / set_task | key từ snapshot; enabled: trạng thái đích |
| set_favorite | recipe_id; enabled: trạng thái đích |

Android kiểm tra toàn bộ batch trước khi làm; lệnh thay đổi luôn có bản xem trước và xác nhận/hủy. Snapshot thay đổi thì đề xuất hết hiệu lực. Lệnh giữ ngày/bữa đích, không phụ thuộc màn đang xem. Bấm xác nhận tiêu thụ đề xuất một lần. Chỉ báo thành công sau khi lưu SharedPreferences và đồng bộ AlarmManager xong. Lỗi lưu được báo riêng; dữ liệu đang hiển thị trong bộ nhớ có thể chưa bền vững, cần mở lại ứng dụng để kiểm tra.

Hội thoại ở ViewModel: giữ qua mở/đóng trợ lý, xem công thức và xoay máy; không lưu sau khi tiến trình bị kết thúc. Khi lỗi dịch vụ, câu hỏi được giữ để sửa, thử lại hoặc tra cứu offline. Công thức offline cũng mở được từ màn thực đơn.

Catalog backend đóng gói tại `src/bep_nha_api/recipes.json`, được kiểm thử so khớp dữ liệu Android. Khi cập nhật công thức, cập nhật cả hai bản.

## Xác minh

```powershell
.\.venv\Scripts\python.exe -m pytest -q
cd android
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

Instrumentation cần emulator/thiết bị thử riêng. Kiểm tra thủ công trên 360dp, chữ hệ thống 200%, TalkBack, bàn phím; thử từ bốn tab, mở công thức rồi quay lại, xác nhận/hủy, đổi bữa trong khi chờ AI, quyền thông báo bị từ chối. Chưa có kết quả kiểm tra người dùng trung niên thực tế.

Xác minh ngày 03/11/2026: 69 kiểm thử Python và 17 unit test Android đạt; `assembleDebug`, `lintDebug` và `compileDebugAndroidTestKotlin` thành công. Wheel Python có backend cùng catalog. Đã kiểm tra cả cấu hình URL HTTPS khi build và cấu hình trống. 11 ca instrumentation được biên dịch, chưa chạy vì `adb devices` không có thiết bị. Chưa gọi Gemini thật hoặc kiểm chứng giao diện/TalkBack trên thiết bị. Lint còn cảnh báo ảnh `family_meal` không dùng sau khi bỏ banner.

Nguồn kỹ thuật: [Gemini function calling](https://ai.google.dev/gemini-api/docs/function-calling), [Google GenAI SDK](https://ai.google.dev/gemini-api/docs/libraries), [Compose accessibility](https://developer.android.com/develop/ui/compose/accessibility/api-defaults).
