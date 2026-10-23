# Kiểm chứng Lịch Việt

Bản mới nhất **1.0.1**: [kết quả build/test và gói](docs/RELEASE_1.0.1.md),
[đối chiếu đầy đủ MASTER PROMPT](docs/REQUIREMENTS_AUDIT_1.0.1.md).

59 bài JVM, 41 bài Android đạt ở cả phone/tablet emulator API 37; Lint 0 lỗi,
76 cảnh báo; APK/AAB R8 build thành công. XML mới ở artifacts/reports/v1.0.1/.
Nội dung bên dưới giữ bằng chứng của bản 1.0.0 trước khi cập nhật.

# Lịch Việt 1.0.0 — 23/10/2026

## Build và môi trường

Windows, JDK 21, Gradle 9.6.0, AGP 9.4.1, SDK 37, Build Tools 36.0.0.
Ứng dụng native Kotlin/Compose, versionCode 10000, versionName 1.0.0, minSdk 26.
Các gate dưới đây được chạy thực tế, không suy ra từ việc chỉ biên dịch test.

    .\build.ps1 :core:test :app:testDebugUnitTest :app:lintDebug
    .\build.ps1 :app:connectedDebugAndroidTest '-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true'
    .\build.ps1 :app:assembleRelease :app:bundleRelease

Giữ APK sau instrumentation chỉ phục vụ QA emulator để xem tệp xuất và cài
bản release smoke; không thay đổi hành vi APK bàn giao.

## JVM: 56 đạt, 0 lỗi

| Suite | Số bài | Phạm vi |
| --- | ---: | --- |
| VietnameseCalendarTest | 12 | 73.414 ngày khứ hồi, tháng nhuận, Can Chi, lễ, typed ID |
| AstronomicalFixturesTest | 3 | Bảng tháng độc lập, sóc NASA, lịch Việt/Trung 1985 |
| HoroscopeChartEngineTest | 9 | An cung/sao, Cục, Tứ Hóa, 23:00, fixture có nguồn |
| RecurrenceTest | 8 | Lặp dương/âm, tháng nhuận, ngày 30, cuối tháng, 29/2 |
| ReminderPlannerTest | 7 | Mốc nhắc, occurrence, lặp, giờ Việt Nam |
| IcsCodecTest | 10 | Recurrence/ngoại lệ, canonical round trip, alarm, extension, DST |
| EventLayoutEngineTest | 2 | Thanh nhiều ngày, overlap, hai lane, +N |
| HoroscopeProfileValidationTest | 5 | Ngày/giờ, tháng nhuận, kiểu dữ liệu, giới hạn |

24 bài thuộc core, 32 thuộc app. XML được sao chép vào
[artifacts/reports/v1-integration/](artifacts/reports/v1-integration/).

Fixture thiên văn cũ vẫn đạt: 730 ngày 2026–2027 đối chiếu bảng tháng; 25 mốc
sóc lệch tối đa 29,081 giây so với bảng NASA làm tròn tới phút. Mốc đông chí
1984 lệch 5,994 phút trong mô hình kinh độ Mặt Trời đơn giản, vẫn giữ đúng ngày
dân dụng. Đây là sai số fixture đã chọn, không phải bảo đảm trên toàn dải.
Nguồn tại [core/README.md](core/README.md).

## Android: 38 đạt trên emulator API 37

Full suite không có filter lớp: 38 bài, 0 failure, 0 error, 0 skipped.
Bằng chứng điện thoại:
[android-api37.xml](artifacts/reports/v1-integration/android-api37.xml).
Sau chỉnh bố cục chữ lớn/ngày lễ, toàn bộ 38 bài chạy lại ở cấu hình tablet
ngang 1600×1000/density 200 cũng đạt, 0 lỗi:
[android-tablet-api37.xml](artifacts/reports/v1-integration/android-tablet-api37.xml).

