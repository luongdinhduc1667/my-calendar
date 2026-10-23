package vn.lichviet.app

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import vn.lichviet.app.data.PreferencesRepository
import vn.lichviet.app.data.UserPreferences

/** Establish launch preferences before ActivityScenario creates the Activity. */
class LaunchPreferencesRule(private val completed:Boolean=true,private val language:String="vi") : TestRule {
    override fun apply(base:Statement,description:Description)=object:Statement(){
        override fun evaluate() {
            val instrumentation=InstrumentationRegistry.getInstrumentation()
            val context=instrumentation.targetContext
            fun applyLanguage(tag:String) {
                context.getSharedPreferences("locale_bootstrap",0).edit().putString("language",tag).commit()
                if(Build.VERSION.SDK_INT>=33)context.getSystemService(LocaleManager::class.java).applicationLocales=LocaleList.forLanguageTags(tag)
                instrumentation.waitForIdleSync()
            }
            runBlocking {
                PreferencesRepository(context).update(UserPreferences())
                PreferencesRepository(context).setOnboardingCompleted(completed)
            }
            applyLanguage(language)
            try { base.evaluate() }
            finally {
                runBlocking { PreferencesRepository(context).completeOnboarding() }
                applyLanguage("vi")
            }
        }
    }
}
