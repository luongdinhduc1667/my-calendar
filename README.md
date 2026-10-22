# Lịch Việt — Vạn niên & ngày đáng nhớ

Ứng dụng Android native bằng Kotlin và Jetpack Compose, giao diện tiếng Việt,
đỏ son / trắng ngà / vàng champagne. Không có WebView, backend, tài khoản hay
quyền Internet. Đây là bản **0.1.0 để chạy và kiểm thử**, chưa phải bản phát hành
Google Play đã ký và kiểm định đầy đủ.

## Đã triển khai

- Lịch tháng và tuần, vuốt/chuyển tháng, về hôm nay, chọn ngày/năm bằng date picker.
  Ngày dương, ngày âm, ngày lễ và dấu sự kiện xuất hiện trên cùng ô lịch.
- Chuyển đổi âm–dương hai chiều, kiểm tra ngày không tồn tại và tháng nhuận.
  Can Chi ngày/tháng/năm, tiết khí tại trưa Việt Nam, sáu giờ hoàng đạo.
- Lõi lịch chạy offline theo **UTC+7 cố định**, hỗ trợ ngày dương 1900–2100.
  Phân biệt lịch Việt Nam với lịch Trung Quốc tại các mốc 1968 và 1985.
- Sự kiện lưu bằng Room: tạo, sửa, xóa, ghi chú, ngày/giờ, sinh nhật, ngày giỗ.
  Nhập ngày âm trực tiếp hoặc chọn ngày dương. Lặp ngày/tuần/tháng/năm/
  năm âm lịch; chọn tháng thường, nhuận hoặc cả hai; ngày 30 gặp tháng thiếu
  có thể dời về ngày cuối hoặc bỏ qua.
- Nhắc trước 0/1/3/7/14 ngày. Notification Channel, quyền thông báo Android 13+,
  WorkManager lưu lịch nhắc và lập lại sau khi khởi động máy/đổi thời gian.
- Bốn App Widget native: hôm nay, ngày âm, lịch tháng có ngày âm, sự kiện sắp tới.
  Chạm ngày trên widget mở đúng ngày trong ứng dụng. Thêm từ Cài đặt hoặc launcher.
- Tra ngày theo khoảng thời gian và mục đích: khai trương, cưới hỏi, chuyển nhà,
  xuất hành. Mỗi kết quả có lý do theo bộ quy ước được công bố; không chấm điểm
  hay tạo dự đoán ngẫu nhiên.
- Tra con giáp và năm Can Chi từ ngày sinh; danh sách lễ Tết và ngày truyền thống.
- Tablet: Navigation Rail, lịch bên trái và chi tiết/sự kiện bên phải;
  nút toàn màn hình. Điện thoại: thanh điều hướng dưới, bố cục dọc có cuộn.
  Bố cục theo chiều rộng khả dụng, hỗ trợ đổi cấu hình và multi-window.
- Chủ đề sáng/tối/hệ thống, chọn ngày bắt đầu tuần, bật/tắt âm lịch/ngày lễ.
  Cài đặt lưu DataStore. Ngày chọn/tháng/chế độ tuần lưu SavedStateHandle.
- Sao lưu/khôi phục JSON qua trình chọn tệp Android, không cần quyền truy cập
  toàn bộ bộ nhớ. Khôi phục có xác nhận thay thế dữ liệu; xóa dữ liệu cá nhân
  cũng có xác nhận và hủy các lời nhắc liên quan.

## Kiến trúc

```text
core/                            Kotlin/JVM thuần, không phụ thuộc Android
  .../Astronomy.kt                Công thức thiên văn Meeus
  .../VietnameseCalendar.kt       Chuyển lịch, Can Chi, tiết khí, ngày lễ
  .../Almanac.kt                  Quy ước tra ngày tách khỏi phép tính lịch
  src/test/                      Kiểm thử lịch và dữ liệu thiên văn độc lập
app/
  .../CalendarViewModel.kt        MVVM, Flow, SavedStateHandle, Hilt
  .../data/                      Room, repository, DataStore, recurrence, backup
  .../reminder/                  Lập lời nhắc, worker, receiver hệ thống
  .../widget/                    RemoteViews AppWidget, cập nhật nền
  .../ui/                        Design system, lịch, sự kiện, Navigation Compose
  .../ui/tools/                  Đổi lịch, tra ngày, con giáp, ngày lễ
  schemas/                       Room schema v1 được export
  src/test/                      Kiểm thử lặp sự kiện và thời điểm nhắc
  src/androidTest/               Kiểm thử Room, backup, state và Compose
artifacts/                       Ảnh giao diện chụp từ emulator
```

Hai module giữ phần tính lịch độc lập và dễ kiểm thử, tránh chia quá nhiều
module khi dự án còn nhỏ. Hilt cung cấp repository cho ViewModel; widget/worker
dùng cùng database và cùng lõi lịch. Dữ liệu người dùng không được seed bằng
sự kiện mẫu. Các bài kiểm thử tự tạo dữ liệu riêng.

## Công cụ và dependency đã ghim phiên bản

| Thành phần | Phiên bản |
| --- | --- |
| Gradle / Android Gradle Plugin | 9.6.0 / 9.4.1 |
| Kotlin / Compose compiler | 2.2.10 / 2.2.10 |
| Compose BOM | 2025.08.00 |
| minSdk / compileSdk / targetSdk | 26 / 37 / 37 |
| JDK chạy build / bytecode | 21 / 17 |
| Hilt / KSP | 2.59.2 / 2.3.6 |
| Room / DataStore / WorkManager | 2.8.4 / 1.1.7 / 2.10.1 |
| Navigation / Lifecycle | 2.9.3 / 2.9.3 |
| Coroutines | 1.10.2 |
| AndroidX Test runner / Espresso | 1.7.0 / 3.7.0 |

