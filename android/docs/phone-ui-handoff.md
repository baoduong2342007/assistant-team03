# Bếp Nhà: giao diện điện thoại và bàn giao backend

## Thiết kế và phạm vi

Android native, Kotlin/Jetpack Compose, minSdk 26, targetSdk 36. Đợt này tập trung điện thoại, không thêm giao diện tablet hoặc thay đổi backend.

- Bốn tab: Hôm nay, Thực đơn, Đi chợ, Vào bếp. Nút Hỏi Bếp Nhà cố định phía trên tab.
- Menu Thêm mở Gia đình, Nhắc nấu và Món đã lưu.
- Nội dung chính 18sp, chữ phụ/nhãn 16sp; nút chính tối thiểu 56dp. Không ghi đè font scale hệ thống.
- Nền #F7F8F5; chữ #293225; xanh #49613C; nền phụ #EDF1E7; chữ phụ #52604D.
- Cặp màu chữ được kiểm tra tự động ở mức tương phản tối thiểu 4,5:1.
- Nhãn tab và nút được xuống dòng. Khi thiếu chiều ngang, nhóm thao tác xếp dọc.
- Timer ở footer mở hộp chi tiết để tên món dài không chiếm toàn màn hình. Trong Chat, timer/ngữ cảnh chuyển vào vùng cuộn khi chữ lớn hoặc thiếu chiều cao.
- Ô nhập Gia đình và Chat có xử lý IME. Form giữ bản nháp khi xoay máy.
- Hội thoại trong phiên được giữ khi xoay máy hoặc xem công thức. Bản nháp Chat dùng SavedStateHandle; không lưu lịch sử lâu dài hoặc phục hồi đề xuất thực thi sau khi tiến trình kết thúc.

## Nối backend

Build với `-PassistantBaseUrl=https://your-backend.example`. Địa chỉ phải là HTTPS origin/base path, không chứa khóa, query string hoặc thông tin đăng nhập.

`AssistantGateway.ask` gọi `POST /api/assistant/turn`:

```json
{
  "message": "Đổi món mặn cho bữa này",
  "history": [{"role": "user", "text": "Món nào nấu nhanh?"}],
  "context": {}
}
```

`context` thực tế do `AssistantGateway.snapshot` tạo: ngày/bữa, số người, món đang hỏi, thực đơn tuần, định lượng, checklist, giờ ăn, lịch nấu, món đã lưu và timer. Không gửi tên gia đình. Giới hạn câu hỏi 1.000 ký tự, tối đa 20 tin lịch sử. Các lượt chưa có câu trả lời không được đưa vào history như lượt đã hoàn thành.

Phản hồi:

```json
{"reply":"Bạn có thể đổi sang đậu hũ sốt cà chua.","actions":[]}
```

Giữ hợp đồng action hiện có trong [tài liệu trợ lý](../../docs/bep-nha-assistant.md); mỗi action có type, date và meal. Metadata UI (id tin nhắn, nguồn tra cứu, lượt chờ/thất bại) không thêm vào payload.

- Không thay đổi UI để nối một backend mới cùng hợp đồng.
- Không đặt khóa Gemini trong APK.
- Một lượt gửi đang chạy khóa nút Gửi nhưng cho phép soạn câu tiếp.
- Thử lại giữ cùng tin người dùng, không ghi đè bản nháp mới.
- Đổi bữa/món hoặc rời Chat làm yêu cầu cũ hết hiệu lực; phản hồi đến muộn không được thực thi.
- Mọi batch action được kiểm tra trước; thay đổi dữ liệu cần xem trước và xác nhận một lần.
- Chỉ báo đã lưu sau khi persistence/lịch nhắc hiện có hoàn thành.
- Khi chưa cấu hình URL, nút chính là Tìm công thức; kết quả có nhãn Tra cứu trên máy. Đây không phải phản hồi mô hình AI.
- Khi lỗi dịch vụ, người dùng có thể thử lại hoặc tìm cùng câu hỏi trong dữ liệu đóng gói.
- Hỏi từ công thức truyền đúng món; Quay lại mở lại công thức đó.
- Không đưa chế độ trả lời AI giả vào sản phẩm. Fixture chỉ nằm ở preview debug và kiểm thử.

