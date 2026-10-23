package vn.lichviet.app.widget

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lichviet.app.AppLocaleManager
import vn.lichviet.app.LaunchPreferencesRule
import vn.lichviet.app.R
import vn.lichviet.app.data.EventEntity
import vn.lichviet.app.data.UserPreferences
import vn.lichviet.core.VietnameseCalendar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.atomic.AtomicReference

/** Exercise the real app language selection and RemoteViews, including ICS/multi-day data. */
@RunWith(AndroidJUnit4::class)
class WidgetLocalizationTest {
    @get:Rule val launchPreferences = LaunchPreferencesRule(language = "en")
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val date = LocalDate.of(2026, 2, 17)

    @Test fun widgetUsesAppEnglishForDatesHolidaysAndEmptyState() {
        assertEquals("en", AppLocaleManager.language(context))
        withWidget(TodayWidgetProvider::class.java) { root ->
            assertEquals("TODAY · February", root.text(R.id.widget_title))
            assertTrue(root.text(R.id.widget_lunar).startsWith("Lunar 1/1"))
            assertTrue(root.text(R.id.widget_detail).contains("Tết (Lunar New Year)"))
        }
        withWidget(UpcomingWidgetProvider::class.java) { root ->
            assertEquals("UPCOMING EVENTS", root.text(R.id.widget_title))
            assertEquals("None", root.text(R.id.widget_day))
        }
    }

    @Test fun englishUpcomingAndMonthIncludeOngoingMultiDayAndImportedOccurrences() {
        val trip = EventEntity(id = 900, title = "Family trip", solarDate = "2026-02-15",
            allDay = true, endSolarDate = "2026-02-19", colorArgb = android.graphics.Color.WHITE)
        val imported = EventEntity(id = 901, title = "Imported meeting", solarDate = "2026-02-18",
            hour = 9, icsUid = "widget-imported@example.local", icsCalendar = """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//LichViet//Widget test//EN
                BEGIN:VEVENT
                UID:widget-imported@example.local
                DTSTART:20260218T020000Z
                DTEND:20260218T030000Z
                SUMMARY:Imported meeting
                END:VEVENT
                END:VCALENDAR
            """.trimIndent().replace("\n", "\r\n") + "\r\n")
        val pastThisMorning = EventEntity(id = 902, title = "Already finished", solarDate = date.toString(), hour = 7)
        val events = listOf(trip, imported, pastThisMorning)
        withWidget(UpcomingWidgetProvider::class.java, events) { root ->
            assertEquals("2 upcoming", root.text(R.id.widget_day))
            val detail = root.text(R.id.widget_detail)
            assertTrue(detail.contains("Feb 15–Feb 18"))
            assertTrue(detail, Regex("All day\\s+Family trip").containsMatchIn(detail))
            assertTrue(detail.contains("Feb 18"))
            assertTrue(detail.contains("Imported meeting"))
            assertTrue(!detail.contains("Already finished"))
        }
        withWidget(MonthWidgetProvider::class.java, events) { root ->
            assertEquals("FEBRUARY 2026", root.text(R.id.widget_title))
            val grid = root.findViewById<LinearLayout>(R.id.widget_month_grid)
            val heading = grid.getChildAt(0) as LinearLayout
            assertEquals("Mon", heading.getChildAt(0).text(R.id.widget_cell_day))
            val start = date.withDayOfMonth(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            fun cell(day: LocalDate): View {
                val offset = ChronoUnit.DAYS.between(start, day).toInt()
                return (grid.getChildAt(offset / 7 + 1) as LinearLayout).getChildAt(offset % 7)
            }
            val eighteenth = cell(LocalDate.of(2026, 2, 18))
            assertEquals(View.VISIBLE, eighteenth.findViewById<View>(R.id.widget_cell_event).visibility)
            assertTrue(eighteenth.findViewById<View>(R.id.widget_cell_day).contentDescription.toString().endsWith("2 events"))
            // The all-day end is exclusive; the dot must disappear after the trip.
            assertEquals(View.GONE, cell(LocalDate.of(2026, 2, 19)).findViewById<View>(R.id.widget_cell_event).visibility)
        }
    }

    private fun withWidget(provider: Class<out CalendarWidgetProvider>, events: List<EventEntity> = emptyList(), assertions: (View) -> Unit) {
        val result = AtomicReference<Result<Unit>>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            result.set(runCatching {
                val root = CalendarWidgets.render(context, provider, 41, date, events, UserPreferences(),
                    date.atTime(8, 0).atZone(VietnameseCalendar.zoneId).toInstant())
                    .apply(context, FrameLayout(context))
                val density = context.resources.displayMetrics.density
                root.measure(View.MeasureSpec.makeMeasureSpec((400 * density).toInt(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec((420 * density).toInt(), View.MeasureSpec.EXACTLY))
                root.layout(0, 0, root.measuredWidth, root.measuredHeight)
                assertions(root)
            })
        }
        result.get().getOrThrow()
    }

    private fun View.text(id: Int): String = findViewById<TextView>(id).text.toString()
}
