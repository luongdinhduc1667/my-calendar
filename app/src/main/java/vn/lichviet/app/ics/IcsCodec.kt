package vn.lichviet.app.ics

import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.DateList
import net.fortuna.ical4j.model.component.VAlarm
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.component.VTimeZone
import net.fortuna.ical4j.model.parameter.Value
import net.fortuna.ical4j.model.property.Action
import net.fortuna.ical4j.model.property.CalScale
import net.fortuna.ical4j.model.property.Description
import net.fortuna.ical4j.model.property.DtStamp
import net.fortuna.ical4j.model.property.Location
import net.fortuna.ical4j.model.property.ProdId
import net.fortuna.ical4j.model.property.RDate
import net.fortuna.ical4j.model.property.RRule
import net.fortuna.ical4j.model.property.Summary
import net.fortuna.ical4j.model.property.Uid
import net.fortuna.ical4j.model.property.Version
import net.fortuna.ical4j.model.property.XProperty
import vn.lichviet.app.data.EventEntity
import vn.lichviet.app.data.EventType
import vn.lichviet.app.data.LeapMonthPolicy
import vn.lichviet.app.data.MissingDayPolicy
import vn.lichviet.app.data.Recurrence
import vn.lichviet.app.data.RepeatRule
import vn.lichviet.app.data.EventValidation
import vn.lichviet.core.VietnameseCalendar
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Loss-aware RFC 5545 import/export. Imported UID series retain their full canonical VCALENDAR. */
object IcsCodec {
    private const val MAX_CHARS = 4 * 1024 * 1024
    private const val MAX_EVENTS = 10_000
    private const val MAX_UIDS = 5_000
    private const val MAX_OVERRIDES = 500
    private const val X_ENGINE = "X-LICHVIET-ENGINE"
    private const val X_ANCHOR = "X-LICHVIET-ANCHOR"
    private const val X_FROM = "X-LICHVIET-FINITE-FROM"
    private const val X_UNTIL = "X-LICHVIET-FINITE-UNTIL"
    private const val X_COLOR = "X-LICHVIET-COLOR"
    private const val X_REPEAT = "X-LICHVIET-REPEAT"
    private const val X_TYPE = "X-LICHVIET-TYPE"
    private const val X_LUNAR_DAY = "X-LICHVIET-LUNAR-DAY"
    private const val X_LUNAR_MONTH = "X-LICHVIET-LUNAR-MONTH"
    private const val X_LEAP_POLICY = "X-LICHVIET-LEAP-POLICY"
    private const val X_MISSING_DAY = "X-LICHVIET-MISSING-DAY-POLICY"

