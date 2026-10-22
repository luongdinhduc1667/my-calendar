package vn.lichviet.app.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vn.lichviet.app.data.*
import vn.lichviet.app.EditorOperation
import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val repeats=mapOf(RepeatRule.NONE to "Không lặp", RepeatRule.DAILY to "Hằng ngày",RepeatRule.WEEKLY to "Hằng tuần",
    RepeatRule.MONTHLY to "Hằng tháng",RepeatRule.YEARLY to "Hằng năm dương lịch",RepeatRule.LUNAR_YEARLY to "Hằng năm âm lịch")
private val types=mapOf(EventType.EVENT to "Sự kiện",EventType.BIRTHDAY to "Sinh nhật",EventType.DEATH_ANNIVERSARY to "Ngày giỗ")

@Composable
fun EventsScreen(events:List<EventEntity>,onAdd:()->Unit,onEdit:(EventEntity)->Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val today=LocalDate.now(VietnameseCalendar.zoneId)
    val filtered=remember(events,query,today) {
        events.filter{it.title.contains(query,true)||it.note.contains(query,true)}.sortedBy{Recurrence.nextOccurrence(it,today)?:LocalDate.MAX}
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        PageHeading("Những ngày đáng nhớ", "Một lời nhắc, vẹn tròn yêu thương")
        OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,
            placeholder={Text("Tìm sự kiện, sinh nhật, ngày giỗ…")},leadingIcon={Icon(Icons.Outlined.Search,null)},shape=RoundedCornerShape(16.dp))
        Button(onClick=onAdd,shape=RoundedCornerShape(14.dp),contentPadding=PaddingValues(horizontal=20.dp,vertical=14.dp)) {
            Icon(Icons.Outlined.Add,null,Modifier.size(19.dp)); Spacer(Modifier.width(8.dp)); Text("Thêm ngày đáng nhớ")
        }
        if(filtered.isEmpty()) EventEmpty(if(query.isBlank())"Chưa có sự kiện" else "Không tìm thấy sự kiện",if(query.isBlank())"Lưu sinh nhật, ngày giỗ hoặc một cuộc hẹn. Lịch Việt sẽ giúp bạn nhớ." else "Thử tìm bằng một tên hoặc ghi chú khác.")
        filtered.forEach { event ->
            val next=Recurrence.nextOccurrence(event,today)
            EventItem(event,next,onClick={onEdit(event)})
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun DayEvents(date:LocalDate,events:List<EventEntity>,onEdit:(EventEntity)->Unit) {
    val dayEvents=remember(date,events){events.filter{Recurrence.occursOn(it,date)}}
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Lịch hẹn · ${date.dayOfMonth}/${date.monthValue}",style=MaterialTheme.typography.titleMedium)
        if(dayEvents.isEmpty()) EventEmpty("Một khoảng thảnh thơi", "Bạn chưa có lịch hẹn trong ngày này.")
        dayEvents.forEach{EventItem(it,date){onEdit(it)}}
    }
}

@Composable
private fun EventItem(event:EventEntity,date:LocalDate?,onClick:()->Unit) {
    Surface(onClick=onClick,shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surface,
        border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(18.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.width(45.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Text(date?.dayOfMonth?.toString()?:"—",style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.primary)
                Text(date?.let{"TH ${it.monthValue}"}?:"ĐÃ QUA",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.width(3.dp).height(42.dp).background(MaterialTheme.colorScheme.tertiary,RoundedCornerShape(2.dp)))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                Text(event.title,fontWeight=FontWeight.Medium)
                Text("%02d:%02d".format(event.hour,event.minute)+" · "+repeats.getValue(event.repeat),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(event.note.isNotBlank()) Text(event.note,style=MaterialTheme.typography.bodySmall,maxLines=2,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight,"Sửa sự kiện",tint=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(20.dp))
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
fun EventEditor(initial:EventEntity?,date:LocalDate,onDismiss:()->Unit,onSave:(EventEntity)->Unit,onDelete:(Long)->Unit,operation:EditorOperation) {
    var title by rememberSaveable{mutableStateOf(initial?.title?:"")}
    var note by rememberSaveable{mutableStateOf(initial?.note?:"")}
    var solar by rememberSaveable{mutableStateOf(initial?.solarDate?:date.toString())}
    var hour by rememberSaveable{mutableIntStateOf(initial?.hour?:9)}
    var minute by rememberSaveable{mutableIntStateOf(initial?.minute?:0)}
    var repeat by rememberSaveable{mutableStateOf(initial?.repeat?:RepeatRule.NONE)}
    var type by rememberSaveable{mutableStateOf(initial?.type?:EventType.EVENT)}
    var reminder by rememberSaveable{mutableStateOf(initial?.reminderEnabled?:true)}
    var days by rememberSaveable{mutableIntStateOf(initial?.reminderDaysBefore?:0)}
    var leap by rememberSaveable{mutableStateOf(initial?.leapMonthPolicy?:LeapMonthPolicy.REGULAR_ONLY)}
    var missing by rememberSaveable{mutableStateOf(initial?.missingDayPolicy?:MissingDayPolicy.LAST_DAY)}
    var picker by remember{mutableStateOf(false)}
    var lunarPicker by remember{mutableStateOf(false)}
    var deleting by remember{mutableStateOf(false)}
    val saving=operation.running
    val error=operation.error
    val context=LocalContext.current
    val selected=LocalDate.parse(solar)
    val lunar=remember(selected){VietnameseCalendar.solarToLunar(selected)}
    ModalBottomSheet(onDismissRequest={if(!saving)onDismiss()},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).imePadding(),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(if(initial==null)"Thêm ngày đáng nhớ" else "Chỉnh sửa sự kiện",style=MaterialTheme.typography.headlineMedium)
            OutlinedTextField(title,{title=it.take(120)},label={Text("Tên sự kiện *")},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(12.dp))
            ChoiceField("Loại sự kiện",type,types) { type=it; if(it==EventType.DEATH_ANNIVERSARY) repeat=RepeatRule.LUNAR_YEARLY }
            OutlinedCard(onClick={picker=true},modifier=Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.CalendarMonth,null)
                    Column{ Text(selected.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))); Text("Âm lịch: ${lunar.day}/${lunar.month}${if(lunar.isLeap)" nhuận" else ""}/${lunar.year}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            TextButton(onClick={lunarPicker=true}){Icon(Icons.Outlined.NightsStay,null,Modifier.size(17.dp));Spacer(Modifier.width(7.dp));Text("Nhập ngày âm lịch")}
            OutlinedButton(onClick={TimePickerDialog(context,{_,h,m->hour=h;minute=m},hour,minute,true).show()},modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Schedule,null,Modifier.size(19.dp)); Spacer(Modifier.width(8.dp)); Text("Thời gian: %02d:%02d".format(hour,minute))
            }
            Text("Giờ Việt Nam (UTC+7)",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceField("Lặp lại",repeat,repeats){repeat=it}
            if(repeat==RepeatRule.LUNAR_YEARLY) {
                ChoiceField("Khi gặp tháng nhuận",leap,mapOf(LeapMonthPolicy.REGULAR_ONLY to "Chỉ tháng thường",LeapMonthPolicy.LEAP_ONLY to "Chỉ tháng nhuận",LeapMonthPolicy.BOTH to "Cả tháng thường và nhuận")){leap=it}
                ChoiceField("Ngày 30 khi tháng chỉ có 29 ngày",missing,mapOf(MissingDayPolicy.LAST_DAY to "Nhắc vào ngày cuối tháng",MissingDayPolicy.SKIP to "Bỏ qua năm đó")){missing=it}
            }
            OutlinedTextField(note,{note=it.take(2000)},label={Text("Ghi chú")},modifier=Modifier.fillMaxWidth(),minLines=2,maxLines=5,shape=RoundedCornerShape(12.dp))
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                Text("Nhắc tôi",style=MaterialTheme.typography.titleSmall)
                Switch(reminder,{reminder=it})
            }
            if(reminder) ChoiceField("Thời điểm nhắc",days,mapOf(0 to "Đúng ngày",1 to "Trước 1 ngày",3 to "Trước 3 ngày",7 to "Trước 7 ngày",14 to "Trước 14 ngày")){days=it}
            if(reminder) Text("Thông báo có thể trễ khi Android tiết kiệm pin. Cần cho phép thông báo để nhận lời nhắc.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            Button(onClick={
                        onSave(EventEntity(id=initial?.id?:0,title=title.trim(),note=note.trim(),solarDate=solar,hour=hour,minute=minute,repeat=repeat,type=type,
                            reminderDaysBefore=days,reminderEnabled=reminder,lunarDay=if(repeat==RepeatRule.LUNAR_YEARLY)lunar.day else null,
                            lunarMonth=if(repeat==RepeatRule.LUNAR_YEARLY)lunar.month else null,leapMonthPolicy=leap,missingDayPolicy=missing))
            },modifier=Modifier.fillMaxWidth(),enabled=title.isNotBlank()&&!saving,contentPadding=PaddingValues(16.dp)) { Text(if(saving)"Đang lưu…" else "Lưu sự kiện") }
            if(initial!=null) TextButton(onClick={deleting=true},modifier=Modifier.fillMaxWidth(),enabled=!saving) { Text("Xóa sự kiện",color=MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(24.dp))
        }
    }
    if(picker) CalendarDatePicker(selected,{picker=false}){solar=it.toString();picker=false}
    if(lunarPicker)LunarInputDialog(selected,{lunarPicker=false}){solar=it.toString();lunarPicker=false}
    if(deleting) AlertDialog(onDismissRequest={deleting=false},title={Text("Xóa sự kiện?")},text={Text("Sự kiện và lời nhắc liên quan sẽ được xóa khỏi thiết bị.")},confirmButton={TextButton(onClick={
        onDelete(initial!!.id);deleting=false
    }){Text("Xóa",color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton(onClick={deleting=false}){Text("Giữ lại")}})
}

@Composable
private fun LunarInputDialog(initial:LocalDate,onDismiss:()->Unit,onSelected:(LocalDate)->Unit) {
    val lunar=remember(initial){VietnameseCalendar.solarToLunar(initial)}
    var day by rememberSaveable{mutableStateOf(lunar.day.toString())}
    var month by rememberSaveable{mutableStateOf(lunar.month.toString())}
    var year by rememberSaveable{mutableStateOf(lunar.year.toString())}
    var leap by rememberSaveable{mutableStateOf(lunar.isLeap)}
    var error by remember{mutableStateOf<String?>(null)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Chọn ngày âm lịch")},text={
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(day,{day=it.filter(Char::isDigit).take(2)},label={Text("Ngày")},singleLine=true,modifier=Modifier.weight(1f))
                OutlinedTextField(month,{month=it.filter(Char::isDigit).take(2)},label={Text("Tháng")},singleLine=true,modifier=Modifier.weight(1f))
            }
            OutlinedTextField(year,{year=it.filter(Char::isDigit).take(4)},label={Text("Năm âm lịch")},singleLine=true,modifier=Modifier.fillMaxWidth())
            Row(verticalAlignment=Alignment.CenterVertically){Checkbox(leap,{leap=it});Text("Tháng nhuận")}
            Text("Ngày âm sẽ được chuyển sang ngày dương tương ứng. Chọn lặp hằng năm âm lịch để tạo lời nhắc ngày giỗ hoặc sinh nhật.",style=MaterialTheme.typography.bodySmall)
            error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        }
    },confirmButton={TextButton(onClick={
        val d=day.toIntOrNull();val m=month.toIntOrNull();val y=year.toIntOrNull()
        val solar=if(d!=null&&m!=null&&y!=null)VietnameseCalendar.lunarToSolar(vn.lichviet.core.LunarDate(d,m,y,leap)) else null
        if(solar==null)error="Ngày hoặc tháng nhuận không hợp lệ trong khoảng 1900–2100." else onSelected(solar)
    }){Text("Chọn ngày")}},dismissButton={TextButton(onClick=onDismiss){Text("Hủy")}})
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

