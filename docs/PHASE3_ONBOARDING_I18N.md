# Phase 3 — Onboarding và ngôn ngữ

## Phạm vi đã tích hợp

Onboarding Compose có bốn trang: chào mừng, lịch âm dương, sự kiện/nhắc nhở và
cá nhân hóa. Người dùng có thể vuốt, đi tiếp/quay lại, chạm chỉ báo trang hoặc
bỏ qua; Back ở trang sau đi về trang trước. Các trang cuộn và giới hạn chiều
rộng nội dung cho màn hình lớn. Trạng thái hoàn tất lưu DataStore; màn hình
chờ đọc trạng thái tránh hiện thoáng qua onboarding khi mở lại ứng dụng.
Trong Cài đặt có thể xem lại giới thiệu.

Trang cá nhân hóa cho chọn VI/EN, sáng/tối/hệ thống, thứ Hai/Chủ Nhật đầu tuần
và hiển thị âm lịch. Lựa chọn thiết lập nhắc mặc định tắt; bỏ qua không tự yêu
cầu quyền. Khi người dùng bật rồi hoàn tất, host tiếp tục luồng thiết lập nhắc
và giải thích quyền của ứng dụng.

`AppLocaleManager` dùng framework `LocaleManager` trên Android 13 trở lên.
Trên API 26–32, lựa chọn được ghi vào kho bootstrap nhỏ, áp dụng qua scoped
Context và tạo lại Activity. Widget dùng `localizedContext`; đổi ngôn ngữ yêu
cầu cập nhật widget và tên kênh thông báo. Cài đặt giao diện vẫn dùng
DataStore. Ngôn ngữ mặc định là tiếng Việt; hỗ trợ lựa chọn English rõ ràng.

`CalendarText` định dạng ngày/tháng theo resource của ngôn ngữ ứng dụng, định
dạng giờ theo cả ngôn ngữ và lựa chọn 12/24 giờ của hệ thống. Can Chi, tiết khí,
ngày lễ, mục đích, trực và lý do Almanac được ánh xạ từ typed ID của core.
Giao diện không suy luận ý nghĩa bằng so khớp nhãn tiếng Việt. API trả nhãn
Việt cũ của core được giữ cho tương thích với caller cũ.

Đổi âm–dương, tra ngày, lễ Tết và widget dùng tài nguyên VI/EN. Chuỗi có số
lượng dùng plurals ở nơi cần thiết. Khoảng trắng trước hậu tố tháng nhuận được
giữ bằng cú pháp resource có quote. Tên sao Tử Vi truyền thống giữ cách gọi
Việt trong English; giải thích và nhãn thao tác có bản dịch riêng.

Widget tháng/sắp tới dùng cùng `EventOccurrenceEngine` với lịch để hiển thị
ICS và sự kiện đang kéo dài qua nhiều ngày. Widget sắp tới nhận thời điểm
hiện tại thật và loại lịch đã kết thúc trong ngày; kiểm thử truyền clock cố
định. Dấu sự kiện tháng có màu và phương án tăng tương phản; mô tả hỗ trợ đọc
màn hình có ngày âm và số sự kiện. Chạm vẫn mở đúng ngày trong ứng dụng.

## Tệp chính

- Host/cài đặt: `app/src/main/java/vn/lichviet/app/MainActivity.kt`,
  `AppLocaleManager.kt`, `CalendarViewModel.kt`, `data/PreferencesRepository.kt`,
  `ui/App.kt`, `ui/OnboardingScreen.kt`.
- Nội dung lịch: `ui/CalendarText.kt`,
  `ui/tools/{ToolComponents,ConverterScreen,AuspiciousScreen,HolidaysScreen}.kt`,
  `widget/CalendarWidgets.kt`; các đường dẫn thuộc
  `app/src/main/java/vn/lichviet/app/`.
- Core: `core/src/main/kotlin/vn/lichviet/core/{VietnameseCalendar,Almanac}.kt`;
  thêm typed ID cho Can Chi, giờ, tiết khí, ngày lễ và đánh giá Almanac.
- Resource: `app/src/main/res/values/` và `values-en/` cho ứng dụng,
  onboarding, lịch/công cụ/widget, sự kiện, ICS và Tử Vi;
  `res/xml/locales_config.xml`; `res/layout/widget_calendar.xml`, `widget_cell.xml`.
- Kiểm thử: `LaunchPreferencesRule`, `ui/OnboardingJourneyTest`,
  `ui/CalendarTextLocalizationTest`, `widget/WidgetRenderingTest`,
  `widget/WidgetLocalizationTest` trong `app/src/androidTest/java/vn/lichviet/app/`;
  typed-ID regression trong `core/.../VietnameseCalendarTest.kt`.

Không thêm dependency hay Room migration riêng cho Phase 3. Bảng hồ sơ và
schema v4 thuộc Phase 4. Trạng thái onboarding và ngôn ngữ lưu ngoài Room.