    fun preview(text: String, existingUidToId: Map<String, Long> = emptyMap(), untitled: String = "(Untitled)"): IcsImportPreview {
        if (text.length > MAX_CHARS) return IcsImportPreview(emptyList(), listOf("ICS file is too large"), listOf(IcsDiagnostic(IcsIssueCode.FILE_TOO_LARGE)))
        val parsed = try { IcsSupport.parse(text) }
            catch (error: Exception) { return IcsImportPreview(emptyList(), listOf(error.message ?: "Invalid ICS file"), listOf(IcsDiagnostic(IcsIssueCode.PARSE_ERROR,error.message))) }
        val allEvents = parsed.calendar.getComponents<VEvent>("VEVENT")
        if (allEvents.size > MAX_EVENTS) return IcsImportPreview(emptyList(), listOf("Too many VEVENT components"), listOf(IcsDiagnostic(IcsIssueCode.EVENT_LIMIT)))
        if(allEvents.isEmpty()) return IcsImportPreview(emptyList(), listOf("No VEVENT components"), listOf(IcsDiagnostic(IcsIssueCode.NO_EVENTS)))
        val fileErrors = mutableListOf<String>()
        val fileDiagnostics = mutableListOf<IcsDiagnostic>()
        allEvents.forEachIndexed { index, event ->
            if (event.uid.orElse(null)?.value.isNullOrBlank()) { fileErrors += "VEVENT #${index + 1}: missing UID"; fileDiagnostics += IcsDiagnostic(IcsIssueCode.MISSING_UID,"VEVENT #${index+1}") }
        }
        val groups = allEvents.mapNotNull { event -> event.uid.orElse(null)?.value?.takeIf(String::isNotBlank)?.let { it to event } }
            .groupBy({ it.first }, { it.second })
        if (groups.size > MAX_UIDS) return IcsImportPreview(emptyList(), listOf("Too many UIDs"), listOf(IcsDiagnostic(IcsIssueCode.EVENT_LIMIT)))
        val items = groups.map { (uid, series) ->
            val canonical = singleUidCalendar(parsed.calendar, uid)
            val errors = mutableListOf<String>()
            val diagnostics = mutableListOf<IcsDiagnostic>()
            val warnings = mutableListOf<String>()
            val master = series.singleOrNull { IcsSupport.recurrenceId(it) == null }
            if (master == null) { errors += "UID requires exactly one master VEVENT"; diagnostics += IcsDiagnostic(IcsIssueCode.INVALID_MASTER) }
            if (series.size - 1 > MAX_OVERRIDES) { errors += "Too many recurrence overrides"; diagnostics += IcsDiagnostic(IcsIssueCode.OVERRIDE_LIMIT) }
            var draft = EventEntity(title = master?.summary?.value?.ifBlank { untitled } ?: untitled, solarDate = "1970-01-01", icsUid = uid)
            var offsets = emptyList<Int>()
            if (master != null && errors.isEmpty()) {
                try {
                    series.forEach(IcsSupport::checkUnsupported)
                    val startProperty = IcsSupport.eventStart(master)
                    IcsSupport.zone(startProperty, parsed.zones)
                    val startValue = startProperty.date
                    val start = IcsSupport.instant(startValue, startProperty, parsed.zones)
                    val end = IcsSupport.endInstant(master, startValue, parsed.zones)
                    IcsSupport.checkedDuration(start, end)
                    val localStart = start.atZone(IcsSupport.displayZone)
                    val localEnd = end.atZone(IcsSupport.displayZone)
                    val allDay = startValue is LocalDate
                    val note = master.description?.value.orEmpty()
                    val location = master.location?.value.orEmpty()
                    val alarmResult = supportedAlarms(master, series.filter { it !== master })
                    offsets = alarmResult.first
                    warnings += alarmResult.second
                    if(alarmResult.second.isNotEmpty()) diagnostics += IcsDiagnostic(IcsIssueCode.ALARM_PARTIAL,alarmResult.second.joinToString("; "))
                    val native = if (series.size == 1 && master.getProperties<net.fortuna.ical4j.model.Property>("EXDATE").isEmpty()) nativeExtension(master) else null
                    draft = EventEntity(
                        title = master.summary?.value?.ifBlank { untitled } ?: untitled,
                        note = note,
                        solarDate = (native?.anchor ?: localStart.toLocalDate()).toString(),
                        hour = if (allDay) 9 else localStart.hour,
                        minute = if (allDay) 0 else localStart.minute,
                        allDay = allDay,
                        endSolarDate = if (end == start) null else localEnd.toLocalDate().minusDays(java.time.temporal.ChronoUnit.DAYS.between(native?.anchor ?: localStart.toLocalDate(), localStart.toLocalDate())).toString(),
                        endHour = if (allDay || end == start) null else localEnd.hour,
                        endMinute = if (allDay || end == start) null else localEnd.minute,
                        location = location,
                        colorArgb = master.getProperty<net.fortuna.ical4j.model.Property>(X_COLOR).orElse(null)?.value?.let { color ->
                            require(color.matches(Regex("#[0-9A-Fa-f]{6}")))
                            (color.removePrefix("#").toLong(16) or 0xff000000L).toInt()
                        },
                        reminderEnabled = offsets.isNotEmpty(),
                        reminderDaysBefore = (offsets.firstOrNull { it % 1440 == 0 } ?: 0) / 1440,
                        icsUid = uid,
                        icsCalendar = if (native == null) canonical else null,
                        repeat = native?.repeat ?: RepeatRule.NONE,
                        type = native?.type ?: EventType.EVENT,
                        lunarDay = native?.lunarDay,
                        lunarMonth = native?.lunarMonth,
                        leapMonthPolicy = native?.leapPolicy ?: LeapMonthPolicy.REGULAR_ONLY,
                        missingDayPolicy = native?.missingDayPolicy ?: MissingDayPolicy.LAST_DAY,
                    )
                    if (native != null && nativeMatches(master, draft)) {
                        warnings += "Restored native recurrence from X-LICHVIET-*"
                        diagnostics += IcsDiagnostic(IcsIssueCode.NATIVE_RESTORED)
                    } else if (master.getProperty<net.fortuna.ical4j.model.Property>(X_REPEAT).isPresent) {
                        draft = draft.copy(solarDate = localStart.toLocalDate().toString(), endSolarDate = if (end == start) null else localEnd.toLocalDate().toString(), repeat = RepeatRule.NONE, icsCalendar = canonical, lunarDay = null, lunarMonth = null)
                        warnings += "Changed native recurrence metadata is preserved as ICS"
                        diagnostics += IcsDiagnostic(IcsIssueCode.NATIVE_IGNORED)
                    }
                    draft = EventValidation.validated(draft)
                    // Probe a bounded window so an invalid timezone or unsupported recurrence is
                    // shown before the user commits the import.
                    EventOccurrenceEngine.between(listOf(draft), localStart.toLocalDate().minusDays(1),
                        localStart.toLocalDate().plusDays(366))
                } catch (error: Exception) {
                    errors += error.message ?: "Cannot read event series"
                    diagnostics += IcsDiagnostic(if(error is UnsupportedIcsException) IcsIssueCode.UNSUPPORTED_FEATURE else IcsIssueCode.INVALID_DATA,error.message)
                }
            }
            IcsPreviewItem(uid, draft.title, canonical, draft, existingUidToId[uid], offsets, warnings, errors, diagnostics)
        }
        return IcsImportPreview(items, fileErrors, fileDiagnostics)
    }

