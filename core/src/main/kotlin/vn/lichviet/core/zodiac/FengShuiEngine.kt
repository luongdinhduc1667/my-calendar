package vn.lichviet.core.zodiac

import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.floor
import vn.lichviet.core.CanChiId
import vn.lichviet.core.VietnameseCalendar
import vn.lichviet.core.horoscope.CivilDayRule
import vn.lichviet.core.horoscope.FiveElement

enum class Trigram(val lines: Int, val symbol: String, val element: FiveElement) {
    KAN(2, "☵", FiveElement.WATER), GEN(4, "☶", FiveElement.EARTH), ZHEN(1, "☳", FiveElement.WOOD),
    XUN(6, "☴", FiveElement.WOOD), LI(5, "☲", FiveElement.FIRE), KUN(0, "☷", FiveElement.EARTH),
    DUI(3, "☱", FiveElement.METAL), QIAN(7, "☰", FiveElement.METAL);
}
enum class EightHouseStar(val traditionallyFavourable: Boolean) {
    FU_WEI(true), SHENG_QI(true), TIAN_YI(true), YAN_NIAN(true), HUO_HAI(false), WU_GUI(false), LIU_SHA(false), JUE_MING(false),
}
data class CompassMountain(val index: Int, val centerDegrees: Int, val trigram: Trigram, val element: FiveElement)
data class HouseDirection(val trigram: Trigram, val star: EightHouseStar)
data class HouseCompass(val facing: CompassMountain, val sitting: CompassMountain, val directions: List<HouseDirection>)
data class HourPillar(val effectiveDate: LocalDate, val pair: CanChiId, val auspicious: Boolean)
data class CivilHourSegment(val startHour: Int, val endHour: Int, val pillar: HourPillar)

object FengShuiEngine {
    // 正五行 of the 24 mountains, clockwise with Tý at 0°. Not the Hong Fan or double-mountain table.
    private val mountainElements = listOf(
        FiveElement.WATER, FiveElement.WATER, FiveElement.EARTH, FiveElement.EARTH, FiveElement.WOOD, FiveElement.WOOD,
        FiveElement.WOOD, FiveElement.WOOD, FiveElement.EARTH, FiveElement.WOOD, FiveElement.FIRE, FiveElement.FIRE,
        FiveElement.FIRE, FiveElement.FIRE, FiveElement.EARTH, FiveElement.EARTH, FiveElement.METAL, FiveElement.METAL,
        FiveElement.METAL, FiveElement.METAL, FiveElement.EARTH, FiveElement.METAL, FiveElement.WATER, FiveElement.WATER,
    )

    /** Clockwise from north; intervals include their lower edge and exclude the upper. */
    fun mountain(degrees: Double): CompassMountain {
        require(degrees.isFinite())
        val normalized = ((degrees % 360) + 360) % 360
        val index = floor((normalized + 7.5) / 15).toInt() % 24
        val trigram = Trigram.entries[floor((normalized + 22.5) / 45).toInt() % 8]
        return CompassMountain(index, index * 15, trigram, mountainElements[index])
    }

    /** Đại Du Niên computed from the three changed lines, bottom line = bit 0. */
    fun houseStar(base: Trigram, direction: Trigram): EightHouseStar = when (base.lines xor direction.lines) {
        0 -> EightHouseStar.FU_WEI
        4 -> EightHouseStar.SHENG_QI
        3 -> EightHouseStar.TIAN_YI
        7 -> EightHouseStar.YAN_NIAN
        1 -> EightHouseStar.HUO_HAI
        6 -> EightHouseStar.WU_GUI
        5 -> EightHouseStar.LIU_SHA
        else -> EightHouseStar.JUE_MING
    }

    /** House trigram is its sitting direction (opposite facing), not an occupant's birth gua. */
    fun houseCompass(facingDegrees: Double): HouseCompass {
        val facing = mountain(facingDegrees)
        val sitting = mountain(facingDegrees + 180)
        return HouseCompass(facing, sitting, Trigram.entries.map { HouseDirection(it, houseStar(sitting.trigram, it)) })
    }

    /** Wall time at UTC+7; explicit early-Zi convention shared with the natal-chart engine. */
    fun hourPillar(date: LocalDate, time: LocalTime, dayRule: CivilDayRule = CivilDayRule.CIVIL_MIDNIGHT): HourPillar {
        VietnameseCalendar.requireSupported(date)
        val effectiveDate = if (dayRule == CivilDayRule.EARLY_ZI_NEXT_DAY && time.hour == 23) date.plusDays(1) else date
        VietnameseCalendar.requireSupported(effectiveDate)
        val day = VietnameseCalendar.dayCanChiId(effectiveDate)
        val branch = ((time.hour + 1) / 2) % 12
        val stem = ((day.stemIndex % 5) * 2 + branch) % 10
        return HourPillar(effectiveDate, CanChiId(stem, branch), VietnameseCalendar.auspiciousHourIds(effectiveDate).any { it.branchIndex == branch })
    }

    /** Tý is split at midnight so 00:xx and 23:xx never silently share a different day's stem. */
    fun civilHours(date: LocalDate, dayRule: CivilDayRule = CivilDayRule.CIVIL_MIDNIGHT): List<CivilHourSegment> =
        listOf(0 to 1).plus((1..21 step 2).map { it to it + 2 }).plus(23 to 24).map { (start, end) ->
            CivilHourSegment(start, end, hourPillar(date, LocalTime.of(start, 0), dayRule))
        }
}
