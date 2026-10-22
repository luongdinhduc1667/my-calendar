package vn.lichviet.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecurrenceTest {
    private fun date(value: String) = LocalDate.parse(value)
    private fun event(anchor: String, repeat: RepeatRule, missing: MissingDayPolicy = MissingDayPolicy.LAST_DAY) =
        EventEntity(title = "Lịch gia đình", solarDate = anchor, repeat = repeat, missingDayPolicy = missing)

    @Test fun oneTimeNeverOccursBeforeAnchorOrAfterward() {
        val event = event("2026-10-22", RepeatRule.NONE)
        assertEquals(date("2026-10-22"), Recurrence.nextOccurrence(event, date("2026-10-01")))
        assertTrue(Recurrence.occursOn(event, date("2026-10-22")))
        assertNull(Recurrence.nextOccurrence(event, date("2026-10-23")))
    }

    @Test fun weeklyUsesAnchorWeekdayAndIsInclusive() {
        val event = event("2026-10-22", RepeatRule.WEEKLY)
        assertEquals(date("2026-10-29"), Recurrence.nextOccurrence(event, date("2026-10-23")))
        assertTrue(Recurrence.occursOn(event, date("2026-10-29")))
        assertFalse(Recurrence.occursOn(event, date("2026-10-28")))
    }

    @Test fun monthEndPolicyDoesNotDriftFollowingMonths() {
        val event = event("2026-01-31", RepeatRule.MONTHLY)
        assertEquals(date("2026-02-28"), Recurrence.nextOccurrence(event, date("2026-02-01")))
        assertEquals(date("2026-03-31"), Recurrence.nextOccurrence(event, date("2026-03-01")))
        assertEquals(date("2026-03-31"), Recurrence.nextOccurrence(event.copy(missingDayPolicy = MissingDayPolicy.SKIP), date("2026-02-01")))
    }

    @Test fun leapDayBirthdayUsesExplicitMissingDayPolicy() {
        val event = event("2024-02-29", RepeatRule.YEARLY)
        assertEquals(date("2025-02-28"), Recurrence.nextOccurrence(event, date("2025-01-01")))
        assertEquals(date("2028-02-29"), Recurrence.nextOccurrence(event.copy(missingDayPolicy = MissingDayPolicy.SKIP), date("2025-01-01")))
    }

    @Test fun lunarBirthdayTracksLunarNewYearAcrossSolarYears() {
        val event = event("2026-02-17", RepeatRule.LUNAR_YEARLY)
        assertEquals(date("2027-02-06"), Recurrence.nextOccurrence(event, date("2026-02-18")))
        assertFalse(Recurrence.occursOn(event, date("2027-02-17")))
    }

    @Test fun lunarDayThirtyFallsBackToTwentyNineOnlyWhenRequested() {
        val event = event("2024-02-09", RepeatRule.LUNAR_YEARLY)
        assertEquals(date("2025-01-28"), Recurrence.nextOccurrence(event, date("2024-02-10")))
        assertFalse(Recurrence.occursOn(event.copy(missingDayPolicy = MissingDayPolicy.SKIP), date("2025-01-28")))
    }

    @Test fun leapMonthPoliciesCanIncludeOrExcludeRepeatedLunarMonth() {
        val event = event("2023-02-20", RepeatRule.LUNAR_YEARLY)
        assertEquals(date("2024-03-10"), Recurrence.nextOccurrence(event, date("2023-02-21")))
        assertEquals(date("2023-03-22"), Recurrence.nextOccurrence(event.copy(leapMonthPolicy = LeapMonthPolicy.BOTH), date("2023-02-21")))
        assertEquals(date("2023-03-22"), Recurrence.nextOccurrence(event.copy(leapMonthPolicy = LeapMonthPolicy.LEAP_ONLY), date("2023-02-21")))
    }

    @Test fun explicitLunarAnchorIsHonoredAndSupportedRangeIsBounded() {
        val event = event("2026-01-01", RepeatRule.LUNAR_YEARLY).copy(lunarDay = 1, lunarMonth = 1)
        assertEquals(date("2026-02-17"), Recurrence.nextOccurrence(event, date("2026-01-01")))
        assertNull(Recurrence.nextOccurrence(event, date("2101-01-01")))
        assertNull(Recurrence.nextOccurrence(event.copy(solarDate = "invalid"), date("2026-01-01")))
        assertNull(Recurrence.nextOccurrence(event.copy(lunarDay = 31), date("2026-01-01")))
    }
}
