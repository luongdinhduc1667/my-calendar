package vn.lichviet.app.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.sync.withLock
import vn.lichviet.app.data.AppDatabase
import vn.lichviet.app.data.EventEntity
import vn.lichviet.app.data.EventMutationLock
import vn.lichviet.core.VietnameseCalendar
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/** Persistent best-effort reminders. Android battery restrictions can delay delivery. */
object ReminderScheduler {
    const val CHANNEL_ID = "calendar_reminders"
    internal const val EVENT_ID = "event_id"
    internal const val OCCURRENCE = "occurrence"
    internal const val FINGERPRINT = "fingerprint"
    private fun workName(id: Long) = "event-reminder-$id"

    fun initialize(context: Context) {
        ensureChannel(context)
        WorkManager.getInstance(context).enqueueUniqueWork(
            "calendar-refresh", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<CalendarRefreshWorker>().build(),
        )
    }

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL_ID, "Nhắc lịch và ngày giỗ", NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Sinh nhật, ngày giỗ và sự kiện bạn đã lưu" })
    }

    suspend fun schedule(context: Context, id: Long, append: Boolean = false, afterOccurrence: LocalDate? = null) {
        EventMutationLock.mutex.withLock { scheduleLocked(context, id, append, afterOccurrence) }
    }

    /** Caller must hold EventMutationLock, including the initial read and enqueue. */
    internal suspend fun scheduleLocked(context: Context, id: Long, append: Boolean = false, afterOccurrence: LocalDate? = null) {
        val event = AppDatabase.get(context).eventDao().getById(id)
        if (event == null || !event.reminderEnabled) { cancel(context, id); return }
        val now = ZonedDateTime.now(VietnameseCalendar.zoneId)
        val plan = ReminderPlanner.next(event, now, afterOccurrence)
        if (plan == null) {
            // The just-delivered notification must remain visible for a one-time event.
            WorkManager.getInstance(context).cancelUniqueWork(workName(id))
            return
        }
        val input = Data.Builder().putLong(EVENT_ID, id)
            .putString(OCCURRENCE, plan.occurrenceDate.toString()).putInt(FINGERPRINT, fingerprint(event)).build()
        val request = OneTimeWorkRequestBuilder<EventReminderWorker>()
            .setInputData(input)
            .setInitialDelay(Duration.between(now, plan.trigger).toMillis().coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .addTag("calendar-reminders").build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(id), if (append) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE, request,
        )
    }

    internal fun fingerprint(event: EventEntity): Int = event.toString().hashCode()

    fun cancel(context: Context, id: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(id))
        context.getSystemService(NotificationManager::class.java).cancel(id.hashCode())
    }
}