    fun export(
        events: List<EventEntity>,
        startInclusive: LocalDate? = null,
        endExclusive: LocalDate? = null,
        reminderOffsetsById: Map<Long, List<Int>> = emptyMap(),
    ): String {
        require(events.size <= MAX_EVENTS) { "Quá nhiều sự kiện để xuất ICS" }
        if (startInclusive != null || endExclusive != null) require(startInclusive != null && endExclusive != null && endExclusive > startInclusive)
        val output = Calendar()
        output.propertyList = output.propertyList.add(ProdId("-//Lich Viet//Calendar 1.0//VI"))
        output.propertyList = output.propertyList.add(Version("2.0", "2.0"))
        output.propertyList = output.propertyList.add(CalScale(CalScale.VALUE_GREGORIAN))
        val timezoneDefinitions = mutableMapOf<String, String>()
        events.forEach { event ->
            val canonical = event.icsCalendar
            if (canonical != null) {
                val source = IcsSupport.parse(canonical).calendar
                source.getComponents<VTimeZone>("VTIMEZONE").forEach { timezone ->
                    val id = timezone.getProperty<net.fortuna.ical4j.model.property.TzId>("TZID").orElse(null)?.value
                    if (id != null) {
                        val definition = timezone.toString()
                        val previous = timezoneDefinitions[id]
                        require(previous == null || previous == definition) { "Conflicting VTIMEZONE definitions for TZID=$id" }
                        if (previous == null) { timezoneDefinitions[id] = definition; output.componentList = output.componentList.add(timezone.copy()) }
                    }
                }
                source.getComponents<VEvent>("VEVENT").forEach { original ->
                    val copy = original.copy()
                    if (IcsSupport.recurrenceId(copy) == null) {
                        copy.propertyList.removeIf { it.name == X_COLOR }
                        event.colorArgb?.let { copy.propertyList = copy.propertyList.add(XProperty(X_COLOR, "#%06X".format(it and 0xffffff))) }
                        reminderOffsetsById[event.id]?.let { offsets ->
                            copy.componentList.removeIf { it is VAlarm && supportedAlarmOffset(it) != null }
                            addAlarms(copy, event.title, if (event.reminderEnabled) offsets else emptyList())
                        }
                    }
                    output.componentList = output.componentList.add(copy)
                }
            } else {
                val offsets = reminderOffsetsById[event.id]
                    ?: if (event.reminderEnabled) listOf(event.reminderDaysBefore * 1440) else emptyList()
                nativeEvent(event, offsets, startInclusive, endExclusive)?.let { output.componentList = output.componentList.add(it) }
            }
        }
        return output.toString()
    }

