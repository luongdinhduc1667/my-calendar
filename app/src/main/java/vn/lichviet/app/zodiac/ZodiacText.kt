package vn.lichviet.app.zodiac

import android.content.Context
import vn.lichviet.app.R
import vn.lichviet.app.ui.CalendarText
import vn.lichviet.core.CanChiId
import vn.lichviet.core.horoscope.FiveElement
import vn.lichviet.core.zodiac.*

/** All calculated results carry IDs; changing language never changes a calculation. */
internal class ZodiacText(private val context: Context) {
    private fun array(id: Int, index: Int) = context.resources.getStringArray(id)[index]
    fun sign(sign: ZodiacSign) = "${context.resources.getStringArray(R.array.calendar_branches)[sign.ordinal]} · ${array(R.array.zd_animals, sign.ordinal)}"
    fun animal(sign: ZodiacSign) = array(R.array.zd_animals, sign.ordinal)
    fun element(element: FiveElement) = array(R.array.zd_elements, element.ordinal)
    fun napAm(value: NapAm) = array(R.array.zd_nap_am, value.ordinal)
    fun relation(value: BranchRelation) = array(R.array.zd_branch_relations, value.ordinal)
    fun canChi(value: CanChiId) = CalendarText.canChi(context, value)
    fun trigram(value: Trigram) = array(R.array.fs_trigrams, value.ordinal)
    fun direction(value: Trigram) = array(R.array.fs_directions, value.ordinal)
    fun mountain(value: CompassMountain) = array(R.array.fs_mountains, value.index)
    fun star(value: EightHouseStar) = array(R.array.fs_stars, value.ordinal)
    fun elementStatement(first: FiveElement, second: FiveElement): String = context.getString(when (ZodiacEngine.elementRelation(first, second)) {
        ElementRelation.SAME -> R.string.fs_element_same
        ElementRelation.GENERATES -> R.string.fs_element_generates
        ElementRelation.GENERATED_BY -> R.string.fs_element_generated_by
        ElementRelation.CONTROLS -> R.string.fs_element_controls
        ElementRelation.CONTROLLED_BY -> R.string.fs_element_controlled_by
    }, element(first), element(second))
}
