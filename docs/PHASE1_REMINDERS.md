# Phase 1 — Lời nhắc đáng tin cậy

## Hành vi đã triển khai

- Mỗi sự kiện có tối đa 16 mốc nhắc độc lập, tính theo phút trước giờ diễn ra; trình sửa sự kiện có các mốc nhanh từ đúng giờ đến 14 ngày và mốc ngày tùy chọn. Sự kiện cả ngày dùng 09:00 theo múi giờ Việt Nam (UTC+7).
- `AlarmManager` gửi lời nhắc. Khi có quyền báo thức chính xác, ứng dụng dùng `setExactAndAllowWhileIdle`; khi không có hoặc quyền bị thu hồi giữa chừng, dùng `setAndAllowWhileIdle`. `WorkManager` chỉ đối soát trạng thái Room định kỳ 12 giờ và sau các thay đổi hệ thống.
- Mỗi mốc có ID ổn định, một `PendingIntent` cho lần nhắc chính và một cho hoãn. Broadcast phải khớp ngày và thời điểm đã lưu trong Room trước khi gửi thông báo; một lần giao lại không tạo thông báo trùng. Các hành động hỗ trợ hoãn 5/10/15 phút và hoàn thành lần xuất hiện.
- Khởi động lại, cập nhật gói, đổi ngày/giờ/múi giờ, thay đổi quyền báo thức chính xác và mở lại ứng dụng đều kích hoạt đối soát. Khi thông báo bị chặn, lần nhắc chưa bị đánh dấu đã gửi; sau khi bật lại quyền/kênh và đối soát, lời nhắc chưa quá 24 giờ có thể được phát lại.
- Thông báo dùng danh tính riêng theo ngày xuất hiện để lần lặp tiếp theo vẫn có thể báo âm/rung. Người dùng chỉnh âm/rung qua cài đặt kênh thông báo Android.

## Dữ liệu và tệp thay đổi

- Room `AppDatabase` tăng từ phiên bản 1 lên 2; `MIGRATION_1_2` thêm `events.allDay` mặc định `0`, tạo `event_reminders` cùng khóa ngoại và chỉ mục duy nhất `(eventId, offsetMinutes)`, rồi chuyển mốc cũ `reminderDaysBefore` của các sự kiện đang bật nhắc. Lược đồ v2 được xuất trong `app/schemas/vn.lichviet.app.data.AppDatabase/2.json`. `EventDao.save` cập nhật hàng hiện có bằng `UPDATE`, giữ nguyên ID và các lời nhắc con.
- Tệp dữ liệu: `app/src/main/java/vn/lichviet/app/data/{AppDatabase,EventEntity,EventReminderEntity,EventRepository,BackupRepository}.kt`. Lưu sự kiện và các mốc nhắc trong một giao dịch Room; hủy/lập báo thức sau khi giao dịch thành công. Sao lưu JSON v2 chứa cả `allDay` và danh sách mốc; vẫn nhập được JSON v1. Xuất/nhập giới hạn 20 triệu ký tự. Xóa dữ liệu/nhập bản sao lưu được bảo vệ khỏi hủy coroutine giữa chừng.
- Tệp lập lịch: `app/src/main/java/vn/lichviet/app/reminder/{AlarmReminderScheduler,ReminderPermissionManager,ReminderPlanner,ReminderScheduler,ReminderReceiver,ReminderNotificationManager,CalendarRefreshReceiver,EventReminderWorker}.kt`; manifest khai báo quyền và receiver. Worker nhắc lịch v0.1 được giữ như no-op để các công việc cũ còn trong hàng đợi không gửi trùng.
- Giao diện: `app/src/main/java/vn/lichviet/app/{CalendarViewModel,MainActivity}.kt`, `app/src/main/java/vn/lichviet/app/ui/{App,EventsScreen}.kt`. Trạng thái mốc nhắc đang tải khóa thao tác chọn/lưu và giữ thay đổi chưa lưu khi Activity được tạo lại. Cài đặt cho phép mở quyền thông báo, kênh âm/rung và quyền báo thức chính xác; ứng dụng kiểm tra lại quyền khi quay lại màn hình.
- Build/test: `app/build.gradle.kts` đóng gói lược đồ v1 làm asset kiểm thử. `app/src/test/java/vn/lichviet/app/reminder/ReminderPlannerTest.kt`; `app/src/androidTest/java/vn/lichviet/app/data/{ReminderMigrationTest,ReminderDeliveryTest}.kt`; `app/src/androidTest/java/vn/lichviet/app/reminder/AlarmReminderIntegrationTest.kt`; `app/src/androidTest/java/vn/lichviet/app/ui/{MultipleReminderEditorTest,EventJourneyTest}.kt`.
- Không thêm thư viện mới.

