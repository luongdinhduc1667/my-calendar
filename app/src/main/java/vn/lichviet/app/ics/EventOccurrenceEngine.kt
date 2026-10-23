package vn.lichviet.app.ics

import net.fortuna.ical4j.model.Period
import net.fortuna.ical4j.model.TimeZoneRegistry
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.DtStart
import vn.lichviet.app.data.EventEntity
import vn.lichviet.app.data.Recurrence
import vn.lichviet.core.VietnameseCalendar
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.Temporal

/** Expands native and RFC 5545 events into real instances in Vietnam's display zone. */
object EventOccurrenceEngine {
    private const val MAX_WINDOW_DAYS = 367L
    private const val MAX_OCCURRENCES = 20_000

    fun between(
        events: List<EventEntity>,
        startInclusive: LocalDate,
        endExclusive: LocalDate,
    ): List<EventOccurrence> {
        require(endExclusive > startInclusive && !endExclusive.isAfter(startInclusive.plusDays(MAX_WINDOW_DAYS))) {
            "Khoảng tra cứu phải nằm trong 367 ngày"
        }
        return events.flatMap { event ->
            if (event.icsCalendar != null) importedBetween(event, startInclusive, endExclusive)
            else nativeBetween(event, startInclusive, endExclusive)
        }.sortedWith(compareBy<EventOccurrence>({ it.start }, { it.eventId }, { it.occurrenceKey }))
    }

    fun next(event: EventEntity, after: Instant): EventOccurrence? {
        if (event.icsCalendar == null) {
            val anchor = LocalDate.parse(event.solarDate)
            var date = Recurrence.nextOccurrence(event, after.atZone(IcsSupport.displayZone).toLocalDate()) ?: return null
            var item = nativeOccurrence(event, anchor, date)
            if (item.start <= after) {
                date = Recurrence.nextOccurrence(event, date.plusDays(1)) ?: return null
                item = nativeOccurrence(event, anchor, date)
            }
            return item
        }
        val parsed = IcsSupport.parse(event.icsCalendar)
        val series = parsed.calendar.getComponents<VEvent>("VEVENT")
        val master = series.singleOrNull { IcsSupport.recurrenceId(it) == null }
            ?: throw UnsupportedIcsException("ICS series requires one master")
        val seed = IcsSupport.eventStart(master)
        val zone = IcsSupport.zone(seed, parsed.zones)
        val explicit = buildList {
            if (!isCancelled(master)) add(IcsSupport.instant(seed.date, seed, parsed.zones))
            master.getProperties<net.fortuna.ical4j.model.property.RDate<Temporal>>("RDATE").forEach { rdate ->
                rdate.dates.forEach { add(IcsSupport.instant(it, rdate, parsed.zones)) }
                rdate.periods.orElse(emptySet()).forEach { add(IcsSupport.instant(it.start, rdate, parsed.zones)) }
            }
            series.filter { it !== master && !isCancelled(it) }.forEach {
                val property = IcsSupport.eventStart(it)
                add(IcsSupport.instant(property.date, property, parsed.zones))
            }
        }
        val rules = if (isCancelled(master)) emptyList() else master.getProperties<net.fortuna.ical4j.model.property.RRule<Temporal>>("RRULE")
        var floor = after
        repeat(MAX_OCCURRENCES) {
            val candidates = explicit.filter { it > floor }.toMutableList()
            val temporalFloor = IcsSupport.temporalAt(floor, seed.date, zone)
            rules.forEach { rule -> rule.recur.getNextDate(seed.date, temporalFloor)?.let {
                val candidate = IcsSupport.instant(it, seed, parsed.zones)
                if (candidate > floor) candidates += candidate
            } }
            val candidate = candidates.minOrNull() ?: return null
            val date = candidate.atZone(IcsSupport.displayZone).toLocalDate()
            if (date > VietnameseCalendar.maxDate) return null
            val actual = importedBetween(event, date, date.plusDays(1), parsed).filter { it.start > after }.minByOrNull { it.start }
            if (actual != null) return actual
            floor = candidate
        }
        throw UnsupportedIcsException("Too many excluded recurrence instances")
    }

