package vn.lichviet.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import vn.lichviet.app.R
import vn.lichviet.app.AppLocaleManager
import vn.lichviet.app.data.AppDatabase
import vn.lichviet.app.data.EventEntity
import vn.lichviet.app.data.PreferencesRepository
import vn.lichviet.app.data.UserPreferences
import vn.lichviet.app.ics.EventOccurrence
import vn.lichviet.app.ics.EventOccurrenceEngine
import vn.lichviet.app.ui.CalendarText
import vn.lichviet.core.VietnameseCalendar
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

abstract class CalendarWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        CalendarWidgets.initialize(context)
        CalendarWidgets.requestUpdate(context)
    }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) {
        CalendarWidgets.requestUpdate(context)
    }
}
class TodayWidgetProvider : CalendarWidgetProvider()
class LunarWidgetProvider : CalendarWidgetProvider()
class MonthWidgetProvider : CalendarWidgetProvider()
class UpcomingWidgetProvider : CalendarWidgetProvider()

object CalendarWidgets {
    private val providers = listOf(TodayWidgetProvider::class.java, LunarWidgetProvider::class.java,
        MonthWidgetProvider::class.java, UpcomingWidgetProvider::class.java)

    fun initialize(context: Context) {
        val now = ZonedDateTime.now(VietnameseCalendar.zoneId)
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        val daily = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, midnight).toMillis(), TimeUnit.MILLISECONDS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("calendar-widget-daily", ExistingPeriodicWorkPolicy.KEEP, daily)
    }

    fun requestUpdate(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork("calendar-widget-update", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build())
    }

    internal suspend fun updateAll(context: Context) {
        val now = Instant.now()
        val today = now.atZone(VietnameseCalendar.zoneId).toLocalDate()
        val manager = AppWidgetManager.getInstance(context)
        val registered = providers.associateWith { manager.getAppWidgetIds(ComponentName(context, it)) }
        if (registered.values.all { it.isEmpty() }) return
        val events = AppDatabase.get(context).eventDao().getAll()
        val preferences = PreferencesRepository(context).preferences.first()
        for ((provider, ids) in registered) for (id in ids) {
            manager.updateAppWidget(id, render(context, provider, id, today, events, preferences, now))
        }
    }

    internal fun render(context: Context, provider: Class<out CalendarWidgetProvider>, id: Int,
        today: LocalDate, events: List<EventEntity>, preferences: UserPreferences, now: Instant): RemoteViews {
        val localized = AppLocaleManager.localizedContext(context)
        val views = RemoteViews(localized.packageName, R.layout.widget_calendar)
        views.setOnClickPendingIntent(R.id.widget_root, openDate(localized, id, today))
        if (today !in VietnameseCalendar.minDate..VietnameseCalendar.maxDate) {
            views.setTextViewText(R.id.widget_title, localized.getString(R.string.widget_app_title))
            views.setTextViewText(R.id.widget_day, today.dayOfMonth.toString())
            views.setTextViewText(R.id.widget_lunar, localized.getString(R.string.widget_supported_years))
            return views
        }
        val lunar = VietnameseCalendar.solarToLunar(today)
        val solarLabel = CalendarText.fullDate(localized, today)
        when (provider) {
            MonthWidgetProvider::class.java -> {
                views.setTextViewText(R.id.widget_title, CalendarText.monthYear(localized, java.time.YearMonth.from(today)).uppercase(CalendarText.locale(localized)))
                views.setViewVisibility(R.id.widget_day, View.GONE)
                views.setViewVisibility(R.id.widget_lunar, View.GONE)
                views.setViewVisibility(R.id.widget_detail, View.GONE)
                views.setViewVisibility(R.id.widget_month_grid, View.VISIBLE)
                buildMonth(localized, views, id, today, preferences, events)
            }
            UpcomingWidgetProvider::class.java -> {
                // Expand real occurrences so moved ICS exceptions and ongoing multi-day
                // events have the same title, dates and time as the in-app calendar.
                val ongoing = EventOccurrenceEngine.between(events, today, today.plusDays(1))
                    .filter { if (it.endExclusive > it.start) it.endExclusive > now else it.start >= now }
                    .groupBy { it.eventId }
                val upcoming = events.mapNotNull { event ->
                    ongoing[event.id]?.firstOrNull() ?: EventOccurrenceEngine.next(event, now.minusNanos(1))
                }.sortedWith(compareBy({ maxOf(it.start, now) }, { it.eventId })).take(4)
                views.setTextViewText(R.id.widget_title, localized.getString(R.string.widget_upcoming_title))
                views.setTextViewText(R.id.widget_day, if (upcoming.isEmpty()) localized.getString(R.string.widget_upcoming_none)
                    else localized.getString(R.string.widget_upcoming_count, upcoming.size))
                views.setTextViewText(R.id.widget_lunar, localized.getString(if (upcoming.isEmpty()) R.string.widget_upcoming_tap else R.string.widget_upcoming_categories))
                views.setTextViewText(R.id.widget_detail, upcoming.joinToString("\n\n") { occurrence ->
                    localized.getString(R.string.widget_upcoming_line, occurrenceDate(localized, occurrence),
                        if (occurrence.allDay) localized.getString(R.string.widget_all_day)
                        else CalendarText.time(localized, occurrence.start.atZone(VietnameseCalendar.zoneId).toLocalTime()), occurrence.title)
                })
                upcoming.firstOrNull()?.let { views.setOnClickPendingIntent(R.id.widget_root, openDate(localized, id, maxOf(today, it.startDate))) }
                views.setTextViewTextSize(R.id.widget_day, TypedValue.COMPLEX_UNIT_SP, 28f)
            }
            LunarWidgetProvider::class.java -> {
                views.setTextViewText(R.id.widget_title, localized.getString(R.string.widget_lunar_title, lunar.month,
                    if (lunar.isLeap) localized.getString(R.string.widget_lunar_leap) else ""))
                views.setTextViewText(R.id.widget_day, lunar.day.toString())
                views.setTextViewText(R.id.widget_lunar, localized.getString(R.string.widget_lunar_year, CalendarText.yearCanChi(localized, lunar.year)))
                views.setTextViewText(R.id.widget_detail, CalendarText.shortDate(localized, today))
                views.setInt(R.id.widget_detail, "setMaxLines", 1)
            }
            else -> {
                views.setTextViewText(R.id.widget_title, localized.getString(R.string.widget_today_title,
                    CalendarText.monthName(localized, java.time.YearMonth.from(today))))
                views.setTextViewText(R.id.widget_day, today.dayOfMonth.toString())
                views.setTextViewText(R.id.widget_lunar, if (preferences.showLunar) localized.getString(R.string.widget_today_lunar,
                    CalendarText.lunarShort(localized, lunar), CalendarText.yearCanChi(localized, lunar.year)) else solarLabel)
                val detail = if (preferences.showHolidays) CalendarText.holidays(localized, today).joinToString(" · ") else ""
                views.setTextViewText(R.id.widget_detail, detail.ifBlank { CalendarText.weekdayFull(localized, today) })
                views.setInt(R.id.widget_detail, "setMaxLines", 1)
            }
        }
        return views
    }

    private fun occurrenceDate(context: Context, occurrence: EventOccurrence): String {
        val start = CalendarText.monthDay(context, occurrence.startDate)
        val lastDate = occurrence.endDateExclusive.minusDays(1)
        return if (lastDate > occurrence.startDate) context.getString(R.string.widget_date_range,
            start, CalendarText.monthDay(context, lastDate)) else start
    }

    private fun buildMonth(context: Context, views: RemoteViews, widgetId: Int, today: LocalDate,
        prefs: UserPreferences, events: List<EventEntity>) {
        views.removeAllViews(R.id.widget_month_grid)
        val weekdays = context.resources.getStringArray(if (prefs.mondayFirst) R.array.widget_weekdays_monday else R.array.widget_weekdays_sunday)
        val header = RemoteViews(context.packageName, R.layout.widget_week)
        weekdays.forEach { label ->
            val cell = RemoteViews(context.packageName, R.layout.widget_cell)
            cell.setTextViewText(R.id.widget_cell_day, label)
            cell.setTextColor(R.id.widget_cell_day, Color.parseColor("#77776D"))
            cell.setViewVisibility(R.id.widget_cell_lunar, View.GONE)
            header.addView(R.id.widget_week_cells, cell)
        }
        views.addView(R.id.widget_month_grid, header)
        val start = today.withDayOfMonth(1).with(TemporalAdjusters.previousOrSame(if (prefs.mondayFirst) DayOfWeek.MONDAY else DayOfWeek.SUNDAY))
        val occurrences = EventOccurrenceEngine.between(events, start, start.plusDays(42))
        for (week in 0..5) {
            val row = RemoteViews(context.packageName, R.layout.widget_week)
            for (column in 0..6) {
                val date = start.plusDays((week * 7 + column).toLong())
                val cell = RemoteViews(context.packageName, R.layout.widget_cell)
                cell.setTextViewText(R.id.widget_cell_day, date.dayOfMonth.toString())
                val lunar = date.takeIf { it in VietnameseCalendar.minDate..VietnameseCalendar.maxDate }?.let(VietnameseCalendar::solarToLunar)
                cell.setTextViewText(R.id.widget_cell_lunar, if (lunar?.day == 1) "1/${lunar.month}" else lunar?.day?.toString() ?: "")
                cell.setViewVisibility(R.id.widget_cell_lunar, if (prefs.showLunar) View.VISIBLE else View.GONE)
                if (date.month != today.month) cell.setTextColor(R.id.widget_cell_day, Color.parseColor("#B0B1AA"))
                if (date == today) {
                    cell.setInt(R.id.widget_cell_day, "setBackgroundColor", Color.parseColor("#BE513B"))
                    cell.setTextColor(R.id.widget_cell_day, Color.WHITE)
                }
                val dayEvents = occurrences.filter { date >= it.startDate && date < it.endDateExclusive }
                cell.setViewVisibility(R.id.widget_cell_event, if (dayEvents.isEmpty()) View.GONE else View.VISIBLE)
                val markerColor = dayEvents.firstOrNull()?.colorArgb ?: Color.parseColor("#BE513B")
                cell.setTextColor(R.id.widget_cell_event, when {
                    date == today -> Color.WHITE
                    ColorUtils.calculateContrast(markerColor, Color.parseColor("#F5F2E9")) < 3 -> Color.parseColor("#223B32")
                    else -> markerColor
                })
                val dateDescription = lunar?.let {
                    context.getString(R.string.widget_cell_description, CalendarText.shortDate(context, date), CalendarText.lunarShort(context, it))
                } ?: CalendarText.shortDate(context, date)
                cell.setContentDescription(R.id.widget_cell_day, if (dayEvents.isEmpty()) dateDescription else
                    context.resources.getQuantityString(R.plurals.widget_cell_events_description, dayEvents.size, dateDescription, dayEvents.size))
                cell.setOnClickPendingIntent(R.id.widget_cell_day, openDate(context, widgetId * 100 + week * 7 + column, date))
                row.addView(R.id.widget_week_cells, cell)
            }
            views.addView(R.id.widget_month_grid, row)
        }
    }

    private fun openDate(context: Context, request: Int, date: LocalDate): PendingIntent = PendingIntent.getActivity(
        context, request, Intent().setClassName(context, "vn.lichviet.app.MainActivity")
            .setData(Uri.parse("lichviet://widget/$request/$date"))
            .putExtra("selected_date", date.toString()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

class WidgetUpdateWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        CalendarWidgets.updateAll(applicationContext)
        return Result.success()
    }
}
