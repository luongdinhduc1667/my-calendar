package vn.lichviet.app.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.lichviet.app.R
import vn.lichviet.app.ics.EventOccurrence
import vn.lichviet.app.ics.EventOccurrenceEngine
import androidx.compose.ui.graphics.Color
import java.time.LocalTime
import vn.lichviet.app.data.*
import vn.lichviet.app.EditorOperation
import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable private fun repeatOptions() = mapOf(RepeatRule.NONE to stringResource(R.string.repeat_none),RepeatRule.DAILY to stringResource(R.string.repeat_daily),RepeatRule.WEEKLY to stringResource(R.string.repeat_weekly),RepeatRule.MONTHLY to stringResource(R.string.repeat_monthly),RepeatRule.YEARLY to stringResource(R.string.repeat_yearly),RepeatRule.LUNAR_YEARLY to stringResource(R.string.repeat_lunar))
@Composable private fun typeOptions() = mapOf(EventType.EVENT to stringResource(R.string.type_event),EventType.BIRTHDAY to stringResource(R.string.type_birthday),EventType.DEATH_ANNIVERSARY to stringResource(R.string.type_memorial))

@Composable
fun EventsScreen(events:List<EventEntity>,onAdd:()->Unit,onEdit:(EventEntity)->Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val today=LocalDate.now(VietnameseCalendar.zoneId)
    val next by produceState<Map<Long,EventOccurrence?>>(emptyMap(),events,today) {
        value=withContext(Dispatchers.Default) { events.associate { event -> event.id to runCatching {
            EventOccurrenceEngine.next(event,today.atStartOfDay(VietnameseCalendar.zoneId).toInstant().minusMillis(1))
        }.getOrNull() } }
    }
    val filtered=remember(events,query,next) { events.filter{it.title.contains(query,true)||it.note.contains(query,true)||it.location.contains(query,true)}.sortedBy{next[it.id]?.start?:java.time.Instant.MAX} }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=24.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        item { PageHeading(stringResource(R.string.events_title),stringResource(R.string.events_subtitle)) }
        item { OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text(stringResource(R.string.events_search))},leadingIcon={Icon(Icons.Outlined.Search,null)},shape=RoundedCornerShape(16.dp)) }
        item { Button(onClick=onAdd,shape=RoundedCornerShape(14.dp),contentPadding=PaddingValues(horizontal=20.dp,vertical=14.dp)) {
            Icon(Icons.Outlined.Add,null,Modifier.size(19.dp));Spacer(Modifier.width(8.dp));Text(stringResource(R.string.events_add))
        } }
        if(filtered.isEmpty()) item { EventEmpty(stringResource(if(query.isBlank()) R.string.events_empty else R.string.events_no_results),stringResource(if(query.isBlank()) R.string.events_empty_body else R.string.events_no_results_body)) }
        items(filtered,key={it.id}) { event -> EventItem(event,next[event.id]) { onEdit(event) } }
    }
}

@Composable
fun DayEvents(date:LocalDate,events:List<EventEntity>,onEdit:(EventEntity)->Unit) {
    val context=LocalContext.current
    val dayEvents by produceState<List<EventOccurrence>>(emptyList(),date,events) {
        value=withContext(Dispatchers.Default) { events.flatMap { event -> runCatching { EventOccurrenceEngine.between(listOf(event),date,date.plusDays(1)) }.getOrDefault(emptyList()) }.sortedBy{it.start} }
    }
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.events_day_title,CalendarText.shortDate(context,date)),style=MaterialTheme.typography.titleMedium)
        if(dayEvents.isEmpty()) EventEmpty(stringResource(R.string.events_free),stringResource(R.string.events_free_body))
        dayEvents.forEach { EventItem(it.event,it) { onEdit(it.event) } }
    }
}

