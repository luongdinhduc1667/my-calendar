package vn.lichviet.app.data

import android.content.Context
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import vn.lichviet.app.reminder.ReminderScheduler
import vn.lichviet.app.widget.CalendarWidgets
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val database = AppDatabase.get(context)
    private val dao = database.eventDao()
    val events = dao.observeAll()
    suspend fun getAll(): List<EventEntity> = dao.getAll()
    suspend fun getById(id: Long): EventEntity? = dao.getById(id)

    suspend fun reminderOffsets(eventId: Long): List<Int> =
        AppDatabase.get(context).reminderDao().forEvent(eventId).map { it.offsetMinutes }

    suspend fun save(event: EventEntity, offsetsMinutes: List<Int>? = null): Long {
        val validated = EventValidation.validated(event).let {
            if (it.icsUid == null) it.copy(icsUid = "${UUID.randomUUID()}@lichviet.local") else it
        }
        val offsets = offsetsMinutes?.distinct()?.sorted()?.also {
            require(it.size <= 16 && it.all { offset -> offset in 0..525_600 }) { "Mốc nhắc không hợp lệ" }
            require(!validated.reminderEnabled || it.isNotEmpty()) { "Hãy chọn ít nhất một mốc nhắc" }
        }
        return EventMutationLock.mutex.withLock {
            withContext(NonCancellable + Dispatchers.IO) {
                val (id, previouslyExisting, removedIds) = database.withTransaction {
                    val old = if (validated.id != 0L) dao.getById(validated.id) else null
                    val savedId = dao.save(validated)
                    val reminderDao = database.reminderDao()
                    val existing = reminderDao.forEvent(savedId)
                    if (old != null && scheduleChanged(old, validated)) reminderDao.resetAfterEventEdit(savedId)
                    val removed = mutableListOf<Long>()
                    if (offsets != null) {
                        existing.filter { it.offsetMinutes !in offsets }.forEach {
                            removed += it.id
                            reminderDao.delete(it.id)
                        }
                        val current = existing.map { it.offsetMinutes }.toSet()
                        offsets.filterNot { it in current }.forEach {
                            reminderDao.insert(EventReminderEntity(eventId = savedId, offsetMinutes = it))
                        }
                    }
                    Triple(savedId, old != null, removed)
                }
                removedIds.forEach { ReminderScheduler.cancelAlarm(context, it) }
                if (previouslyExisting) ReminderScheduler.cancelLocked(context, id)
                ReminderScheduler.scheduleLocked(context, id)
                CalendarWidgets.requestUpdate(context)
                id
            }
        }
    }

    suspend fun delete(id: Long) = EventMutationLock.mutex.withLock {
        withContext(NonCancellable + Dispatchers.IO) {
            val removed = database.withTransaction {
                val reminders = database.reminderDao().forEvent(id)
                dao.delete(id)
                reminders
            }
            removed.forEach { ReminderScheduler.cancelAlarm(context, it.id) }
            ReminderScheduler.cancelLocked(context, id)
            CalendarWidgets.requestUpdate(context)
        }
    }

    private fun scheduleChanged(old: EventEntity, updated: EventEntity): Boolean =
        old.solarDate != updated.solarDate || old.hour != updated.hour || old.minute != updated.minute ||
            old.allDay != updated.allDay || old.repeat != updated.repeat || old.type != updated.type ||
            old.reminderEnabled != updated.reminderEnabled || old.lunarDay != updated.lunarDay ||
            old.lunarMonth != updated.lunarMonth || old.leapMonthPolicy != updated.leapMonthPolicy ||
            old.missingDayPolicy != updated.missingDayPolicy || old.icsCalendar != updated.icsCalendar
}
