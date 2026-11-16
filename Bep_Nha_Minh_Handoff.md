# Bếp Nhà Mình — bàn giao triển khai Gemini và hướng phát triển

Cập nhật ngày 16/11/2026. Phạm vi hiện tại là trợ lý tiếng Việt để gợi ý và chỉnh từng bữa trong thực đơn bảy ngày, hỗ trợ bữa trưa và tối.

## Trạng thái mã nguồn

- Repo `baoduong2342007/assistant-team03`. Nhánh local `backend-testing` và ref theo dõi `origin/backend-testing` cùng ở commit `88f71d8` (`test: validate Gemini meal planning and fix regressions`).
- Chưa xác minh có pull request từ `backend-testing` vào `main`. Tra cứu GitHub không tìm thấy kết quả cho nhánh này. [Trang so sánh để mở PR](https://github.com/baoduong2342007/assistant-team03/compare/main...backend-testing?expand=1).

## Đã triển khai

- **Backend.** FastAPI nhận câu chat, ngữ cảnh thực đơn và lịch sử qua `POST /api/assistant/turn`. Gemini 3.6 Flash chọn `suggest_meals` để gợi ý hoặc `propose_actions` để đề xuất thao tác. Backend xác thực ngày, bữa, vị trí món, ID công thức và tham số trước khi chuyển về Android. Catalog hiện có 14 món.
- **Gợi ý mâm.** Xếp tối đa hai mâm theo nguyên liệu có sẵn, nguyên liệu cần tránh và yêu cầu chỉ dùng đồ đang có. Thẻ hiển thị lượng cần theo khẩu phần. Kiểm tra tồn kho hiện dựa trên tên nguyên liệu.
- **Chỉnh qua chat.** Người dùng chọn mâm, xem trước, sửa từng món rồi xác nhận hoặc hủy. Món khác trong đề xuất được giữ khi chỉ sửa một vị trí. Sau xác nhận, app cập nhật thực đơn, danh sách đi chợ và lịch nấu. Đề xuất gắn với trạng thái thực đơn lúc tạo và chỉ áp dụng một lần.
- **Sửa lỗi hồi quy.** Giữ ngày đã nói khi hỏi lại trưa/tối. Xử lý câu không dấu, câu phủ định, “món đó”, nguyên liệu đã hết và tên nguyên liệu dễ nhầm. Đổi món làm tăng lượng cần thì bỏ dấu “đã có” để kiểm tra lại. Chọn đúng món hiện tại giữ tiến độ nấu.
- **Lỗi dịch vụ.** Phân biệt hết hạn mức, quá tải và timeout. Android hiển thị thời gian chờ nếu Google trả về. Khóa Gemini cấu hình ở tiến trình backend.

## Kết quả kiểm thử

| Hạng mục | Kết quả |
| --- | --- |
| Python | 116 ca đạt |
| Android JVM | 24 ca đạt |
| APK và lint | Build debug, lint và biên dịch instrumentation thành công |
| Gemini API thật | 26 yêu cầu qua hai đợt. Sau sửa đạt 13/16 kịch bản, gồm 10/10 phản hồi HTTP 200 đạt kiểm tra hành vi và 3 ca xử lý cục bộ. Ba ca còn lại gặp hai lỗi 503 và một lỗi 429. |
| Android trên thiết bị hoặc emulator | Chưa chạy luồng đầu cuối. APK đã build đang có URL backend trống, dùng được chế độ tra cứu offline. |

Ba ca nên chạy lại khi có hạn mức là sửa đề xuất đang mở, gợi ý mâm tránh nguyên liệu và mở danh sách đi chợ.

## Thứ tự phát triển tiếp theo

| Ưu tiên | Việc cần làm | Điều kiện nghiệm thu |
| --- | --- | --- |
| 1 | Cấu hình backend HTTPS nội bộ, build APK có URL và nghiệm thu trên điện thoại hoặc emulator | Gợi ý → chọn → sửa → xác nhận → mở lại app. Kiểm tra đúng món, định lượng đi chợ và lịch nhắc. |
| 2 | Gắn mỗi mâm với lượt chat và điều kiện tạo mâm | “Mâm 2 khác mâm 1 thế nào?” tham chiếu đúng. Khi thay nguyên liệu hoặc điều kiện, thẻ cũ không còn được áp dụng nhầm. |
| 3 | Thêm ngân sách, thời gian nấu và số lượng tồn theo đơn vị | Lọc theo ràng buộc bằng dữ liệu app. Nếu thiếu thông tin hoặc không có mâm phù hợp, hỏi lại hoặc giải thích rõ. |
| 4 | Mở rộng thực đơn nhiều bữa, nhiều ngày và kho món | Xem trước thay đổi theo từng bữa, xác nhận theo nhóm. Android và backend dùng cùng phiên bản catalog. |
| Trước khi mở công khai | Thêm xác thực, giới hạn yêu cầu, thống kê lỗi và token | Đo được mức sử dụng, giới hạn theo người dùng và giữ khóa ở backend. |

Sau khi luồng một bữa ổn định, có thể tối ưu lượng ngữ cảnh gửi Gemini, lưu hội thoại có phiên bản và bổ sung bữa sáng, danh sách đi chợ cả ngày hoặc nhiều bộ hẹn giờ.

## Tệp và lệnh bàn giao

- Backend và bộ chọn mâm nằm trong `src/bep_nha_api/app.py` và `src/bep_nha_api/meal_planning.py`.
- Luồng Android nằm trong `PlannerViewModel.kt`, `AssistantGateway.kt`, `AssistantCommands.kt` và `AssistantScreen.kt`. Cập nhật checklist và lịch nấu nằm trong `PlannerEngine.kt`.
- Chi tiết thiết lập và kiểm thử ở `docs/bep-nha-assistant.md` và `docs/gemini-review-2026-11-16.md`. Số liệu theo ca ở `docs/gemini-live-review-2026-11-16.json`.
- APK debug ở `android/app/build/outputs/apk/debug/app-debug.apk`.

Từ gốc repo, chạy kiểm thử Python và ba ca Gemini còn pending khi quota cho phép.

```powershell
.\.venv\Scripts\python.exe -m pytest -q
.\.venv\Scripts\python.exe scripts/test_gemini_live.py --case pending --case pantry_exclusion --case navigate
```

Từ thư mục `android`, thay URL ví dụ bằng backend HTTPS của nhóm.

```powershell
.\gradlew.bat '-PassistantBaseUrl=https://your-backend.example' testDebugUnitTest assembleDebug lintDebug
```
