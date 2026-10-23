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
import java.util.UUID
import vn.lichviet.app.horoscope.HoroscopeProfileEntity
import vn.lichviet.app.horoscope.HoroscopeProfileCodec

data class BackupSnapshot(
    val events: List<EventEntity>,
    val preferences: UserPreferences,
    val reminderOffsets: List<List<Int>> = events.map { if (it.reminderEnabled) listOf(it.reminderDaysBefore * 1440) else emptyList() },
    val profiles: List<HoroscopeProfileEntity> = emptyList(),
    /** Older backups predate profiles; restoring them must preserve current profiles. */
    val profilesIncluded: Boolean = true,
)

/** JSON transport only; callers select files with Android's Storage Access Framework. */
@Singleton
class BackupRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val database = AppDatabase.get(context)
    private val preferences = PreferencesRepository(context)

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        EventMutationLock.mutex.withLock {
            val events = database.eventDao().getAll()
            BackupCodec.encode(BackupSnapshot(events, preferences.preferences.first(),
                events.map { event -> database.reminderDao().forEvent(event.id).map { it.offsetMinutes } },
                database.horoscopeProfileDao().getAll()))
        }
    }

    /** Replaces local events/settings. Separate stores use best-effort rollback on a settings write failure. */
    suspend fun importJson(json: String): Int = withContext(Dispatchers.IO) {
        val snapshot = BackupCodec.decode(json)
        EventMutationLock.mutex.withLock {
            withContext(NonCancellable) {
            val previousEvents = database.eventDao().getAll()
            val previousReminders = database.reminderDao().getAll()
            val previousProfiles = database.horoscopeProfileDao().getAll()
            val previousPreferences = preferences.preferences.first()
            val ids = database.withTransaction {
                database.eventDao().deleteAll()
                if (snapshot.profilesIncluded) {
                    database.horoscopeProfileDao().deleteAll()
                    snapshot.profiles.forEach { database.horoscopeProfileDao().insert(it.copy(id = 0)) }
                }
                snapshot.events.mapIndexed { index, event ->
                    database.eventDao().save(event.copy(id = 0,
                        icsUid = event.icsUid ?: "${UUID.randomUUID()}@lichviet.local")).also { id ->
                        snapshot.reminderOffsets[index].forEach { offset ->
                            database.reminderDao().insert(EventReminderEntity(eventId = id, offsetMinutes = offset))
                        }
                    }
                }
            }
            try {
                preferences.update(snapshot.preferences)
            } catch (error: Exception) {
                // No jobs have changed yet; retain the old event IDs used by their pending reminders.
                withContext(NonCancellable) {
                    try {
                        database.withTransaction {
                            database.eventDao().deleteAll()
                            previousEvents.forEach { database.eventDao().insert(it) }
                            previousReminders.forEach { database.reminderDao().insert(it) }
                            database.horoscopeProfileDao().deleteAll()
                            previousProfiles.forEach { database.horoscopeProfileDao().insert(it) }
                        }
                        preferences.update(previousPreferences)
                    } catch (rollbackError: Exception) { error.addSuppressed(rollbackError) }
                }
                throw error
            }
            withContext(NonCancellable) {
                clearReminders(previousReminders)
                ids.forEach { ReminderScheduler.scheduleLocked(context, it) }
                CalendarWidgets.requestUpdate(context)
            }
            ids.size
            }
        }
    }

    /** Removes saved events, settings, their notifications and pending event reminders. */
    suspend fun deleteAllData() = EventMutationLock.mutex.withLock {
        withContext(NonCancellable) {
            val previousReminders = database.reminderDao().getAll()
            database.withTransaction {
                database.eventDao().deleteAll()
                database.horoscopeProfileDao().deleteAll()
            }
            clearReminders(previousReminders)
            preferences.reset()
            CalendarWidgets.requestUpdate(context)
        }
    }

    private fun clearReminders(previousReminders: List<EventReminderEntity>) {
        previousReminders.forEach { ReminderScheduler.cancelAlarm(context, it.id) }
        WorkManager.getInstance(context).cancelAllWorkByTag("calendar-reminders")
        context.getSystemService(NotificationManager::class.java).cancelAll()
    }
}

