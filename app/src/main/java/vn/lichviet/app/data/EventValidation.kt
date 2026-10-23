package vn.lichviet.app.data

import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate
import java.time.LocalDateTime

internal object EventValidation {
    fun validated(event: EventEntity): EventEntity {
        require(event.title.isNotBlank()) { "Vui lòng nhập tên sự kiện" }
        require(event.title.length <= 200 && event.note.length <= 4000) { "Nội dung sự kiện quá dài" }
        val date = LocalDate.parse(event.solarDate)
        require(date in VietnameseCalendar.minDate..VietnameseCalendar.maxDate) { "Chỉ hỗ trợ năm 1900–2100" }
        require(event.hour in 0..23 && event.minute in 0..59) { "Giờ không hợp lệ" }
        require(event.reminderDaysBefore in 0..365) { "Nhắc trước từ 0 đến 365 ngày" }
        require(event.lunarDay == null || event.lunarDay in 1..30) { "Ngày âm không hợp lệ" }
        require(event.lunarMonth == null || event.lunarMonth in 1..12) { "Tháng âm không hợp lệ" }
        require(event.location.length <= 500) { "Địa điểm quá dài" }
        require(event.icsUid == null || event.icsUid.length in 1..512) { "UID không hợp lệ" }
        require(event.icsCalendar == null || (event.icsUid != null && event.icsCalendar.length <= 4_000_000)) {
            "Dữ liệu lịch ICS không hợp lệ"
        }
        require(event.colorArgb == null || (event.colorArgb ushr 24) == 0xff) { "Màu sự kiện không hợp lệ" }
        if (event.endSolarDate == null) {
            require(event.endHour == null && event.endMinute == null) { "Ngày kết thúc không hợp lệ" }
        } else {
            val endDate = LocalDate.parse(event.endSolarDate)
            require(endDate in VietnameseCalendar.minDate..VietnameseCalendar.maxDate.plusDays(1)) { "Ngày kết thúc ngoài phạm vi" }
            if (event.allDay) {
                require(event.endHour == null && event.endMinute == null && endDate > date) { "Ngày kết thúc không hợp lệ" }
                require(java.time.temporal.ChronoUnit.DAYS.between(date, endDate) <= 366) { "Sự kiện không được kéo dài quá 366 ngày" }
            } else {
                val endHour = event.endHour
                val endMinute = event.endMinute
                require(endHour != null && endHour in 0..23 && endMinute != null && endMinute in 0..59) { "Giờ kết thúc không hợp lệ" }
                require(LocalDateTime.of(endDate, java.time.LocalTime.of(endHour, endMinute)) >
                    LocalDateTime.of(date, java.time.LocalTime.of(event.hour, event.minute))) { "Giờ kết thúc phải sau giờ bắt đầu" }
                require(java.time.Duration.between(date.atTime(event.hour, event.minute), endDate.atTime(endHour, endMinute)) <= java.time.Duration.ofDays(366)) { "Sự kiện không được kéo dài quá 366 ngày" }
            }
        }
        return event.copy(title = event.title.trim(), note = event.note.trim(), location = event.location.trim())
    }
}
