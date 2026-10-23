package vn.lichviet.app.ui

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lichviet.app.MainActivity
import vn.lichviet.app.LaunchPreferencesRule
import vn.lichviet.app.data.EventEntity
import vn.lichviet.app.data.EventRepository
import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MultipleReminderEditorTest {
    @get:Rule(order=0) val launchPreferences=LaunchPreferencesRule()
    @get:Rule(order=1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun multipleOffsetsSurviveEditorRecreationAndSave() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (Build.VERSION.SDK_INT >= 33) InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val repository = EventRepository(context)
        val title = "Nhiều mốc ${UUID.randomUUID().toString().take(8)}"
        val date = LocalDate.now(VietnameseCalendar.zoneId).plusDays(2)
        val id = runBlocking { repository.save(EventEntity(title = title, solarDate = date.toString()), listOf(5, 1440)) }
        try {
            compose.onNode(hasText("Sự kiện") and isSelectable()).performClick()
            compose.onNodeWithText(title).performScrollTo().performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Đang tải mốc nhắc…").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithText(context.getString(vn.lichviet.app.R.string.event_minutes_before,10)).performScrollTo().performClick()
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText("Lưu sự kiện").performScrollTo().performClick()
            compose.waitUntil(5_000) { runBlocking { repository.reminderOffsets(id) == listOf(5, 10, 1440) } }
            assertEquals(listOf(5, 10, 1440), runBlocking { repository.reminderOffsets(id) })
        } finally { runBlocking { repository.delete(id) } }
    }
}
