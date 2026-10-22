package vn.lichviet.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import vn.lichviet.app.reminder.ReminderScheduler
import vn.lichviet.app.widget.CalendarWidgets

@HiltAndroidApp
class LichVietApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CalendarWidgets.initialize(this)
        ReminderScheduler.initialize(this)
    }
}
