package vn.lichviet.app.reminder

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.lichviet.app.data.AppDatabase
import vn.lichviet.app.data.EventEntity
import vn.lichviet.app.data.EventReminderEntity
import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class AlarmReminderIntegrationTest {
    @Test fun alarmCanBeUpdatedAndCanceledWithStablePendingIntentAndFallbackMode() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val id = System.currentTimeMillis()
        val date = LocalDate.now(VietnameseCalendar.zoneId).toString()
        val first = System.currentTimeMillis() + 180_000
        try {
            val mode = AlarmReminderScheduler.schedule(context, id, date, first)
            if (!ReminderPermissionManager.canScheduleExact(context)) assertEquals(AlarmReminderScheduler.Mode.INEXACT, mode)
            assertTrue(AlarmReminderScheduler.hasPendingIntent(context, id))
            assertEquals(AlarmReminderScheduler.Mode.INEXACT,
                AlarmReminderScheduler.schedule(context, id, date, first + 60_000, allowExact = false))
            assertTrue(AlarmReminderScheduler.hasPendingIntent(context, id))
        } finally { AlarmReminderScheduler.cancel(context, id) }
        assertFalse(AlarmReminderScheduler.hasPendingIntent(context, id))
    }

    @Test fun receiverPostsOneRealNotificationAndRejectsDuplicateBroadcast() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (Build.VERSION.SDK_INT >= 33) InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        ReminderNotificationManager.ensureChannel(context)
        val db = AppDatabase.get(context)
        val date = LocalDate.now(VietnameseCalendar.zoneId)
        val eventId = db.eventDao().save(EventEntity(title = "Kiểm thử nhắc lịch", solarDate = date.toString(),
            hour = 0, minute = 0))
        val reminderId = db.reminderDao().insert(EventReminderEntity(eventId = eventId, offsetMinutes = 0))
        val trigger = System.currentTimeMillis() - 1_000
        try {
            db.reminderDao().setScheduled(reminderId, date.toString(), trigger)
            val broadcast = Intent(context, ReminderReceiver::class.java).setAction("fire")
                .setData(Uri.parse("lichviet://alarm/fire/$reminderId"))
                .putExtra(ReminderReceiver.REMINDER_ID, reminderId)
                .putExtra(ReminderReceiver.DATE, date.toString())
                .putExtra(ReminderReceiver.TRIGGER, trigger)
            context.sendBroadcast(broadcast)
            withTimeout(10_000) {
                while (db.reminderDao().getById(reminderId)?.lastDeliveredDate != date.toString()) delay(50)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            withTimeout(5_000) {
                while (manager.activeNotifications.none { it.notification.extras.getString("android.title") == "Kiểm thử nhắc lịch" }) delay(50)
            }
            val first = manager.activeNotifications.single { it.notification.extras.getString("android.title") == "Kiểm thử nhắc lịch" }
            context.sendBroadcast(broadcast)
            delay(300)
            val second = manager.activeNotifications.single { it.key == first.key }
            assertEquals(first.postTime, second.postTime)
            assertEquals(date.toString(), db.reminderDao().getById(reminderId)?.lastDeliveredDate)
        } finally {
            ReminderNotificationManager.cancel(context, reminderId)
            db.eventDao().delete(eventId)
        }
    }

    @Test fun bootAndTimezoneReceiversRebuildAlarmFromPersistedEvent() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.get(context)
        val date = LocalDate.now(VietnameseCalendar.zoneId).plusDays(1)
        val eventId = db.eventDao().save(EventEntity(title = "Khôi phục lời nhắc", solarDate = date.toString(), hour = 9))
        val reminderId = db.reminderDao().insert(EventReminderEntity(eventId = eventId, offsetMinutes = 0))
        try {
            for (action in listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIMEZONE_CHANGED)) {
                AlarmReminderScheduler.cancel(context, reminderId)
                db.reminderDao().setScheduled(reminderId, null, null)
                CalendarRefreshReceiver().onReceive(context, Intent(action))
                withTimeout(15_000) {
                    while (db.reminderDao().getById(reminderId)?.scheduledDate != date.toString()) delay(100)
                }
                assertTrue(AlarmReminderScheduler.hasPendingIntent(context, reminderId))
            }
        } finally {
            AlarmReminderScheduler.cancel(context, reminderId)
            db.eventDao().delete(eventId)
        }
    }
}
