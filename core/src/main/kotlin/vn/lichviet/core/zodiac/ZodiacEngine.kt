package vn.lichviet.core.zodiac

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import vn.lichviet.core.CanChiId
import vn.lichviet.core.LunarDate
import vn.lichviet.core.VietnameseCalendar
import vn.lichviet.core.horoscope.FiveElement
import vn.lichviet.core.horoscope.YinYang

/** Vietnamese animals, ordered by earthly branch. Mão is Cat and Sửu is Buffalo. */
enum class ZodiacSign(val symbol: String, val branchElement: FiveElement) {
    RAT("🐀", FiveElement.WATER), BUFFALO("🐃", FiveElement.EARTH), TIGER("🐅", FiveElement.WOOD),
    CAT("🐈", FiveElement.WOOD), DRAGON("🐉", FiveElement.EARTH), SNAKE("🐍", FiveElement.FIRE),
    HORSE("🐎", FiveElement.FIRE), GOAT("🐐", FiveElement.EARTH), MONKEY("🐒", FiveElement.METAL),
    ROOSTER("🐓", FiveElement.METAL), DOG("🐕", FiveElement.EARTH), PIG("🐖", FiveElement.WATER);

    val polarity: YinYang get() = if (ordinal % 2 == 0) YinYang.YANG else YinYang.YIN
}

/** Consecutive pairs of the sixty-cycle, starting at Giáp Tý / Ất Sửu. */
enum class NapAm(val element: FiveElement) {
    HAI_TRUNG_KIM(FiveElement.METAL), LU_TRUNG_HOA(FiveElement.FIRE), DAI_LAM_MOC(FiveElement.WOOD),
    LO_BANG_THO(FiveElement.EARTH), KIEM_PHONG_KIM(FiveElement.METAL), SON_DAU_HOA(FiveElement.FIRE),
    GIAN_HA_THUY(FiveElement.WATER), THANH_DAU_THO(FiveElement.EARTH), BACH_LAP_KIM(FiveElement.METAL),
    DUONG_LIEU_MOC(FiveElement.WOOD), TUYEN_TRUNG_THUY(FiveElement.WATER), OC_THUONG_THO(FiveElement.EARTH),
    TICH_LICH_HOA(FiveElement.FIRE), TUNG_BACH_MOC(FiveElement.WOOD), TRUONG_LUU_THUY(FiveElement.WATER),
    SA_TRUNG_KIM(FiveElement.METAL), SON_HA_HOA(FiveElement.FIRE), BINH_DIA_MOC(FiveElement.WOOD),
    BICH_THUONG_THO(FiveElement.EARTH), KIM_BACH_KIM(FiveElement.METAL), PHU_DANG_HOA(FiveElement.FIRE),
    THIEN_HA_THUY(FiveElement.WATER), DAI_TRACH_THO(FiveElement.EARTH), THOA_XUYEN_KIM(FiveElement.METAL),
    TANG_DO_MOC(FiveElement.WOOD), DAI_KHE_THUY(FiveElement.WATER), SA_TRUNG_THO(FiveElement.EARTH),
    THIEN_THUONG_HOA(FiveElement.FIRE), THACH_LUU_MOC(FiveElement.WOOD), DAI_HAI_THUY(FiveElement.WATER),
}

enum class BranchRelation { SAME, SIX_HARMONY, TRINE_COMPANION, OPPOSITION, ORDINARY }
enum class ElementRelation { SAME, GENERATES, GENERATED_BY, CONTROLS, CONTROLLED_BY }
enum class ForecastPeriod { DAY, WEEK, MONTH, YEAR }
enum class ForecastTone { COOPERATE, REVIEW, CONSOLIDATE, BALANCED, MIXED }

