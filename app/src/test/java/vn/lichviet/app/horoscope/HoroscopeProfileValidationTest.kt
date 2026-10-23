package vn.lichviet.app.horoscope

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import vn.lichviet.core.horoscope.*

class HoroscopeProfileValidationTest {
    @Test fun solarAndLeapLunarInputsProduceSameSupportedChart() {
        val lunar = HoroscopeProfileEntity(label = "Gia đình", dateKind = "LUNAR", year = 2023, month = 2, day = 1, isLeap = true)
        val solar = lunar.copy(dateKind = "SOLAR", year = 2023, month = 3, day = 22, isLeap = false)
        val left = HoroscopeChartEngine.calculate(HoroscopeProfileValidation.birthProfile(HoroscopeProfileValidation.validated(lunar)))
        val right = HoroscopeChartEngine.calculate(HoroscopeProfileValidation.birthProfile(HoroscopeProfileValidation.validated(solar)))
        assertEquals(right.starPlacements, left.starPlacements)
        assertEquals(right.lifeBranch, left.lifeBranch)
        assertTrue(UnsupportedChartFeature.LEAP_MONTH_AUXILIARY_PLACEMENT in left.unsupportedFeatures)
    }

    @Test fun explicitDayRulePreservesTheUsersChoiceAtTetBoundary() {
        val civil = HoroscopeProfileEntity(year = 2026, month = 2, day = 16, hour = 23, minute = 30)
        val next = civil.copy(civilDayRule = "EARLY_ZI_NEXT_DAY")
        val civilChart = HoroscopeChartEngine.calculate(HoroscopeProfileValidation.birthProfile(civil))
        val nextChart = HoroscopeChartEngine.calculate(HoroscopeProfileValidation.birthProfile(next))
        assertEquals(LocalDate.of(2026, 2, 16), civilChart.effectiveSolarDate)
        assertEquals(LocalDate.of(2026, 2, 17), nextChart.effectiveSolarDate)
        assertEquals(2025, civilChart.effectiveLunarDate.year)
        assertEquals(2026, nextChart.effectiveLunarDate.year)
    }

    @Test fun impossibleDateZoneAndUnsupportedLeapNeverReachPersistence() {
        listOf(HoroscopeProfileEntity(month = 2, day = 31), HoroscopeProfileEntity(hour = 24),
            HoroscopeProfileEntity(utcOffsetMinutes = 480), HoroscopeProfileEntity(dateKind = "LUNAR", year = 2026, month = 2, isLeap = true),
            HoroscopeProfileEntity(label = "a".repeat(81)), HoroscopeProfileEntity(group = "unknown"),
            HoroscopeProfileEntity(year = 2100, month = 12, day = 31, hour = 23, civilDayRule = "EARLY_ZI_NEXT_DAY")
        ).forEach { invalid -> assertTrue("Must reject $invalid", runCatching { HoroscopeProfileValidation.validated(invalid) }.isFailure) }
    }

    @Test fun blankNameAndNicknamesAreValidWithoutRealNames() {
        assertEquals("", HoroscopeProfileValidation.validated(HoroscopeProfileEntity(label = "   ")).label)
        assertEquals("Bé Mây", HoroscopeProfileValidation.validated(HoroscopeProfileEntity(label = " Bé Mây ")).label)
    }

    @Test fun traditionalChartRingUsesTwelveDistinctOuterCellsAndLeavesCenterFree() {
        val cells = EarthlyBranch.entries.map(::chartCell)
        assertEquals(12, cells.distinct().size)
        assertTrue(cells.all { (x, y) -> x in 0..3 && y in 0..3 && (x == 0 || x == 3 || y == 0 || y == 3) })
        assertEquals(0 to 0, chartCell(EarthlyBranch.SI))
        assertEquals(3 to 3, chartCell(EarthlyBranch.HAI))
    }
}
