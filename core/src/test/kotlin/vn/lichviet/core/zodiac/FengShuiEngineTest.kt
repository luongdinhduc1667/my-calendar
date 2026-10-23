package vn.lichviet.core.zodiac

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test
import vn.lichviet.core.CanChiId
import vn.lichviet.core.VietnameseCalendar
import vn.lichviet.core.horoscope.CivilDayRule
import vn.lichviet.core.zodiac.EightHouseStar.*

class FengShuiEngineTest {
    @Test fun hourStemsMatchAllFiveRowsOfHkoTableFive() {
        // 2024-02-10 is Giáp Thìn; the following nine dates cover every day stem.
        val initial = LocalDate.of(2024, 2, 10)
        assertEquals(CanChiId(0, 4), VietnameseCalendar.dayCanChiId(initial))
        val ziStems = listOf(0, 2, 4, 6, 8, 0, 2, 4, 6, 8)
        ziStems.forEachIndexed { offset, stem ->
            val date = initial.plusDays(offset.toLong())
            assertEquals(CanChiId(stem, 0), FengShuiEngine.hourPillar(date, LocalTime.MIDNIGHT).pair)
            assertEquals(CanChiId((stem + 11) % 10, 11), FengShuiEngine.hourPillar(date, LocalTime.of(22, 59)).pair)
        }
    }

    @Test fun ziRolloverIsExplicitAndNeverReadsBeyondTheSupportedEnd() {
        val date = LocalDate.of(2024, 2, 10)
        val civil = FengShuiEngine.hourPillar(date, LocalTime.of(23, 30))
        val early = FengShuiEngine.hourPillar(date, LocalTime.of(23, 30), CivilDayRule.EARLY_ZI_NEXT_DAY)
        assertEquals(CanChiId(0, 0), civil.pair)
        assertEquals(CanChiId(2, 0), early.pair)
        assertEquals(date.plusDays(1), early.effectiveDate)
        assertEquals(early, FengShuiEngine.hourPillar(date.plusDays(1), LocalTime.of(0, 30)))
        assertThrows(IllegalArgumentException::class.java) {
            FengShuiEngine.hourPillar(VietnameseCalendar.maxDate, LocalTime.of(23, 0), CivilDayRule.EARLY_ZI_NEXT_DAY)
        }
    }

    @Test fun civilHourSegmentsCoverAll1440MinutesOnceIncludingBothPartsOfZi() {
        val hours = FengShuiEngine.civilHours(LocalDate.of(2026, 11, 24))
        assertEquals(13, hours.size)
        assertEquals((0 until 24).toList(), hours.flatMap { it.startHour until it.endHour })
        assertEquals(hours.first().pillar.pair, hours.last().pillar.pair)
        assertEquals(6, hours.map { it.pillar }.distinctBy { it.pair.branchIndex }.count { it.auspicious })
        assertEquals(6, hours.map { it.pillar }.distinctBy { it.pair.branchIndex }.count { !it.auspicious })
    }

    @Test fun standardMountainsHaveCorrectCentersAndHalfOpenBoundaries() {
        for (index in 0 until 24) {
            assertEquals(index, FengShuiEngine.mountain(index * 15.0).index)
            assertEquals((index + 1) % 24, FengShuiEngine.mountain(index * 15.0 + 7.5).index)
            assertEquals(index, FengShuiEngine.mountain(index * 15.0 + 7.4999).index)
        }
        assertEquals(FengShuiEngine.mountain(0.0), FengShuiEngine.mountain(360.0))
        assertEquals(FengShuiEngine.mountain(345.0), FengShuiEngine.mountain(-15.0))
        assertEquals(Trigram.KAN, FengShuiEngine.mountain(22.499).trigram)
        assertEquals(Trigram.GEN, FengShuiEngine.mountain(22.5).trigram)
        assertThrows(IllegalArgumentException::class.java) { FengShuiEngine.mountain(Double.NaN) }
    }

    @Test fun eightHousesMatchTheFullClassicalTableAndHouseBaseIsSittingNotFacing() {
        // Rows and columns N, NE, E, SE, S, SW, W, NW. Independent table from 大遊年變卦.
        val expected = listOf(
            listOf(FU_WEI, WU_GUI, TIAN_YI, SHENG_QI, YAN_NIAN, JUE_MING, HUO_HAI, LIU_SHA),
            listOf(WU_GUI, FU_WEI, LIU_SHA, JUE_MING, HUO_HAI, SHENG_QI, YAN_NIAN, TIAN_YI),
            listOf(TIAN_YI, LIU_SHA, FU_WEI, YAN_NIAN, SHENG_QI, HUO_HAI, JUE_MING, WU_GUI),
            listOf(SHENG_QI, JUE_MING, YAN_NIAN, FU_WEI, TIAN_YI, WU_GUI, LIU_SHA, HUO_HAI),
            listOf(YAN_NIAN, HUO_HAI, SHENG_QI, TIAN_YI, FU_WEI, LIU_SHA, WU_GUI, JUE_MING),
            listOf(JUE_MING, SHENG_QI, HUO_HAI, WU_GUI, LIU_SHA, FU_WEI, TIAN_YI, YAN_NIAN),
            listOf(HUO_HAI, YAN_NIAN, JUE_MING, LIU_SHA, WU_GUI, TIAN_YI, FU_WEI, SHENG_QI),
            listOf(LIU_SHA, TIAN_YI, WU_GUI, HUO_HAI, JUE_MING, YAN_NIAN, SHENG_QI, FU_WEI),
        )
        Trigram.entries.forEachIndexed { row, base ->
            Trigram.entries.forEachIndexed { column, direction -> assertEquals("$base / $direction", expected[row][column], FengShuiEngine.houseStar(base, direction)) }
        }
        val house = FengShuiEngine.houseCompass(180.0)
        assertEquals(12, house.facing.index) // Ngọ
        assertEquals(0, house.sitting.index) // Tý / Khảm
        assertEquals(Trigram.KAN, house.sitting.trigram)
        assertEquals(FU_WEI, house.directions.first().star)
        assertEquals(4, house.directions.count { it.star.traditionallyFavourable })
    }
}
