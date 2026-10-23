package vn.lichviet.app.horoscope

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.util.UUID
import vn.lichviet.app.R
import vn.lichviet.core.horoscope.*

enum class ChartExportFormat(val extension: String, val mimeType: String) {
    PNG("png", "image/png"), PDF("pdf", "application/pdf")
}

/** Native, offline document generation. No screenshot or external rendering service receives birth data. */
object ChartExporter {
    fun create(context: Context, profile: HoroscopeProfileEntity, chart: HoroscopeChart, format: ChartExportFormat): File {
        val directory = File(context.cacheDir, "horoscope").apply { check(mkdirs() || isDirectory) }
        // Keep recent Sharesheet grants usable; discard only expired exports from earlier sessions.
        directory.listFiles()?.filter { it.isFile && it.lastModified() < System.currentTimeMillis() - 86_400_000L }
            ?.forEach(File::delete)
        val output = File(directory, "lichviet-chart-${UUID.randomUUID()}.${format.extension}")
        try {
            val labels = HoroscopeText(context)
            if (format == ChartExportFormat.PNG) {
                val bitmap = Bitmap.createBitmap(1600, 2900, Bitmap.Config.ARGB_8888)
                try {
                    val canvas = Canvas(bitmap)
                    val painter = Painter(canvas, labels, 1600f, 2900f)
                    painter.poster(profile, chart)
                    output.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                } finally { bitmap.recycle() }
            } else {
                val pdf = PdfDocument()
                try {
                    val page1 = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
                    Painter(page1.canvas, labels, 595f, 842f).chartPage(profile, chart)
                    pdf.finishPage(page1)
                    val page2 = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, 2).create())
                    Painter(page2.canvas, labels, 595f, 842f).detailPage(profile, chart)
                    pdf.finishPage(page2)
                    output.outputStream().use(pdf::writeTo)
                } finally { pdf.close() }
            }
            return output
        } catch (error: Exception) { output.delete(); throw error }
    }

    private class Painter(val canvas: Canvas, val labels: HoroscopeText, val width: Float, val height: Float) {
        private val ink = Color.rgb(37, 43, 39)
        private val red = Color.rgb(159, 48, 42)
        private val green = Color.rgb(35, 86, 70)
        private val cream = Color.rgb(251, 248, 239)
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private fun background() { canvas.drawColor(cream) }
        private fun text(value: String, x: Float, top: Float, size: Float, maxWidth: Float, color: Int = ink, bold: Boolean = false, maxLines: Int = 100): Float {
            paint.color = color
            paint.textSize = size
            paint.typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
            val lines = wrap(value, maxWidth)
            var baseline = top - paint.fontMetrics.ascent
            lines.take(maxLines).forEach { line -> canvas.drawText(line, x, baseline, paint); baseline += size * 1.32f }
            return top + minOf(lines.size, maxLines) * size * 1.32f
        }
        private fun wrap(value: String, maxWidth: Float): List<String> = buildList {
            value.split('\n').forEach { paragraph ->
                var line = ""
                paragraph.split(' ').forEach { word ->
                    val candidate = if (line.isEmpty()) word else "$line $word"
                    if (paint.measureText(candidate) <= maxWidth) line = candidate
                    else {
                        if (line.isNotEmpty()) add(line)
                        line = word
                        while (paint.measureText(line) > maxWidth && line.length > 1) {
                            val count = paint.breakText(line, true, maxWidth, null).coerceAtLeast(1)
                            add(line.take(count)); line = line.drop(count)
                        }
                    }
                }
                add(line)
            }
        }
        private fun heading(profile: HoroscopeProfileEntity, chart: HoroscopeChart, margin: Float, size: Float): Float {
            var y = text(labels.get(R.string.hs_title), margin, margin, size * 1.6f, width - 2 * margin, red, true)
            y = text(labels.label(profile), margin, y + size * .5f, size * 1.25f, width - 2 * margin, bold = true)
            y = text(labels.birth(profile), margin, y + size * .3f, size, width - 2 * margin)
            y = text("${labels.sexes[TraditionalSex.valueOf(profile.traditionalSex).ordinal]} · ${labels.dayRules[CivilDayRule.valueOf(profile.civilDayRule).ordinal]}", margin, y, size, width - 2 * margin)
            y = text("${labels.get(R.string.hs_effective_date)}: ${labels.solar(chart.effectiveSolarDate)} · ${labels.lunar(chart.effectiveLunarDate)}", margin, y, size, width - 2 * margin)
            return y
        }
        private fun chart(chart: HoroscopeChart, x: Float, y: Float, side: Float, font: Float) {
            val cell = side / 4
            chart.palaces.forEach { palace ->
                val (col, row) = chartCell(palace.branch)
                val rect = RectF(x + col * cell, y + row * cell, x + (col + 1) * cell, y + (row + 1) * cell)
                paint.color = if (palace.role == PalaceRole.LIFE) Color.rgb(244, 228, 214) else cream
                paint.style = Paint.Style.FILL; canvas.drawRect(rect, paint)
                paint.color = Color.rgb(184, 175, 152); paint.style = Paint.Style.STROKE; paint.strokeWidth = side / 1000
                canvas.drawRect(rect, paint); paint.style = Paint.Style.FILL
                var line = text(labels.palaces[palace.role.ordinal], rect.left + font * .5f, rect.top + font * .5f, font, cell - font, red, true, 2)
                line = text(labels.branches[palace.branch.ordinal] + if (palace.containsBody) " · ${labels.get(R.string.hs_body_marker)}" else "", rect.left + font * .5f, line + font * .2f, font * .82f, cell - font, maxLines = 2)
                val stars = palace.mainStars.joinToString("\n", transform = labels::star).ifEmpty { labels.get(R.string.hs_no_main_short) }
                text(stars, rect.left + font * .5f, line + font * .4f, font * .9f, cell - font, green, true, 5)
                text("${palace.majorCycle.firstVirtualAge}–${palace.majorCycle.lastVirtualAge}", rect.left + font * .5f, rect.bottom - font * 1.6f, font * .8f, cell - font)
            }
            var centerY = y + cell * 1.3f
            centerY = text(labels.get(R.string.hs_chart_center), x + cell * 1.16f, centerY, font * 1.1f, cell * 1.68f, red, true)
            centerY = text(labels.get(R.string.hs_life_body, labels.branches[chart.lifeBranch.ordinal], labels.branches[chart.bodyBranch.ordinal]), x + cell * 1.16f, centerY + font, font, cell * 1.68f)
            text(labels.bureaus[chart.bureau.ordinal], x + cell * 1.16f, centerY + font, font, cell * 1.68f, green, true)
        }
        private fun palaceDetails(chart: HoroscopeChart, top: Float, margin: Float, font: Float, columns: Int, rowHeight: Float) {
            val gap = font
            val colWidth = (width - margin * 2 - gap * (columns - 1)) / columns
            val ordered = PalaceRole.entries.map { role -> chart.palaces.single { it.role == role } }
            ordered.forEachIndexed { index, palace ->
                val x = margin + index % columns * (colWidth + gap)
                var y = top + index / columns * rowHeight
                y = text(labels.palace(palace), x, y, font, colWidth, red, true)
                y = text("${labels.get(R.string.hs_main_stars)}: ${palace.mainStars.joinToString(", ", transform = labels::star).ifEmpty { labels.get(R.string.hs_no_stars) }}", x, y + font * .2f, font * .88f, colWidth)
                y = text("${labels.get(R.string.hs_aux_stars)}: ${palace.auxiliaryStars.joinToString(", ", transform = labels::star).ifEmpty { labels.get(R.string.hs_no_stars) }}", x, y, font * .88f, colWidth)
                text(labels.get(R.string.hs_major, palace.majorCycle.firstVirtualAge, palace.majorCycle.lastVirtualAge), x, y, font * .85f, colWidth)
            }
        }
        fun poster(profile: HoroscopeProfileEntity, chart: HoroscopeChart) {
            background()
            val margin = 52f
            val y = heading(profile, chart, margin, 23f)
            chart(chart, margin, y + 25f, width - margin * 2, 28f)
            val detailsTop = y + 25f + width - margin * 2 + 32f
            palaceDetails(chart, detailsTop, margin, 20f, 3, 190f)
            var footer = detailsTop + 4 * 190f + 10f
            footer = text(labels.get(R.string.hs_edition), margin, footer, 19f, width - 2 * margin)
            footer = text(chart.ruleSetId, margin, footer, 16f, width - 2 * margin)
            text(labels.get(R.string.hs_export_caption), margin, footer + 8f, 18f, width - 2 * margin)
        }
        fun chartPage(profile: HoroscopeProfileEntity, chart: HoroscopeChart) {
            background()
            val margin = 24f
            val y = heading(profile, chart, margin, 10f)
            chart(chart, margin, y + 12f, width - 2 * margin, 11f)
            var footer = y + 12f + width - 2 * margin + 12f
            footer = text(labels.get(R.string.hs_edition), margin, footer, 9f, width - 2 * margin)
            footer = text(chart.ruleSetId, margin, footer, 8f, width - 2 * margin)
            text(labels.get(R.string.hs_export_caption), margin, footer + 4f, 9f, width - 2 * margin)
        }
        fun detailPage(profile: HoroscopeProfileEntity, chart: HoroscopeChart) {
            background()
            val margin = 24f
            var y = text(labels.label(profile), margin, margin, 16f, width - 2 * margin, red, true)
            y = text(labels.get(R.string.hs_export_page), margin, y + 4f, 10f, width - 2 * margin)
            palaceDetails(chart, y + 20f, margin, 11f, 2, 92f)
            y += 20f + 6 * 92f
            y = text(labels.get(R.string.hs_transforms), margin, y, 11f, width - 2 * margin, red, true)
            chart.transformations.forEach { transformation ->
                y = text(labels.get(R.string.hs_transform_line, labels.transformations[transformation.kind.ordinal], labels.star(transformation.star), transformation.branch?.let { labels.branches[it.ordinal] } ?: labels.get(R.string.hs_unplaced)), margin, y + 2f, 9f, width - 2 * margin)
            }
            text(labels.get(R.string.hs_scope_note), margin, y + 6f, 9f, width - 2 * margin)
        }
    }
}
