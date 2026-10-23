package vn.lichviet.app.ics

import org.junit.Assert.*
import org.junit.Test
import vn.lichviet.app.data.EventEntity
import java.time.LocalDate

class EventLayoutEngineTest {
    @Test fun barsContinueAcrossWeekAndMonthBoundariesWithExclusiveEnd() {
        val event = EventEntity(id = 1, title = "Trip", solarDate = "2026-10-30", allDay = true, endSolarDate = "2026-11-05")
        val occurrences = EventOccurrenceEngine.between(listOf(event), LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 9))
        val first = EventLayoutEngine.week(occurrences, LocalDate.of(2026, 10, 26)).segments.single()
        assertEquals(4, first.startColumn); assertEquals(3, first.span); assertTrue(first.begins); assertFalse(first.ends)
        val next = EventLayoutEngine.week(occurrences, LocalDate.of(2026, 11, 2)).segments.single()
        assertEquals(0, next.startColumn); assertEquals(3, next.span); assertFalse(next.begins); assertTrue(next.ends)
        assertTrue(EventOccurrenceEngine.between(listOf(event), LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 6)).isEmpty())
    }
    @Test fun overlappingBarsUseLanesAndCountOverflowPerDay() {
        val date = LocalDate.of(2026, 10, 26)
        val events = (1L..4L).map { EventEntity(id = it, title = "Event $it", solarDate = date.toString(), allDay = true, endSolarDate = date.plusDays(it).toString()) }
        val occurrences = EventOccurrenceEngine.between(events, date, date.plusDays(7))
        val layout = EventLayoutEngine.week(occurrences, date)
        assertEquals(2, layout.segments.size)
        assertEquals(2, layout.overflow[date])
        assertEquals(1, layout.overflow[date.plusDays(1)])
        assertNull(layout.overflow[date.plusDays(2)])
    }
}
