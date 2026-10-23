package vn.lichviet.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Broadcast entry point verifies Room state before delivering any notification. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val id = intent.getLongExtra(REMINDER_ID, -1)
                val date = intent.getStringExtra(DATE).orEmpty()
                if (id < 1 || date.isEmpty()) return@launch
                val key = intent.getStringExtra(KEY) ?: date
                when (intent.action) {
                    "fire" -> ReminderScheduler.fire(context.applicationContext, id, date, key,
                        intent.getLongExtra(TRIGGER, -1), snooze = false)
                    "snooze-fire" -> ReminderScheduler.fire(context.applicationContext, id, date, key,
                        intent.getLongExtra(TRIGGER, -1), snooze = true)
                    ACTION_SNOOZE -> ReminderScheduler.snooze(context.applicationContext, id, date, key,
                        intent.getIntExtra(SNOOZE_MINUTES, 0))
                    ACTION_CHOICES -> ReminderScheduler.snoozeChoices(context.applicationContext, id, date, key)
                    ACTION_COMPLETE -> ReminderScheduler.complete(context.applicationContext, id, date, key)
                }
            } finally { result.finish() }
        }
    }

    companion object {
        const val REMINDER_ID = "reminder_id"
        const val DATE = "occurrence_date"
        const val KEY = "occurrence_key"
        const val TRIGGER = "trigger_millis"
        const val SNOOZE_MINUTES = "snooze_minutes"
        const val ACTION_SNOOZE = "vn.lichviet.app.SNOOZE_REMINDER"
        const val ACTION_CHOICES = "vn.lichviet.app.SNOOZE_CHOICES"
        const val ACTION_COMPLETE = "vn.lichviet.app.COMPLETE_REMINDER"
    }
}
