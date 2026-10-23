package vn.lichviet.app.zodiac

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import vn.lichviet.app.R
import vn.lichviet.app.ui.CalendarDatePicker
import vn.lichviet.app.ui.CalendarText
import vn.lichviet.core.VietnameseCalendar
import vn.lichviet.core.zodiac.*

@Composable
fun ZodiacWorkspace(referenceDate: LocalDate, onOpenDate: (LocalDate) -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val labels = remember(context, configuration) { ZodiacText(context) }
    var selected by rememberSaveable { mutableIntStateOf(VietnameseCalendar.yearCanChiId(VietnameseCalendar.solarToLunar(referenceDate).year).branchIndex) }
    var detail by rememberSaveable { mutableStateOf(false) }
    var birth by rememberSaveable { mutableStateOf<String?>(null) }
    var reference by rememberSaveable { mutableStateOf(referenceDate.toString()) }
    var picker by rememberSaveable { mutableStateOf<String?>(null) }
    var periodIndex by rememberSaveable { mutableIntStateOf(0) }
    var showDays by rememberSaveable { mutableStateOf(false) }
    val date = LocalDate.parse(reference)
    val sign = ZodiacSign.entries[selected]
    val birthDate = birth?.let(LocalDate::parse)
    val facts = remember(birthDate) { birthDate?.let(ZodiacEngine::birthFacts) }
    val period = ForecastPeriod.entries[periodIndex]
    val forecast by produceState<Result<ZodiacForecast>?>(null, sign, date, period, detail) {
        value = null
        if (detail) value = withContext(Dispatchers.Default) { runCatching { ZodiacEngine.forecast(sign, date, period) } }
    }
    BackHandler(detail) { detail = false }
    LazyColumn(Modifier.fillMaxSize().testTag("zd_list"), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            if (detail) TextButton({ detail = false }) { Text(stringResource(R.string.zd_all)) }
            Text(if (detail) "${sign.symbol} ${labels.sign(sign)}" else stringResource(R.string.zd_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (!detail) Text(stringResource(R.string.zd_intro), Modifier.padding(top = 8.dp))
        }
        item {
            OutlinedButton({ picker = "birth" }, Modifier.fillMaxWidth().testTag("zd_birth_picker")) {
                Text(facts?.let { CalendarText.shortDate(context, it.solarDate) } ?: stringResource(R.string.zd_birth))
            }
            Text(stringResource(R.string.zd_birth_note), style = MaterialTheme.typography.bodySmall)
        }
        if (!detail) {
            item {
                BoxWithConstraints {
                    val columns = if (maxWidth >= 700.dp) 4 else if (configuration.fontScale >= 1.5f) 2 else 3
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ZodiacSign.entries.chunked(columns).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { animal ->
                                    OutlinedCard(Modifier.weight(1f).clickable {
                                        selected = animal.ordinal
                                        if (facts?.sign != animal) birth = null
                                        detail = true
                                    }.testTag("zd_animal_${animal.name}")) {
                                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(animal.symbol, fontSize = 30.sp)
                                            Text(labels.sign(animal), style = MaterialTheme.typography.titleSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { CulturalSourceButton(CulturalSource.VIETNAMESE_ANIMALS, CulturalSource.ZODIAC_SYMBOLS) }
        } else {
            item {
                ZodiacCard {
                    Text(stringResource(R.string.zd_facts), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.zd_branch_element, labels.element(sign.branchElement), stringArrayResource(R.array.zd_polarities)[sign.polarity.ordinal]))
                    facts?.let {
                        Text(stringResource(R.string.zd_lunar_year, it.lunarDate.year, labels.canChi(it.canChi)))
                        Text(stringResource(R.string.zd_nap_am, labels.napAm(it.napAm), labels.element(it.napAm.element)), fontWeight = FontWeight.Bold)
                        TextButton({ birth = null }) { Text(stringResource(R.string.zd_clear_birth)) }
                    }
                    Text(stringResource(R.string.zd_traits), style = MaterialTheme.typography.titleMedium)
                    Text(stringArrayResource(R.array.zd_traits)[selected])
                    Text(stringResource(R.string.zd_traits_note), style = MaterialTheme.typography.bodySmall)
                    CulturalSourceButton(CulturalSource.ZODIAC_SYMBOLS, CulturalSource.VIETNAMESE_ANIMALS, CulturalSource.ELEMENTS_NAP_AM)
                }
            }
            item {
                ZodiacCard {
                    Text(stringResource(R.string.zd_relations), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.zd_harmony, labels.sign(ZodiacEngine.harmonyPartner(sign))))
                    Text(stringResource(R.string.zd_trine, ZodiacEngine.companions(sign).joinToString(" · ", transform = labels::sign)))
                    Text(stringResource(R.string.zd_opposition, labels.sign(ZodiacEngine.opposition(sign))))
                    Text(stringResource(R.string.zd_relation_note), style = MaterialTheme.typography.bodySmall)
                    CulturalSourceButton(CulturalSource.BRANCH_RELATIONS)
                }
            }
            item {
                ZodiacCard {
                    Text(stringResource(R.string.zd_forecast), style = MaterialTheme.typography.titleLarge)
                    OutlinedButton({ picker = "reference" }, Modifier.fillMaxWidth().testTag("zd_reference_picker")) {
                        Text("${stringResource(R.string.zd_reference_date)} · ${CalendarText.shortDate(context, date)}")
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        stringArrayResource(R.array.zd_periods).forEachIndexed { index, label ->
                            FilterChip(periodIndex == index, { periodIndex = index; showDays = false }, { Text(label) }, Modifier.testTag("zd_period_${ForecastPeriod.entries[index].name}"))
                        }
                    }
                    Text(stringResource(R.string.zd_period_note), style = MaterialTheme.typography.bodySmall)
                    when {
                        forecast == null -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(stringResource(R.string.zd_loading)) }
                        forecast?.isFailure == true -> Text(stringResource(R.string.zd_load_error), color = MaterialTheme.colorScheme.error)
                        else -> forecast?.getOrNull()?.let { result ->
                            Text(stringArrayResource(R.array.zd_tones)[result.tone.ordinal], style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("zd_forecast_tone"))
                            Text(stringResource(R.string.zd_range, CalendarText.shortDate(context, result.window.start), CalendarText.shortDate(context, result.window.end)), Modifier.testTag("zd_forecast_range"))
                            if (result.window.clipped) Text(stringResource(R.string.zd_partial), color = MaterialTheme.colorScheme.tertiary)
                            Text(result.anchor?.let { stringResource(R.string.zd_anchor, labels.canChi(it), labels.relation(result.anchorRelation!!)) } ?: stringResource(R.string.zd_week_anchor))
                            Text(stringResource(R.string.zd_context, labels.canChi(result.yearContext), labels.canChi(result.monthContext)), style = MaterialTheme.typography.bodySmall)
                            Text(stringArrayResource(R.array.zd_readings)[result.tone.ordinal])
                            Text(stringResource(R.string.zd_work), fontWeight = FontWeight.Bold)
                            Text(stringArrayResource(R.array.zd_work_readings)[result.tone.ordinal])
                            Text(stringResource(R.string.zd_relationships), fontWeight = FontWeight.Bold)
                            Text(stringArrayResource(R.array.zd_relationship_readings)[result.tone.ordinal])
                            Text(stringResource(R.string.zd_counts,
                                pluralStringResource(R.plurals.zd_harmony_days, result.harmonyDays.size, result.harmonyDays.size),
                                pluralStringResource(R.plurals.zd_opposition_days, result.oppositionDays.size, result.oppositionDays.size),
                                pluralStringResource(R.plurals.zd_total_days, result.days.size, result.days.size)))
                            Text(stringResource(R.string.zd_editorial_note), style = MaterialTheme.typography.bodySmall)
                            TextButton({ showDays = !showDays }, Modifier.testTag("zd_toggle_days")) { Text(stringResource(if (showDays) R.string.zd_hide_days else R.string.zd_show_days)) }
                            CulturalSourceButton(result.source, CulturalSource.STEM_BRANCH_HOURS, tag = "zd_forecast_sources")
                        }
                    }
                }
            }
            if (showDays) {
                item { Text(stringResource(R.string.zd_day_breakdown), style = MaterialTheme.typography.titleLarge) }
                items(forecast?.getOrNull()?.days.orEmpty(), key = { "day_${it.date}" }) { day ->
                    val description = stringResource(R.string.zd_open_date, CalendarText.fullDate(context, day.date))
                    OutlinedCard(onClick = { onOpenDate(day.date) }, modifier = Modifier.fillMaxWidth().testTag("zd_day_${day.date}").semantics { contentDescription = description }) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${CalendarText.fullDate(context, day.date)} · ${labels.canChi(day.canChi)}", fontWeight = FontWeight.Bold)
                            Text(labels.relation(day.relation), color = if (day.relation == BranchRelation.OPPOSITION) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            item {
                ZodiacCard {
                    Text(stringResource(R.string.zd_variants), style = MaterialTheme.typography.titleLarge)
                    ZodiacEngine.yearVariants(sign).forEach { variant ->
                        Text("${labels.canChi(variant.canChi)} · ${labels.napAm(variant.napAm)}", fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.zd_variant_years, variant.lunarYears.joinToString()), style = MaterialTheme.typography.bodySmall)
                    }
                    CulturalSourceButton(CulturalSource.ELEMENTS_NAP_AM, CulturalSource.STEM_BRANCH_HOURS)
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
    picker?.let { mode ->
        CalendarDatePicker(if (mode == "birth") birthDate ?: date else date, { picker = null }, inputMode = true) { chosen ->
            if (mode == "birth") {
                birth = chosen.toString(); selected = ZodiacEngine.birthFacts(chosen).sign.ordinal; detail = true
            } else reference = chosen.toString()
            showDays = false; picker = null
        }
    }
}

@Composable
internal fun ZodiacCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
