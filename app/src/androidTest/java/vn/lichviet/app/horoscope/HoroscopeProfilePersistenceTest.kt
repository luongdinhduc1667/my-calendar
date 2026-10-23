package vn.lichviet.app.horoscope

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import vn.lichviet.app.data.AppDatabase

@RunWith(AndroidJUnit4::class)
class HoroscopeProfilePersistenceTest {
    @Test fun multipleProfilesSurviveReopenEditAndDeleteIndependently() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "horoscope-profiles-${UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            val me = HoroscopeProfileEntity(label = "Biệt danh", year = 1984, month = 2, day = 2, hour = 0, minute = 30)
            val friend = HoroscopeProfileEntity(label = "Bạn", group = "FRIEND", dateKind = "LUNAR", year = 2023, month = 2, day = 1, isLeap = true, traditionalSex = "FEMALE", civilDayRule = "EARLY_ZI_NEXT_DAY")
            val first = db.horoscopeProfileDao().save(HoroscopeProfileValidation.validated(me))
            val second = db.horoscopeProfileDao().save(HoroscopeProfileValidation.validated(friend))
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            assertEquals(listOf(me.copy(id = first), friend.copy(id = second)), db.horoscopeProfileDao().getAll())
            val edit = me.copy(id = first, label = "Tên mới", hour = 23, civilDayRule = "EARLY_ZI_NEXT_DAY")
            assertEquals(first, db.horoscopeProfileDao().save(edit))
            assertEquals(edit, db.horoscopeProfileDao().getById(first))
            db.horoscopeProfileDao().delete(first)
            assertNull(db.horoscopeProfileDao().getById(first))
            assertEquals(friend.copy(id = second), db.horoscopeProfileDao().getAll().single())
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun migrationFromVersionThreeRetainsEventsAndCreatesAnEmptyProfileTable() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "horoscope-migration-${UUID.randomUUID()}.db"
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open("vn.lichviet.app.data.AppDatabase/3.json")
            .bufferedReader().use { it.readText() }).getJSONObject("database")
        context.getDatabasePath(name).parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).apply {
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                execSQL(entity.getString("createSql").replace("$" + "{TABLE_NAME}", entity.getString("tableName")))
                val indices = entity.getJSONArray("indices")
                for (item in 0 until indices.length()) execSQL(indices.getJSONObject(item).getString("createSql").replace("$" + "{TABLE_NAME}", entity.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (index in 0 until setup.length()) execSQL(setup.getString(index))
            execSQL("""INSERT INTO events (id,title,note,solarDate,hour,minute,allDay,repeat,type,reminderDaysBefore,reminderEnabled,lunarDay,lunarMonth,leapMonthPolicy,missingDayPolicy,endSolarDate,endHour,endMinute,location,colorArgb,icsUid,icsCalendar)
                VALUES (61,'Không mất dữ liệu','Ghi chú','2026-10-23',9,0,0,'NONE','EVENT',0,1,NULL,NULL,'REGULAR_ONLY','LAST_DAY',NULL,NULL,NULL,'Hà Nội',NULL,'v3-61@test.local',NULL)""")
            execSQL("INSERT INTO event_reminders (id,eventId,offsetMinutes) VALUES (71,61,15)")
            version = 3; close()
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_3_4).build()
        try {
            assertEquals("Không mất dữ liệu", db.eventDao().getById(61)?.title)
            assertEquals("Hà Nội", db.eventDao().getById(61)?.location)
            assertEquals(71L, db.reminderDao().forEvent(61).single().id)
            assertEquals(emptyList<HoroscopeProfileEntity>(), db.horoscopeProfileDao().getAll())
            val id = db.horoscopeProfileDao().insert(HoroscopeProfileEntity(label = "Mới"))
            assertEquals("Mới", db.horoscopeProfileDao().getById(id)?.label)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun backupCodecPreservesConventionsAndRejectsCoercibleWrongTypes() {
        val profile = HoroscopeProfileEntity(label = "Mây", group = "FAMILY", dateKind = "LUNAR", year = 2023, month = 2, day = 1,
            isLeap = true, hour = 23, minute = 45, traditionalSex = "FEMALE", civilDayRule = "EARLY_ZI_NEXT_DAY")
        assertEquals(profile, HoroscopeProfileCodec.decode(HoroscopeProfileCodec.encode(profile)))
        val invalid = HoroscopeProfileCodec.encode(profile).put("utcOffsetMinutes", "420")
        assertTrue(runCatching { HoroscopeProfileCodec.decode(invalid) }.isFailure)
        val falseLeap = HoroscopeProfileCodec.encode(profile).put("year", 2026)
        assertTrue(runCatching { HoroscopeProfileCodec.decode(falseLeap) }.isFailure)
    }
}
