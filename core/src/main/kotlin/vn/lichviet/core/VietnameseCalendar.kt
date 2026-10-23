package vn.lichviet.core

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.floor

data class LunarDate(val day: Int, val month: Int, val year: Int, val isLeap: Boolean = false) {
    val shortLabel: String get() = "$day/$month${if (isLeap) " nhuận" else ""}"
    val fullLabel: String get() = "Ngày $day tháng $month${if (isLeap) " nhuận" else ""}, năm ${VietnameseCalendar.yearCanChi(year)}"
}

/** Stable cultural identifiers. Presentation belongs to the Android resource layer. */
data class CanChiId(val stemIndex: Int, val branchIndex: Int, val isLeapMonth: Boolean = false)
data class AuspiciousHourId(val branchIndex: Int, val fromHour: Int, val toHour: Int)
enum class SolarTermId {
    SPRING_EQUINOX, CLEAR_AND_BRIGHT, GRAIN_RAIN, START_OF_SUMMER, GRAIN_FULL,
    GRAIN_IN_EAR, SUMMER_SOLSTICE, MINOR_HEAT, MAJOR_HEAT, START_OF_AUTUMN,
    END_OF_HEAT, WHITE_DEW, AUTUMN_EQUINOX, COLD_DEW, FROST_DESCENT,
    START_OF_WINTER, MINOR_SNOW, MAJOR_SNOW, WINTER_SOLSTICE, MINOR_COLD,
    MAJOR_COLD, START_OF_SPRING, RAIN_WATER, AWAKENING_OF_INSECTS,
}
enum class HolidayId {
    NEW_YEAR, VALENTINES_DAY, INTERNATIONAL_WOMENS_DAY, REUNIFICATION_DAY,
    LABOUR_DAY, NATIONAL_DAY, VIETNAMESE_WOMENS_DAY, TEACHERS_DAY, CHRISTMAS,
    TET, TET_SECOND_DAY, TET_THIRD_DAY, LANTERN_FESTIVAL, COLD_FOOD_FESTIVAL,
    HUNG_KINGS_DAY, VESAK, DRAGON_BOAT_FESTIVAL, VU_LAN, MID_AUTUMN_FESTIVAL,
    KITCHEN_GODS_DAY, FIRST_LUNAR_DAY, FULL_MOON_DAY, NEW_YEARS_EVE,
    VIETNAMESE_CULTURE_DAY,
}
data class HolidayOccurrenceId(val id: HolidayId, val isLeapMonth: Boolean = false)

data class CalendarDayData(val lunar: LunarDate, val holidays: List<HolidayOccurrenceId>)
data class IsoCalendarWeek(val number: Int, val year: Int)

/**
 * Modern Vietnamese calendar, calculated offline with a fixed UTC+7 calendar meridian.
 * Supported solar dates: 1900-01-01 through 2100-12-31, inclusive. Dates preceding
 * 1968 use these same modern rules, not a reconstruction of regional historical calendars.
 */
