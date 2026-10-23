package vn.lichviet.app.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.lichviet.app.R
import vn.lichviet.app.ui.CalendarText
import vn.lichviet.core.HolidayOccurrenceId
import vn.lichviet.core.LunarDate
import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate
import java.time.YearMonth

private data class HolidayOccurrence(val date: LocalDate, val names: List<HolidayOccurrenceId>, val lunar: LunarDate)
private data class HolidayYear(val year: Int, val occurrences: List<HolidayOccurrence>)

@Composable
fun HolidaysScreen(year: Int, onSelectDate: (LocalDate) -> Unit) {
    val context = LocalContext.current
    var shownYear by rememberSaveable(year) { mutableIntStateOf(year.coerceIn(1900, 2100)) }
    var yearDialog by remember { mutableStateOf(false) }
    var enteredYear by rememberSaveable { mutableStateOf(year.toString()) }
    val today = remember { LocalDate.now(VietnameseCalendar.zoneId) }
    val loadedYear by produceState<HolidayYear?>(initialValue = null, shownYear) {
        val requestedYear = shownYear
        value = withContext(Dispatchers.Default) {
            val first = LocalDate.of(requestedYear, 1, 1)
            val occurrences = (0 until first.lengthOfYear()).mapNotNull { offset ->
                val date = first.plusDays(offset.toLong())
                val data = VietnameseCalendar.dayData(date)
                if (data.holidays.isNotEmpty()) HolidayOccurrence(date, data.holidays, data.lunar) else null
            }
            HolidayYear(requestedYear, occurrences)
        }
    }
    // Never show the previous year's data while a newly selected year is loading.
    val holidays = loadedYear?.takeIf { it.year == shownYear }?.occurrences
    val nextHoliday = remember(holidays, today) { holidays?.firstOrNull { !it.date.isBefore(today) } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item(key = "heading") {
            ToolHeading(stringResource(R.string.tool_holidays_eyebrow), stringResource(R.string.tool_holidays_title),
                stringResource(R.string.tool_holidays_description))
        }
        item(key = "year_selector") {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { shownYear-- }, enabled = shownYear > 1900) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.tool_holidays_previous_year))
                }
                TextButton(onClick = { enteredYear = shownYear.toString(); yearDialog = true }) {
                    Text(stringResource(R.string.tool_holidays_year, shownYear), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { shownYear++ }, enabled = shownYear < 2100) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.tool_holidays_next_year))
                }
            }
        }
        if (holidays == null) item(key = "loading") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.tool_holidays_loading, shownYear), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (nextHoliday != null) item(key = "next_holiday_${nextHoliday.date}") {
            ToolCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.tool_holidays_coming), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Text(nextHoliday.names.joinToString(" · ") { CalendarText.holiday(context, it) }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(CalendarText.fullDate(context, nextHoliday.date), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { onSelectDate(nextHoliday.date) }, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.tool_view_on_calendar))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        holidays?.groupBy { it.date.monthValue }?.forEach { (month, occurrences) ->
            item(key = "month_${shownYear}_$month") {
                Text(CalendarText.monthYear(context, YearMonth.of(shownYear, month)).uppercase(CalendarText.locale(context)),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            }
            items(occurrences, key = { "holiday_${it.date}" }) { holiday ->
                ToolCard {
                    val lunar = holiday.lunar
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 4.dp)) {
                            Text(holiday.date.dayOfMonth.toString().padStart(2, '0'), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("/$month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            holiday.names.forEach { name -> Text(CalendarText.holiday(context, name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                            Text(stringResource(R.string.tool_lunar_date_with_year, CalendarText.lunarShort(context, lunar), lunar.year),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { onSelectDate(holiday.date) }, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.tool_view_day)) }
                        }
                    }
                }
            }
        }
        if (holidays != null && holidays.isEmpty()) item(key = "empty") {
            Text(stringResource(R.string.tool_holidays_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item(key = "calendar_note") {
            Text(stringResource(R.string.tool_holidays_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (yearDialog) {
        val validYear = enteredYear.toIntOrNull()?.takeIf { it in 1900..2100 }
        AlertDialog(
            onDismissRequest = { yearDialog = false },
            title = { Text(stringResource(R.string.tool_holidays_choose_year)) },
            text = {
                OutlinedTextField(
                    value = enteredYear,
                    onValueChange = { enteredYear = it.filter(Char::isDigit).take(4) },
                    label = { Text(stringResource(R.string.tool_holidays_solar_year)) },
                    supportingText = { Text(stringResource(R.string.tool_holidays_year_range)) },
                    isError = validYear == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = { validYear?.let { shownYear = it; yearDialog = false } }, enabled = validYear != null) { Text(stringResource(R.string.tool_holidays_view_year)) }
            },
            dismissButton = { TextButton(onClick = { yearDialog = false }) { Text(stringResource(R.string.tool_cancel)) } },
        )
    }
}
