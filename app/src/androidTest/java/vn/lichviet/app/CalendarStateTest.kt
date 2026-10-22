package vn.lichviet.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import vn.lichviet.app.data.EventRepository
import vn.lichviet.app.data.PreferencesRepository
import vn.lichviet.app.data.BackupRepository
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class CalendarStateTest {
    @Test fun restoredSelectionIsAvailableBeforeCollectionAndSurvivesRecreation() {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val state=SavedStateHandle(mapOf("selected" to "2026-02-17", "month" to "2026-02", "week" to true))
            val repository=EventRepository(context)
            val preferences=PreferencesRepository(context)
            val first=CalendarViewModel(repository,preferences,state,BackupRepository(context))
            assertEquals(LocalDate.of(2026,2,17),first.selected.value)
            assertEquals(YearMonth.of(2026,2),first.month.value)
            assertEquals(true,first.weekMode.value)
            first.select(LocalDate.of(2027,2,6))
            first.select(LocalDate.of(1899,12,31))
            val restored=CalendarViewModel(repository,preferences,state,BackupRepository(context))
            assertEquals(LocalDate.of(2027,2,6),restored.selected.value)
            assertEquals(YearMonth.of(2027,2),restored.month.value)
            restored.changeMonth(YearMonth.of(2027,5))
            restored.changeWeekMode(true)
            assertEquals("2027-02",state.get<String>("month"))
            first.viewModelScope.cancel()
            restored.viewModelScope.cancel()
        }
    }
}
