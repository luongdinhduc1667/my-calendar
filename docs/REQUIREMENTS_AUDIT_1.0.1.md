# Đối chiếu MASTER PROMPT — Lịch Việt 1.0.1

Đây là ảnh chụp trạng thái **1.0.1**. Bản 1.1.0 đã bổ sung màn 12 con giáp,
bốn chu kỳ đọc dân gian có nguồn và ba bộ phong thủy; xem
[báo cáo 1.1.0](RELEASE_1.1.0.md) cùng
[nguồn/quy tắc 1.1](ZODIAC_FENG_SHUI_RULES_1.1.md). Các dòng bên dưới được
giữ làm lịch sử, không dùng để kết luận những tính năng ấy còn thiếu ở 1.1.

Ngày rà soát: 23/10/2026. Đối chiếu prompt người dùng gửi lại, mã thật trong
repository và kết quả kiểm thử; không coi một màn giao diện là bằng chứng đã
có thuật toán hoặc đã đủ điều kiện phát hành thương mại.

**Kết luận: chưa đủ 100% MASTER PROMPT.** Phần lịch/sự kiện/nhắc/ICS/widget đã
có triển khai thực và kiểm thử. V1.0.1 sửa thiếu ngày 24/11 và bổ sung các mục
lịch/cài đặt nêu dưới đây. Tử vi 12 con giáp, phong thủy chuyên sâu và QA phát
hành cửa hàng vẫn còn khoảng trống. Không gán phần trăm hoàn thành vì các mục
trong prompt có độ lớn và tiêu chí kiểm chứng khác nhau.

## Đối chiếu từng nhóm

