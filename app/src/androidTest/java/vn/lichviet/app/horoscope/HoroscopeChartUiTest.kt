package vn.lichviet.app.horoscope

import androidx.compose.ui.platform.testTag

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lichviet.app.ui.LichVietTheme
import vn.lichviet.core.horoscope.*

@RunWith(AndroidJUnit4::class)
class HoroscopeChartUiTest {
    @get:Rule val compose = createComposeRule()
    private val profile = HoroscopeProfileEntity(year = 1984, month = 2, day = 2, minute = 30)
    private val chart = HoroscopeChartEngine.calculate(HoroscopeProfileValidation.birthProfile(profile))

    @Test fun traditionalGridHasTwelveTappablePalacesAndReadableSelectedDetails() {
        compose.setContent {
            var selected by remember { mutableStateOf(EarthlyBranch.YIN) }
            LichVietTheme {
                Column(Modifier.widthIn(max = 600.dp)) {
                    HoroscopeChartView(chart, selected, { selected = it })
                    Text(selected.name, Modifier.testTag("selected_branch"))
                }
            }
        }
        EarthlyBranch.entries.forEach { compose.onNodeWithTag("hs_palace_${it.name}").assertExists() }
        compose.onNodeWithTag("hs_palace_YIN").assertIsSelected()
        compose.onNodeWithTag("hs_palace_YOU").performClick().assertIsSelected()
        compose.onNodeWithTag("selected_branch").assertTextEquals("YOU")
    }

    @Test fun editorRejectsImpossibleLunarMonthThenSavesAnExplicitValidBoundaryRule() {
        var saved: HoroscopeProfileEntity? = null
        compose.setContent {
            LichVietTheme { HoroscopeProfileEditor(profile.copy(dateKind = "LUNAR", year = 2026, month = 2, day = 1, isLeap = true,
                hour = 23, civilDayRule = "EARLY_ZI_NEXT_DAY"), HoroscopeOperation(), {}, { saved = it }) }
        }
        compose.onNodeWithTag("hs_profile_save").performClick()
        compose.onNodeWithTag("hs_profile_error").assertIsDisplayed()
        compose.runOnIdle { assertNull(saved) }
        compose.onNodeWithTag("hs_profile_year").performScrollTo().performTextReplacement("2023")
        compose.onNodeWithTag("hs_profile_save").performClick()
        compose.runOnIdle {
            assertNotNull(saved)
            assertEquals("EARLY_ZI_NEXT_DAY", saved!!.civilDayRule)
            assertTrue(saved!!.isLeap)
            assertEquals(23, saved!!.hour)
        }
    }
}
