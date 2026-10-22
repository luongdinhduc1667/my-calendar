package vn.lichviet.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

enum class RepeatRule { NONE, DAILY, WEEKLY, MONTHLY, YEARLY, LUNAR_YEARLY }
enum class EventType { EVENT, BIRTHDAY, DEATH_ANNIVERSARY }
enum class LeapMonthPolicy { REGULAR_ONLY, LEAP_ONLY, BOTH }
enum class MissingDayPolicy { LAST_DAY, SKIP }

/** solarDate is the inclusive start date, in ISO yyyy-MM-dd format. */
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val solarDate: String,
    val hour: Int = 9,
    val minute: Int = 0,
    val repeat: RepeatRule = RepeatRule.NONE,
    val type: EventType = EventType.EVENT,
    val reminderDaysBefore: Int = 0,
    val reminderEnabled: Boolean = true,
    val lunarDay: Int? = null,
    val lunarMonth: Int? = null,
    val leapMonthPolicy: LeapMonthPolicy = LeapMonthPolicy.REGULAR_ONLY,
    val missingDayPolicy: MissingDayPolicy = MissingDayPolicy.LAST_DAY,
)

data class EventOccurrence(val event: EventEntity, val date: LocalDate) {
    val dateTime: LocalDateTime get() = date.atTime(event.hour, event.minute)
}
