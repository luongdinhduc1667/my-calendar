package vn.lichviet.app.data

import vn.lichviet.core.LunarDate
import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Pure recurrence calculation. All lunar dates use Vietnam's UTC+7 calendar. */
object Recurrence {
    fun occursOn(event: EventEntity, date: LocalDate): Boolean =
        nextOccurrence(event, date) == date

    fun nextOccurrence(event: EventEntity, fromInclusive: LocalDate): LocalDate? {
        val anchor = runCatching { LocalDate.parse(event.solarDate) }.getOrNull() ?: return null
        if (anchor !in VietnameseCalendar.minDate..VietnameseCalendar.maxDate) return null
        val from = maxOf(fromInclusive, anchor, VietnameseCalendar.minDate)
        if (from > VietnameseCalendar.maxDate) return null
        val result = when (event.repeat) {
            RepeatRule.NONE -> anchor.takeIf { it >= from }
            RepeatRule.DAILY -> from
            RepeatRule.WEEKLY -> {
                val elapsed = ChronoUnit.DAYS.between(anchor, from)
                anchor.plusWeeks((elapsed + 6) / 7)
            }
            RepeatRule.MONTHLY -> {
                var month = YearMonth.from(from)
                var candidate: LocalDate? = null
                while (month.year <= VietnameseCalendar.maxDate.year) {
                    val day = adjustedDay(anchor.dayOfMonth, month.lengthOfMonth(), event.missingDayPolicy)
                    val possible = day?.let(month::atDay)
                    if (possible != null && possible >= from) { candidate = possible; break }
                    month = month.plusMonths(1)
                }
                candidate
            }
            RepeatRule.YEARLY -> {
                (from.year..VietnameseCalendar.maxDate.year).firstNotNullOfOrNull { year ->
                    val month = YearMonth.of(year, anchor.month)
                    adjustedDay(anchor.dayOfMonth, month.lengthOfMonth(), event.missingDayPolicy)
                        ?.let(month::atDay)?.takeIf { it >= from }
                }
            }
            RepeatRule.LUNAR_YEARLY -> {
                val original = VietnameseCalendar.solarToLunar(anchor)
                val day = event.lunarDay ?: original.day
                val month = event.lunarMonth ?: original.month
                if (day !in 1..30 || month !in 1..12) return null
                val firstYear = VietnameseCalendar.solarToLunar(from).year
                (firstYear..VietnameseCalendar.maxDate.year).firstNotNullOfOrNull { year ->
                    val leapChoices = when (event.leapMonthPolicy) {
                        LeapMonthPolicy.REGULAR_ONLY -> listOf(false)
                        LeapMonthPolicy.LEAP_ONLY -> listOf(true)
                        LeapMonthPolicy.BOTH -> listOf(false, true)
                    }
                    leapChoices.mapNotNull { leap ->
                        val requested = LunarDate(day, month, year, leap)
                        (VietnameseCalendar.lunarToSolar(requested)
                            ?: if (day == 30 && event.missingDayPolicy == MissingDayPolicy.LAST_DAY)
                                VietnameseCalendar.lunarToSolar(requested.copy(day = 29)) else null)
                            ?.takeIf { it >= from }
                    }.minOrNull()
                }
            }
        }
        return result?.takeIf { it <= VietnameseCalendar.maxDate }
    }

    private fun adjustedDay(day: Int, length: Int, policy: MissingDayPolicy): Int? =
        if (day <= length) day else if (policy == MissingDayPolicy.LAST_DAY) length else null
}
