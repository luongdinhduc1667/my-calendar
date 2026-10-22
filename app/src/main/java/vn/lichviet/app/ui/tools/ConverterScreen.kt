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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import vn.lichviet.core.LunarDate
import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate

@Composable
fun ConverterScreen(initialDate: LocalDate, onSelectDate: (LocalDate) -> Unit) {
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
        ToolHeading("Tiện ích · Offline", "Đổi ngày âm dương", "Chuyển đổi theo lịch Việt Nam, múi giờ UTC+7.")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !lunarInput, onClick = {
                if (lunarInput) resultDate?.let { solarDate = it }
                lunarInput = false
            }, label = { Text("Dương → Âm") })
            FilterChip(selected = lunarInput, onClick = {
                if (!lunarInput) {
                    val current = VietnameseCalendar.solarToLunar(solarDate)
                    day = current.day.toString()
                    month = current.month.toString()
                    year = current.year.toString()
                    leap = current.isLeap
                }
                lunarInput = true
            }, label = { Text("Âm → Dương") })
        }
        ToolCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.SyncAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(if (lunarInput) "Ngày âm lịch cần đổi" else "Ngày dương lịch cần đổi", style = MaterialTheme.typography.titleMedium)
            }
            if (lunarInput) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = day, onValueChange = { day = it.filter(Char::isDigit).take(2) },
                        label = { Text("Ngày") }, modifier = Modifier.weight(1f), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = month, onValueChange = { month = it.filter(Char::isDigit).take(2) },
                        label = { Text("Tháng") }, modifier = Modifier.weight(1f), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = year, onValueChange = { year = it.filter(Char::isDigit).take(4) },
                        label = { Text("Năm") }, modifier = Modifier.weight(1.3f), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Tháng nhuận", style = MaterialTheme.typography.bodyLarge)
                        Text("Chỉ chọn nếu đúng tháng nhuận của năm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = leap, onCheckedChange = { leap = it })
                }
                if (resultDate == null) {
                    Text(
                        if (enteredLunar == null) "Nhập ngày 1–30, tháng 1–12 và năm âm lịch 1899–2100."
                        else "Ngày hoặc tháng nhuận không tồn tại, hoặc kết quả nằm ngoài các năm dương lịch 1900–2100.",
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else DateChoice("Dương lịch", solarDate, onClick = { dateDialog = true })
        }
        if (resultDate != null && resultLunar != null) {
            ToolCard {
                Text("KẾT QUẢ CHUYỂN ĐỔI", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(
                    if (lunarInput) resultDate.vietnameseDate()
                    else "${resultLunar.day}/${resultLunar.month}${if (resultLunar.isLeap) " nhuận" else ""}",
                    style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
                )
                Text(
                    if (lunarInput) resultDate.vietnameseFullDate()
                    else "Năm ${VietnameseCalendar.yearCanChi(resultLunar.year)} · ${resultLunar.year}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DetailLine("Ngày", VietnameseCalendar.dayCanChi(resultDate))
                DetailLine("Tháng âm lịch", VietnameseCalendar.monthCanChi(resultLunar))
                Button(onClick = { onSelectDate(resultDate) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Xem ngày này trên lịch", modifier = Modifier.padding(vertical = 6.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        Text("Hỗ trợ ngày dương lịch trong các năm 1900–2100. Phép chuyển đổi được tính trực tiếp trên thiết bị, không cần kết nối mạng.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (dateDialog) VietnameseDateDialog(solarDate, onDismiss = { dateDialog = false }, onSelected = {
        solarDate = it
        dateDialog = false
    })
}
