package vn.lichviet.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri

/** One stable PendingIntent per reminder and kind; occurrence details live in extras and Room. */
internal object AlarmReminderScheduler {
    enum class Mode { EXACT, INEXACT }
    private fun pending(context: Context, id: Long, snooze: Boolean, create: Boolean, date: String? = null,
        key: String? = null, trigger: Long = 0): PendingIntent? {
        val kind = if (snooze) "snooze-fire" else "fire"
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(kind)
            .setData(Uri.parse("lichviet://alarm/$kind/$id"))
            .putExtra(ReminderReceiver.REMINDER_ID, id)
            .putExtra(ReminderReceiver.DATE, date)
            .putExtra(ReminderReceiver.KEY, key)
            .putExtra(ReminderReceiver.TRIGGER, trigger)
        return PendingIntent.getBroadcast(context, 0, intent,
            (if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE) or PendingIntent.FLAG_IMMUTABLE)
    }

    fun schedule(context: Context, id: Long, date: String, key: String, trigger: Long, snooze: Boolean = false,
        allowExact: Boolean = ReminderPermissionManager.canScheduleExact(context)): Mode {
        val manager = context.getSystemService(AlarmManager::class.java)
        val pending = pending(context, id, snooze, true, date, key, trigger)!!
        // A revoked special access can race the permission check. Never drop the reminder.
        if (allowExact) {
            try {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
                return Mode.EXACT
            } catch (_: SecurityException) { /* fall through to the permitted inexact API */ }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        return Mode.INEXACT
    }

    fun schedule(context: Context, id: Long, date: String, trigger: Long, snooze: Boolean = false,
        allowExact: Boolean = ReminderPermissionManager.canScheduleExact(context)): Mode =
        schedule(context, id, date, date, trigger, snooze, allowExact)

    fun hasPendingIntent(context: Context, id: Long, snooze: Boolean = false): Boolean =
        pending(context, id, snooze, false) != null

    fun cancel(context: Context, id: Long, snooze: Boolean = false) {
        pending(context, id, snooze, false)?.let {
            context.getSystemService(AlarmManager::class.java).cancel(it)
            it.cancel()
        }
    }
}
