package vn.lichviet.core.horoscope

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test
import vn.lichviet.core.LunarDate

class HoroscopeChartEngineTest {
    private fun chart(
        date: BirthDateInput,
        time: LocalTime = LocalTime.of(0, 30),
        sex: TraditionalSex = TraditionalSex.MALE,
        dayRule: CivilDayRule = CivilDayRule.CIVIL_MIDNIGHT,
    ) = HoroscopeChartEngine.calculate(BirthProfile(date, time, sex, civilDayRule = dayRule))

    @Test
    fun `classic fire bureau first day example and twelve palace order`() {
        // Full Book vol.3, 安身命例 explicitly places a 甲-year first-month,
        // first-day 子-hour birth in 寅 with fire bureau and 紫微 at 酉.
        // 1984-02-02 is Vietnamese lunar 甲子 1/1, checked by the calendar core.
        val result = chart(BirthDateInput.Solar(LocalDate.of(1984, 2, 2)))
        assertEquals(LunarDate(1, 1, 1984), result.effectiveLunarDate)
        assertEquals(EarthlyBranch.ZI, result.birthHour)
        assertEquals(HeavenlyStem.JIA, result.yearStem)
        assertEquals(EarthlyBranch.ZI, result.yearBranch)
        assertEquals(EarthlyBranch.YIN, result.lifeBranch)
        assertEquals(EarthlyBranch.YIN, result.bodyBranch)
        assertEquals(HeavenlyStem.BING, result.palaceAt(EarthlyBranch.YIN).stem)
        assertEquals(Bureau.FIRE_SIX, result.bureau)
        assertEquals(EarthlyBranch.YOU, result.starPlacements[MainStar.ZI_WEI])
        assertEquals(EarthlyBranch.WEI, result.starPlacements[MainStar.TIAN_FU])
        assertEquals(PalaceRole.LIFE, result.palaceAt(EarthlyBranch.YIN).role)
        assertEquals(PalaceRole.PARENTS, result.palaceAt(EarthlyBranch.MAO).role)
        assertEquals(PalaceRole.SIBLINGS, result.palaceAt(EarthlyBranch.CHOU).role)
        assertEquals(12, result.palaces.size)
        assertEquals(PalaceRole.entries.toSet(), result.palaces.map { it.role }.toSet())
    }

    @Test
    fun `fourteen major star positions follow the classic mnemonic and Tianfu mirror`() {
        // These are verse-derived placements for the preceding source example;
        // they are not represented as an independently verified full-chart golden.
        val result = chart(BirthDateInput.Lunar(LunarDate(1, 1, 1984)))
        val expected = mapOf(
            MainStar.ZI_WEI to EarthlyBranch.YOU,
            MainStar.TIAN_JI to EarthlyBranch.SHEN,
            MainStar.TAI_YANG to EarthlyBranch.WU,
            MainStar.WU_QU to EarthlyBranch.SI,
            MainStar.TIAN_TONG to EarthlyBranch.CHEN,
            MainStar.LIAN_ZHEN to EarthlyBranch.CHOU,
            MainStar.TIAN_FU to EarthlyBranch.WEI,
            MainStar.TAI_YIN to EarthlyBranch.SHEN,
            MainStar.TAN_LANG to EarthlyBranch.YOU,
            MainStar.JU_MEN to EarthlyBranch.XU,
            MainStar.TIAN_XIANG to EarthlyBranch.HAI,
            MainStar.TIAN_LIANG to EarthlyBranch.ZI,
            MainStar.QI_SHA to EarthlyBranch.CHOU,
            MainStar.PO_JUN to EarthlyBranch.SI,
        )
        assertEquals(expected, result.starPlacements.filterKeys { it is MainStar })
        assertEquals(14, result.palaces.sumOf { it.mainStars.size })
    }

    @Test
    fun `source table anchors and documented two emendations`() {
        assertEquals(EarthlyBranch.CHOU, HoroscopeChartEngine.ziWeiBranch(Bureau.WATER_TWO, 1))
        assertEquals(EarthlyBranch.CHEN, HoroscopeChartEngine.ziWeiBranch(Bureau.WOOD_THREE, 1))
        assertEquals(EarthlyBranch.HAI, HoroscopeChartEngine.ziWeiBranch(Bureau.METAL_FOUR, 1))
        assertEquals(EarthlyBranch.WU, HoroscopeChartEngine.ziWeiBranch(Bureau.EARTH_FIVE, 1))
        assertEquals(EarthlyBranch.YOU, HoroscopeChartEngine.ziWeiBranch(Bureau.FIRE_SIX, 1))
        assertEquals(EarthlyBranch.YIN, HoroscopeChartEngine.ziWeiBranch(Bureau.WOOD_THREE, 5))
        assertEquals(EarthlyBranch.HAI, HoroscopeChartEngine.ziWeiBranch(Bureau.METAL_FOUR, 30))
    }

