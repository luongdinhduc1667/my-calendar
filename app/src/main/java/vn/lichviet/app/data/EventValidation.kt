package vn.lichviet.app.data

import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate

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
        return event.copy(title = event.title.trim(), note = event.note.trim())
    }
}
