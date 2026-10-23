package vn.lichviet.app.widget

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith
import vn.lichviet.app.R
import vn.lichviet.app.AppLocaleManager
import vn.lichviet.app.LaunchPreferencesRule
import vn.lichviet.app.ui.CalendarText
import vn.lichviet.app.data.AppDatabase
import vn.lichviet.app.data.EventEntity
import vn.lichviet.app.data.UserPreferences
import vn.lichviet.core.VietnameseCalendar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/** Apply the production RemoteViews through Android to catch unsupported remote method calls. */
@RunWith(AndroidJUnit4::class)
class WidgetRenderingTest {
    @get:Rule val launchPreferences = LaunchPreferencesRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val today = LocalDate.now(VietnameseCalendar.zoneId)

    @Test fun todayWidgetAppliesWithCurrentSolarAndLunarDate() {
        withWidget(TodayWidgetProvider::class.java) { root ->
            val lunar = VietnameseCalendar.solarToLunar(today)
            assertEquals(today.dayOfMonth.toString(), root.text(R.id.widget_day))
            assertTrue(root.text(R.id.widget_lunar).contains(lunar.shortLabel))
        }
    }

    @Test fun lunarWidgetAppliesWithActualLunarDayAndYear() {
        withWidget(LunarWidgetProvider::class.java) { root ->
            val lunar = VietnameseCalendar.solarToLunar(today)
            assertEquals(lunar.day.toString(), root.text(R.id.widget_day))
            assertTrue(root.text(R.id.widget_title).contains("THÁNG ${lunar.month}"))
            assertTrue(root.text(R.id.widget_lunar).contains(VietnameseCalendar.yearCanChi(lunar.year)))
        }
    }

    @Test fun monthWidgetAppliesAllSevenRowsWithCurrentDayLunarValue() {
        withWidget(MonthWidgetProvider::class.java) { root ->
            assertTrue(root.text(R.id.widget_title).contains("${today.monthValue} / ${today.year}"))
            val grid = root.findViewById<LinearLayout>(R.id.widget_month_grid)
            assertEquals(View.VISIBLE, grid.visibility)
            assertEquals(7, grid.childCount) // Weekday labels and six calendar weeks.
            val start = today.withDayOfMonth(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val index = ChronoUnit.DAYS.between(start, today).toInt()
            val row = grid.getChildAt(index / 7 + 1) as LinearLayout
            assertEquals(7, row.childCount)
            val cell = row.getChildAt(index % 7)
            val lunar = VietnameseCalendar.solarToLunar(today)
            assertEquals(today.dayOfMonth.toString(), cell.text(R.id.widget_cell_day))
            assertEquals(if (lunar.day == 1) "1/${lunar.month}" else lunar.day.toString(), cell.text(R.id.widget_cell_lunar))
            assertTrue(cell.measuredHeight > 0)
        }
    }

    @Test fun upcomingWidgetAppliesAndShowsAnEventReadFromRoom() = runBlocking {
        val databaseName = "widget-rendering-${UUID.randomUUID()}.db"
        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
        try {
            val id = database.eventDao().save(EventEntity(title = "Hẹn gia đình từ Room", solarDate = today.toString(), hour = 9, minute = 15))
            val stored = requireNotNull(database.eventDao().getById(id))
            assertTrue(stored.id > 0)
            withWidget(UpcomingWidgetProvider::class.java, listOf(stored)) { root ->
                assertEquals("1 lịch hẹn", root.text(R.id.widget_day))
                assertTrue(root.text(R.id.widget_detail).contains(stored.title))
                assertTrue(root.text(R.id.widget_detail).contains(CalendarText.time(AppLocaleManager.localizedContext(context), LocalTime.of(9, 15))))
            }
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun withWidget(provider: Class<out CalendarWidgetProvider>, events: List<EventEntity> = emptyList(), assertions: (View) -> Unit) {
        val result = AtomicReference<Result<Unit>>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            result.set(runCatching {
                val remoteViews = CalendarWidgets.render(context, provider, 40, today, events, UserPreferences(),
                    today.atTime(8, 0).atZone(VietnameseCalendar.zoneId).toInstant())
                val root = remoteViews.apply(context, FrameLayout(context))
                val width = (400 * context.resources.displayMetrics.density).toInt()
                val height = (420 * context.resources.displayMetrics.density).toInt()
                root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                root.layout(0, 0, width, height)
                assertions(root)
            })
        }
        result.get().getOrThrow()
    }

    private fun View.text(id: Int): String = findViewById<TextView>(id).text.toString()
}
