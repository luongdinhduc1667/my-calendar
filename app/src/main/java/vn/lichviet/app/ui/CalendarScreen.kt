package vn.lichviet.app.ui

import vn.lichviet.app.zodiac.CulturalSourceButton
import vn.lichviet.app.zodiac.ZodiacText
import vn.lichviet.core.zodiac.CulturalSource
import vn.lichviet.core.zodiac.ZodiacEngine
import vn.lichviet.core.zodiac.ZodiacSign

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import vn.lichviet.app.R
import vn.lichviet.app.data.EventEntity
import vn.lichviet.app.ics.EventOccurrence
import vn.lichviet.app.ics.EventLayoutEngine
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.lichviet.core.VietnameseCalendar
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

val ViLocale: Locale = Locale.forLanguageTag("vi-VN")
val DateLabel: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, dd 'tháng' MM", ViLocale)

@Composable
fun CalendarScreen(
    selected: LocalDate, month: YearMonth, weekMode: Boolean, showLunar: Boolean,
    showHolidays: Boolean, mondayFirst: Boolean, eventDates: Set<LocalDate>,
    onSelect: (LocalDate) -> Unit, onMonth: (YearMonth) -> Unit, onWeekMode: (Boolean) -> Unit,
    onToday: () -> Unit, onAddEvent: () -> Unit, onConverter: () -> Unit,
    onHolidays: () -> Unit, occurrences: List<EventOccurrence> = emptyList(),
    onEditEvent: (EventEntity) -> Unit = {}, showWeekNumbers: Boolean = false,
    showAuspiciousHours: Boolean = true, eventsContent: @Composable (LocalDate) -> Unit
) {
    var picker by remember { mutableStateOf(false) }
    val today = LocalDate.now(VietnameseCalendar.zoneId)
    val fontScale=LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val expanded = maxWidth >= (760*fontScale.coerceAtLeast(1f)).dp
        if (expanded) {
            Row(Modifier.fillMaxSize().padding(horizontal = 32.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Column(Modifier.weight(1.25f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                    Spacer(Modifier.height(2.dp))
                    PageHeading(stringResource(R.string.calendar_title), stringResource(R.string.calendar_subtitle))
                    MonthCard(selected, month, weekMode, showLunar, showHolidays, mondayFirst, eventDates,
                        onSelect, onMonth, onWeekMode, onToday, { picker = true },dense=true,occurrences=occurrences,onEditEvent=onEditEvent,showWeekNumbers=showWeekNumbers)
                    QuickActions(onConverter, onHolidays)
                    Spacer(Modifier.height(24.dp))
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.calendar_your_day), style = MaterialTheme.typography.headlineMedium)
                        TextButton(onClick = onAddEvent) { Icon(Icons.Outlined.Add, null, Modifier.size(17.dp)); Text(stringResource(R.string.calendar_event)) }
                    }
                    DayHero(selected,compact=true)
                    eventsContent(selected)
                    DayInformation(selected,showHolidays,showAuspiciousHours)
                    TraditionNote()
                    Spacer(Modifier.height(24.dp))
                }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                PageHeading(stringResource(R.string.calendar_title), stringResource(R.string.calendar_subtitle))
                DayHero(selected)
                MonthCard(selected, month, weekMode, showLunar, showHolidays, mondayFirst, eventDates,
                    onSelect, onMonth, onWeekMode, onToday, { picker = true },occurrences=occurrences,onEditEvent=onEditEvent,showWeekNumbers=showWeekNumbers)
                QuickActions(onConverter, onHolidays)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.calendar_your_day), style = MaterialTheme.typography.headlineMedium)
                    TextButton(onClick = onAddEvent) { Icon(Icons.Outlined.Add, null, Modifier.size(17.dp)); Text(stringResource(R.string.calendar_add_event)) }
                }
                DayInformation(selected,showHolidays,showAuspiciousHours)
                eventsContent(selected)
                TraditionNote()
                Spacer(Modifier.height(22.dp))
            }
        }
    }
    if (picker) CalendarDatePicker(selected, onDismiss = { picker = false }) { onSelect(it); picker = false }
}

