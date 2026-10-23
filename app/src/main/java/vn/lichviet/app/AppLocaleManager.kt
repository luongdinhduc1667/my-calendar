package vn.lichviet.app

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/** Framework per-app locales on Android 13+, a scoped context on older supported Android. */
object AppLocaleManager {
    private const val FILE = "locale_bootstrap"
    private const val KEY = "language"
    private val supported = setOf("vi", "en")

    fun language(context: Context): String {
        if (Build.VERSION.SDK_INT >= 33) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (!locales.isEmpty) return locales[0].language.takeIf { it in supported } ?: "vi"
        }
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, "vi")
            ?.takeIf { it in supported } ?: "vi"
    }

    fun localizedContext(context: Context): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language(context)))
        return context.createConfigurationContext(configuration)
    }

    fun attachContext(context: Context): Context = localizedContext(context)

    fun resetLanguage(context:Context) {
        check(context.getSharedPreferences(FILE,Context.MODE_PRIVATE).edit().clear().commit())
        if(Build.VERSION.SDK_INT>=33) {
            context.getSystemService(LocaleManager::class.java).applicationLocales=LocaleList.forLanguageTags("vi")
        }
    }

    /** A synchronous tiny bootstrap write makes the pre-33 recreation read the new choice. */
    fun setLanguage(activity: Activity, tag: String) {
        require(tag in supported)
        val current = language(activity)
        check(activity.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, tag).commit())
        if (Build.VERSION.SDK_INT >= 33) {
            val manager = activity.getSystemService(LocaleManager::class.java)
            if (manager.applicationLocales.toLanguageTags() != tag) {
                manager.applicationLocales = LocaleList.forLanguageTags(tag)
            }
        } else if (current != tag) {
            activity.recreate()
        }
        vn.lichviet.app.widget.CalendarWidgets.requestUpdate(activity.applicationContext)
        vn.lichviet.app.reminder.ReminderScheduler.ensureChannel(activity.applicationContext)
    }
}
