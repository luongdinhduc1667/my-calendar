package vn.lichviet.app.ui

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lichviet.app.MainActivity
import vn.lichviet.app.LaunchPreferencesRule
import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class WidgetSelectionTest {
    @get:Rule(order=0) val launchPreferences=LaunchPreferencesRule()
    @get:Rule(order=1) val compose=createAndroidComposeRule<MainActivity>()
    @Test fun choosingAnotherDaySurvivesRecreationWithOriginalWidgetIntent() {
        val today=LocalDate.now(VietnameseCalendar.zoneId)
        val original=today.withDayOfMonth(1)
        val chosen=today.withDayOfMonth(2)
        compose.activityRule.scenario.onActivity{activity->
            activity.intent=Intent(activity.intent).putExtra("selected_date",original.toString())
        }
        compose.onNodeWithTag("date_$chosen").performScrollTo().performClick().assertIsSelected()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("date_$chosen").performScrollTo().assertIsSelected()
    }
}
