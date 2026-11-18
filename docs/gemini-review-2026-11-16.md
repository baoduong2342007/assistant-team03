# Bếp Nhà Mình — kiểm thử Gemini và rà soát chức năng

> **Tài liệu lịch sử.** Báo cáo này kiểm thử backend Python `src/bep_nha_api`, nay đã được gỡ. Các lệnh Python bên dưới không còn chạy được.

Ngày báo cáo: 16/11/2026. Model kiểm thử: `gemini-3.6-flash`.

## Kết quả chính

Tool calling đã hoạt động với API thật cho gợi ý mâm, đổi món, sửa tiếp theo ngữ cảnh, đổi khẩu phần và mở màn hình đi chợ. Đã sửa lỗi thực tế làm mất ngày “mai” khi trả lời câu hỏi làm rõ bằng câu dài, cùng các lỗi phát hiện qua kiểm thử hồi quy. Kiến trúc FastAPI → Gemini → kiểm tra đề xuất → Android xác nhận vẫn phù hợp với bản MVP hiện tại.

| Kiểm tra | Kết quả |
|---|---|
| Python toàn repo | 116 kiểm thử đạt; có một cảnh báo deprecation của Starlette TestClient |
| Android JVM | 24 kiểm thử đạt |
| APK debug | `assembleDebug` thành công |
| Android lint | `lintDebug` thành công, vẫn có cảnh báo sẵn có |
| Biên dịch kiểm thử thiết bị | `compileDebugAndroidTestKotlin` thành công |
| Chạy giao diện trên điện thoại/emulator | Chưa thực hiện; máy chưa có Emulator và system image |
| Ruff, kiểm tra whitespace | Đạt |

Các kiểm thử dùng dữ liệu bữa ăn giả lập. Không thay đổi thực đơn cá nhân, không ghi API key vào mã nguồn hoặc APK, không bật thanh toán hay triển khai dịch vụ công khai.

## Kiểm thử API thật

Đã gửi 26 yêu cầu `generateContent` trong hai lượt kiểm thử, ngoài lần kiểm tra danh sách model. Có 22 phản hồi HTTP 200, ba phản hồi 503 quá tải và một phản hồi 429 hạn mức. Không tiếp tục gọi API sau khi kết thúc nhóm yêu cầu có lỗi hạn mức; không chuyển sang model khác để thử tránh giới hạn.

| Đợt | Số kịch bản | Gọi Google thật | Kết quả kịch bản |
|---|---:|---:|---|
| Trước sửa | 16 | 13 | 14 đạt; một lỗi chọn ngày; một lỗi Google 503 |
| Sau sửa | 16 | 13 | 13 đạt; hai lỗi Google 503 và một lỗi 429 |

Mỗi đợt có ba kịch bản làm rõ/từ chối ngay ở backend, không gọi model. Ở đợt sau, **10/10 phản hồi Google trả HTTP 200 đều vượt qua kiểm tra hành vi**. Ba yêu cầu bị lỗi dịch vụ không được tính là đạt tính năng. Các ca tương ứng đã có phản hồi đúng ở đợt đầu, nhưng chưa có kết quả gọi mới thành công cho phiên bản sau sửa do hạn mức.

| Kịch bản sau sửa | Kết quả |
|---|---|
| Tủ lạnh có thịt heo, trứng, rau muống; tối mai nấu gì? | Đạt: hai mâm, đúng ngày/bữa, không có lệnh ghi dữ liệu |
| Đổi canh tối ngày mốt, giữ mặn và rau | Đạt: đúng một vị trí món |
| Cùng yêu cầu nhưng không dấu | Đạt |
| Đừng đổi mặn và rau, chỉ đổi canh | Đạt |
| Sửa đề xuất đang mở bằng “à đổi sang…” | Google 429; chưa kiểm thử lại trực tiếp được |
| Sửa tiếp sau lần đổi trước | Đạt: giữ đúng đích ngày/bữa |
| Không còn thịt heo và trứng, chỉ còn rau muống | Đạt: không tính đồ đã hết là đồ có sẵn |
| Gợi ý mâm tránh thịt heo và trứng | Google 503 ở đợt sau; đợt đầu đạt |
| Chỉ dùng trứng, không mua thêm | Đạt: báo thiếu nguyên liệu, không tạo mâm giả định đủ đồ |
| Đổi khẩu phần cả nhà thành 6 người | Đạt |
| Mở danh sách đi chợ | Google 503 ở đợt sau; đợt đầu đạt |
| Đổi món mặn thành phở bò chưa có trong catalog | Đạt: giải thích giới hạn và không đưa lệnh đổi món |
| Yêu cầu ngày mai nhưng chưa rõ trưa/tối | Đạt tại backend, không gọi Google |
| Trả lời câu làm rõ bằng câu dài | Đạt: giữ ngày mai, giữ món đã yêu cầu |
| Ngày ngoài cửa sổ bảy ngày | Đạt tại backend, không gọi Google |
| Yêu cầu bữa sáng | Đạt tại backend: giải thích hiện chỉ hỗ trợ trưa/tối |

