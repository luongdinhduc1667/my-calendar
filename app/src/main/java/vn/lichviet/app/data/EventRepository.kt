package vn.lichviet.app.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import vn.lichviet.app.reminder.ReminderScheduler
import vn.lichviet.app.widget.CalendarWidgets
import java.time.LocalDate
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val dao = AppDatabase.get(context).eventDao()
    val events = dao.observeAll()
    suspend fun getAll(): List<EventEntity> = dao.getAll()
    suspend fun getById(id: Long): EventEntity? = dao.getById(id)

    suspend fun save(event: EventEntity): Long {
        val validated = EventValidation.validated(event)
        return EventMutationLock.mutex.withLock {
            withContext(NonCancellable) {
                val id = dao.save(validated)
                if (event.id != 0L) ReminderScheduler.cancel(context, event.id)
                ReminderScheduler.scheduleLocked(context, id)
                CalendarWidgets.requestUpdate(context)
                id
            }
        }
    }

    suspend fun delete(id: Long) = EventMutationLock.mutex.withLock {
        withContext(NonCancellable) {
            dao.delete(id)
            ReminderScheduler.cancel(context, id)
            CalendarWidgets.requestUpdate(context)
        }
    }

    fun occurrences(date: LocalDate, events: List<EventEntity>): List<EventOccurrence> = events
        .filter { Recurrence.occursOn(it, date) }.map { EventOccurrence(it, date) }
        .sortedWith(compareBy({ it.event.hour }, { it.event.minute }, { it.event.id }))
}
