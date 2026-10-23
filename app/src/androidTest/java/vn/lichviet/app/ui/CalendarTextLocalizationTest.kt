package vn.lichviet.app.ui

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.lichviet.core.HolidayId
import vn.lichviet.core.HolidayOccurrenceId
import vn.lichviet.core.Almanac
import vn.lichviet.core.LunarDate
import vn.lichviet.core.PurposeId
import vn.lichviet.core.SolarTermId
import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class CalendarTextLocalizationTest {
    private val base = ApplicationProvider.getApplicationContext<Context>()
    private fun language(tag: String): Context = base.createConfigurationContext(Configuration(base.resources.configuration).apply {
        setLocale(Locale.forLanguageTag(tag))
    })

    @Test fun stableIdsRenderInVietnameseAndEnglishWithoutLabelMatching() {
        val vietnamese = language("vi-VN")
        val english = language("en-US")
        val tet = LocalDate.of(2026, 2, 17)
        val lunar = VietnameseCalendar.solarToLunar(tet)
        assertEquals("Tết Nguyên đán", CalendarText.holiday(vietnamese, HolidayOccurrenceId(HolidayId.TET)))
        assertEquals("Tết (Lunar New Year)", CalendarText.holiday(english, HolidayOccurrenceId(HolidayId.TET)))
        assertTrue(CalendarText.lunarFull(vietnamese, lunar).startsWith("Ngày 1 tháng 1"))
        assertTrue(CalendarText.lunarFull(english, lunar).startsWith("Lunar day 1, month 1"))
        assertFalse(CalendarText.dayCanChi(vietnamese, tet) == CalendarText.dayCanChi(english, tet))
        assertFalse(CalendarText.fullDate(vietnamese, tet) == CalendarText.fullDate(english, tet))
        SolarTermId.entries.forEach { assertTrue(CalendarText.solarTerm(english, it).isNotBlank()) }
        HolidayId.entries.forEach { assertTrue(CalendarText.holiday(english, HolidayOccurrenceId(it)).isNotBlank()) }
    }

    @Test fun datePatternsAndLeapSpacingBelongToTheSelectedLanguage() {
        val vietnamese = language("vi")
        val english = language("en")
        val date = LocalDate.of(2026, 2, 17)
        assertEquals("17/02/2026", CalendarText.shortDate(vietnamese, date))
        assertEquals("Feb 17, 2026", CalendarText.shortDate(english, date))
        assertEquals("Tháng 2 / 2026", CalendarText.monthYear(vietnamese, YearMonth.from(date)))
        assertEquals("February 2026", CalendarText.monthYear(english, YearMonth.from(date)))
        val leap = LunarDate(1, 2, 2023, true)
        assertEquals("1/2 nhuận", CalendarText.lunarShort(vietnamese, leap))
        assertEquals("1/2 (leap)", CalendarText.lunarShort(english, leap))
        assertTrue(CalendarText.monthCanChi(vietnamese, leap).endsWith(" (nhuận)"))
        assertTrue(CalendarText.monthCanChi(english, leap).endsWith(" (leap month)"))
        assertEquals("Full moon day (leap month)", CalendarText.holiday(english,
            HolidayOccurrenceId(HolidayId.FULL_MOON_DAY, true)))
    }

    @Test fun everyTypedAssessmentReasonHasBothTranslations() {
        val vietnamese = language("vi")
        val english = language("en")
        val first = LocalDate.of(2026, 1, 1)
        val reasons = (0L..365L).flatMap { day ->
            PurposeId.entries.flatMap { purpose -> Almanac.assessId(first.plusDays(day), purpose).reasons }
        }.distinct()
        assertEquals(vn.lichviet.core.AlmanacReasonId.entries.toSet(), reasons.map { it.id }.toSet())
        reasons.forEach { reason ->
            val vi = CalendarText.assessmentReason(vietnamese, reason)
            val en = CalendarText.assessmentReason(english, reason)
            assertTrue(vi.isNotBlank())
            assertTrue(en.isNotBlank())
            assertFalse(vi == en)
        }
    }
}