Thời gian các phản hồi HTTP 200 ở đợt sau: trung vị khoảng **5,44 giây**, nhanh nhất **2,81 giây**, lâu nhất **21,24 giây**. Đây là đo trên fixture thực đơn tối giản, không đại diện cho một phiên điện thoại với toàn bộ checklist, lịch nấu và lịch sử chat dài. Tổng token Google báo cho các phản hồi thành công của cả hai đợt là 167.986; chưa quy đổi thành tiền.

Lỗi 429 ghi rõ hạn mức ngày của project/model là 20 lượt ở Free Tier và trả `retryDelay: 47007s` tại thời điểm lỗi. Giới hạn thực tế cần xem trong AI Studio; đây không phải một mức chung cố định cho mọi tài khoản. App đã được sửa để hiển thị thời gian chờ do dịch vụ trả về khi có, thay cho lời nhắc “chờ một lát”. Không cộng thời gian này vào thời điểm đọc báo cáo để suy ra giờ khôi phục hạn mức.

### Phạm vi phương pháp

Tiến trình Python trong môi trường làm việc không phân giải được tên miền Google. Vì vậy SDK Python tạo nguyên payload HTTP, kết nối HTTPS được thực hiện bằng runtime JavaScript, rồi phản hồi thực của Google được đưa trở lại SDK và hàm backend để kiểm tra. Payload không chứa header API key trong tệp ghi lại. Bộ replay kiểm tra payload hiện tại trùng với yêu cầu đã gửi trước khi sử dụng phản hồi.

Đây là kiểm thử Gemini thật kết hợp kiểm tra hợp đồng SDK/backend. Chưa phải kiểm thử từ APK qua một backend HTTPS đang chạy trên điện thoại. Bộ Python HTTP TestClient chạy với event loop riêng trong tiến trình kiểm thử để xử lý hạn chế socket cục bộ của môi trường Windows; Android JVM dùng JUnitCore qua Gradle để tránh Gradle worker socket. Không đổi cấu hình runtime của sản phẩm vì các hạn chế này.

## Các vấn đề đã sửa

| Vấn đề | Trước sửa | Sau sửa / cách kiểm tra |
|---|---|---|
| Mất ngày qua câu làm rõ dài | “Ngày mai muốn ăn canh chua cá” → “Bữa tối nhé, giữ nguyên món mặn và món rau giúp mình” có thể đổi bữa tối hôm nay | Giữ ngày/món trong câu trước; tái hiện và xác minh lại bằng Gemini thật |
| Kiểm tra câu không dấu thiếu nhất quán | Model có thể trả món hoặc vị trí sai mà backend không phát hiện; thứ trong tuần không dấu bị bỏ qua | Chuẩn hóa cho kiểm tra tên món, vị trí, câu phủ định và ngày trong tuần; chặn sai tên/vị trí bằng test |
| Đồng âm khi bỏ dấu | “một” có thể bị hiểu như “mốt”, “món này” như “hôm nay”, “đổi sang” như bữa sáng | Giữ cách xử lý riêng cho từ dễ nhầm; có ca hồi quy |
| Nhắc “món đó” nhưng sửa vị trí khác | Có thể nhận lệnh thay mặn khi đang tham chiếu canh | Kiểm tra vị trí theo đề xuất hoặc món đang mở |
| Lệnh phụ không được yêu cầu | Một lô đổi canh có thể kèm thay khẩu phần gia đình | Lệnh thay đổi bổ sung phải có yêu cầu tương ứng; vẫn cho phép yêu cầu ghép rõ ràng |
| Nguyên liệu đã hết còn được tính là có | “Không còn thịt heo, trứng”, “hết thịt lợn” hoặc “chỉ còn…” chưa phủ định đủ danh sách/ngữ cảnh cũ | Đối chiếu tên chuẩn, dùng trạng thái mới nhất, xử lý danh sách phủ định và danh sách chỉ còn |
| Tên nguyên liệu lồng nhau | “Cà chua” có thể bị nhận thành “cá”; “bông cải xanh” thành “cải xanh” | So khớp cụm tên dài trước, không lấy một phần tên nguyên liệu khác |
| Dấu đã có không phản ánh lượng mới | Đổi món làm tăng lượng thịt nhưng danh sách đi chợ vẫn tích đã có | Bỏ tích nguyên liệu cần nhiều hơn, loại mục không còn thuộc mâm, giữ phần không đổi và bữa khác |
| Chọn lại đúng món làm mất tiến độ | Vẫn xóa bước nấu đã xong dù thực đơn không đổi | Không thay trạng thái khi chọn đúng ID món hiện tại |
| Thẻ gợi ý ngụ ý đủ lượng | Biết có tên nguyên liệu nhưng chưa biết số lượng trong tủ | Hiển thị lượng cần theo khẩu phần và nhắc kiểm tra lượng thực tế |
| Lỗi dịch vụ bị gộp chung | Khóa/model, quá tải, hạn mức, timeout khó phân biệt; retry SDK có thể kéo dài chờ | Backend phân biệt lỗi, chỉ một lần gọi mỗi lượt; Android có thông báo 429/504, dùng `Retry-After` khi có |