    private fun singleUidCalendar(source: Calendar, uid: String): String {
        val copy = source.copy()
        copy.componentList.removeIf { component ->
            component !is VTimeZone && (component !is VEvent || component.uid.orElse(null)?.value != uid)
        }
        return copy.toString()
    }

    private fun supportedAlarmOffset(alarm:VAlarm):Int? {
        val trigger=alarm.trigger.orElse(null)?:return null
        val related=trigger.getParameter<net.fortuna.ical4j.model.Parameter>("RELATED").orElse(null)?.value
        if(alarm.action.orElse(null)?.value!=Action.VALUE_DISPLAY||trigger.isAbsolute||
            (related!=null&&!related.equals("START",true))||alarm.repeat.isPresent)return null
        val duration=runCatching { when(val amount=trigger.duration) {
            is java.time.Period->{require(amount.years==0&&amount.months==0);Duration.ofDays(amount.days.toLong())}
            else->Duration.from(amount)
        } }.getOrNull()?:return null
        if(duration>Duration.ZERO||duration.seconds%60L!=0L||-duration.toMinutes() !in 0..525_600)return null
        return -duration.toMinutes().toInt()
    }
    private fun supportedAlarms(master:VEvent,overrides:List<VEvent>):Pair<List<Int>,List<String>> {
        val warnings=mutableListOf<String>()
        if(overrides.any{it.alarms.isNotEmpty()})warnings+="Per-instance VALARMs are preserved but are not scheduled"
        val offsets=master.alarms.mapNotNull { alarm -> supportedAlarmOffset(alarm).also { if(it==null)warnings+="Unsupported VALARM preserved without scheduling" } }.distinct().sorted()
        return offsets to warnings.distinct()
    }

    private data class NativeExtension(
        val repeat: RepeatRule,
        val type: EventType,
        val lunarDay: Int?,
        val lunarMonth: Int?,
        val leapPolicy: LeapMonthPolicy,
        val missingDayPolicy: MissingDayPolicy,
        val anchor: LocalDate?,
    )

    private fun nativeExtension(master: VEvent): NativeExtension? {
        if(master.getProperty<net.fortuna.ical4j.model.Property>(X_ENGINE).orElse(null)?.value!="1")return null
        val rule = master.getProperty<net.fortuna.ical4j.model.Property>(X_REPEAT).orElse(null)?.value ?: return null
        val repeat = RepeatRule.entries.firstOrNull { it.name == rule } ?: throw UnsupportedIcsException("X-LICHVIET-REPEAT không hợp lệ")
        val typeValue = master.getProperty<net.fortuna.ical4j.model.Property>(X_TYPE).orElse(null)?.value
        val type = EventType.entries.firstOrNull { it.name == typeValue } ?: EventType.EVENT
        val lunarDay = master.getProperty<net.fortuna.ical4j.model.Property>(X_LUNAR_DAY).orElse(null)?.value?.toIntOrNull()
        val lunarMonth = master.getProperty<net.fortuna.ical4j.model.Property>(X_LUNAR_MONTH).orElse(null)?.value?.toIntOrNull()
        val leap = master.getProperty<net.fortuna.ical4j.model.Property>(X_LEAP_POLICY).orElse(null)?.value
            ?.let { text -> LeapMonthPolicy.entries.firstOrNull { it.name == text } }
            ?: LeapMonthPolicy.REGULAR_ONLY
        val missing = master.getProperty<net.fortuna.ical4j.model.Property>(X_MISSING_DAY).orElse(null)?.value
            ?.let { text -> MissingDayPolicy.entries.firstOrNull { it.name == text } }
            ?: MissingDayPolicy.LAST_DAY
        if (repeat == RepeatRule.LUNAR_YEARLY && (lunarDay !in 1..30 || lunarMonth !in 1..12))
            throw UnsupportedIcsException("Quy tắc âm lịch X-LICHVIET-* thiếu ngày hoặc tháng")
        return NativeExtension(repeat, type, lunarDay, lunarMonth, leap, missing,master.getProperty<net.fortuna.ical4j.model.Property>(X_ANCHOR).orElse(null)?.value?.let(LocalDate::parse))
    }

