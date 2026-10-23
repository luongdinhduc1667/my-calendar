package vn.lichviet.app.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import vn.lichviet.app.data.EventEntity
import vn.lichviet.app.data.RepeatRule
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderPlannerTest {
    private val event = EventEntity(title = "Giỗ", solarDate = "2026-02-17", repeat = RepeatRule.LUNAR_YEARLY,
        hour = 9, minute = 0, reminderDaysBefore = 7)

    @Test fun lunarAnniversaryLeadDaysUseSolarDaysBeforeOccurrence() {
        val plan = ReminderPlanner.next(event, ZonedDateTime.parse("2026-10-22T12:00:00+07:00"))!!
        assertEquals(LocalDate.parse("2027-02-06"), plan.occurrenceDate)
        assertEquals(ZonedDateTime.of(2027, 1, 30, 9, 0, 0, 0, ZoneId.of("Asia/Ho_Chi_Minh")), plan.trigger)
    }

    @Test fun passedReminderAdvancesToFollowingOccurrence() {
        val plan = ReminderPlanner.next(event, ZonedDateTime.parse("2027-01-30T09:00:00+07:00"))!!
        assertEquals(2028, plan.occurrenceDate.year)
    }

    @Test fun travelDeviceTimezoneNeverChangesVietnamReminderTime() {
        val deviceNow = ZonedDateTime.parse("2027-01-29T17:00:00-08:00[America/Los_Angeles]")
        val plan = ReminderPlanner.next(event, deviceNow)!!
        assertEquals(LocalDate.parse("2027-02-06"), plan.occurrenceDate)
        assertEquals("2027-01-30T02:00:00Z", plan.trigger.toInstant().toString())
    }

    @Test fun zeroLeadOneTimeReminderDoesNotReschedulePastEvent() {
        val oneTime = event.copy(solarDate = "2026-10-22", repeat = RepeatRule.NONE, reminderDaysBefore = 0)
        assertEquals(LocalDate.parse("2026-10-22"), ReminderPlanner.next(oneTime, ZonedDateTime.parse("2026-10-22T08:59:00+07:00"))!!.occurrenceDate)
        assertNull(ReminderPlanner.next(oneTime, ZonedDateTime.parse("2026-10-22T09:00:00+07:00")))
    }

    @Test fun handledOccurrenceIsNeverQueuedTwiceEvenIfWorkerRunsEarly() {
        val daily = event.copy(solarDate = "2026-10-22", repeat = RepeatRule.DAILY, reminderDaysBefore = 0)
        val plan = ReminderPlanner.next(daily, ZonedDateTime.parse("2026-10-22T08:59:00+07:00"), LocalDate.parse("2026-10-22"))!!
        assertEquals(LocalDate.parse("2026-10-23"), plan.occurrenceDate)
    }

    @Test fun multipleMinuteAndDayOffsetsHaveDistinctInstantInVietnam() {
        val oneTime = EventEntity(title = "Hẹn", solarDate = "2026-10-23", hour = 9, minute = 0)
        val now = ZonedDateTime.parse("2026-10-22T08:00:00+07:00")
        assertEquals("2026-10-22T09:00+07:00[Asia/Ho_Chi_Minh]", ReminderPlanner.next(oneTime, 1440, now)?.trigger.toString())
        assertEquals("2026-10-23T08:55+07:00[Asia/Ho_Chi_Minh]", ReminderPlanner.next(oneTime, 5, now)?.trigger.toString())
    }

    @Test fun allDayEventStartsAtNineInVietnamRegardlessOfStoredClockTime() {
        val allDay = EventEntity(title = "Đi chơi", solarDate = "2026-10-23", hour = 18, minute = 30, allDay = true)
        assertEquals("2026-10-23T08:45+07:00[Asia/Ho_Chi_Minh]",
            ReminderPlanner.next(allDay, 15, ZonedDateTime.parse("2026-10-22T12:00:00+07:00"))?.trigger.toString())
    }
}