## Xác minh đã chạy

- `.\build.ps1 :app:testDebugUnitTest`: 15/15 bài đạt.
- `.\build.ps1 :app:connectedDebugAndroidTest`: 20/20 bài đạt trên emulator API 37. Bao gồm mở cơ sở dữ liệu v1 thật bằng migration v2, cập nhật sự kiện vẫn giữ ID lời nhắc, nhận broadcast thật và từ chối bản sao, lập/hủy báo thức thật và nhánh inexact, khôi phục sau boot/đổi múi giờ, cùng chỉnh nhiều mốc sau khi tạo lại Activity.
- `.\build.ps1 :app:lintDebug`: đạt.
- `.\build.ps1 :app:assembleRelease :app:bundleRelease`: đạt.

## Kiểm tra thủ công nên thực hiện trên thiết bị

1. Cập nhật từ bản đã có sự kiện và lời nhắc v0.1; kiểm tra tên, ghi chú, ngày âm lặp hằng năm, mốc cũ và các widget vẫn còn.
2. Tạo sự kiện cách vài phút với hai mốc nhắc; kiểm tra chỉ một thông báo mỗi mốc, mở thông báo vào đúng ngày, thử hoãn 5/10/15 phút và hoàn thành. Sửa ngày/giờ hoặc xóa sự kiện trước lúc nhắc để xác nhận báo thức cũ không hiện.
3. Tắt quyền `POST_NOTIFICATIONS` hoặc tắt kênh lời nhắc trước thời điểm giao; bật lại trong vòng 24 giờ rồi mở ứng dụng. Xác nhận thông báo được đối soát và không mất trạng thái. Sau khi từ chối quyền vĩnh viễn, dùng nút mở cài đặt thông báo của ứng dụng.
4. Thu hồi quyền báo thức chính xác trong cài đặt hệ thống, tạo một lời nhắc và kiểm tra nó vẫn được lập ở nhánh inexact, dù Android có thể giao trễ. Cấp lại quyền, quay về ứng dụng và kiểm tra trạng thái trên màn Cài đặt.
5. Khởi động lại và đổi múi giờ thiết bị khi đã có lời nhắc tương lai; kiểm tra báo thức được khôi phục nhưng ngày/giờ sự kiện vẫn theo UTC+7. Thử xuất JSON v2 rồi nhập lại trên dữ liệu thử, và nhập một bản sao lưu v1.

## Giới hạn nền tảng

Quyền `SCHEDULE_EXACT_ALARM` là quyền truy cập đặc biệt; bản cài mới trên Android 14 trở lên thường không được cấp mặc định. Khi thiếu quyền, lời nhắc inexact có thể trễ do Doze/chính sách pin. Quyền thông báo hoặc kênh bị tắt sẽ ngăn hiển thị; Android cũng không cho ứng dụng bảo đảm thông báo sau khi người dùng force-stop, tắt máy trong thời điểm nhắc, hay trì hoãn quá lâu. Android quản lý âm/rung ở cấp kênh sau khi kênh được tạo.

Tham khảo: [Android Alarms](https://developer.android.com/develop/background-work/services/alarms), [Notification channels](https://developer.android.com/develop/ui/compose/notifications/channels), [Google Play exact alarm policy](https://support.google.com/googleplay/android-developer/answer/16558241?hl=en).
