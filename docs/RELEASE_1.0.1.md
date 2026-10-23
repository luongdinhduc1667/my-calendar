# Lịch Việt 1.0.1 — cập nhật và kiểm chứng

Ngày: 23/10/2026. Version name 1.0.1, version code 10001. Giữ package
vn.lichviet.app, minSdk 26, Room schema 4 và các dependency hiện có.

## Thay đổi

- Thêm **Ngày Văn hóa Việt Nam 24/11**, từ 2026. Một HolidayId dùng chung
  engine/grid/chi tiết/ngày lễ/widget, tên VI/EN. Không thêm hồi tố vào 2025,
  không tự tạo lịch nghỉ bù/hoán đổi. Nguồn và đối chiếu prompt ở
  [REQUIREMENTS_AUDIT_1.0.1.md](REQUIREMENTS_AUDIT_1.0.1.md).
- Thêm công tắc số tuần ISO 8601, giờ hoàng đạo và hiệu ứng chuyển động trong
  DataStore. Số tuần có năm tuần trong mô tả accessibility; trường hợp tuần
  53/giao năm có test. Khi tuần bắt đầu Chủ Nhật, hàng dùng tuần ISO của thứ
  Năm trong hàng, không coi Chủ Nhật là ngày đầu tuần ISO.
- Tùy chọn lễ áp dụng cả thông tin ngày và semantics ô ngày. Ẩn giờ hoàng đạo
  bỏ sáu khung giờ khỏi chi tiết. Tắt hiệu ứng áp dụng cho chuyển route,
  chuyển trang onboarding, đổi cỡ nội dung/chọn ngày và chọn cung lá số;
  các tương tác kéo/ripple/spinner của hệ thống vẫn có thể hoạt động.
- Nút kính lúp mở picker ở chế độ nhập ngày theo locale, chọn kết quả vào
  đúng lịch tháng/ngày. Search sự kiện hiện có được giữ nguyên.
- Khởi tạo bảng lịch thiên văn trên Dispatchers.Default trước khi hiển thị
  lịch; cache dữ liệu ngày có giới hạn 512 entry, đồng bộ đa luồng, chỉ lưu
  LunarDate/HolidayId, không giữ Context hoặc chuỗi dịch. Grid và danh sách
  lễ tái sử dụng cùng dữ liệu; màu event dot được nhớ theo cửa sổ/occurrence.
- Thanh event nhiều ngày tăng chiều cao theo font scale; đánh dấu cả thứ Bảy
  và Chủ Nhật. Không đưa ra chỉ số FPS hoặc phần trăm nhanh hơn khi chưa đo.

## Tương thích dữ liệu

Không đổi bảng Room, không destructive migration, không xóa sự kiện/hồ sơ.
Ba khóa DataStore mới có mặc định: số tuần tắt, giờ hoàng đạo bật, hiệu ứng bật.
Backup **v5** lưu ba khóa mới; vẫn đọc v1–4, dùng các mặc định trên nếu thiếu
vì định dạng cũ. V4 vẫn giữ hồ sơ; v1–3 vẫn giữ hồ sơ hiện tại khi backup không
có trường profiles. V5 kiểm tra boolean đúng kiểu, không ép chuỗi thành boolean.
V1.0.0 không đọc được backup v5 mới; giữ backup cũ nếu cần quay về bản cũ.

## Gate thực tế

| Kiểm tra | Kết quả |
| --- | --- |
| JVM core | 27 bài đạt, 0 lỗi; thêm 3 bài cho lễ/ISO/cache đa luồng |
| JVM app | 32 bài đạt, 0 lỗi |
| Full Android, phone API 37 | 41 bài đạt, 0 failure/error/skipped |
| Full Android, tablet API 37, 1600×1000/density 200 | 41 bài đạt, 0 failure/error/skipped |
| Lint debug | 0 lỗi, 76 cảnh báo; không baseline hoặc tắt lỗi |
| APK/AAB release | Build thành công với R8/resource shrinking |

