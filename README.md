# Lịch Việt 1.0.1

Ứng dụng Android native bằng Kotlin và Jetpack Compose: lịch âm dương Việt Nam,
sự kiện và ngày giỗ, lời nhắc, widget và lá số Tử Vi theo một bộ quy tắc có nguồn.
Các phép tính và dữ liệu cá nhân hoạt động trên thiết bị. Ứng dụng không cần tài
khoản, backend hay quyền Internet.

Source đặt `versionName = 1.0.1`, `versionCode = 10001`. Bản này đã qua gate
tích hợp trên emulator API 37; chưa phải gói Google Play đã ký bằng khóa phát hành.
Kết quả build và kiểm thử cuối cùng được ghi tại [VALIDATION.md](VALIDATION.md).

V1.0.1 thêm Ngày Văn hóa Việt Nam 24/11 từ năm 2026, số tuần ISO, tìm ngày
bằng ô nhập, công tắc giờ hoàng đạo/hiệu ứng và cache lịch có giới hạn.
Chưa đủ toàn bộ MASTER PROMPT; xem
[đối chiếu yêu cầu](docs/REQUIREMENTS_AUDIT_1.0.1.md) và
[báo cáo 1.0.1](docs/RELEASE_1.0.1.md).

## Tính năng

- **Lịch âm dương offline:** lịch tháng/tuần, chọn ngày/năm, chuyển âm–dương
  hai chiều, tháng nhuận, Can Chi, tiết khí tại trưa Việt Nam, giờ hoàng đạo và
  lễ Tết. Calendar engine dùng UTC+7 cố định, hỗ trợ ngày dương 1900–2100.
- **Sự kiện:** tạo/sửa/xóa, ghi chú, địa điểm, sinh nhật, ngày giỗ âm lịch, cả
  ngày hoặc nhiều ngày, màu palette/mã HEX. Khoảng thời gian dùng `[start, end)`;
  ngày kết thúc của sự kiện cả ngày không được tính vào sự kiện. Thanh nhiều
  ngày nối qua hàng tuần và có chỉ báo khi không đủ chỗ hiển thị.
- **Lặp sự kiện:** ngày, tuần, tháng, năm dương hoặc năm âm; chọn tháng thường,
  tháng nhuận hoặc cả hai. Ngày 30 gặp tháng thiếu có thể dời về ngày cuối
  tháng hoặc bỏ qua.
- **Lời nhắc:** tối đa 16 mốc độc lập mỗi sự kiện, từ đúng giờ đến nhắc trước
  nhiều ngày; hoãn 5/10/15 phút và hoàn thành lần xuất hiện. AlarmManager dùng
  nhánh chính xác khi được cấp quyền, nhánh inexact khi thiếu quyền;
  WorkManager đối soát trạng thái nền. Hệ thống khôi phục lịch nhắc sau reboot,
  cập nhật ứng dụng và đổi ngày/giờ/múi giờ. Âm/rung chỉnh qua kênh thông báo
  của Android. Chi tiết ở [Phase 1](docs/PHASE1_REMINDERS.md).
- **iCalendar/ICS:** chọn tệp hoặc mở ICS từ ứng dụng khác; xem trước từng UID,
  bỏ qua/nhập/cập nhật trùng UID bằng giao dịch Room. iCal4j đọc và ghi nội dung,
  timezone, lặp và ngoại lệ được hỗ trợ; giữ canonical ICS cho chuỗi nhập.
  Xuất một/nhiều/tất cả sự kiện và chia sẻ qua Android Sharesheet. Lịch lặp âm
  dùng phần mở rộng `X-LICHVIET-*` và các ngày xuất hiện hữu hạn cho ứng dụng
  khác. Phạm vi tương thích và giới hạn ở
  [Phase 2](docs/PHASE2_ADVANCED_CALENDAR.md).
- **Bốn widget native:** hôm nay, ngày âm, lịch tháng và sự kiện sắp tới.
  Widget tháng có ngày âm và dấu sự kiện; lịch sắp tới dùng cùng bộ mở rộng
  occurrence với ứng dụng, kể cả ICS và sự kiện đang kéo dài qua nhiều ngày.
  Chạm widget mở đúng ngày.
- **Tra ngày theo khoảng:** bốn mục đích khai trương, cưới hỏi, chuyển nhà,
  xuất hành; mỗi kết quả nêu lý do theo bộ quy ước dân gian được công bố.
- **Onboarding và cá nhân hóa:** bốn trang giới thiệu, bỏ qua/xem lại; chọn
  tiếng Việt hoặc English, sáng/tối/hệ thống, đầu tuần và hiển thị âm lịch.
  Thiết lập nhắc là lựa chọn rõ ràng của người dùng. Xem
  [Phase 3](docs/PHASE3_ONBOARDING_I18N.md).