| Nhóm | Số bài | Kiểm chứng |
| --- | ---: | --- |
| CalendarStateTest | 1 | Trạng thái lịch |
| BackupCodecTest | 2 | Backup, validation |
| ReminderDeliveryTest | 1 | Claim atomic, chống giao trùng |
| ReminderMigrationTest | 2 | V1/V2 → hiện tại giữ sự kiện, reminder, trạng thái, FK |
| RoomPersistenceTest | 1 | Lưu/đọc dữ liệu thật |
| ChartExportTest | 1 | PNG 1600×2900, PDF A4 hai trang, FileProvider, render PDF thật |
| HoroscopeChartUiTest | 2 | 12 cung chọn được, editor và validation |
| HoroscopeJourneyTest | 1 | Tạo, recreation, sửa/xóa hồ sơ qua MainActivity/Room |
| HoroscopeProfilePersistenceTest | 3 | Nhiều hồ sơ, migration 3→4, backup |
| IcsRuntimeTest | 3 | Timezone/DST, UTF-8, UID, rollback, VTIMEZONE sai |
| AlarmReminderIntegrationTest | 3 | Alarm cập nhật/hủy/fallback, notification không trùng, boot/timezone |
| CalendarNavigationTest | 4 | Ngày âm, đổi tháng, các tab, converter |
| CalendarTextLocalizationTest | 3 | VI/EN, date pattern, tháng nhuận, lý do Almanac |
| EventJourneyTest | 1 | CRUD sự kiện qua UI |
| MultipleReminderEditorTest | 1 | Nhiều mốc nhắc qua recreation và lưu |
| OnboardingJourneyTest | 2 | Skip bền, đổi VI→EN, restart, xem lại |
| WidgetSelectionTest | 1 | Ngày chọn qua recreation với intent widget |
| WidgetLocalizationTest | 2 | English, ICS/nhiều ngày, end exclusive |
| WidgetRenderingTest | 4 | RemoteViews thật, dữ liệu Room |

Receiver boot/timezone được gọi trực tiếp để kiểm tra khôi phục từ dữ liệu lưu;
đây không phải thử tắt/bật máy thật hoặc Doze dài hạn. Fixture không được đóng
gói thành dữ liệu mẫu của ứng dụng.

## Lint, release và kiểm tra trực quan

- Lint debug: **0 lỗi, 76 cảnh báo**, không baseline/tắt lỗi. Chủ yếu UseKtx,
  phiên bản dependency, thuộc tính API mới có fallback, resource chưa dùng và
  plurals candidate; còn cỡ chữ widget nhỏ, icon launcher, overdraw và dấu chấm
  trong XML. XML lưu trong thư mục báo cáo.
- APK/AAB release dùng R8 và resource shrinking đã build thành công. Chỉ bỏ
  cảnh báo hai lớp không bắt buộc đã kiểm tra: ZoneRulesProvider không công
  khai trên Android (iCal4j có fallback) và annotation ToString compile-only
  của threeten-extra. Không bỏ toàn bộ cảnh báo thư viện.
- Đã ký **bản sao QA** bằng debug keystore có sẵn, cài cập nhật APK release
  minified, mở fixture ICS qua ACTION_VIEW/FileProvider, thấy preview đúng,
  nhập rồi mở lại nhận UID trùng với lựa chọn Update. Không có crash
  AndroidRuntime trong lần smoke. Ảnh: artifacts/v1-release-ics.png.
- Đã xem PNG và cả hai trang PDF từ fixture English/biệt danh dài: đủ 12 cung,
  tên sao, đại hạn, chi tiết phụ tinh, Tứ Hóa trên PDF và ghi chú nguồn/phạm vi;
  không thấy chữ bị cắt trong fixture. [Tệp xuất QA](artifacts/horoscope-qa/).
