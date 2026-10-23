package vn.lichviet.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import vn.lichviet.app.widget.CalendarWidgets

private val Context.userPreferences by preferencesDataStore(name = "preferences")
enum class AppTheme { SYSTEM, LIGHT, DARK }
data class UserPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val showLunar: Boolean = true,
    val showHolidays: Boolean = true,
    val mondayFirst: Boolean = true,
    val showWeekNumbers: Boolean = false,
    val showAuspiciousHours: Boolean = true,
    val animationsEnabled: Boolean = true,
)

@Singleton
class PreferencesRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    private val lunarKey = booleanPreferencesKey("show_lunar")
    private val holidayKey = booleanPreferencesKey("show_holidays")
    private val mondayKey = booleanPreferencesKey("monday_first")
    private val weekNumbersKey = booleanPreferencesKey("show_week_numbers")
    private val auspiciousHoursKey = booleanPreferencesKey("show_auspicious_hours")
    private val animationsKey = booleanPreferencesKey("animations_enabled")
    private val onboardingKey = booleanPreferencesKey("onboarding_completed")
    val onboardingCompleted = context.userPreferences.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { it[onboardingKey] ?: false }
    val preferences = context.userPreferences.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { values ->
        UserPreferences(
            theme = runCatching { AppTheme.valueOf(values[themeKey] ?: "SYSTEM") }.getOrDefault(AppTheme.SYSTEM),
            showLunar = values[lunarKey] ?: true,
            showHolidays = values[holidayKey] ?: true,
            mondayFirst = values[mondayKey] ?: true,
            showWeekNumbers = values[weekNumbersKey] ?: false,
            showAuspiciousHours = values[auspiciousHoursKey] ?: true,
            animationsEnabled = values[animationsKey] ?: true,
        )
    }
    suspend fun setTheme(value: AppTheme) { context.userPreferences.edit { it[themeKey] = value.name } }
    suspend fun setWeekNumbers(value: Boolean) { context.userPreferences.edit { it[weekNumbersKey] = value } }
    suspend fun setAuspiciousHours(value: Boolean) { context.userPreferences.edit { it[auspiciousHoursKey] = value } }
    suspend fun setAnimations(value: Boolean) { context.userPreferences.edit { it[animationsKey] = value } }
    suspend fun completeOnboarding() = setOnboardingCompleted(true)
    suspend fun setOnboardingCompleted(value:Boolean) { context.userPreferences.edit { it[onboardingKey] = value } }
    suspend fun setShowLunar(value: Boolean) {
        context.userPreferences.edit { it[lunarKey] = value }
        CalendarWidgets.requestUpdate(context)
    }
    suspend fun setShowHolidays(value: Boolean) {
        context.userPreferences.edit { it[holidayKey] = value }
        CalendarWidgets.requestUpdate(context)
    }
    suspend fun setMondayFirst(value: Boolean) {
        context.userPreferences.edit { it[mondayKey] = value }
        CalendarWidgets.requestUpdate(context)
    }
    suspend fun update(value: UserPreferences) {
        context.userPreferences.edit {
            it[themeKey] = value.theme.name
            it[lunarKey] = value.showLunar
            it[holidayKey] = value.showHolidays
            it[mondayKey] = value.mondayFirst
            it[weekNumbersKey] = value.showWeekNumbers
            it[auspiciousHoursKey] = value.showAuspiciousHours
            it[animationsKey] = value.animationsEnabled
        }
        CalendarWidgets.requestUpdate(context)
    }
    suspend fun reset() {
        vn.lichviet.app.AppLocaleManager.resetLanguage(context)
        context.userPreferences.edit { it.clear() }
        CalendarWidgets.requestUpdate(context)
    }
}