object BackupCodec {
    const val MAX_JSON_CHARS = 20_000_000
    fun encode(snapshot: BackupSnapshot): String = JSONObject().apply {
        put("format", "lich-viet-backup")
        put("version", 5)
        require(snapshot.profiles.size <= 1000)
        put("profiles", JSONArray().apply { snapshot.profiles.forEach { put(HoroscopeProfileCodec.encode(it)) } })
        put("preferences", JSONObject().apply {
            put("theme", snapshot.preferences.theme.name)
            put("showLunar", snapshot.preferences.showLunar)
            put("showHolidays", snapshot.preferences.showHolidays)
            put("mondayFirst", snapshot.preferences.mondayFirst)
            put("showWeekNumbers", snapshot.preferences.showWeekNumbers)
            put("showAuspiciousHours", snapshot.preferences.showAuspiciousHours)
            put("animationsEnabled", snapshot.preferences.animationsEnabled)
        })
        put("events", JSONArray().apply {
            snapshot.events.forEachIndexed { index, event -> put(JSONObject().apply {
                put("title", event.title); put("note", event.note); put("solarDate", event.solarDate)
                put("hour", event.hour); put("minute", event.minute); put("repeat", event.repeat.name)
                put("allDay", event.allDay)
                put("type", event.type.name); put("reminderDaysBefore", event.reminderDaysBefore)
                put("reminderEnabled", event.reminderEnabled)
                put("lunarDay", event.lunarDay ?: JSONObject.NULL)
                put("lunarMonth", event.lunarMonth ?: JSONObject.NULL)
                put("leapMonthPolicy", event.leapMonthPolicy.name)
                put("missingDayPolicy", event.missingDayPolicy.name)
                put("endSolarDate", event.endSolarDate ?: JSONObject.NULL)
                put("endHour", event.endHour ?: JSONObject.NULL)
                put("endMinute", event.endMinute ?: JSONObject.NULL)
                put("location", event.location)
                put("colorArgb", event.colorArgb ?: JSONObject.NULL)
                put("icsUid", event.icsUid ?: JSONObject.NULL)
                put("icsCalendar", event.icsCalendar ?: JSONObject.NULL)
                put("reminderOffsetsMinutes", JSONArray().apply {
                    snapshot.reminderOffsets[index].forEach { offset -> put(offset) }
                })
            }) }
        })
    }.toString(2).also { require(it.length <= MAX_JSON_CHARS) { "Tệp sao lưu quá lớn" } }

