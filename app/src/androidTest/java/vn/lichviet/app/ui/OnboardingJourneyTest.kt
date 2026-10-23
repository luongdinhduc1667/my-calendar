package vn.lichviet.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lichviet.app.AppLocaleManager
import vn.lichviet.app.LaunchPreferencesRule
import vn.lichviet.app.MainActivity
import vn.lichviet.app.data.PreferencesRepository

@RunWith(AndroidJUnit4::class)
class OnboardingJourneyTest {
    @get:Rule(order=0) val launchPreferences=LaunchPreferencesRule(completed=false)
    @get:Rule(order=1) val compose=createAndroidComposeRule<MainActivity>()

    private fun waitForCalendar() {
        compose.waitUntil(10_000){compose.onAllNodesWithTag("calendar_grid").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("calendar_grid").performScrollTo().assertIsDisplayed()
    }

    @Test fun skipIsPersistedAndDoesNotReturnAfterActivityRecreation() {
        compose.waitUntil(10_000){compose.onAllNodesWithText("Lịch Việt – Mỗi ngày một ý nghĩa").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Lịch Việt – Mỗi ngày một ý nghĩa").assertIsDisplayed()
        compose.onNodeWithText("Bỏ qua").performClick()
        waitForCalendar()
        assertTrue(runBlocking { PreferencesRepository(compose.activity).onboardingCompleted.first() })
        compose.activityRule.scenario.recreate()
        waitForCalendar()
        compose.onNodeWithText("Bỏ qua").assertDoesNotExist()
    }

    @Test fun languageChangeKeepsPersonalizationPageAndPersistsAfterRestart() {
        compose.waitUntil(10_000){compose.onAllNodesWithText("Tiếp tục").fetchSemanticsNodes().isNotEmpty()}
        repeat(2) { compose.onNodeWithText("Tiếp tục").performClick();compose.waitForIdle() }
        compose.onNodeWithText("Quay lại").performClick()
        compose.waitForIdle()
        repeat(2) { compose.onNodeWithText("Tiếp tục").performClick();compose.waitForIdle() }
        compose.onNodeWithText("English").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Get started").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Get started").performClick()
        waitForCalendar()
        assertEquals("en",AppLocaleManager.language(compose.activity))
        compose.onNode(hasText("Settings") and isSelectable()).performClick()
        compose.onNodeWithText("View introduction again").performScrollTo().performClick()
        compose.onNodeWithText("Lịch Việt – Meaning in every day").assertIsDisplayed()
        compose.onNodeWithText("Skip").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNode(hasText("Settings") and isSelectable()).assertIsDisplayed()
        assertEquals("en",AppLocaleManager.language(compose.activity))
    }
}
