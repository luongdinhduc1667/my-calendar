package vn.lichviet.core.horoscope

import java.time.ZoneOffset
import vn.lichviet.core.LunarDate
import vn.lichviet.core.VietnameseCalendar

/**
 * Deterministic placements from the Ming Nanyangtang 《新鋟希夷陳先生紫微斗數全書》,
 * volume 3. See core/HOROSCOPE_RULES.md for exact sections and source emendations.
 */
object HoroscopeChartEngine {
    const val RULE_SET_ID = "ming-nanyangtang-ziwei-quanshu-vol3-v1"
    private val vietnamOffset = ZoneOffset.ofHours(7)

    // Each character is the nạp âm element for two consecutive sexagenary pairs:
    // 甲子/乙丑, 丙寅/丁卯, ... 壬戌/癸亥. Source: 六十花甲子納音歌.
    private const val NAYIN_ELEMENTS = "金火木土金火水土金木水土火木水金火木土金火水土金木水土火木水"

    // Day 1..30, indexed at day-1. Source: 定金木水火土局 diagrams. The 木三
    // day-5 and 金四 day-30 entries are two documented editorial emendations.
    private val ziWeiByBureau = mapOf(
        Bureau.WATER_TWO to "丑寅寅卯卯辰辰巳巳午午未未申申酉酉戌戌亥亥子子丑丑寅寅卯卯辰",
        Bureau.WOOD_THREE to "辰丑寅巳寅卯午卯辰未辰巳申巳午酉午未戌未申亥申酉子酉戌丑戌亥",
        Bureau.METAL_FOUR to "亥辰丑寅子巳寅卯丑午卯辰寅未辰巳卯申巳午辰酉午未巳戌未申午亥",
        Bureau.EARTH_FIVE to "午亥辰丑寅未子巳寅卯申丑午卯辰酉寅未辰巳戌卯申巳午亥辰酉午未",
        Bureau.FIRE_SIX to "酉午亥辰丑寅戌未子巳寅卯亥申丑午卯辰子酉寅未辰巳丑戌卯申巳午",
    )

    init {
        check(NAYIN_ELEMENTS.length == 30)
        check(ziWeiByBureau.keys == Bureau.entries.toSet())
        check(ziWeiByBureau.values.all { it.length == 30 })
    }