Đã thêm 29 kiểm thử Python và ba kiểm thử JVM so với bản trước đợt rà soát này.

## Các cải thiện nên làm tiếp

Những mục dưới đây được xác định khi đọc mã và đối chiếu luồng sản phẩm; chưa được triển khai hoặc xác minh trên thiết bị trong đợt này.

| Ưu tiên | Điểm chưa hợp lý / giới hạn | Cải thiện đề xuất | Điều kiện nghiệm thu |
|---|---|---|---|
| 1 | APK hiện có `ASSISTANT_BASE_URL` rỗng | Chạy backend thử nội bộ có HTTPS, cấu hình URL khi build và chạy luồng thật trên thiết bị thử | Gợi ý → chọn → sửa → xác nhận → đóng/mở app giữ đúng món, đi chợ và nhắc nấu |
| 1 | Mâm gợi ý chưa được đính kèm thành dữ liệu riêng trong từng tin nhắn; ngữ cảnh gửi model chưa có toàn bộ mâm đang được đưa ra | Gắn mâm với ID lượt chat, gửi ID/ngày/bữa/món khi tham chiếu; quy định rõ lúc gợi ý cũ hết hiệu lực | Hỏi “mâm 2 khác mâm 1 thế nào”, đổi điều kiện rồi chọn mâm không lấy nhầm gợi ý cũ |
| 1 | Thẻ mâm cũ có thể còn khi một câu mới chỉ trả văn bản hoặc hỏi lại; điều kiện tủ lạnh mới không được gắn vào phiên bản thẻ | Thiết kế trạng thái vòng đời của gợi ý, phân biệt câu hỏi xem chi tiết với yêu cầu thay điều kiện; vô hiệu hóa thẻ không còn phù hợp | Không chọn được mâm vi phạm điều kiện mới; câu hỏi thuần thông tin vẫn cho chọn đúng mâm |
| 1 | Gợi ý chỉ xét tên nguyên liệu, chưa kiểm tra ngân sách/thời gian/số lượng tồn | Thêm trường ràng buộc và lượng/đơn vị; lọc bằng tính toán xác định, tái dùng quy tắc cost/minutes/định lượng của app | Yêu cầu bắt buộc không bị bỏ qua; thiếu dữ liệu thì hỏi lại; không nói “đủ đồ” chỉ vì trùng tên |
| 1 trước khi mở công khai | Endpoint hiện phục vụ thử nghiệm nội bộ, chưa có xác thực và hạn mức theo người dùng | Thêm xác thực ở gateway, giới hạn lượt/tần suất, theo dõi lỗi và token ở dạng không chứa key/nội dung riêng tư | Có giới hạn rõ ràng và không tự động gọi lặp khi hết hạn mức |
| 2 | Người dùng nói “lên thực đơn” dễ kỳ vọng cả ngày/tuần, nhưng mỗi lượt hiện chủ yếu một bữa | Mở thao tác theo nhóm bữa; xem trước từng ngày và xác nhận cả nhóm; cân nhắc giới hạn tám action hiện tại | Đổi nhiều bữa không cập nhật dở dang, mỗi bữa vẫn đúng nhóm món và ngày |
| 2 | Kho chỉ có 14 món, mâm bắt buộc mặn/canh/rau; chưa có dữ liệu dinh dưỡng có cấu trúc | Mở rộng catalog có nguồn và quyền sử dụng, chuẩn hóa nguyên liệu/đơn vị, đồng bộ một nguồn giữa Android và backend | Backend và APK cùng phiên bản catalog; không tạo ID/công thức giả |
| 2 | Gửi toàn catalog và nhiều ngữ cảnh mỗi lượt | Đo token trên snapshot điện thoại thật; rút ngữ cảnh về bữa/món liên quan, tóm tắt lịch sử có kiểm soát | Giảm token/thời gian mà vẫn giữ đúng tham chiếu và điều kiện; không bỏ dữ liệu cần kiểm tra |
| 2 | Hội thoại chỉ sống trong ViewModel | Lưu lịch sử và ngữ cảnh có phiên bản khi cần; đề xuất cũ phải được kiểm tra lại trước khi có thể xác nhận | Mở lại app xem được lịch sử nhưng không thực thi nhầm lệnh cũ |
| 3 | Chưa có bữa sáng, nhiều timer, bộ lọc đi chợ cả ngày và nhập giọng nói | Làm theo các giai đoạn riêng sau khi luồng một bữa được nghiệm thu trên thiết bị | Mở rộng mô hình dữ liệu/UI/nhắc nấu đồng bộ, có kiểm thử tương ứng |

