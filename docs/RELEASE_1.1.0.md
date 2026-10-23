# Lịch Việt 1.1.0

Ngày hoàn thiện: 23/10/2026. `versionName=1.1.0`, `versionCode=10100`.

## Nội dung mới

- Dashboard **đủ 12 con giáp Việt Nam**, Mèo/Trâu, chi tiết từng tuổi,
  tra ngày sinh dương chính xác quanh Tết, âm/dương, hành địa chi, lục hợp,
  cùng nhóm tam hợp, lục xung; đủ năm tổ hợp Can Chi/nạp âm cho mỗi tuổi và
  các nhãn năm âm 1899–2100.
- **Dự báo dân gian ngày/tuần/tháng/năm**: chọn ngày tham chiếu, chu kỳ âm
  có tách tháng nhuận, chủ đề và gợi ý công việc/giao tiếp, bảng từng ngày,
  mở ngày đó trong lịch. Lời luận do app viết từ bảng quan hệ có nguồn;
  giao diện phân biệt phép tính truyền thống với diễn giải biên tập.
- **Ngũ hành/nạp âm**: so sánh hai ngày sinh, ghi rõ hướng sinh/khắc, không
  lẫn hành thiên can, địa chi và nạp âm; không tạo điểm hợp tuổi.
- **Can Chi giờ**: đủ 12 giờ kép/13 đoạn của ngày dân dụng, chọn đổi ngày
  lúc 00h hoặc 23h, hiển thị hoàng/hắc đạo và ngày hiệu lực. Có xử lý giới
  hạn ngày 2100-12-31 thay vì đọc vượt phạm vi.
- **24 Sơn / Bát Trạch**: nhập góc mặt tiền, tính tọa đối diện, chính ngũ
  hành, trạch quái theo tọa và tám phương Đại Du Niên. Có sơ đồ phương vị;
  tên cát/hung lịch sử hiển thị cùng quy ước và nguồn.
- Chi tiết lịch thêm **nạp âm ngày và chi đối xung**. Ba phần 12 con giáp,
  lá số Đẩu Số và phong thủy có tab riêng và giữ lựa chọn qua recreation.
- Nội dung mới đủ VI/EN; nguồn, mục sách, phạm vi dùng và phương pháp có
  sẵn offline. Nguồn liên kết mở ngoài app; không thêm quyền INTERNET.

Nguồn cụ thể, công thức và giới hạn được ghi tại
[ZODIAC_FENG_SHUI_RULES_1.1.md](ZODIAC_FENG_SHUI_RULES_1.1.md), đồng thời
có nút nguồn tại các kết quả trong app. Thư viện/cổ thư không phải tác giả
của lời luận theo ngày mà Lịch Việt biên soạn.

## Dữ liệu và tương thích

Giữ **Room schema 4, JSON backup 5**, vẫn đọc backup v1–v4. Không thêm bảng
hoặc migration; ngày sinh tra nhanh không được ghi vào Room. Hồ sơ lá số,
sự kiện, lời nhắc và ICS giữ nguyên định dạng. Không thêm dependency, quyền
vị trí/cảm biến, dịch vụ mạng hoặc công việc nền cho tử vi.

## Kết quả kiểm tra thực tế

| Kiểm tra | Kết quả |
| --- | --- |
| JVM core | 41 bài đạt, 0 lỗi; thêm 14 bài về nạp âm, 12 tuổi, quan hệ, tháng nhuận, Tết, biên phạm vi, Can Chi giờ, 24 Sơn và ma trận Bát Trạch 8×8 |
| JVM app | 32 bài đạt, 0 lỗi |
| Android full suite, phone API 37 | 45 bài đạt, 0 lỗi/skipped |
| Android nhóm tử vi mới sau chỉnh hiển thị, phone | 3 bài hành trình đạt; gồm chọn chu kỳ, tháng nhuận, nguồn, recreation/chuyển tab, mở lại lịch, sinh trước Tết và hướng nhập không hợp lệ |
| Android nhóm tử vi mới, tablet 1600×1000, density 200 | 4 bài đạt: 3 hành trình và 1 bài English |
| Lint debug | 0 lỗi, 76 cảnh báo hiện có; không thêm baseline/tắt kiểm tra |
| Resource parity | 626 tên string/plural/array ở mỗi bộ default, VI, EN; không thiếu tên dịch |
| Release | APK R8/resource shrinking và AAB build thành công |
| Runtime release QA | Cài bản sao ký khóa debug, mở lạnh dashboard tablet và nhập góc trong công cụ phương vị; không có AndroidRuntime error trong log được kiểm tra |

Đã xem ảnh thực dashboard/chi tiết trên điện thoại và dashboard tablet;
dashboard ở cả phone/tablet cỡ chữ 200% hiển thị/scroll được (tab rộng có
cuộn ngang). Chưa chứng nhận máy thật/OEM,
API 26–32, TalkBack, mọi đa cửa sổ, pin hoặc benchmark FPS. Bốn bài mới trên
tablet là một tập con, không gọi đó là toàn bộ 45 bài chạy trên tablet.

Báo cáo XML được giữ cục bộ tại `artifacts/reports/v1.1.0/`:
`android-phone-api37-full.xml`, `android-phone-api37-zodiac.xml`,
`android-tablet-api37-zodiac.xml`, các XML JVM, `lint-debug.xml`, `summary.json`.

Ảnh QA đã xem: [điện thoại](../artifacts/v110-zodiac-phone.png),
[chi tiết/dự báo](../artifacts/v110-zodiac-reading-phone.png),
[tablet R8](../artifacts/v110-release-tablet-zodiac.png),
[phone chữ 200%](../artifacts/v110-phone-font200.png),
[tablet chữ 200%](../artifacts/v110-tablet-font200.png).

Lệnh kiểm tra/phát hành:

```powershell
.\build.ps1 :core:test :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest '-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true'
.\build.ps1 :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.package=vn.lichviet.app.zodiac' '-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true'
.\build.ps1 :app:assembleRelease :app:bundleRelease
```

## Gói bàn giao

- [APK debug cài thử](../artifacts/LichViet-1.1.0-debug.apk)
- [APK release chưa ký](../artifacts/LichViet-1.1.0-release-unsigned.apk)
- [AAB release](../artifacts/LichViet-1.1.0-release.aab)
- [SHA-256](../artifacts/SHA256SUMS-1.1.0.txt)

APK/AAB/idsig được `.gitignore` bỏ qua. Bản sao `artifacts/qa/release-smoke.apk`
chỉ dùng khóa debug để QA; APK release bàn giao vẫn chưa ký bằng keystore
phát hành. Chưa tạo store listing hoặc tải Google Play.

Phạm vi 1.1 không bao gồm nhị thập bát tú, Huyền Không phi tinh, Kỳ Môn,
cung phi người ở hoặc luận giải dự đoán chuyên sâu từ lá số Đẩu Số. Phạm vi
lá số cũ vẫn được mô tả tại [HOROSCOPE_RULES.md](../core/HOROSCOPE_RULES.md).
