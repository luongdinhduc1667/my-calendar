package vn.lichviet.app.reminder

import vn.lichviet.app.data.EventEntity
import vn.lichviet.app.data.Recurrence
import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate
import java.time.ZonedDateTime

data class ReminderPlan(val occurrenceDate: LocalDate, val trigger: ZonedDateTime)

/** Event wall-clock times and lead days follow Vietnam (UTC+7), including when travelling. */
object ReminderPlanner {
    fun next(event: EventEntity, now: ZonedDateTime, afterOccurrence: LocalDate? = null): ReminderPlan? {
        if (!event.reminderEnabled) return null
        val vietnamNow = now.withZoneSameInstant(VietnameseCalendar.zoneId)
        var from = vietnamNow.toLocalDate().plusDays(event.reminderDaysBefore.toLong())
        if (afterOccurrence != null) from = maxOf(from, afterOccurrence.plusDays(1))
        var occurrence = Recurrence.nextOccurrence(event, from) ?: return null
        var trigger = occurrence.atTime(event.hour, event.minute).atZone(VietnameseCalendar.zoneId)
            .minusDays(event.reminderDaysBefore.toLong())
        if (!trigger.isAfter(vietnamNow)) {
            occurrence = Recurrence.nextOccurrence(event, occurrence.plusDays(1)) ?: return null
            trigger = occurrence.atTime(event.hour, event.minute).atZone(VietnameseCalendar.zoneId)
                .minusDays(event.reminderDaysBefore.toLong())
        }
        return ReminderPlan(occurrence, trigger)
    }
}