@Composable
fun PageHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DayHero(date: LocalDate,compact:Boolean=false) {
    val context=LocalContext.current
    val locale=LocalConfiguration.current.locales[0]
    val lunar = remember(date) { VietnameseCalendar.solarToLunar(date) }
    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.primaryContainer
    Box(Modifier.fillMaxWidth().heightIn(min = if(compact)132.dp else 172.dp).clip(RoundedCornerShape(24.dp)).background(background)) {
        LandscapeArt(Modifier.align(Alignment.BottomEnd).fillMaxWidth(0.54f).height(if(compact)132.dp else 165.dp))
        Column(Modifier.padding(if(compact)16.dp else 22.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(date.format(DateTimeFormatter.ofPattern("EEEE", locale)).uppercase(locale), color = primary,
                style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                Text(date.dayOfMonth.toString().padStart(2, '0'), fontSize = if(compact)52.sp else 68.sp, fontFamily = FontFamily.Serif,
                    color = primary, lineHeight = if(compact)60.sp else 76.sp)
                Column {
                    Text(date.format(DateTimeFormatter.ofPattern("MMMM",locale)), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(date.year.toString(), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .65f))
                }
            }
            Text(CalendarText.lunarFull(context,lunar),
                color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun LandscapeArt(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawCircle(Color(0xFFD8A364).copy(alpha = .65f), w * .15f, Offset(w * .64f, h * .29f))
        fun mountain(points: List<Offset>, color: Color) {
            val p = Path().apply { moveTo(0f,h); points.forEach { lineTo(it.x,it.y) }; lineTo(w,h); close() }
            drawPath(p, color)
        }
        mountain(listOf(Offset(0f,h*.9f),Offset(w*.17f,h*.55f),Offset(w*.28f,h*.67f),Offset(w*.49f,h*.37f),Offset(w*.72f,h*.72f),Offset(w*.9f,h*.49f),Offset(w,h*.63f)),Color(0xFFAAB4A0).copy(alpha=.38f))
        mountain(listOf(Offset(0f,h),Offset(w*.32f,h*.75f),Offset(w*.48f,h*.86f),Offset(w*.74f,h*.63f),Offset(w,h*.83f)),Color(0xFF7C947C).copy(alpha=.36f))
        mountain(listOf(Offset(0f,h),Offset(w*.48f,h*.92f),Offset(w*.72f,h*.83f),Offset(w,h*.94f)),Color(0xFF4D6F5E).copy(alpha=.44f))
        val ink = Color(0xFF7F775B).copy(alpha=.7f)
        val x=w*.58f; val y=h*.8f
        drawLine(ink,Offset(x,y),Offset(x,y-h*.12f),2f)
        drawLine(ink,Offset(x+w*.18f,y),Offset(x+w*.18f,y-h*.12f),2f)
        val roof=Path().apply{moveTo(x-w*.05f,y-h*.12f);quadraticTo(x+w*.04f,y-h*.14f,x+w*.09f,y-h*.25f);quadraticTo(x+w*.14f,y-h*.14f,x+w*.23f,y-h*.12f)}
        drawPath(roof,ink,style=Stroke(3f))
        drawLine(ink,Offset(x-w*.02f,y-h*.1f),Offset(x+w*.2f,y-h*.1f),2f)
        for(i in 0..2) {
            val yy=h*(.36f+i*.055f)
            val cloud=Path().apply{moveTo(w*.06f,yy);cubicTo(w*.22f,yy-h*.04f,w*.18f,yy+h*.02f,w*.36f,yy-h*.02f)}
            drawPath(cloud,Color.White.copy(alpha=.48f),style=Stroke(2f))
        }
    }
}

@Composable
private fun MonthCard(
    selected: LocalDate, month: YearMonth, weekMode: Boolean, showLunar: Boolean, showHolidays: Boolean,
    mondayFirst: Boolean, eventDates: Set<LocalDate>, onSelect: (LocalDate) -> Unit,
    onMonth: (YearMonth) -> Unit, onWeekMode: (Boolean) -> Unit, onToday: () -> Unit, onPicker: () -> Unit,dense:Boolean=false,occurrences:List<EventOccurrence> = emptyList(),onEditEvent:(EventEntity)->Unit = {},showWeekNumbers:Boolean=false
) {
    val context=LocalContext.current
    val locale=LocalConfiguration.current.locales[0]
    val fontScale=LocalDensity.current.fontScale
    val today = LocalDate.now(VietnameseCalendar.zoneId)
    val startOfWeek = if(mondayFirst) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
    val start = remember(month, selected, weekMode, mondayFirst) {
        (if (weekMode) selected else month.atDay(1)).with(TemporalAdjusters.previousOrSame(startOfWeek))
    }
    val count = if(weekMode) 7 else (((month.atEndOfMonth().toEpochDay()-start.toEpochDay()+1+6)/7)*7).toInt()
    val dates = remember(start,count) { List(count) { start.plusDays(it.toLong()) } }
    val dayData = remember(dates) { dates.associateWith { if(it in VietnameseCalendar.minDate..VietnameseCalendar.maxDate) VietnameseCalendar.dayData(it) else null } }
    val firstEventColors = remember(dates,occurrences) { dates.associateWith { date ->
        occurrences.firstOrNull{date>=it.startDate&&date<it.endDateExclusive}?.let{it.colorArgb?:EventColors.default(it.event.type)}
    } }
    val weekColumnWidth=(30*fontScale).dp
    var drag by remember { mutableFloatStateOf(0f) }
    val change: (Long) -> Unit = { offset ->
        if(weekMode) onSelect(selected.plusWeeks(offset).coerceIn(VietnameseCalendar.minDate, VietnameseCalendar.maxDate))
        else onMonth(month.plusMonths(offset).coerceIn(YearMonth.of(1900,1),YearMonth.of(2100,12)))
    }
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Weighted day cells need a finite width inside the horizontal scrolling viewport.
        val calendarWidth=maxOf(maxWidth,((320+if(showWeekNumbers)30 else 0)*fontScale).dp)
        val scrollModifier=if(calendarWidth>maxWidth) Modifier.horizontalScroll(rememberScrollState()) else Modifier
        Column(Modifier.animateContentSize(animationSpec=appMotionSpec()).then(scrollModifier).width(calendarWidth).padding(if(dense)12.dp else 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable(onClick=onPicker).padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(CalendarText.monthName(context,month), modifier=Modifier.testTag("month_title"), style=MaterialTheme.typography.titleLarge)
                    Text(" / ${month.year}", modifier=Modifier.testTag("month_year"), style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(Icons.Outlined.KeyboardArrowDown,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick={change(-1)},Modifier.size(40.dp).testTag("previous_month")) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft,stringResource(R.string.calendar_previous))
                }
                IconButton(onClick={change(1)},Modifier.size(40.dp).testTag("next_month")) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,stringResource(R.string.calendar_next))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top=if(dense)4.dp else 8.dp,bottom=if(dense)8.dp else 16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                Row(Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.background).padding(3.dp)) {
                    listOf(false to stringResource(R.string.calendar_month), true to stringResource(R.string.calendar_week)).forEach { (value,text) ->
                        val chosen=weekMode==value
                        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if(chosen) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable{onWeekMode(value)}.padding(horizontal=19.dp,vertical=8.dp)) {
                            Text(text,style=MaterialTheme.typography.labelMedium,color=if(chosen)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                TextButton(onClick=onToday,contentPadding=PaddingValues(horizontal=8.dp)) { Text(stringResource(R.string.calendar_today),fontSize=12.sp) }
            }
            val names=List(7){startOfWeek.plus(it.toLong()).getDisplayName(java.time.format.TextStyle.SHORT,locale)}
            Row(Modifier.fillMaxWidth().padding(bottom=10.dp)) {
                if(showWeekNumbers)Text(stringResource(R.string.calendar_week_column),Modifier.width(weekColumnWidth),textAlign=TextAlign.Center,style=MaterialTheme.typography.labelSmall)
                names.forEach { Text(it,Modifier.weight(1f),textAlign=TextAlign.Center,style=MaterialTheme.typography.labelSmall,
                    color=if(it==DayOfWeek.SUNDAY.getDisplayName(java.time.format.TextStyle.SHORT,locale))MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Column(Modifier.testTag("calendar_grid").pointerInput(month,weekMode,selected) {
                detectHorizontalDragGestures(onDragStart={drag=0f},onDragEnd={if(drag>100)change(-1) else if(drag< -100)change(1)}) { _, amount -> drag+=amount }
            }) {
                dates.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(2.dp)) {
                        if(showWeekNumbers) {
                            val weekNumber=VietnameseCalendar.isoWeekForRow(week.first())
                            val weekLabel=stringResource(R.string.calendar_week_label,weekNumber.number,weekNumber.year)
                            Text(weekNumber.number.toString(),Modifier.width(weekColumnWidth).padding(top=8.dp).testTag("week_${weekNumber.year}_${weekNumber.number}")
                                .semantics{contentDescription=weekLabel},textAlign=TextAlign.Center,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        week.forEach { date ->
                            val info=dayData.getValue(date)
                            val ld=info?.lunar
                            val active=date==selected
                            val inMonth=YearMonth.from(date)==month
                            val holiday=showHolidays && info?.holidays?.isNotEmpty()==true
                            val holidayLabel=if(holiday)info!!.holidays.joinToString(" · "){CalendarText.holiday(context,it)} else ""
                            val cellColor by animateColorAsState(if(active)MaterialTheme.colorScheme.primary else if(date==today)MaterialTheme.colorScheme.primaryContainer else Color.Transparent,animationSpec=appMotionSpec(),label="selection")
                            Column(Modifier.weight(1f).heightIn(min=(if(dense)48*fontScale else 61*fontScale).dp).clip(RoundedCornerShape(13.dp)).background(cellColor)
                                .clickable(enabled=ld!=null){onSelect(date)}.semantics{this.selected=active;contentDescription=CalendarText.fullDate(context,date)+(ld?.let{", "+CalendarText.lunarFull(context,it)}?:", "+context.getString(R.string.calendar_out_of_range))+if(holidayLabel.isNotBlank())", $holidayLabel" else ""}
                                .testTag("date_$date").padding(vertical=if(dense)4.dp else 6.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                Text(date.dayOfMonth.toString(),fontSize=17.sp,lineHeight=if(dense)20.sp else 22.sp,fontWeight=if(active||date==today)FontWeight.SemiBold else FontWeight.Medium,
                                    color=if(active)MaterialTheme.colorScheme.onPrimary else if(!inMonth)MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.4f)
                                    else if(date.dayOfWeek==DayOfWeek.SUNDAY||holiday)MaterialTheme.colorScheme.primary else if(date.dayOfWeek==DayOfWeek.SATURDAY)MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface)
                                if(showLunar && ld!=null) Text(if(ld.day==1)"1/${ld.month}${if(ld.isLeap)"n" else ""}" else ld.day.toString(),fontSize=10.sp,lineHeight=if(dense)12.sp else 14.sp,
                                    color=if(active)MaterialTheme.colorScheme.onPrimary.copy(alpha=.7f) else if(ld.day==1||ld.day==15)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=if(inMonth).8f else .4f))
                                Spacer(Modifier.height(3.dp))
                                Box(Modifier.size(3.dp).background(if(date in eventDates) {if(active)MaterialTheme.colorScheme.onPrimary else firstEventColors[date]?.let{Color(it)}?:MaterialTheme.colorScheme.tertiary} else Color.Transparent,CircleShape))
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        if(showWeekNumbers)Spacer(Modifier.width(weekColumnWidth+2.dp))
                        CalendarEventBars(week.first(),occurrences,onSelect,onEditEvent)
                    }
                }
            }
            if(!dense)HorizontalDivider(Modifier.padding(top=12.dp,bottom=12.dp),color=MaterialTheme.colorScheme.outlineVariant)
            if(!dense)Row(horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(5.dp).background(MaterialTheme.colorScheme.tertiary,CircleShape))
                    Text(stringResource(R.string.calendar_has_events),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(stringResource(R.string.calendar_zone),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        }
    }
}

@Composable
private fun CalendarEventBars(start: LocalDate, occurrences: List<EventOccurrence>, onSelect: (LocalDate) -> Unit, onEdit: (EventEntity) -> Unit) {
    val fontScale=LocalDensity.current.fontScale
    val rowHeight=(24*fontScale).dp
    val layout = remember(start, occurrences) { EventLayoutEngine.week(occurrences, start) }
    if (layout.laneCount == 0) return
    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(rowHeight * layout.laneCount)) {
            val unit = maxWidth / 7
            layout.segments.forEach { segment ->
                val color = Color(segment.occurrence.colorArgb ?: EventColors.default(segment.occurrence.event.type))
                Surface(onClick = { onEdit(segment.occurrence.event) },
                    modifier = Modifier.offset(x = unit * segment.startColumn, y = rowHeight * segment.lane)
                        .width(unit * segment.span).height((22*fontScale).dp).padding(horizontal = 1.dp)
                        .testTag("event_bar_${segment.occurrence.eventId}_${segment.occurrence.occurrenceKey}"),
                    shape = RoundedCornerShape(topStart = if (segment.begins) 6.dp else 0.dp, bottomStart = if (segment.begins) 6.dp else 0.dp,
                        topEnd = if (segment.ends) 6.dp else 0.dp, bottomEnd = if (segment.ends) 6.dp else 0.dp),
                    color = color, contentColor = EventColors.foreground(color)) {
                    Text(segment.occurrence.title, Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        fontSize = 10.sp, lineHeight = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (layout.overflow.isNotEmpty()) Row(Modifier.fillMaxWidth()) {
            repeat(7) { column ->
                val day = start.plusDays(column.toLong()); val count = layout.overflow[day] ?: 0
                val overflowLabel=stringResource(R.string.calendar_more,count)
                Box(Modifier.weight(1f).heightIn(min = 48.dp).clickable(enabled = count > 0) { onSelect(day) }, contentAlignment = Alignment.Center) {
                    if (count > 0) Text("+$count", style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.semantics { contentDescription = overflowLabel })
                }
            }
        }
    }
}

@Composable
private fun QuickActions(onConverter:()->Unit,onHolidays:()->Unit) {
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        listOf(Triple(stringResource(R.string.calendar_converter),Icons.Outlined.SwapHoriz,onConverter),Triple(stringResource(R.string.calendar_holidays),Icons.Outlined.Celebration,onHolidays)).forEach { (title,icon,click) ->
            Surface(onClick=click,Modifier.weight(1f),shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface,
                border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
                Row(Modifier.padding(horizontal=14.dp,vertical=16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                    Icon(icon,null,Modifier.size(20.dp),tint=MaterialTheme.colorScheme.tertiary)
                    Text(title,style=MaterialTheme.typography.labelMedium,modifier=Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward,null,Modifier.size(14.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun DayInformation(date:LocalDate,showHolidays:Boolean=true,showAuspiciousHours:Boolean=true) {
    val context=LocalContext.current
    val zodiacLabels=ZodiacText(context)
    val dayPair=remember(date){VietnameseCalendar.dayCanChiId(date)}
    val dayNapAm=remember(dayPair){ZodiacEngine.napAm(dayPair)}
    val data=remember(date){VietnameseCalendar.dayData(date)}
    val lunar=data.lunar
    val holidays=if(showHolidays)data.holidays.map{CalendarText.holiday(context,it)} else emptyList()
    Surface(shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.surface,
        border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.secondaryContainer,RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center) {
                    Icon(Icons.Outlined.NightsStay,null,tint=MaterialTheme.colorScheme.secondary)
                }
                Column {
                    Text(CalendarText.lunarFull(context,lunar),style=MaterialTheme.typography.titleMedium)
                    Text(CalendarText.yearCanChi(context,lunar.year),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if(holidays.isNotEmpty()) Text(holidays.joinToString(" · "),color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.bodyMedium)
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            InfoLine(stringResource(R.string.calendar_day_stems),CalendarText.dayCanChi(context,date))
            InfoLine(stringResource(R.string.zd_day_nap_am),zodiacLabels.napAm(dayNapAm))
            InfoLine(stringResource(R.string.zd_day_opposition),zodiacLabels.sign(ZodiacEngine.opposition(ZodiacSign.entries[dayPair.branchIndex])))
            InfoLine(stringResource(R.string.calendar_month_stems),CalendarText.monthCanChi(context,lunar))
            InfoLine(stringResource(R.string.calendar_solar_term),CalendarText.solarTerm(context,date))
            CulturalSourceButton(CulturalSource.ELEMENTS_NAP_AM,CulturalSource.BRANCH_RELATIONS)
            if(showAuspiciousHours) {
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.WbSunny,null,Modifier.size(19.dp),tint=MaterialTheme.colorScheme.tertiary)
                Text(stringResource(R.string.calendar_hours),style=MaterialTheme.typography.titleSmall)
            }
            CalendarText.auspiciousHours(context,date).chunked(2).forEach { pair ->
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    pair.forEach { hour ->
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(MaterialTheme.colorScheme.background).padding(horizontal=10.dp,vertical=9.dp)) {
                            Text(hour,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun InfoLine(label:String,value:String) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
        Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium,textAlign=TextAlign.End)
    }
}

@Composable
fun TraditionNote() {
    Row(Modifier.padding(horizontal=6.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Outlined.Info,null,Modifier.size(15.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.calendar_tradition),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDatePicker(initial:LocalDate,onDismiss:()->Unit,inputMode:Boolean=false,onDate:(LocalDate)->Unit) {
    val state=rememberDatePickerState(initialSelectedDateMillis=initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),yearRange=1900..2100,
        initialDisplayMode=if(inputMode)DisplayMode.Input else DisplayMode.Picker)
    DatePickerDialog(onDismissRequest=onDismiss,confirmButton={TextButton(onClick={state.selectedDateMillis?.let{onDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())}}){Text(stringResource(R.string.event_select_date))}},dismissButton={TextButton(onClick=onDismiss){Text(stringResource(R.string.event_cancel))}}) {
        DatePicker(state=state,title={Text(stringResource(R.string.event_select_solar),Modifier.padding(start=24.dp,top=20.dp))},showModeToggle=true)
    }
}
