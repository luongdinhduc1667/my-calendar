package vn.lichviet.app.reminder

import vn.lichviet.app.data.EventEntity
import vn.lichviet.app.data.Recurrence
import vn.lichviet.app.ics.EventOccurrence
import vn.lichviet.app.ics.EventOccurrenceEngine
import vn.lichviet.core.VietnameseCalendar
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

data class ReminderPlan(
    val occurrenceDate: LocalDate,
    val trigger: ZonedDateTime,
    val occurrenceKey: String = occurrenceDate.toString(),
    val occurrence: EventOccurrence? = null,
)

/** Event wall-clock times and lead days follow Vietnam (UTC+7), including when travelling. */
object ReminderPlanner {
    /** Reminder row state uses a stable instance key, not only a calendar date. */
    fun nextForRow(
        event: EventEntity, offsetMinutes: Int, now: ZonedDateTime,
        lastDeliveredKey: String?, lastDeliveredTriggerMillis: Long?,
    ): ReminderPlan? {
        if (event.icsCalendar == null) {
            val lastDate = lastDeliveredKey?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            return next(event, offsetMinutes, now, lastDate)
        }
        if (!event.reminderEnabled) return null
        require(offsetMinutes in 0..525_600)
        val offset = java.time.Duration.ofMinutes(offsetMinutes.toLong())
        val nowInstant = now.toInstant()
        var floor = nowInstant.plus(offset)
        if (lastDeliveredTriggerMillis != null) {
            floor = maxOf(floor, Instant.ofEpochMilli(lastDeliveredTriggerMillis).plus(offset).plusMillis(1))
        }
        repeat(4) {
            val occurrence = EventOccurrenceEngine.next(event, floor.minusMillis(1)) ?: return null
            val triggerInstant = occurrence.start.minus(offset)
            if (triggerInstant.isAfter(nowInstant) && occurrence.occurrenceKey != lastDeliveredKey) {
                return ReminderPlan(occurrence.startDate,
                    triggerInstant.atZone(VietnameseCalendar.zoneId), occurrence.occurrenceKey, occurrence)
            }
            floor = occurrence.start.plusMillis(1)
        }
        return null
    }

    fun next(event: EventEntity, now: ZonedDateTime, afterOccurrence: LocalDate? = null): ReminderPlan? {
        return next(event, event.reminderDaysBefore * 1440, now, afterOccurrence)
    }

    fun next(event: EventEntity, offsetMinutes: Int, now: ZonedDateTime, afterOccurrence: LocalDate? = null): ReminderPlan? {
        if (!event.reminderEnabled) return null
        require(offsetMinutes in 0..525_600)
        val vietnamNow = now.withZoneSameInstant(VietnameseCalendar.zoneId)
        var from = vietnamNow.toLocalDate().plusDays(offsetMinutes.toLong() / 1440)
        if (afterOccurrence != null) from = maxOf(from, afterOccurrence.plusDays(1))
        var occurrence = Recurrence.nextOccurrence(event, from) ?: return null
        val hour = if (event.allDay) 9 else event.hour
        val minute = if (event.allDay) 0 else event.minute
        var trigger = occurrence.atTime(hour, minute).atZone(VietnameseCalendar.zoneId)
            .minusMinutes(offsetMinutes.toLong())
        if (!trigger.isAfter(vietnamNow)) {
            occurrence = Recurrence.nextOccurrence(event, occurrence.plusDays(1)) ?: return null
            trigger = occurrence.atTime(hour, minute).atZone(VietnameseCalendar.zoneId)
                .minusMinutes(offsetMinutes.toLong())
        }
        return ReminderPlan(occurrence, trigger)
    }
}
