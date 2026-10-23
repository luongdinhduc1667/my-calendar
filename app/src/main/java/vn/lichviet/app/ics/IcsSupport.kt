package vn.lichviet.app.ics

import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.Period
import net.fortuna.ical4j.model.TimeZoneRegistry
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.DateProperty
import net.fortuna.ical4j.model.property.DtEnd
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.property.RecurrenceId
import vn.lichviet.core.VietnameseCalendar
import java.io.StringReader
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.Temporal
import java.time.temporal.TemporalAmount
import java.time.zone.ZoneRules
import net.fortuna.ical4j.model.ZoneRulesBuilder
import net.fortuna.ical4j.model.component.VTimeZone

internal data class ParsedIcsCalendar(val calendar: Calendar, val zones: TimeZoneRegistry)

internal object IcsSupport {
    val displayZone: ZoneId = VietnameseCalendar.zoneId

    fun parse(text: String): ParsedIcsCalendar {
        val builder = CalendarBuilder()
        val calendar = try { builder.build(StringReader(text)) }
        catch (error: Exception) { throw UnsupportedIcsException("Tệp ICS không hợp lệ: ${error.message}", error) }
        validateEmbeddedTimeZones(calendar)
        return ParsedIcsCalendar(calendar, builder.registry)
    }

    /** Android does not expose custom ZoneRulesProvider registration. Never silently
     * replace an embedded definition with different platform timezone rules. */
    private fun validateEmbeddedTimeZones(calendar: Calendar) {
        calendar.getComponents<VTimeZone>("VTIMEZONE").forEach { definition ->
            val id = definition.timeZoneId?.value
                ?: throw UnsupportedIcsException("VTIMEZONE thiếu TZID")
            val platform = runCatching { ZoneId.of(id, TimeZoneRegistry.ZONE_ALIASES).rules }.getOrNull()
                ?: throw UnsupportedIcsException("Custom VTIMEZONE chưa được hỗ trợ trên Android: $id")
            val embedded = runCatching { ZoneRulesBuilder().vTimeZone(definition).build() }.getOrElse {
                throw UnsupportedIcsException("VTIMEZONE không thể giải mã: $id", it)
            }
            if (!equivalentRules(embedded, platform)) throw UnsupportedIcsException(
                "VTIMEZONE khác quy tắc timezone của thiết bị trong 1900–2100: $id",
            )
        }
    }

    private fun equivalentRules(first: ZoneRules, second: ZoneRules): Boolean {
        var cursor = VietnameseCalendar.minDate.minusDays(2).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
        val end = VietnameseCalendar.maxDate.plusDays(2).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
        repeat(10_000) {
            if (first.getOffset(cursor) != second.getOffset(cursor)) return false
            val transition = listOfNotNull(first.nextTransition(cursor), second.nextTransition(cursor))
                .minByOrNull { it.instant } ?: return true
            if (transition.instant >= end) return true
            if (first.getOffset(transition.instant.minusNanos(1)) != second.getOffset(transition.instant.minusNanos(1))) return false
            cursor = transition.instant
        }
        return false
    }

    fun zone(property: net.fortuna.ical4j.model.Property, registry: TimeZoneRegistry): ZoneId {
        val tzid = property.getParameter<net.fortuna.ical4j.model.parameter.TzId>("TZID")
            .orElse(null)?.value ?: return displayZone
        val registered = runCatching { registry.getTimeZone(tzid) }.getOrNull()
        val zone = (if (registered != null) runCatching { registry.getZoneId(tzid) }.getOrNull() else null)
            ?: runCatching { ZoneId.of(tzid) }.getOrNull()
        return zone ?: throw UnsupportedIcsException("TZID không thể giải mã: $tzid")
    }

