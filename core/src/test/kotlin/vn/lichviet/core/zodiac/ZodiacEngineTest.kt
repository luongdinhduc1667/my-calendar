package vn.lichviet.core.zodiac

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import vn.lichviet.core.CanChiId
import vn.lichviet.core.VietnameseCalendar
import vn.lichviet.core.horoscope.FiveElement

class ZodiacEngineTest {
    @Test fun vietnameseSignsChangeAtTetRatherThanJanuaryFirst() {
        val eve = ZodiacEngine.birthFacts(LocalDate.of(2026, 2, 16))
        val tet = ZodiacEngine.birthFacts(LocalDate.of(2026, 2, 17))
        assertEquals(2025, eve.lunarDate.year)
        assertEquals(ZodiacSign.SNAKE, eve.sign)
        assertEquals(NapAm.PHU_DANG_HOA, eve.napAm)
        assertEquals(2026, tet.lunarDate.year)
        assertEquals(ZodiacSign.HORSE, tet.sign)
        assertEquals(NapAm.THIEN_HA_THUY, tet.napAm)
        assertEquals(ZodiacSign.CAT, ZodiacEngine.birthFacts(LocalDate.of(2023, 1, 22)).sign)
        assertEquals(ZodiacSign.BUFFALO, ZodiacEngine.birthFacts(LocalDate.of(2021, 2, 12)).sign)
    }

    @Test fun napAmFixturesComeFromConsecutivePairsInTheClassicalSixtyCycle() {
        val fixtures = mapOf(1984 to NapAm.HAI_TRUNG_KIM, 1985 to NapAm.HAI_TRUNG_KIM, 1986 to NapAm.LU_TRUNG_HOA,
            1988 to NapAm.DAI_LAM_MOC, 1990 to NapAm.LO_BANG_THO, 2000 to NapAm.BACH_LAP_KIM,
            1968 to NapAm.DAI_TRACH_THO, 1982 to NapAm.DAI_HAI_THUY, 2026 to NapAm.THIEN_HA_THUY)
        fixtures.forEach { (year, expected) -> assertEquals("$year", expected, ZodiacEngine.napAm(VietnameseCalendar.yearCanChiId(year))) }
        assertEquals(0, ZodiacEngine.cycleIndex(CanChiId(0, 0)))
        assertEquals(59, ZodiacEngine.cycleIndex(CanChiId(9, 11)))
        assertThrows(IllegalArgumentException::class.java) { ZodiacEngine.napAm(CanChiId(0, 1)) }
    }

    @Test fun sixHarmoniesAndSixOppositionsMatchTheSourcePairs() {
        val harmonies = listOf(0 to 1, 2 to 11, 3 to 10, 4 to 9, 5 to 8, 6 to 7)
        harmonies.forEach { (a, b) ->
            assertEquals(BranchRelation.SIX_HARMONY, ZodiacEngine.relation(ZodiacSign.entries[a], ZodiacSign.entries[b]))
            assertEquals(BranchRelation.SIX_HARMONY, ZodiacEngine.relation(ZodiacSign.entries[b], ZodiacSign.entries[a]))
        }
        (0..5).forEach { a -> assertEquals(BranchRelation.OPPOSITION, ZodiacEngine.relation(ZodiacSign.entries[a], ZodiacSign.entries[a + 6])) }
        for (a in ZodiacSign.entries) for (b in ZodiacSign.entries) assertEquals(ZodiacEngine.relation(a, b), ZodiacEngine.relation(b, a))
    }

    @Test fun trinesReturnTwoCompanionsWithoutCallingAPairACompleteTransformation() {
        val groups = listOf(listOf(8, 0, 4), listOf(5, 9, 1), listOf(2, 6, 10), listOf(11, 3, 7))
        groups.forEach { group -> group.forEach { index ->
            assertEquals(group.filter { it != index }.toSet(), ZodiacEngine.companions(ZodiacSign.entries[index]).map { it.ordinal }.toSet())
        } }
    }

