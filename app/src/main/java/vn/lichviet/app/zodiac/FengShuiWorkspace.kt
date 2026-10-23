package vn.lichviet.app.zodiac

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin
import vn.lichviet.app.R
import vn.lichviet.app.ui.CalendarDatePicker
import vn.lichviet.app.ui.CalendarText
import vn.lichviet.core.horoscope.CivilDayRule
import vn.lichviet.core.zodiac.*

@Composable
fun FengShuiWorkspace(initialDate: LocalDate) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val labels = remember(context, configuration) { ZodiacText(context) }
    var tool by rememberSaveable { mutableIntStateOf(0) }
    var picker by rememberSaveable { mutableStateOf<String?>(null) }
    var firstBirth by rememberSaveable { mutableStateOf<String?>(null) }
    var secondBirth by rememberSaveable { mutableStateOf<String?>(null) }
    var dateText by rememberSaveable { mutableStateOf(initialDate.toString()) }
    var earlyZi by rememberSaveable { mutableStateOf(false) }
    var angleText by rememberSaveable { mutableStateOf("") }
    val date = LocalDate.parse(dateText)
    val first = remember(firstBirth) { firstBirth?.let { ZodiacEngine.birthFacts(LocalDate.parse(it)) } }
    val second = remember(secondBirth) { secondBirth?.let { ZodiacEngine.birthFacts(LocalDate.parse(it)) } }
    val dayRule = if (earlyZi) CivilDayRule.EARLY_ZI_NEXT_DAY else CivilDayRule.CIVIL_MIDNIGHT
    val hours = remember(date, dayRule) { runCatching { FengShuiEngine.civilHours(date, dayRule) } }
    val angle = angleText.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..360.0 }
    val compass = remember(angle) { angle?.let(FengShuiEngine::houseCompass) }
    LazyColumn(Modifier.fillMaxSize().testTag("fs_list"), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(stringResource(R.string.fs_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.fs_intro), Modifier.padding(top = 8.dp))
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.fs_elements, R.string.fs_hours, R.string.fs_compass).forEachIndexed { index, title ->
                    FilterChip(tool == index, { tool = index }, { Text(stringResource(title)) }, Modifier.testTag("fs_tool_$index"))
                }
            }
        }
        when (tool) {
            0 -> {
                item {
                    ZodiacCard {
                        Text(stringResource(R.string.fs_elements), style = MaterialTheme.typography.titleLarge)
                        OutlinedButton({ picker = "first" }, Modifier.fillMaxWidth().testTag("fs_first_birth")) { Text(first?.let { CalendarText.shortDate(context, it.solarDate) } ?: stringResource(R.string.fs_first_birth)) }
                        first?.let { BirthFactLines(it, labels) }
                        OutlinedButton({ picker = "second" }, Modifier.fillMaxWidth().testTag("fs_second_birth")) { Text(second?.let { CalendarText.shortDate(context, it.solarDate) } ?: stringResource(R.string.fs_second_birth)) }
                        second?.let { BirthFactLines(it, labels) }
                        if (first != null && second != null) {
                            Text(labels.relation(ZodiacEngine.relation(first.sign, second.sign)), style = MaterialTheme.typography.titleMedium)
                            Text(labels.elementStatement(first.napAm.element, second.napAm.element), fontWeight = FontWeight.Bold, modifier = Modifier.testTag("fs_element_result"))
                        } else Text(stringResource(R.string.fs_select_births))
                        Text(stringResource(R.string.fs_compare_note), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.zd_birth_note), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.fs_element_cycles))
                        CulturalSourceButton(CulturalSource.ELEMENTS_NAP_AM, CulturalSource.BRANCH_RELATIONS)
                    }
                }
            }
            1 -> {
                item {
                    ZodiacCard {
                        Text(stringResource(R.string.fs_hours), style = MaterialTheme.typography.titleLarge)
                        OutlinedButton({ picker = "hour" }, Modifier.fillMaxWidth().testTag("fs_hour_date")) { Text(CalendarText.fullDate(context, date)) }
                        Text(CalendarText.dayCanChi(context, date), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.zd_nap_am, labels.napAm(ZodiacEngine.napAm(vn.lichviet.core.VietnameseCalendar.dayCanChiId(date))),
                            labels.element(ZodiacEngine.napAm(vn.lichviet.core.VietnameseCalendar.dayCanChiId(date)).element)))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(!earlyZi, { earlyZi = false }, { Text(stringResource(R.string.fs_civil_day)) })
                            FilterChip(earlyZi, { earlyZi = true }, { Text(stringResource(R.string.fs_early_zi)) }, Modifier.testTag("fs_early_zi"))
                        }
                        Text(stringResource(R.string.fs_hour_note), style = MaterialTheme.typography.bodySmall)
                        CulturalSourceButton(CulturalSource.STEM_BRANCH_HOURS, CulturalSource.ELEMENTS_NAP_AM, CulturalSource.HOUR_ALMANAC)
                    }
                }
                if (hours.isFailure) item { Text(stringResource(R.string.fs_hour_last_date), color = MaterialTheme.colorScheme.error) }
                else hours.getOrThrow().forEach { hour ->
                    item(key = "hour_${hour.startHour}") {
                        OutlinedCard(Modifier.fillMaxWidth().testTag("fs_hour_${hour.startHour}")) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(stringResource(R.string.fs_hour_range, hour.startHour, hour.endHour), style = MaterialTheme.typography.titleMedium)
                                Text("${labels.canChi(hour.pillar.pair)} · ${stringResource(if (hour.pillar.auspicious) R.string.fs_good_hour else R.string.fs_other_hour)}",
                                    color = if (hour.pillar.auspicious) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                if (hour.pillar.effectiveDate != date) Text(stringResource(R.string.fs_effective_date, CalendarText.shortDate(context, hour.pillar.effectiveDate)))
                            }
                        }
                    }
                }
            }
            else -> {
                item {
                    ZodiacCard {
                        Text(stringResource(R.string.fs_compass), style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(angleText, { value -> if (value.length <= 12) angleText = value }, modifier = Modifier.fillMaxWidth().testTag("fs_facing_angle"),
                            label = { Text(stringResource(R.string.fs_facing)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
                            isError = angleText.isNotBlank() && angle == null)
                        Text(stringResource(R.string.fs_facing_help), style = MaterialTheme.typography.bodySmall)
                        if (angleText.isNotBlank() && angle == null) Text(stringResource(R.string.fs_angle_error), color = MaterialTheme.colorScheme.error)
                        if (angle != null && compass != null) {
                            CompassDiagram(angle)
                            Text(stringResource(R.string.fs_mountain_details), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.fs_sitting, labels.mountain(compass.sitting), labels.mountain(compass.facing)), fontWeight = FontWeight.Bold, modifier = Modifier.testTag("fs_compass_result"))
                            Text(stringResource(R.string.fs_mountain, labels.mountain(compass.sitting), labels.element(compass.sitting.element)))
                            Text(stringResource(R.string.fs_mountain, labels.mountain(compass.facing), labels.element(compass.facing.element)))
                            Text(stringResource(R.string.fs_house, compass.sitting.trigram.symbol, labels.trigram(compass.sitting.trigram)), style = MaterialTheme.typography.titleMedium)
                        }
                        Text(stringResource(R.string.fs_house_note), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.fs_compass_note), style = MaterialTheme.typography.bodySmall)
                        CulturalSourceButton(CulturalSource.COMPASS_EIGHT_HOUSES)
                    }
                }
                if (compass != null) {
                    item { Text(stringResource(R.string.fs_direction_table), style = MaterialTheme.typography.titleLarge) }
                    compass.directions.forEach { direction ->
                        item(key = "direction_${direction.trigram}") {
                            OutlinedCard(Modifier.fillMaxWidth().testTag("fs_direction_${direction.trigram}")) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(stringResource(R.string.fs_direction_result, labels.direction(direction.trigram), labels.trigram(direction.trigram), labels.star(direction.star)), style = MaterialTheme.typography.titleMedium)
                                    Text(stringResource(if (direction.star.traditionallyFavourable) R.string.fs_favourable else R.string.fs_unfavourable),
                                        color = if (direction.star.traditionallyFavourable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    item { Text(stringResource(R.string.fs_eight_houses_note), style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
    picker?.let { field ->
        val initial = (if (field == "first") first?.solarDate else if (field == "second") second?.solarDate else date) ?: initialDate
        CalendarDatePicker(initial, { picker = null }, inputMode = true) { selected ->
            when (field) { "first" -> firstBirth = selected.toString(); "second" -> secondBirth = selected.toString(); else -> dateText = selected.toString() }
            picker = null
        }
    }
}

@Composable
private fun BirthFactLines(facts: BirthYearFacts, labels: ZodiacText) {
    Text("${facts.sign.symbol} ${labels.sign(facts.sign)} · ${labels.canChi(facts.canChi)}", fontWeight = FontWeight.Bold)
    Text(stringResource(R.string.zd_nap_am, labels.napAm(facts.napAm), labels.element(facts.napAm.element)))
}

/** A bearing diagram; the accessible text/table next to it contains every result. */
@Composable
private fun CompassDiagram(facingDegrees: Double) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    Box(Modifier.fillMaxWidth().height(240.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(220.dp)) {
            val center = this.center
            val radius = size.minDimension * .45f
            drawCircle(outline, radius, center, style = Stroke(2.dp.toPx()))
            drawCircle(outline, radius * .72f, center, style = Stroke(1.dp.toPx()))
            for (index in 0 until 24) {
                val radians = Math.toRadians(index * 15.0 - 90)
                val from = radius * if (index % 3 == 0) .8f else .9f
                drawLine(if (index == 0) primary else outline,
                    center + Offset(cos(radians).toFloat() * from, sin(radians).toFloat() * from),
                    center + Offset(cos(radians).toFloat() * radius, sin(radians).toFloat() * radius), 2.dp.toPx())
            }
            val radians = Math.toRadians(facingDegrees - 90)
            val point = center + Offset(cos(radians).toFloat() * radius * .78f, sin(radians).toFloat() * radius * .78f)
            drawLine(primary, center, point, 4.dp.toPx())
            val side = Offset(-sin(radians).toFloat(), cos(radians).toFloat()) * 7.dp.toPx()
            val back = point - Offset(cos(radians).toFloat(), sin(radians).toFloat()) * 16.dp.toPx()
            drawPath(Path().apply { moveTo(point.x, point.y); lineTo((back + side).x, (back + side).y); lineTo((back - side).x, (back - side).y); close() }, primary)
        }
        Text(stringArrayResource(R.array.fs_directions)[0], Modifier.align(Alignment.TopCenter), style = MaterialTheme.typography.labelMedium)
    }
}
