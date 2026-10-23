package vn.lichviet.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupCodecTest {
    @Test fun jsonRoundTripPreservesVietnameseTextRecurrenceAndPreferences() {
        val snapshot = BackupSnapshot(listOf(EventEntity(title = "Giỗ bà ngoại", note = "Hương, hoa\nTại nhà", solarDate = "2026-10-22",
            repeat = RepeatRule.LUNAR_YEARLY, type = EventType.DEATH_ANNIVERSARY, lunarDay = 30, lunarMonth = 8,
            leapMonthPolicy = LeapMonthPolicy.BOTH, missingDayPolicy = MissingDayPolicy.SKIP)),
            UserPreferences(AppTheme.DARK, showLunar = true, showHolidays = false, mondayFirst = false,
                showWeekNumbers=true,showAuspiciousHours=false,animationsEnabled=false))
        assertEquals(snapshot, BackupCodec.decode(BackupCodec.encode(snapshot)))
    }

    @Test fun corruptOrFractionalInputIsRejectedBeforeRestore() {
        val valid = BackupCodec.encode(BackupSnapshot(listOf(EventEntity(title = "Hẹn", solarDate = "2026-10-22")), UserPreferences()))
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode("{}") }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(valid.replace("\"hour\": 9", "\"hour\": 9.5")) }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(valid.replace("2026-10-22", "2200-10-22")) }
    }

    @Test fun previousBackupDefaultsNewSettingsAndVersionFiveRejectsWrongBooleanTypes() {
        val current=BackupCodec.encode(BackupSnapshot(emptyList(),UserPreferences(showWeekNumbers=true,animationsEnabled=false)))
        val legacy=org.json.JSONObject(current).apply {
            put("version",4)
            getJSONObject("preferences").apply { remove("showWeekNumbers");remove("showAuspiciousHours");remove("animationsEnabled") }
        }
        assertEquals(UserPreferences(),BackupCodec.decode(legacy.toString()).preferences)
        val malformed=org.json.JSONObject(current).apply {getJSONObject("preferences").put("animationsEnabled","false")}
        assertThrows(IllegalArgumentException::class.java){BackupCodec.decode(malformed.toString())}
    }
}
