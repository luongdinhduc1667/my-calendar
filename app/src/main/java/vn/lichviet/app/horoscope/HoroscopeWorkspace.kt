package vn.lichviet.app.horoscope

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.Normalizer
import java.util.Locale
import vn.lichviet.app.R
import vn.lichviet.core.horoscope.*

@Composable
fun HoroscopeWorkspace() {
    val context = LocalContext.current
    val factory = remember(context.applicationContext) { HoroscopeViewModel.factory(context.applicationContext) }
    val model: HoroscopeViewModel = viewModel(factory = factory)
    val profiles by model.profiles.collectAsStateWithLifecycle()
    val selection by model.selection.collectAsStateWithLifecycle()
    val operation by model.operation.collectAsStateWithLifecycle()
    val exportReady by model.exportReady.collectAsStateWithLifecycle()
    val notice by model.notice.collectAsStateWithLifecycle()
    val labels = remember(context) { HoroscopeText(context) }
    var editor by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var exportAction by remember { mutableStateOf<String?>(null) }
    // Snapshot the reviewed profile; later selection changes cannot substitute personal data after confirmation.
    var confirmedSelection by remember { mutableStateOf<HoroscopeSelection?>(null) }
    val png = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { model.exportTo(context, it) }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { model.exportTo(context, it) }
    LaunchedEffect(operation.completed) {
        if (operation.completed) { editor = false; deleteId = null; model.resetOperation() }
    }
    LaunchedEffect(exportReady) {
        exportReady?.let { export ->
            model.exportLaunched()
            if (export.share) {
                runCatching {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", export.file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = export.format.mimeType; putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = ClipData.newUri(context.contentResolver, "chart", uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.hs_share_chooser)))
                }.onFailure { model.exportFailed() }
            } else if (export.format == ChartExportFormat.PNG) png.launch("lichviet-chart.png") else pdf.launch("lichviet-chart.pdf")
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.hs_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.hs_profiles), style = MaterialTheme.typography.labelMedium)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            profiles.forEach { profile -> FilterChip(selection?.profile?.id == profile.id, { model.select(profile.id) }, { Text(labels.label(profile)) }) }
            AssistChip(onClick = { model.resetOperation(); editingId = null; editor = true }, label = { Text(stringResource(R.string.hs_new)) },
                leadingIcon = { Icon(Icons.Outlined.Add, null) }, modifier = Modifier.testTag("hs_new_profile"))
        }
        notice?.let { message ->
            Row { Text(stringResource(message), Modifier.weight(1f)); TextButton(model::clearNotice) { Text(stringResource(R.string.hs_confirm)) } }
        }
        if (!editor) operation.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        val selected = selection
        if (selected == null) {
            Text(stringResource(R.string.hs_intro), Modifier.padding(top = 16.dp))
            Text(stringResource(R.string.hs_empty), Modifier.padding(vertical = 16.dp))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton({ model.resetOperation(); editingId = selected.profile.id; editor = true }) {
                    Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp)); Text(stringResource(R.string.hs_edit), Modifier.padding(start = 6.dp))
                }
                TextButton({ deleteId = selected.profile.id }, enabled = !operation.running) {
                    Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(18.dp)); Text(stringResource(R.string.hs_delete), Modifier.padding(start = 6.dp))
                }
            }
            if (operation.running && !editor) LinearProgressIndicator(Modifier.fillMaxWidth())
            HoroscopeContent(selected, profiles, onExport = { action -> confirmedSelection = selected; exportAction = action }, modifier = Modifier.weight(1f))
        }
    }
    if (editor) HoroscopeProfileEditor(profiles.firstOrNull { it.id == editingId }, operation,
        onDismiss = { editor = false; model.resetOperation() }, onSave = model::save)
    deleteId?.let { id ->
        val profile = profiles.firstOrNull { it.id == id }
        if (profile != null) AlertDialog(onDismissRequest = { if (!operation.running) deleteId = null },
            title = { Text(stringResource(R.string.hs_delete)) }, text = { Text(stringResource(R.string.hs_delete_confirm, labels.label(profile))) },
            confirmButton = { TextButton({ model.delete(id) }, enabled = !operation.running) { Text(stringResource(R.string.hs_delete)) } },
            dismissButton = { TextButton({ deleteId = null }, enabled = !operation.running) { Text(stringResource(R.string.hs_cancel)) } })
    }
    exportAction?.let { action ->
        AlertDialog(onDismissRequest = { exportAction = null; confirmedSelection = null },
            title = { Text(stringResource(R.string.hs_export_confirm_title)) }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                confirmedSelection?.profile?.let { Text(labels.label(it), fontWeight = FontWeight.Bold); Text(labels.birth(it)) }
                Text(stringResource(R.string.hs_export_confirm))
            } },
            confirmButton = { TextButton({
                val reviewed = confirmedSelection
                exportAction = null; confirmedSelection = null
                if (reviewed != null) model.prepareExport(context, reviewed, if (action.endsWith("PDF")) ChartExportFormat.PDF else ChartExportFormat.PNG, action.startsWith("SHARE"))
            }, enabled = confirmedSelection != null && !operation.running) { Text(stringResource(R.string.hs_confirm)) } },
            dismissButton = { TextButton({ exportAction = null; confirmedSelection = null }) { Text(stringResource(R.string.hs_cancel)) } })
    }
}