Thứ tự khuyến nghị: nghiệm thu luồng hiện tại trên thiết bị → quản lý mâm gợi ý theo lượt chat → ràng buộc ngân sách/thời gian/tồn kho → nhiều bữa và catalog lớn hơn. Chưa cần đổi toàn bộ sang Firebase AI Logic hoặc Room chỉ để tool calling hoạt động; quyết định lưu trữ nên đi cùng phạm vi dữ liệu mới.

## Tệp và cách chạy lại

- Backend: `src/bep_nha_api/app.py`, `src/bep_nha_api/meal_planning.py`.
- Kiểm thử: `tests/test_bep_nha_review.py`, `scripts/test_gemini_live.py`.
- Android: `PlannerEngine.kt`, `AssistantGateway.kt`, `AssistantCommands.kt`, `AssistantScreen.kt` và các test JVM tương ứng.
- Dữ liệu kết quả tổng hợp không có key: `docs/gemini-live-review-2026-11-16.json`.
- APK: `android/app/build/outputs/apk/debug/app-debug.apk`.

Từ gốc repo, chỉ khi project có hạn mức trở lại, có thể chạy các ca cần kiểm tra lại:

```powershell
.\.venv\Scripts\python.exe scripts/test_gemini_live.py --case pending --case pantry_exclusion --case navigate
```

Script lấy `GEMINI_API_KEY` từ môi trường hoặc hỏi key bằng ô nhập ẩn trong terminal. Không cần dán key vào mã. Mặc định báo cáo nằm trong `.venv/gemini-live-report.json`; `--output` cho phép chọn tệp khác. Chạy toàn bộ mặc định có tối đa 16 kịch bản và dừng nếu API trả 429. Kết quả lỗi trả exit code 1 để không biến lỗi hạn mức thành kiểm thử đạt.

Các lệnh kiểm tra trong terminal thông thường:

```powershell
.\.venv\Scripts\python.exe -m pytest -q
cd android
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug compileDebugAndroidTestKotlin
```

Kiểm thử thiết bị cần emulator hoặc thiết bị thử riêng. Không chạy suite xóa dữ liệu thử nghiệm trên điện thoại đang chứa dữ liệu cần giữ. APK chưa được gắn URL backend chỉ có luồng tra cứu offline; API key không thay thế cấu hình URL này.

## Tài liệu đối chiếu

- [Gemini function calling](https://ai.google.dev/gemini-api/docs/function-calling)
- [Google GenAI Python SDK](https://googleapis.github.io/python-genai/)
- [Hạn mức Gemini và nơi xem hạn mức project](https://ai.google.dev/gemini-api/docs/rate-limits)
