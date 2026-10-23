package vn.lichviet.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ReminderMigrationTest {
    @Test fun versionOneEventsGainStableReminderRowsWithoutLosingData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "reminder-migration-${UUID.randomUUID()}.db"
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open("vn.lichviet.app.data.AppDatabase/1.json")
            .bufferedReader().use { it.readText() }).getJSONObject("database")
        val createEvents = schema.getJSONArray("entities").getJSONObject(0).getString("createSql")
            .replace("$" + "{TABLE_NAME}", "events")
        context.getDatabasePath(name).parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).apply {
            execSQL(createEvents)
            val setup = schema.getJSONArray("setupQueries")
            for (index in 0 until setup.length()) execSQL(setup.getString(index))
            execSQL("""INSERT INTO events (id,title,note,solarDate,hour,minute,repeat,type,reminderDaysBefore,reminderEnabled,lunarDay,lunarMonth,leapMonthPolicy,missingDayPolicy)
                VALUES (11,'Giỗ bà','Ghi chú cũ','2026-10-22',8,30,'LUNAR_YEARLY','DEATH_ANNIVERSARY',3,1,12,9,'BOTH','LAST_DAY')""")
            execSQL("""INSERT INTO events (id,title,note,solarDate,hour,minute,repeat,type,reminderDaysBefore,reminderEnabled,lunarDay,lunarMonth,leapMonthPolicy,missingDayPolicy)
                VALUES (12,'Không nhắc','','2026-10-23',9,0,'NONE','EVENT',0,0,NULL,NULL,'REGULAR_ONLY','LAST_DAY')""")
            version = 1
            close()
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4).build()
        try {
            val event = db.eventDao().getById(11)!!
            assertEquals("Giỗ bà", event.title)
            assertEquals("Ghi chú cũ", event.note)
            assertEquals(false, event.allDay)
            assertNotNull(event.icsUid)
            val reminder = db.reminderDao().forEvent(11).single()
            assertEquals(3 * 1440, reminder.offsetMinutes)
            assertEquals(emptyList<EventReminderEntity>(), db.reminderDao().forEvent(12))
            db.eventDao().save(event.copy(title = "Giỗ bà ngoại"))
            assertEquals(reminder.id, db.reminderDao().forEvent(11).single().id)
            assertEquals("Giỗ bà ngoại", db.eventDao().getById(11)?.title)
            db.eventDao().delete(11)
            assertNull(db.reminderDao().getById(reminder.id))
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun versionTwoOccurrenceStateAndEventsMigrateWithoutLoss() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "ics-migration-${UUID.randomUUID()}.db"
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open("vn.lichviet.app.data.AppDatabase/2.json")
            .bufferedReader().use { it.readText() }).getJSONObject("database")
        context.getDatabasePath(name).parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).apply {
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                execSQL(entity.getString("createSql").replace("$" + "{TABLE_NAME}", entity.getString("tableName")))
                val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (item in 0 until indices.length())
                    execSQL(indices.getJSONObject(item).getString("createSql").replace("$" + "{TABLE_NAME}", entity.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (index in 0 until setup.length()) execSQL(setup.getString(index))
            execSQL("""INSERT INTO events (id,title,note,solarDate,hour,minute,allDay,repeat,type,reminderDaysBefore,reminderEnabled,lunarDay,lunarMonth,leapMonthPolicy,missingDayPolicy)
                VALUES (21,'Họp','Giữ nguyên','2026-10-25',10,15,0,'NONE','EVENT',0,1,NULL,NULL,'REGULAR_ONLY','LAST_DAY')""")
            execSQL("""INSERT INTO event_reminders (id,eventId,offsetMinutes,scheduledDate,scheduledTriggerMillis,lastDeliveredDate,lastDeliveredTriggerMillis,snoozeDate,snoozeUntilMillis,completedDate)
                VALUES (31,21,15,'2026-10-25',123456,'2026-10-24',123000,'2026-10-24',123111,'2026-10-23')""")
            version = 2
            close()
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4).build()
        try {
            val event = db.eventDao().getById(21)!!
            assertEquals("Họp", event.title)
            assertEquals("Giữ nguyên", event.note)
            assertEquals("", event.location)
            assertNotNull(event.icsUid)
            val row = db.reminderDao().getById(31)!!
            assertEquals("2026-10-25", row.scheduledOccurrenceKey)
            assertEquals("2026-10-24", row.lastDeliveredOccurrenceKey)
            assertEquals("2026-10-24", row.snoozeOccurrenceKey)
            assertEquals("2026-10-23", row.completedOccurrenceKey)
            assertEquals(123456L, row.scheduledTriggerMillis)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