- **Tử Vi:** lưu nhiều hồ sơ với biệt danh, ngày âm/dương, tháng nhuận, giờ sinh
  và quy ước đổi ngày lúc 23:00; lá số 4×4 có chọn cung, zoom/pan và chế độ danh
  sách. Các phép an cung/sao, Cục và đại hạn/tiểu hạn thuộc bộ quy tắc đã chọn;
  có tra tên cung/sao, nguồn quy tắc và so sánh thông tin tính toán giữa hồ sơ.
  Xuất PNG/PDF hoặc chia sẻ cần xác nhận dữ liệu cá nhân. Phạm vi chính xác ở
  [Phase 4](docs/PHASE4_HOROSCOPE.md) và
  [quy tắc calculation engine](core/HOROSCOPE_RULES.md).
- **Tablet và lưu dữ liệu:** bố cục theo chiều rộng khả dụng, lịch/chi tiết hai
  cột, Navigation Rail, chế độ toàn màn hình; dữ liệu Room, cài đặt DataStore.
  Sao lưu/khôi phục JSON bằng trình chọn tệp Android, hỗ trợ định dạng cũ.
  Khôi phục thay thế và xóa dữ liệu đều có xác nhận.

## Kiến trúc và dữ liệu

```text
core/                          Kotlin/JVM thuần, không phụ thuộc Android
  .../VietnameseCalendar.kt     Lịch Việt, typed ID cho nội dung văn hóa
  .../Astronomy.kt              Mô hình thiên văn và UTC+7
  .../Almanac.kt                Quy ước tra ngày có lý do
  .../horoscope/               Calculation engine và bộ quy tắc Tử Vi
app/
  .../CalendarViewModel.kt      Flow, SavedStateHandle, thao tác theo lifecycle
  .../data/                    Room, DataStore, recurrence, backup, mutation lock
  .../reminder/                AlarmManager, notification, receiver, đối soát
  .../ics/                     iCal4j codec, occurrence/layout engine, SAF, preview
  .../horoscope/               Hồ sơ Room, UI lá số, tra cứu, PNG/PDF
  .../widget/                  RemoteViews AppWidget và cập nhật nền
  .../ui/                      Compose design system, lịch, sự kiện, onboarding
  .../ui/CalendarText.kt        Ánh xạ typed ID sang resource VI/EN
  .../AppLocaleManager.kt       Ngôn ngữ riêng cho ứng dụng
  schemas/                     Room schema 1, 2, 3, 4
  src/test/                    Kiểm thử JVM
  src/androidTest/             Room migration, hệ thống Android và Compose
docs/                          Báo cáo từng phase
artifacts/                     Gói build, ảnh và báo cáo đã lưu
```

Hilt cấp repository cho ViewModel; widget, lời nhắc và lịch dùng chung dữ liệu
sự kiện và calendar engine. Thao tác nhiều bảng dùng giao dịch Room; thao tác
ghi/khôi phục phối hợp qua mutex. Không tạo dữ liệu mẫu trong cơ sở dữ liệu
production. Hồ sơ Tử Vi lưu đầu vào, kết quả được tính lại từ rule-set có ID.

| Migration | Thay đổi |
| --- | --- |
| 1 → 2 | Thêm cả ngày và bảng nhiều mốc nhắc; chuyển mốc nhắc cũ sang bảng con |
| 2 → 3 | Thời điểm kết thúc, địa điểm, màu, UID/canonical ICS và khóa occurrence của lời nhắc |
| 3 → 4 | Thêm hồ sơ Tử Vi, giữ sự kiện và lời nhắc |

Không dùng destructive migration. JSON backup v5 chứa sự kiện, mốc nhắc,
cài đặt được hỗ trợ và hồ sơ; vẫn đọc v1–v4. Bản sao lưu cũ không có hồ sơ sẽ
giữ hồ sơ hiện tại. Lựa chọn ngôn ngữ và trạng thái onboarding được lưu riêng
trên thiết bị, không phải một lịch đồng bộ đám mây.

## Mở và build

Yêu cầu Android Studio hỗ trợ AGP 9.4, JDK 21, Android SDK Platform 37 và Build
Tools 36.0.0. Thiết bị chạy ứng dụng cần Android 8.0/API 26 trở lên.

1. Mở thư mục repository trong Android Studio.
2. Tạo `local.properties` trỏ tới Android SDK trên máy, ví dụ
   `sdk.dir=D\:\\code\\Android\\Sdk`. Tệp này không đưa vào Git.
3. Gradle Sync, chọn thiết bị/emulator rồi Run module `app`.

PowerShell:

```powershell
.\build.ps1
.\build.ps1 :app:connectedDebugAndroidTest
.\build.ps1 :app:assembleRelease :app:bundleRelease
```

`build.ps1` tự tìm một số đường dẫn JDK trên máy phát triển hiện tại; trên máy
khác đặt `JAVA_HOME` tới JDK 21. Có thể dùng Gradle wrapper trực tiếp:

```sh
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:assembleRelease :app:bundleRelease
```

Sau build, APK cài thử ở `app/build/outputs/apk/debug/app-debug.apk`; APK release
chưa ký ở `app/build/outputs/apk/release/app-release-unsigned.apk`; AAB ở
`app/build/outputs/bundle/release/app-release.aab`. Bản release cần keystore
riêng để phát hành; repository không chứa khóa hoặc mật khẩu phát hành.
Gói v1.0.1 đã lưu trong [artifacts/](artifacts/), kèm
[SHA-256](artifacts/SHA256SUMS-1.0.1.txt). Các gói `0.1.0` và `1.0.0` là lịch sử;
không dùng chúng để đánh giá source 1.0.1.

