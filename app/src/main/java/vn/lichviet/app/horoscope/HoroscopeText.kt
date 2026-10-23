package vn.lichviet.app.horoscope

import android.content.Context
import vn.lichviet.app.R
import vn.lichviet.app.ui.CalendarText
import vn.lichviet.core.LunarDate
import vn.lichviet.core.horoscope.*

/** Resource-backed names are shared by Compose, the accessible list and exported files. */
class HoroscopeText(private val context: Context) {
    val branches = context.resources.getStringArray(R.array.hs_branches)
    val stems = context.resources.getStringArray(R.array.hs_stems)
    val palaces = context.resources.getStringArray(R.array.hs_palaces)
    val topics = context.resources.getStringArray(R.array.hs_palace_topics)
    private val main = context.resources.getStringArray(R.array.hs_main_names)
    private val auxiliary = context.resources.getStringArray(R.array.hs_aux_names)
    val bureaus = context.resources.getStringArray(R.array.hs_bureaus)
    val sexes = context.resources.getStringArray(R.array.hs_sexes)
    val dayRules = context.resources.getStringArray(R.array.hs_day_rules)
    val transformations = context.resources.getStringArray(R.array.hs_transformations)
    val polarities = context.resources.getStringArray(R.array.hs_polarities)
    fun get(id: Int, vararg args: Any): String = context.getString(id, *args)
    fun label(profile: HoroscopeProfileEntity) = profile.label.ifBlank { get(R.string.hs_unnamed) }
    fun star(id: HoroscopeStarId): String = when (id) {
        is MainStar -> main[id.ordinal]
        is AuxiliaryStar -> auxiliary[id.ordinal]
    }
    fun lunar(date: LunarDate) = get(R.string.hs_lunar_date, date.day, date.month, date.year,
        if (date.isLeap) get(R.string.hs_leap_suffix) else "")
    fun solar(date: java.time.LocalDate) = CalendarText.shortDate(context, date)
    fun birth(profile: HoroscopeProfileEntity): String {
        val date = if (profile.dateKind == "LUNAR") {
            lunar(LunarDate(profile.day, profile.month, profile.year, profile.isLeap))
        } else CalendarText.shortDate(context, java.time.LocalDate.of(profile.year, profile.month, profile.day))
        return get(R.string.hs_birth_summary,
            "${get(if (profile.dateKind == "LUNAR") R.string.hs_lunar else R.string.hs_solar)} $date",
            CalendarText.time(context, java.time.LocalTime.of(profile.hour, profile.minute)))
    }
    fun palace(palace: HoroscopePalace): String = get(R.string.hs_palace_label,
        palaces[palace.role.ordinal], stems[palace.stem.ordinal], branches[palace.branch.ordinal],
        if (palace.containsBody) " · ${get(R.string.hs_body_marker)}" else "")
    fun starNote(star: HoroscopeStarId): String = get(when (star) {
        is MainStar -> R.string.hs_star_main_note
        AuxiliaryStar.WEN_CHANG, AuxiliaryStar.WEN_QU -> R.string.hs_star_wen_note
        AuxiliaryStar.ZUO_FU, AuxiliaryStar.YOU_BI -> R.string.hs_star_fu_note
        AuxiliaryStar.LU_CUN, AuxiliaryStar.QING_YANG, AuxiliaryStar.TUO_LUO -> R.string.hs_star_lu_note
        AuxiliaryStar.TIAN_MA -> R.string.hs_star_ma_note
        AuxiliaryStar.TIAN_KONG, AuxiliaryStar.DI_JIE -> R.string.hs_star_kong_note
    })
}

/** Traditional ring, top left 巳 through clockwise to 辰; center 2×2 remains informational. */
internal fun chartCell(branch: EarthlyBranch): Pair<Int, Int> = when (branch) {
    EarthlyBranch.SI -> 0 to 0; EarthlyBranch.WU -> 1 to 0
    EarthlyBranch.WEI -> 2 to 0; EarthlyBranch.SHEN -> 3 to 0
    EarthlyBranch.YOU -> 3 to 1; EarthlyBranch.XU -> 3 to 2
    EarthlyBranch.HAI -> 3 to 3; EarthlyBranch.ZI -> 2 to 3
    EarthlyBranch.CHOU -> 1 to 3; EarthlyBranch.YIN -> 0 to 3
    EarthlyBranch.MAO -> 0 to 2; EarthlyBranch.CHEN -> 0 to 1
}
