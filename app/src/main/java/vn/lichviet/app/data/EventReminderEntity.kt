package vn.lichviet.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/** A stable reminder ID survives edits to its parent event and identifies its alarm PendingIntents. */
@Entity(
    tableName = "event_reminders",
    foreignKeys = [ForeignKey(
        entity = EventEntity::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("eventId"), Index(value = ["eventId", "offsetMinutes"], unique = true)],
)
data class EventReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    /** Time before event start. An all-day event starts at 09:00 Vietnam time. */
    val offsetMinutes: Int,
    val scheduledDate: String? = null,
    val scheduledOccurrenceKey: String? = null,
    val scheduledTriggerMillis: Long? = null,
    val lastDeliveredDate: String? = null,
    val lastDeliveredOccurrenceKey: String? = null,
    val lastDeliveredTriggerMillis: Long? = null,
    val snoozeDate: String? = null,
    val snoozeOccurrenceKey: String? = null,
    val snoozeUntilMillis: Long? = null,
    val completedDate: String? = null,
    val completedOccurrenceKey: String? = null,
)

@Dao
interface EventReminderDao {
    @Query("SELECT * FROM event_reminders ORDER BY eventId, offsetMinutes")
    suspend fun getAll(): List<EventReminderEntity>

    @Query("SELECT * FROM event_reminders WHERE eventId = :eventId ORDER BY offsetMinutes")
    suspend fun forEvent(eventId: Long): List<EventReminderEntity>

    @Query("SELECT * FROM event_reminders WHERE id = :id")
    suspend fun getById(id: Long): EventReminderEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(reminder: EventReminderEntity): Long

    @Query("DELETE FROM event_reminders WHERE id = :id")
    suspend fun delete(id: Long)
    @Query("DELETE FROM event_reminders WHERE eventId = :eventId")
    suspend fun deleteForEvent(eventId: Long)

    @Query("UPDATE event_reminders SET scheduledDate = :date, scheduledOccurrenceKey = :key, scheduledTriggerMillis = :trigger WHERE id = :id")
    suspend fun setScheduled(id: Long, date: String?, key: String?, trigger: Long?)
    suspend fun setScheduled(id: Long, date: String?, trigger: Long?) = setScheduled(id, date, date, trigger)

    /** A duplicate or stale broadcast cannot claim the same occurrence twice. */
    @Query("""UPDATE event_reminders SET lastDeliveredDate = :date, lastDeliveredOccurrenceKey = :key, lastDeliveredTriggerMillis = :trigger
        WHERE id = :id AND scheduledOccurrenceKey = :key AND scheduledTriggerMillis = :trigger
        AND (lastDeliveredOccurrenceKey IS NULL OR lastDeliveredTriggerMillis IS NULL
             OR lastDeliveredOccurrenceKey != :key OR lastDeliveredTriggerMillis != :trigger)""")
    suspend fun claimDelivery(id: Long, date: String, key: String, trigger: Long): Int
    suspend fun claimDelivery(id: Long, date: String, trigger: Long): Int = claimDelivery(id, date, date, trigger)

    @Query("""UPDATE event_reminders SET lastDeliveredDate = NULL, lastDeliveredOccurrenceKey = NULL, lastDeliveredTriggerMillis = NULL
        WHERE id = :id AND scheduledOccurrenceKey = :key AND scheduledTriggerMillis = :trigger
        AND lastDeliveredOccurrenceKey = :key AND lastDeliveredTriggerMillis = :trigger""")
    suspend fun releaseDelivery(id: Long, key: String, trigger: Long)

    @Query("UPDATE event_reminders SET snoozeDate = :date, snoozeOccurrenceKey = :key, snoozeUntilMillis = :trigger WHERE id = :id")
    suspend fun setSnooze(id: Long, date: String?, key: String?, trigger: Long?)
    suspend fun setSnooze(id: Long, date: String?, trigger: Long?) = setSnooze(id, date, date, trigger)

    @Query("""UPDATE event_reminders SET snoozeDate = NULL, snoozeOccurrenceKey = NULL, snoozeUntilMillis = NULL
        WHERE id = :id AND snoozeOccurrenceKey = :key AND snoozeUntilMillis = :trigger""")
    suspend fun claimSnooze(id: Long, key: String, trigger: Long): Int

    @Query("UPDATE event_reminders SET completedDate = :date, completedOccurrenceKey = :key, snoozeDate = NULL, snoozeOccurrenceKey = NULL, snoozeUntilMillis = NULL WHERE eventId = :eventId")
    suspend fun completeOccurrence(eventId: Long, date: String, key: String)
    suspend fun completeOccurrence(eventId: Long, date: String) = completeOccurrence(eventId, date, date)

    @Query("""UPDATE event_reminders SET scheduledDate = NULL, scheduledOccurrenceKey = NULL, scheduledTriggerMillis = NULL,
        lastDeliveredDate = NULL, lastDeliveredOccurrenceKey = NULL, lastDeliveredTriggerMillis = NULL,
        snoozeDate = NULL, snoozeOccurrenceKey = NULL, snoozeUntilMillis = NULL,
        completedDate = NULL, completedOccurrenceKey = NULL WHERE eventId = :eventId""")
    suspend fun resetAfterEventEdit(eventId: Long)
}
