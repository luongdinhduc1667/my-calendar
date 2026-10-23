package vn.lichviet.app.ics

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.lichviet.app.R
import vn.lichviet.app.data.EventEntity
import vn.lichviet.app.data.RepeatRule
import vn.lichviet.app.ui.EventsScreen
import vn.lichviet.core.VietnameseCalendar

/** SAF never requests broad storage access. Import changes are committed by the repository transaction. */
@Composable
fun IcsWorkspace(
    events: List<EventEntity>, externalUri: Uri?, onConsumed: () -> Unit,
    onAdd: () -> Unit, onEdit: (EventEntity) -> Unit,
) {
    val context = LocalContext.current
    val model: IcsViewModel = viewModel()
    val preview by model.preview.collectAsState()
    val choices by model.choices.collectAsState()
    val busy by model.busy.collectAsState()
    val message by model.message.collectAsState()
    val share by model.share.collectAsState()
    var exporting by rememberSaveable { mutableStateOf(false) }
    var selectedIds by rememberSaveable { mutableStateOf(listOf<Long>()) }
    var fromText by rememberSaveable { mutableStateOf(LocalDate.now(VietnameseCalendar.zoneId).toString()) }
    var untilText by rememberSaveable { mutableStateOf(LocalDate.now(VietnameseCalendar.zoneId).plusYears(5).toString()) }
    var rangeEnabled by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(externalUri, busy) {
        externalUri?.takeIf { !busy }?.let { model.load(it); onConsumed() }
    }
    LaunchedEffect(share) {
        share?.let { uri ->
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/calendar"; putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newUri(context.contentResolver, "iCalendar", uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.ics_share)))
            } catch (_: android.content.ActivityNotFoundException) { model.showMessage(context.getString(R.string.ics_export_failed)) }
            model.consumeShare()
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            model.load(it)
        }
    }
    fun range(): Pair<LocalDate?, LocalDate?> {
        if (!rangeEnabled) return null to null
        val from = LocalDate.parse(fromText)
        val until = LocalDate.parse(untilText)
        require(until > from && from >= VietnameseCalendar.minDate && until <= VietnameseCalendar.maxDate.plusDays(1))
        return from to until
    }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")) { uri ->
        if (uri != null) {
            runCatching { range() }.onSuccess { (from, until) -> model.export(selectedIds.toSet(), from, until, uri); exporting = false }
                .onFailure { model.showMessage(context.getString(R.string.ics_range_invalid)) }
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { open.launch(arrayOf("text/calendar", "application/ics", "application/x-ical", "application/octet-stream")) }, enabled = !busy, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.FileOpen, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.ics_import))
            }
            OutlinedButton(onClick = { selectedIds = events.map { it.id }; exporting = true }, enabled = events.isNotEmpty() && !busy, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.IosShare, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.ics_export))
            }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        EventsScreen(events, onAdd, onEdit)
    }
    if (preview != null) AlertDialog(
        onDismissRequest = { if (!busy) model.dismissPreview() },
        title = { Text(stringResource(R.string.ics_preview)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text(stringResource(R.string.ics_preview_help), style = MaterialTheme.typography.bodySmall) }
                items(preview!!.fileDiagnostics) { IcsDiagnosticMessage(it) }
                items(preview!!.items, key = { it.uid }) { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.title.ifBlank { stringResource(R.string.ics_untitled) }, style = MaterialTheme.typography.titleSmall)
                        Text(item.draft.solarDate + if (item.draft.endSolarDate != null) " → ${item.draft.endSolarDate}" else "", style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.ics_uid,item.uid), style = MaterialTheme.typography.labelSmall)
                        item.diagnostics.forEach { IcsDiagnosticMessage(it, item.errors.isNotEmpty()) }
                        if (item.canImport) {
                            val options = if (item.duplicateEventId == null) listOf(IcsImportChoice.SKIP, IcsImportChoice.IMPORT) else listOf(IcsImportChoice.SKIP, IcsImportChoice.UPDATE)
                            if (item.duplicateEventId != null) Text(stringResource(R.string.ics_duplicate), color = MaterialTheme.colorScheme.primary)
                            options.forEach { choice ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val label=stringResource(when(choice) { IcsImportChoice.SKIP -> R.string.ics_skip; IcsImportChoice.IMPORT -> R.string.ics_import_one; IcsImportChoice.UPDATE -> R.string.ics_update })
                                    RadioButton(choices[item.uid] == choice, onClick = { model.choose(item.uid, choice) }, enabled = !busy, modifier=Modifier.semantics{contentDescription=label+": "+item.title})
                                    Text(stringResource(when(choice) { IcsImportChoice.SKIP -> R.string.ics_skip; IcsImportChoice.IMPORT -> R.string.ics_import_one; IcsImportChoice.UPDATE -> R.string.ics_update }))
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = !busy && choices.values.any { it != IcsImportChoice.SKIP }, onClick = {
            model.apply()
        }) { Text(stringResource(R.string.ics_apply)) } },
        dismissButton = { TextButton(enabled = !busy, onClick = { model.dismissPreview() }) { Text(stringResource(R.string.event_cancel)) } },
    )
    if (exporting) AlertDialog(
        onDismissRequest = { if (!busy) exporting = false }, title = { Text(stringResource(R.string.ics_export)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    TextButton(onClick = { selectedIds = if (selectedIds.size == events.size) emptyList() else events.map { it.id } }) { Text(stringResource(R.string.ics_select_all)) }
                }
                items(events, key = { it.id }) { event ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(event.id in selectedIds, onCheckedChange = { selectedIds = if (it) selectedIds + event.id else selectedIds - event.id },modifier=Modifier.semantics{contentDescription=event.title})
                        Text(event.title)
                    }
                }
                item {
                    val rangeLabel=stringResource(R.string.ics_finite_range)
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(rangeEnabled, { rangeEnabled = it },modifier=Modifier.semantics{contentDescription=rangeLabel}); Text(rangeLabel) }
                    Text(stringResource(R.string.ics_lunar_compatibility), style = MaterialTheme.typography.bodySmall)
                    if (rangeEnabled) {
                        OutlinedTextField(fromText, { fromText = it }, label = { Text(stringResource(R.string.ics_range_start)) }, singleLine = true)
                        OutlinedTextField(untilText, { untilText = it }, label = { Text(stringResource(R.string.ics_range_end)) }, singleLine = true)
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = selectedIds.isNotEmpty() && !busy, onClick = {
            if (runCatching { range() }.isFailure) model.showMessage(context.getString(R.string.ics_range_invalid))
            else create.launch("lich-viet.ics")
        }) { Text(stringResource(R.string.ics_save_file)) } },
        dismissButton = {
            Row {
                TextButton(enabled = !busy, onClick = { exporting = false }) { Text(stringResource(R.string.event_cancel)) }
                TextButton(enabled = selectedIds.isNotEmpty() && !busy, onClick = {
                    runCatching { range() }.onSuccess { (from, until) -> model.export(selectedIds.toSet(), from, until, null); exporting = false }
                        .onFailure { model.showMessage(context.getString(R.string.ics_range_invalid)) }
                }) { Text(stringResource(R.string.ics_share)) }
            }
        },
    )
    message?.let { text -> AlertDialog(onDismissRequest = { model.dismissMessage() }, text = { Text(text) }, confirmButton = {
        TextButton(onClick = { model.dismissMessage() }) { Text(stringResource(R.string.event_ok)) }
    }) }
}
