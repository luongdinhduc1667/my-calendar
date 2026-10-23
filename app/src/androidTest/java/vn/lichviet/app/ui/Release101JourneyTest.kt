package vn.lichviet.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lichviet.app.LaunchPreferencesRule
import vn.lichviet.app.MainActivity
import vn.lichviet.app.R
import vn.lichviet.app.data.PreferencesRepository

@RunWith(AndroidJUnit4::class)
class Release101JourneyTest {
    @get:Rule(order=0) val preferences=LaunchPreferencesRule()
    @get:Rule(order=1) val compose=createAndroidComposeRule<MainActivity>()

    @Test fun findCultureDayAndPersistDisplaySettingsAcrossRecreation() {
        val repository=PreferencesRepository(compose.activity)
        compose.waitUntil(10_000){compose.onAllNodesWithTag("calendar_grid").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.calendar_find_date)).performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("24112026")
        compose.onNodeWithText(compose.activity.getString(R.string.event_select_date)).performClick()
        compose.onNodeWithTag("date_2026-11-24").performScrollTo().assertIsSelected()
        val holiday=compose.activity.getString(R.string.calendar_holiday_culture_day)
        compose.onNodeWithTag("date_2026-11-24").assert(hasContentDescription(holiday,substring=true))
        compose.onNodeWithText(holiday).performScrollTo().assertIsDisplayed()

        fun openSettings() { compose.onNode(hasText("Cài đặt") and isSelectable()).performClick() }
        openSettings()
        listOf(R.string.settings_week_numbers,R.string.settings_auspicious_hours,R.string.settings_animations).forEach { id ->
            compose.onNodeWithText(compose.activity.getString(id)).performScrollTo().performClick()
        }
        compose.waitUntil(5_000){runBlocking{repository.preferences.first().let{it.showWeekNumbers&&!it.showAuspiciousHours&&!it.animationsEnabled}}}
        compose.activityRule.scenario.recreate()
        val restored=runBlocking{repository.preferences.first()}
        assertTrue(restored.showWeekNumbers);assertFalse(restored.showAuspiciousHours);assertFalse(restored.animationsEnabled)
        compose.onNode(hasText("Lịch") and isSelectable()).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.calendar_hours)).assertDoesNotExist()
        assertTrue(compose.onAllNodes(hasContentDescription("Tuần ISO",substring=true)).fetchSemanticsNodes().isNotEmpty())

        openSettings()
        compose.onNodeWithText(compose.activity.getString(R.string.settings_holidays)).performScrollTo().performClick()
        compose.waitUntil(5_000){runBlocking{!repository.preferences.first().showHolidays}}
        compose.onNode(hasText("Lịch") and isSelectable()).performClick()
        compose.onNodeWithText(holiday).assertDoesNotExist()
        compose.onNodeWithTag("date_2026-11-24").assert(hasContentDescription(holiday,substring=true).not())
    }
}
