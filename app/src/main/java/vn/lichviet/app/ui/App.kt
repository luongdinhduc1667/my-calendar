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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.DpSize
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.lichviet.app.CalendarViewModel
import vn.lichviet.app.AppLocaleManager
import vn.lichviet.app.R
import vn.lichviet.app.ics.EventOccurrence
import vn.lichviet.app.ics.EventOccurrenceEngine
import vn.lichviet.app.ics.IcsWorkspace
import vn.lichviet.app.data.*
import vn.lichviet.app.ui.tools.*
import vn.lichviet.app.widget.*
import vn.lichviet.app.reminder.ReminderPermissionManager
import vn.lichviet.app.reminder.ReminderScheduler
import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate

private data class Destination(val route:String,val label:Int,val icon:ImageVector)
private val destinations=listOf(
    Destination("calendar",R.string.nav_calendar,Icons.Outlined.CalendarMonth),Destination("auspicious",R.string.nav_auspicious,Icons.Outlined.WbSunny),
    Destination("horoscope",R.string.nav_horoscope,Icons.Outlined.AutoAwesome),Destination("events",R.string.nav_events,Icons.Outlined.EventNote),
    Destination("settings",R.string.nav_settings,Icons.Outlined.Tune)
)

@OptIn(ExperimentalMaterial3Api::class,ExperimentalMaterial3WindowSizeClassApi::class)
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
    val incomingIcs by model.incomingIcsUri.collectAsStateWithLifecycle()
    val reminderSetup by model.reminderSetup.collectAsStateWithLifecycle()
    val scope=rememberCoroutineScope()
    val snack=remember{SnackbarHostState()}
    var editor by rememberSaveable{mutableStateOf(false)}
    var editingId by rememberSaveable{mutableStateOf<Long?>(null)}
    val context=LocalContext.current
    var fullscreen by rememberSaveable{mutableStateOf(false)}
    var replayOnboarding by rememberSaveable{mutableStateOf(false)}
    var dateSearch by rememberSaveable{mutableStateOf(false)}
    val animationsEnabled=LocalAppAnimations.current
    val permissions=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(granted) ReminderScheduler.requestRefresh(context)
        else scope.launch{snack.showSnackbar(context.getString(R.string.notifications_denied))}
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
    LaunchedEffect(incomingIcs,replayOnboarding) { if(incomingIcs!=null && !replayOnboarding)navigate("events") }
    LaunchedEffect(reminderSetup,replayOnboarding) {
        if(reminderSetup && !replayOnboarding) {
            navigate("settings")
            model.consumeReminderSetup()
            snack.showSnackbar(context.getString(R.string.notifications_setup_hint))
        }
    }
    val language = LocalConfiguration.current.let { AppLocaleManager.language(context) }
    val changeLanguage:(String)->Unit = { tag -> (context as? Activity)?.let { AppLocaleManager.setLanguage(it,tag) } }
    if(replayOnboarding) {
        OnboardingScreen(preferences,language,changeLanguage,
            {scope.launch{model.setTheme(it)}},{scope.launch{model.setMonday(it)}},{scope.launch{model.setLunar(it)}}) { setup ->
            replayOnboarding=false
            if(setup)scope.launch{model.completeOnboarding(true)}
        }
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
        val rail=WindowSizeClass.calculateFromSize(DpSize(maxWidth,maxHeight)).widthSizeClass>=WindowWidthSizeClass.Expanded
        val railWidth=(92*LocalDensity.current.fontScale.coerceAtLeast(1f)).dp
        DisposableEffect(fullscreen,rail) {
            val activity=context as? Activity
            val controller=activity?.let{WindowCompat.getInsetsController(it.window,it.window.decorView)}
            controller?.systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if(fullscreen && rail)controller?.hide(WindowInsetsCompat.Type.systemBars()) else controller?.show(WindowInsetsCompat.Type.systemBars())
            onDispose{controller?.show(WindowInsetsCompat.Type.systemBars())}
        }
        Row(Modifier.fillMaxSize()) {
            if(rail) NavigationRail(containerColor=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxHeight().width(railWidth),header={
                Text(stringResource(R.string.app_monogram),Modifier.padding(vertical=24.dp),fontFamily=FontFamily.Serif,fontSize=30.sp,color=MaterialTheme.colorScheme.primary)
            }) {
                destinations.forEach{dest->NavigationRailItem(selected=current==dest.route,onClick={navigate(dest.route)},icon={Icon(dest.icon,null)},label={Text(stringResource(dest.label))},modifier=Modifier.fillMaxWidth().padding(vertical=8.dp))}
            }
            Scaffold(modifier=Modifier.weight(1f),containerColor=MaterialTheme.colorScheme.background,
                topBar={
                    Row(Modifier.fillMaxWidth().padding(horizontal=if(rail)32.dp else 20.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
                        if(current in listOf("converter","holidays")) {
                            IconButton(onClick={nav.popBackStack()}){Icon(Icons.AutoMirrored.Outlined.ArrowBack,stringResource(R.string.back))}
                        }
                        Box(Modifier.size(30.dp).background(MaterialTheme.colorScheme.primary,RoundedCornerShape(9.dp)),contentAlignment=Alignment.Center) {
                            Icon(Icons.Outlined.CalendarToday,null,Modifier.size(17.dp),tint=MaterialTheme.colorScheme.onPrimary)
                        }
                        Text(stringResource(R.string.app_wordmark),Modifier.padding(start=10.dp).weight(1f),fontSize=13.sp,fontWeight=FontWeight.Bold,letterSpacing=2.sp)
                        if(current=="calendar")IconButton(onClick={dateSearch=true}){Icon(Icons.Outlined.Search,stringResource(R.string.calendar_find_date))}
                        Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.secondaryContainer.copy(alpha=.65f)) {
                            Row(Modifier.padding(horizontal=10.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                                Icon(Icons.Outlined.CheckCircle,null,Modifier.size(11.dp),tint=MaterialTheme.colorScheme.secondary)
                                Text(stringResource(R.string.offline),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
                            }
                        }
                        if(rail)IconButton(onClick={fullscreen=!fullscreen}){Icon(if(fullscreen)Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,stringResource(if(fullscreen)R.string.fullscreen_exit else R.string.fullscreen_enter))}
                    }
                },
                bottomBar={if(!rail) NavigationBar(containerColor=MaterialTheme.colorScheme.surface,tonalElevation=0.dp,windowInsets=WindowInsets(0)) {
                    destinations.forEach{dest->NavigationBarItem(selected=current==dest.route,onClick={navigate(dest.route)},icon={Icon(dest.icon,stringResource(dest.label),Modifier.size(22.dp))},label={Text(stringResource(dest.label),fontSize=10.sp)},colors=NavigationBarItemDefaults.colors(indicatorColor=MaterialTheme.colorScheme.primaryContainer,selectedIconColor=MaterialTheme.colorScheme.primary,selectedTextColor=MaterialTheme.colorScheme.primary))}
                }},snackbarHost={SnackbarHost(snack)}
            ) { padding ->
                NavHost(navController=nav,startDestination="calendar",modifier=Modifier.padding(padding).fillMaxSize(),
                    enterTransition={if(animationsEnabled)androidx.compose.animation.fadeIn() else androidx.compose.animation.EnterTransition.None},
                    exitTransition={if(animationsEnabled)androidx.compose.animation.fadeOut() else androidx.compose.animation.ExitTransition.None},
                    popEnterTransition={if(animationsEnabled)androidx.compose.animation.fadeIn() else androidx.compose.animation.EnterTransition.None},
                    popExitTransition={if(animationsEnabled)androidx.compose.animation.fadeOut() else androidx.compose.animation.ExitTransition.None}) {
                    composable("calendar") {
                        val start=remember(month,date,week,preferences.mondayFirst) {
                            (if(week)date else month.atDay(1)).with(java.time.temporal.TemporalAdjusters.previousOrSame(
                                if(preferences.mondayFirst)java.time.DayOfWeek.MONDAY else java.time.DayOfWeek.SUNDAY))
                        }
                        val occurrences by produceState(emptyList<EventOccurrence>(),events,start,week) {
                            try {
                                value=withContext(Dispatchers.Default) {
                                    EventOccurrenceEngine.between(events,start,start.plusDays(if(week)7 else 42))
                                }
                            } catch(cancelled:kotlinx.coroutines.CancellationException) { throw cancelled }
                            catch(error:Exception) {
                                value=emptyList()
                                snack.showSnackbar(context.getString(R.string.calendar_events_error))
                            }
                        }
                        val eventDates=remember(occurrences,start,week) {
                            val end=start.plusDays(if(week)7 else 42)
                            occurrences.flatMap { item ->
                                generateSequence(maxOf(start,item.startDate)){it.plusDays(1)}
                                    .takeWhile{it<end && it<item.endDateExclusive}.toList()
                            }.toSet()
                        }
                        CalendarScreen(date,month,week,preferences.showLunar,preferences.showHolidays,preferences.mondayFirst,eventDates,
                            model::select,model::changeMonth,model::changeWeekMode,model::goToday,add,
                            {nav.navigate("converter")},{nav.navigate("holidays")},
                            eventsContent={DayEvents(it,events,edit)},occurrences=occurrences,onEditEvent=edit,
                            showWeekNumbers=preferences.showWeekNumbers,showAuspiciousHours=preferences.showAuspiciousHours)
                    }
                    composable("auspicious"){AuspiciousScreen{model.select(it);navigate("calendar")}}
                    composable("horoscope"){HoroscopeScreen(date){model.select(it);navigate("calendar")}}
                    composable("events"){IcsWorkspace(events,incomingIcs?.let(android.net.Uri::parse),model::consumeIcsUri,add,edit)}
                    composable("settings"){SettingsScreen(preferences,
                        {scope.launch{model.setTheme(it)}},{scope.launch{model.setLunar(it)}},
                        {scope.launch{model.setHolidays(it)}},{scope.launch{model.setMonday(it)}},requestNotifications,
                        model::exportBackup,model::importBackup,model::deleteAllData,language,changeLanguage,{replayOnboarding=true},
                        {scope.launch{model.setWeekNumbers(it)}},{scope.launch{model.setAuspiciousHours(it)}},{scope.launch{model.setAnimations(it)}})}
                    composable("converter"){ConverterScreen(date){model.select(it);navigate("calendar")}}
                    composable("holidays"){HolidaysScreen(month.year){model.select(it);navigate("calendar")}}
                }
            }
        }
    }
    if(dateSearch) CalendarDatePicker(date,onDismiss={dateSearch=false},inputMode=true){model.select(it);dateSearch=false}
    if(editor) {
        val initial=events.firstOrNull{it.id==editingId}
        if(editingId==null || initial!=null) EventEditor(initial,date,{if(!editorOperation.running){editor=false;model.resetEditorOperation()}},
            model::saveFromEditor,model::deleteFromEditor,editorOperation,model::reminderOffsets)
        else Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()}
    }
}

