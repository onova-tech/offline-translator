package tech.onova.offlinetranslator.di

import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import tech.onova.offlinetranslator.BuildConfig
import tech.onova.offlinetranslator.core.content.PlayContentPacks
import tech.onova.offlinetranslator.core.data.datastore.JsonDataStoreFactory
import tech.onova.offlinetranslator.core.data.datastore.androidDataStoreDirectory
import tech.onova.offlinetranslator.core.data.settings.DataStoreAppSettingsRepository
import tech.onova.offlinetranslator.core.domain.permissions.PermissionController
import tech.onova.offlinetranslator.core.domain.ports.AppInfo
import tech.onova.offlinetranslator.core.domain.ports.AppLanguageController
import tech.onova.offlinetranslator.core.domain.ports.AppSettingsRepository
import tech.onova.offlinetranslator.core.domain.ports.AppVisibility
import tech.onova.offlinetranslator.core.domain.ports.ClipboardWriter
import tech.onova.offlinetranslator.core.domain.ports.ContentPacks
import tech.onova.offlinetranslator.core.domain.ports.DeviceCapabilities
import tech.onova.offlinetranslator.core.domain.ports.LanguageNames
import tech.onova.offlinetranslator.core.domain.ports.MonotonicClock
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.ProcessVisibility
import tech.onova.offlinetranslator.core.domain.ports.ScreenAwake
import tech.onova.offlinetranslator.core.domain.ports.ScreenReader
import tech.onova.offlinetranslator.core.domain.ports.SettingsPages
import tech.onova.offlinetranslator.core.domain.ports.SpeechInput
import tech.onova.offlinetranslator.core.domain.ports.SpeechOutput
import tech.onova.offlinetranslator.core.domain.ports.TextSharer
import tech.onova.offlinetranslator.core.domain.ports.TranslationEngine
import tech.onova.offlinetranslator.core.domain.speech.SpeechCatalog
import tech.onova.offlinetranslator.core.domain.speech.packs
import tech.onova.offlinetranslator.core.domain.translation.LanguageCatalog
import tech.onova.offlinetranslator.core.platform.AndroidClipboardWriter
import tech.onova.offlinetranslator.core.platform.AndroidDeviceCapabilities
import tech.onova.offlinetranslator.core.platform.AndroidLanguageNames
import tech.onova.offlinetranslator.core.platform.AndroidMonotonicClock
import tech.onova.offlinetranslator.core.platform.AndroidPermissionController
import tech.onova.offlinetranslator.core.platform.AndroidSettingsPages
import tech.onova.offlinetranslator.core.platform.AppCompatLanguageController
import tech.onova.offlinetranslator.core.platform.CurrentActivityTracker
import tech.onova.offlinetranslator.core.platform.ProcessAppVisibility
import tech.onova.offlinetranslator.core.platform.TaskVisibility
import tech.onova.offlinetranslator.core.platform.display.AndroidTextSharer
import tech.onova.offlinetranslator.core.platform.sensors.AndroidScreenAwake
import tech.onova.offlinetranslator.core.platform.speech.AndroidScreenReader
import tech.onova.offlinetranslator.core.platform.speech.AndroidSpeechInput
import tech.onova.offlinetranslator.core.platform.speech.AndroidSpeechOutput
import tech.onova.offlinetranslator.core.speech.AppSpeechInput
import tech.onova.offlinetranslator.core.speech.AppSpeechOutput
import tech.onova.offlinetranslator.core.toolapi.DefaultToolRegistry
import tech.onova.offlinetranslator.core.toolapi.ToolRegistry
import tech.onova.offlinetranslator.core.translation.BergamotEngine
import kotlin.time.Clock

/** The app's bindings (spec 081): the settings, and the phone's pieces the Translator reaches through ports. */
val appModule =
    module {
        single<Clock> { Clock.System }
        single {
            JsonDataStoreFactory(
                fileSystem = FileSystem.SYSTEM,
                directory = androidDataStoreDirectory(androidContext()),
                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            )
        }
        single { DataStoreAppSettingsRepository(get()) } bind AppSettingsRepository::class
        single<DeviceCapabilities> { AndroidDeviceCapabilities(androidContext()) }
        single<AppLanguageController> { AppCompatLanguageController() }
        single<ClipboardWriter> { AndroidClipboardWriter(androidContext()) }
        single<TextSharer> { AndroidTextSharer(androidContext()) }
        single { CurrentActivityTracker() }
        single<MonotonicClock> { AndroidMonotonicClock(androidContext()) }
        single<ProcessVisibility> { ProcessAppVisibility() }
        single { TaskVisibility(get<ProcessVisibility>()) }
        // Asked once, when a ViewModel is created: it follows that screen's task (spec 016 research R2).
        factory<AppVisibility> { get<TaskVisibility>().forCurrentTask() }
        single<PermissionController> { AndroidPermissionController(androidContext(), get()) }
        // A conversation keeps the screen on while its page is open (spec 012).
        single<ScreenAwake> { AndroidScreenAwake(get()) }
        // The phone's own recognizer and voices, on the device only (spec 012), and the app's own (spec 082); the
        // Translator routes between them per language (its module binds the plain SpeechInput and SpeechOutput).
        single<SpeechInput>(named("phone")) { AndroidSpeechInput(androidContext()) }
        single<SpeechOutput>(named("phone")) { AndroidSpeechOutput(androidContext()) }
        single<SpeechInput>(named("app")) {
            val packs = get<ContentPacks>()
            AppSpeechInput(androidContext()) { packs.location(SpeechCatalog.recognitionPack) }
        }
        single<SpeechOutput>(named("app")) {
            val packs = get<ContentPacks>()
            AppSpeechOutput(
                voiceFolder = { language -> SpeechCatalog.voices[language]?.let { packs.location(it.pack) } },
                espeakFolder = { packs.location(SpeechCatalog.voiceDataPack) },
            )
        }
        single<ScreenReader> { AndroidScreenReader(androidContext()) }
        // Bergamot, on its own thread; it reads the packs ContentPacks installs (spec 012 research R3).
        single<TranslationEngine> { BergamotEngine() }
        // The packs through Google Play, the app's only network (spec 012 research R5): one per language of the catalog
        // (spec 080) and the speech packs (spec 082), the same packs as build.gradle.kts's assetPacks.
        single<ContentPacks> {
            val screen = get<CurrentActivityTracker>()
            PlayContentPacks(
                androidContext(),
                LanguageCatalog.all.mapNotNull { it.pack } + SpeechCatalog.packs,
                screen = { screen.current() },
                bundled =
                    BuildConfig.BUNDLED_PACKS
                        .split(',')
                        .filter { it.isNotBlank() }
                        .map(::PackId)
                        .toSet(),
            )
        }
        single<SettingsPages> { AndroidSettingsPages(androidContext()) }
        // The phone's names for the languages (spec 080 research R4).
        single<LanguageNames> { AndroidLanguageNames() }
        single { AppInfo(versionName = BuildConfig.VERSION_NAME, deviceModel = Build.MANUFACTURER + " " + Build.MODEL) }
        single<ToolRegistry> { DefaultToolRegistry(getAll()) }
    }
