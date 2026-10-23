package vn.lichviet.app.ui

import android.content.Context
import vn.lichviet.app.R
import vn.lichviet.core.AlmanacReason
import vn.lichviet.core.AlmanacReasonId
import vn.lichviet.core.AssessmentLabelId
import vn.lichviet.core.AuspiciousHourId
import vn.lichviet.core.CanChiId
import vn.lichviet.core.HolidayId
import vn.lichviet.core.HolidayOccurrenceId
import vn.lichviet.core.LunarDate
import vn.lichviet.core.OfficerId
import vn.lichviet.core.PurposeId
import vn.lichviet.core.SolarTermId
import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Android presentation for stable core IDs. Never infer meaning from a Vietnamese label. */
object CalendarText {
    fun locale(context: Context): Locale = context.resources.configuration.locales[0]
    private fun array(context: Context, id: Int, index: Int) = context.resources.getStringArray(id)[index]
    private fun formatter(context: Context, pattern: Int) =
        DateTimeFormatter.ofPattern(context.getString(pattern), locale(context))

    fun shortDate(context: Context, date: LocalDate): String = date.format(formatter(context, R.string.calendar_short_date_pattern))

    fun fullDate(context: Context, date: LocalDate): String = date.format(formatter(context, R.string.calendar_full_date_pattern))
        .replaceFirstChar { it.titlecase(locale(context)) }

    fun monthDay(context: Context, date: LocalDate): String = date.format(formatter(context, R.string.calendar_month_day_pattern))

    fun monthYear(context: Context, month: YearMonth): String = month.format(formatter(context, R.string.calendar_month_year_pattern))

    fun monthName(context: Context, month: YearMonth): String = month.format(formatter(context, R.string.calendar_month_name_pattern))

