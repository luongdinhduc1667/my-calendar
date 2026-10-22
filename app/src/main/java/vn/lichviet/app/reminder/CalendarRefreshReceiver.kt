package vn.lichviet.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import vn.lichviet.app.data.AppDatabase
import vn.lichviet.app.widget.CalendarWidgets

/** Recalculate wall-clock reminders after reboot or a clock/time-zone change. */
class CalendarRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_DATE_CHANGED,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) {
            ReminderScheduler.initialize(context)
            CalendarWidgets.requestUpdate(context)
        }
    }
}

class CalendarRefreshWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        AppDatabase.get(applicationContext).eventDao().getAll().forEach {
            ReminderScheduler.schedule(applicationContext, it.id)
        }
        CalendarWidgets.requestUpdate(applicationContext)
        return Result.success()
    }
}