    fun instant(value: Temporal, property: net.fortuna.ical4j.model.Property, registry: TimeZoneRegistry): Instant =
        when (value) {
            is Instant -> value
            is ZonedDateTime -> value.toInstant()
            is OffsetDateTime -> value.toInstant()
            is LocalDateTime -> value.atZone(zone(property, registry)).toInstant()
            is LocalDate -> value.atStartOfDay(zone(property, registry)).toInstant()
            else -> throw UnsupportedIcsException("Kiểu ngày ICS chưa hỗ trợ: ${value.javaClass.simpleName}")
        }

    fun temporalAt(instant: Instant, template: Temporal, zone: ZoneId): Temporal = when (template) {
        is LocalDate -> instant.atZone(zone).toLocalDate()
        is LocalDateTime -> instant.atZone(zone).toLocalDateTime()
        is ZonedDateTime -> instant.atZone(template.zone)
        is OffsetDateTime -> instant.atOffset(template.offset)
        is Instant -> instant
        else -> throw UnsupportedIcsException("Kiểu lặp ICS chưa hỗ trợ: ${template.javaClass.simpleName}")
    }

    fun eventStart(event: VEvent): DtStart<Temporal> =
        event.getProperty<DtStart<Temporal>>("DTSTART").orElseThrow {
            UnsupportedIcsException("VEVENT thiếu DTSTART")
        }

    fun eventEnd(event: VEvent, start: Temporal): Temporal {
        val explicit = event.getProperty<DtEnd<Temporal>>("DTEND").orElse(null)
        if (explicit != null) return explicit.date
        val duration = event.getDuration()
        if (duration != null) return plus(start, duration.duration)
        return if (start is LocalDate) start.plusDays(1) else start
    }

    fun endInstant(event: VEvent, start: Temporal, registry: TimeZoneRegistry): Instant {
        val explicit = event.getProperty<DtEnd<Temporal>>("DTEND").orElse(null)
        return if (explicit != null) {
            zone(explicit, registry)
            instant(explicit.date, explicit, registry)
        }
            else instant(eventEnd(event, start), eventStart(event), registry)
    }

    fun plus(value: Temporal, amount: TemporalAmount): Temporal = try { value.plus(amount) }
        catch (error: Exception) { throw UnsupportedIcsException("DURATION không hỗ trợ: $amount", error) }

    fun occurrenceKey(value: Temporal, property: net.fortuna.ical4j.model.Property, zones: TimeZoneRegistry): String =
        if (value is LocalDate) "DATE:$value" else "INSTANT:${instant(value, property, zones).toEpochMilli()}"

    fun recurrenceId(event: VEvent): RecurrenceId<Temporal>? =
        event.getProperty<RecurrenceId<Temporal>>("RECURRENCE-ID").orElse(null)

    fun checkUnsupported(event: VEvent) {
        val recurrenceId = recurrenceId(event)
        val range = recurrenceId?.getParameter<net.fortuna.ical4j.model.Parameter>("RANGE")
            ?.orElse(null)?.value
        if (range != null) throw UnsupportedIcsException("RECURRENCE-ID;RANGE=$range chưa được hỗ trợ")
        if (event.getProperty<net.fortuna.ical4j.model.Property>("EXRULE").isPresent)
            throw UnsupportedIcsException("EXRULE chưa được hỗ trợ")
        event.getProperties<net.fortuna.ical4j.model.Property>("RRULE").forEach { property ->
            val value = property.value.uppercase()
            if (Regex("(?:^|;)FREQ=(SECONDLY|MINUTELY|HOURLY)(?:;|$)").containsMatchIn(value))
                throw UnsupportedIcsException("RRULE tần suất dưới một ngày chưa được hỗ trợ")
        }
    }

    fun temporalPeriod(start: Temporal, end: Temporal): Period<Temporal> = Period(start, end)

    fun checkedDuration(start: Instant, end: Instant): Duration {
        val duration = Duration.between(start, end)
        if (duration.isNegative || duration > Duration.ofDays(366))
            throw UnsupportedIcsException("Khoảng thời gian sự kiện không hợp lệ hoặc quá 366 ngày")
        return duration
    }
}
