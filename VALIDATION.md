# Kết quả kiểm chứng — 22/10/2026

## Build thực tế

Đã chạy Gradle trên Windows với JDK 21, Android SDK 37 và Build Tools 36.0.0.
Các lệnh sau hoàn tất **BUILD SUCCESSFUL**:

```powershell
.\build.ps1 :core:test :app:testDebugUnitTest
.\build.ps1 :app:connectedDebugAndroidTest
.\build.ps1 :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug :app:bundleRelease :app:assembleRelease
```

- APK debug cài và chạy được trên Android Emulator API 37.
- APK release và AAB release được tạo với R8/resource shrinking.
- Bản release **chưa ký bằng khóa phát hành**; APK debug có chữ ký debug để cài thử.
- Android Lint debug: **0 lỗi, 41 cảnh báo**. Chủ yếu là phiên bản thư viện mới hơn,
  thuộc tính widget API 31 có fallback cho API cũ, chuỗi chưa tách tài nguyên,
  cỡ chữ nhỏ trong widget và khuyến nghị launcher/theme. Không tắt lint để vượt build.
- Kiểm tra manifest đã merge: không có quyền `INTERNET`. Các quyền phụ thuộc
  WorkManager gồm wake lock, network state và foreground service; hai quyền
  khai báo trực tiếp là thông báo và nhận boot completed.

## Kiểm thử JVM: 27 ca, không lỗi

| Suite | Số ca | Nội dung chính |
| --- | ---: | --- |
| VietnameseCalendarTest | 11 | Toàn bộ 73.414 ngày 1900–2100 khứ hồi, tháng nhuận, Can Chi, lễ, UTC+7, tra ngày |
| AstronomicalFixturesTest | 3 | Bảng tháng độc lập 2026–2027, thời điểm sóc NASA, khác biệt Việt/Trung năm 1985 |
| RecurrenceTest | 8 | Tết lặp năm âm, tháng nhuận, ngày 30/tháng thiếu, 29/2, cuối tháng, tuần/ngày |
| ReminderPlannerTest | 5 | Nhắc trước, giờ đã qua, không lặp trùng, sự kiện một lần, đi nước ngoài |

730 ngày trong 2026–2027 được đối chiếu với các mốc tháng đã công bố.
25 thời điểm sóc năm 2026–2027 lệch tối đa **29,081 giây** so với bảng NASA làm
tròn tới phút. Một mốc đông chí 1984 lệch **5,994 phút** trong mô hình kinh độ
Mặt Trời đơn giản; vẫn giữ đúng ngày dân dụng. Đây là kết quả trên các fixture
đã chọn, không phải khẳng định sai số bảo đảm trên mọi ngày 1900–2100.

Nguồn và chi tiết tại [core/README.md](core/README.md).

## Kiểm thử Android

Chạy trên Android Emulator 17 / API 37, thay đổi kích thước cửa sổ khả dụng:

| Cấu hình | Kết quả |
| --- | --- |
| Điện thoại mặc định | 10/10 ca giao diện/state/database lúc trước khi thêm suite widget |
| Điện thoại nhỏ: 960×1920 px, density 480, rộng 320dp | 14/14 ca |
| Tablet: 1600×1000 px, density 200, rộng 1280dp | 14/14 ca, chạy lại sau tinh chỉnh bố cục cuối |

Các ca Android gồm:

- Room CRUD trên database riêng, đóng/mở lại và giữ nguyên enum/chính sách lặp.
- Backup JSON giữ dấu tiếng Việt, tùy chọn lịch và chính sách tháng nhuận;
  loại bỏ dữ liệu hỏng, ngày sai và số phân số trước khi khôi phục.
- ViewModel khôi phục ngày/tháng/chế độ tuần từ SavedStateHandle; chặn ngày
  ngoài khoảng hỗ trợ; đồng bộ tiêu đề tháng khi chuyển sang tuần.
- Điều hướng năm tab/rail, chuyển tháng trước/sau, đổi lịch và quay lại.
- Tạo ngày giỗ lặp âm lịch qua giao diện, lưu Room, tái tạo Activity, mở lại và xóa.
- Chọn ngày khác sau khi mở từ widget, tái tạo Activity và giữ đúng lựa chọn mới.
- Bốn RemoteViews widget được Android inflate/apply và đo layout thật, gồm
  widget sự kiện đọc dữ liệu từ Room riêng. Không chỉ kiểm tra XML biên dịch.

XML kết quả được giữ trong `artifacts/reports/phone`, `small-phone` và `tablet`.
Ảnh chụp thực tế ở `artifacts/phone.png` và `artifacts/tablet.png`.
Emulator đã được trả về độ phân giải/density ban đầu sau kiểm thử.

## Chưa kiểm chứng

- Chưa có thiết bị Android thật hoặc emulator API 26 trong lượt chạy này.
- Chưa đo FPS, startup, pin, bộ nhớ, benchmark hay toàn bộ kích thước chữ/TalkBack.
- Chưa kiểm tra lịch giao thông báo qua nhiều ngày Doze/khởi động lại thực tế;
  đã kiểm thử quy tắc lập thời điểm nhắc, nhưng WorkManager vẫn là best effort.
- Widget đã kiểm tra Android apply/render, chưa chạy thử dài ngày trên mọi launcher.
- Chưa có migration test giữa nhiều phiên bản Room vì hiện chỉ có schema v1.
- Chưa kiểm thử nâng cấp bản đã ký, Play pre-launch report hay thực hiện phát hành.
