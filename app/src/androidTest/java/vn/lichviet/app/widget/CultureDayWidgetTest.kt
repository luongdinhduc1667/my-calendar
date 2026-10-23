package vn.lichviet.app.widget

import android.content.Context
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lichviet.app.LaunchPreferencesRule
import vn.lichviet.app.R
import vn.lichviet.app.data.UserPreferences
import vn.lichviet.app.ui.CalendarText
import vn.lichviet.core.HolidayId
import vn.lichviet.core.HolidayOccurrenceId
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class CultureDayWidgetTest {
    @get:Rule val preferences=LaunchPreferencesRule(language="en")
    @Test fun cultureDayUsesTheAppLanguageAndReachesTheRealTodayWidget() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val localized=vn.lichviet.app.AppLocaleManager.localizedContext(context)
        assertEquals("Vietnamese Culture Day",CalendarText.holiday(localized,HolidayOccurrenceId(HolidayId.VIETNAMESE_CULTURE_DAY)))
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val day=LocalDate.of(2026,11,24)
            val view=CalendarWidgets.render(context,TodayWidgetProvider::class.java,101,day,emptyList(),UserPreferences(),
                now=day.atTime(12,0).atZone(vn.lichviet.core.VietnameseCalendar.zoneId).toInstant())
                .apply(context,FrameLayout(context))
            assertTrue(view.findViewById<TextView>(R.id.widget_detail).text.toString().contains("Vietnamese Culture Day"))
        }
    }
}