@Composable
private fun HoroscopeContent(selection: HoroscopeSelection, profiles: List<HoroscopeProfileEntity>, onExport: (String) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val labels = remember(context) { HoroscopeText(context) }
    val chart = selection.chart
    var branch by rememberSaveable(selection.profile.id) { mutableIntStateOf(chart.lifeBranch.ordinal) }
    var listMode by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val chosen = chart.palaceAt(EarthlyBranch.at(branch))
    val chartContent: @Composable () -> Unit = {
        HoroscopeCard {
            Text(labels.label(selection.profile), style = MaterialTheme.typography.titleLarge)
            Text(labels.birth(selection.profile))
            Text("${stringResource(R.string.hs_effective_date)}: ${labels.solar(chart.effectiveSolarDate)} · ${labels.lunar(chart.effectiveLunarDate)}", style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.hs_birth_year, labels.stems[chart.yearStem.ordinal], labels.branches[chart.yearBranch.ordinal], labels.polarities[chart.yearYinYang.ordinal]))
            Text(stringResource(R.string.hs_bureau, labels.bureaus[chart.bureau.ordinal], chart.bureau.startingVirtualAge))
            Text(labels.dayRules[selection.profile.let { CivilDayRule.valueOf(it.civilDayRule).ordinal }], style = MaterialTheme.typography.bodySmall)
        }
        ChoiceStrip("", listOf(stringResource(R.string.hs_chart), stringResource(R.string.hs_readable)), if (listMode) 1 else 0) { listMode = it == 1 }
        OutlinedTextField(query, { query = it }, label = { Text(stringResource(R.string.hs_search)) }, modifier = Modifier.fillMaxWidth().testTag("hs_search"), singleLine = true)
        if (listMode) ReadablePalaces(chart, labels, branch, query) { branch = it.ordinal }
        else {
            HoroscopeChartView(chart, EarthlyBranch.at(branch), { branch = it.ordinal })
            if (query.isNotBlank()) ReadablePalaces(chart, labels, branch, query) { branch = it.ordinal }
        }
    }
    val detailsContent: @Composable () -> Unit = {
        HoroscopeCard { HoroscopePalaceDetail(chart, chosen) }
        HoroscopeCard { CycleLookup(chart, labels) }
        HoroscopeCard { StarGlossary(chart, labels, query) }
        HoroscopeCard {
            Text(stringResource(R.string.hs_transforms), style = MaterialTheme.typography.titleMedium)
            chart.transformations.forEach { transformation -> Text(stringResource(R.string.hs_transform_line, labels.transformations[transformation.kind.ordinal], labels.star(transformation.star), transformation.branch?.let { labels.branches[it.ordinal] } ?: stringResource(R.string.hs_unplaced))) }
            Text(stringResource(R.string.hs_transform_note), style = MaterialTheme.typography.bodySmall)
        }
        HoroscopeCard { ProfileComparison(selection, profiles, labels) }
        HoroscopeCard { ExportActions(onExport) }
        HoroscopeCard { SourceNotes(chart) }
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (maxWidth >= 900.dp) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.weight(1.15f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) { chartContent(); Spacer(Modifier.height(20.dp)) }
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) { detailsContent(); Spacer(Modifier.height(20.dp)) }
        } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            chartContent(); detailsContent(); Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
