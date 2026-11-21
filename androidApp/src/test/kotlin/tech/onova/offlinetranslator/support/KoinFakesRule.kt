package tech.onova.offlinetranslator.support

import org.junit.rules.ExternalResource
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.robolectric.RuntimeEnvironment
import tech.onova.offlinetranslator.core.domain.permissions.PermissionController
import tech.onova.offlinetranslator.core.domain.ports.AppInfo
import tech.onova.offlinetranslator.core.domain.ports.AppLanguageController
import tech.onova.offlinetranslator.core.domain.ports.AppSettingsRepository
import tech.onova.offlinetranslator.core.domain.ports.AppVisibility
import tech.onova.offlinetranslator.core.domain.ports.ClipboardWriter
import tech.onova.offlinetranslator.core.domain.ports.ContentPacks
import tech.onova.offlinetranslator.core.domain.ports.DeviceCapabilities
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
import tech.onova.offlinetranslator.di.appModule
import tech.onova.offlinetranslator.feature.settings.settingsModule
import tech.onova.offlinetranslator.feature.toolstranslate.di.toolsTranslateModule
import java.util.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Starts the real Koin graph with storage and device services replaced by deterministic fakes (Teal palette, a fixed
 * time zone and clock). Order it before any rule that launches UI.
 */
class KoinFakesRule(
    val capabilities: DeviceCapabilities = FakeCapabilities(),
) : ExternalResource() {
    val appSettings = InMemoryAppSettings()
    val language = FakeLanguage()
    val clipboard = FakeClipboard()
    val clock = FakeMonotonicClock()
    val visibility = FakeAppVisibility()
    val textSharer = FakeTextSharer()
    val settingsPages = FakeSettingsPages()
    val permissions = FakePermissionController()
    val screenAwake = FakeScreenAwake()
    val wallClock = FakeWallClock(TEST_NOW)

    // The Translator's ports (spec 012): packs, engine, speech in and out, and the screen reader.
    // Both language packs are on the phone unless a test says otherwise, so the Translator shows itself (spec 012).
    val contentPacks =
        FakeContentPacks(
            FakeContentPacks.installed(PackId("translate_pt")),
            FakeContentPacks.installed(PackId("translate_es")),
        )
    val translationEngine = FakeTranslationEngine()
    val speechInput = FakeSpeechInput()
    val speechOutput = FakeSpeechOutput()

    /** The app's own recognition and voices (spec 082), used where the routing sends a language to them. */
    val appSpeechInput = FakeSpeechInput()
    val appSpeechOutput = FakeSpeechOutput()
    val screenReader = FakeScreenReader()

    private var systemZone: TimeZone? = null

    override fun before() {
        pinTimeZone()
        startKoin {
            // The real stores live in the test's own files folder.
            androidContext(RuntimeEnvironment.getApplication())
            modules(appModule, settingsModule, toolsTranslateModule, fakesModule())
        }
    }

    /** Every port and repository the app reads, replaced by the fakes above. */
    private fun fakesModule() =
        module {
            single<AppSettingsRepository> { appSettings }
            single<AppLanguageController> { language }
            single<DeviceCapabilities> { capabilities }
            single { AppInfo(versionName = "1.0.0-test", deviceModel = "Test Phone") }
            single<ClipboardWriter> { clipboard }
            single<TextSharer> { textSharer }
            single<SettingsPages> { settingsPages }
            single<MonotonicClock> { clock }
            single<Clock> { wallClock }
            single<AppVisibility> { visibility }
            single<ProcessVisibility> { visibility }
            single<PermissionController> { permissions }
            single<ScreenAwake> { screenAwake }
            single<ContentPacks> { contentPacks }
            single<TranslationEngine> { translationEngine }
            // The phone's and the app's speech (spec 082): the Translator's routing between them runs as in the app.
            single<SpeechInput>(named("phone")) { speechInput }
            single<SpeechOutput>(named("phone")) { speechOutput }
            single<SpeechInput>(named("app")) { appSpeechInput }
            single<SpeechOutput>(named("app")) { appSpeechOutput }
            single<ScreenReader> { screenReader }
        }

    /** Screens that show local times and dates look the same on every machine. */
    private fun pinTimeZone() {
        systemZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone(TEST_ZONE))
    }

    override fun after() {
        stopKoin()
        systemZone?.let(TimeZone::setDefault)
    }

    private companion object {
        val TEST_NOW = Instant.parse("2026-10-21T15:30:00Z")
        const val TEST_ZONE = "America/Sao_Paulo"
    }
}
