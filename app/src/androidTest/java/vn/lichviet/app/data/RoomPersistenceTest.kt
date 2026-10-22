package vn.lichviet.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** A real v1 on-disk database is closed and reopened to verify persistence and converters. */
@RunWith(AndroidJUnit4::class)
class RoomPersistenceTest {
    @Test fun createReadUpdateDeleteSurvivesDatabaseReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "persistence-${UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            val original = EventEntity(title = "Giỗ ông", note = "Chuẩn bị hương hoa", solarDate = "2026-10-22",
                hour = 8, minute = 15, repeat = RepeatRule.LUNAR_YEARLY, type = EventType.DEATH_ANNIVERSARY,
                reminderDaysBefore = 3, lunarDay = 12, lunarMonth = 9,
                leapMonthPolicy = LeapMonthPolicy.BOTH, missingDayPolicy = MissingDayPolicy.SKIP)
            val id = db.eventDao().save(original)
            assertNotEquals(0L, id)
            assertEquals(original.copy(id = id), db.eventDao().observeAll().first().single())
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            assertEquals(original.copy(id = id), db.eventDao().getById(id))
            val changed = original.copy(id = id, title = "Giỗ ông nội", reminderEnabled = false,
                repeat = RepeatRule.YEARLY, missingDayPolicy = MissingDayPolicy.LAST_DAY)
            assertEquals(id, db.eventDao().save(changed))
            assertEquals(changed, db.eventDao().getAll().single())
            db.eventDao().delete(id)
            assertNull(db.eventDao().getById(id))
            assertEquals(emptyList<EventEntity>(), db.eventDao().observeAll().first())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
