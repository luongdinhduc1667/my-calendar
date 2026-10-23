package vn.lichviet.app

import android.os.Bundle
import android.content.Intent
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import vn.lichviet.app.ui.LichVietApp
import vn.lichviet.app.ui.LichVietTheme
import vn.lichviet.app.ui.OnboardingScreen
import kotlinx.coroutines.launch
import vn.lichviet.app.reminder.ReminderNotificationManager
import vn.lichviet.app.reminder.ReminderPermissionManager
import vn.lichviet.app.reminder.ReminderScheduler

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val model: CalendarViewModel by viewModels()
    private var reminderAccess: Pair<Boolean, Boolean>? = null
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocaleManager.attachContext(newBase))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if(savedInstanceState==null)handleLaunchIntent(intent)
        setContent {
            val preferences by model.preferences.collectAsStateWithLifecycle()
            val onboarding by model.onboardingCompleted.collectAsStateWithLifecycle()
            val calendarReady by model.calendarReady.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()
            LaunchedEffect(onboarding) {
                // On Android 8–12 a full settings reset also needs a fresh scoped locale context.
                if(resources.configuration.locales[0].language!=AppLocaleManager.language(this@MainActivity))recreate()
            }
            LichVietTheme(preferences.theme.name, preferences.animationsEnabled) {
                val lightBars=MaterialTheme.colorScheme.background.luminance()>.5f
                SideEffect {
                    WindowCompat.getInsetsController(window,window.decorView).apply {
                        isAppearanceLightStatusBars=lightBars
                        isAppearanceLightNavigationBars=lightBars
                    }
                }
                when(if (calendarReady) onboarding else null) {
                    null -> Surface {
                        Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()}
                    }
                    false -> OnboardingScreen(preferences,AppLocaleManager.language(this),
                        {AppLocaleManager.setLanguage(this,it)},
                        {scope.launch{model.setTheme(it)}},{scope.launch{model.setMonday(it)}},{scope.launch{model.setLunar(it)}}) { setup ->
                        scope.launch { model.completeOnboarding(setup) }
                    }
                    true -> LichVietApp(model)
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }
    override fun onResume() {
        super.onResume()
        val access = ReminderPermissionManager.canScheduleExact(this) to ReminderNotificationManager.canNotify(this)
        if (reminderAccess != access) ReminderScheduler.requestRefresh(this)
        reminderAccess = access
    }
    private fun handleLaunchIntent(intent:Intent?) {
        selectIntentDate(intent)
        if(intent?.action==Intent.ACTION_VIEW && intent.data?.scheme in setOf("content","file")) {
            intent.data?.toString()?.let(model::openIcsUri)
        }
    }
    private fun selectIntentDate(intent: Intent?) {
        intent?.getStringExtra("selected_date")?.let { value ->
            runCatching { java.time.LocalDate.parse(value) }.getOrNull()?.let(model::select)
        }
    }
}
