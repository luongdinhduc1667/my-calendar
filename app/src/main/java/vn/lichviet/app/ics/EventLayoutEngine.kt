package vn.lichviet.app.ics

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class EventBarSegment(val occurrence: EventOccurrence, val lane: Int, val startColumn: Int, val span: Int,
    val begins: Boolean, val ends: Boolean)
data class EventWeekLayout(val segments: List<EventBarSegment>, val overflow: Map<LocalDate, Int>, val laneCount: Int)

/** Stable greedy interval packing. Bars are clipped at week edges; end dates are exclusive. */
object EventLayoutEngine {
    fun week(occurrences: List<EventOccurrence>, weekStart: LocalDate, visibleLanes: Int = 2): EventWeekLayout {
        require(visibleLanes in 1..8)
        val weekEnd = weekStart.plusDays(7)
        val occupiedUntil = mutableListOf<Int>()
        val segments = mutableListOf<EventBarSegment>()
        val overflow = mutableMapOf<LocalDate, Int>()
        occurrences.asSequence().filter { it.startDate < weekEnd && it.endDateExclusive > weekStart }
            .sortedWith(compareBy<EventOccurrence>({ maxOf(it.startDate, weekStart) }, { -ChronoUnit.DAYS.between(it.startDate, it.endDateExclusive) }, { it.eventId }, { it.occurrenceKey }))
            .forEach { occurrence ->
                val start = ChronoUnit.DAYS.between(weekStart, maxOf(weekStart, occurrence.startDate)).toInt()
                val end = ChronoUnit.DAYS.between(weekStart, minOf(weekEnd, occurrence.endDateExclusive)).toInt()
                val lane = occupiedUntil.indexOfFirst { it <= start }.let { if (it < 0) occupiedUntil.size else it }
                if (lane == occupiedUntil.size) occupiedUntil.add(end) else occupiedUntil[lane] = end
                if (lane < visibleLanes) segments += EventBarSegment(occurrence, lane, start, end - start,
                    occurrence.startDate >= weekStart, occurrence.endDateExclusive <= weekEnd)
                else for (column in start until end) {
                    val day = weekStart.plusDays(column.toLong())
                    overflow[day] = (overflow[day] ?: 0) + 1
                }
            }
        return EventWeekLayout(segments, overflow, minOf(occupiedUntil.size, visibleLanes))
    }
}