data class BirthYearFacts(val solarDate: LocalDate, val lunarDate: LunarDate, val canChi: CanChiId, val sign: ZodiacSign, val napAm: NapAm)
data class ZodiacYearVariant(val canChi: CanChiId, val napAm: NapAm, val lunarYears: List<Int>)
data class ForecastDay(val date: LocalDate, val canChi: CanChiId, val relation: BranchRelation)
data class ForecastWindow(val start: LocalDate, val end: LocalDate, val clipped: Boolean)
data class ZodiacForecast(
    val sign: ZodiacSign,
    val period: ForecastPeriod,
    val referenceDate: LocalDate,
    val window: ForecastWindow,
    /** No synthetic weekly pillar: week reports aggregate their actual civil dates. */
    val anchor: CanChiId?,
    val anchorRelation: BranchRelation?,
    val tone: ForecastTone,
    val days: List<ForecastDay>,
    val yearContext: CanChiId,
    val monthContext: CanChiId,
    val source: CulturalSource = CulturalSource.BRANCH_RELATIONS,
) {
    val harmonyDays: List<ForecastDay> get() = days.filter { it.relation == BranchRelation.SIX_HARMONY || it.relation == BranchRelation.TRINE_COMPANION }
    val oppositionDays: List<ForecastDay> get() = days.filter { it.relation == BranchRelation.OPPOSITION }
}

object ZodiacEngine {
    private val harmonyPartners = listOf(1, 0, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2)
    private val generationCycle = listOf(FiveElement.WOOD, FiveElement.FIRE, FiveElement.EARTH, FiveElement.METAL, FiveElement.WATER)

    fun birthFacts(date: LocalDate): BirthYearFacts {
        val lunar = VietnameseCalendar.solarToLunar(date)
        val pair = VietnameseCalendar.yearCanChiId(lunar.year)
        return BirthYearFacts(date, lunar, pair, ZodiacSign.entries[pair.branchIndex], napAm(pair))
    }

    fun cycleIndex(pair: CanChiId): Int {
        require(pair.stemIndex in 0..9 && pair.branchIndex in 0..11 && pair.stemIndex % 2 == pair.branchIndex % 2)
        return (0..59).first { it % 10 == pair.stemIndex && it % 12 == pair.branchIndex }
    }

    fun napAm(pair: CanChiId): NapAm = NapAm.entries[cycleIndex(pair) / 2]
    fun stemElement(stem: Int): FiveElement {
        require(stem in 0..9)
        return listOf(FiveElement.WOOD, FiveElement.FIRE, FiveElement.EARTH, FiveElement.METAL, FiveElement.WATER)[stem / 2]
    }

    fun relation(first: ZodiacSign, second: ZodiacSign): BranchRelation = when {
        first == second -> BranchRelation.SAME
        harmonyPartners[first.ordinal] == second.ordinal -> BranchRelation.SIX_HARMONY
        (first.ordinal + 6) % 12 == second.ordinal -> BranchRelation.OPPOSITION
        first.ordinal % 4 == second.ordinal % 4 -> BranchRelation.TRINE_COMPANION
        else -> BranchRelation.ORDINARY
    }

    fun companions(sign: ZodiacSign): List<ZodiacSign> = ZodiacSign.entries.filter { relation(sign, it) == BranchRelation.TRINE_COMPANION }
    fun harmonyPartner(sign: ZodiacSign): ZodiacSign = ZodiacSign.entries[harmonyPartners[sign.ordinal]]
    fun opposition(sign: ZodiacSign): ZodiacSign = ZodiacSign.entries[(sign.ordinal + 6) % 12]

    /** Direction is explicit: the first element generates/controls the second, or vice versa. */
    fun elementRelation(first: FiveElement, second: FiveElement): ElementRelation {
        val a = generationCycle.indexOf(first)
        val b = generationCycle.indexOf(second)
        return when (Math.floorMod(b - a, 5)) {
            0 -> ElementRelation.SAME
            1 -> ElementRelation.GENERATES
            4 -> ElementRelation.GENERATED_BY
            2 -> ElementRelation.CONTROLS
            else -> ElementRelation.CONTROLLED_BY
        }
    }

