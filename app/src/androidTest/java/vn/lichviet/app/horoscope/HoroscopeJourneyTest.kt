package vn.lichviet.app.horoscope

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lichviet.app.LaunchPreferencesRule
import vn.lichviet.app.MainActivity
import vn.lichviet.app.data.AppDatabase

@RunWith(AndroidJUnit4::class)
class HoroscopeJourneyTest {
    @get:Rule(order = 0) val launchPreferences = LaunchPreferencesRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun createRecreateEditAndDeleteARealRoomProfile() {
        val name = "Hồ sơ QA ${UUID.randomUUID().toString().take(8)}"
        val edited = "$name sửa"
        val database = AppDatabase.get(compose.activity)
        try {
            compose.onNode(hasText("Tử vi") and isSelectable()).performClick()
            compose.onNodeWithTag("hs_new_profile").performClick()
            compose.onNodeWithTag("hs_profile_name").performTextReplacement(name)
            compose.onNodeWithTag("hs_profile_save").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText(name).fetchSemanticsNodes().size >= 2 }
            compose.onNodeWithTag("hs_chart_grid").performScrollTo().assertIsDisplayed()
            compose.activityRule.scenario.recreate()
            compose.waitUntil(10_000) { compose.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Sửa hồ sơ").performClick()
            compose.onNodeWithTag("hs_profile_name").performTextReplacement(edited)
            compose.onNodeWithTag("hs_profile_save").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText(edited).fetchSemanticsNodes().size >= 2 }
            compose.onNodeWithText("Xóa hồ sơ").performClick()
            compose.onAllNodesWithText("Xóa hồ sơ").onLast().performClick()
            compose.waitUntil(10_000) { runBlocking { database.horoscopeProfileDao().getAll().none { it.label == edited } } }
        } finally {
            runBlocking {
                database.horoscopeProfileDao().getAll().filter { it.label == name || it.label == edited }
                    .forEach { database.horoscopeProfileDao().delete(it.id) }
            }
        }
    }
}