    @Test
    fun `sourced auxiliaries and Jia transformations have specific branches`() {
        val result = chart(BirthDateInput.Solar(LocalDate.of(1984, 2, 2)))
        assertEquals(EarthlyBranch.XU, result.starPlacements[AuxiliaryStar.WEN_CHANG])
        assertEquals(EarthlyBranch.CHEN, result.starPlacements[AuxiliaryStar.WEN_QU])
        assertEquals(EarthlyBranch.CHEN, result.starPlacements[AuxiliaryStar.ZUO_FU])
        assertEquals(EarthlyBranch.XU, result.starPlacements[AuxiliaryStar.YOU_BI])
        assertEquals(EarthlyBranch.YIN, result.starPlacements[AuxiliaryStar.LU_CUN])
        assertEquals(EarthlyBranch.MAO, result.starPlacements[AuxiliaryStar.QING_YANG])
        assertEquals(EarthlyBranch.CHOU, result.starPlacements[AuxiliaryStar.TUO_LUO])
        assertEquals(EarthlyBranch.YIN, result.starPlacements[AuxiliaryStar.TIAN_MA])
        assertEquals(EarthlyBranch.HAI, result.starPlacements[AuxiliaryStar.TIAN_KONG])
        assertEquals(EarthlyBranch.HAI, result.starPlacements[AuxiliaryStar.DI_JIE])
        assertEquals(
            listOf(MainStar.LIAN_ZHEN, MainStar.PO_JUN, MainStar.WU_QU, MainStar.TAI_YANG),
            result.transformations.map { it.star },
        )
        assertTrue(result.transformations.all { it.branch == result.starPlacements[it.star] })
    }

    @Test
    fun `all ten four transformation rows follow the selected Ming edition`() {
        // Full Book vol.3, 安祿權科忌四星變化訣. These rows intentionally
        // preserve this edition's 庚日武同相 and 壬梁紫府武 readings.
        val rows: Map<HeavenlyStem, List<HoroscopeStarId>> = mapOf(
            HeavenlyStem.JIA to listOf(MainStar.LIAN_ZHEN, MainStar.PO_JUN, MainStar.WU_QU, MainStar.TAI_YANG),
            HeavenlyStem.YI to listOf(MainStar.TIAN_JI, MainStar.TIAN_LIANG, MainStar.ZI_WEI, MainStar.TAI_YIN),
            HeavenlyStem.BING to listOf(MainStar.TIAN_TONG, MainStar.TIAN_JI, AuxiliaryStar.WEN_CHANG, MainStar.LIAN_ZHEN),
            HeavenlyStem.DING to listOf(MainStar.TAI_YIN, MainStar.TIAN_TONG, MainStar.TIAN_JI, MainStar.JU_MEN),
            HeavenlyStem.WU to listOf(MainStar.TAN_LANG, MainStar.TAI_YIN, AuxiliaryStar.YOU_BI, MainStar.TIAN_JI),
            HeavenlyStem.JI to listOf(MainStar.WU_QU, MainStar.TAN_LANG, MainStar.TIAN_LIANG, AuxiliaryStar.WEN_QU),
            HeavenlyStem.GENG to listOf(MainStar.TAI_YANG, MainStar.WU_QU, MainStar.TIAN_TONG, MainStar.TIAN_XIANG),
            HeavenlyStem.XIN to listOf(MainStar.JU_MEN, MainStar.TAI_YANG, AuxiliaryStar.WEN_QU, AuxiliaryStar.WEN_CHANG),
            HeavenlyStem.REN to listOf(MainStar.TIAN_LIANG, MainStar.ZI_WEI, MainStar.TIAN_FU, MainStar.WU_QU),
            HeavenlyStem.GUI to listOf(MainStar.PO_JUN, MainStar.JU_MEN, MainStar.TAI_YIN, MainStar.TAN_LANG),
        )
        rows.forEach { (stem, stars) ->
            val result = chart(BirthDateInput.Lunar(LunarDate(1, 1, 1984 + stem.index)))
            assertEquals(stem, result.yearStem)
            assertEquals("$stem transformation row", stars, result.transformations.map { it.star })
            assertEquals(TransformationKind.entries, result.transformations.map { it.kind })
        }
    }

