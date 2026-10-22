package vn.lichviet.core

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.floor

data class LunarDate(val day: Int, val month: Int, val year: Int, val isLeap: Boolean = false) {
    val shortLabel: String get() = "$day/$month${if (isLeap) " nhuận" else ""}"
    val fullLabel: String get() = "Ngày $day tháng $month${if (isLeap) " nhuận" else ""}, năm ${VietnameseCalendar.yearCanChi(year)}"
}

/**
 * Modern Vietnamese calendar, calculated offline with a fixed UTC+7 calendar meridian.
 * Supported solar dates: 1900-01-01 through 2100-12-31, inclusive. Dates preceding
 * 1968 use these same modern rules, not a reconstruction of regional historical calendars.
 */
object VietnameseCalendar {
    val zoneId: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
    val minDate: LocalDate = LocalDate.of(1900, 1, 1)
    val maxDate: LocalDate = LocalDate.of(2100, 12, 31)

    internal val stems = listOf("Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý")
    internal val branches = listOf("Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi")
    private val terms = listOf(
        "Xuân phân", "Thanh minh", "Cốc vũ", "Lập hạ", "Tiểu mãn", "Mang chủng",
        "Hạ chí", "Tiểu thử", "Đại thử", "Lập thu", "Xử thử", "Bạch lộ",
        "Thu phân", "Hàn lộ", "Sương giáng", "Lập đông", "Tiểu tuyết", "Đại tuyết",
        "Đông chí", "Tiểu hàn", "Đại hàn", "Lập xuân", "Vũ thủy", "Kinh trập",
    )

    private data class Month(val start: LocalDate, val end: LocalDate, val lunar: LunarDate)

    // Immutable lookup is generated from astronomy once. There is no bundled Chinese
    // calendar table and no dependency on the device time zone or a network service.
    private val months: List<Month> by lazy {
        val newMoonDays = HashMap<Int, LocalDate>()
        fun moonDay(k: Int): LocalDate = newMoonDays.getOrPut(k) { Astronomy.localDate(Astronomy.newMoon(k)) }
        fun monthElevenLunation(year: Int): Int {
            val solstice = Astronomy.winterSolstice(year)
            val solsticeDate = Astronomy.localDate(solstice)
            var k = Astronomy.estimateLunation(solstice)
            while (moonDay(k).isAfter(solsticeDate)) k--
            while (!moonDay(k + 1).isAfter(solsticeDate)) k++
            return k
        }
        buildList {
            for (solsticeYear in 1898..2101) {
                val first = monthElevenLunation(solsticeYear)
                val next = monthElevenLunation(solsticeYear + 1)
                val count = next - first
                check(count == 12 || count == 13)
                val leapIndex = if (count == 13) {
                    (1 until count).first { offset ->
                        val startSector = floor(Astronomy.solarLongitude(Astronomy.localMidnight(moonDay(first + offset))) / 30).toInt()
                        val endSector = floor(Astronomy.solarLongitude(Astronomy.localMidnight(moonDay(first + offset + 1))) / 30).toInt()
                        startSector == endSector
                    }
                } else -1
                var month = 11
                var year = solsticeYear
                for (i in 0 until count) {
                    val leap = i == leapIndex
                    if (i > 0 && !leap) {
                        month++
                        if (month == 13) {
                            month = 1
                            year++
                        }
                    }
                    add(Month(moonDay(first + i), moonDay(first + i + 1), LunarDate(1, month, year, leap)))
                }
            }
        }
    }

    fun solarToLunar(date: LocalDate): LunarDate {
        requireSupported(date)
        var low = 0
        var high = months.lastIndex
        while (low <= high) {
            val mid = (low + high).ushr(1)
            if (months[mid].start <= date) low = mid + 1 else high = mid - 1
        }
        val month = months[high]
        return month.lunar.copy(day = ChronoUnit.DAYS.between(month.start, date).toInt() + 1)
    }

    /** Returns null for an impossible day, nonexistent leap month, or unsupported result. */
    fun lunarToSolar(lunar: LunarDate): LocalDate? {
        if (lunar.day !in 1..30 || lunar.month !in 1..12 || lunar.year !in 1899..2100) return null
        val month = months.firstOrNull {
            it.lunar.year == lunar.year && it.lunar.month == lunar.month && it.lunar.isLeap == lunar.isLeap
        } ?: return null
        val date = month.start.plusDays(lunar.day.toLong() - 1)
        return date.takeIf { it < month.end && it in minDate..maxDate }
    }

