package vn.lichviet.app.zodiac

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lichviet.app.LaunchPreferencesRule
import vn.lichviet.app.MainActivity
import vn.lichviet.app.R

@RunWith(AndroidJUnit4::class)
class ZodiacLocalizationTest {
    @get:Rule(order = 0) val preferences = LaunchPreferencesRule(language = "en")
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun englishUsesVietnameseCatAndBuffaloAndLocalisedPeriodLabels() {
        compose.onNode(hasText(compose.activity.getString(R.string.nav_horoscope)) and isSelectable()).performClick()
        compose.onNodeWithTag("zd_animal_BUFFALO").assert(hasText("Buffalo", substring = true))
        compose.onNodeWithTag("zd_animal_CAT").performClick()
        compose.onNodeWithTag("zd_list").performScrollToNode(hasTestTag("zd_period_MONTH"))
        compose.onNodeWithTag("zd_period_MONTH").assertTextEquals("Month").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("zd_forecast_tone").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("zd_toggle_days").performScrollTo().assertTextEquals("Show daily table")
    }
}
