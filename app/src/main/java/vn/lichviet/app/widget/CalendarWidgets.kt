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
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import vn.lichviet.app.R
import vn.lichviet.app.data.AppDatabase
import vn.lichviet.app.data.EventEntity
import vn.lichviet.app.data.PreferencesRepository
import vn.lichviet.app.data.Recurrence
import vn.lichviet.app.data.UserPreferences
import vn.lichviet.core.VietnameseCalendar
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
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
        val today = LocalDate.now(VietnameseCalendar.zoneId)
        val manager = AppWidgetManager.getInstance(context)
        val registered = providers.associateWith { manager.getAppWidgetIds(ComponentName(context, it)) }
        if (registered.values.all { it.isEmpty() }) return
        val events = AppDatabase.get(context).eventDao().getAll()
        val preferences = PreferencesRepository(context).preferences.first()
        for ((provider, ids) in registered) for (id in ids) {
            manager.updateAppWidget(id, render(context, provider, id, today, events, preferences))
        }
    }

    internal fun render(context: Context, provider: Class<out CalendarWidgetProvider>, id: Int,
        today: LocalDate, events: List<EventEntity>, preferences: UserPreferences): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_calendar)
        views.setOnClickPendingIntent(R.id.widget_root, openDate(context, id, today))
        if (today !in VietnameseCalendar.minDate..VietnameseCalendar.maxDate) {
            views.setTextViewText(R.id.widget_title, "LỊCH VIỆT")
            views.setTextViewText(R.id.widget_day, today.dayOfMonth.toString())
            views.setTextViewText(R.id.widget_lunar, "Hỗ trợ lịch âm năm 1900–2100")
            return views
        }
        val lunar = VietnameseCalendar.solarToLunar(today)
        val solarLabel = today.format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale.forLanguageTag("vi-VN")))
        when (provider) {
            MonthWidgetProvider::class.java -> {
                views.setTextViewText(R.id.widget_title, "THÁNG ${today.monthValue} / ${today.year}")
                views.setViewVisibility(R.id.widget_day, View.GONE)
                views.setViewVisibility(R.id.widget_lunar, View.GONE)
                views.setViewVisibility(R.id.widget_detail, View.GONE)
                views.setViewVisibility(R.id.widget_month_grid, View.VISIBLE)
                buildMonth(context, views, id, today, preferences)
            }
            UpcomingWidgetProvider::class.java -> {
                val upcoming = events.mapNotNull { event -> Recurrence.nextOccurrence(event, today)?.let { event to it } }
                    .sortedWith(compareBy({ it.second }, { it.first.hour }, { it.first.minute })).take(4)
                views.setTextViewText(R.id.widget_title, "LỊCH HẸN SẮP TỚI")
                views.setTextViewText(R.id.widget_day, if (upcoming.isEmpty()) "Chưa có" else "${upcoming.size} lịch hẹn")
                views.setTextViewText(R.id.widget_lunar, if (upcoming.isEmpty()) "Chạm để thêm sự kiện" else "Sinh nhật · Ngày giỗ · Sự kiện")
                views.setTextViewText(R.id.widget_detail, upcoming.joinToString("\n\n") { (event, date) ->
                    "${date.format(DateTimeFormatter.ofPattern("dd/MM"))}  ${event.hour.toString().padStart(2, '0')}:${event.minute.toString().padStart(2, '0')}  ${event.title}"
                })
                upcoming.firstOrNull()?.let { views.setOnClickPendingIntent(R.id.widget_root, openDate(context, id, it.second)) }
                views.setTextViewTextSize(R.id.widget_day, TypedValue.COMPLEX_UNIT_SP, 28f)
            }
            LunarWidgetProvider::class.java -> {
                views.setTextViewText(R.id.widget_title, "ÂM LỊCH · THÁNG ${lunar.month}${if (lunar.isLeap) " NHUẬN" else ""}")
                views.setTextViewText(R.id.widget_day, lunar.day.toString())
                views.setTextViewText(R.id.widget_lunar, "Năm ${VietnameseCalendar.yearCanChi(lunar.year)}")
                views.setTextViewText(R.id.widget_detail, today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                views.setInt(R.id.widget_detail, "setMaxLines", 1)
            }
            else -> {
                views.setTextViewText(R.id.widget_title, "HÔM NAY · THÁNG ${today.monthValue}")
                views.setTextViewText(R.id.widget_day, today.dayOfMonth.toString())
                views.setTextViewText(R.id.widget_lunar, if (preferences.showLunar) "Âm lịch ${lunar.shortLabel} · ${VietnameseCalendar.yearCanChi(lunar.year)}" else solarLabel)
                val detail = if (preferences.showHolidays) VietnameseCalendar.holidays(today).joinToString(" · ") else ""
                views.setTextViewText(R.id.widget_detail, detail.ifBlank { today.format(DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("vi-VN"))) })
                views.setInt(R.id.widget_detail, "setMaxLines", 1)
            }
        }
        return views
    }

    private fun buildMonth(context: Context, views: RemoteViews, widgetId: Int, today: LocalDate, prefs: UserPreferences) {
        views.removeAllViews(R.id.widget_month_grid)
        val weekdays = if (prefs.mondayFirst) listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
            else listOf("CN", "T2", "T3", "T4", "T5", "T6", "T7")
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
                cell.setContentDescription(R.id.widget_cell_day, "${date.dayOfMonth}/${date.monthValue}/${date.year}${lunar?.let { ", âm lịch ${it.shortLabel}" } ?: ""}")
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