@Composable
private fun SettingsScreen(preferences:UserPreferences,onTheme:(AppTheme)->Unit,onLunar:(Boolean)->Unit,onHolidays:(Boolean)->Unit,onMonday:(Boolean)->Unit,onNotifications:()->Unit,
    onExport:suspend()->String,onImport:suspend(String)->Int,onDeleteAll:suspend()->Unit,
    language:String,onLanguage:(String)->Unit,onReplay:()->Unit,
    onWeekNumbers:(Boolean)->Unit,onAuspiciousHours:(Boolean)->Unit,onAnimations:(Boolean)->Unit) {
    val context=LocalContext.current
    val version=remember(context){context.packageManager.getPackageInfo(context.packageName,0).versionName.orEmpty()}
    var widgets by remember{mutableStateOf(false)}
    var info by remember{mutableStateOf(false)}
    var licenses by remember{mutableStateOf(false)}
    var pendingImport by remember{mutableStateOf<String?>(null)}
    var deleteAll by remember{mutableStateOf(false)}
    var busy by remember{mutableStateOf(false)}
    var message by remember{mutableStateOf<String?>(null)}
    var exactInfo by remember{mutableStateOf(false)}
    var exactAllowed by remember { mutableStateOf(ReminderPermissionManager.canScheduleExact(context)) }
    val lifecycleOwner=LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer=LifecycleEventObserver { _, event ->
            if(event==Lifecycle.Event.ON_RESUME) {
                val granted=ReminderPermissionManager.canScheduleExact(context)
                if(granted!=exactAllowed)ReminderScheduler.requestRefresh(context)
                exactAllowed=granted
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose{lifecycleOwner.lifecycle.removeObserver(observer)}
    }
    val scope=rememberCoroutineScope()
    val exportFile=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
        if(uri!=null)scope.launch {
            busy=true
            try {
                val json=onExport()
                withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use{it.write(json)}?:error("Cannot open file")}
                message=context.getString(R.string.backup_saved)
            }catch(e:Exception){message=context.getString(R.string.backup_save_error)}finally{busy=false}
        }
    }
    val importFile=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)scope.launch {
            busy=true
            try {
                val json=withContext(Dispatchers.IO){
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use{reader->
                        val text=StringBuilder();val buffer=CharArray(8192)
                        while(true){val length=reader.read(buffer);if(length<0)break;text.append(buffer,0,length);require(text.length<=BackupCodec.MAX_JSON_CHARS){"File too large"}}
                        text.toString()
                    }?:error("Cannot open file")
                }
                pendingImport=json
            }catch(e:Exception){message=context.getString(R.string.backup_read_error)}finally{busy=false}
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(22.dp)) {
        PageHeading(stringResource(R.string.settings_title), stringResource(R.string.settings_subtitle))
        SettingsGroup(stringResource(R.string.settings_appearance)) {
            ChoiceField(stringResource(R.string.settings_language),language,mapOf("vi" to stringResource(R.string.language_vi),"en" to stringResource(R.string.language_en)),onLanguage)
            TextButton(onClick=onReplay){Text(stringResource(R.string.settings_replay_onboarding))}
            ChoiceField(stringResource(R.string.settings_theme),preferences.theme,mapOf(AppTheme.SYSTEM to stringResource(R.string.settings_theme_system),AppTheme.LIGHT to stringResource(R.string.settings_theme_light),AppTheme.DARK to stringResource(R.string.settings_theme_dark)),onTheme)
            SettingsToggle(stringResource(R.string.settings_lunar),stringResource(R.string.settings_lunar_subtitle),preferences.showLunar,onLunar)
            SettingsToggle(stringResource(R.string.settings_holidays),stringResource(R.string.settings_holidays_subtitle),preferences.showHolidays,onHolidays)
            SettingsToggle(stringResource(R.string.settings_monday),stringResource(R.string.settings_monday_subtitle),preferences.mondayFirst,onMonday)
            SettingsToggle(stringResource(R.string.settings_week_numbers),stringResource(R.string.settings_week_numbers_subtitle),preferences.showWeekNumbers,onWeekNumbers)
            SettingsToggle(stringResource(R.string.settings_auspicious_hours),stringResource(R.string.settings_auspicious_hours_subtitle),preferences.showAuspiciousHours,onAuspiciousHours)
            SettingsToggle(stringResource(R.string.settings_animations),stringResource(R.string.settings_animations_subtitle),preferences.animationsEnabled,onAnimations)
        }
        SettingsGroup(stringResource(R.string.settings_reminders_widgets)) {
            OutlinedButton(onClick=onNotifications,modifier=Modifier.fillMaxWidth()) { Icon(Icons.Outlined.NotificationsNone,null,Modifier.size(19.dp));Spacer(Modifier.width(8.dp));Text(stringResource(R.string.notifications_allow)) }
            OutlinedButton(onClick={
                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))
            },modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.notifications_system_settings))}
            OutlinedButton(onClick={
                ReminderScheduler.ensureChannel(context)
                context.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID,ReminderScheduler.CHANNEL_ID))
            },modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.notifications_sound_vibration))}
            if(Build.VERSION.SDK_INT>=31) {
                Text(if(exactAllowed)stringResource(R.string.exact_allowed) else stringResource(R.string.exact_denied),
                    style=MaterialTheme.typography.bodyMedium,color=if(exactAllowed)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error)
                if(!exactAllowed)OutlinedButton(onClick={exactInfo=true},modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.exact_enable))}
            }
            OutlinedButton(onClick={widgets=true},modifier=Modifier.fillMaxWidth()){Icon(Icons.Outlined.Widgets,null,Modifier.size(19.dp));Spacer(Modifier.width(8.dp));Text(stringResource(R.string.widgets_add))}
            Text(stringResource(R.string.reminder_timing_note),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SettingsGroup(stringResource(R.string.settings_privacy)) {
            Text(stringResource(R.string.privacy_local),style=MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.calendar_history_note),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick={exportFile.launch("lich-viet-${LocalDate.now(VietnameseCalendar.zoneId)}.json")},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.backup_export))}
            OutlinedButton(onClick={importFile.launch(arrayOf("application/json","text/plain"))},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.backup_import))}
            TextButton(onClick={deleteAll=true},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.delete_personal_data),color=MaterialTheme.colorScheme.error)}
            if(busy)LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
            message?.let{Text(it,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.primary)}
            TextButton(onClick={info=true}){Text(stringResource(R.string.about_sources))}
            TextButton(onClick={licenses=true}){Text(stringResource(R.string.licenses_open))}
        }
        Text(stringResource(R.string.app_version_format,version),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(24.dp))
    }
    pendingImport?.let{json->AlertDialog(onDismissRequest={if(!busy)pendingImport=null},title={Text(stringResource(R.string.backup_restore_title))},text={Text(stringResource(R.string.backup_restore_body))},
        confirmButton={TextButton(enabled=!busy,onClick={scope.launch{busy=true;try{val count=onImport(json);message=context.resources.getQuantityString(R.plurals.backup_restored_count,count,count);pendingImport=null}catch(e:Exception){message=context.getString(R.string.backup_restore_error);pendingImport=null}finally{busy=false}}}){Text(stringResource(R.string.backup_replace))}},
        dismissButton={TextButton(enabled=!busy,onClick={pendingImport=null}){Text(stringResource(R.string.cancel))}})}
    if(deleteAll)AlertDialog(onDismissRequest={if(!busy)deleteAll=false},title={Text(stringResource(R.string.delete_all_title))},text={Text(stringResource(R.string.delete_all_body))},
        confirmButton={TextButton(enabled=!busy,onClick={scope.launch{busy=true;try{onDeleteAll();message=context.getString(R.string.data_deleted);deleteAll=false}catch(e:Exception){message=context.getString(R.string.data_delete_error);deleteAll=false}finally{busy=false}}}){Text(stringResource(R.string.delete_all),color=MaterialTheme.colorScheme.error)}},
        dismissButton={TextButton(enabled=!busy,onClick={deleteAll=false}){Text(stringResource(R.string.keep))}})
    if(widgets) AlertDialog(onDismissRequest={widgets=false},title={Text(stringResource(R.string.widgets_title))},text={
        Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            val manager=AppWidgetManager.getInstance(context)
            Text(stringResource(R.string.widgets_body))
            listOf(stringResource(R.string.widget_today) to TodayWidgetProvider::class.java,stringResource(R.string.widget_lunar) to LunarWidgetProvider::class.java,stringResource(R.string.widget_month) to MonthWidgetProvider::class.java,stringResource(R.string.widget_upcoming) to UpcomingWidgetProvider::class.java).forEach{(title,provider)->
                if(manager.isRequestPinAppWidgetSupported) OutlinedButton(onClick={manager.requestPinAppWidget(ComponentName(context,provider),null,null);widgets=false},modifier=Modifier.fillMaxWidth()){Text(title)}
            }
            if(!manager.isRequestPinAppWidgetSupported) Text(stringResource(R.string.widgets_manual))
        }
    },confirmButton={TextButton(onClick={widgets=false}){Text(stringResource(R.string.close))}})
    if(exactInfo) AlertDialog(onDismissRequest={exactInfo=false},title={Text(stringResource(R.string.exact_permission_title))},
        text={Text(stringResource(R.string.exact_permission_body))},
        confirmButton={TextButton(onClick={
            exactInfo=false
            ReminderPermissionManager.exactAlarmSettingsIntent(context)?.let { intent ->
                runCatching { context.startActivity(intent) }.onFailure { message=context.getString(R.string.settings_open_error) }
            }
        }){Text(stringResource(R.string.exact_permission_open))}},dismissButton={TextButton(onClick={exactInfo=false}){Text(stringResource(R.string.later))}})
    if(info) AlertDialog(onDismissRequest={info=false},title={Text(stringResource(R.string.app_name))},text={Text(stringResource(R.string.about_body,version))},confirmButton={TextButton(onClick={info=false}){Text(stringResource(R.string.understood))}})
    if(licenses) {
        val text=remember(context){listOf("LICENSE.ical4j","LICENSE.calendar-core").joinToString("\n\n") { name ->
            context.assets.open("licenses/$name").bufferedReader().use{it.readText()}
        }}
        AlertDialog(onDismissRequest={licenses=false},title={Text(stringResource(R.string.licenses_title))},
            text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())){Text(text,style=MaterialTheme.typography.bodySmall)}},
            confirmButton={TextButton(onClick={licenses=false}){Text(stringResource(R.string.close))}})
    }
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
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants=true){}.toggleable(checked,role=Role.Switch,onValueChange=onChange),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.titleSmall);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        Switch(checked,null)
    }
}
