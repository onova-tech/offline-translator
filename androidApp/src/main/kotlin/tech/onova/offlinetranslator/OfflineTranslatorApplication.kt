package tech.onova.offlinetranslator

import android.app.Application
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import tech.onova.offlinetranslator.core.platform.CurrentActivityTracker
import tech.onova.offlinetranslator.core.platform.TaskVisibility
import tech.onova.offlinetranslator.di.appModule
import tech.onova.offlinetranslator.feature.settings.settingsModule
import tech.onova.offlinetranslator.feature.toolstranslate.di.toolsTranslateModule

/** Offline Translator (spec 081): the Translator and its Settings, nothing started in the background. */
class OfflineTranslatorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@OfflineTranslatorApplication)
            modules(appModule, settingsModule, toolsTranslateModule)
        }
        // Permission requests and the packs' confirmation dialog need the resumed activity (spec 004 research R7).
        registerActivityLifecycleCallbacks(get<CurrentActivityTracker>())
        // Before any screen: a ViewModel follows the task it was created in (spec 016 research R2).
        registerActivityLifecycleCallbacks(get<TaskVisibility>().activityCallbacks)
    }
}