    private val transformationsByStem: Map<HeavenlyStem, List<HoroscopeStarId>> = mapOf(
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

    fun calculate(profile: BirthProfile): HoroscopeChart {
        require(profile.utcOffset == vietnamOffset) {
            "Only birth times at the fixed UTC+7 Vietnamese calendar meridian are supported"
        }
        val declaredSolar = when (val date = profile.date) {
            is BirthDateInput.Solar -> date.date.also(VietnameseCalendar::requireSupported)
            is BirthDateInput.Lunar -> VietnameseCalendar.lunarToSolar(date.date)
                ?: throw IllegalArgumentException("Invalid or unsupported Vietnamese lunar birth date")
        }
        val effectiveSolar = if (
            profile.civilDayRule == CivilDayRule.EARLY_ZI_NEXT_DAY && profile.time.hour == 23
        ) declaredSolar.plusDays(1) else declaredSolar
        VietnameseCalendar.requireSupported(effectiveSolar)
        val lunar = VietnameseCalendar.solarToLunar(effectiveSolar)
        val hour = birthHour(profile.time.hour)
        val yearStem = HeavenlyStem.at(lunar.year + 6)
        val yearBranch = EarthlyBranch.at(lunar.year + 8)
        val yinYang = if (yearStem.index % 2 == 0) YinYang.YANG else YinYang.YIN

        // The source explicitly counts a leap month as the following month for
        // Mệnh/Thân. A leap twelfth month wraps to month one without altering
        // the source lunar year's stem.
        val effectiveMonth = if (lunar.isLeap) lunar.month % 12 + 1 else lunar.month
        val monthStart = EarthlyBranch.YIN.advance(effectiveMonth - 1)
        val life = monthStart.advance(-hour.index)
        val body = monthStart.advance(hour.index)
        val palaceStems = palaceStems(yearStem)
        val bureau = bureauFor(palaceStems.getValue(life), life)
        val placements = linkedMapOf<HoroscopeStarId, EarthlyBranch>()
        placements.putAll(mainStars(bureau, lunar.day))
        placements.putAll(auxiliaryStars(yearStem, yearBranch, hour, lunar))

        val majorDirection = if (
            (yinYang == YinYang.YANG && profile.traditionalSex == TraditionalSex.MALE) ||
            (yinYang == YinYang.YIN && profile.traditionalSex == TraditionalSex.FEMALE)
        ) 1 else -1
        val majorCycles = (0 until 12).map { index ->
            val firstAge = bureau.startingVirtualAge + 10 * index
            MajorCycle(life.advance(index * majorDirection), firstAge, firstAge + 9)
        }
        val majorByBranch = majorCycles.associateBy { it.branch }
        val palaces = EarthlyBranch.entries.map { branch ->
            HoroscopePalace(
                role = PalaceRole.entries[Math.floorMod(life.index - branch.index, 12)],
                branch = branch,
                stem = palaceStems.getValue(branch),
                containsBody = branch == body,
                mainStars = MainStar.entries.filter { placements[it] == branch },
                auxiliaryStars = AuxiliaryStar.entries.filter { placements[it] == branch },
                majorCycle = majorByBranch.getValue(branch),
            )
        }
        val transformations = TransformationKind.entries.mapIndexed { index, kind ->
            val star = transformationsByStem.getValue(yearStem)[index]
            StarTransformation(kind, star, placements[star])
        }
        val unsupported = mutableSetOf(
            UnsupportedChartFeature.STAR_BRIGHTNESS,
            UnsupportedChartFeature.OTHER_AUXILIARY_STARS,
            UnsupportedChartFeature.PREDICTIVE_INTERPRETATION,
            UnsupportedChartFeature.HISTORICAL_TIME_AND_TRUE_SOLAR_TIME,
        )
        if (lunar.isLeap) unsupported += UnsupportedChartFeature.LEAP_MONTH_AUXILIARY_PLACEMENT

        return HoroscopeChart(
            ruleSetId = RULE_SET_ID,
            input = profile,
            effectiveSolarDate = effectiveSolar,
            effectiveLunarDate = lunar,
            birthHour = hour,
            yearStem = yearStem,
            yearBranch = yearBranch,
            yearYinYang = yinYang,
            lifeBranch = life,
            bodyBranch = body,
            bureau = bureau,
            palaces = palaces,
            starPlacements = placements.toMap(),
            transformations = transformations,
            majorCycles = majorCycles,
            minorCycleAgeOneBranch = minorAgeOne(yearBranch),
            unsupportedFeatures = unsupported,
        )
    }

    private fun birthHour(hour: Int): EarthlyBranch =
        EarthlyBranch.at(if (hour == 23) 0 else (hour + 1) / 2)

    private fun palaceStems(yearStem: HeavenlyStem): Map<EarthlyBranch, HeavenlyStem> {
        // 起五寅例: 甲己→丙, 乙庚→戊, 丙辛→庚, 丁壬→壬, 戊癸→甲 at 寅.
        val yinStem = HeavenlyStem.at((yearStem.index % 5) * 2 + 2)
        return EarthlyBranch.entries.associateWith { branch ->
            yinStem.advance(Math.floorMod(branch.index - EarthlyBranch.YIN.index, 12))
        }
    }

    private fun bureauFor(stem: HeavenlyStem, branch: EarthlyBranch): Bureau {
        val sexagenaryIndex = (0 until 6).map { stem.index + it * 10 }
            .singleOrNull { it % 12 == branch.index }
            ?: error("Palace stem and branch do not form a sexagenary pair")
        val element = FiveElement.fromSourceGlyph(NAYIN_ELEMENTS[sexagenaryIndex / 2])
        return Bureau.fromElement(element)
    }

    private fun mainStars(bureau: Bureau, lunarDay: Int): Map<MainStar, EarthlyBranch> {
        val ziWei = ziWeiBranch(bureau, lunarDay)
        // 安天府圖 mirrors 紫微 over the 寅–申 axis (both stars coincide there).
        val tianFu = EarthlyBranch.at(4 - ziWei.index)
        return linkedMapOf(
            MainStar.ZI_WEI to ziWei,
            MainStar.TIAN_JI to ziWei.advance(-1),
            MainStar.TAI_YANG to ziWei.advance(-3),
            MainStar.WU_QU to ziWei.advance(-4),
            MainStar.TIAN_TONG to ziWei.advance(-5),
            MainStar.LIAN_ZHEN to ziWei.advance(-8),
            MainStar.TIAN_FU to tianFu,
            MainStar.TAI_YIN to tianFu.advance(1),
            MainStar.TAN_LANG to tianFu.advance(2),
            MainStar.JU_MEN to tianFu.advance(3),
            MainStar.TIAN_XIANG to tianFu.advance(4),
            MainStar.TIAN_LIANG to tianFu.advance(5),
            MainStar.QI_SHA to tianFu.advance(6),
            MainStar.PO_JUN to tianFu.advance(10),
        )
    }

    /** Package-internal lookup, exposed to tests for the two emended source cells. */
    internal fun ziWeiBranch(bureau: Bureau, lunarDay: Int): EarthlyBranch {
        require(lunarDay in 1..30)
        return EarthlyBranch.fromSourceGlyph(ziWeiByBureau.getValue(bureau)[lunarDay - 1])
    }

    private fun auxiliaryStars(
        yearStem: HeavenlyStem,
        yearBranch: EarthlyBranch,
        hour: EarthlyBranch,
        lunar: LunarDate,
    ): Map<AuxiliaryStar, EarthlyBranch> {
        val luCun = when (yearStem) {
            HeavenlyStem.JIA -> EarthlyBranch.YIN
            HeavenlyStem.YI -> EarthlyBranch.MAO
            HeavenlyStem.BING, HeavenlyStem.WU -> EarthlyBranch.SI
            HeavenlyStem.DING, HeavenlyStem.JI -> EarthlyBranch.WU
            HeavenlyStem.GENG -> EarthlyBranch.SHEN
            HeavenlyStem.XIN -> EarthlyBranch.YOU
            HeavenlyStem.REN -> EarthlyBranch.HAI
            HeavenlyStem.GUI -> EarthlyBranch.ZI
        }
        val tianMa = when (yearBranch) {
            EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU -> EarthlyBranch.SHEN
            EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN -> EarthlyBranch.YIN
            EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU -> EarthlyBranch.HAI
            EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI -> EarthlyBranch.SI
        }
        return linkedMapOf<AuxiliaryStar, EarthlyBranch>().apply {
            put(AuxiliaryStar.WEN_CHANG, EarthlyBranch.XU.advance(-hour.index))
            put(AuxiliaryStar.WEN_QU, EarthlyBranch.CHEN.advance(hour.index))
            put(AuxiliaryStar.LU_CUN, luCun)
            put(AuxiliaryStar.QING_YANG, luCun.advance(1))
            put(AuxiliaryStar.TUO_LUO, luCun.advance(-1))
            put(AuxiliaryStar.TIAN_MA, tianMa)
            put(AuxiliaryStar.DI_JIE, EarthlyBranch.HAI.advance(hour.index))
            put(AuxiliaryStar.TIAN_KONG, EarthlyBranch.HAI.advance(-hour.index))
            // This edition states that leap months count as the following month
            // only in 安身命例. It does not settle month-based auxiliary stars.
            if (!lunar.isLeap) {
                put(AuxiliaryStar.ZUO_FU, EarthlyBranch.CHEN.advance(lunar.month - 1))
                put(AuxiliaryStar.YOU_BI, EarthlyBranch.XU.advance(1 - lunar.month))
            }
        }
    }

    private fun minorAgeOne(yearBranch: EarthlyBranch): EarthlyBranch = when (yearBranch) {
        EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU -> EarthlyBranch.CHEN
        EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN -> EarthlyBranch.XU
        EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU -> EarthlyBranch.WEI
        EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI -> EarthlyBranch.CHOU
    }
}
