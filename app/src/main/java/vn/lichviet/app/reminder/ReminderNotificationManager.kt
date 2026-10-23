package vn.lichviet.app.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import vn.lichviet.app.MainActivity
import vn.lichviet.app.AppLocaleManager
import vn.lichviet.app.R
import vn.lichviet.app.ui.CalendarText
import vn.lichviet.app.data.EventReminderEntity
import vn.lichviet.app.ics.EventOccurrence
import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

internal object ReminderNotificationManager {
    const val CHANNEL_ID = "calendar_reminders"
    private fun tag(reminderId: Long, key: String) = "event-reminder-$reminderId-${key.hashCode().toUInt().toString(16)}"

    fun ensureChannel(context: Context) {
        val localized=AppLocaleManager.localizedContext(context)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, localized.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = localized.getString(R.string.reminder_channel_description)
            },
        )
    }

    fun cancel(context: Context, reminderId: Long) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel("event-reminder-$reminderId", 0)
        try {
            manager.activeNotifications.filter { it.tag?.startsWith("event-reminder-$reminderId-") == true }
                .forEach { manager.cancel(it.tag, it.id) }
        } catch (_: SecurityException) { }
    }

    fun canNotify(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE

    fun show(context: Context, occurrence: EventOccurrence, reminder: EventReminderEntity): Boolean {
        if (!canNotify(context)) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        ensureChannel(context)
        val date = occurrence.startDate
        val key = occurrence.occurrenceKey
        val open = Intent(context, MainActivity::class.java)
            .setData(Uri.parse("lichviet://reminder/${reminder.id}/${Uri.encode(key)}"))
            .putExtra("selected_date", date.toString())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val contentIntent = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val localized=AppLocaleManager.localizedContext(context)
        val days = ChronoUnit.DAYS.between(LocalDate.now(VietnameseCalendar.zoneId), date)
        val whenText = when {
            days == 0L -> localized.getString(R.string.reminder_today)
            days > 0 -> localized.resources.getQuantityString(R.plurals.reminder_days_remaining,days.toInt(),days.toInt())
            else -> localized.getString(R.string.reminder_past)
        }
        val dateText=CalendarText.shortDate(localized,date)
        val timeText=if(occurrence.allDay)localized.getString(R.string.event_all_day) else CalendarText.time(localized,occurrence.start.atZone(VietnameseCalendar.zoneId).toLocalTime())
        val body="$whenText · $dateText · $timeText"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(occurrence.title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(listOf(body, occurrence.location, occurrence.note).filter(String::isNotBlank).joinToString("\n")))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, localized.getString(R.string.reminder_snooze5), action(context, ReminderReceiver.ACTION_SNOOZE, reminder.id, date, key, 5))
            .addAction(0, localized.getString(R.string.reminder_snooze_other), action(context, ReminderReceiver.ACTION_CHOICES, reminder.id, date, key, 0))
            .addAction(0, localized.getString(R.string.reminder_done), action(context, ReminderReceiver.ACTION_COMPLETE, reminder.id, date, key, 0))
            .build()
        return try { manager.notify(tag(reminder.id, key), 0, notification); true }
        catch (_: SecurityException) { false }
    }

    fun showSnoozeChoices(context: Context, occurrence: EventOccurrence, reminder: EventReminderEntity) {
        val localized=AppLocaleManager.localizedContext(context)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        ensureChannel(context)
        val date = occurrence.startDate
        val key = occurrence.occurrenceKey
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(occurrence.title)
            .setContentText(localized.getString(R.string.reminder_snooze_question))
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, localized.getString(R.string.reminder_minutes,5), action(context, ReminderReceiver.ACTION_SNOOZE, reminder.id, date, key, 5))
            .addAction(0, localized.getString(R.string.reminder_minutes,10), action(context, ReminderReceiver.ACTION_SNOOZE, reminder.id, date, key, 10))
            .addAction(0, localized.getString(R.string.reminder_minutes,15), action(context, ReminderReceiver.ACTION_SNOOZE, reminder.id, date, key, 15))
            .build()
        try { context.getSystemService(NotificationManager::class.java).notify(tag(reminder.id, key), 0, notification) }
        catch (_: SecurityException) { }
    }

    private fun action(context: Context, action: String, id: Long, date: LocalDate, key: String, minutes: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(action)
            .setData(Uri.parse("lichviet://reminder-action/$action/$id/${Uri.encode(key)}/$minutes"))
            .putExtra(ReminderReceiver.REMINDER_ID, id)
            .putExtra(ReminderReceiver.DATE, date.toString())
            .putExtra(ReminderReceiver.KEY, key)
            .putExtra(ReminderReceiver.SNOOZE_MINUTES, minutes)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