    fun dayCanChi(date: LocalDate): String {
        requireSupported(date)
        val jdn = julianDayNumber(date)
        return "${stems[Math.floorMod(jdn + 9, 10)]} ${branches[Math.floorMod(jdn + 1, 12)]}"
    }

    fun monthCanChi(lunar: LunarDate): String {
        require(lunar.month in 1..12) { "Tháng âm lịch phải từ 1 đến 12" }
        return "${stems[Math.floorMod(lunar.year * 12 + lunar.month + 3, 10)]} ${branches[(lunar.month + 1) % 12]}${if (lunar.isLeap) " (nhuận)" else ""}"
    }

    fun yearCanChi(year: Int): String =
        "${stems[Math.floorMod(year + 6, 10)]} ${branches[Math.floorMod(year + 8, 12)]}"

    /** Solar term prevailing at noon in Vietnam. This is not an exact transition-time API. */
    fun solarTerm(date: LocalDate): String {
        requireSupported(date)
        return terms[floor(solarLongitudeAtNoon(date) / 15).toInt()]
    }

    /** Traditional six auspicious double-hours, ordered from Tý to Hợi. */
    fun auspiciousHours(date: LocalDate): List<String> {
        requireSupported(date)
        val start = Math.floorMod(2 * (dayBranch(date) - 2), 12)
        val good = setOf(0, 1, 4, 5, 7, 10).map { (start + it) % 12 }.toSet()
        return (0..11).filter { it in good }.map {
            val from = if (it == 0) 23 else it * 2 - 1
            val to = it * 2 + 1
            "${branches[it]} (${from.toString().padStart(2, '0')}:00–${to.toString().padStart(2, '0')}:00)"
        }
    }

    /** Named observances; not a legal public-holiday or compensatory leave schedule. */
    fun holidays(date: LocalDate): List<String> {
        val lunar = solarToLunar(date)
        return buildList {
            when (date.monthValue to date.dayOfMonth) {
                1 to 1 -> add("Tết Dương lịch")
                2 to 14 -> add("Lễ Tình nhân")
                3 to 8 -> add("Ngày Quốc tế Phụ nữ")
                4 to 30 -> add("Ngày Giải phóng miền Nam")
                5 to 1 -> add("Ngày Quốc tế Lao động")
                9 to 2 -> add("Quốc khánh Việt Nam")
                10 to 20 -> add("Ngày Phụ nữ Việt Nam")
                11 to 20 -> add("Ngày Nhà giáo Việt Nam")
                12 to 25 -> add("Giáng sinh")
            }
            if (!lunar.isLeap) {
                when (lunar.month to lunar.day) {
                    1 to 1 -> add("Tết Nguyên đán")
                    1 to 2 -> add("Mùng 2 Tết")
                    1 to 3 -> add("Mùng 3 Tết")
                    1 to 15 -> add("Tết Nguyên tiêu")
                    3 to 3 -> add("Tết Hàn thực")
                    3 to 10 -> add("Giỗ Tổ Hùng Vương")
                    4 to 15 -> add("Lễ Phật đản")
                    5 to 5 -> add("Tết Đoan ngọ")
                    7 to 15 -> add("Lễ Vu lan")
                    8 to 15 -> add("Tết Trung thu")
                    12 to 23 -> add("Ông Công, Ông Táo")
                }
            }
            if (lunar.day == 1) add("Mùng một${if (lunar.isLeap) " tháng nhuận" else ""}")
            if (lunar.day == 15) add("Ngày rằm${if (lunar.isLeap) " tháng nhuận" else ""}")
            if (date < maxDate) {
                val tomorrow = solarToLunar(date.plusDays(1))
                if (tomorrow.month == 1 && tomorrow.day == 1 && !tomorrow.isLeap) add("Giao thừa")
            }
        }
    }

    internal fun requireSupported(date: LocalDate) {
        require(date in minDate..maxDate) { "Hỗ trợ ngày dương lịch từ 01/01/1900 đến 31/12/2100" }
    }

    internal fun dayBranch(date: LocalDate): Int = Math.floorMod(julianDayNumber(date) + 1, 12)
    internal fun solarLongitudeAtNoon(date: LocalDate): Double =
        Astronomy.solarLongitude(Astronomy.localMidnight(date) + 0.5)
    private fun julianDayNumber(date: LocalDate): Int = (date.toEpochDay() + 2440588L).toInt()
}
