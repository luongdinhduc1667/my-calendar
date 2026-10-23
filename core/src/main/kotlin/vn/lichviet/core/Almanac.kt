package vn.lichviet.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.floor

data class DayAssessment(val isRecommended: Boolean, val reasons: List<String>, val label: String)
enum class PurposeId { OPENING, WEDDING, MOVING, TRAVEL }
enum class OfficerId { KIEN, TRU, MAN, BINH, DINH, CHAP, PHA, NGUY, THANH, THU, KHAI, BE }
enum class AssessmentLabelId { CANDIDATE, CONSIDER }
enum class AlmanacReasonId { AUSPICIOUS_DAY, ORDINARY_DAY, PREFERRED_OFFICER, OTHER_OFFICER, TAM_NUONG, NGUYET_KY, CULTURAL_REFERENCE }
data class AlmanacReason(val id: AlmanacReasonId, val officer: OfficerId? = null, val purpose: PurposeId? = null)
data class TypedDayAssessment(val isRecommended: Boolean, val reasons: List<AlmanacReason>, val labelId: AssessmentLabelId)

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
    fun dayOfficerId(date: LocalDate): OfficerId = OfficerId.entries[officerIndex(date)]

    private fun officerIndex(date: LocalDate): Int {
        VietnameseCalendar.requireSupported(date)
        val solarMonth = floor(Astronomy.normalize(VietnameseCalendar.solarLongitudeAtNoon(date) - 315) / 30).toInt()
        val monthBranch = (solarMonth + 2) % 12
        return Math.floorMod(VietnameseCalendar.dayBranch(date) - monthBranch, 12)
    }

    fun assessId(date: LocalDate, purpose: PurposeId): TypedDayAssessment {
        val lunar = VietnameseCalendar.solarToLunar(date)
        val goodDay = isAuspiciousDay(date)
        val officer = officerIndex(date)
        // Explicit product convention: a small subset of traditional rules. There is
        // no hidden scoring and no claim of agreement with every published almanac.
        val preferred = when (purpose) {
            PurposeId.OPENING -> setOf(2, 8, 9, 10)
            PurposeId.WEDDING -> setOf(4, 8, 10)
            PurposeId.MOVING -> setOf(4, 8, 10)
            PurposeId.TRAVEL -> setOf(2, 8, 10)
        }
        val avoided = lunar.day in tamNuong || lunar.day in nguyetKy
        val reasons = buildList {
            add(AlmanacReason(if (goodDay) AlmanacReasonId.AUSPICIOUS_DAY else AlmanacReasonId.ORDINARY_DAY))
            add(AlmanacReason(if (officer in preferred) AlmanacReasonId.PREFERRED_OFFICER else AlmanacReasonId.OTHER_OFFICER,
                OfficerId.entries[officer], purpose))
            if (lunar.day in tamNuong) add(AlmanacReason(AlmanacReasonId.TAM_NUONG))
            if (lunar.day in nguyetKy) add(AlmanacReason(AlmanacReasonId.NGUYET_KY))
            add(AlmanacReason(AlmanacReasonId.CULTURAL_REFERENCE))
        }
        // Either a hoàng đạo day or a purpose-related officer is a candidate.
        // Requiring both can incorrectly leave entire solar months with no
        // candidates because the two independent cycles need not intersect.
        val recommended = (goodDay || officer in preferred) && !avoided
        return TypedDayAssessment(recommended, reasons,
            if (recommended) AssessmentLabelId.CANDIDATE else AssessmentLabelId.CONSIDER)
    }

    fun assess(date: LocalDate, purpose: String): DayAssessment {
        val id = purposeId(purpose)
        val typed = assessId(date, id)
        val reasons = typed.reasons.map { reason -> when (reason.id) {
            AlmanacReasonId.AUSPICIOUS_DAY -> "Ngày hoàng đạo theo tháng âm"
            AlmanacReasonId.ORDINARY_DAY -> "Ngày không thuộc nhóm hoàng đạo theo tháng âm"
            AlmanacReasonId.PREFERRED_OFFICER -> "Trực ${officers[reason.officer!!.ordinal]}: thuộc nhóm tham khảo cho ${purpose.lowercase()}"
            AlmanacReasonId.OTHER_OFFICER -> "Trực ${officers[reason.officer!!.ordinal]}: ngoài nhóm tham khảo cho ${purpose.lowercase()}"
            AlmanacReasonId.TAM_NUONG -> "Ngày Tam Nương theo quan niệm dân gian"
            AlmanacReasonId.NGUYET_KY -> "Ngày Nguyệt Kỵ theo quan niệm dân gian"
            AlmanacReasonId.CULTURAL_REFERENCE -> "Chỉ tham khảo văn hóa; chưa xét tuổi, giờ sinh hoặc các hệ xem ngày khác"
        } }
        return DayAssessment(typed.isRecommended, reasons,
            if (typed.labelId == AssessmentLabelId.CANDIDATE) "Có thể tham khảo" else "Cân nhắc thêm")
    }

    private fun purposeId(purpose: String): PurposeId {
        val index = purposes.indexOf(purpose)
        require(index >= 0) { "Mục đích được hỗ trợ: ${purposes.joinToString()}" }
        return PurposeId.entries[index]
    }

    /** Inclusive date search. Up to one calendar year per query keeps the UI responsive. */
    fun findDates(start: LocalDate, end: LocalDate, purpose: String): List<LocalDate> {
        return findDates(start, end, purposeId(purpose))
    }

    fun findDates(start: LocalDate, end: LocalDate, purpose: PurposeId): List<LocalDate> {
        VietnameseCalendar.requireSupported(start)
        VietnameseCalendar.requireSupported(end)
        require(!end.isBefore(start)) { "Ngày kết thúc phải từ ngày bắt đầu trở đi" }
        require(ChronoUnit.DAYS.between(start, end) <= 366) { "Mỗi lần tra cứu tối đa 367 ngày" }
        return generateSequence(start) { it.plusDays(1) }.takeWhile { it <= end }
            .filter { assessId(it, purpose).isRecommended }.toList()
    }
}