`device.ps1` dành cho QA trên `emulator-5554`: cài/mở app, chụp ảnh, đọc log,
đổi kích thước. Ví dụ `./device.ps1 Resize 1600x1000 200`; sau đó `ResetSize`
để trả cấu hình mặc định. Script không nằm trong APK.

| Dependency/công cụ | Phiên bản ghim |
| --- | --- |
| Gradle / Android Gradle Plugin | 9.6.0 / 9.4.1 |
| Kotlin / Compose compiler | 2.2.10 / 2.2.10 |
| Compose BOM | 2025.08.00 |
| minSdk / compileSdk / targetSdk | 26 / 37 / 37 |
| JDK chạy Gradle / bytecode | 21 / 17 |
| Hilt / KSP | 2.59.2 / 2.3.6 |
| Room / DataStore / WorkManager | 2.8.4 / 1.1.7 / 2.10.1 |
| Navigation / Lifecycle | 2.9.3 / 2.9.3 |
| Coroutines / iCal4j | 1.10.2 / 4.4.0 |
| AndroidX Test runner / Espresso | 1.7.0 / 3.7.0 |

AGP 9 dùng Kotlin tích hợp. Các phiên bản được ghim để build lặp lại được,
không phải tuyên bố chúng là phiên bản mới nhất. Giấy phép và attribution nằm
trong [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md),
[notices của calendar core](core/THIRD_PARTY_NOTICES.md) và mục giấy phép trong
ứng dụng. Nguồn/mức kiểm chứng lịch tại [core/README.md](core/README.md).

## Kiểm chứng và giới hạn

- Gate 1.0.1: **59 kiểm thử JVM và 41 kiểm thử Android đạt**, full suite
  Android đạt cả ở cấu hình điện thoại và tablet ngang trên emulator API 37.
  Lint debug **0 lỗi, 76 cảnh báo**; release APK/AAB R8 build thành công và
  luồng nhập ICS đã chạy trên bản release ký debug để QA. Kết quả/bằng chứng
  và giới hạn kiểm chứng ở [VALIDATION.md](VALIDATION.md).
- Lịch trước năm 1968 dùng hồi tố UTC+7 hiện đại, không tái dựng lịch sử từng
  miền. Tiết khí là giá trị tại trưa, không phải API thời điểm chuyển tiết
  chính xác. Sai số và các fixture độc lập được mô tả trong tài liệu core.
- Ngày tốt và Tử Vi là hệ thống văn hóa truyền thống. Không có nội dung dự
  đoán chắc chắn về sức khỏe, tài chính hay tương lai. Chưa có full-chart
  golden độc lập cho toàn bộ lá số của ấn bản đã chọn; độ sáng sao, nhiều phụ
  tinh và giờ mặt trời/lịch sử nằm ngoài phạm vi được công bố.
- Lời nhắc chính xác phụ thuộc quyền và điều kiện Android. Nhánh inexact,
  widget và công việc đối soát có thể trễ trong Doze/tiết kiệm pin. Không thể
  bảo đảm giao thông báo khi máy tắt, ứng dụng bị force-stop hoặc thông báo
  bị chặn. Sự kiện native và âm lịch dùng giờ Việt Nam; ICS giữ timezone nguồn.
- ICS hỗ trợ tập thuộc tính/phép lặp được công bố, không bao trùm mọi ứng dụng
  iCalendar. VTIMEZONE nhúng phải khớp timezone của thiết bị trong 1900–2100;
  custom TZID/định nghĩa khác hoặc thiếu lịch sử bị từ chối. `EXRULE`, `RECURRENCE-ID;RANGE` và lặp dưới một ngày bị từ
  chối; một số VALARM được giữ để xuất lại nhưng chưa được lên lịch. Một lần
  mở rộng tối đa 367 ngày và thời lượng sự kiện tối đa 366 ngày. Không có đồng
  bộ tài khoản hoặc chỉnh sửa occurrence ngoại lệ ICS trong bản này.
- Sao lưu JSON và ảnh/PDF chia sẻ có thể chứa dữ liệu cá nhân đọc được. Người
  dùng chọn nơi lưu; ứng dụng không tự tải lên. Room và DataStore không có
  giao dịch nguyên tử xuyên hai kho khi mất nguồn giữa lúc khôi phục.
- Đã xem một số màn ở font 200%, VI sáng/English tối và tablet ngang. Chưa
  kiểm chứng đầy đủ trên thiết bị thật/API 26, mọi launcher/foldable,
  TalkBack, mọi luồng font scale và multi-window; chưa benchmark pin, FPS hay cold start.
  Mã hỗ trợ và một bài smoke test không thay thế các kiểm định này.
- Trước khi phát hành Google Play cần hoàn tất ký gói, QA thiết bị thật,
  store listing, chính sách quyền riêng tư và biểu mẫu Data safety. Repository
  không tự phát hành hoặc tải gói lên cửa hàng.