    fun decode(json: String): BackupSnapshot {
        require(json.length <= MAX_JSON_CHARS) { "Tệp sao lưu quá lớn" }
        try {
            val root = JSONObject(json)
            val version = strictInt(root, "version")
            require(strictString(root, "format") == "lich-viet-backup" && version in 1..5) { "Định dạng sao lưu không được hỗ trợ" }
            val settings = root.getJSONObject("preferences")
            val preferences = UserPreferences(AppTheme.valueOf(strictString(settings, "theme")), strictBoolean(settings, "showLunar"),
                strictBoolean(settings, "showHolidays"), strictBoolean(settings, "mondayFirst"),
                showWeekNumbers = if (version >= 5) strictBoolean(settings, "showWeekNumbers") else false,
                showAuspiciousHours = if (version >= 5) strictBoolean(settings, "showAuspiciousHours") else true,
                animationsEnabled = if (version >= 5) strictBoolean(settings, "animationsEnabled") else true)
            val array = root.getJSONArray("events")
            require(array.length() <= 10_000) { "Tệp chứa quá nhiều sự kiện" }
            val offsets = mutableListOf<List<Int>>()
            val events = (0 until array.length()).map { index ->
                val event = array.getJSONObject(index)
                val decoded = EventValidation.validated(EventEntity(
                    title = strictString(event, "title"), note = strictString(event, "note"), solarDate = strictString(event, "solarDate"),
                    hour = strictInt(event, "hour"), minute = strictInt(event, "minute"),
                    allDay = if (version >= 2) strictBoolean(event, "allDay") else false,
                    repeat = RepeatRule.valueOf(strictString(event, "repeat")), type = EventType.valueOf(strictString(event, "type")),
                    reminderDaysBefore = strictInt(event, "reminderDaysBefore"), reminderEnabled = strictBoolean(event, "reminderEnabled"),
                    lunarDay = if (event.isNull("lunarDay")) null else strictInt(event, "lunarDay"),
                    lunarMonth = if (event.isNull("lunarMonth")) null else strictInt(event, "lunarMonth"),
                    leapMonthPolicy = LeapMonthPolicy.valueOf(strictString(event, "leapMonthPolicy")),
                    missingDayPolicy = MissingDayPolicy.valueOf(strictString(event, "missingDayPolicy")),
                    endSolarDate = if (version >= 3) nullableString(event, "endSolarDate") else null,
                    endHour = if (version >= 3) nullableInt(event, "endHour") else null,
                    endMinute = if (version >= 3) nullableInt(event, "endMinute") else null,
                    location = if (version >= 3) strictString(event, "location") else "",
                    colorArgb = if (version >= 3) nullableInt(event, "colorArgb") else null,
                    icsUid = if (version >= 3) nullableString(event, "icsUid") else null,
                    icsCalendar = if (version >= 3) nullableString(event, "icsCalendar") else null,
                ))
                val choices = if (version == 1) {
                    if (decoded.reminderEnabled) listOf(decoded.reminderDaysBefore * 1440) else emptyList()
                } else {
                    val values = event.getJSONArray("reminderOffsetsMinutes")
                    require(values.length() <= 16) { "Quá nhiều mốc nhắc" }
                    (0 until values.length()).map { strictArrayInt(values, it) }.also { numbers ->
                        require(numbers.distinct().size == numbers.size && numbers.all { it in 0..525_600 }) { "Mốc nhắc không hợp lệ" }
                        require(!decoded.reminderEnabled || numbers.isNotEmpty()) { "Sự kiện thiếu mốc nhắc" }
                    }
                }
                offsets += choices
                decoded
            }
            require(events.mapNotNull { it.icsUid }.let { it.size == it.distinct().size }) { "UID trùng trong bản sao lưu" }
            events.forEach { event ->
                event.icsCalendar?.let { canonical ->
                    val preview = vn.lichviet.app.ics.IcsCodec.preview(canonical)
                    require(preview.fileErrors.isEmpty() && preview.items.size == 1 && preview.items.single().canImport && preview.items.single().uid == event.icsUid) { "Chuỗi ICS sao lưu không hợp lệ" }
                }
            }
            val profiles = if (version >= 4) {
                val arrayProfiles = root.getJSONArray("profiles")
                require(arrayProfiles.length() <= 1000)
                (0 until arrayProfiles.length()).map { HoroscopeProfileCodec.decode(arrayProfiles.getJSONObject(it)) }
            } else emptyList()
            return BackupSnapshot(events, preferences, offsets, profiles, version >= 4)
        } catch (error: Exception) {
            throw IllegalArgumentException("Tệp sao lưu không hợp lệ hoặc không được hỗ trợ", error)
        }
    }

    private fun strictString(json: JSONObject, key: String): String =
        json.get(key) as? String ?: throw IllegalArgumentException("Chuỗi không hợp lệ")

    private fun strictBoolean(json: JSONObject, key: String): Boolean =
        json.get(key) as? Boolean ?: throw IllegalArgumentException("Giá trị không hợp lệ")

    private fun nullableString(json: JSONObject, key: String): String? =
        if (json.isNull(key)) null else strictString(json, key)

    private fun nullableInt(json: JSONObject, key: String): Int? =
        if (json.isNull(key)) null else strictInt(json, key)

    private fun strictInt(json: JSONObject, key: String): Int {
        val number = json.get(key) as? Number ?: throw IllegalArgumentException("Số không hợp lệ")
        val value = number.toDouble()
        require(value.isFinite() && value % 1.0 == 0.0 && value in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble())
        return value.toInt()
    }

    private fun strictArrayInt(array: JSONArray, index: Int): Int {
        val number = array.get(index) as? Number ?: throw IllegalArgumentException("Số không hợp lệ")
        val value = number.toDouble()
        require(value.isFinite() && value % 1.0 == 0.0 && value in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble())
        return value.toInt()
    }
}
