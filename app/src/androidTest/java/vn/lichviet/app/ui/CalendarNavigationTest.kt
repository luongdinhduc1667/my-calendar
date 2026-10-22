package vn.lichviet.app.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lichviet.app.MainActivity
import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate
import java.time.YearMonth

/** User journeys shared by the phone navigation bar and the tablet navigation rail. */
@RunWith(AndroidJUnit4::class)
class CalendarNavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun startup_displaysCalendarAndLunarDate() {
        compose.onNodeWithTag("calendar_grid").performScrollTo().assertIsDisplayed()

        val today = LocalDate.now(VietnameseCalendar.zoneId)
        compose.onNodeWithTag("date_$today")
            .performScrollTo()
            .assertIsDisplayed()
            .assert(hasContentDescription("âm lịch", substring = true))
    }

    @Test
    fun nextMonth_changesDisplayedMonth_andPreviousReturns() {
        compose.onNodeWithTag("next_month").performScrollTo().assertIsDisplayed()
        val before = displayedMonth()

        compose.onNodeWithTag("next_month").performClick()
        assertDisplayedMonth(before.plusMonths(1))

        compose.onNodeWithTag("previous_month").performClick()
        assertDisplayedMonth(before)
        compose.onNodeWithTag("calendar_grid").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun primaryDestinations_openTheirScreens_andReturnToCalendar() {
        listOf(
            "Ngày tốt" to "Tìm ngày phù hợp",
            "Tử vi" to "Khám phá tuổi của bạn",
            "Sự kiện" to "Những ngày đáng nhớ",
            "Cài đặt" to "Theo cách của bạn",
        ).forEach { (destination, title) ->
            navigationItem(destination).performClick()
            heading(title).performScrollTo().assertIsDisplayed()
        }

        navigationItem("Lịch").performClick()
        compose.onNodeWithTag("calendar_grid").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun converterQuickAction_opensConversion_andBackReturnsToCalendar() {
        compose.onNodeWithText("Đổi âm dương").performScrollTo().performClick()
        heading("Đổi ngày âm dương").assertIsDisplayed()
        compose.onNodeWithText("Dương → Âm").assertIsDisplayed()
        compose.onNodeWithText("Âm → Dương").assertIsDisplayed()

        compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.onNodeWithTag("calendar_grid").performScrollTo().assertIsDisplayed()
    }

    private fun navigationItem(label: String): SemanticsNodeInteraction =
        compose.onNode(hasText(label) and isSelectable())

    private fun heading(text: String): SemanticsNodeInteraction =
        compose.onNode(hasText(text) and hasClickAction().not())

    private fun displayedMonth(): YearMonth {
        // Read what the user sees so this also covers the December → January boundary.
        val month = taggedText("month_title")
            .removePrefix("Tháng ").toInt()
        val year = taggedText("month_year")
            .trim().removePrefix("/ ").toInt()
        return YearMonth.of(year, month)
    }

    private fun taggedText(tag: String): String =
        compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsProperties.Text].joinToString("") { it.text }

    private fun assertDisplayedMonth(expected: YearMonth) {
        compose.onNodeWithTag("month_title", useUnmergedTree = true)
            .assertTextEquals("Tháng ${expected.monthValue}")
            .assertIsDisplayed()
        compose.onNodeWithTag("month_year", useUnmergedTree = true)
            .assertTextEquals(" / ${expected.year}")
            .assertIsDisplayed()
    }
}
