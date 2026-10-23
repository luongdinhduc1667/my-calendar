package vn.lichviet.app.reminder

import android.app.NotificationManager
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.sync.withLock
import vn.lichviet.app.data.AppDatabase
import vn.lichviet.app.data.EventEntity
import vn.lichviet.app.data.EventMutationLock
import vn.lichviet.app.data.EventReminderEntity
import vn.lichviet.app.data.Recurrence
import vn.lichviet.app.ics.EventOccurrence
import vn.lichviet.app.ics.EventOccurrenceEngine
import vn.lichviet.core.VietnameseCalendar
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/** AlarmManager delivers user-visible reminders; WorkManager only reconciles durable Room state. */
object ReminderScheduler {
    const val CHANNEL_ID = ReminderNotificationManager.CHANNEL_ID
    private const val REFRESH = "calendar-refresh"
    private const val PERIODIC = "calendar-reminder-reconcile"

    fun initialize(context: Context) {
        ReminderNotificationManager.ensureChannel(context)
        val work = WorkManager.getInstance(context)
        // Existing v0.1 WorkRequests are intentionally inert after upgrading EventReminderWorker.
        work.cancelAllWorkByTag("calendar-reminders")
        work.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<CalendarRefreshWorker>(12, TimeUnit.HOURS).build())
        requestRefresh(context)
    }

    fun requestRefresh(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(REFRESH, ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<CalendarRefreshWorker>().build())
    }

    fun ensureChannel(context: Context) = ReminderNotificationManager.ensureChannel(context)

    suspend fun rescheduleAll(context: Context) = EventMutationLock.mutex.withLock {
        AppDatabase.get(context).eventDao().getAll().forEach { scheduleLocked(context, it.id) }
    }

    suspend fun schedule(context: Context, id: Long) = EventMutationLock.mutex.withLock { scheduleLocked(context, id) }

    /** Caller holds EventMutationLock from the event write through alarm enqueue. */
    internal suspend fun scheduleLocked(context: Context, id: Long) {
        val database = AppDatabase.get(context)
        val event = database.eventDao().getById(id)
        var reminders = database.reminderDao().forEvent(id)
        if (event == null || !event.reminderEnabled) {
            reminders.forEach { cancelAlarm(context, it.id) }
            return
        }
        if (reminders.isEmpty()) {
            database.reminderDao().insert(EventReminderEntity(eventId = id, offsetMinutes = event.reminderDaysBefore * 1440))
            reminders = database.reminderDao().forEvent(id)
        }
        reminders.forEach { scheduleRowLocked(context, event, it) }
    }

    private suspend fun scheduleRowLocked(context: Context, event: EventEntity, row: EventReminderEntity) {
        val dao = AppDatabase.get(context).reminderDao()
        AlarmReminderScheduler.cancel(context, row.id)
        val now = ZonedDateTime.now(VietnameseCalendar.zoneId)
        val nowMillis = now.toInstant().toEpochMilli()
        val pendingDate = row.scheduledDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val pendingKey = row.scheduledOccurrenceKey ?: row.scheduledDate
        val pendingAt = row.scheduledTriggerMillis
        val pendingOccurrence = if (pendingDate != null && pendingKey != null)
            resolve(event, pendingDate, pendingKey) else null
        val pendingExpectedAt = pendingOccurrence?.let { reminderAnchor(event, it).minusSeconds(row.offsetMinutes * 60L).toEpochMilli() }
        val pendingUndelivered = pendingOccurrence != null && pendingAt != null && pendingExpectedAt == pendingAt &&
            !(row.lastDeliveredOccurrenceKey == pendingKey && row.lastDeliveredTriggerMillis == pendingAt) &&
            row.completedOccurrenceKey != pendingKey &&
            pendingAt >= nowMillis - 24 * 60 * 60 * 1000L
        if (pendingUndelivered) {
            // Reboot/reconciliation must preserve an inexact alarm that Android has delayed.
            AlarmReminderScheduler.schedule(context, row.id, row.scheduledDate!!, pendingKey!!, pendingAt!!)
        } else {
            var plan = ReminderPlanner.nextForRow(event, row.offsetMinutes, now,
                row.lastDeliveredOccurrenceKey, row.lastDeliveredTriggerMillis)
            if (plan != null && plan.occurrenceKey == row.completedOccurrenceKey) {
                plan = ReminderPlanner.nextForRow(event, row.offsetMinutes, now,
                    plan.occurrenceKey, plan.trigger.toInstant().toEpochMilli())
            }
            if (plan == null) {
                dao.setScheduled(row.id, null, null, null)
            } else {
                val millis = plan.trigger.toInstant().toEpochMilli()
                dao.setScheduled(row.id, plan.occurrenceDate.toString(), plan.occurrenceKey, millis)
                AlarmReminderScheduler.schedule(context, row.id, plan.occurrenceDate.toString(), plan.occurrenceKey, millis)
            }
        }
        AlarmReminderScheduler.cancel(context, row.id, snooze = true)
        val snoozeKey = row.snoozeOccurrenceKey ?: row.snoozeDate
        if (row.snoozeDate != null && snoozeKey != null && row.snoozeUntilMillis != null && snoozeKey != row.completedOccurrenceKey &&
            resolve(event, LocalDate.parse(row.snoozeDate), snoozeKey) != null) {
            if (row.snoozeUntilMillis >= nowMillis - 24 * 60 * 60 * 1000L) {
                AlarmReminderScheduler.schedule(context, row.id, row.snoozeDate, snoozeKey, row.snoozeUntilMillis, snooze = true)
            } else dao.setSnooze(row.id, null, null, null)
        }
    }

    private fun reminderAnchor(event: EventEntity, occurrence: EventOccurrence): Instant =
        if (event.icsCalendar == null && occurrence.allDay)
            occurrence.startDate.atTime(9, 0).atZone(VietnameseCalendar.zoneId).toInstant()
        else occurrence.start

    private fun resolve(event: EventEntity, date: LocalDate, key: String): EventOccurrence? =
        EventOccurrenceEngine.between(listOf(event), date, date.plusDays(1))
            .firstOrNull { it.occurrenceKey == key && it.startDate == date }

    /** Cancel before deleting a parent; its child rows are removed by the foreign key. */
    suspend fun cancel(context: Context, id: Long) = EventMutationLock.mutex.withLock { cancelLocked(context, id) }

    internal suspend fun cancelLocked(context: Context, id: Long) {
        AppDatabase.get(context).reminderDao().forEvent(id).forEach { cancelAlarm(context, it.id) }
        // Notifications posted by v0.1 used the event ID directly.
        context.getSystemService(NotificationManager::class.java).cancel(id.hashCode())
        WorkManager.getInstance(context).cancelUniqueWork("event-reminder-$id")
    }

    internal fun cancelAlarm(context: Context, reminderId: Long) {
        AlarmReminderScheduler.cancel(context, reminderId)
        AlarmReminderScheduler.cancel(context, reminderId, snooze = true)
        ReminderNotificationManager.cancel(context, reminderId)
    }

    suspend fun fire(context: Context, reminderId: Long, occurrence: String, key: String, trigger: Long, snooze: Boolean) = EventMutationLock.mutex.withLock {
        val db = AppDatabase.get(context)
        val dao = db.reminderDao()
        val row = dao.getById(reminderId) ?: return@withLock
        val event = db.eventDao().getById(row.eventId) ?: return@withLock
        val date = runCatching { LocalDate.parse(occurrence) }.getOrNull() ?: return@withLock
        val actual = resolve(event, date, key) ?: return@withLock
        if (!event.reminderEnabled || row.completedOccurrenceKey == key) return@withLock
        if (!ReminderNotificationManager.canNotify(context)) return@withLock
        if (snooze) {
            if (dao.claimSnooze(row.id, key, trigger) != 1) return@withLock
        } else {
            if (dao.claimDelivery(row.id, occurrence, key, trigger) != 1) return@withLock
        }
        if (!ReminderNotificationManager.show(context, actual, row)) {
            if (snooze) dao.setSnooze(row.id, occurrence, key, trigger)
            else dao.releaseDelivery(row.id, key, trigger)
            return@withLock
        }
        if (!snooze) scheduleRowLocked(context, event, row.copy(lastDeliveredDate = occurrence,
            lastDeliveredOccurrenceKey = key,
            lastDeliveredTriggerMillis = trigger))
    }

    suspend fun snooze(context: Context, reminderId: Long, occurrence: String, key: String, minutes: Int) = EventMutationLock.mutex.withLock {
        if (minutes !in listOf(5, 10, 15)) return@withLock
        val db = AppDatabase.get(context)
        val row = db.reminderDao().getById(reminderId) ?: return@withLock
        if (row.lastDeliveredOccurrenceKey != key || row.completedOccurrenceKey == key || db.eventDao().getById(row.eventId)?.reminderEnabled != true) return@withLock
        val millis = Instant.now().plusSeconds(minutes * 60L).toEpochMilli()
        db.reminderDao().setSnooze(row.id, occurrence, key, millis)
        AlarmReminderScheduler.schedule(context, row.id, occurrence, key, millis, snooze = true)
        ReminderNotificationManager.cancel(context, row.id)
    }

    suspend fun snoozeChoices(context: Context, reminderId: Long, occurrence: String, key: String) = EventMutationLock.mutex.withLock {
        val db = AppDatabase.get(context)
        val row = db.reminderDao().getById(reminderId) ?: return@withLock
        val event = db.eventDao().getById(row.eventId) ?: return@withLock
        if (event.reminderEnabled && row.lastDeliveredOccurrenceKey == key && row.completedOccurrenceKey != key) {
            val date = runCatching { LocalDate.parse(occurrence) }.getOrNull() ?: return@withLock
            val actual = resolve(event, date, key) ?: return@withLock
            ReminderNotificationManager.showSnoozeChoices(context, actual, row)
        }
    }

    suspend fun complete(context: Context, reminderId: Long, occurrence: String, key: String) = EventMutationLock.mutex.withLock {
        val db = AppDatabase.get(context)
        val row = db.reminderDao().getById(reminderId) ?: return@withLock
        if (row.lastDeliveredOccurrenceKey != key && row.scheduledOccurrenceKey != key && row.snoozeOccurrenceKey != key) return@withLock
        db.reminderDao().completeOccurrence(row.eventId, occurrence, key)
        db.reminderDao().forEvent(row.eventId).forEach { cancelAlarm(context, it.id) }
        scheduleLocked(context, row.eventId)
    }
}