AGP 9 dùng Kotlin tích hợp sẵn. Plugin Compose cùng phiên bản Kotlin.
Espresso 3.7 được khai báo trực tiếp để tương thích Android 17, thay vì bản
3.5 cũ được kéo vào gián tiếp từ Compose testing. Phiên bản ghim phục vụ
build lặp lại được; không có nghĩa mọi thư viện đều là bản mới nhất.

## Mở và build

1. Mở thư mục này bằng Android Studio hỗ trợ AGP 9.4.
2. Cài Android SDK Platform 37 và Build Tools 36.0.0, cấu hình JDK 21.
3. Tạo `local.properties` trỏ tới SDK trên máy, ví dụ
   `sdk.dir=D\:\\code\\Android\\Sdk`. Tệp này không đưa vào Git.
4. Chờ Gradle Sync, chọn thiết bị Android 8.0 trở lên rồi Run `app`.

PowerShell trên máy hiện tại:

```powershell
.\build.ps1
.\build.ps1 :app:connectedDebugAndroidTest
```

`build.ps1` tự tìm JDK tại hai đường dẫn thường dùng trên máy này; trên máy khác
hãy đặt `JAVA_HOME`. Có thể dùng wrapper trực tiếp:

```sh
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:bundleRelease :app:assembleRelease
```

APK cài thử: `app/build/outputs/apk/debug/app-debug.apk`.
AAB release: `app/build/outputs/bundle/release/app-release.aab`.
APK release chưa ký: `app/build/outputs/apk/release/app-release-unsigned.apk`.
Để phát hành, dùng Android Studio → Generate Signed App Bundle / APK và keystore
riêng. Không có khóa phát hành hay mật khẩu trong repository.

`device.ps1` hỗ trợ QA trên **emulator-5554**: cài/mở app, chụp ảnh, đọc log và đổi
độ phân giải. Ví dụ `./device.ps1 Resize 1600x1000 200`; dùng `ResetSize` để trả lại
cấu hình ban đầu. Script chỉ dành cho máy phát triển, không nằm trong APK.

## Kiểm chứng và giới hạn

Xem [VALIDATION.md](VALIDATION.md) để biết kết quả build/kiểm thử thực tế.
Nguồn thiên văn, dữ liệu kiểm chứng và giấy phép MIT nằm tại
[core/README.md](core/README.md) và
[core/THIRD_PARTY_NOTICES.md](core/THIRD_PARTY_NOTICES.md).

- Các ngày trước 1968 áp dụng hồi tố quy tắc UTC+7 hiện đại, không tái dựng lịch
  lịch sử của từng miền. Thuật toán là mô hình lịch dân dụng; xem tài liệu lõi
  để biết sai số đo được. Kiểm thử khứ hồi không chứng minh mọi ngày đều chính
  xác so với mọi bảng thiên văn độc lập.
- Ngày tốt/giờ hoàng đạo là thông tin văn hóa theo một bộ quy ước đơn giản hóa.
  Không có dự báo tử vi ngày/tuần/tháng/năm, lá số an sao, ngũ hành nạp âm,
  nhị thập bát tú hoặc tư vấn quyết định tài chính/sức khỏe.
- WorkManager **không bảo đảm thông báo đúng từng phút hoặc widget đổi đúng
  nửa đêm** khi thiết bị bị Doze/tiết kiệm pin. Chưa có AlarmManager exact alarm.
- Nhắc giờ theo Việt Nam ngay cả khi đi nước ngoài. Đây là chính sách của bản này,
  không phải chọn múi giờ riêng cho từng sự kiện.
- Sao lưu JSON chứa dữ liệu cá nhân ở dạng đọc được; do người dùng chọn nơi lưu.
  Room và DataStore là hai kho riêng, không có giao dịch nguyên tử xuyên hai kho
  nếu thiết bị mất nguồn giữa lúc khôi phục. Lỗi cập nhật cài đặt có thử rollback.
- Room mới có schema v1, chưa có lịch sử migration để kiểm thử nâng cấp schema.
- Còn cần onboarding, xuất/nhập lịch chuẩn ICS, màu sự kiện tùy chọn, số tuần,
  tùy chọn giảm chuyển động, bản dịch tiếng Anh đầy đủ và cấu hình thông báo
  chính xác nếu sản phẩm yêu cầu. Chuỗi giao diện hiện chủ yếu ở Kotlin.
- Chưa đo FPS, pin, cold start hay benchmark trên thiết bị yếu; chưa kiểm định
  toàn bộ font scale/TalkBack/foldable/API 26. Không tuyên bố đã sẵn sàng phát hành.
- Trước Google Play cần ký bản phát hành, kiểm thử thiết bị thật, hoàn tất nội dung,
  chính sách quyền riêng tư, Data Safety, store listing và kiểm tra chính sách tại
  thời điểm gửi. Ứng dụng không tự tải lên hay phát hành.

Tham chiếu build: [AGP 9.4](https://developer.android.com/build/releases/agp-9-4-0-release-notes),
[Kotlin tích hợp](https://developer.android.com/build/migrate-to-built-in-kotlin),
[Compose BOM](https://developer.android.com/develop/ui/compose/bom),
[AndroidX Test](https://developer.android.com/jetpack/androidx/releases/test).