    /** Follow both the app language and the user's system 12/24-hour preference. */
    fun time(context: Context, time: LocalTime): String {
        val skeleton = if (android.text.format.DateFormat.is24HourFormat(context)) "Hm" else "hm"
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale(context), skeleton)
        return time.format(DateTimeFormatter.ofPattern(pattern, locale(context)))
    }

    fun weekdayShort(context: Context, date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("EEE", locale(context)))
    fun weekdayFull(context: Context, date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("EEEE", locale(context)))
        .replaceFirstChar { it.titlecase(locale(context)) }

    fun lunarShort(context: Context, lunar: LunarDate): String = context.getString(R.string.calendar_lunar_short,
        lunar.day, lunar.month, if (lunar.isLeap) context.getString(R.string.calendar_leap_suffix) else "")

    fun lunarFull(context: Context, lunar: LunarDate): String = context.getString(R.string.calendar_lunar_full,
        lunar.day, lunar.month, if (lunar.isLeap) context.getString(R.string.calendar_leap_suffix) else "",
        canChi(context, VietnameseCalendar.yearCanChiId(lunar.year)))

    fun canChi(context: Context, id: CanChiId): String = context.getString(R.string.calendar_can_chi,
        array(context, R.array.calendar_stems, id.stemIndex), array(context, R.array.calendar_branches, id.branchIndex),
        if (id.isLeapMonth) context.getString(R.string.calendar_leap_parenthesized) else "")

    fun dayCanChi(context: Context, date: LocalDate): String = canChi(context, VietnameseCalendar.dayCanChiId(date))
    fun monthCanChi(context: Context, lunar: LunarDate): String = canChi(context, VietnameseCalendar.monthCanChiId(lunar))
    fun yearCanChi(context: Context, year: Int): String = canChi(context, VietnameseCalendar.yearCanChiId(year))

    fun solarTerm(context: Context, id: SolarTermId): String = array(context, R.array.calendar_solar_terms, id.ordinal)
    fun solarTerm(context: Context, date: LocalDate): String = solarTerm(context, VietnameseCalendar.solarTermId(date))

    fun auspiciousHour(context: Context, id: AuspiciousHourId): String = context.getString(R.string.calendar_auspicious_hour,
        array(context, R.array.calendar_branches, id.branchIndex), id.fromHour, id.toHour)
    fun auspiciousHours(context: Context, date: LocalDate): List<String> =
        VietnameseCalendar.auspiciousHourIds(date).map { auspiciousHour(context, it) }

    fun holiday(context: Context, occurrence: HolidayOccurrenceId): String {
        val id = when (occurrence.id) {
            HolidayId.NEW_YEAR -> R.string.calendar_holiday_new_year
            HolidayId.VALENTINES_DAY -> R.string.calendar_holiday_valentines
            HolidayId.INTERNATIONAL_WOMENS_DAY -> R.string.calendar_holiday_international_womens
            HolidayId.REUNIFICATION_DAY -> R.string.calendar_holiday_reunification
            HolidayId.LABOUR_DAY -> R.string.calendar_holiday_labour
            HolidayId.NATIONAL_DAY -> R.string.calendar_holiday_national
            HolidayId.VIETNAMESE_WOMENS_DAY -> R.string.calendar_holiday_vietnamese_womens
            HolidayId.TEACHERS_DAY -> R.string.calendar_holiday_teachers
            HolidayId.CHRISTMAS -> R.string.calendar_holiday_christmas
            HolidayId.TET -> R.string.calendar_holiday_tet
            HolidayId.TET_SECOND_DAY -> R.string.calendar_holiday_tet_second
            HolidayId.TET_THIRD_DAY -> R.string.calendar_holiday_tet_third
            HolidayId.LANTERN_FESTIVAL -> R.string.calendar_holiday_lantern
            HolidayId.COLD_FOOD_FESTIVAL -> R.string.calendar_holiday_cold_food
            HolidayId.HUNG_KINGS_DAY -> R.string.calendar_holiday_hung_kings
            HolidayId.VESAK -> R.string.calendar_holiday_vesak
            HolidayId.DRAGON_BOAT_FESTIVAL -> R.string.calendar_holiday_dragon_boat
            HolidayId.VU_LAN -> R.string.calendar_holiday_vu_lan
            HolidayId.MID_AUTUMN_FESTIVAL -> R.string.calendar_holiday_mid_autumn
            HolidayId.KITCHEN_GODS_DAY -> R.string.calendar_holiday_kitchen_gods
            HolidayId.FIRST_LUNAR_DAY -> R.string.calendar_holiday_first_lunar
            HolidayId.FULL_MOON_DAY -> R.string.calendar_holiday_full_moon
            HolidayId.NEW_YEARS_EVE -> R.string.calendar_holiday_new_years_eve
            HolidayId.VIETNAMESE_CULTURE_DAY -> R.string.calendar_holiday_culture_day
        }
        val name = context.getString(id)
        return if (occurrence.isLeapMonth) context.getString(R.string.calendar_holiday_in_leap_month, name) else name
    }
    fun holidays(context: Context, date: LocalDate): List<String> = VietnameseCalendar.holidayIds(date).map { holiday(context, it) }

    fun purpose(context: Context, id: PurposeId): String = context.getString(when (id) {
        PurposeId.OPENING -> R.string.calendar_purpose_opening
        PurposeId.WEDDING -> R.string.calendar_purpose_wedding
        PurposeId.MOVING -> R.string.calendar_purpose_moving
        PurposeId.TRAVEL -> R.string.calendar_purpose_travel
    })
    fun officer(context: Context, id: OfficerId): String = array(context, R.array.calendar_officers, id.ordinal)
    private fun purposeInSentence(context: Context, id: PurposeId): String =
        purpose(context, id).replaceFirstChar { it.lowercase(locale(context)) }
    fun assessmentLabel(context: Context, id: AssessmentLabelId): String = context.getString(when (id) {
        AssessmentLabelId.CANDIDATE -> R.string.calendar_assessment_candidate
        AssessmentLabelId.CONSIDER -> R.string.calendar_assessment_consider
    })
    fun assessmentReason(context: Context, reason: AlmanacReason): String = when (reason.id) {
        AlmanacReasonId.AUSPICIOUS_DAY -> context.getString(R.string.calendar_reason_auspicious)
        AlmanacReasonId.ORDINARY_DAY -> context.getString(R.string.calendar_reason_ordinary)
        AlmanacReasonId.PREFERRED_OFFICER -> context.getString(R.string.calendar_reason_preferred_officer,
            officer(context, requireNotNull(reason.officer)), purposeInSentence(context, requireNotNull(reason.purpose)))
        AlmanacReasonId.OTHER_OFFICER -> context.getString(R.string.calendar_reason_other_officer,
            officer(context, requireNotNull(reason.officer)), purposeInSentence(context, requireNotNull(reason.purpose)))
        AlmanacReasonId.TAM_NUONG -> context.getString(R.string.calendar_reason_tam_nuong)
        AlmanacReasonId.NGUYET_KY -> context.getString(R.string.calendar_reason_nguyet_ky)
        AlmanacReasonId.CULTURAL_REFERENCE -> context.getString(R.string.calendar_reason_cultural)
    }
}
