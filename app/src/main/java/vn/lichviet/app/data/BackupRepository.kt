package vn.lichviet.app.data

import android.app.NotificationManager
import android.content.Context
import androidx.room.withTransaction
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import vn.lichviet.app.reminder.ReminderScheduler
import vn.lichviet.app.widget.CalendarWidgets
import javax.inject.Inject
import javax.inject.Singleton

data class BackupSnapshot(val events: List<EventEntity>, val preferences: UserPreferences)

/** JSON transport only; callers select files with Android's Storage Access Framework. */
@Singleton
class BackupRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val database = AppDatabase.get(context)
    private val preferences = PreferencesRepository(context)

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        EventMutationLock.mutex.withLock {
            BackupCodec.encode(BackupSnapshot(database.eventDao().getAll(), preferences.preferences.first()))
        }
    }

    /** Replaces local events/settings. Separate stores use best-effort rollback on a settings write failure. */
    suspend fun importJson(json: String): Int = withContext(Dispatchers.IO) {
        val snapshot = BackupCodec.decode(json)
        EventMutationLock.mutex.withLock {
            val previousEvents = database.eventDao().getAll()
            val previousPreferences = preferences.preferences.first()
            val ids = database.withTransaction {
                database.eventDao().deleteAll()
                snapshot.events.map { database.eventDao().save(it.copy(id = 0)) }
            }
            try {
                preferences.update(snapshot.preferences)
            } catch (error: Exception) {
                // No jobs have changed yet; retain the old event IDs used by their pending reminders.
                withContext(NonCancellable) {
                    try {
                        database.withTransaction {
                            database.eventDao().deleteAll()
                            previousEvents.forEach { database.eventDao().save(it) }
                        }
                        preferences.update(previousPreferences)
                    } catch (rollbackError: Exception) { error.addSuppressed(rollbackError) }
                }
                throw error
            }
            withContext(NonCancellable) {
                clearReminders()
                ids.forEach { ReminderScheduler.scheduleLocked(context, it) }
                CalendarWidgets.requestUpdate(context)
            }
            ids.size
        }
    }

    /** Removes saved events, settings, their notifications and pending event reminders. */
    suspend fun deleteAllData() = EventMutationLock.mutex.withLock {
        database.eventDao().deleteAll()
        clearReminders()
        preferences.reset()
        CalendarWidgets.requestUpdate(context)
    }

    private fun clearReminders() {
        WorkManager.getInstance(context).cancelAllWorkByTag("calendar-reminders")
        context.getSystemService(NotificationManager::class.java).cancelAll()
    }
}

object BackupCodec {
    fun encode(snapshot: BackupSnapshot): String = JSONObject().apply {
        put("format", "lich-viet-backup")
        put("version", 1)
        put("preferences", JSONObject().apply {
            put("theme", snapshot.preferences.theme.name)
            put("showLunar", snapshot.preferences.showLunar)
            put("showHolidays", snapshot.preferences.showHolidays)
            put("mondayFirst", snapshot.preferences.mondayFirst)
        })
        put("events", JSONArray().apply {
            snapshot.events.forEach { event -> put(JSONObject().apply {
                put("title", event.title); put("note", event.note); put("solarDate", event.solarDate)
                put("hour", event.hour); put("minute", event.minute); put("repeat", event.repeat.name)
                put("type", event.type.name); put("reminderDaysBefore", event.reminderDaysBefore)
                put("reminderEnabled", event.reminderEnabled)
                put("lunarDay", event.lunarDay ?: JSONObject.NULL)
                put("lunarMonth", event.lunarMonth ?: JSONObject.NULL)
                put("leapMonthPolicy", event.leapMonthPolicy.name)
                put("missingDayPolicy", event.missingDayPolicy.name)
            }) }
        })
    }.toString(2)

    fun decode(json: String): BackupSnapshot {
        require(json.length <= 2_000_000) { "Tệp sao lưu quá lớn" }
        try {
            val root = JSONObject(json)
            require(strictString(root, "format") == "lich-viet-backup" && strictInt(root, "version") == 1) { "Định dạng sao lưu không được hỗ trợ" }
            val settings = root.getJSONObject("preferences")
            val preferences = UserPreferences(AppTheme.valueOf(strictString(settings, "theme")), strictBoolean(settings, "showLunar"),
                strictBoolean(settings, "showHolidays"), strictBoolean(settings, "mondayFirst"))
            val array = root.getJSONArray("events")
            require(array.length() <= 10_000) { "Tệp chứa quá nhiều sự kiện" }
            val events = (0 until array.length()).map { index ->
                val event = array.getJSONObject(index)
                EventValidation.validated(EventEntity(
                    title = strictString(event, "title"), note = strictString(event, "note"), solarDate = strictString(event, "solarDate"),
                    hour = strictInt(event, "hour"), minute = strictInt(event, "minute"),
                    repeat = RepeatRule.valueOf(strictString(event, "repeat")), type = EventType.valueOf(strictString(event, "type")),
                    reminderDaysBefore = strictInt(event, "reminderDaysBefore"), reminderEnabled = strictBoolean(event, "reminderEnabled"),
                    lunarDay = if (event.isNull("lunarDay")) null else strictInt(event, "lunarDay"),
                    lunarMonth = if (event.isNull("lunarMonth")) null else strictInt(event, "lunarMonth"),
                    leapMonthPolicy = LeapMonthPolicy.valueOf(strictString(event, "leapMonthPolicy")),
                    missingDayPolicy = MissingDayPolicy.valueOf(strictString(event, "missingDayPolicy")),
                ))
            }
            return BackupSnapshot(events, preferences)
        } catch (error: Exception) {
            throw IllegalArgumentException("Tệp sao lưu không hợp lệ hoặc không được hỗ trợ", error)
        }
    }

    private fun strictString(json: JSONObject, key: String): String =
        json.get(key) as? String ?: throw IllegalArgumentException("Chuỗi không hợp lệ")

    private fun strictBoolean(json: JSONObject, key: String): Boolean =
        json.get(key) as? Boolean ?: throw IllegalArgumentException("Giá trị không hợp lệ")

    private fun strictInt(json: JSONObject, key: String): Int {
        val number = json.get(key) as? Number ?: throw IllegalArgumentException("Số không hợp lệ")
        val value = number.toDouble()
        require(value.isFinite() && value % 1.0 == 0.0 && value in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble())
        return value.toInt()
    }
}
