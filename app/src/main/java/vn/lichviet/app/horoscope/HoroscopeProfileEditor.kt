package vn.lichviet.app.horoscope

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import vn.lichviet.app.R
import vn.lichviet.core.LunarDate
import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChoiceStrip(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { index, value -> FilterChip(selected == index, { onSelect(index) }, { Text(value) }) }
        }
    }
}

@Composable
internal fun HoroscopeProfileEditor(
    profile: HoroscopeProfileEntity?, operation: HoroscopeOperation,
    onDismiss: () -> Unit, onSave: (HoroscopeProfileEntity) -> Unit,
) {
    val original = profile ?: HoroscopeProfileEntity()
    var label by rememberSaveable(original.id) { mutableStateOf(original.label) }
    var group by rememberSaveable(original.id) { mutableIntStateOf(listOf("SELF", "FAMILY", "FRIEND").indexOf(original.group)) }
    var kind by rememberSaveable(original.id) { mutableStateOf(original.dateKind) }
    var day by rememberSaveable(original.id) { mutableStateOf(original.day.toString()) }
    var month by rememberSaveable(original.id) { mutableStateOf(original.month.toString()) }
    var year by rememberSaveable(original.id) { mutableStateOf(original.year.toString()) }
    var leap by rememberSaveable(original.id) { mutableStateOf(original.isLeap) }
    var hour by rememberSaveable(original.id) { mutableStateOf(original.hour.toString()) }
    var minute by rememberSaveable(original.id) { mutableStateOf(original.minute.toString()) }
    var sex by rememberSaveable(original.id) { mutableIntStateOf(if (original.traditionalSex == "MALE") 0 else 1) }
    var rule by rememberSaveable(original.id) { mutableIntStateOf(if (original.civilDayRule == "CIVIL_MIDNIGHT") 0 else 1) }
    var invalid by rememberSaveable { mutableStateOf(false) }
    Dialog(onDismissRequest = { if (!operation.running) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(16.dp).widthIn(max = 640.dp).fillMaxWidth().fillMaxHeight(.94f), shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(if (profile == null) R.string.hs_new else R.string.hs_edit), style = MaterialTheme.typography.headlineSmall)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(label, { if (it.length <= 80) label = it }, Modifier.fillMaxWidth().testTag("hs_profile_name"),
                        label = { Text(stringResource(R.string.hs_label)) }, singleLine = true, enabled = !operation.running)
                    ChoiceStrip(stringResource(R.string.hs_group), stringArrayResource(R.array.hs_groups).toList(), group) { group = it }
                    ChoiceStrip(stringResource(R.string.hs_date_kind), listOf(stringResource(R.string.hs_solar), stringResource(R.string.hs_lunar)), if (kind == "SOLAR") 0 else 1) { index ->
                        val next = if (index == 0) "SOLAR" else "LUNAR"
                        if (next != kind) {
                            runCatching {
                                if (next == "LUNAR") {
                                    VietnameseCalendar.solarToLunar(LocalDate.of(year.toInt(), month.toInt(), day.toInt())).also {
                                        day = it.day.toString(); month = it.month.toString(); year = it.year.toString(); leap = it.isLeap
                                    }
                                } else {
                                    requireNotNull(VietnameseCalendar.lunarToSolar(LunarDate(day.toInt(), month.toInt(), year.toInt(), leap))).also {
                                        day = it.dayOfMonth.toString(); month = it.monthValue.toString(); year = it.year.toString(); leap = false
                                    }
                                }
                            }
                            kind = next
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumericInput(day, { day = it; invalid = false }, R.string.hs_day, "hs_profile_day", Modifier.weight(1f), 2)
                        NumericInput(month, { month = it; invalid = false }, R.string.hs_month, "hs_profile_month", Modifier.weight(1f), 2)
                        NumericInput(year, { year = it; invalid = false }, R.string.hs_year, "hs_profile_year", Modifier.weight(1.4f), 4)
                    }
                    if (kind == "LUNAR") Row(Modifier.fillMaxWidth()
                        .semantics(mergeDescendants = true) { }
                        .toggleable(value = leap, enabled = !operation.running, role = Role.Switch,
                            onValueChange = { leap = it; invalid = false })
                        .testTag("hs_profile_leap"), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.hs_leap), Modifier.weight(1f))
                        Switch(leap, onCheckedChange = null, enabled = !operation.running)
                    }
                    Text(stringResource(R.string.hs_range), style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumericInput(hour, { hour = it; invalid = false }, R.string.hs_hour, "hs_profile_hour", Modifier.weight(1f), 2)
                        NumericInput(minute, { minute = it; invalid = false }, R.string.hs_minute, "hs_profile_minute", Modifier.weight(1f), 2)
                    }
                    Text(stringResource(R.string.hs_zone_note), style = MaterialTheme.typography.bodyMedium)
                    ChoiceStrip(stringResource(R.string.hs_sex), stringArrayResource(R.array.hs_sexes).toList(), sex) { sex = it }
                    Text(stringResource(R.string.hs_sex_note), style = MaterialTheme.typography.bodySmall)
                    ChoiceStrip(stringResource(R.string.hs_day_rule), stringArrayResource(R.array.hs_day_rules).toList(), rule) { rule = it }
                    Text(stringResource(R.string.hs_day_rule_note), style = MaterialTheme.typography.bodySmall)
                }
                val error = if (invalid) R.string.hs_invalid else operation.error
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("hs_profile_error")) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onDismiss, enabled = !operation.running) { Text(stringResource(R.string.hs_cancel)) }
                    Button(onClick = {
                        val value = original.copy(label = label, group = listOf("SELF", "FAMILY", "FRIEND")[group], dateKind = kind,
                            day = day.toIntOrNull() ?: 0, month = month.toIntOrNull() ?: 0, year = year.toIntOrNull() ?: 0,
                            isLeap = kind == "LUNAR" && leap, hour = hour.toIntOrNull() ?: -1, minute = minute.toIntOrNull() ?: -1,
                            traditionalSex = if (sex == 0) "MALE" else "FEMALE", civilDayRule = if (rule == 0) "CIVIL_MIDNIGHT" else "EARLY_ZI_NEXT_DAY")
                        if (runCatching { HoroscopeProfileValidation.validated(value) }.isSuccess) onSave(value) else invalid = true
                    }, enabled = !operation.running, modifier = Modifier.weight(1f).testTag("hs_profile_save")) {
                        if (operation.running) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.hs_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun NumericInput(value: String, change: (String) -> Unit, label: Int, tag: String, modifier: Modifier, max: Int) {
    OutlinedTextField(value, { if (it.length <= max && it.all(Char::isDigit)) change(it) }, modifier.testTag(tag),
        label = { Text(stringResource(label)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
}
