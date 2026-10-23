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
import vn.lichviet.app.ui.CalendarText
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ZodiacJourneyTest {
    @get:Rule(order = 0) val preferences = LaunchPreferencesRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    private fun openZodiac() {
        compose.onNode(hasText("Tử vi") and isSelectable()).performClick()
    }

    private fun scrollTo(tag: String) {
        compose.onNodeWithTag("zd_list").performScrollToNode(hasTestTag(tag))
    }

    @Test fun twelveAnimalsAllPeriodsSourcesAndRecreationAreUsable() {
        openZodiac()
        compose.onNodeWithTag("zd_animal_PIG").performScrollTo().assertExists()
        compose.onNodeWithTag("zd_animal_CAT").performScrollTo().performClick()
        compose.onNodeWithText("🐈 Mão · Mèo").assertExists()
        scrollTo("zd_reference_picker")
        compose.onNodeWithTag("zd_reference_picker").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("01042023")
        compose.onNodeWithText(compose.activity.getString(R.string.event_select_date)).performClick()
        for (period in listOf("DAY", "WEEK", "MONTH", "YEAR")) {
            scrollTo("zd_period_$period")
            compose.onNodeWithTag("zd_period_$period").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("zd_forecast_tone").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("zd_forecast_range").assertExists()
            if (period == "MONTH") compose.onNodeWithTag("zd_forecast_range").assertTextEquals(compose.activity.getString(R.string.zd_range,
                CalendarText.shortDate(compose.activity, LocalDate.of(2023, 3, 22)), CalendarText.shortDate(compose.activity, LocalDate.of(2023, 4, 19))))
        }
        compose.onNodeWithTag("zd_list").performScrollToNode(hasTestTag("zd_toggle_days"))
        compose.onNodeWithTag("zd_forecast_sources").performScrollTo().performClick()
        compose.onNodeWithText("三命通會 · Tam Mệnh Thông Hội · II").assertExists()
        compose.onNodeWithText("Đóng").performClick()
        compose.activityRule.scenario.recreate()
        scrollTo("zd_period_YEAR")
        compose.onNodeWithTag("zd_period_YEAR").assertIsSelected()
        compose.onNodeWithTag("hs_section_2").performClick()
        compose.onNodeWithTag("hs_section_0").performClick()
        scrollTo("zd_period_YEAR")
        compose.onNodeWithTag("zd_period_YEAR").assertIsSelected()
        compose.onNodeWithTag("zd_period_DAY").performClick()
        scrollTo("zd_toggle_days")
        compose.onNodeWithTag("zd_toggle_days").performClick()
        scrollTo("zd_day_2023-04-01")
        compose.onNodeWithTag("zd_day_2023-04-01").performClick()
        compose.onNodeWithTag("date_2023-04-01").performScrollTo().assertIsSelected()
    }

    @Test fun solarBirthLookupResolvesTheDayBeforeTet() {
        openZodiac()
        compose.onNodeWithTag("zd_birth_picker").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("16022026")
        compose.onNodeWithText(compose.activity.getString(R.string.event_select_date)).performClick()
        compose.onNodeWithText("🐍 Tỵ · Rắn").assertExists()
        compose.onNodeWithText("Năm âm lịch 2025 · Ất Tỵ").assertExists()
        compose.onNodeWithText("Nạp âm: Phú Đăng Hỏa · Hỏa").assertExists()
    }

    @Test fun compassRejectsInvalidInputAndDisplaysSittingBasedHouse() {
        openZodiac()
        compose.onNodeWithTag("hs_section_2").performClick()
        compose.onNodeWithTag("fs_tool_2").performScrollTo().performClick()
        compose.onNodeWithTag("fs_facing_angle").performTextReplacement("361")
        compose.onNodeWithText("Nhập góc hợp lệ từ 0 đến 360.").assertExists()
        compose.onNodeWithTag("fs_compass_result").assertDoesNotExist()
        compose.onNodeWithTag("fs_facing_angle").performTextReplacement("180")
        compose.onNodeWithTag("fs_list").performScrollToNode(hasTestTag("fs_compass_result"))
        compose.onNodeWithTag("fs_compass_result").assertTextEquals("Tọa Tý · hướng Ngọ")
        compose.onNodeWithTag("fs_list").performScrollToNode(hasTestTag("fs_direction_KAN"))
        compose.onNodeWithText("Bắc · Khảm · Phục Vị").assertExists()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("fs_list").performScrollToNode(hasTestTag("fs_compass_result"))
        compose.onNodeWithTag("fs_compass_result").assertTextEquals("Tọa Tý · hướng Ngọ")
    }
}