object VietnameseCalendar {
    val zoneId: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
    val minDate: LocalDate = LocalDate.of(1900, 1, 1)
    val maxDate: LocalDate = LocalDate.of(2100, 12, 31)
    private val dayCache = object : LinkedHashMap<LocalDate, CalendarDayData>(512, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<LocalDate, CalendarDayData>) = size > 512
    }

    /** Bounded, thread-safe cache shared by grid, detail, holiday list and widgets.
     * Labels are deliberately excluded so changing the app language cannot stale them. */
    fun dayData(date: LocalDate): CalendarDayData = synchronized(dayCache) {
        dayCache.getOrPut(date) {
            val lunar = solarToLunar(date)
            CalendarDayData(lunar, computeHolidayIds(date, lunar))
        }
    }

    /** Call from a background dispatcher before showing calendar calculations. */
    fun warmUp() { solarToLunar(minDate) }

    /** ISO numbering uses the Thursday inside the displayed Monday/Sunday-first row. */
    fun isoWeekForRow(start: LocalDate): IsoCalendarWeek {
        val thursday = start.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.THURSDAY))
        val fields = java.time.temporal.WeekFields.ISO
        return IsoCalendarWeek(thursday.get(fields.weekOfWeekBasedYear()), thursday.get(fields.weekBasedYear()))
    }

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

    fun dayCanChiId(date: LocalDate): CanChiId {
        requireSupported(date)
        val jdn = julianDayNumber(date)
        return CanChiId(Math.floorMod(jdn + 9, 10), Math.floorMod(jdn + 1, 12))
    }

    fun dayCanChi(date: LocalDate): String = canChiLabel(dayCanChiId(date))

    fun monthCanChiId(lunar: LunarDate): CanChiId {
        require(lunar.month in 1..12) { "Tháng âm lịch phải từ 1 đến 12" }
        return CanChiId(Math.floorMod(lunar.year * 12 + lunar.month + 3, 10),
            (lunar.month + 1) % 12, lunar.isLeap)
    }

    fun monthCanChi(lunar: LunarDate): String = canChiLabel(monthCanChiId(lunar))

    fun yearCanChiId(year: Int): CanChiId = CanChiId(Math.floorMod(year + 6, 10), Math.floorMod(year + 8, 12))

    fun yearCanChi(year: Int): String = canChiLabel(yearCanChiId(year))

    private fun canChiLabel(id: CanChiId): String =
        "${stems[id.stemIndex]} ${branches[id.branchIndex]}${if (id.isLeapMonth) " (nhuận)" else ""}"

    /** Solar term prevailing at noon in Vietnam. This is not an exact transition-time API. */
    fun solarTermId(date: LocalDate): SolarTermId {
        requireSupported(date)
        return SolarTermId.entries[floor(solarLongitudeAtNoon(date) / 15).toInt()]
    }

    fun solarTerm(date: LocalDate): String = terms[solarTermId(date).ordinal]

    /** Traditional six auspicious double-hours, ordered from Tý to Hợi. */
    fun auspiciousHourIds(date: LocalDate): List<AuspiciousHourId> {
        requireSupported(date)
        val start = Math.floorMod(2 * (dayBranch(date) - 2), 12)
        val good = setOf(0, 1, 4, 5, 7, 10).map { (start + it) % 12 }.toSet()
        return (0..11).filter { it in good }.map {
            val from = if (it == 0) 23 else it * 2 - 1
            val to = it * 2 + 1
            AuspiciousHourId(it, from, to)
        }
    }

    fun auspiciousHours(date: LocalDate): List<String> = auspiciousHourIds(date).map {
        "${branches[it.branchIndex]} (${it.fromHour.toString().padStart(2, '0')}:00–${it.toHour.toString().padStart(2, '0')}:00)"
    }

    /** Named observances; not a legal public-holiday or compensatory leave schedule. */
    fun holidayIds(date: LocalDate): List<HolidayOccurrenceId> = dayData(date).holidays

    private fun computeHolidayIds(date: LocalDate, lunar: LunarDate): List<HolidayOccurrenceId> {
        return buildList {
            when (date.monthValue to date.dayOfMonth) {
                1 to 1 -> add(HolidayOccurrenceId(HolidayId.NEW_YEAR))
                2 to 14 -> add(HolidayOccurrenceId(HolidayId.VALENTINES_DAY))
                3 to 8 -> add(HolidayOccurrenceId(HolidayId.INTERNATIONAL_WOMENS_DAY))
                4 to 30 -> add(HolidayOccurrenceId(HolidayId.REUNIFICATION_DAY))
                5 to 1 -> add(HolidayOccurrenceId(HolidayId.LABOUR_DAY))
                9 to 2 -> add(HolidayOccurrenceId(HolidayId.NATIONAL_DAY))
                10 to 20 -> add(HolidayOccurrenceId(HolidayId.VIETNAMESE_WOMENS_DAY))
                11 to 20 -> add(HolidayOccurrenceId(HolidayId.TEACHERS_DAY))
                // Resolution 28/2026/QH16, effective 2026-07-01. No retrospective observance.
                11 to 24 -> if (date.year >= 2026) add(HolidayOccurrenceId(HolidayId.VIETNAMESE_CULTURE_DAY))
                12 to 25 -> add(HolidayOccurrenceId(HolidayId.CHRISTMAS))
            }
            if (!lunar.isLeap) {
                when (lunar.month to lunar.day) {
                    1 to 1 -> add(HolidayOccurrenceId(HolidayId.TET))
                    1 to 2 -> add(HolidayOccurrenceId(HolidayId.TET_SECOND_DAY))
                    1 to 3 -> add(HolidayOccurrenceId(HolidayId.TET_THIRD_DAY))
                    1 to 15 -> add(HolidayOccurrenceId(HolidayId.LANTERN_FESTIVAL))
                    3 to 3 -> add(HolidayOccurrenceId(HolidayId.COLD_FOOD_FESTIVAL))
                    3 to 10 -> add(HolidayOccurrenceId(HolidayId.HUNG_KINGS_DAY))
                    4 to 15 -> add(HolidayOccurrenceId(HolidayId.VESAK))
                    5 to 5 -> add(HolidayOccurrenceId(HolidayId.DRAGON_BOAT_FESTIVAL))
                    7 to 15 -> add(HolidayOccurrenceId(HolidayId.VU_LAN))
                    8 to 15 -> add(HolidayOccurrenceId(HolidayId.MID_AUTUMN_FESTIVAL))
                    12 to 23 -> add(HolidayOccurrenceId(HolidayId.KITCHEN_GODS_DAY))
                }
            }
            if (lunar.day == 1) add(HolidayOccurrenceId(HolidayId.FIRST_LUNAR_DAY, lunar.isLeap))
            if (lunar.day == 15) add(HolidayOccurrenceId(HolidayId.FULL_MOON_DAY, lunar.isLeap))
            if (date < maxDate) {
                val tomorrow = solarToLunar(date.plusDays(1))
                if (tomorrow.month == 1 && tomorrow.day == 1 && !tomorrow.isLeap) add(HolidayOccurrenceId(HolidayId.NEW_YEARS_EVE))
            }
        }
    }

    fun holidays(date: LocalDate): List<String> = holidayIds(date).map { occurrence ->
        when (occurrence.id) {
            HolidayId.NEW_YEAR -> "Tết Dương lịch"
            HolidayId.VALENTINES_DAY -> "Lễ Tình nhân"
            HolidayId.INTERNATIONAL_WOMENS_DAY -> "Ngày Quốc tế Phụ nữ"
            HolidayId.REUNIFICATION_DAY -> "Ngày Giải phóng miền Nam"
            HolidayId.LABOUR_DAY -> "Ngày Quốc tế Lao động"
            HolidayId.NATIONAL_DAY -> "Quốc khánh Việt Nam"
            HolidayId.VIETNAMESE_WOMENS_DAY -> "Ngày Phụ nữ Việt Nam"
            HolidayId.TEACHERS_DAY -> "Ngày Nhà giáo Việt Nam"
            HolidayId.CHRISTMAS -> "Giáng sinh"
            HolidayId.TET -> "Tết Nguyên đán"
            HolidayId.TET_SECOND_DAY -> "Mùng 2 Tết"
            HolidayId.TET_THIRD_DAY -> "Mùng 3 Tết"
            HolidayId.LANTERN_FESTIVAL -> "Tết Nguyên tiêu"
            HolidayId.COLD_FOOD_FESTIVAL -> "Tết Hàn thực"
            HolidayId.HUNG_KINGS_DAY -> "Giỗ Tổ Hùng Vương"
            HolidayId.VESAK -> "Lễ Phật đản"
            HolidayId.DRAGON_BOAT_FESTIVAL -> "Tết Đoan ngọ"
            HolidayId.VU_LAN -> "Lễ Vu lan"
            HolidayId.MID_AUTUMN_FESTIVAL -> "Tết Trung thu"
            HolidayId.KITCHEN_GODS_DAY -> "Ông Công, Ông Táo"
            HolidayId.FIRST_LUNAR_DAY -> "Mùng một${if (occurrence.isLeapMonth) " tháng nhuận" else ""}"
            HolidayId.FULL_MOON_DAY -> "Ngày rằm${if (occurrence.isLeapMonth) " tháng nhuận" else ""}"
            HolidayId.NEW_YEARS_EVE -> "Giao thừa"
            HolidayId.VIETNAMESE_CULTURE_DAY -> "Ngày Văn hóa Việt Nam"
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