- Đã xem lịch điện thoại, tablet ngang 1600×1000/density 200, VI sáng và
  English tối. QA cỡ chữ 200% phát hiện và sửa chiều rộng lưới/rail; khi cần
  lịch cuộn ngang hoặc chuyển một cột để đọc chữ lớn. Đây là kiểm tra một số
  màn thực tế, không bao phủ mọi luồng ở mọi cỡ chữ.
- Resource: 514 string/plural/string-array có thể dịch trong mỗi bộ mặc định,
  VI và EN, không thiếu tên. AAB tắt language split để đổi VI/EN offline.
  Chi tiết kỹ thuật lỗi ICS giữ nguyên nội dung gốc sau nút mở riêng.
- Manifest release merged không có INTERNET, quyền đọc toàn bộ bộ nhớ hoặc
  USE_EXACT_ALARM. Có POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED,
  SCHEDULE_EXACT_ALARM; WorkManager thêm wake lock, network state, foreground
  service và quyền receiver nội bộ.

## Gói bàn giao

- [LichViet-1.0.0-debug.apk](artifacts/LichViet-1.0.0-debug.apk): cài thử, ký debug.
- [LichViet-1.0.0-release-unsigned.apk](artifacts/LichViet-1.0.0-release-unsigned.apk): release tối ưu, **chưa ký phát hành**.
- [LichViet-1.0.0-release.aab](artifacts/LichViet-1.0.0-release.aab): cần ký trước phát hành.
- [SHA256SUMS-1.0.0.txt](artifacts/SHA256SUMS-1.0.0.txt): đối chiếu đúng gói.

artifacts/qa/release-smoke.apk chỉ là bản sao ký debug để QA, không phải gói
phát hành. Gói 0.1.0 và báo cáo phase trước được giữ làm lịch sử.

## Giới hạn và QA còn cần trước phát hành cửa hàng

- Chỉ có emulator API 37: chưa runtime API 26–32, máy thật, foldable,
  multi-window, đầy đủ launcher hoặc TalkBack; chưa benchmark pin/FPS/cold
  start hay nền dài hạn. Smoke test không chứng nhận hiệu năng/accessibility.
- Exact alarm phụ thuộc quyền/Android/OEM. Không bảo đảm khi tắt máy,
  force-stop hoặc chặn thông báo. Sound/vibration theo channel Android.
  Xem [Phase 1](docs/PHASE1_REMINDERS.md).
- ICS: VTIMEZONE nhúng phải khớp timezone của thiết bị trong 1900–2100;
  custom TZID, định nghĩa khác/thiếu lịch sử bị từ chối. EXRULE, RANGE, lặp
  dưới một ngày, một số VALARM chưa hỗ trợ. Không sync tài khoản/editor ngoại
  lệ. Xem [Phase 2](docs/PHASE2_ADVANCED_CALENDAR.md).
- Lịch dùng UTC+7 hồi tố trước 1968; tiết khí tại trưa không thay API thời điểm
  chuyển tiết chính xác. Ngày tốt và Tử Vi là thông tin văn hóa.
- Tử Vi tính bộ có nguồn của ấn bản Minh Nam Dương Đường, có hai hiệu chỉnh
  bảng công bố, chưa có full-chart golden độc lập. Không gán độ sáng sao,
  phụ tinh chưa xác minh, giờ mặt trời hoặc lời dự đoán ngẫu nhiên.
  Xem [quy tắc](core/HOROSCOPE_RULES.md), [Phase 4](docs/PHASE4_HOROSCOPE.md).
- Backup/ảnh/PDF có thể chứa dữ liệu cá nhân đọc được. Người dùng chọn lưu và
  chia sẻ. Room/DataStore không có transaction xuyên hai kho nếu mất nguồn
  giữa restore; chưa kiểm thử mất nguồn thật.
- Phát hành Play cần keystore riêng, QA máy thật, store listing, chính sách
  quyền riêng tư và Data safety. Không tự commit, push hoặc publish.
