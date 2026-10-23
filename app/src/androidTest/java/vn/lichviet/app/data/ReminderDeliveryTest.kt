package vn.lichviet.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderDeliveryTest {
    @Test fun firstDeliveryClaimsExactlyOnceAndOldAlarmCannotClaimAfterUpdate() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val eventId = db.eventDao().save(EventEntity(title = "Hẹn", solarDate = "2026-10-23"))
            val id = db.reminderDao().insert(EventReminderEntity(eventId = eventId, offsetMinutes = 10))
            val dao = db.reminderDao()
            dao.setScheduled(id, "2026-10-23", 1_000L)
            assertEquals(1, dao.claimDelivery(id, "2026-10-23", 1_000L))
            assertEquals(0, dao.claimDelivery(id, "2026-10-23", 1_000L))
            dao.setScheduled(id, "2026-10-24", 2_000L)
            assertEquals(0, dao.claimDelivery(id, "2026-10-23", 1_000L))
            assertEquals(1, dao.claimDelivery(id, "2026-10-24", 2_000L))
            dao.setSnooze(id, "2026-10-24", 3_000L)
            assertEquals(1, dao.claimSnooze(id, "2026-10-24", 3_000L))
            assertEquals(0, dao.claimSnooze(id, "2026-10-24", 3_000L))
        } finally { db.close() }
    }
}
