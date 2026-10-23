package vn.lichviet.app.reminder

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import vn.lichviet.app.widget.CalendarWidgets

/** System changes invalidate wall-clock alarms; Room is the durable source for restoration. */
class CalendarRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> ReminderScheduler.requestRefresh(context)
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                // The user may already have revoked the permission again.
                if (ReminderPermissionManager.canScheduleExact(context)) ReminderScheduler.requestRefresh(context)
            }
            else -> return
        }
        CalendarWidgets.requestUpdate(context)
    }
}

class CalendarRefreshWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        ReminderScheduler.rescheduleAll(applicationContext)
        CalendarWidgets.requestUpdate(applicationContext)
        Result.success()
    } catch (_: Exception) { Result.retry() }
}
