# Trợ lý Gemini cho Bếp Nhà Android

> **Tài liệu lịch sử của backend Python.** APK hiện dùng Firebase AI Logic/Spark và xử lý Kotlin. Hướng dẫn đang áp dụng: [Firebase Spark demo](firebase-spark-demo.md). Các lệnh backend và `assistantBaseUrl` bên dưới chỉ phục vụ đối chiếu bản cũ, không cấu hình APK hiện tại.

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

Khi sửa một đề xuất, Android gửi thêm `context.pending_menu_changes` chứa ngày, bữa, vị trí và ID các món đang xem trước. Sau khi xác nhận, `context.last_menu_changes` cho phép hiểu câu tiếp nối như “À đổi sang canh cải thịt bằm”. Trợ lý dùng ngày từ snapshot tại múi giờ Việt Nam để quy đổi “mai”, “ngày mai nữa” hoặc “ngày mốt”.

Phản hồi gồm `reply`, `actions` và `suggestions`. Android chấp nhận phản hồi cũ chỉ có hai trường đầu. `propose_actions` tạo danh sách thao tác như trước. Mỗi action luôn có `type`, `date` ISO và `meal` (`lunch`/`dinner`):

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

### Gợi ý và chỉnh mâm qua chat

`suggest_meals` nhận `date`, `meal` và `pantry_items`. Backend đối chiếu nguyên liệu với câu chat, xếp hạng mâm theo nguyên liệu tận dụng được và số nguyên liệu còn thiếu. Tool hỗ trợ `exclude_ingredients` cho tên nguyên liệu có trong kho và `pantry_only` khi người dùng yêu cầu chỉ dùng đồ đã có. Phản hồi gợi ý trả `actions: []` cùng tối đa hai mâm. Mỗi phần tử `suggestions` gồm `id` từ 1 đến 2, `date`, `meal`, `recipe_ids` theo thứ tự mặn/canh/rau và `pantry_keys` theo định dạng `tên|đơn vị`.

1. Gửi “Tủ lạnh có thịt heo, trứng và rau muống. Tối mai nấu gì?”. Thẻ mâm cho biết bữa, các món, nguyên liệu tận dụng được và lượng cần thêm theo khẩu phần gia đình.
2. Nhấn **Dùng mâm 1** hoặc gửi “Lấy mâm 1”. App mở bản xem trước của mâm trong lượt gợi ý gần nhất.
3. Nhấn **Sửa đề xuất** và gửi “Đổi món canh thành canh chua cá”. App giữ các thay đổi đã đề xuất ở vị trí món mặn và rau, rồi mở bản xem trước đã sửa.
4. Nhấn **Xác nhận thay đổi**. Mở đúng ngày và bữa ở màn Thực đơn, rồi kiểm tra Đi chợ và Vào bếp. Nếu bữa đó có lịch nhắc, giờ nhắc theo timeline mới.

Hội thoại ở ViewModel: giữ qua mở/đóng trợ lý, xem công thức và xoay máy; không lưu sau khi tiến trình bị kết thúc. Khi lỗi dịch vụ, câu hỏi được giữ để sửa, thử lại hoặc tra cứu offline. Công thức offline cũng mở được từ màn thực đơn.

Catalog backend đóng gói tại `src/bep_nha_api/recipes.json`, được kiểm thử so khớp dữ liệu Android. Khi cập nhật công thức, cập nhật cả hai bản.

Khi đổi món, những nguyên liệu không còn thuộc mâm hoặc có lượng cần tăng thêm sẽ được bỏ dấu đã có để kiểm tra lại. Chọn lại đúng món hiện tại không xóa tiến độ nấu. Thẻ gợi ý chỉ đối chiếu loại nguyên liệu đã nêu; lượng hiển thị là lượng cần theo khẩu phần, chưa phải số lượng tồn kho đã xác minh.

Mỗi lượt gửi chỉ thực hiện một lần gọi Gemini, timeout SDK 25 giây và không tự retry. Backend phân biệt lỗi hạn mức (429), phản hồi không hợp lệ (502), dịch vụ/cấu hình (503) và chờ quá lâu (504). Với 429, chuyển tiếp thời gian chờ hợp lệ từ `RetryInfo` thành `Retry-After`; Android hiển thị thời gian này nếu có, không mặc định rằng hạn mức ngày sẽ hồi phục sau vài giây.

## Xác minh

```powershell
.\.venv\Scripts\python.exe -m pytest -q
cd android
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

Instrumentation cần emulator/thiết bị thử riêng. Kiểm tra thủ công trên 360dp, chữ hệ thống 200%, TalkBack, bàn phím; thử từ bốn tab, mở công thức rồi quay lại, xác nhận/hủy, đổi bữa trong khi chờ AI, quyền thông báo bị từ chối. Chưa có kết quả kiểm tra người dùng trung niên thực tế.

Xác minh ngày 03/11/2026: 69 kiểm thử Python và 17 unit test Android đạt; `assembleDebug`, `lintDebug` và `compileDebugAndroidTestKotlin` thành công. Wheel Python có backend cùng catalog. Đã kiểm tra cả cấu hình URL HTTPS khi build và cấu hình trống. 11 ca instrumentation được biên dịch, chưa chạy vì `adb devices` không có thiết bị. Chưa gọi Gemini thật hoặc kiểm chứng giao diện/TalkBack trên thiết bị. Lint còn cảnh báo ảnh `family_meal` không dùng sau khi bỏ banner.

Xác minh gợi ý và chỉnh mâm ngày 16/11/2026. 87 kiểm thử Python và 21 unit test Android đạt. `assembleDebug`, `lintDebug` và `compileDebugAndroidTestKotlin` thành công. APK debug đã được xác minh chữ ký. Luồng Gemini được kiểm tra với phản hồi mô phỏng qua Google GenAI SDK. Kiểm thử trên thiết bị và gọi Gemini thật cần môi trường Android thử nghiệm và backend HTTPS có khóa API.

Nguồn kỹ thuật: [Gemini function calling](https://ai.google.dev/gemini-api/docs/function-calling), [Google GenAI SDK](https://ai.google.dev/gemini-api/docs/libraries), [Compose accessibility](https://developer.android.com/develop/ui/compose/accessibility/api-defaults).

Rà soát với API thật ngày 16/11/2026: 116 kiểm thử Python và 24 kiểm thử JVM đạt; APK, lint và biên dịch instrumentation thành công. Các lỗi ngày, tên không dấu, nguyên liệu và đồng bộ checklist đã được sửa. Đợt kiểm tra sau sửa có 10 phản hồi Google thành công đều đúng hành vi, ba ca xử lý cục bộ đạt và ba lượt lỗi dịch vụ (hai 503, một 429). Chi tiết phương pháp, giới hạn và cải thiện tiếp theo ở [báo cáo kiểm thử](gemini-review-2026-11-16.md).

Chạy bộ API tùy chọn bằng `.\.venv\Scripts\python.exe scripts/test_gemini_live.py`. Script dùng dữ liệu giả lập, lấy key từ môi trường hoặc ô nhập ẩn, không lưu key và dừng khi gặp 429. Dùng `--case` để kiểm thử riêng một tình huống khi hạn mức thấp.