    private fun nativeBetween(event: EventEntity, from: LocalDate, until: LocalDate): List<EventOccurrence> {
        val anchor = runCatching { LocalDate.parse(event.solarDate) }.getOrNull() ?: return emptyList()
        val endDate = event.endSolarDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val spanDays = if (endDate == null) 1L else
            java.time.temporal.ChronoUnit.DAYS.between(anchor, endDate).coerceIn(0, 366) + 1
        var candidate = Recurrence.nextOccurrence(event, from.minusDays(spanDays))
        val result = ArrayList<EventOccurrence>()
        while (candidate != null && candidate < until) {
            val occurrence = nativeOccurrence(event, anchor, candidate)
            if (overlaps(occurrence, from, until)) result += occurrence
            if (result.size > MAX_OCCURRENCES) throw UnsupportedIcsException("Quá nhiều lần lặp trong khoảng tra cứu")
            candidate = Recurrence.nextOccurrence(event, candidate.plusDays(1))
        }
        return result
    }

    private fun nativeOccurrence(event: EventEntity, anchor: LocalDate, date: LocalDate): EventOccurrence {
        val zone = IcsSupport.displayZone
        val start = date.atTime(if (event.allDay) LocalTime.MIDNIGHT else LocalTime.of(event.hour, event.minute))
            .atZone(zone).toInstant()
        val anchorEndDate = event.endSolarDate?.let(LocalDate::parse)
        val end = when {
            anchorEndDate == null && event.allDay -> date.plusDays(1).atStartOfDay(zone).toInstant()
            anchorEndDate == null -> start
            event.allDay -> date.plusDays(java.time.temporal.ChronoUnit.DAYS.between(anchor, anchorEndDate))
                .atStartOfDay(zone).toInstant()
            else -> {
                val days = java.time.temporal.ChronoUnit.DAYS.between(anchor, anchorEndDate)
                date.plusDays(days).atTime(event.endHour ?: event.hour, event.endMinute ?: event.minute)
                    .atZone(zone).toInstant()
            }
        }
        IcsSupport.checkedDuration(start, end)
        return occurrence(event, date.toString(), start, end, event.allDay, event.title, event.note, event.location, event.colorArgb)
    }

    private fun importedBetween(event: EventEntity, from: LocalDate, until: LocalDate,
        parsed: ParsedIcsCalendar = IcsSupport.parse(event.icsCalendar ?: error("Missing ICS"))): List<EventOccurrence> {
        val events = parsed.calendar.getComponents<VEvent>("VEVENT")
        val master = events.singleOrNull { IcsSupport.recurrenceId(it) == null }
            ?: throw UnsupportedIcsException("Chuỗi ICS cần đúng một VEVENT gốc")
        events.forEach(IcsSupport::checkUnsupported)
        val masterStart = IcsSupport.eventStart(master)
        IcsSupport.zone(masterStart, parsed.zones)
        val startValue = masterStart.date
        val originalStart = IcsSupport.instant(startValue, masterStart, parsed.zones)
        val originalEnd = IcsSupport.endInstant(master, startValue, parsed.zones)
        val duration = IcsSupport.checkedDuration(originalStart, originalEnd)

        val overrides = events.filter { it !== master }.associateBy { override ->
            val id = IcsSupport.recurrenceId(override)!!
            IcsSupport.occurrenceKey(id.date, id, parsed.zones)
        }
        if (overrides.size != events.size - 1) throw UnsupportedIcsException("RECURRENCE-ID bị trùng")

        val startZone = IcsSupport.zone(masterStart, parsed.zones)
        val searchStart = from.minusDays(duration.toDays().coerceAtMost(366) + 3)
            .atStartOfDay(IcsSupport.displayZone).toInstant()
        val searchEnd = until.plusDays(3).atStartOfDay(IcsSupport.displayZone).toInstant()
        val query = IcsSupport.temporalPeriod(
            IcsSupport.temporalAt(searchStart, startValue, startZone),
            IcsSupport.temporalAt(searchEnd, startValue, startZone),
        )
        val periods: Set<Period<Temporal>> = try { master.calculateRecurrenceSet(query) }
            catch (error: Exception) { throw UnsupportedIcsException("Không tính được quy tắc lặp ICS: ${error.message}", error) }
        if (periods.size > MAX_OCCURRENCES) throw UnsupportedIcsException("Quá nhiều lần lặp trong khoảng tra cứu")

        val output = linkedMapOf<String, EventOccurrence>()
        periods.forEach { period ->
            val key = IcsSupport.occurrenceKey(period.start, masterStart, parsed.zones)
            val replacement = overrides[key]
            val item = if (replacement == null) {
                if (isCancelled(master)) null else {
                    val start = IcsSupport.instant(period.start, masterStart, parsed.zones)
                    val end = if (period.end == period.start) start.plus(duration)
                        else IcsSupport.instant(period.end, masterStart, parsed.zones)
                    IcsSupport.checkedDuration(start, end)
                    occurrence(event, key, start, end, startValue is LocalDate,
                        master.summary?.value ?: event.title,
                        master.description?.value ?: event.note,
                        master.location?.value ?: event.location, event.colorArgb)
                }
            } else overrideOccurrence(event, replacement, key, master, duration, parsed.zones)
            if (item != null && overlaps(item, from, until)) output[key] = item
        }
        // A moved exception can enter the window even when its original recurrence is outside it.
        overrides.forEach { (key, replacement) ->
            if (key !in output) {
                val item = overrideOccurrence(event, replacement, key, master, duration, parsed.zones)
                if (item != null && overlaps(item, from, until)) output[key] = item
            }
        }
        return output.values.toList()
    }

