# Lịch Việt 1.0.0 — kiểm kê và kế hoạch nâng cấp

Bản tiếp theo **1.0.1** đã có [đối chiếu MASTER PROMPT](docs/REQUIREMENTS_AUDIT_1.0.1.md)
và [báo cáo cập nhật/kiểm chứng](docs/RELEASE_1.0.1.md). Kế hoạch 1.0.0 bên dưới
được giữ làm lịch sử, không phải tuyên bố mọi mục của MASTER PROMPT đã hoàn tất.

## Hiện trạng trước nâng cấp

Ứng dụng là một project Android native Kotlin/Jetpack Compose với hai module `:core` và `:app`.
`core` tính lịch âm Việt Nam ngoại tuyến theo UTC+7 trong khoảng 1900–2100. `app` dùng
Navigation Compose, một `CalendarViewModel`, Room v1 cho bảng `events`, DataStore cho cài đặt,
WorkManager cho lời nhắc và RemoteViews cho bốn widget. Các màn lịch tháng/tuần, sự kiện,
ngày tốt, chuyển đổi âm dương, ngày lễ và tra Can Chi/con giáp đã hoạt động.

Các điểm mở rộng liên quan trực tiếp đến đặc tả 1.0.0:

| Thành phần | Mã hiện tại | Giới hạn cần xử lý |
| --- | --- | --- |
| Reminder | `app/reminder`, `EventRepository`, `CalendarRefreshReceiver` | Một mốc nhắc/sự kiện; WorkManager có thể trễ; chưa snooze/hoàn thành. |
| Event | `EventEntity`, `AppDatabase`, `Recurrence`, `BackupRepository` | Room v1; sự kiện một thời điểm, chưa thời lượng, màu, địa điểm, UID ICS. |
| Calendar | `CalendarScreen`, `App`, `CalendarViewModel` | Ngày có sự kiện chỉ có dấu chấm; chưa thanh sự kiện nhiều ngày. |
| Settings | `PreferencesRepository`, `SettingsScreen` trong `App.kt` | Chưa onboarding, ngôn ngữ riêng cho app, cấu hình nhắc chi tiết. |
| Localization | `res/values/strings.xml`, chuỗi trực tiếp trong Compose | Nội dung hiện chủ yếu tiếng Việt cố định. |
| Horoscope | `ui/tools/HoroscopeScreen.kt` | Tra ngày sinh và con giáp; chưa hồ sơ, lá số, engine Tử Vi Đẩu Số. |

`MainActivity.attachBaseContext` hiện ép locale Việt Nam, vì vậy chỉ thêm tài nguyên
`values-en` sẽ chưa kích hoạt được tiếng Anh. Một số chuỗi văn hóa cũng được trả trực tiếp
từ `:core`; ứng dụng cần ánh xạ theo mã định danh ổn định để bản dịch không dựa vào so
khớp câu tiếng Việt.

## Trình tự tích hợp

1. **Lời nhắc:** thêm AlarmManager và đường dự phòng; quyền exact alarm theo chính sách Android;
   nhiều mốc nhắc; Receiver kiểm tra trạng thái lưu trong Room; phục hồi khi khởi động, cập nhật,
   đổi thời gian/múi giờ. Migration đầu tiên giữ nguyên sự kiện hiện có. Kiểm tra trên emulator
   trước khi thay đổi cấu trúc sự kiện lần nữa.
2. **Lịch nâng cao:** migration tiếp theo bổ sung khoảng `[start, end)`, cả ngày, màu, địa điểm,
   thông tin ICS. Tách parser/writer iCalendar khỏi mô hình hiển thị. Nhập qua preview và giao
   dịch Room; mở rộng recurrence và bố cục lịch tháng/tuần. Sao lưu cũ vẫn đọc được.
3. **Onboarding và ngôn ngữ:** DataStore lưu trạng thái; bốn trang giới thiệu; chuyển chuỗi UI sang
   Android Resources cho tiếng Việt/Anh; cấu hình ngôn ngữ app và kiểm tra đổi ngôn ngữ tại chỗ.