    /** All five stem variants; year numbers are lunar-year labels, not Jan 1 birthdays. */
    fun yearVariants(sign: ZodiacSign): List<ZodiacYearVariant> = (0..59).filter { it % 12 == sign.ordinal }.map { index ->
        val pair = CanChiId(index % 10, index % 12)
        ZodiacYearVariant(pair, napAm(pair), (1899..2100).filter { Math.floorMod(it - 1984, 60) == index })
    }

    fun forecast(sign: ZodiacSign, date: LocalDate, period: ForecastPeriod): ZodiacForecast {
        val lunar = VietnameseCalendar.solarToLunar(date)
        val window = window(date, period)
        val days = generateSequence(window.start) { it.plusDays(1) }.takeWhile { it <= window.end }.map { day ->
            val pair = VietnameseCalendar.dayCanChiId(day)
            ForecastDay(day, pair, relation(sign, ZodiacSign.entries[pair.branchIndex]))
        }.toList()
        val anchor = when (period) {
            ForecastPeriod.DAY -> VietnameseCalendar.dayCanChiId(date)
            ForecastPeriod.WEEK -> null
            ForecastPeriod.MONTH -> VietnameseCalendar.monthCanChiId(lunar)
            ForecastPeriod.YEAR -> VietnameseCalendar.yearCanChiId(lunar.year)
        }
        val relation = anchor?.let { relation(sign, ZodiacSign.entries[it.branchIndex]) }
        val tone = if (relation == null) {
            val harmony = days.any { it.relation in setOf(BranchRelation.SIX_HARMONY, BranchRelation.TRINE_COMPANION) }
            val opposition = days.any { it.relation == BranchRelation.OPPOSITION }
            when {
                harmony && opposition -> ForecastTone.MIXED
                opposition -> ForecastTone.REVIEW
                harmony -> ForecastTone.COOPERATE
                else -> ForecastTone.BALANCED
            }
        } else when (relation) {
            BranchRelation.SIX_HARMONY, BranchRelation.TRINE_COMPANION -> ForecastTone.COOPERATE
            BranchRelation.OPPOSITION -> ForecastTone.REVIEW
            BranchRelation.SAME -> ForecastTone.CONSOLIDATE
            BranchRelation.ORDINARY -> ForecastTone.BALANCED
        }
        return ZodiacForecast(sign, period, date, window, anchor, relation, tone, days,
            VietnameseCalendar.yearCanChiId(lunar.year), VietnameseCalendar.monthCanChiId(lunar))
    }

    /** Day: civil date. Week: Mon–Sun. Month/year: actual Vietnamese lunar period.
     * A leap month is separate. At the supported limits a partial period is marked. */
    fun window(date: LocalDate, period: ForecastPeriod): ForecastWindow {
        VietnameseCalendar.requireSupported(date)
        if (period == ForecastPeriod.DAY) return ForecastWindow(date, date, false)
        if (period == ForecastPeriod.WEEK) {
            val start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val end = start.plusDays(6)
            return ForecastWindow(maxOf(start, VietnameseCalendar.minDate), minOf(end, VietnameseCalendar.maxDate),
                start < VietnameseCalendar.minDate || end > VietnameseCalendar.maxDate)
        }
        val lunar = VietnameseCalendar.solarToLunar(date)
        fun belongs(day: LocalDate): Boolean {
            val other = VietnameseCalendar.solarToLunar(day)
            return other.year == lunar.year && (period == ForecastPeriod.YEAR || (other.month == lunar.month && other.isLeap == lunar.isLeap))
        }
        var start = date
        var end = date
        while (start > VietnameseCalendar.minDate && belongs(start.minusDays(1))) start = start.minusDays(1)
        while (end < VietnameseCalendar.maxDate && belongs(end.plusDays(1))) end = end.plusDays(1)
        return ForecastWindow(start, end, start == VietnameseCalendar.minDate && VietnameseCalendar.solarToLunar(start).let { it.day != 1 || (period == ForecastPeriod.YEAR && it.month != 1) }
            || end == VietnameseCalendar.maxDate)
    }
}
