package vn.lichviet.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import vn.lichviet.app.data.EventEntity
import vn.lichviet.app.data.EventType

object EventColors {
    val palette = listOf(0xFFB94436.toInt(), 0xFF367661.toInt(), 0xFF3269A8.toInt(), 0xFF7952A4.toInt(), 0xFFAF5D12.toInt(), 0xFFAA4370.toInt())
    fun default(type: EventType): Int = when (type) {
        EventType.EVENT -> palette[2]
        EventType.BIRTHDAY -> palette[5]
        EventType.DEATH_ANNIVERSARY -> palette[1]
    }
    fun of(event: EventEntity): Color = Color(event.colorArgb ?: default(event.type))
    /** Select the greater WCAG contrast of black and white for an opaque sRGB color. */
    fun foreground(background: Color): Color = if (background.luminance() > .179f) Color.Black else Color.White
    fun parse(hex: String): Int? = hex.removePrefix("#").takeIf { it.matches(Regex("[0-9a-fA-F]{6}")) }
        ?.toLong(16)?.let { (it or 0xff000000L).toInt() }
    fun hex(color: Int): String = "#%06X".format(color and 0xffffff)
}