4. **Tử vi:** chọn một bộ quy tắc Nam phái từ《Tử Vi Đẩu Số Toàn Thư》quyển 2; lưu nhiều hồ sơ đầu
   vào trên thiết bị; tính các phần có công thức và ví dụ đối chiếu; trình bày lá số 4×4, danh sách
   dễ đọc và nguồn của từng quy tắc. Thành phần không có nguồn/mẫu kiểm chứng được ghi rõ là
   chưa hỗ trợ, không dựng kết quả giả.
5. **Tích hợp:** kiểm thử migration từ Room v1; unit/instrumentation test; kiểm tra quyền, lifecycle,
   build release, màn hình nhỏ/tablet/dark mode. Chỉ đổi `versionName` thành `1.0.0` sau khi
   chức năng được tích hợp và kiểm chứng ở mức phù hợp.

## Nguyên tắc tương thích

- Không thay package name, không dùng destructive migration và không làm mất sự kiện v1.
- Trước khi có bảng con tham chiếu sự kiện, thay cách `INSERT(REPLACE)` hiện tại bằng cập nhật thực
  sự để tránh xóa ngầm lời nhắc hoặc ngoại lệ ICS.
- Khi nhắc chính xác chưa được cấp quyền, dùng đường dự phòng và hiển thị giới hạn cho người dùng.
- Các sự kiện âm lịch dùng calendar engine hiện có; thời điểm thông báo là `Instant`, tính từ
  giờ dân sự Việt Nam. Sự kiện nhập từ múi giờ khác cần giữ thông tin múi giờ riêng.
- Quy tắc lặp ICS Gregorian không được ép vào enum lặp âm lịch hiện tại nếu làm mất ý nghĩa.
- Dữ liệu lá số riêng tư lưu local; lúc chia sẻ phải có thao tác xác nhận rõ của người dùng.
- Nguồn Nam phái không xác định dứt khoát quy ước đổi ngày cho giờ Tý lúc 23:00.
  Hồ sơ phải lưu quy ước được người dùng chọn hoặc tạm từ chối giờ này; không tự suy đoán.
- `ComponentActivity` hiện tại không dùng AppCompat. Trên API 33+ có thể dùng `LocaleManager`;
  API 26–32 cần áp dụng locale lưu trong DataStore vào context rồi tái tạo Activity khi đổi.

## Nguồn cần giữ cùng mã

- Android exact alarms: <https://developer.android.com/develop/background-work/services/alarms/schedule>
- Chính sách exact alarm Google Play: <https://support.google.com/googleplay/android-developer/answer/13161072>
- iCalendar: <https://www.rfc-editor.org/rfc/rfc5545.html>
- iCal4j Android: <https://www.ical4j.org/android/>
- Tử Vi Nam phái, quyển 2: <https://zh.wikisource.org/zh-hant/紫微斗數全書/卷二>

Tài liệu này là kế hoạch, không phải danh sách tính năng đã hoàn thành. Kết quả của từng giai đoạn
được cập nhật riêng khi build và kiểm thử thực tế hoàn tất.

## Kết quả tích hợp ngày 23/10/2026

Đã tích hợp bốn phase trên project hiện có, Room v1→v4 không destructive
migration, version 1.0.0/10000. Gate cuối: 56 bài JVM, 38 bài Android đạt ở cả
cấu hình điện thoại và tablet ngang trên emulator API 37, Lint 0 lỗi/76 cảnh
báo, APK/AAB R8 build thành công. Có runtime smoke ICS trên release, kiểm tra
ảnh/PDF và một số màn ở font 200%. Gói và bằng chứng tại
[VALIDATION.md](VALIDATION.md); báo cáo phase ở [docs/](docs/).

Nghiên cứu nguồn đã xác định bộ tính cuối dùng ấn bản Minh Nam Dương Đường,
quyển 3, thay cho việc trộn quy tắc từ nhiều ấn bản. Khác biệt Tứ Hóa và hai
hiệu chỉnh bảng được công bố trong [core/HOROSCOPE_RULES.md](core/HOROSCOPE_RULES.md).
Phần chưa có nguồn/kiểm chứng không được tạo kết quả giả. Chưa chứng nhận
production trên mọi thiết bị; phạm vi thuật toán và QA còn cần được ghi rõ
trong báo cáo cuối. Không tự commit, push hoặc phát hành.
