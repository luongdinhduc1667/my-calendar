package vn.lichviet.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.floor

data class DayAssessment(val isRecommended: Boolean, val reasons: List<String>, val label: String)

/**
 * A deliberately limited, explainable folk-calendar filter for cultural reference.
 * It is not a prediction, a personalized birth-chart calculation, or a full Thông thư.
 * Reasons expose every rule so that callers never need to present a fabricated score.
 */
object Almanac {
    val purposes: List<String> = listOf("Khai trương", "Cưới hỏi", "Chuyển nhà", "Xuất hành")
    private val officers = listOf("Kiến", "Trừ", "Mãn", "Bình", "Định", "Chấp", "Phá", "Nguy", "Thành", "Thu", "Khai", "Bế")
    private val tamNuong = setOf(3, 7, 13, 18, 22, 27)
    private val nguyetKy = setOf(5, 14, 23)

    fun isAuspiciousDay(date: LocalDate): Boolean {
        val lunar = VietnameseCalendar.solarToLunar(date)
        val shift = ((lunar.month - 1) % 6) * 2
        return VietnameseCalendar.dayBranch(date) in setOf(0, 1, 5, 7).map { (it + shift) % 12 }
    }

    /** Twelve officers use the solar month (changes at the twelve tiết), not lunar month. */
    fun dayOfficer(date: LocalDate): String = officers[officerIndex(date)]

    private fun officerIndex(date: LocalDate): Int {
        VietnameseCalendar.requireSupported(date)
        val solarMonth = floor(Astronomy.normalize(VietnameseCalendar.solarLongitudeAtNoon(date) - 315) / 30).toInt()
        val monthBranch = (solarMonth + 2) % 12
        return Math.floorMod(VietnameseCalendar.dayBranch(date) - monthBranch, 12)
    }

    fun assess(date: LocalDate, purpose: String): DayAssessment {
        require(purpose in purposes) { "Mục đích được hỗ trợ: ${purposes.joinToString()}" }
        val lunar = VietnameseCalendar.solarToLunar(date)
        val goodDay = isAuspiciousDay(date)
        val officer = officerIndex(date)
        // Explicit product convention: a small subset of traditional rules. There is
        // no hidden scoring and no claim of agreement with every published almanac.
        val preferred = when (purpose) {
            "Khai trương" -> setOf(2, 8, 9, 10)
            "Cưới hỏi" -> setOf(4, 8, 10)
            "Chuyển nhà" -> setOf(4, 8, 10)
            else -> setOf(2, 8, 10)
        }
        val avoided = lunar.day in tamNuong || lunar.day in nguyetKy
        val reasons = buildList {
            add(if (goodDay) "Ngày hoàng đạo theo tháng âm" else "Ngày không thuộc nhóm hoàng đạo theo tháng âm")
            add("Trực ${officers[officer]}${if (officer in preferred) ": thuộc nhóm tham khảo cho ${purpose.lowercase()}" else ": ngoài nhóm tham khảo cho ${purpose.lowercase()}"}")
            if (lunar.day in tamNuong) add("Ngày Tam Nương theo quan niệm dân gian")
            if (lunar.day in nguyetKy) add("Ngày Nguyệt Kỵ theo quan niệm dân gian")
            add("Chỉ tham khảo văn hóa; chưa xét tuổi, giờ sinh hoặc các hệ xem ngày khác")
        }
        // Either a hoàng đạo day or a purpose-related officer is a candidate.
        // Requiring both can incorrectly leave entire solar months with no
        // candidates because the two independent cycles need not intersect.
        val recommended = (goodDay || officer in preferred) && !avoided
        return DayAssessment(recommended, reasons, if (recommended) "Có thể tham khảo" else "Cân nhắc thêm")
    }

    /** Inclusive date search. Up to one calendar year per query keeps the UI responsive. */
    fun findDates(start: LocalDate, end: LocalDate, purpose: String): List<LocalDate> {
        VietnameseCalendar.requireSupported(start)
        VietnameseCalendar.requireSupported(end)
        require(!end.isBefore(start)) { "Ngày kết thúc phải từ ngày bắt đầu trở đi" }
        require(ChronoUnit.DAYS.between(start, end) <= 366) { "Mỗi lần tra cứu tối đa 367 ngày" }
        return generateSequence(start) { it.plusDays(1) }.takeWhile { it <= end }
            .filter { assess(it, purpose).isRecommended }.toList()
    }
}
