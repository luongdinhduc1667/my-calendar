package vn.lichviet.app.ui.tools

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import vn.lichviet.app.R
import vn.lichviet.app.ui.CalendarText
import vn.lichviet.core.LunarDate
import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate

@Composable
fun ConverterScreen(initialDate: LocalDate, onSelectDate: (LocalDate) -> Unit) {
    val context = LocalContext.current
    var lunarInput by rememberSaveable { mutableStateOf(false) }
    var solarDate by rememberSaveable(initialDate) { mutableStateOf(initialDate) }
    val initialLunar = remember(initialDate) { VietnameseCalendar.solarToLunar(initialDate) }
    var day by rememberSaveable(initialDate) { mutableStateOf(initialLunar.day.toString()) }
    var month by rememberSaveable(initialDate) { mutableStateOf(initialLunar.month.toString()) }
    var year by rememberSaveable(initialDate) { mutableStateOf(initialLunar.year.toString()) }
    var leap by rememberSaveable(initialDate) { mutableStateOf(initialLunar.isLeap) }
    var dateDialog by remember { mutableStateOf(false) }

    val enteredLunar = remember(day, month, year, leap) {
        val d = day.toIntOrNull()
        val m = month.toIntOrNull()
        val y = year.toIntOrNull()
        if (d != null && d in 1..30 && m != null && m in 1..12 && y != null && y in 1899..2100) {
            LunarDate(d, m, y, leap)
        } else null
    }
    val resultDate = remember(lunarInput, solarDate, enteredLunar) {
        if (lunarInput) enteredLunar?.let { runCatching { VietnameseCalendar.lunarToSolar(it) }.getOrNull() }
        else solarDate
    }
    val resultLunar = remember(resultDate) { resultDate?.let(VietnameseCalendar::solarToLunar) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ToolHeading(stringResource(R.string.tool_converter_eyebrow), stringResource(R.string.tool_converter_title),
            stringResource(R.string.tool_converter_description))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !lunarInput, onClick = {
                if (lunarInput) resultDate?.let { solarDate = it }
                lunarInput = false
            }, label = { Text(stringResource(R.string.tool_converter_solar_to_lunar)) })
            FilterChip(selected = lunarInput, onClick = {
                if (!lunarInput) {
                    val current = VietnameseCalendar.solarToLunar(solarDate)
                    day = current.day.toString()
                    month = current.month.toString()
                    year = current.year.toString()
                    leap = current.isLeap
                }
                lunarInput = true
            }, label = { Text(stringResource(R.string.tool_converter_lunar_to_solar)) })
        }
        ToolCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.SyncAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(if (lunarInput) R.string.tool_converter_lunar_input else R.string.tool_converter_solar_input),
                    style = MaterialTheme.typography.titleMedium)
            }
            if (lunarInput) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = day, onValueChange = { day = it.filter(Char::isDigit).take(2) },
                        label = { Text(stringResource(R.string.tool_day)) }, modifier = Modifier.weight(1f), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = month, onValueChange = { month = it.filter(Char::isDigit).take(2) },
                        label = { Text(stringResource(R.string.tool_month)) }, modifier = Modifier.weight(1f), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = year, onValueChange = { year = it.filter(Char::isDigit).take(4) },
                        label = { Text(stringResource(R.string.tool_year)) }, modifier = Modifier.weight(1.3f), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.tool_converter_leap_month), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.tool_converter_leap_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = leap, onCheckedChange = { leap = it })
                }
                if (resultDate == null) {
                    Text(
                        stringResource(if (enteredLunar == null) R.string.tool_converter_invalid_fields else R.string.tool_converter_invalid_date),
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else DateChoice(stringResource(R.string.tool_solar_calendar), solarDate, onClick = { dateDialog = true })
        }
        if (resultDate != null && resultLunar != null) {
            ToolCard {
                Text(stringResource(R.string.tool_converter_result), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(
                    if (lunarInput) CalendarText.shortDate(context, resultDate)
                    else CalendarText.lunarShort(context, resultLunar),
                    style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
                )
                Text(
                    if (lunarInput) CalendarText.fullDate(context, resultDate)
                    else stringResource(R.string.tool_converter_lunar_year, CalendarText.yearCanChi(context, resultLunar.year), resultLunar.year),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DetailLine(stringResource(R.string.tool_day), CalendarText.dayCanChi(context, resultDate))
                DetailLine(stringResource(R.string.tool_converter_lunar_month), CalendarText.monthCanChi(context, resultLunar))
                Button(onClick = { onSelectDate(resultDate) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.tool_converter_view_date), modifier = Modifier.padding(vertical = 6.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        Text(stringResource(R.string.tool_converter_footer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (dateDialog) VietnameseDateDialog(solarDate, onDismiss = { dateDialog = false }, onSelected = {
        solarDate = it
        dateDialog = false
    })
}