## Kết quả đã chạy ngày 23/10/2026

| Gate | Kết quả thực tế |
| --- | --- |
| `OnboardingJourneyTest` | 2/2 đạt trên emulator API 37; do root chạy |
| `CalendarTextLocalizationTest` | 3/3 đạt trên cùng emulator |
| `WidgetRenderingTest` | 4/4 đạt; apply và layout RemoteViews thật |
| `WidgetLocalizationTest` | 2/2 đạt; chọn English thật, ICS và nhiều ngày |
| `:core:test` | Thành công; XML tại thời điểm gate có 24 bài, 0 lỗi; tác vụ up-to-date |
| Tên resource trong ba cặp `calendar_text`, `tool_strings`, `widget_strings` | VI/EN lần lượt 52/52, 62/62 và 23/23; không thiếu tên |

Hai bài onboarding xác nhận bỏ qua được lưu sau khi tạo lại Activity, chuyển
VI → EN ngay tại trang cá nhân hóa, hoàn tất, xem lại từ Settings và giữ ngôn
ngữ sau khi tạo lại Activity. Chín bài tiếp theo kiểm tra date pattern, khoảng
trắng tháng nhuận, các lý do có typed ID, widget lấy dữ liệu Room, nhãn English,
ICS, sự kiện đang diễn ra và quy ước ngày kết thúc exclusive.

Lần đầu `WidgetLocalizationTest` có một assertion kỳ vọng hai khoảng trắng
giữa nhãn và tiêu đề; Android chuẩn hóa resource thành một khoảng trắng. Đã
sửa assertion để kiểm tra nội dung với khoảng trắng hợp lệ và chạy lại 2/2
đạt. Không cần thay code production cho lỗi assertion này.

Báo cáo XML của chín bài được giữ ở
[`artifacts/reports/v1-localization/`](../artifacts/reports/v1-localization/).
Mỗi lớp được chạy riêng bằng tham số runner `class` để xác nhận số bài thực
trong XML; không suy ra đã chạy đủ chỉ từ thông báo build thành công.

Ví dụ chạy lại một lớp khi cần xác minh thay đổi liên quan:

```powershell
.\build.ps1 :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=vn.lichviet.app.ui.CalendarTextLocalizationTest'
```

Gate tích hợp cuối đã đạt 56 bài JVM, 38 bài Android ở cả cấu hình điện thoại
và tablet ngang trên emulator API 37, Lint 0 lỗi/76 cảnh báo, release R8 thành
công. Cả ba bộ resource mặc định/VI/EN có đủ 514 mục dịch; AAB giữ cả hai ngôn
ngữ để đổi offline. QA trực quan font 200% đã dẫn đến sửa chiều rộng lưới/rail
và dùng một cột lịch khi chữ lớn. Xem [VALIDATION.md](../VALIDATION.md) cho bằng
chứng và giới hạn; chưa kiểm chứng mọi luồng font scale hoặc TalkBack.

## Kiểm tra thủ công còn cần

1. Cài mới hoặc dùng Cài đặt → xem lại giới thiệu; kiểm tra bốn trang, vuốt,
   Back, bỏ qua và hoàn tất. Thử cả lựa chọn thiết lập nhắc và từ chối quyền.
2. Chuyển VI ↔ EN ở onboarding và Settings, đổi sáng/tối/hệ thống, xoay màn
   hình và mở lại app. Kiểm tra biểu mẫu đang nhập và tab đang chọn.
3. Lặp lại trên API 26–32 để xác minh nhánh scoped Context; gate tự động nêu
   trên mới chạy API 37, không phải bằng chứng nhánh cũ đã được chạy.
4. Trên điện thoại nhỏ/tablet/multi-window, tăng font tới 200%, bật TalkBack;
   kiểm tra chip English dài, ngày nhập, nút điều hướng và thứ tự đọc.
5. Ghim cả bốn widget, đổi ngôn ngữ và ngày đầu tuần; kiểm tra trên launcher
   thực, các kích thước widget và sự kiện nhiều ngày/ICS. Apply RemoteViews
   trong test không thay thế việc kiểm tra trên mọi launcher.

Giới hạn nền và nhắc lịch nằm ở [Phase 1](PHASE1_REMINDERS.md); phạm vi ICS ở
[Phase 2](PHASE2_ADVANCED_CALENDAR.md); quy tắc và giới hạn lá số ở
[Phase 4](PHASE4_HOROSCOPE.md) và
[core/HOROSCOPE_RULES.md](../core/HOROSCOPE_RULES.md). Việc hỗ trợ resource VI/EN
không phải tuyên bố mọi luồng lỗi, thiết bị hoặc dịch vụ Android đều đã qua
kiểm tra localization trên thiết bị thật.