    @Test
    fun `leap lunar birth uses following month for life and body but leaves uncertain assistants absent`() {
        val lunarInput = BirthDateInput.Lunar(LunarDate(1, 2, 2023, isLeap = true))
        val fromLunar = chart(lunarInput)
        val fromSolar = chart(BirthDateInput.Solar(LocalDate.of(2023, 3, 22)))
        assertEquals(fromSolar.effectiveLunarDate, fromLunar.effectiveLunarDate)
        assertEquals(LunarDate(1, 2, 2023, true), fromLunar.effectiveLunarDate)
        assertEquals(EarthlyBranch.CHEN, fromLunar.lifeBranch)
        assertEquals(EarthlyBranch.CHEN, fromLunar.bodyBranch)
        assertEquals(HeavenlyStem.BING, fromLunar.palaceAt(EarthlyBranch.CHEN).stem)
        assertEquals(Bureau.EARTH_FIVE, fromLunar.bureau)
        assertEquals(EarthlyBranch.WU, fromLunar.starPlacements[MainStar.ZI_WEI])
        assertFalse(fromLunar.starPlacements.containsKey(AuxiliaryStar.ZUO_FU))
        assertFalse(fromLunar.starPlacements.containsKey(AuxiliaryStar.YOU_BI))
        assertTrue(UnsupportedChartFeature.LEAP_MONTH_AUXILIARY_PLACEMENT in fromLunar.unsupportedFeatures)
    }

    @Test
    fun `birth double-hour and civil day rule are explicit at the day boundary`() {
        val date = BirthDateInput.Solar(LocalDate.of(2026, 2, 16))
        assertEquals(EarthlyBranch.HAI, chart(date, LocalTime.of(22, 59)).birthHour)
        assertEquals(EarthlyBranch.ZI, chart(date, LocalTime.of(23, 0)).birthHour)
        assertEquals(EarthlyBranch.ZI, chart(date, LocalTime.of(0, 59)).birthHour)
        assertEquals(EarthlyBranch.CHOU, chart(date, LocalTime.of(1, 0)).birthHour)
        val civil = chart(date, LocalTime.of(23, 30))
        val earlyZi = chart(date, LocalTime.of(23, 30), dayRule = CivilDayRule.EARLY_ZI_NEXT_DAY)
        assertEquals(LunarDate(29, 12, 2025), civil.effectiveLunarDate)
        assertEquals(LunarDate(1, 1, 2026), earlyZi.effectiveLunarDate)
        assertEquals(LocalDate.of(2026, 2, 17), earlyZi.effectiveSolarDate)
        assertEquals(EarthlyBranch.ZI, earlyZi.birthHour)
    }

    @Test
    fun `major and minor cycles use source directions and counted ages`() {
        val date = BirthDateInput.Solar(LocalDate.of(1984, 2, 2))
        val male = chart(date)
        assertNull(male.majorCycleAtVirtualAge(5))
        assertEquals(MajorCycle(EarthlyBranch.YIN, 6, 15), male.majorCycleAtVirtualAge(6))
        assertEquals(MajorCycle(EarthlyBranch.MAO, 16, 25), male.majorCycleAtVirtualAge(16))
        assertEquals(EarthlyBranch.XU, male.minorPalaceAtVirtualAge(1))
        assertEquals(EarthlyBranch.HAI, male.minorPalaceAtVirtualAge(2))
        assertEquals(EarthlyBranch.XU, male.minorPalaceAtVirtualAge(13))

        val female = chart(date, sex = TraditionalSex.FEMALE)
        assertEquals(MajorCycle(EarthlyBranch.CHOU, 16, 25), female.majorCycleAtVirtualAge(16))
        assertEquals(EarthlyBranch.YOU, female.minorPalaceAtVirtualAge(2))
        try {
            female.minorPalaceAtVirtualAge(0)
            fail("Zero is not a traditional counted age")
        } catch (_: IllegalArgumentException) { }
    }

    @Test
    fun `unsupported zones and invalid lunar dates fail rather than silently alter charts`() {
        val vietnam = BirthProfile(BirthDateInput.Solar(LocalDate.of(1984, 2, 2)), LocalTime.MIDNIGHT, TraditionalSex.MALE)
        val originalDefaultZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"))
            val west = HoroscopeChartEngine.calculate(vietnam)
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
            assertEquals(west.starPlacements, HoroscopeChartEngine.calculate(vietnam).starPlacements)
        } finally {
            TimeZone.setDefault(originalDefaultZone)
        }
        try {
            HoroscopeChartEngine.calculate(vietnam.copy(utcOffset = ZoneOffset.ofHours(8)))
            fail("UTC+8 must be rejected")
        } catch (_: IllegalArgumentException) { }
        try {
            chart(BirthDateInput.Lunar(LunarDate(1, 2, 2026, isLeap = true)))
            fail("Nonexistent leap month must be rejected")
        } catch (_: IllegalArgumentException) { }
    }
}
