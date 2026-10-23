package vn.lichviet.app.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Kept so already-persisted v0.1 WorkRequests deserialize safely after the upgrade. */
class EventReminderWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = Result.success()
}
