package vn.lichviet.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lichviet.app.MainActivity
import vn.lichviet.app.LaunchPreferencesRule
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class EventJourneyTest {
    @get:Rule(order=0) val launchPreferences=LaunchPreferencesRule()
    @get:Rule(order=1) val compose=createAndroidComposeRule<MainActivity>()

    @Test fun lunarAnniversaryCanBeSavedReopenedAndDeleted() {
        val title="Ngày giỗ kiểm thử ${UUID.randomUUID().toString().take(8)}"
        compose.onNode(hasText("Sự kiện") and isSelectable()).performClick()
        compose.onNodeWithText("Thêm ngày đáng nhớ").performScrollTo().performClick()
        compose.onNodeWithText("Tên sự kiện *").performTextInput(title)
        compose.onNodeWithText("Loại sự kiện").performClick()
        compose.onNodeWithText("Ngày giỗ").performClick()
        compose.onNodeWithText("Nhắc tôi").performScrollTo()
        compose.onAllNodes(isToggleable()).onLast().performClick()
        compose.onNodeWithText("Lưu sự kiện").performScrollTo().performClick()
        compose.waitUntil(timeoutMillis=5000){compose.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText(title).performScrollTo().assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText(title).performScrollTo().performClick()
        compose.onNodeWithText("Hằng năm âm lịch").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Xóa sự kiện").performScrollTo().performClick()
        compose.onNodeWithText("Xóa",useUnmergedTree=true).performClick()
        compose.waitUntil(timeoutMillis=5000){compose.onAllNodesWithText(title).fetchSemanticsNodes().isEmpty()}
    }
}