    private fun nativeEvent(event: EventEntity, offsets: List<Int>, from: LocalDate?, until: LocalDate?): VEvent? {
        val anchor=LocalDate.parse(event.solarDate)
        val finite=event.repeat==RepeatRule.LUNAR_YEARLY || (event.repeat in listOf(RepeatRule.MONTHLY,RepeatRule.YEARLY)&&event.missingDayPolicy==MissingDayPolicy.LAST_DAY)
        val finiteDates=if(finite)finiteDates(event,from,until) else emptyList()
        if(finite&&finiteDates.isEmpty())return null
        val startDate = if(finite)finiteDates.first() else anchor
        val shift=java.time.temporal.ChronoUnit.DAYS.between(anchor,startDate)
        val zone = IcsSupport.displayZone
        val start = if (event.allDay) startDate.atStartOfDay(zone).toInstant()
            else startDate.atTime(event.hour, event.minute).atZone(zone).toInstant()
        val endDate = event.endSolarDate?.let(LocalDate::parse)?.plusDays(shift)
        val end = when {
            endDate == null && event.allDay -> startDate.plusDays(1).atStartOfDay(zone).toInstant()
            endDate == null -> start
            event.allDay -> endDate.atStartOfDay(zone).toInstant()
            else -> endDate.atTime(event.endHour ?: event.hour, event.endMinute ?: event.minute)
                .atZone(zone).toInstant()
        }
        IcsSupport.checkedDuration(start, end)
        val item = if (event.allDay) VEvent(startDate, endDate ?: startDate.plusDays(1), event.title)
            else if (end > start) VEvent(start, end, event.title) else VEvent(start, event.title)
        item.propertyList = item.propertyList.add(Uid(event.icsUid ?: if (event.id > 0) "lichviet-event-${event.id}@lichviet.local" else UUID.randomUUID().toString()))
        item.propertyList = item.propertyList.add(DtStamp(Instant.now()))
        if (event.note.isNotEmpty()) item.propertyList = item.propertyList.add(Description(event.note))
        if (event.location.isNotEmpty()) item.propertyList = item.propertyList.add(Location(event.location))
        item.propertyList = item.propertyList.add(XProperty(X_ENGINE,"1"))
        item.propertyList = item.propertyList.add(XProperty(X_ANCHOR,event.solarDate))
        if(finite) {
            item.propertyList = item.propertyList.add(XProperty(X_FROM,(from?:VietnameseCalendar.minDate).toString()))
            item.propertyList = item.propertyList.add(XProperty(X_UNTIL,(until?:VietnameseCalendar.maxDate.plusDays(1)).toString()))
        }
        event.colorArgb?.let { item.propertyList = item.propertyList.add(XProperty(X_COLOR,"#%06X".format(it and 0xffffff))) }
        item.propertyList = item.propertyList.add(XProperty(X_REPEAT, event.repeat.name))
        item.propertyList = item.propertyList.add(XProperty(X_TYPE, event.type.name))
        item.propertyList = item.propertyList.add(XProperty(X_MISSING_DAY, event.missingDayPolicy.name))
        item.propertyList = item.propertyList.add(XProperty(X_LEAP_POLICY, event.leapMonthPolicy.name))
        if (event.lunarDay != null) item.propertyList = item.propertyList.add(XProperty(X_LUNAR_DAY, event.lunarDay.toString()))
        if (event.lunarMonth != null) item.propertyList = item.propertyList.add(XProperty(X_LUNAR_MONTH, event.lunarMonth.toString()))
        when (event.repeat) {
            RepeatRule.NONE -> Unit
            RepeatRule.DAILY -> item.propertyList = item.propertyList.add(RRule<Instant>("FREQ=DAILY"))
            RepeatRule.WEEKLY -> item.propertyList = item.propertyList.add(RRule<Instant>("FREQ=WEEKLY"))
            RepeatRule.MONTHLY -> if (event.missingDayPolicy == MissingDayPolicy.SKIP)
                item.propertyList = item.propertyList.add(RRule<Instant>("FREQ=MONTHLY")) else addFiniteRdates(item, event, finiteDates.drop(1))
            RepeatRule.YEARLY -> if (event.missingDayPolicy == MissingDayPolicy.SKIP)
                item.propertyList = item.propertyList.add(RRule<Instant>("FREQ=YEARLY")) else addFiniteRdates(item, event, finiteDates.drop(1))
            RepeatRule.LUNAR_YEARLY -> addFiniteRdates(item, event, finiteDates.drop(1))
        }
        addAlarms(item,event.title,offsets)
        return item
    }

