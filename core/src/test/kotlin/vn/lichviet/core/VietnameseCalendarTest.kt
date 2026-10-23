package vn.lichviet.core

import java.time.LocalDate
import java.util.TimeZone
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class VietnameseCalendarTest {
    @Test
    fun `Tet agrees with independent published calendar fixtures`() {
        // HKO Gregorian–Lunar tables are independent fixtures for these dates,
        // on which UTC+7 and UTC+8 agree. Vietnamese 2026 fixture: government
        // fireworks schedule cited in README, confirming 17 February = 1/1.
        val fixtures = listOf(
            LocalDate.of(2020, 1, 25), LocalDate.of(2021, 2, 12),
            LocalDate.of(2022, 2, 1), LocalDate.of(2023, 1, 22),
            LocalDate.of(2024, 2, 10), LocalDate.of(2025, 1, 29),
            LocalDate.of(2026, 2, 17),
        )
        fixtures.forEach { date ->
            assertEquals(date.toString(), LunarDate(1, 1, date.year), VietnameseCalendar.solarToLunar(date))
            assertEquals(date, VietnameseCalendar.lunarToSolar(LunarDate(1, 1, date.year)))
        }
    }

    @Test
    fun `Vietnamese new year 1968 differs from Chinese calendar`() {
        // NASA's phase table: 1968-01-29 16:29 UT. This falls on Jan 29 in
        // UTC+7, but Jan 30 in UTC+8; HKO's 1968 table confirms Chinese Jan30.
        assertEquals(LunarDate(1, 1, 1968), VietnameseCalendar.solarToLunar(LocalDate.of(1968, 1, 29)))
        assertEquals(LunarDate(2, 1, 1968), VietnameseCalendar.solarToLunar(LocalDate.of(1968, 1, 30)))
        assertEquals(LocalDate.of(1968, 1, 29), VietnameseCalendar.lunarToSolar(LunarDate(1, 1, 1968)))
    }

    @Test
    fun `new moon ephemeris agrees with NASA minute precision`() {
        val fixture = Astronomy.julianDay(LocalDate.of(1968, 1, 29)) + (16 * 60 + 29) / 1440.0
        val estimate = Astronomy.estimateLunation(fixture)
        val calculated = (estimate - 1..estimate + 1).map { Astronomy.newMoon(it) }.minBy { abs(it - fixture) }
        assertTrue("New moon must be within one minute of NASA fixture", abs(calculated - fixture) * 86400 < 60)
        assertEquals(LocalDate.of(1968, 1, 29), Astronomy.localDate(calculated))
    }

    @Test
    fun `leap month is distinct from regular month and preserves year numbering`() {
        assertEquals(LocalDate.of(2023, 2, 20), VietnameseCalendar.lunarToSolar(LunarDate(1, 2, 2023)))
        assertEquals(LocalDate.of(2023, 3, 22), VietnameseCalendar.lunarToSolar(LunarDate(1, 2, 2023, true)))
        assertEquals(LunarDate(1, 3, 2023), VietnameseCalendar.solarToLunar(LocalDate.of(2023, 4, 20)))
        assertEquals(LunarDate(1, 6, 2025, true), VietnameseCalendar.solarToLunar(LocalDate.of(2025, 7, 25)))
        // The exceptional late leap month in 2033 catches simplistic rules that
        // assume a leap month can only occur between spring and autumn.
        assertEquals(LunarDate(1, 11, 2033, true), VietnameseCalendar.solarToLunar(LocalDate.of(2033, 12, 22)))
        assertNull(VietnameseCalendar.lunarToSolar(LunarDate(1, 2, 2026, true)))
    }

    @Test
    fun `every supported solar date roundtrips and months have 29 or 30 days`() {
        var date = VietnameseCalendar.minDate
        var previous: LunarDate? = null
        var visited = 0
        while (date <= VietnameseCalendar.maxDate) {
            val lunar = VietnameseCalendar.solarToLunar(date)
            assertTrue("$date returned invalid lunar day $lunar", lunar.day in 1..30)
            assertTrue(lunar.month in 1..12)
            assertEquals("Roundtrip $date $lunar", date, VietnameseCalendar.lunarToSolar(lunar))
            previous?.let {
                if (lunar.day == 1) assertTrue("Short month before $date", it.day == 29 || it.day == 30)
                else assertEquals(it.day + 1, lunar.day)
            }
            previous = lunar
            date = date.plusDays(1)
            visited++
        }
        assertEquals(73414, visited)
    }

    @Test
    fun `invalid dates are rejected rather than normalized into another month`() {
        assertNull(VietnameseCalendar.lunarToSolar(LunarDate(0, 1, 2026)))
        assertNull(VietnameseCalendar.lunarToSolar(LunarDate(31, 1, 2026)))
        assertNull(VietnameseCalendar.lunarToSolar(LunarDate(1, 13, 2026)))
        // 2026 month12 of 2025 is a 29-day month, confirmed by Vietnamese
        // government fixture 16 February 2026 = 29 tháng Chạp.
        assertNull(VietnameseCalendar.lunarToSolar(LunarDate(30, 12, 2025)))
        assertNull(VietnameseCalendar.lunarToSolar(LunarDate(1, 1, 2200)))
        try {
            VietnameseCalendar.solarToLunar(LocalDate.of(1899, 12, 31))
            fail("Unsupported date should throw")
        } catch (_: IllegalArgumentException) { }
    }

    @Test
    fun `calculation does not depend on process default time zone`() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"))
            val west = VietnameseCalendar.solarToLunar(LocalDate.of(1968, 1, 29))
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
            assertEquals(west, VietnameseCalendar.solarToLunar(LocalDate.of(1968, 1, 29)))
            assertEquals("Asia/Ho_Chi_Minh", VietnameseCalendar.zoneId.id)
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun `can chi known reference and twelve branch hours`() {
        assertEquals("Giáp Tý", VietnameseCalendar.dayCanChi(LocalDate.of(2000, 1, 7)))
        assertEquals("Đinh Mùi", VietnameseCalendar.dayCanChi(LocalDate.of(2026, 2, 2)))
        assertEquals("Bính Ngọ", VietnameseCalendar.yearCanChi(2026))
        assertEquals("Kỷ Sửu", VietnameseCalendar.monthCanChi(LunarDate(15, 12, 2025)))
        val hours = VietnameseCalendar.auspiciousHours(LocalDate.of(2000, 1, 7))
        assertEquals(6, hours.size)
        assertEquals(listOf("Tý", "Sửu", "Mão", "Ngọ", "Thân", "Dậu"), hours.map { it.substringBefore(' ') })
    }

    @Test
    fun `holidays use Vietnamese lunar date and do not repeat Tet in leap months`() {
        assertTrue("Tết Nguyên đán" in VietnameseCalendar.holidays(LocalDate.of(2026, 2, 17)))
        assertTrue("Giao thừa" in VietnameseCalendar.holidays(LocalDate.of(2026, 2, 16)))
        assertTrue("Tết Trung thu" in VietnameseCalendar.holidays(LocalDate.of(2026, 9, 25)))
        assertFalse("Tết Nguyên tiêu" in VietnameseCalendar.holidays(LocalDate.of(2023, 4, 5)))
    }

    @Test
    fun `term tracks the season at Vietnam noon`() {
        assertEquals("Đông chí", VietnameseCalendar.solarTerm(LocalDate.of(2026, 12, 23)))
        assertEquals("Hạ chí", VietnameseCalendar.solarTerm(LocalDate.of(2026, 6, 22)))
        assertEquals("Lập xuân", VietnameseCalendar.solarTerm(LocalDate.of(2026, 2, 5)))
    }

    @Test
    fun `date search is bounded and every result exposes selection reasons`() {
        val start = LocalDate.of(2026, 10, 1)
        val end = LocalDate.of(2026, 10, 31)
        val dates = Almanac.findDates(start, end, "Khai trương")
        assertTrue("Expected at least one candidate", dates.isNotEmpty())
        dates.forEach {
            assertTrue(it in start..end)
            val assessment = Almanac.assess(it, "Khai trương")
            assertTrue(assessment.isRecommended)
            assertTrue(assessment.reasons.size >= 3)
            assertTrue(assessment.reasons.any { reason -> "tham khảo" in reason })
            assertFalse(VietnameseCalendar.solarToLunar(it).day in setOf(3, 5, 7, 13, 14, 18, 22, 23, 27))
        }
    }

    @Test
    fun `stable cultural IDs preserve calculations without interpreting labels`() {
        assertEquals(CanChiId(0, 0), VietnameseCalendar.dayCanChiId(LocalDate.of(2000, 1, 7)))
        assertEquals(CanChiId(2, 6), VietnameseCalendar.yearCanChiId(2026))
        assertTrue(VietnameseCalendar.monthCanChiId(LunarDate(1, 2, 2023, true)).isLeapMonth)
        assertEquals(SolarTermId.WINTER_SOLSTICE, VietnameseCalendar.solarTermId(LocalDate.of(2026, 12, 23)))
        assertTrue(VietnameseCalendar.holidayIds(LocalDate.of(2026, 2, 17)).any { it.id == HolidayId.TET })
        assertTrue(VietnameseCalendar.holidayIds(LocalDate.of(2023, 4, 5)).none { it.id == HolidayId.LANTERN_FESTIVAL })
        val start = LocalDate.of(2026, 10, 1)
        val end = LocalDate.of(2026, 10, 31)
        assertEquals(Almanac.findDates(start, end, "Khai trương"),
            Almanac.findDates(start, end, PurposeId.OPENING))
        assertEquals(Almanac.assess(start, "Khai trương").isRecommended,
            Almanac.assessId(start, PurposeId.OPENING).isRecommended)
    }
}
