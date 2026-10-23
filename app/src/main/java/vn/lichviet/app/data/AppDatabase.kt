package vn.lichviet.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import vn.lichviet.app.horoscope.HoroscopeProfileEntity
import vn.lichviet.app.horoscope.HoroscopeProfileDao

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY solarDate, hour, minute, id")
    fun observeAll(): Flow<List<EventEntity>>
    @Query("SELECT * FROM events ORDER BY solarDate, hour, minute, id")
    suspend fun getAll(): List<EventEntity>
    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: Long): EventEntity?
    @Query("SELECT * FROM events WHERE icsUid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): EventEntity?
    @Insert
    suspend fun insert(event: EventEntity): Long
    @Update
    suspend fun update(event: EventEntity): Int
    /** REPLACE deletes the parent row and would cascade-delete stable reminder IDs. */
    suspend fun save(event: EventEntity): Long {
        if (event.id == 0L) return insert(event)
        check(update(event) == 1) { "Event not found: ${event.id}" }
        return event.id
    }
    @Query("DELETE FROM events WHERE id = :id")
    suspend fun delete(id: Long)
    @Query("DELETE FROM events")
    suspend fun deleteAll()
}

class EventConverters {
    @TypeConverter fun repeatToString(value: RepeatRule): String = value.name
    @TypeConverter fun stringToRepeat(value: String): RepeatRule = RepeatRule.valueOf(value)
    @TypeConverter fun typeToString(value: EventType): String = value.name
    @TypeConverter fun stringToType(value: String): EventType = EventType.valueOf(value)
    @TypeConverter fun leapToString(value: LeapMonthPolicy): String = value.name
    @TypeConverter fun stringToLeap(value: String): LeapMonthPolicy = LeapMonthPolicy.valueOf(value)
    @TypeConverter fun missingToString(value: MissingDayPolicy): String = value.name
    @TypeConverter fun stringToMissing(value: String): MissingDayPolicy = MissingDayPolicy.valueOf(value)
}

@Database(entities = [EventEntity::class, EventReminderEntity::class, HoroscopeProfileEntity::class], version = 4, exportSchema = true)
@TypeConverters(EventConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun reminderDao(): EventReminderDao
    abstract fun horoscopeProfileDao(): HoroscopeProfileDao
    companion object {
        @Volatile private var instance: AppDatabase? = null
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `events` ADD COLUMN `allDay` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""CREATE TABLE IF NOT EXISTS `event_reminders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `eventId` INTEGER NOT NULL, `offsetMinutes` INTEGER NOT NULL, `scheduledDate` TEXT, `scheduledTriggerMillis` INTEGER, `lastDeliveredDate` TEXT, `lastDeliveredTriggerMillis` INTEGER, `snoozeDate` TEXT, `snoozeUntilMillis` INTEGER, `completedDate` TEXT, FOREIGN KEY(`eventId`) REFERENCES `events`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_event_reminders_eventId` ON `event_reminders` (`eventId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_event_reminders_eventId_offsetMinutes` ON `event_reminders` (`eventId`, `offsetMinutes`)")
                db.execSQL("INSERT INTO event_reminders (eventId, offsetMinutes) SELECT id, reminderDaysBefore * 1440 FROM events WHERE reminderEnabled = 1")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `events` ADD COLUMN `endSolarDate` TEXT")
                db.execSQL("ALTER TABLE `events` ADD COLUMN `endHour` INTEGER")
                db.execSQL("ALTER TABLE `events` ADD COLUMN `endMinute` INTEGER")
                db.execSQL("ALTER TABLE `events` ADD COLUMN `location` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `events` ADD COLUMN `colorArgb` INTEGER")
                db.execSQL("ALTER TABLE `events` ADD COLUMN `icsUid` TEXT")
                db.execSQL("ALTER TABLE `events` ADD COLUMN `icsCalendar` TEXT")
                val ids = db.query("SELECT id FROM events").use { cursor ->
                    buildList { while (cursor.moveToNext()) add(cursor.getLong(0)) }
                }
                ids.forEach { id ->
                    val uid = "${java.util.UUID.randomUUID()}@lichviet.local"
                    db.execSQL("UPDATE events SET icsUid = ? WHERE id = ?", arrayOf<Any>(uid, id))
                }
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_events_icsUid` ON `events` (`icsUid`)")
                db.execSQL("ALTER TABLE `event_reminders` ADD COLUMN `scheduledOccurrenceKey` TEXT")
                db.execSQL("ALTER TABLE `event_reminders` ADD COLUMN `lastDeliveredOccurrenceKey` TEXT")
                db.execSQL("ALTER TABLE `event_reminders` ADD COLUMN `snoozeOccurrenceKey` TEXT")
                db.execSQL("ALTER TABLE `event_reminders` ADD COLUMN `completedOccurrenceKey` TEXT")
                db.execSQL("UPDATE event_reminders SET scheduledOccurrenceKey = scheduledDate, lastDeliveredOccurrenceKey = lastDeliveredDate, snoozeOccurrenceKey = snoozeDate, completedOccurrenceKey = completedDate")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS `horoscope_profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `label` TEXT NOT NULL, `group` TEXT NOT NULL, `dateKind` TEXT NOT NULL, `year` INTEGER NOT NULL, `month` INTEGER NOT NULL, `day` INTEGER NOT NULL, `isLeap` INTEGER NOT NULL, `hour` INTEGER NOT NULL, `minute` INTEGER NOT NULL, `traditionalSex` TEXT NOT NULL, `civilDayRule` TEXT NOT NULL, `utcOffsetMinutes` INTEGER NOT NULL)""")
            }
        }
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "lich-viet.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build().also { instance = it }
        }
    }
}