@Composable
private fun EventItem(event:EventEntity,occurrence:EventOccurrence?,onClick:()->Unit) {
    val context=LocalContext.current
    val date=occurrence?.startDate
    val repeats=repeatOptions()
    Surface(onClick=onClick,shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surface,border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(18.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.widthIn(min=42.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Text(date?.dayOfMonth?.toString()?:"—",style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.primary)
                Text(date?.let{CalendarText.monthYear(context,java.time.YearMonth.from(it))}?:stringResource(R.string.events_past),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.width(4.dp).height(48.dp).background(EventColors.of(event),RoundedCornerShape(2.dp)))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                Text(occurrence?.title?:event.title,fontWeight=FontWeight.Medium)
                val time=if(occurrence?.allDay?:event.allDay) stringResource(R.string.event_all_day) else CalendarText.time(context,occurrence?.start?.atZone(VietnameseCalendar.zoneId)?.toLocalTime()?:LocalTime.of(event.hour,event.minute))
                Text(time+" · "+(if(event.icsCalendar!=null)stringResource(R.string.event_series) else repeats.getValue(event.repeat)),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(occurrence!=null && occurrence.endDateExclusive>occurrence.startDate.plusDays(1)) Text(CalendarText.shortDate(context,occurrence.startDate)+" → "+CalendarText.shortDate(context,occurrence.endDateExclusive.minusDays(1)),style=MaterialTheme.typography.bodySmall)
                if(event.location.isNotBlank()) Text(occurrence?.location?:event.location,style=MaterialTheme.typography.bodySmall)
                if(event.note.isNotBlank()) Text(occurrence?.note?:event.note,style=MaterialTheme.typography.bodySmall,maxLines=2,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight,stringResource(R.string.event_edit),tint=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(20.dp))
        }
    }
}

@Composable
private fun EventEmpty(title:String,body:String) {
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.5f)) {
        Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.EventNote,null,tint=MaterialTheme.colorScheme.tertiary)
            Text(title,style=MaterialTheme.typography.titleSmall)
            Text(body,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditor(initial:EventEntity?,date:LocalDate,onDismiss:()->Unit,onSave:(EventEntity,List<Int>)->Unit,onDelete:(Long)->Unit,
    operation:EditorOperation,getReminderOffsets:suspend (Long)->List<Int>) {
    var title by rememberSaveable{mutableStateOf(initial?.title?:"")}
    var note by rememberSaveable{mutableStateOf(initial?.note?:"")}
    var location by rememberSaveable{mutableStateOf(initial?.location?:"")}
    var hasEnd by rememberSaveable{mutableStateOf(initial?.endSolarDate!=null)}
    var endSolar by rememberSaveable{mutableStateOf(initial?.endSolarDate?:date.plusDays(1).toString())}
    var endHour by rememberSaveable{mutableIntStateOf(initial?.endHour?:10)}
    var endMinute by rememberSaveable{mutableIntStateOf(initial?.endMinute?:0)}
    var color by rememberSaveable{mutableStateOf(initial?.colorArgb)}
    var hex by rememberSaveable{mutableStateOf(initial?.colorArgb?.let(EventColors::hex)?:"")}
    var endPicker by remember{mutableStateOf(false)}
    var solar by rememberSaveable{mutableStateOf(initial?.solarDate?:date.toString())}
    var hour by rememberSaveable{mutableIntStateOf(initial?.hour?:9)}
    var minute by rememberSaveable{mutableIntStateOf(initial?.minute?:0)}
    var allDay by rememberSaveable{mutableStateOf(initial?.allDay?:false)}
    var repeat by rememberSaveable{mutableStateOf(initial?.repeat?:RepeatRule.NONE)}
    var type by rememberSaveable{mutableStateOf(initial?.type?:EventType.EVENT)}
    var reminder by rememberSaveable{mutableStateOf(initial?.reminderEnabled?:true)}
    var selectedOffsets by rememberSaveable(initial?.id){mutableStateOf(listOf((initial?.reminderDaysBefore?:0)*1440))}
    var offsetsLoaded by rememberSaveable(initial?.id){mutableStateOf(initial==null)}
    var customDays by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(initial?.id) {
        if (initial != null && !offsetsLoaded) {
            runCatching { getReminderOffsets(initial.id) }
                .onSuccess { selectedOffsets = it; offsetsLoaded = true }
        }
    }
    var leap by rememberSaveable{mutableStateOf(initial?.leapMonthPolicy?:LeapMonthPolicy.REGULAR_ONLY)}
    var missing by rememberSaveable{mutableStateOf(initial?.missingDayPolicy?:MissingDayPolicy.LAST_DAY)}
    var picker by remember{mutableStateOf(false)}
    var lunarPicker by remember{mutableStateOf(false)}
    var deleting by remember{mutableStateOf(false)}
    val saving=operation.running
    val error=operation.error
    val context=LocalContext.current
    val repeats=repeatOptions()
    val types=typeOptions()
    val imported=initial?.icsCalendar!=null
    val validColor=hex.isBlank()||EventColors.parse(hex)!=null
    val selected=LocalDate.parse(solar)
    val validEnd=remember(selected,endSolar,allDay,hasEnd,hour,minute,endHour,endMinute) {
        !hasEnd||runCatching { val end=LocalDate.parse(endSolar);val startAt=selected.atTime(if(allDay)LocalTime.MIDNIGHT else LocalTime.of(hour,minute));val endAt=end.atTime(if(allDay)LocalTime.MIDNIGHT else LocalTime.of(endHour,endMinute));endAt>startAt && java.time.Duration.between(startAt,endAt)<=java.time.Duration.ofDays(366) }.getOrDefault(false)
    }
    val lunar=remember(selected){VietnameseCalendar.solarToLunar(selected)}
    ModalBottomSheet(onDismissRequest={if(!saving)onDismiss()},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).imePadding(),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(if(initial==null)stringResource(R.string.events_add) else stringResource(R.string.event_edit),style=MaterialTheme.typography.headlineMedium)
            if(imported) Text(stringResource(R.string.event_ics_readonly),style=MaterialTheme.typography.bodyMedium)
            if(!imported) {
            OutlinedTextField(title,{title=it.take(120)},label={Text(stringResource(R.string.event_title))},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(12.dp))
            ChoiceField(stringResource(R.string.event_type),type,types) { type=it; if(it==EventType.DEATH_ANNIVERSARY) repeat=RepeatRule.LUNAR_YEARLY }
            OutlinedCard(onClick={picker=true},modifier=Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.CalendarMonth,null)
                    Column{ Text(CalendarText.shortDate(context,selected)); Text(stringResource(R.string.event_lunar_date,CalendarText.lunarFull(context,lunar)),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            TextButton(onClick={lunarPicker=true}){Icon(Icons.Outlined.NightsStay,null,Modifier.size(17.dp));Spacer(Modifier.width(7.dp));Text(stringResource(R.string.event_lunar_input))}
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                Text(stringResource(R.string.event_is_all_day),style=MaterialTheme.typography.titleSmall)
                Switch(allDay,{allDay=it},modifier=Modifier.semantics{contentDescription=context.getString(R.string.event_is_all_day)})
            }
            if(!allDay) OutlinedButton(onClick={TimePickerDialog(context,{_,h,m->hour=h;minute=m},hour,minute,android.text.format.DateFormat.is24HourFormat(context)).show()},modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Schedule,null,Modifier.size(19.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.event_start_time,CalendarText.time(context,LocalTime.of(hour,minute))))
            }
            if(allDay) Text(stringResource(R.string.event_all_day_reminder),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                Text(stringResource(R.string.event_has_end),Modifier.weight(1f));Switch(hasEnd,{hasEnd=it},modifier=Modifier.semantics{contentDescription=context.getString(R.string.event_has_end)})
            }
            if(hasEnd) {
                OutlinedButton(onClick={endPicker=true},modifier=Modifier.fillMaxWidth()){Text(stringResource(if(allDay)R.string.event_end_date else R.string.event_end_date_timed,CalendarText.shortDate(context,LocalDate.parse(endSolar))))}
                if(!allDay) OutlinedButton(onClick={TimePickerDialog(context,{_,h,m->endHour=h;endMinute=m},endHour,endMinute,android.text.format.DateFormat.is24HourFormat(context)).show()},modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.event_end_time,CalendarText.time(context,LocalTime.of(endHour,endMinute))))}
                if(allDay)Text(stringResource(R.string.event_end_exclusive_help),style=MaterialTheme.typography.bodySmall)
                if(!validEnd)Text(stringResource(R.string.event_bad_end),color=MaterialTheme.colorScheme.error)
            }
            Text(stringResource(R.string.event_vietnam_time),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceField(stringResource(R.string.event_repeat),repeat,repeats){repeat=it}
            if(repeat==RepeatRule.LUNAR_YEARLY) {
                ChoiceField(stringResource(R.string.event_leap_policy),leap,mapOf(LeapMonthPolicy.REGULAR_ONLY to stringResource(R.string.event_regular_month),LeapMonthPolicy.LEAP_ONLY to stringResource(R.string.event_leap_month),LeapMonthPolicy.BOTH to stringResource(R.string.event_both_months))){leap=it}
            }
            if(repeat in listOf(RepeatRule.LUNAR_YEARLY,RepeatRule.MONTHLY,RepeatRule.YEARLY)) {
                ChoiceField(stringResource(R.string.event_missing_day),missing,mapOf(MissingDayPolicy.LAST_DAY to stringResource(R.string.event_last_day),MissingDayPolicy.SKIP to stringResource(R.string.event_skip_year))){missing=it}
            }
            OutlinedTextField(location,{location=it.take(500)},label={Text(stringResource(R.string.event_location))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(note,{note=it.take(2000)},label={Text(stringResource(R.string.event_note))},modifier=Modifier.fillMaxWidth(),minLines=2,maxLines=5,shape=RoundedCornerShape(12.dp))
            }
            Text(stringResource(R.string.event_color),style=MaterialTheme.typography.titleSmall)
            EventColors.palette.chunked(3).forEach { row -> Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { row.forEach { value ->
                FilledIconToggleButton(modifier=Modifier.semantics{contentDescription=EventColors.hex(value)},checked=color==value,onCheckedChange={color=value;hex=EventColors.hex(value)},colors=IconButtonDefaults.filledIconToggleButtonColors(containerColor=Color(value),checkedContainerColor=Color(value))) { if(color==value)Icon(Icons.Outlined.Check,EventColors.hex(value),tint=EventColors.foreground(Color(value))) }
            } } }
            TextButton(onClick={color=null;hex=""}){Text(stringResource(R.string.event_color_default))}
            OutlinedTextField(hex,{hex=it.take(7);EventColors.parse(it)?.let{parsed->color=parsed};if(it.isBlank())color=null},label={Text(stringResource(R.string.event_color_hex))},isError=!validColor,singleLine=true,modifier=Modifier.fillMaxWidth())
            if(!validColor)Text(stringResource(R.string.event_color_bad),color=MaterialTheme.colorScheme.error)
            val previewColor=Color(color?:EventColors.default(type))
            Surface(color=previewColor,contentColor=EventColors.foreground(previewColor),shape=RoundedCornerShape(8.dp)){Text(stringResource(R.string.event_color_preview),Modifier.padding(12.dp))}
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                Text(stringResource(R.string.event_remind),style=MaterialTheme.typography.titleSmall)
                Switch(reminder,{reminder=it},modifier=Modifier.semantics{contentDescription=context.getString(R.string.event_remind)})
            }
            if(reminder) {
                Text(stringResource(R.string.event_reminder_offsets),style=MaterialTheme.typography.titleSmall)
                if(!offsetsLoaded) Text(stringResource(R.string.event_loading_offsets),style=MaterialTheme.typography.bodySmall)
                val options=listOf(0 to stringResource(R.string.event_at_time),5 to stringResource(R.string.event_minutes_before,5),10 to stringResource(R.string.event_minutes_before,10),15 to stringResource(R.string.event_minutes_before,15),30 to stringResource(R.string.event_minutes_before,30),60 to stringResource(R.string.event_hours_before,1),1440 to stringResource(R.string.event_days_before,1),2880 to stringResource(R.string.event_days_before,2),4320 to stringResource(R.string.event_days_before,3),10080 to stringResource(R.string.event_days_before,7),20160 to stringResource(R.string.event_days_before,14))
                options.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        row.forEach { (offset,label) ->
                            FilterChip(selected=offset in selectedOffsets,onClick={
                                selectedOffsets=(if(offset in selectedOffsets)selectedOffsets-offset else selectedOffsets+offset).distinct().sorted()
                            },label={Text(label)},enabled=!saving&&offsetsLoaded)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(customDays,{customDays=it.filter(Char::isDigit).take(3)},label={Text(stringResource(R.string.event_custom_days))},
                        modifier=Modifier.weight(1f),singleLine=true,enabled=offsetsLoaded)
                    OutlinedButton(onClick={
                        val days=customDays.toIntOrNull()
                        if(days!=null && days in 1..365){selectedOffsets=(selectedOffsets+days*1440).distinct().sorted();customDays=""}
                    },enabled=customDays.toIntOrNull() in 1..365 && !saving && offsetsLoaded){Text(stringResource(R.string.event_add))}
                }
                selectedOffsets.filter { offset -> options.none { it.first==offset } }.forEach { offset ->
                    TextButton(onClick={selectedOffsets=selectedOffsets-offset},enabled=!saving&&offsetsLoaded){Text(stringResource(R.string.event_minutes_before,offset)+" · "+stringResource(R.string.event_remove))}
                }
                if(selectedOffsets.isEmpty()) Text(stringResource(R.string.event_pick_reminder),color=MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.event_reminder_fallback),
                    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            error?.let{Text(stringResource(it),color=MaterialTheme.colorScheme.error)}
            Button(onClick={
                        val updated=if(imported) initial!!.copy(colorArgb=color,reminderEnabled=reminder,reminderDaysBefore=(selectedOffsets.minOrNull()?:0)/1440)
                        else EventEntity(id=initial?.id?:0,title=title.trim(),note=note.trim(),solarDate=solar,hour=hour,minute=minute,allDay=allDay,repeat=repeat,type=type,
                            reminderDaysBefore=(selectedOffsets.minOrNull()?:0)/1440,reminderEnabled=reminder,lunarDay=if(repeat==RepeatRule.LUNAR_YEARLY)lunar.day else null,
                            lunarMonth=if(repeat==RepeatRule.LUNAR_YEARLY)lunar.month else null,leapMonthPolicy=leap,missingDayPolicy=missing,
                            endSolarDate=if(hasEnd)endSolar else null,endHour=if(hasEnd&&!allDay)endHour else null,endMinute=if(hasEnd&&!allDay)endMinute else null,
                            colorArgb=color,location=location.trim(),icsUid=initial?.icsUid)
                        onSave(updated,selectedOffsets)
            },modifier=Modifier.fillMaxWidth(),enabled=title.isNotBlank()&&!saving&&offsetsLoaded&&validEnd&&validColor&&(!reminder||selectedOffsets.isNotEmpty()),contentPadding=PaddingValues(16.dp)) { Text(if(saving)stringResource(R.string.event_saving) else stringResource(R.string.event_save)) }
            if(initial!=null) TextButton(onClick={deleting=true},modifier=Modifier.fillMaxWidth(),enabled=!saving) { Text(stringResource(R.string.event_delete),color=MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(24.dp))
        }
    }
    if(picker) CalendarDatePicker(selected,{picker=false}){solar=it.toString();picker=false}
    if(endPicker)CalendarDatePicker(LocalDate.parse(endSolar).coerceAtMost(VietnameseCalendar.maxDate),{endPicker=false}){endSolar=it.toString();endPicker=false}
    if(lunarPicker)LunarInputDialog(selected,{lunarPicker=false}){solar=it.toString();lunarPicker=false}
    if(deleting) AlertDialog(onDismissRequest={deleting=false},title={Text(stringResource(R.string.event_delete_title))},text={Text(stringResource(R.string.event_delete_body))},confirmButton={TextButton(onClick={
        onDelete(initial!!.id);deleting=false
    }){Text(stringResource(R.string.event_remove),color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton(onClick={deleting=false}){Text(stringResource(R.string.event_keep))}})
}

@Composable
private fun LunarInputDialog(initial:LocalDate,onDismiss:()->Unit,onSelected:(LocalDate)->Unit) {
    val context=LocalContext.current
    val lunar=remember(initial){VietnameseCalendar.solarToLunar(initial)}
    var day by rememberSaveable{mutableStateOf(lunar.day.toString())}
    var month by rememberSaveable{mutableStateOf(lunar.month.toString())}
    var year by rememberSaveable{mutableStateOf(lunar.year.toString())}
    var leap by rememberSaveable{mutableStateOf(lunar.isLeap)}
    var error by remember{mutableStateOf(false)}
    AlertDialog(onDismissRequest=onDismiss,title={Text(stringResource(R.string.event_select_lunar))},text={
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(day,{day=it.filter(Char::isDigit).take(2)},label={Text(stringResource(R.string.event_day))},singleLine=true,modifier=Modifier.weight(1f))
                OutlinedTextField(month,{month=it.filter(Char::isDigit).take(2)},label={Text(stringResource(R.string.event_month))},singleLine=true,modifier=Modifier.weight(1f))
            }
            OutlinedTextField(year,{year=it.filter(Char::isDigit).take(4)},label={Text(stringResource(R.string.event_lunar_year))},singleLine=true,modifier=Modifier.fillMaxWidth())
            Row(verticalAlignment=Alignment.CenterVertically){Checkbox(leap,{leap=it},modifier=Modifier.semantics{contentDescription=context.getString(R.string.event_is_leap)});Text(stringResource(R.string.event_is_leap))}
            Text(stringResource(R.string.event_lunar_help),style=MaterialTheme.typography.bodySmall)
            if(error)Text(stringResource(R.string.event_bad_lunar),color=MaterialTheme.colorScheme.error)
        }
    },confirmButton={TextButton(onClick={
        val d=day.toIntOrNull();val m=month.toIntOrNull();val y=year.toIntOrNull()
        val solar=if(d!=null&&m!=null&&y!=null)VietnameseCalendar.lunarToSolar(vn.lichviet.core.LunarDate(d,m,y,leap)) else null
        if(solar==null)error=true else onSelected(solar)
    }){Text(stringResource(R.string.event_select_date))}},dismissButton={TextButton(onClick=onDismiss){Text(stringResource(R.string.event_cancel))}})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ChoiceField(label:String,value:T,options:Map<T,String>,onChange:(T)->Unit) {
    var open by remember{mutableStateOf(false)}
    ExposedDropdownMenuBox(expanded=open,onExpandedChange={open=it}) {
        OutlinedTextField(value=options[value]?:"",onValueChange={},readOnly=true,label={Text(label)},modifier=Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(open)},shape=RoundedCornerShape(12.dp))
        ExposedDropdownMenu(expanded=open,onDismissRequest={open=false}) {
            options.forEach{(item,text)->DropdownMenuItem(text={Text(text)},onClick={onChange(item);open=false})}
        }
    }
}