    @Test fun elementRelationshipsKeepTheirDirectionAndDoNotConflateNapAmWithStemElement() {
        assertEquals(ElementRelation.GENERATES, ZodiacEngine.elementRelation(FiveElement.METAL, FiveElement.WATER))
        assertEquals(ElementRelation.GENERATED_BY, ZodiacEngine.elementRelation(FiveElement.WATER, FiveElement.METAL))
        assertEquals(ElementRelation.CONTROLS, ZodiacEngine.elementRelation(FiveElement.WOOD, FiveElement.EARTH))
        assertEquals(ElementRelation.CONTROLLED_BY, ZodiacEngine.elementRelation(FiveElement.EARTH, FiveElement.WOOD))
        assertEquals(ElementRelation.SAME, ZodiacEngine.elementRelation(FiveElement.FIRE, FiveElement.FIRE))
        val jiaZi = CanChiId(0, 0)
        assertEquals(FiveElement.WOOD, ZodiacEngine.stemElement(jiaZi.stemIndex))
        assertEquals(FiveElement.METAL, ZodiacEngine.napAm(jiaZi).element)
        assertEquals(FiveElement.WATER, ZodiacSign.RAT.branchElement)
    }

    @Test fun eachAnimalHasFiveSixtyCycleVariantsAndEverySupportedLunarYearOccursOnce() {
        val years = mutableListOf<Int>()
        ZodiacSign.entries.forEach { sign ->
            val variants = ZodiacEngine.yearVariants(sign)
            assertEquals(5, variants.size)
            variants.forEach { row ->
                assertEquals(sign.ordinal, row.canChi.branchIndex)
                row.lunarYears.forEach { year -> assertEquals(row.canChi, VietnameseCalendar.yearCanChiId(year)) }
                years += row.lunarYears
            }
        }
        assertEquals((1899..2100).toList(), years.sorted())
    }

    @Test fun forecastMonthSeparatesRegularAndLeapFebruary2023() {
        val regular = ZodiacEngine.window(LocalDate.of(2023, 3, 1), ForecastPeriod.MONTH)
        val leap = ZodiacEngine.window(LocalDate.of(2023, 4, 1), ForecastPeriod.MONTH)
        assertEquals(ForecastWindow(LocalDate.of(2023, 2, 20), LocalDate.of(2023, 3, 21), false), regular)
        assertEquals(ForecastWindow(LocalDate.of(2023, 3, 22), LocalDate.of(2023, 4, 19), false), leap)
        assertTrue(ZodiacEngine.forecast(ZodiacSign.CAT, LocalDate.of(2023, 4, 1), ForecastPeriod.MONTH).anchor!!.isLeapMonth)
    }

    @Test fun forecastYearUsesTetAndTheWeekUsesActualMondayThroughSunday() {
        assertEquals(ForecastWindow(LocalDate.of(2023, 1, 22), LocalDate.of(2024, 2, 9), false),
            ZodiacEngine.window(LocalDate.of(2023, 8, 1), ForecastPeriod.YEAR))
        val week = ZodiacEngine.forecast(ZodiacSign.RAT, LocalDate.of(2026, 2, 18), ForecastPeriod.WEEK)
        assertEquals(LocalDate.of(2026, 2, 16), week.window.start)
        assertEquals(LocalDate.of(2026, 2, 22), week.window.end)
        assertEquals(7, week.days.size)
        assertNull(week.anchor)
        assertNotEquals(VietnameseCalendar.solarToLunar(week.days.first().date).year, VietnameseCalendar.solarToLunar(week.days.last().date).year)
        week.days.forEach { assertEquals(VietnameseCalendar.dayCanChiId(it.date), it.canChi) }
    }

    @Test fun allTwelveSignsHaveFourReproducibleSourceLinkedForecastsIncludingRangeEdges() {
        for (date in listOf(LocalDate.of(2026, 11, 24), VietnameseCalendar.minDate, VietnameseCalendar.maxDate)) {
            for (sign in ZodiacSign.entries) for (period in ForecastPeriod.entries) {
                val report = ZodiacEngine.forecast(sign, date, period)
                assertEquals(report, ZodiacEngine.forecast(sign, date, period))
                assertTrue(report.days.isNotEmpty())
                assertTrue(report.days.size <= 385)
                assertEquals(report.window.start, report.days.first().date)
                assertEquals(report.window.end, report.days.last().date)
                assertEquals(CulturalSource.BRANCH_RELATIONS, report.source)
                assertTrue(report.days.all { it.date in VietnameseCalendar.minDate..VietnameseCalendar.maxDate })
            }
        }
        assertTrue(ZodiacEngine.window(VietnameseCalendar.minDate, ForecastPeriod.YEAR).clipped)
        assertTrue(ZodiacEngine.window(VietnameseCalendar.maxDate, ForecastPeriod.YEAR).clipped)
    }
}