## Kiểm chứng kỹ thuật

Kết quả ngày 06/11/2026: build debug và lint thành công, **18 unit test và 26 instrumentation test trên máy ảo API 37 đạt**. Có ca hồi quy cho vùng nhập ngang 640 × 140dp ở chữ 200% và phản hồi backend chứa thao tác không hợp lệ. Dữ liệu được giữ nguyên khi từ chối đề xuất không hợp lệ.

Lệnh qua Gradle/Android Studio:

```text
:app:assembleDebug :app:testDebugUnitTest :app:lintDebug
:app:connectedDebugAndroidTest
```

Chạy instrumentation trên máy ảo dành cho thử nghiệm: NativeAppTest xóa dữ liệu Bếp Nhà trước từng ca. Không chạy trên điện thoại đang chứa thực đơn cần giữ.

Giữ Compose BOM/AGP hiện có. Thư viện instrumentation dùng Espresso 3.7.0, runner 1.7.0 và JUnit extension 1.3.0. Espresso mới sửa việc gọi InputManager bằng reflection, theo [release notes AndroidX Test](https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0).

- `AccessibilityContrastTest`: màu chữ trên nền thực tế.
- `AssistantFlowTest`: bốn lối vào, xoay máy, quay lại công thức, bản nháp, retry, gửi trùng, bỏ phản hồi cũ và xác nhận một lần.
- `PhoneAccessibilityTest`: khung 320dp/chữ 200%, viewport thấp, nút gửi/timer, gợi ý chỉ điền ô nhập, đọc đề xuất dài và giữ vị trí khi có tin mới.
- `PhonePreviews.kt` (debug): 320/360/412dp, chữ 100/130/200%, ngang 150%, câu trả lời/lỗi/đề xuất.
- Các kiểm thử domain/persistence/luồng native cũ tiếp tục chạy.

Ảnh do instrumentation tạo trong external files của ứng dụng, thư mục `native-previews`. Chạy bằng Android Studio/Gradle có thể gỡ APK và xóa ảnh sau khi kết thúc. Để thu ảnh, cài bằng các task `installDebug installDebugAndroidTest`, chạy AndroidJUnitRunner qua adb, rồi pull thư mục ảnh trước khi gỡ app.

## Giới hạn nghiệm thu

Các test kích thước/chữ lớn dùng Compose density override hoặc DeviceConfigurationOverride.ForcedSize, không đại diện mọi bàn phím hoặc hãng điện thoại. Preview viewport thấp không mô phỏng toàn bộ bàn phím Android.

Đã kiểm tra trực tiếp trên máy ảo API 37: nhập câu hỏi bằng Gboard, xoay ngang với chữ hệ thống 200%, tìm công thức, điều hướng cử chỉ và ba nút. Khi chiều cao quá thấp, thanh trên nhường chỗ cho ô nhập; composer chuyển sang một hàng và tạm bỏ nhãn nổi. Nút tìm rút thành “Tìm”, vẫn có mô tả accessibility đầy đủ. Gửi ở vùng thấp sẽ đóng bàn phím để đọc kết quả. Sau kiểm tra đã khôi phục font scale, xoay màn hình và kiểu điều hướng ban đầu của máy ảo.

Ảnh màn ngang với IME thật: [cử chỉ](phone-ui/real-landscape-ime-text200.png), [ba nút](phone-ui/real-landscape-ime-text200-threebutton.png). Các ảnh fixture và ảnh thực được phân biệt trong [danh mục ảnh](phone-ui/README.md).

Chưa xác minh giọng đọc/thao tác TalkBack thực tế, điện thoại vật lý hoặc các bàn phím khác. Chưa cấu hình endpoint nên không coi AI thật đã được kiểm chứng. Thử với năm người dùng trung niên là bước nghiệm thu sản phẩm riêng; chưa có kết quả sử dụng thực tế.

APK debug mới được tạo ở `app/build/outputs/apk/debug/app-debug.apk`. Không dùng APK cũ trong thư mục releases để đánh giá UI mới.
