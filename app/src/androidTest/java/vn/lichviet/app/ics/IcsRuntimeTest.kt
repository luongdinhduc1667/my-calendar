package vn.lichviet.app.ics

import android.content.Context
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import vn.lichviet.app.data.AppDatabase
import vn.lichviet.app.data.EventRepository

/** iCal4j uses service loading and timezone resources; exercise its actual Android runtime. */
@RunWith(AndroidJUnit4::class)
class IcsRuntimeTest {
    @Test fun incompatibleEmbeddedTimezoneIsRejectedInsteadOfSilentlyUsingPlatformRules() {
        val text = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Timezone test//EN
            BEGIN:VTIMEZONE
            TZID:Etc/UTC
            BEGIN:STANDARD
            DTSTART:19700101T000000
            TZOFFSETFROM:+0200
            TZOFFSETTO:+0200
            END:STANDARD
            END:VTIMEZONE
            BEGIN:VEVENT
            UID:timezone-conflict@example.test
            DTSTAMP:20260101T000000Z
            DTSTART;TZID=Etc/UTC:20261101T090000
            SUMMARY:Must not become 09:00 UTC
            END:VEVENT
            END:VCALENDAR
        """.trimIndent().replace("\n", "\r\n") + "\r\n"
        val preview = IcsCodec.preview(text)
        assertTrue(preview.fileErrors.isNotEmpty() || preview.items.any { !it.canImport })
    }

    private fun calendar(uid: String, summary: String) = """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//LichViet Android Runtime Test//EN
        BEGIN:VEVENT
        UID:$uid
        DTSTAMP:20260101T000000Z
        DTSTART;TZID=America/New_York:20260307T090000
        DTEND;TZID=America/New_York:20260307T100000
        RRULE:FREQ=DAILY;COUNT=4
        EXDATE;TZID=America/New_York:20260308T090000
        RDATE;TZID=America/New_York:20260312T090000
        SUMMARY:$summary
        DESCRIPTION:Tiếng Việt\, Hà Nội\nDòng thứ hai
        LOCATION:Hà Nội
        END:VEVENT
        BEGIN:VEVENT
        UID:$uid
        DTSTAMP:20260101T000000Z
        RECURRENCE-ID;TZID=America/New_York:20260309T090000
        DTSTART;TZID=America/New_York:20260310T110000
        DTEND;TZID=America/New_York:20260310T120000
        SUMMARY:Moved
        END:VEVENT
        END:VCALENDAR
    """.trimIndent().replace("\n", "\r\n") + "\r\n"

    @Test fun timezoneRecurrenceAndUtf8ExpandOnAndroid() {
        val preview = IcsCodec.preview(calendar("runtime@example.test", "Hẹn tiếng Việt"))
        val item = preview.items.single()
        assertTrue(item.errors.toString(), item.canImport)
        assertEquals("Tiếng Việt, Hà Nội\nDòng thứ hai", item.draft.note)
        val instances = EventOccurrenceEngine.between(listOf(item.draft), LocalDate.of(2026, 3, 7), LocalDate.of(2026, 3, 14))
        assertEquals(4, instances.size)
        assertEquals(Instant.parse("2026-03-07T14:00:00Z"), instances.first().start)
        assertEquals(Instant.parse("2026-03-12T13:00:00Z"), instances.last().start)
        assertTrue(instances.any { it.title == "Moved" && it.start == Instant.parse("2026-03-10T15:00:00Z") })
        val roundTrip = IcsCodec.preview(IcsCodec.export(listOf(item.draft))).items.single()
        assertTrue(roundTrip.errors.toString(), roundTrip.canImport)
    }

    @Test fun safImportSupportsUidUpdateSkipAndAtomicRollback() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = IcsRepository(context)
        val database = AppDatabase.get(context)
        val uid = "android-${UUID.randomUUID()}@example.test"
        val directory = File(context.cacheDir, "ics").apply { mkdirs() }
        // Retained for root's runtime smoke using the actual minified release APK.
        File(directory, "release-smoke.ics").writeText(calendar("release-smoke@example.test", "Release ICS timezone check"), Charsets.UTF_8)
        val file = File(directory, "test-${UUID.randomUUID()}.ics")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val secondUid = "atomic-${UUID.randomUUID()}@example.test"
        try {
            file.writeText(calendar(uid, "First"), Charsets.UTF_8)
            val first = repository.preview(uri)
            assertEquals(1, repository.import(first, mapOf(uid to IcsImportChoice.IMPORT)).imported)
            val original = database.eventDao().getByUid(uid)!!
            val skipped = repository.preview(uri)
            assertEquals(original.id, skipped.items.single().duplicateEventId)
            assertEquals(1, repository.import(skipped, mapOf(uid to IcsImportChoice.SKIP)).skipped)
            file.writeText(calendar(uid, "Updated"), Charsets.UTF_8)
            assertEquals(1, repository.import(repository.preview(uri), mapOf(uid to IcsImportChoice.UPDATE)).updated)
            val updated = database.eventDao().getByUid(uid)!!
            assertEquals(original.id, updated.id)
            assertEquals("Updated", updated.title)
            val valid = IcsCodec.preview(calendar(secondUid, "Must roll back")).items.single()
            val invalid = valid.copy(uid = "bad", errors = listOf("invalid"))
            var rejected = false
            try { repository.import(IcsImportPreview(listOf(valid, invalid)), mapOf(secondUid to IcsImportChoice.IMPORT, "bad" to IcsImportChoice.IMPORT)) }
            catch (_: IllegalArgumentException) { rejected = true }
            assertTrue(rejected)
            assertNull(database.eventDao().getByUid(secondUid))
        } finally {
            for (key in listOf(uid, secondUid)) database.eventDao().getByUid(key)?.let { EventRepository(context).delete(it.id) }
            file.delete()
        }
    }
}