| Mục prompt | Trạng thái tại 1.0.1 | Bằng chứng / phần còn thiếu |
| --- | --- | --- |
| 1. Code chạy thật, không dữ liệu giả | Có trong phạm vi công bố | Build/test thực, không chèn fixture vào production. Thuật toán chưa xác minh được ghi là chưa hỗ trợ. Chưa chứng nhận production trên mọi máy. |
| 2. Kotlin, Compose, M3, Hilt, Room, DataStore, Navigation, WorkManager, Coroutines, API 26 | Có | app/build.gradle.kts, CalendarViewModel, AppDatabase, PreferencesRepository. Retrofit/Coil/backend không cần cho phạm vi offline hiện tại. |
| 2,16. Kiến trúc | Một phần | Hai module app/core, core tính toán thuần Kotlin, repository/ViewModel/UI tách trách nhiệm. Chưa tách domain/use-case và mọi feature thành module Clean Architecture riêng. Cấu trúc module gợi ý trong prompt không bắt buộc sao chép nguyên xi. |
| 3. Thiết kế sáng/tối, tiếng Việt, components | Phần lớn | MaterialTheme, calendar cells/cards, editor/dialog/picker, trạng thái trống/loading. Chưa có skeleton shimmer, FAB riêng hoặc dynamic color; không có shared transition. Các hiệu ứng tùy chọn không làm nút chức năng bị bỏ trống. |
| 3. Animation | Có tập chính | Vuốt tháng, chọn ngày/cung, animateContentSize, onboarding pager/spring/AnimatedContent. V1.0.1 thêm công tắc hiệu ứng app; không coi ripple, thao tác kéo hoặc spinner tiến trình là hiệu ứng phải bỏ. Chưa đo FPS. |
| 4. Responsive | Có mã, QA chưa đầy đủ | WindowSizeClass, rail/bottom navigation, lịch hai cột, chữ lớn chuyển một cột, safe insets/edge-to-edge. Đã chạy điện thoại/tablet emulator; chưa đủ máy thật, foldable, mọi multi-window, TalkBack. |
| 5. Lịch dương | Có | Tháng/tuần, chi tiết inline, tháng/năm, ngày bất kỳ/hôm nay, cuối tuần, ngày lễ, dot sự kiện, swipe/nút, lưu chọn qua recreation. V1.0.1 thêm số tuần ISO và nút tìm ngày bằng ô nhập. Search sự kiện có tiêu đề/ghi chú/địa điểm. |
| 6. Lịch âm | Phần lớn, có giới hạn | Hai chiều, tháng nhuận/thiếu, Can Chi ngày/tháng/năm, tiết khí tại trưa, sóc/vọng/Tết, UTC+7, fixture Việt/Trung. Chưa có API Can Chi đầy đủ của từng giờ; giờ hoàng đạo hiện mới có Chi và khoảng giờ. Chưa tái dựng lịch sử vùng miền, chưa tính chính xác thời điểm giao tiết. |
| 7. Lịch chính | Có tập chính | Ô dương/âm, mùng 1/rằm nổi màu, lễ/sự kiện, trạng thái chọn/hôm nay, thẻ thông tin và agenda, tablet hai cột. Chi tiết ngày hiện inline, không phải route Day Detail riêng. |
| 8. Tử vi 12 con giáp | Chưa đủ | Có engine lá số 12 cung riêng, dữ liệu năm/giờ và Can Chi trong tính toán. Không đồng nghĩa có dashboard 12 con giáp/Zodiac Detail đầy đủ. Chưa có bộ nội dung đặc điểm 12 tuổi, nạp âm năm sinh đầy đủ dạng tra cứu, tuổi hợp/xung hay tử vi ngày/tuần/tháng/năm có nguồn. |
| 8. Lá số chuyên sâu | Có phạm vi được công bố | Mệnh/Thân, Cục, 14 chính tinh, tập phụ tinh, Tứ Hóa, đại/tiểu hạn, hồ sơ, 4×4/list/zoom, tra cứu, so sánh phép tính, PNG/PDF. Chưa có full-chart golden độc lập, độ sáng sao/diễn giải dự đoán hoặc mọi phụ tinh. core/HOROSCOPE_RULES.md nêu trường phái và hiệu chỉnh. |
| 9. Phong thủy/ngày tốt | Một phần | Hoàng đạo/hắc đạo ngày theo quy ước đã công bố, sáu giờ hoàng đạo, Trực, Tam Nương/Nguyệt Kỵ; tra bốn mục đích: khai trương/cưới/chuyển nhà/xuất hành. Chưa có sáu giờ hắc đạo hiển thị riêng, nhị thập bát tú, xung tuổi, hướng xuất hành, bộ ngũ hành ngày, động thổ/ký kết/công việc quan trọng. Không tự thêm kết luận tốt/xấu khi chưa có nguồn. |
| 10. Events/reminders | Có | CRUD, bắt đầu/kết thúc, ghi chú/địa điểm/màu, lặp dương/âm, sinh nhật/giỗ, chính sách tháng nhuận/thiếu, nhiều mốc, snooze/done. Room, channels/quyền, AlarmManager chính xác hoặc fallback + WorkManager. Force-stop/tắt máy/chặn thông báo vẫn có giới hạn Android. |
| 11. Màn hình | Một phần theo cách tổ chức | Calendar, picker, converter, chart workspace, tra ngày, events/editor, holidays, settings, onboarding. Day Detail inline và editor/picker là dialog; Horoscope Dashboard 12 con giáp/Zodiac Detail như prompt chưa có. |
| 12. Widget | Có, QA launcher chưa đủ | Hôm nay/tháng/ngày âm/sắp tới bằng RemoteViews; múi giờ Việt Nam, cập nhật ngày, chạm mở đúng ngày. Glance chỉ là lựa chọn nếu phù hợp. Chưa thử mọi launcher/kích thước/OEM. |
| 13. Settings | Có tập yêu cầu chính từ 1.0.1 | Theme, đầu tuần, lịch âm, lễ, giờ hoàng đạo, hiệu ứng, thông báo, backup/restore/xóa, VI/EN. Dùng cỡ chữ hệ thống; chưa có thanh chọn cỡ chữ riêng (prompt chỉ yêu cầu nếu cần). |
| 14. Performance | Có tối ưu, chưa có chứng nhận | V1.0.1 warm-up thiên văn trên Default trước lịch, cache ngày tối đa 512 entry chỉ chứa ID, nhớ dữ liệu grid và màu sự kiện theo cửa sổ. ICS/tra ngày/export dùng dispatcher nền. Chưa có Macrobenchmark/baseline profile, đo pin/FPS/cold start hoặc low-end device QA. |
| 15. Riêng tư | Có thiết kế local | Không tài khoản/server/INTERNET, quyền có mục đích, SAF/FileProvider, xác nhận xuất lá số, xóa dữ liệu. Chưa có trang chính sách quyền riêng tư công khai/Data safety đã nộp Play. |
| 17. Tests | Có tập tự động, QA còn thiếu | Lịch âm, tháng nhuận, Can Chi, lễ, recurrence, timezone, ViewModel state, Room migration, Compose/navigation/recreation. Tests mới kiểm tra 24/11, ISO qua giao năm, cache đa luồng, settings bền, backup cũ/mới và widget English. Process death thật, reboot/Doze dài hạn, thiết bị cấu hình thấp chưa đầy đủ. |
| 18–20. Release và tài liệu | Một phần | Source/build/test, R8, adaptive launcher icon, README, APK/AAB, báo cáo. Chưa có splash tùy chỉnh/AndroidX splash compat riêng, keystore phát hành, store listing, Play upload hoặc QA thương mại hoàn chỉnh. |

