package vn.lichviet.app.ics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import vn.lichviet.app.data.EventEntity
import vn.lichviet.app.data.RepeatRule
import java.time.Instant
import java.time.LocalDate

class IcsCodecTest {
    private val recurring = """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//Test//Calendar//EN
        BEGIN:VEVENT
        UID:series@example.test
        DTSTAMP:20260101T000000Z
        DTSTART;TZID=America/New_York:20260307T090000
        DTEND;TZID=America/New_York:20260307T100000
        RRULE:FREQ=DAILY;COUNT=4
        EXDATE;TZID=America/New_York:20260308T090000
        RDATE;TZID=America/New_York:20260312T090000
        SUMMARY:Morning appointment
        X-MY-UNKNOWN:keep-me
        BEGIN:VALARM
        ACTION:DISPLAY
        DESCRIPTION:Reminder
        TRIGGER:-PT30M
        END:VALARM
        END:VEVENT
        BEGIN:VEVENT
        UID:series@example.test
        DTSTAMP:20260101T000000Z
        RECURRENCE-ID;TZID=America/New_York:20260309T090000
        DTSTART;TZID=America/New_York:20260310T110000
        DTEND;TZID=America/New_York:20260310T120000
        SUMMARY:Moved appointment
        END:VEVENT
        BEGIN:VEVENT
        UID:series@example.test
        DTSTAMP:20260101T000000Z
        RECURRENCE-ID;TZID=America/New_York:20260310T090000
        DTSTART;TZID=America/New_York:20260310T090000
        STATUS:CANCELLED
        SUMMARY:Cancelled appointment
        END:VEVENT
        END:VCALENDAR
    """.trimIndent().replace("\n", "\r\n") + "\r\n"

    @Test fun recurringSeriesAppliesRdateExdateMovedAndCancelled() {
        val preview = IcsCodec.preview(recurring)
        assertTrue(preview.fileErrors.toString(), preview.fileErrors.isEmpty())
        val item = preview.items.single()
        assertTrue(item.errors.toString(), item.canImport)
        assertEquals(listOf(30), item.displayAlarmOffsetsMinutes)
        assertTrue(item.canonicalCalendar.contains("X-MY-UNKNOWN:keep-me"))
        val event = item.draft.copy(id = 10)
        val actual = EventOccurrenceEngine.between(listOf(event), LocalDate.of(2026, 3, 7), LocalDate.of(2026, 3, 14))
        assertEquals(3, actual.size)
        assertEquals(Instant.parse("2026-03-07T14:00:00Z"), actual[0].start)
        assertEquals("Moved appointment", actual[1].title)
        assertEquals(Instant.parse("2026-03-10T15:00:00Z"), actual[1].start)
        assertEquals(Instant.parse("2026-03-12T13:00:00Z"), actual[2].start)
        assertEquals(3, actual.map { it.occurrenceKey }.distinct().size)
    }

    @Test fun importedCalendarRoundTripsUnknownPropertiesAndOverrides() {
        val event = IcsCodec.preview(recurring).items.single().draft.copy(id = 5)
        val exported = IcsCodec.export(listOf(event))
        assertTrue(exported.contains("X-MY-UNKNOWN:keep-me"))
        assertTrue(exported.contains("RECURRENCE-ID"))
        assertTrue(exported.contains("VALARM"))
        assertEquals(3, EventOccurrenceEngine.between(
            listOf(IcsCodec.preview(exported).items.single().draft),
            LocalDate.of(2026, 3, 7), LocalDate.of(2026, 3, 14),
        ).size)
    }

