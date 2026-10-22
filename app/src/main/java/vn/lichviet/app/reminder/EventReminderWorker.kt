package vn.lichviet.app.reminder

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.sync.withLock
import vn.lichviet.app.data.AppDatabase
import vn.lichviet.app.data.EventMutationLock
import vn.lichviet.app.data.Recurrence
import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class EventReminderWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = EventMutationLock.mutex.withLock {
        val id = inputData.getLong(ReminderScheduler.EVENT_ID, -1)
        val event = AppDatabase.get(applicationContext).eventDao().getById(id) ?: return@withLock Result.success()
        if (!event.reminderEnabled || ReminderScheduler.fingerprint(event) != inputData.getInt(ReminderScheduler.FINGERPRINT, 0))
            return@withLock Result.success()
        val date = runCatching { LocalDate.parse(inputData.getString(ReminderScheduler.OCCURRENCE)) }.getOrNull()
            ?: return@withLock Result.failure()
        val today = LocalDate.now(VietnameseCalendar.zoneId)
        if (date >= today && Recurrence.occursOn(event, date) && canNotify()) {
            ReminderScheduler.ensureChannel(applicationContext)
            val intent = Intent().setClassName(applicationContext, "vn.lichviet.app.MainActivity")
                .setData(Uri.parse("lichviet://reminder/$id/$date"))
                .putExtra("selected_date", date.toString())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pending = PendingIntent.getActivity(applicationContext, id.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val remaining = ChronoUnit.DAYS.between(today, date)
            val prefix = if (remaining == 0L) "Hôm nay" else "Còn $remaining ngày"
            val body = "$prefix · ${date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} lúc ${event.hour.toString().padStart(2, '0')}:${event.minute.toString().padStart(2, '0')}"
            val notification = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
                .setContentTitle(event.title).setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(listOf(body, event.note).filter(String::isNotBlank).joinToString("\n")))
                .setContentIntent(pending).setAutoCancel(true).setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER).build()
            try { applicationContext.getSystemService(NotificationManager::class.java).notify(id.hashCode(), notification) }
            catch (_: SecurityException) { /* Permission may change between check and notify. */ }
        }
        ReminderScheduler.scheduleLocked(applicationContext, id, append = true, afterOccurrence = date)
        Result.success()
    }

    private fun canNotify(): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}
