package vn.lichviet.app

import android.os.Bundle
import android.content.Intent
import android.content.Context
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import vn.lichviet.app.ui.LichVietApp
import vn.lichviet.app.ui.LichVietTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val model: CalendarViewModel by viewModels()
    override fun attachBaseContext(newBase: Context) {
        val configuration = Configuration(newBase.resources.configuration)
        configuration.setLocale(java.util.Locale.forLanguageTag("vi"))
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if(savedInstanceState==null)selectIntentDate(intent)
        setContent {
            val preferences by model.preferences.collectAsStateWithLifecycle()
            LichVietTheme(preferences.theme.name) {
                val lightBars=MaterialTheme.colorScheme.background.luminance()>.5f
                SideEffect {
                    WindowCompat.getInsetsController(window,window.decorView).apply {
                        isAppearanceLightStatusBars=lightBars
                        isAppearanceLightNavigationBars=lightBars
                    }
                }
                LichVietApp(model)
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        selectIntentDate(intent)
    }
    private fun selectIntentDate(intent: Intent?) {
        intent?.getStringExtra("selected_date")?.let { value ->
            runCatching { java.time.LocalDate.parse(value) }.getOrNull()?.let(model::select)
        }
    }
}