    private fun overrideOccurrence(
        event: EventEntity,
        override: VEvent,
        key: String,
        master: VEvent,
        masterDuration: Duration,
        zones: TimeZoneRegistry,
    ): EventOccurrence? {
        if (isCancelled(override)) return null
        val startProperty = override.getProperty<DtStart<Temporal>>("DTSTART").orElse(null)
            ?: throw UnsupportedIcsException("VEVENT ngoại lệ thiếu DTSTART")
        IcsSupport.zone(startProperty, zones)
        val startValue = startProperty.date
        val start = IcsSupport.instant(startValue, startProperty, zones)
        val end = if (override.getProperty<net.fortuna.ical4j.model.Property>("DTEND").isPresent ||
            override.getProperty<net.fortuna.ical4j.model.Property>("DURATION").isPresent)
            IcsSupport.endInstant(override, startValue, zones) else start.plus(masterDuration)
        IcsSupport.checkedDuration(start, end)
        return occurrence(event, key, start, end, startValue is LocalDate,
            override.summary?.value ?: master.summary?.value ?: event.title,
            override.description?.value ?: master.description?.value ?: event.note,
            override.location?.value ?: master.location?.value ?: event.location, event.colorArgb)
    }

    private fun isCancelled(event: VEvent): Boolean = event.status?.value.equals("CANCELLED", true)

    private fun occurrence(
        event: EventEntity,
        key: String,
        start: Instant,
        end: Instant,
        allDay: Boolean,
        title: String,
        note: String,
        location: String,
        color: Int?,
    ): EventOccurrence {
        val date = start.atZone(IcsSupport.displayZone).toLocalDate()
        val exclusive = when {
            end <= start -> date.plusDays(1)
            allDay -> end.atZone(IcsSupport.displayZone).toLocalDate()
            end.atZone(IcsSupport.displayZone).toLocalTime() == LocalTime.MIDNIGHT ->
                end.atZone(IcsSupport.displayZone).toLocalDate()
            else -> end.atZone(IcsSupport.displayZone).toLocalDate().plusDays(1)
        }
        return EventOccurrence(event, key, start, end, date, maxOf(date.plusDays(1), exclusive),
            title, note, location, allDay, color)
    }

    private fun overlaps(item: EventOccurrence, from: LocalDate, until: LocalDate): Boolean =
        item.startDate < until && item.endDateExclusive > from
}
