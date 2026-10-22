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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.lichviet.core.Almanac
import vn.lichviet.core.DayAssessment
import vn.lichviet.core.LunarDate
import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private data class DateSearch(val start: LocalDate, val end: LocalDate, val purpose: String)
private data class DateCandidate(val date: LocalDate, val lunar: LunarDate, val canChi: String, val assessment: DayAssessment)

@Composable
fun AuspiciousScreen(onSelectDate: (LocalDate) -> Unit) {
    val today = remember { LocalDate.now(VietnameseCalendar.zoneId) }
    var start by rememberSaveable { mutableStateOf(today.withDayOfMonth(1)) }
    var end by rememberSaveable { mutableStateOf(today.withDayOfMonth(today.lengthOfMonth())) }
    var purpose by rememberSaveable { mutableStateOf("Khai trương") }
    var dateDialog by remember { mutableStateOf<String?>(null) }
    var request by remember { mutableStateOf<DateSearch?>(null) }
    var results by remember { mutableStateOf<List<DateCandidate>?>(null) }
    var searching by remember { mutableStateOf(false) }
    LaunchedEffect(request) {
        results = null
        val query = request
        searching = query != null
        if (query != null) {
            try {
                results = withContext(Dispatchers.Default) {
                    Almanac.findDates(query.start, query.end, query.purpose).map { date ->
                        DateCandidate(date, VietnameseCalendar.solarToLunar(date), VietnameseCalendar.dayCanChi(date), Almanac.assess(date, query.purpose))
                    }
                }
            } finally {
                searching = false
            }
        }
    }
    val rangeError = when {
        end.isBefore(start) -> "Ngày kết thúc cần bằng hoặc sau ngày bắt đầu."
        ChronoUnit.DAYS.between(start, end) > 365 -> "Vui lòng chọn khoảng thời gian tối đa 366 ngày."
        else -> null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item(key = "heading") {
            ToolHeading("Sắp xếp việc quan trọng", "Tìm ngày phù hợp", "Tra cứu ngày theo khoảng thời gian và công việc bạn dự định.")
        }
        item(key = "form") {
            ToolCard {
                Text("Bạn đang lên kế hoạch gì?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                listOf(listOf("Khai trương", "Cưới hỏi"), listOf("Chuyển nhà", "Xuất hành")).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        pair.forEach { item ->
                            FilterChip(
                                selected = purpose == item,
                                onClick = { purpose = item; request = null; results = null },
                                label = { Text(item) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DateChoice("Từ ngày", start, onClick = { dateDialog = "start" })
                DateChoice("Đến ngày", end, onClick = { dateDialog = "end" })
                if (rangeError != null) Text(rangeError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = { request = DateSearch(start, end, purpose) },
                    enabled = rangeError == null && !searching,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                    Text(if (searching) "Đang tìm ngày…" else "Tìm ngày phù hợp", modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                }
            }
        }
        if (searching) item(key = "loading") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Đang tra cứu lịch trên thiết bị…", style = MaterialTheme.typography.bodySmall)
            }
        }
        results?.let { dates ->
            item(key = "result_count") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${dates.size} ngày được gợi ý", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${start.vietnameseDate()} – ${end.vietnameseDate()} · $purpose", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (dates.isEmpty()) item(key = "empty") {
                ToolCard {
                    Icon(Icons.Rounded.EventAvailable, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Chưa có ngày phù hợp trong khoảng này", style = MaterialTheme.typography.titleMedium)
                    Text("Bạn có thể mở rộng khoảng thời gian hoặc chọn công việc khác.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(dates, key = { "result_${it.date}" }) { candidate ->
                val date = candidate.date
                val lunar = candidate.lunar
                ToolCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(date.dayOfMonth.toString().padStart(2, '0'), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(date.vietnameseFullDate(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Âm lịch ${lunar.day}/${lunar.month}${if (lunar.isLeap) " nhuận" else ""} · ${candidate.canChi}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(candidate.assessment.label, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    candidate.assessment.reasons.forEach { reason ->
                        Text("• $reason", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { onSelectDate(date) }, modifier = Modifier.align(Alignment.End)) {
                        Text("Xem trên lịch")
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
        item(key = "cultural_note") {
            ToolCard {
                Text("Hiểu đúng về ngày tốt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Bộ lọc dùng một số quy ước dân gian: ngày hoàng đạo, trực phù hợp với công việc và tránh Tam Nương, Nguyệt Kỵ. Các truyền thống có thể áp dụng quy tắc khác nhau; kết quả chỉ phục vụ tham khảo văn hóa, không dự đoán thành công hay rủi ro.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    dateDialog?.let { target ->
        VietnameseDateDialog(
            initialDate = if (target == "start") start else end,
            onDismiss = { dateDialog = null },
            onSelected = {
                if (target == "start") start = it else end = it
                request = null
                results = null
                dateDialog = null
            },
        )
    }
}
