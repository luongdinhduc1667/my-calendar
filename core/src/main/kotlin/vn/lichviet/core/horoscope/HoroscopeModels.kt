package vn.lichviet.core.horoscope

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import vn.lichviet.core.LunarDate

/** Order follows the twelve earthly branches, starting at Tý. Labels belong in the UI. */
enum class EarthlyBranch(val index: Int, internal val sourceGlyph: Char) {
    ZI(0, '子'), CHOU(1, '丑'), YIN(2, '寅'), MAO(3, '卯'), CHEN(4, '辰'),
    SI(5, '巳'), WU(6, '午'), WEI(7, '未'), SHEN(8, '申'), YOU(9, '酉'),
    XU(10, '戌'), HAI(11, '亥');

    fun advance(steps: Int): EarthlyBranch = entries[Math.floorMod(index + steps, 12)]

    companion object {
        fun at(index: Int): EarthlyBranch = entries[Math.floorMod(index, 12)]
        internal fun fromSourceGlyph(glyph: Char): EarthlyBranch =
            entries.firstOrNull { it.sourceGlyph == glyph }
                ?: error("Unknown earthly branch in source table: $glyph")
    }
}

enum class HeavenlyStem(val index: Int) {
    JIA(0), YI(1), BING(2), DING(3), WU(4), JI(5), GENG(6), XIN(7), REN(8), GUI(9);

    fun advance(steps: Int): HeavenlyStem = entries[Math.floorMod(index + steps, 10)]
    companion object { fun at(index: Int): HeavenlyStem = entries[Math.floorMod(index, 10)] }
}

enum class TraditionalSex { MALE, FEMALE }
enum class YinYang { YANG, YIN }

/** A civil date remains on that date; EARLY_ZI_NEXT_DAY assigns 23:00–23:59 to tomorrow. */
enum class CivilDayRule { CIVIL_MIDNIGHT, EARLY_ZI_NEXT_DAY }

sealed interface BirthDateInput {
    data class Solar(val date: LocalDate) : BirthDateInput
    data class Lunar(val date: LunarDate) : BirthDateInput
}

/**
 * The sex field is solely the binary parameter used by the selected historical cycle rules.
 * Time is a wall-clock time at the fixed UTC+7 Vietnamese calendar meridian.
 */
data class BirthProfile(
    val date: BirthDateInput,
    val time: LocalTime,
    val traditionalSex: TraditionalSex,
    val utcOffset: ZoneOffset = ZoneOffset.ofHours(7),
    val civilDayRule: CivilDayRule = CivilDayRule.CIVIL_MIDNIGHT,
    val label: String? = null,
)

/** The source orders these roles counterclockwise from Mệnh. */
enum class PalaceRole {
    LIFE, SIBLINGS, SPOUSE, CHILDREN, WEALTH, HEALTH, TRAVEL,
    FRIENDS, CAREER, PROPERTY, FORTUNE, PARENTS,
}

enum class FiveElement(val sourceGlyph: Char) {
    WATER('水'), WOOD('木'), METAL('金'), EARTH('土'), FIRE('火');

    companion object {
        internal fun fromSourceGlyph(glyph: Char): FiveElement =
            entries.firstOrNull { it.sourceGlyph == glyph }
                ?: error("Unknown element in nạp âm table: $glyph")
    }
}

enum class Bureau(val element: FiveElement, val startingVirtualAge: Int) {
    WATER_TWO(FiveElement.WATER, 2),
    WOOD_THREE(FiveElement.WOOD, 3),
    METAL_FOUR(FiveElement.METAL, 4),
    EARTH_FIVE(FiveElement.EARTH, 5),
    FIRE_SIX(FiveElement.FIRE, 6);

    companion object {
        internal fun fromElement(element: FiveElement): Bureau =
            entries.first { it.element == element }
    }
}

sealed interface HoroscopeStarId

enum class MainStar : HoroscopeStarId {
    ZI_WEI, TIAN_JI, TAI_YANG, WU_QU, TIAN_TONG, LIAN_ZHEN,
    TIAN_FU, TAI_YIN, TAN_LANG, JU_MEN, TIAN_XIANG, TIAN_LIANG,
    QI_SHA, PO_JUN,
}

/** Only stars with unambiguous placements in this edition are emitted. */
enum class AuxiliaryStar : HoroscopeStarId {
    WEN_CHANG, WEN_QU, ZUO_FU, YOU_BI, LU_CUN, QING_YANG,
    TUO_LUO, TIAN_MA, TIAN_KONG, DI_JIE,
}

enum class TransformationKind { LU, QUAN, KHOA, JI }

data class StarTransformation(
    val kind: TransformationKind,
    val star: HoroscopeStarId,
    /** Null only when the star's leap-month placement is intentionally unsupported. */
    val branch: EarthlyBranch?,
)

data class MajorCycle(
    val branch: EarthlyBranch,
    val firstVirtualAge: Int,
    val lastVirtualAge: Int,
)

enum class UnsupportedChartFeature {
    LEAP_MONTH_AUXILIARY_PLACEMENT,
    STAR_BRIGHTNESS,
    OTHER_AUXILIARY_STARS,
    PREDICTIVE_INTERPRETATION,
    HISTORICAL_TIME_AND_TRUE_SOLAR_TIME,
}

data class HoroscopePalace(
    val role: PalaceRole,
    val branch: EarthlyBranch,
    val stem: HeavenlyStem,
    val containsBody: Boolean,
    val mainStars: List<MainStar>,
    val auxiliaryStars: List<AuxiliaryStar>,
    val majorCycle: MajorCycle,
)

data class HoroscopeChart(
    val ruleSetId: String,
    val input: BirthProfile,
    val effectiveSolarDate: LocalDate,
    val effectiveLunarDate: LunarDate,
    val birthHour: EarthlyBranch,
    val yearStem: HeavenlyStem,
    val yearBranch: EarthlyBranch,
    val yearYinYang: YinYang,
    val lifeBranch: EarthlyBranch,
    val bodyBranch: EarthlyBranch,
    val bureau: Bureau,
    val palaces: List<HoroscopePalace>,
    val starPlacements: Map<HoroscopeStarId, EarthlyBranch>,
    val transformations: List<StarTransformation>,
    val majorCycles: List<MajorCycle>,
    val minorCycleAgeOneBranch: EarthlyBranch,
    val unsupportedFeatures: Set<UnsupportedChartFeature>,
) {
    fun palaceAt(branch: EarthlyBranch): HoroscopePalace = palaces[branch.index]

    /** Traditional counted age, 1-based. Ages before the bureau starts have no đại hạn. */
    fun majorCycleAtVirtualAge(age: Int): MajorCycle? {
        require(age >= 1) { "Virtual age must be at least 1" }
        return majorCycles.firstOrNull { age in it.firstVirtualAge..it.lastVirtualAge }
    }

    /** Traditional counted age, 1-based. This returns the branch for tiểu hạn. */
    fun minorPalaceAtVirtualAge(age: Int): EarthlyBranch {
        require(age >= 1) { "Virtual age must be at least 1" }
        val step = if (input.traditionalSex == TraditionalSex.MALE) 1 else -1
        return minorCycleAgeOneBranch.advance((age - 1) * step)
    }
}
