# Phase 2 — Lịch nâng cao và ICS

## Phạm vi tích hợp

Ứng dụng dùng iCal4j 4.4.0 để đọc/ghi iCalendar, không tách dòng thủ công thay
cho parser RFC. Storage Access Framework chọn tệp và nơi xuất; FileProvider
chia sẻ tệp giới hạn trong cache. Có thể mở `text/calendar` từ ứng dụng khác.
Preview hiển thị từng UID, lỗi/cảnh báo và lựa chọn nhập, cập nhật hoặc bỏ qua
UID trùng. Ghi sự kiện và các mốc nhắc trong một transaction Room; lỗi validation
không để lại một phần dữ liệu. ViewModel giữ preview/lựa chọn qua recreation.

Chuỗi nhập lưu VCALENDAR canonical của từng UID, bao gồm master, các ngoại lệ,
VTIMEZONE và thuộc tính chưa được ánh xạ. Các phép tính hỗ trợ DATE, UTC,
floating local time (giờ Việt Nam), TZID, RRULE từ ngày trở lên, RDATE, EXDATE,
RECURRENCE-ID di chuyển/hủy, DTEND hoặc DURATION. UTF-8, escaped text, folded
lines và CRLF do thư viện xử lý. Giờ bắt đầu/kết thúc có thể dùng timezone riêng;
lịch hiển thị các Instant quy đổi sang UTC+7.

Android không cung cấp API công khai để đăng ký custom ZoneRulesProvider của
iCal4j. Để không dùng nhầm giờ, VTIMEZONE nhúng chỉ được chấp nhận khi quy tắc
offset và mọi transition trong 1900–2100 khớp timezone có trên thiết bị. Custom
TZID hoặc định nghĩa khác/thiếu lịch sử bị từ chối rõ ràng. Tệp chỉ có TZID IANA
không có VTIMEZONE dùng timezone của thiết bị. Đây là giới hạn tương thích có
chủ ý; giữ canonical không thay thế việc kiểm chứng giờ.

VALARM tương đối trước DTSTART thuộc tập được hỗ trợ chuyển thành nhiều mốc
nhắc. Cảnh báo nêu rõ alarm chưa thể lên lịch; dữ liệu canonical vẫn giữ lại để
xuất. Không tuyên bố hỗ trợ đầy đủ mọi VALARM hoặc mọi biến thể iCalendar.

Xuất một, nhiều hoặc toàn bộ sự kiện, lưu tệp và chia sẻ đều có thao tác người
dùng. Lặp âm lịch và quy tắc native cuối tháng xuất các lần xuất hiện hữu hạn
trong khoảng đã chọn bằng RDATE, cùng `X-LICHVIET-*` mô tả quy tắc gốc. DTSTART
thuộc chính khoảng xuất. Khi nhập lại, chỉ phục hồi quy tắc native nếu dữ liệu
lịch chưa bị chỉnh thành một quy tắc khác; nếu đã chỉnh, giữ canonical ICS.
Ứng dụng khác chỉ nhận được những lần xuất hiện hữu hạn, không được hứa sẽ tự
tính tiếp âm lịch. Chi tiết thuộc tính nằm cạnh codec trong mã nguồn.

Sự kiện native có thời điểm kết thúc, cả ngày, địa điểm và màu palette/HEX.
Khoảng thời gian dùng `[start, end)`, ngày kết thúc cả ngày là exclusive. Lịch
tháng/tuần dùng chung occurrence engine và layout engine: thanh nhiều ngày tiếp
tục qua hàng tuần, hai lane và chỉ báo `+N` khi vượt chỗ. Có thể chạm thanh để
mở sự kiện. Màu áp dụng cho thanh, chấm và thẻ, với màu chữ theo tương phản.
Widget và nhắc lịch dùng cùng các occurrence, kể cả các ngoại lệ ICS.

## Tệp và thay đổi dữ liệu

- `app/src/main/java/vn/lichviet/app/ics/`: codec, support/diagnostic models,
  occurrence/layout engine, repository, ViewModel và workspace SAF.
- `data/{EventEntity,EventValidation,EventRepository,AppDatabase,BackupRepository}.kt`:
  thời lượng/màu/UID, migration và backup tương thích.
- `ui/{CalendarScreen,EventsScreen,EventColors,App}.kt`: event bars, editor và
  entry point nhập/xuất; `CalendarViewModel.kt` mở rộng occurrence ngoài main thread.
- `reminder/`, `widget/CalendarWidgets.kt`: khóa occurrence và dữ liệu dùng chung.
- `res/values*/{calendar_events,ics_diagnostics,event_plurals}.xml`, manifest và
  `res/xml/ics_file_paths.xml`: VI/EN, intent filter và scoped provider.
- `app/src/test/.../ics/` và `app/src/androidTest/.../ics/`: phép lặp/layout,
  runtime thư viện, giao dịch nhập và UID.

Room `MIGRATION_2_3` bổ sung cột kết thúc/địa điểm/màu/UID/canonical ICS vào
events và khóa occurrence vào trạng thái reminder. Có unique index cho UID;
không destructive migration. V1→2 và 3→4 thuộc các phase bên cạnh. Backup v4
vẫn đọc định dạng cũ; tài liệu tích hợp có kết quả kiểm tra toàn chuỗi migration.

Dependency mới: `org.mnode.ical4j:ical4j:4.4.0` và core-library desugaring cho
Java API trên Android cũ. Material 3 window size class dùng cho bố cục adaptive.
Giấy phép BSD của iCal4j có trong assets và `THIRD_PARTY_NOTICES.md`.

## Kiểm chứng

Gate JVM có 10 bài `IcsCodecTest` và 2 bài `EventLayoutEngineTest`: RRULE,
RDATE/EXDATE, ngoại lệ di chuyển/hủy, giữ thuộc tính lạ, end exclusive,
lặp âm/VALARM, khoảng xuất hữu hạn, kiểm tra extension đã bị chỉnh, hai lần
xuất hiện trong một ngày và duration qua DST. Kết quả gate Android/release
tích hợp, số bài và bằng chứng runtime cuối cùng ở
[VALIDATION.md](../VALIDATION.md).

## Giới hạn và kiểm tra thủ công

- Từ chối EXRULE, `RECURRENCE-ID;RANGE=THISANDFUTURE`, lặp dưới một ngày,
  timezone không giải được/VTIMEZONE không tương thích và thời lượng quá 366 ngày. Một cửa sổ mở rộng tối
  đa 367 ngày, tối đa 20.000 occurrence; có giới hạn kích thước/số UID/override.
- Sửa native qua editor; chuỗi ICS có thể cập nhật bằng nhập lại cùng UID.
  Không có editor riêng cho một ngoại lệ ICS hoặc đồng bộ tài khoản.
- Quyền thông báo/exact alarm và giới hạn nền vẫn theo Phase 1. Giữ một VALARM
  trong tệp xuất không đồng nghĩa Android đã lên lịch loại alarm đó.
- Kiểm tra chọn tệp thực từ nhiều document provider, hủy picker, tệp lỗi,
  UID trùng, chọn phạm vi xuất và mở Sharesheet. So sánh lịch Gregorian trong
  ứng dụng khác; kiểm tra DST và sự kiện đi qua tháng/năm.
- Tạo ba sự kiện nhiều ngày chồng nhau để kiểm tra `+N`, thử palette/HEX sáng
  và tối, xoay tablet, phóng font và chạm mở sự kiện. Các luồng provider/launcher
  của bên thứ ba cần QA thiết bị thật bổ sung.
