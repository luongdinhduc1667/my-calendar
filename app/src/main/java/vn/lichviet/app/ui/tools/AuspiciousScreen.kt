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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.lichviet.core.Almanac
import vn.lichviet.core.CanChiId
import vn.lichviet.core.LunarDate
import vn.lichviet.core.PurposeId
import vn.lichviet.core.TypedDayAssessment
import vn.lichviet.core.VietnameseCalendar
import vn.lichviet.app.R
import vn.lichviet.app.ui.CalendarText
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private data class DateSearch(val start: LocalDate, val end: LocalDate, val purpose: PurposeId)
private data class DateCandidate(val date: LocalDate, val lunar: LunarDate, val canChi: CanChiId, val assessment: TypedDayAssessment)

@Composable
fun AuspiciousScreen(onSelectDate: (LocalDate) -> Unit) {
    val context = LocalContext.current
    val today = remember { LocalDate.now(VietnameseCalendar.zoneId) }
    var start by rememberSaveable { mutableStateOf(today.withDayOfMonth(1)) }
    var end by rememberSaveable { mutableStateOf(today.withDayOfMonth(today.lengthOfMonth())) }
    var purpose by rememberSaveable { mutableStateOf(PurposeId.OPENING) }
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
                        DateCandidate(date, VietnameseCalendar.solarToLunar(date), VietnameseCalendar.dayCanChiId(date), Almanac.assessId(date, query.purpose))
                    }
                }
            } finally {
                searching = false
            }
        }
    }
    val rangeError = when {
        end.isBefore(start) -> stringResource(R.string.tool_auspicious_invalid_order)
        ChronoUnit.DAYS.between(start, end) > 365 -> stringResource(R.string.tool_auspicious_invalid_range)
        else -> null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item(key = "heading") {
            ToolHeading(stringResource(R.string.tool_auspicious_eyebrow), stringResource(R.string.tool_auspicious_title),
                stringResource(R.string.tool_auspicious_description))
        }
        item(key = "form") {
            ToolCard {
                Text(stringResource(R.string.tool_auspicious_plan), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                listOf(listOf(PurposeId.OPENING, PurposeId.WEDDING), listOf(PurposeId.MOVING, PurposeId.TRAVEL)).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        pair.forEach { item ->
                            FilterChip(
                                selected = purpose == item,
                                onClick = { purpose = item; request = null; results = null },
                                label = { Text(CalendarText.purpose(context, item)) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DateChoice(stringResource(R.string.tool_auspicious_from), start, onClick = { dateDialog = "start" })
                DateChoice(stringResource(R.string.tool_auspicious_to), end, onClick = { dateDialog = "end" })
                if (rangeError != null) Text(rangeError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = { request = DateSearch(start, end, purpose) },
                    enabled = rangeError == null && !searching,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                    Text(stringResource(if (searching) R.string.tool_auspicious_searching else R.string.tool_auspicious_search),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                }
            }
        }
        if (searching) item(key = "loading") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.tool_auspicious_loading), style = MaterialTheme.typography.bodySmall)
            }
        }
        results?.let { dates ->
            item(key = "result_count") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(pluralStringResource(R.plurals.tool_auspicious_count, dates.size, dates.size), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.tool_auspicious_result_range, CalendarText.shortDate(context, start),
                        CalendarText.shortDate(context, end), CalendarText.purpose(context, purpose)),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (dates.isEmpty()) item(key = "empty") {
                ToolCard {
                    Icon(Icons.Rounded.EventAvailable, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.tool_auspicious_empty_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.tool_auspicious_empty_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(dates, key = { "result_${it.date}" }) { candidate ->
                val date = candidate.date
                val lunar = candidate.lunar
                ToolCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(date.dayOfMonth.toString().padStart(2, '0'), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(CalendarText.fullDate(context, date), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.tool_auspicious_lunar_can_chi, CalendarText.lunarShort(context, lunar), CalendarText.canChi(context, candidate.canChi)),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(CalendarText.assessmentLabel(context, candidate.assessment.labelId), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    candidate.assessment.reasons.forEach { reason ->
                        Text(stringResource(R.string.tool_bullet_item, CalendarText.assessmentReason(context, reason)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { onSelectDate(date) }, modifier = Modifier.align(Alignment.End)) {
                        Text(stringResource(R.string.tool_view_on_calendar))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
        item(key = "cultural_note") {
            ToolCard {
                Text(stringResource(R.string.tool_auspicious_note_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.tool_auspicious_note_body),
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
