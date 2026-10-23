package vn.lichviet.app.reminder

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/** Special app access is optional: denied devices receive an inexact alarm. */
object ReminderPermissionManager {
    fun canScheduleExact(context: Context): Boolean = Build.VERSION.SDK_INT < 31 ||
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun exactAlarmSettingsIntent(context: Context): Intent? = if (Build.VERSION.SDK_INT >= 31)
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
    else null
}