## Những thiếu sót được xử lý trong 1.0.1

1. **Ngày Văn hóa Việt Nam 24/11:** thêm HolidayId/resource VI/EN; xuất hiện từ
   năm 2026 trên mọi nơi dùng chung engine: grid, thông tin ngày, màn ngày lễ,
   widget. Có kiểm thử không xuất hiện hồi tố vào 2025 và không lệch sang 23/25.
2. **Số tuần:** cài đặt DataStore, cột ISO 8601 trong tháng/tuần, semantics có
   số tuần và năm tuần. Khi tuần hiển thị bắt đầu Chủ Nhật, lấy tuần ISO của
   thứ Năm trong hàng; Chủ Nhật có thể thuộc tuần ISO trước hàng đang hiển thị.
   Có kiểm thử tuần 53 và năm tuần khác năm dương quanh giao năm.
3. **Giờ hoàng đạo/hiệu ứng:** thêm công tắc thật; trạng thái qua recreation,
   backup và restore. Ẩn ngày lễ cũng được áp dụng vào thẻ chi tiết và mô tả
   ô ngày, thay vì chỉ đổi màu grid.
4. **Tìm ngày:** nút kính lúp mở picker ở chế độ nhập ngày theo locale. Kết
   quả chọn điều hướng tới đúng tháng/ngày, có test nhập 24/11/2026.
5. **Tối ưu:** cache ngày giới hạn, không cache chuỗi dịch; khởi tạo bảng lịch
   thiên văn ở nền trước màn lịch; màu event dot không quét lại từng khi đổi
   chọn; chiều cao thanh sự kiện tăng theo font để tránh cắt chữ.
6. **Tương thích:** giữ Room schema 4/sự kiện/hồ sơ; backup 5 lưu ba tùy chọn
   mới, đọc v1–4 với giá trị mặc định. Không nâng dependency hàng loạt.

## Căn cứ cho 24/11 và phạm vi dữ liệu lễ

[Cổng Chính phủ](https://xaydungchinhsach.chinhphu.vn/ngay-24-11-hang-nam-la-ngay-van-hoa-viet-nam-nguoi-lao-dong-duoc-nghi-huong-nguyen-luong-119260113152642414.htm)
ghi ngày 24/11 là Ngày Văn hóa Việt Nam theo Nghị quyết 28/2026/QH16, có hiệu
lực 01/07/2026; ngày nghỉ đầu tiên 24/11/2026. Đã kiểm tra nguồn ngày 23/10/2026.
[Bộ Nội vụ](https://moha.gov.vn/tin-tuc/infographics-lich-nghi-ngay-van-hoa-viet-nam---id19711)
công bố phương án nghỉ một ngày vào 24/11/2026. App thêm ngày lễ cố định,
không tự sinh lịch hoán đổi hoặc các ngày nghỉ bù cho mọi năm/cơ quan.

## Các việc còn lại để đạt toàn bộ prompt

- Chuẩn hóa và kiểm chứng bộ nội dung 12 con giáp, nạp âm năm sinh, tuổi
  hợp/xung, Can Chi giờ; nếu có dự báo ngày/tuần/tháng/năm phải có nguồn và
  cách diễn giải nhất quán, không sinh điểm/vận hạn ngẫu nhiên.
- Chọn và kiểm chứng trường phái cho nhị thập bát tú, ngũ hành ngày, xung tuổi,
  hướng xuất hành và ba mục đích tra ngày còn thiếu; thêm fixture độc lập.
- Hoàn thiện UX còn thiếu nếu giữ yêu cầu riêng: Zodiac Detail, Day Detail
  route, skeleton/FAB/dynamic color và splash tương thích Android cũ.
- Chạy API 26–32, máy thật/OEM, foldable/multi-window/TalkBack, process death,
  reboot/Doze dài hạn; đo hiệu năng/pin bằng công cụ trước khi đưa ra chỉ số.
- Chuẩn bị chính sách riêng tư/Data safety/store listing, keystore riêng,
  kiểm tra chính sách Play tại ngày phát hành và ký/upload theo quyền người dùng.

Kết quả build/test và gói 1.0.1 ở [RELEASE_1.0.1.md](RELEASE_1.0.1.md).
Không đánh dấu phần chưa làm là DONE chỉ vì đã có APK.