internal fun HoroscopeCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

private fun normalized(value: String) = Normalizer.normalize(value, Normalizer.Form.NFD).replace("\\p{M}".toRegex(), "").lowercase(Locale.ROOT).replace('đ', 'd')

@Composable
private fun ReadablePalaces(chart: HoroscopeChart, labels: HoroscopeText, selected: Int, query: String, onSelect: (EarthlyBranch) -> Unit) {
    val matches = chart.palaces.filter { palace -> normalized(labels.palace(palace) + palace.mainStars.joinToString(transform = labels::star) + palace.auxiliaryStars.joinToString(transform = labels::star)).contains(normalized(query)) }
    if (matches.isEmpty()) Text(stringResource(R.string.hs_no_results))
    matches.forEach { palace ->
        OutlinedCard(Modifier.fillMaxWidth().clickable { onSelect(palace.branch) }.testTag("hs_list_${palace.branch.name}"),
            colors = CardDefaults.outlinedCardColors(containerColor = if (palace.branch.ordinal == selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(labels.palace(palace), style = MaterialTheme.typography.titleMedium)
                Text("${stringResource(R.string.hs_main_stars)}: ${palace.mainStars.joinToString(" · ", transform = labels::star).ifEmpty { stringResource(R.string.hs_no_stars) }}")
                Text("${stringResource(R.string.hs_aux_stars)}: ${palace.auxiliaryStars.joinToString(" · ", transform = labels::star).ifEmpty { stringResource(R.string.hs_no_stars) }}")
                Text(stringResource(R.string.hs_major, palace.majorCycle.firstVirtualAge, palace.majorCycle.lastVirtualAge))
            }
        }
    }
}

@Composable
private fun CycleLookup(chart: HoroscopeChart, labels: HoroscopeText) {
    var age by rememberSaveable { mutableStateOf("30") }
    Text(stringResource(R.string.hs_major_note), style = MaterialTheme.typography.bodyMedium)
    OutlinedTextField(age, { if (it.length <= 3 && it.all(Char::isDigit)) age = it }, label = { Text(stringResource(R.string.hs_age)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    val number = age.toIntOrNull()?.takeIf { it in 1..120 }
    if (number == null) Text(stringResource(R.string.hs_age_invalid), color = MaterialTheme.colorScheme.error)
    else {
        Text(stringResource(R.string.hs_minor, number, labels.branches[chart.minorPalaceAtVirtualAge(number).ordinal]))
        val major = chart.majorCycleAtVirtualAge(number)
        Text(if (major == null) stringResource(R.string.hs_before_major) else stringResource(R.string.hs_major_at_age, number, labels.branches[major.branch.ordinal]))
        Text("安大限訣 · 安小限訣", style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StarGlossary(chart: HoroscopeChart, labels: HoroscopeText, query: String) {
    val all: List<HoroscopeStarId> = MainStar.entries + AuxiliaryStar.entries
    val filtered = all.filter { normalized(labels.star(it)).contains(normalized(query)) }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    Text(stringResource(R.string.hs_dictionary), style = MaterialTheme.typography.titleMedium)
    if (filtered.isEmpty()) Text(stringResource(R.string.hs_no_results))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        filtered.forEach { star ->
            val index = all.indexOf(star)
            FilterChip(selected == index, { selected = index }, { Text(labels.star(star)) })
        }
    }
    val star = all[selected.coerceIn(all.indices)].takeIf { it in filtered } ?: filtered.firstOrNull()
    if (star != null) {
        val placement = chart.starPlacements[star]
        Text(if (placement == null) "${labels.star(star)} · ${stringResource(R.string.hs_unplaced)}"
            else stringResource(R.string.hs_star_at, labels.star(star), labels.palaces[chart.palaceAt(placement).role.ordinal], labels.branches[placement.ordinal]), fontWeight = FontWeight.Bold)
        Text(labels.starNote(star))
    }
}

@Composable
private fun ProfileComparison(current: HoroscopeSelection, profiles: List<HoroscopeProfileEntity>, labels: HoroscopeText) {
    val others = profiles.filter { it.id != current.profile.id }
    var secondId by rememberSaveable(current.profile.id) { mutableStateOf<Long?>(null) }
    Text(stringResource(R.string.hs_compare), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.hs_compare_note), style = MaterialTheme.typography.bodySmall)
    if (others.isEmpty()) Text(stringResource(R.string.hs_compare_min))
    else {
        Text(stringResource(R.string.hs_compare_choose), style = MaterialTheme.typography.labelLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            others.forEach { profile -> FilterChip(secondId == profile.id, { secondId = profile.id }, { Text(labels.label(profile)) }) }
        }
        val other = others.firstOrNull { it.id == secondId }
        val otherChart = remember(other) { other?.let { HoroscopeChartEngine.calculate(HoroscopeProfileValidation.birthProfile(it)) } }
        if (otherChart != null) {
            Text("${labels.label(current.profile)} | ${labels.label(other!!)}", fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.hs_compare_fact, stringResource(R.string.hs_fact_life), labels.branches[current.chart.lifeBranch.ordinal], labels.branches[otherChart.lifeBranch.ordinal]))
            Text(stringResource(R.string.hs_compare_fact, stringResource(R.string.hs_fact_body), labels.branches[current.chart.bodyBranch.ordinal], labels.branches[otherChart.bodyBranch.ordinal]))
            Text(stringResource(R.string.hs_compare_fact, stringResource(R.string.hs_fact_bureau), labels.bureaus[current.chart.bureau.ordinal], labels.bureaus[otherChart.bureau.ordinal]))
            Text(stringResource(R.string.hs_compare_fact, stringResource(R.string.hs_fact_hour), labels.branches[current.chart.birthHour.ordinal], labels.branches[otherChart.birthHour.ordinal]))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExportActions(onExport: (String) -> Unit) {
    Text(stringResource(R.string.hs_export), style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton({ onExport("SAVE_PNG") }) { Text(stringResource(R.string.hs_save_image)) }
        OutlinedButton({ onExport("SAVE_PDF") }) { Text(stringResource(R.string.hs_save_pdf)) }
        OutlinedButton({ onExport("SHARE_PNG") }) { Text(stringResource(R.string.hs_share_image)) }
        OutlinedButton({ onExport("SHARE_PDF") }) { Text(stringResource(R.string.hs_share_pdf)) }
    }
}

@Composable
private fun SourceNotes(chart: HoroscopeChart) {
    val uri = LocalUriHandler.current
    Text(stringResource(R.string.hs_scope), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.hs_intro))
    Text(stringResource(R.string.hs_edition), fontWeight = FontWeight.Bold)
    Text(chart.ruleSetId, style = MaterialTheme.typography.labelSmall)
    Text(stringResource(R.string.hs_scope_note))
    if (UnsupportedChartFeature.LEAP_MONTH_AUXILIARY_PLACEMENT in chart.unsupportedFeatures) Text(stringResource(R.string.hs_leap_warning))
    TextButton({ uri.openUri("https://www.shidianguji.com/zh/book/SDZJ0170/chapter/1jvzooy7dkbhh") }) { Text(stringResource(R.string.hs_source)) }
    TextButton({ uri.openUri("https://www.digital.archives.go.jp/file/1078787") }) { Text(stringResource(R.string.hs_source_catalog)) }
}
