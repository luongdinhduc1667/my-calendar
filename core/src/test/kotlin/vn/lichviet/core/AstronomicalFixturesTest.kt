package vn.lichviet.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * External fixtures are intentionally separate from roundtrip tests: an internally
 * consistent calendar could still give every lunar date the wrong month number.
 */
class AstronomicalFixturesTest {
    @Test
    fun `all 730 civil dates in 2026 and 2027 match independent HKO month fixtures`() {
        // https://www.hko.gov.hk/en/gts/time/calendar/pdf/files/2026e.pdf
        // https://www.hko.gov.hk/en/gts/time/calendar/pdf/files/2027e.pdf
        // These civil month starts also match NASA new moons shifted to UTC+7
        // (separately verified below), so no UTC+8-only table is substituted for
        // the Vietnamese calendar. Neither year has a leap lunar month.
        val starts = listOf(
            "2025-12-20" to LunarDate(1, 11, 2025),
            "2026-01-19" to LunarDate(1, 12, 2025),
            "2026-02-17" to LunarDate(1, 1, 2026),
            "2026-03-19" to LunarDate(1, 2, 2026),
            "2026-04-17" to LunarDate(1, 3, 2026),
            "2026-05-17" to LunarDate(1, 4, 2026),
            "2026-06-15" to LunarDate(1, 5, 2026),
            "2026-07-14" to LunarDate(1, 6, 2026),
            "2026-08-13" to LunarDate(1, 7, 2026),
            "2026-09-11" to LunarDate(1, 8, 2026),
            "2026-10-10" to LunarDate(1, 9, 2026),
            "2026-11-09" to LunarDate(1, 10, 2026),
            "2026-12-09" to LunarDate(1, 11, 2026),
            "2027-01-08" to LunarDate(1, 12, 2026),
            "2027-02-06" to LunarDate(1, 1, 2027),
            "2027-03-08" to LunarDate(1, 2, 2027),
            "2027-04-07" to LunarDate(1, 3, 2027),
            "2027-05-06" to LunarDate(1, 4, 2027),
            "2027-06-05" to LunarDate(1, 5, 2027),
            "2027-07-04" to LunarDate(1, 6, 2027),
            "2027-08-02" to LunarDate(1, 7, 2027),
            "2027-09-01" to LunarDate(1, 8, 2027),
            "2027-09-30" to LunarDate(1, 9, 2027),
            "2027-10-29" to LunarDate(1, 10, 2027),
            "2027-11-28" to LunarDate(1, 11, 2027),
            "2027-12-28" to LunarDate(1, 12, 2027),
        ).map { (date, lunar) -> LocalDate.parse(date) to lunar }
        var date = LocalDate.of(2026, 1, 1)
        var checked = 0
        while (date < LocalDate.of(2028, 1, 1)) {
            val fixture = starts.last { it.first <= date }
            val expected = fixture.second.copy(day = ChronoUnit.DAYS.between(fixture.first, date).toInt() + 1)
            assertEquals("Independent fixture for $date", expected, VietnameseCalendar.solarToLunar(date))
            assertEquals(date, VietnameseCalendar.lunarToSolar(expected))
            checked++
            date = date.plusDays(1)
        }
        assertEquals(730, checked)
    }

    @Test
    fun `all 25 new moons in 2026 and 2027 agree with NASA within one minute`() {
        // Fred Espenak, NASA GSFC: https://eclipse.gsfc.nasa.gov/phase/phases2001.html
        // Published precision: minute, Universal Time; these are not device times.
        val universalTimes = listOf(
            "2026-01-18T19:52", "2026-02-17T12:01", "2026-03-19T01:23",
            "2026-04-17T11:52", "2026-05-16T20:01", "2026-06-15T02:54",
            "2026-07-14T09:43", "2026-08-12T17:37", "2026-09-11T03:27",
            "2026-10-10T15:50", "2026-11-09T07:02", "2026-12-09T00:52",
            "2027-01-07T20:24", "2027-02-06T15:56", "2027-03-08T09:29",
            "2027-04-06T23:51", "2027-05-06T10:58", "2027-06-04T19:40",
            "2027-07-04T03:02", "2027-08-02T10:05", "2027-08-31T17:41",
            "2027-09-30T02:36", "2027-10-29T13:36", "2027-11-28T03:24",
            "2027-12-27T20:12",
        ).map(LocalDateTime::parse)
        var maximumErrorSeconds = 0.0
        universalTimes.forEach { fixture ->
            val expected = Astronomy.julianDay(fixture.toLocalDate()) + fixture.toLocalTime().toSecondOfDay() / 86400.0
            val estimate = Astronomy.estimateLunation(expected)
            val calculated = (estimate - 1..estimate + 1).map(Astronomy::newMoon).minBy { abs(it - expected) }
            val error = abs(calculated - expected) * 86400
            maximumErrorSeconds = maxOf(maximumErrorSeconds, error)
            assertTrue("$fixture UT differs by $error seconds", error < 60)
            assertEquals("Vietnamese civil day for $fixture", fixture.plusHours(7).toLocalDate(), Astronomy.localDate(calculated))
        }
        println(String.format(Locale.ROOT, "NASA 2026–2027: %d new moons; maximum difference %.3f seconds from minute-rounded fixtures", universalTimes.size, maximumErrorSeconds))
    }

    @Test
    fun `1985 month numbering follows Vietnamese winter solstice day rather than Chinese table`() {
        // Independent astronomy: RASC Observer's Handbook 1984, printed page 46
        // (PDF page 48), gives 21 December at 16:23 UT for winter solstice.
        // https://www.rasc.ca/sites/default/files/publications/ObserversHandbook-1984.pdf
        // NASA gives the next new moon as 22 December 1984 at 11:47 UT.
        // Thus solstice is 21 December in UTC+7 but 22 December in UTC+8,
        // while the new moon is 22 December in both zones. Different month 11
        // placement shifts Vietnamese Tet to 21 January instead of HKO 20 February.
        val expected = Astronomy.julianDay(LocalDate.of(1984, 12, 21)) + (16 * 60 + 23) / 1440.0
        val calculated = Astronomy.winterSolstice(1984)
        val errorMinutes = abs(calculated - expected) * 1440
        assertTrue("Low-precision solar model error: $errorMinutes minutes", errorMinutes < 20)
        assertEquals(LocalDate.of(1984, 12, 21), Astronomy.localDate(calculated))
        assertEquals(LunarDate(1, 11, 1984), VietnameseCalendar.solarToLunar(LocalDate.of(1984, 11, 23)))
        assertEquals(LunarDate(1, 12, 1984), VietnameseCalendar.solarToLunar(LocalDate.of(1984, 12, 22)))
        assertEquals(LunarDate(1, 1, 1985), VietnameseCalendar.solarToLunar(LocalDate.of(1985, 1, 21)))
        assertEquals(LunarDate(1, 2, 1985), VietnameseCalendar.solarToLunar(LocalDate.of(1985, 2, 20)))
        assertEquals(LunarDate(1, 2, 1985, true), VietnameseCalendar.solarToLunar(LocalDate.of(1985, 3, 21)))
        assertEquals(LunarDate(1, 3, 1985), VietnameseCalendar.solarToLunar(LocalDate.of(1985, 4, 20)))
        println(String.format(Locale.ROOT, "RASC winter solstice 1984: low-precision solar model difference %.3f minutes", errorMinutes))
    }
}