    @Test fun allDayDtendIsExclusiveAcrossSeveralDays() {
        val text = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Calendar//EN
            BEGIN:VEVENT
            UID:multi@example.test
            DTSTAMP:20260101T000000Z
            DTSTART;VALUE=DATE:20260401
            DTEND;VALUE=DATE:20260404
            SUMMARY:Three days
            END:VEVENT
            END:VCALENDAR
        """.trimIndent().replace("\n", "\r\n") + "\r\n"
        val item = IcsCodec.preview(text).items.single()
        assertTrue(item.errors.toString(), item.canImport)
        val event = item.draft
        assertEquals("2026-04-04", event.endSolarDate)
        assertEquals(1, EventOccurrenceEngine.between(listOf(event), LocalDate.of(2026, 4, 3), LocalDate.of(2026, 4, 4)).size)
        assertTrue(EventOccurrenceEngine.between(listOf(event), LocalDate.of(2026, 4, 4), LocalDate.of(2026, 4, 5)).isEmpty())
    }

    @Test fun ownLunarExtensionRestoresNativeRuleAndAlarm() {
        val event = EventEntity(id = 11, title = "Ngày giỗ", solarDate = "2026-03-17",
            repeat = RepeatRule.LUNAR_YEARLY, lunarDay = 29, lunarMonth = 1,
            reminderEnabled = true, icsUid = "lunar@example.test")
        val exported = IcsCodec.export(listOf(event), reminderOffsetsById = mapOf(11L to listOf(30, 1440)))
        assertTrue(exported.contains("X-LICHVIET-REPEAT:LUNAR_YEARLY"))
        assertTrue(exported.contains("RDATE"))
        val imported = IcsCodec.preview(exported).items.single()
        assertTrue(imported.errors.toString(), imported.canImport)
        assertEquals(RepeatRule.LUNAR_YEARLY, imported.draft.repeat)
        assertEquals(null, imported.draft.icsCalendar)
        assertEquals(listOf(30, 1440), imported.displayAlarmOffsetsMinutes)
    }

    @Test fun unsupportedRangeIsReportedWithoutClaimingImportSupport() {
        val text = recurring.replace(
            "RECURRENCE-ID;TZID=America/New_York:20260309T090000",
            "RECURRENCE-ID;RANGE=THISANDFUTURE;TZID=America/New_York:20260309T090000",
        )
        val item = IcsCodec.preview(text).items.single()
        assertFalse(item.canImport)
        assertTrue(item.errors.any { it.contains("RANGE") })
    }

    @Test fun dayBasedAlarmAndInvalidRowsAreValidatedDuringPreview() {
        val dayAlarm = IcsCodec.preview(recurring.replace("TRIGGER:-PT30M", "TRIGGER:-P1D")).items.single()
        assertTrue(dayAlarm.errors.toString(), dayAlarm.canImport)
        assertEquals(listOf(1440), dayAlarm.displayAlarmOffsetsMinutes)
        assertFalse(IcsCodec.preview(recurring.replace("SUMMARY:Morning appointment", "SUMMARY:" + "x".repeat(201))).items.single().canImport)
        assertFalse(IcsCodec.preview(recurring.replace("202603", "220003")).items.single().canImport)
    }

    @Test fun finiteExportHasNoOriginalOccurrenceOutsideSelectedRange() {
        val event = EventEntity(id = 12, title = "Birthday", solarDate = "1980-02-29", allDay = true,
            repeat = RepeatRule.YEARLY, reminderEnabled = false)
        val from = LocalDate.of(2026, 1, 1); val until = LocalDate.of(2030, 1, 1)
        val item = IcsCodec.preview(IcsCodec.export(listOf(event), from, until)).items.single()
        assertTrue(item.errors.toString(), item.canImport)
        assertEquals("1980-02-29", item.draft.solarDate)
        val external = item.draft.copy(icsCalendar = item.canonicalCalendar, repeat = RepeatRule.NONE)
        assertTrue(EventOccurrenceEngine.between(listOf(external), LocalDate.of(1980, 1, 1), LocalDate.of(1981, 1, 1)).isEmpty())
        assertEquals(LocalDate.of(2026, 2, 28), EventOccurrenceEngine.next(external, from.atStartOfDay(vn.lichviet.core.VietnameseCalendar.zoneId).toInstant())?.startDate)
        assertNull(EventOccurrenceEngine.next(external, until.atStartOfDay(vn.lichviet.core.VietnameseCalendar.zoneId).toInstant()))
    }

    @Test fun externallyChangedNativeRulesStayCanonicalInsteadOfBeingFlattened() {
        val event = EventEntity(id = 13, title = "Daily", solarDate = "2026-01-01", repeat = RepeatRule.DAILY, reminderEnabled = false)
        val exported = IcsCodec.export(listOf(event)).replace("RRULE:FREQ=DAILY", "RRULE:FREQ=DAILY;COUNT=2")
        val item = IcsCodec.preview(exported).items.single()
        assertTrue(item.errors.toString(), item.canImport)
        assertNotNull(item.draft.icsCalendar)
        assertEquals(RepeatRule.NONE, item.draft.repeat)
        assertEquals(2, EventOccurrenceEngine.between(listOf(item.draft), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 5)).size)
    }

    @Test fun nextHandlesTwoInstancesOnOneDayAndExhaustedCount() {
        val text = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VEVENT
            UID:twice@example.test
            DTSTAMP:20260101T000000Z
            DTSTART:20260101T020000Z
            RRULE:FREQ=DAILY;COUNT=2;BYHOUR=2,10
            SUMMARY:Twice
            END:VEVENT
            END:VCALENDAR
        """.trimIndent().replace("\n", "\r\n") + "\r\n"
        val item = IcsCodec.preview(text).items.single()
        assertTrue(item.errors.toString(), item.canImport)
        val next = EventOccurrenceEngine.next(item.draft, Instant.parse("2026-01-01T02:00:00Z"))!!
        assertEquals(Instant.parse("2026-01-01T10:00:00Z"), next.start)
        assertNull(EventOccurrenceEngine.next(item.draft, next.start))
    }

    @Test fun nominalDayDurationTracksDstForEachOccurrence() {
        val text = recurring.substringBefore("BEGIN:VEVENT\r\nUID:series@example.test\r\nDTSTAMP:20260101T000000Z\r\nRECURRENCE-ID") + "END:VCALENDAR\r\n"
        val nominal = text.replace("DTEND;TZID=America/New_York:20260307T100000", "DURATION:P1D")
        val item = IcsCodec.preview(nominal).items.single()
        assertTrue(item.errors.toString(), item.canImport)
        val items = EventOccurrenceEngine.between(listOf(item.draft), LocalDate.of(2026, 3, 7), LocalDate.of(2026, 3, 14))
        assertEquals(Instant.parse("2026-03-08T13:00:00Z"), items.first().endExclusive)
        val march9 = items.first { it.start == Instant.parse("2026-03-09T13:00:00Z") }
        assertEquals(Instant.parse("2026-03-10T13:00:00Z"), march9.endExclusive)
    }
}