    private fun addAlarms(item:VEvent,title:String,offsets:List<Int>) {
        offsets.distinct().filter{it in 0..525_600}.forEach { offset ->
            val alarm=VAlarm(Duration.ofMinutes(-offset.toLong()))
            alarm.propertyList = alarm.propertyList.add(Action(Action.VALUE_DISPLAY));alarm.propertyList = alarm.propertyList.add(Description(title));item.componentList = item.componentList.add(alarm)
        }
    }
    private fun finiteDates(event:EventEntity,from:LocalDate?,until:LocalDate?):List<LocalDate> {
        val first=maxOf(LocalDate.parse(event.solarDate),from?:VietnameseCalendar.minDate)
        val last=minOf(until?:VietnameseCalendar.maxDate.plusDays(1),VietnameseCalendar.maxDate.plusDays(1))
        val dates=mutableListOf<LocalDate>()
        var date=Recurrence.nextOccurrence(event,first)
        while(date!=null&&date<last) {
            dates+=date
            if(dates.size>3000)throw UnsupportedIcsException("More than 3000 finite occurrences")
            date=Recurrence.nextOccurrence(event,date.plusDays(1))
        }
        return dates
    }
    private fun addFiniteRdates(item:VEvent,event:EventEntity,dates:List<LocalDate>) {
        if(dates.isEmpty())return
        if(event.allDay)item.propertyList = item.propertyList.add(RDate(net.fortuna.ical4j.model.ParameterList().add(Value.DATE),DateList(dates)))
        else item.propertyList = item.propertyList.add(RDate(DateList(dates.map{it.atTime(event.hour,event.minute).atZone(IcsSupport.displayZone).toInstant()})))
    }
    private fun nativeMatches(master:VEvent,draft:EventEntity):Boolean = runCatching {
        val allowed = setOf("DTSTAMP", "UID", "SUMMARY", "DESCRIPTION", "LOCATION", "DTSTART", "DTEND", "DURATION", "RRULE", "RDATE", X_ENGINE, X_ANCHOR, X_FROM, X_UNTIL, X_COLOR, X_REPEAT, X_TYPE, X_LUNAR_DAY, X_LUNAR_MONTH, X_LEAP_POLICY, X_MISSING_DAY)
        if (master.propertyList.all.any { it.name !in allowed } || master.alarms.any { supportedAlarmOffset(it) == null }) return@runCatching false
        val from=master.getProperty<net.fortuna.ical4j.model.Property>(X_FROM).orElse(null)?.value?.let(LocalDate::parse)
        val until=master.getProperty<net.fortuna.ical4j.model.Property>(X_UNTIL).orElse(null)?.value?.let(LocalDate::parse)
        val expected=nativeEvent(draft,emptyList(),from,until)?:return@runCatching false
        fun properties(event:VEvent)=listOf("DTSTART","DTEND","DURATION","RRULE","RDATE","EXDATE","STATUS").flatMap { name -> event.getProperties<net.fortuna.ical4j.model.Property>(name).map { it.toString() } }.sorted()
        properties(master)==properties(expected)
    }.getOrDefault(false)
}