Ba bài Android tăng thêm: một bài backup cũ/mới và kiểu dữ liệu, một journey
thật tìm 24/11 → tên lễ → settings → recreation → số tuần/ẩn giờ/ẩn lễ,
một bài widget English dùng RemoteViews thật. Các kiểm thử 1.0.0 còn lại chạy
lại đầy đủ, gồm Room migration, notifications, ICS và lá số/PNG/PDF.

XML lưu tại [artifacts/reports/v1.0.1/](../artifacts/reports/v1.0.1/).
Ảnh tại artifacts/v101-phone.png, v101-culture-day.png và v101-tablet.png.
Bản sao APK release ký debug để QA đã cài/chạy thực tế: ngày 24/11, bật số
tuần và preview ICS đúng UID trùng đều hoạt động; không có crash AndroidRuntime
trong lần smoke. Đã xem lịch với số tuần ở font 200% trên phone/tablet; tablet
chuyển một cột, phone có cuộn ngang để giữ chữ dễ đọc. Ảnh thêm ở
artifacts/v101-week-numbers.png, v101-tablet-font200.png,
v101-phone-font200.png và v101-release-ics.png. 524 mục tài nguyên dịch ở mỗi
bộ VI/EN, không thiếu tên. summary.json lưu kết quả kiểm tra gói cuối.

Lệnh chạy:

    .\build.ps1 :core:test :app:testDebugUnitTest :app:lintDebug
    .\build.ps1 :app:connectedDebugAndroidTest '-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true'
    .\build.ps1 :app:assembleRelease :app:bundleRelease

## Các tệp chính

- core/.../VietnameseCalendar.kt, CalendarRelease101Test.kt: lễ mới, cache,
  warm-up và định danh tuần ISO.
- app/.../data/PreferencesRepository.kt, BackupRepository.kt: tùy chọn và
  backup v5; CalendarViewModel.kt/MainActivity.kt: chuẩn bị core ở nền.
- ui/CalendarScreen.kt, CalendarText.kt, App.kt: tìm ngày, tuần ISO,
  semantics lễ, settings, chiều cao event bar.
- ui/Theme.kt, OnboardingScreen.kt, horoscope/HoroscopeChartView.kt: tắt
  hiệu ứng không thiết yếu theo cài đặt.
- ui/tools/HolidaysScreen.kt: tái sử dụng cache theo ngày.
- res/values*/release_101.xml: tài nguyên mặc định/VI/EN.
- Release101JourneyTest, CultureDayWidgetTest, BackupCodecTest: Android QA.
- app/build.gradle.kts: 1.0.1/10001; device.ps1: shortcut ngày lễ cho QA.

## Gói

- [APK cài thử](../artifacts/LichViet-1.0.1-debug.apk), ký debug.
- [APK release chưa ký](../artifacts/LichViet-1.0.1-release-unsigned.apk).
- [AAB release](../artifacts/LichViet-1.0.1-release.aab), cần ký phát hành.
- [SHA-256](../artifacts/SHA256SUMS-1.0.1.txt).

Không tự commit, push hoặc publish. Bản sao artifacts/qa/release-smoke.apk ký
bằng debug keystore chỉ phục vụ QA, không phải gói phát hành.

## Phần chưa đủ prompt và QA còn cần

V1.0.1 **không đồng nghĩa đã đủ toàn bộ MASTER PROMPT**. Danh sách đầy đủ trong
[báo cáo đối chiếu](REQUIREMENTS_AUDIT_1.0.1.md): tử vi 12 con giáp/ngày-tuần-
tháng-năm, nạp âm/tuổi hợp-xung, một số bộ phong thủy/mục đích tra ngày,
Can Chi giờ và các UX riêng vẫn thiếu hoặc chỉ có một phần. Không tạo dự đoán
ngẫu nhiên để lấp các mục này.

Chưa chạy API 26–32/máy thật, mọi launcher/foldable/multi-window/TalkBack,
process death/Doze/OEM dài hạn; chưa benchmark pin/FPS/cold start. Lịch lịch sử,
VTIMEZONE, giới hạn background và engine Tử Vi giữ các giới hạn đã công bố ở
[VALIDATION.md](../VALIDATION.md) và báo cáo phase. Phát hành cửa hàng còn cần
keystore riêng, chính sách riêng tư/Data safety/store listing và QA máy thật.
