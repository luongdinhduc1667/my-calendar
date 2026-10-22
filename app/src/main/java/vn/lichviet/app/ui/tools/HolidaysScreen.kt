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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.lichviet.core.LunarDate
import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate

private data class HolidayOccurrence(val date: LocalDate, val names: List<String>, val lunar: LunarDate)
private data class HolidayYear(val year: Int, val occurrences: List<HolidayOccurrence>)

@Composable
fun HolidaysScreen(year: Int, onSelectDate: (LocalDate) -> Unit) {
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
                val names = VietnameseCalendar.holidays(date)
                if (names.isNotEmpty()) HolidayOccurrence(date, names, VietnameseCalendar.solarToLunar(date)) else null
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
            ToolHeading("Gìn giữ những ngày ý nghĩa", "Lễ Tết & kỷ niệm", "Ngày lễ dương lịch, lễ Tết, mùng một và ngày rằm theo lịch Việt.")
        }
        item(key = "year_selector") {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { shownYear-- }, enabled = shownYear > 1900) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Năm trước")
                }
                TextButton(onClick = { enteredYear = shownYear.toString(); yearDialog = true }) {
                    Text("Năm $shownYear", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { shownYear++ }, enabled = shownYear < 2100) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Năm sau")
                }
            }
        }
        if (holidays == null) item(key = "loading") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Đang tính ngày lễ năm $shownYear…", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (nextHoliday != null) item(key = "next_holiday_${nextHoliday.date}") {
            ToolCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("SẮP ĐẾN", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Text(nextHoliday.names.joinToString(" · "), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(nextHoliday.date.vietnameseFullDate(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { onSelectDate(nextHoliday.date) }, modifier = Modifier.align(Alignment.End)) {
                    Text("Xem trên lịch")
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        holidays?.groupBy { it.date.monthValue }?.forEach { (month, occurrences) ->
            item(key = "month_${shownYear}_$month") {
                Text("THÁNG ${month.toString().padStart(2, '0')}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
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
                            holiday.names.forEach { name -> Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                            Text("Âm lịch ${lunar.day}/${lunar.month}${if (lunar.isLeap) " nhuận" else ""} · ${lunar.year}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { onSelectDate(holiday.date) }, modifier = Modifier.align(Alignment.End)) { Text("Xem ngày") }
                        }
                    }
                }
            }
        }
        if (holidays != null && holidays.isEmpty()) item(key = "empty") {
            Text("Chưa có ngày lễ trong năm được chọn.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item(key = "calendar_note") {
            Text("Các ngày âm lịch được đổi theo múi giờ Việt Nam. Danh sách thể hiện ngày lễ và kỷ niệm; không bao gồm lịch nghỉ bù từng năm.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (yearDialog) {
        val validYear = enteredYear.toIntOrNull()?.takeIf { it in 1900..2100 }
        AlertDialog(
            onDismissRequest = { yearDialog = false },
            title = { Text("Chọn năm") },
            text = {
                OutlinedTextField(
                    value = enteredYear,
                    onValueChange = { enteredYear = it.filter(Char::isDigit).take(4) },
                    label = { Text("Năm dương lịch") },
                    supportingText = { Text("Từ 1900 đến 2100") },
                    isError = validYear == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = { validYear?.let { shownYear = it; yearDialog = false } }, enabled = validYear != null) { Text("Xem năm") }
            },
            dismissButton = { TextButton(onClick = { yearDialog = false }) { Text("Hủy") } },
        )
    }
}
