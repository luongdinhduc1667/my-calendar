package vn.lichviet.app.ui

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.lichviet.app.CalendarViewModel
import vn.lichviet.app.data.*
import vn.lichviet.app.ui.tools.*
import vn.lichviet.app.widget.*
import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate

private data class Destination(val route:String,val label:String,val icon:ImageVector)
private val destinations=listOf(
    Destination("calendar","Lịch",Icons.Outlined.CalendarMonth),Destination("auspicious","Ngày tốt",Icons.Outlined.WbSunny),
    Destination("horoscope","Tử vi",Icons.Outlined.AutoAwesome),Destination("events","Sự kiện",Icons.Outlined.EventNote),
    Destination("settings","Cài đặt",Icons.Outlined.Tune)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LichVietApp(model:CalendarViewModel) {
    val nav=rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current=entry?.destination?.route?:"calendar"
    val date by model.selected.collectAsStateWithLifecycle()
    val month by model.month.collectAsStateWithLifecycle()
    val week by model.weekMode.collectAsStateWithLifecycle()
    val events by model.events.collectAsStateWithLifecycle()
    val preferences by model.preferences.collectAsStateWithLifecycle()
    val editorOperation by model.editorOperation.collectAsStateWithLifecycle()
    val scope=rememberCoroutineScope()
    val snack=remember{SnackbarHostState()}
    var editor by rememberSaveable{mutableStateOf(false)}
    var editingId by rememberSaveable{mutableStateOf<Long?>(null)}
    val context=LocalContext.current
    var fullscreen by rememberSaveable{mutableStateOf(false)}
    val permissions=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(!granted)scope.launch{snack.showSnackbar("Thông báo chưa được cho phép. Bạn có thể bật trong Cài đặt.")}
    }
    val requestNotifications:()->Unit={
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
            permissions.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    val edit:(EventEntity)->Unit={model.resetEditorOperation();editingId=it.id;editor=true}
    val add:()->Unit={model.resetEditorOperation();editingId=null;editor=true}
    LaunchedEffect(editorOperation.completed) {
        if(editorOperation.completed){
            editor=false
            if(editorOperation.requestNotifications)requestNotifications()
            model.resetEditorOperation()
        }
    }
    fun navigate(route:String) {
        nav.navigate(route){popUpTo(nav.graph.startDestinationId){saveState=true};launchSingleTop=true;restoreState=true}
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
        val rail=maxWidth>=840.dp
        DisposableEffect(fullscreen,rail) {
            val activity=context as? Activity
            val controller=activity?.let{WindowCompat.getInsetsController(it.window,it.window.decorView)}
            controller?.systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if(fullscreen && rail)controller?.hide(WindowInsetsCompat.Type.systemBars()) else controller?.show(WindowInsetsCompat.Type.systemBars())
            onDispose{controller?.show(WindowInsetsCompat.Type.systemBars())}
        }
        Row(Modifier.fillMaxSize()) {
            if(rail) NavigationRail(containerColor=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxHeight().width(92.dp),header={
                Text("越",Modifier.padding(vertical=24.dp),fontFamily=FontFamily.Serif,fontSize=30.sp,color=MaterialTheme.colorScheme.primary)
            }) {
                destinations.forEach{dest->NavigationRailItem(selected=current==dest.route,onClick={navigate(dest.route)},icon={Icon(dest.icon,null)},label={Text(dest.label)},modifier=Modifier.padding(vertical=8.dp))}
            }
            Scaffold(modifier=Modifier.weight(1f),containerColor=MaterialTheme.colorScheme.background,
                topBar={
                    Row(Modifier.fillMaxWidth().padding(horizontal=if(rail)32.dp else 20.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
                        if(current in listOf("converter","holidays")) {
                            IconButton(onClick={nav.popBackStack()}){Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Quay lại")}
                        }
                        Box(Modifier.size(30.dp).background(MaterialTheme.colorScheme.primary,RoundedCornerShape(9.dp)),contentAlignment=Alignment.Center) {
                            Icon(Icons.Outlined.CalendarToday,null,Modifier.size(17.dp),tint=MaterialTheme.colorScheme.onPrimary)
                        }
                        Text("LỊCH VIỆT",Modifier.padding(start=10.dp).weight(1f),fontSize=13.sp,fontWeight=FontWeight.Bold,letterSpacing=2.sp)
                        Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.secondaryContainer.copy(alpha=.65f)) {
                            Row(Modifier.padding(horizontal=10.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                                Icon(Icons.Outlined.CheckCircle,null,Modifier.size(11.dp),tint=MaterialTheme.colorScheme.secondary)
                                Text("Ngoại tuyến",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
                            }
                        }
                        if(rail)IconButton(onClick={fullscreen=!fullscreen}){Icon(if(fullscreen)Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,if(fullscreen)"Thoát toàn màn hình" else "Toàn màn hình")}
                    }
                },
                bottomBar={if(!rail) NavigationBar(containerColor=MaterialTheme.colorScheme.surface,tonalElevation=0.dp,windowInsets=WindowInsets(0)) {
                    destinations.forEach{dest->NavigationBarItem(selected=current==dest.route,onClick={navigate(dest.route)},icon={Icon(dest.icon,dest.label,Modifier.size(22.dp))},label={Text(dest.label,fontSize=10.sp)},colors=NavigationBarItemDefaults.colors(indicatorColor=MaterialTheme.colorScheme.primaryContainer,selectedIconColor=MaterialTheme.colorScheme.primary,selectedTextColor=MaterialTheme.colorScheme.primary))}
                }},snackbarHost={SnackbarHost(snack)}
            ) { padding ->
                NavHost(navController=nav,startDestination="calendar",modifier=Modifier.padding(padding).fillMaxSize()) {
                    composable("calendar") {
                        val eventDates=remember(events,month,date,week) {
                            val start=(if(week)date else month.atDay(1)).minusDays(7)
                            (0..48).map{start.plusDays(it.toLong())}.filter{d->d in VietnameseCalendar.minDate..VietnameseCalendar.maxDate && events.any{Recurrence.occursOn(it,d)}}.toSet()
                        }
                        CalendarScreen(date,month,week,preferences.showLunar,preferences.showHolidays,preferences.mondayFirst,eventDates,
                            model::select,model::changeMonth,model::changeWeekMode,model::goToday,add,
                            {nav.navigate("converter")},{nav.navigate("holidays")}){DayEvents(it,events,edit)}
                    }
                    composable("auspicious"){AuspiciousScreen{model.select(it);navigate("calendar")}}
                    composable("horoscope"){HoroscopeScreen()}
                    composable("events"){EventsScreen(events,add,edit)}
                    composable("settings"){SettingsScreen(preferences,
                        {scope.launch{model.setTheme(it)}},{scope.launch{model.setLunar(it)}},
                        {scope.launch{model.setHolidays(it)}},{scope.launch{model.setMonday(it)}},requestNotifications,
                        model::exportBackup,model::importBackup,model::deleteAllData)}
                    composable("converter"){ConverterScreen(date){model.select(it);navigate("calendar")}}
                    composable("holidays"){HolidaysScreen(month.year){model.select(it);navigate("calendar")}}
                }
            }
        }
    }
    if(editor) EventEditor(events.firstOrNull{it.id==editingId},date,{if(!editorOperation.running){editor=false;model.resetEditorOperation()}},model::saveFromEditor,model::deleteFromEditor,editorOperation)
}

@Composable
private fun SettingsScreen(preferences:UserPreferences,onTheme:(AppTheme)->Unit,onLunar:(Boolean)->Unit,onHolidays:(Boolean)->Unit,onMonday:(Boolean)->Unit,onNotifications:()->Unit,
    onExport:suspend()->String,onImport:suspend(String)->Int,onDeleteAll:suspend()->Unit) {
    val context=LocalContext.current
    var widgets by remember{mutableStateOf(false)}
    var info by remember{mutableStateOf(false)}
    var pendingImport by remember{mutableStateOf<String?>(null)}
    var deleteAll by remember{mutableStateOf(false)}
    var busy by remember{mutableStateOf(false)}
    var message by remember{mutableStateOf<String?>(null)}
    val scope=rememberCoroutineScope()
    val exportFile=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
        if(uri!=null)scope.launch {
            busy=true
            try {
                val json=onExport()
                withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use{it.write(json)}?:error("Không mở được tệp")}
                message="Đã lưu bản sao lưu."
            }catch(e:Exception){message="Không thể lưu bản sao lưu. Vui lòng thử lại."}finally{busy=false}
        }
    }
    val importFile=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)scope.launch {
            busy=true
            try {
                val json=withContext(Dispatchers.IO){
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use{reader->
                        val text=StringBuilder();val buffer=CharArray(8192)
                        while(true){val length=reader.read(buffer);if(length<0)break;text.append(buffer,0,length);require(text.length<=2_000_000){"Tệp quá lớn"}}
                        text.toString()
                    }?:error("Không mở được tệp")
                }
                pendingImport=json
            }catch(e:Exception){message="Không thể đọc tệp. Hãy chọn bản sao lưu Lịch Việt dưới 2 MB."}finally{busy=false}
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(22.dp)) {
        PageHeading("Theo cách của bạn", "Một cuốn lịch, mang dấu ấn riêng")
        SettingsGroup("GIAO DIỆN") {
            ChoiceField("Chủ đề",preferences.theme,mapOf(AppTheme.SYSTEM to "Theo hệ thống",AppTheme.LIGHT to "Sáng · Trắng ngà",AppTheme.DARK to "Tối · Đen than"),onTheme)
            SettingsToggle("Hiển thị lịch âm","Ngày âm bên dưới ngày dương",preferences.showLunar,onLunar)
            SettingsToggle("Đánh dấu ngày lễ","Lễ truyền thống và ngày kỷ niệm",preferences.showHolidays,onHolidays)
            SettingsToggle("Bắt đầu tuần từ thứ Hai","Tắt để bắt đầu từ Chủ Nhật",preferences.mondayFirst,onMonday)
        }
        SettingsGroup("NHẮC NHỞ & WIDGET") {
            OutlinedButton(onClick={onNotifications();context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))},modifier=Modifier.fillMaxWidth()) { Icon(Icons.Outlined.NotificationsNone,null,Modifier.size(19.dp));Spacer(Modifier.width(8.dp));Text("Cài đặt thông báo") }
            OutlinedButton(onClick={widgets=true},modifier=Modifier.fillMaxWidth()){Icon(Icons.Outlined.Widgets,null,Modifier.size(19.dp));Spacer(Modifier.width(8.dp));Text("Thêm widget màn hình chính")}
            Text("Lời nhắc dùng giờ Việt Nam (UTC+7). Android có thể trì hoãn thông báo khi tiết kiệm pin.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SettingsGroup("DỮ LIỆU & QUYỀN RIÊNG TƯ") {
            Text("Sự kiện và cài đặt được lưu trên thiết bị. Không cần tài khoản, không quảng cáo và không gửi dữ liệu lên máy chủ.",style=MaterialTheme.typography.bodyMedium)
            Text("Lịch tính theo UTC+7 trong khoảng 1900–2100. Các ngày lịch sử dùng quy tắc Việt Nam hiện đại; không tái dựng lịch vùng miền trước năm 1968.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick={exportFile.launch("lich-viet-${LocalDate.now(VietnameseCalendar.zoneId)}.json")},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Sao lưu ra tệp")}
            OutlinedButton(onClick={importFile.launch(arrayOf("application/json","text/plain"))},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Khôi phục từ tệp")}
            TextButton(onClick={deleteAll=true},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Xóa dữ liệu cá nhân",color=MaterialTheme.colorScheme.error)}
            if(busy)LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
            message?.let{Text(it,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.primary)}
            TextButton(onClick={info=true}){Text("Về Lịch Việt & nguồn tham khảo")}
        }
        Text("LỊCH VIỆT  /  0.1.0",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(24.dp))
    }
    pendingImport?.let{json->AlertDialog(onDismissRequest={if(!busy)pendingImport=null},title={Text("Khôi phục bản sao lưu?")},text={Text("Toàn bộ sự kiện và cài đặt hiện tại sẽ được thay thế bằng nội dung trong tệp. Bạn nên sao lưu dữ liệu hiện tại trước khi tiếp tục.")},
        confirmButton={TextButton(enabled=!busy,onClick={scope.launch{busy=true;try{val count=onImport(json);message="Đã khôi phục $count sự kiện.";pendingImport=null}catch(e:Exception){message="Tệp sao lưu không hợp lệ hoặc không thể khôi phục.";pendingImport=null}finally{busy=false}}}){Text("Thay thế dữ liệu")}},
        dismissButton={TextButton(enabled=!busy,onClick={pendingImport=null}){Text("Hủy")}})}
    if(deleteAll)AlertDialog(onDismissRequest={if(!busy)deleteAll=false},title={Text("Xóa dữ liệu cá nhân?")},text={Text("Toàn bộ sự kiện, lời nhắc và cài đặt sẽ bị xóa khỏi thiết bị. Hành động này không thể hoàn tác.")},
        confirmButton={TextButton(enabled=!busy,onClick={scope.launch{busy=true;try{onDeleteAll();message="Đã xóa dữ liệu cá nhân.";deleteAll=false}catch(e:Exception){message="Chưa thể xóa dữ liệu.";deleteAll=false}finally{busy=false}}}){Text("Xóa tất cả",color=MaterialTheme.colorScheme.error)}},
        dismissButton={TextButton(enabled=!busy,onClick={deleteAll=false}){Text("Giữ lại")}})
    if(widgets) AlertDialog(onDismissRequest={widgets=false},title={Text("Lịch Việt trên màn hình chính")},text={
        Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            val manager=AppWidgetManager.getInstance(context)
            Text("Chọn widget để xem lịch mà không cần mở ứng dụng.")
            listOf("Ngày hôm nay" to TodayWidgetProvider::class.java,"Âm dương" to LunarWidgetProvider::class.java,"Lịch tháng" to MonthWidgetProvider::class.java,"Sự kiện sắp tới" to UpcomingWidgetProvider::class.java).forEach{(title,provider)->
                if(manager.isRequestPinAppWidgetSupported) OutlinedButton(onClick={manager.requestPinAppWidget(ComponentName(context,provider),null,null);widgets=false},modifier=Modifier.fillMaxWidth()){Text(title)}
            }
            if(!manager.isRequestPinAppWidgetSupported) Text("Nhấn giữ màn hình chính → Tiện ích (Widgets) → Lịch Việt để thêm widget.")
        }
    },confirmButton={TextButton(onClick={widgets=false}){Text("Đóng")}})
    if(info) AlertDialog(onDismissRequest={info=false},title={Text("Lịch Việt")},text={Text("Lịch âm Việt Nam tính từ thời điểm sóc và kinh độ Mặt Trời, theo múi giờ cố định UTC+7. Công thức thiên văn Meeus, tham chiếu mã nguồn MIT của Sonia Keys; kiểm thử đối chiếu mốc thiên văn NASA.\n\nNgày tốt và giờ hoàng đạo theo quy tắc dân gian được ghi trong tài liệu dự án. Đây là thông tin văn hóa để tham khảo. Ứng dụng không tạo dự báo ngẫu nhiên.\n\nPhiên bản 0.1.0 · Android Native / Jetpack Compose")},confirmButton={TextButton(onClick={info=false}){Text("Đã hiểu")}})
}

@Composable
private fun SettingsGroup(title:String,content:@Composable ColumnScope.()->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text(title,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.tertiary,letterSpacing=1.5.sp)
        Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface,border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp),content=content)
        }
    }
}

@Composable
private fun SettingsToggle(title:String,subtitle:String,checked:Boolean,onChange:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.titleSmall);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        Switch(checked,onChange)
    }
}
