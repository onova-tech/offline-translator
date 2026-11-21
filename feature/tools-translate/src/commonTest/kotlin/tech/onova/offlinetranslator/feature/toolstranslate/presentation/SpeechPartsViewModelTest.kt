package tech.onova.offlinetranslator.feature.toolstranslate.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import tech.onova.offlinetranslator.core.domain.ports.AppVisibility
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport
import tech.onova.offlinetranslator.core.domain.speech.SpeechCatalog
import tech.onova.offlinetranslator.core.domain.speech.SpeechEngine
import tech.onova.offlinetranslator.feature.toolstranslate.domain.AppSpeechParts
import tech.onova.offlinetranslator.feature.toolstranslate.domain.EngineUse
import tech.onova.offlinetranslator.feature.toolstranslate.domain.RoutedSpeechInput
import tech.onova.offlinetranslator.feature.toolstranslate.domain.RoutedSpeechOutput
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SetupNeeds
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SpeechChoices
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SpeechDownloads
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SpeechKind
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeContentPacks
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeSpeechInput
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeSpeechOutput
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.InMemoryTranslatorSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Spec 080 story 4 (T044): each language's speech and voice on the Manage languages page, and what gets them. */
@OptIn(ExperimentalCoroutinesApi::class)
class SpeechPartsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val speechIn = FakeSpeechInput()
    private val speechOut = FakeSpeechOutput()
    private val visible = MutableStateFlow(true)
    private val ja = LanguageCode("ja")
    private val ar = LanguageCode("ar")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val settings = InMemoryTranslatorSettings()
    private var packs = FakeContentPacks()

    private fun TestScope.model(): SpeechPartsViewModel {
        val visibility =
            object : AppVisibility {
                override val isVisible = visible
            }
        val downloads = SpeechDownloads { testScheduler.currentTime }
        val parts = AppSpeechParts(packs)
        val choices =
            SpeechChoices(
                RoutedSpeechInput(speechIn, FakeSpeechInput(), parts, settings, downloads),
                RoutedSpeechOutput(speechOut, FakeSpeechOutput(), parts, settings),
                settings,
            )
        val model = SpeechPartsViewModel(speechIn, speechOut, downloads, visibility, packs, choices)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        model.show(listOf(ja, ar), phoneLocales = listOf("pt-BR"))
        runCurrent()
        return model
    }

    private fun TestScope.waitPast(millis: Long) {
        repeat((millis / SetupNeeds.RECHECK_MILLIS + 2).toInt()) {
            advanceTimeBy(SetupNeeds.RECHECK_MILLIS + 1)
            runCurrent()
        }
    }

    @Test
    fun eachLanguageShowsWhatThePhoneHasForIt() =
        runTest(dispatcher) {
            speechIn.support["ja-JP"] = RecognitionSupport.CAN_DOWNLOAD
            speechIn.support["ar-SA"] = RecognitionSupport.UNSUPPORTED
            speechOut.voices["ar-SA"] = VoiceSupport.NO_OFFLINE_VOICE
            val parts = model().uiState.value.parts
            assertEquals(SpeechParts(RecognitionSupport.CAN_DOWNLOAD, VoiceSupport.READY), parts[ja])
            assertEquals(SpeechParts(RecognitionSupport.UNSUPPORTED, VoiceSupport.NO_OFFLINE_VOICE), parts[ar])
        }

    @Test
    fun aLanguageAskedForIsWatchedUntilItIsReady() =
        runTest(dispatcher) {
            speechIn.support["ja-JP"] = RecognitionSupport.CAN_DOWNLOAD
            val model = model()
            model.get(ja)
            runCurrent()
            assertEquals(listOf("ja-JP"), speechIn.requested)
            assertEquals(
                RecognitionSupport.DOWNLOADING,
                model.uiState.value.parts[ja]
                    ?.recognition,
            )
            speechIn.support["ja-JP"] = RecognitionSupport.READY
            advanceTimeBy(SetupNeeds.RECHECK_MILLIS + 1)
            runCurrent()
            assertEquals(
                RecognitionSupport.READY,
                model.uiState.value.parts[ja]
                    ?.recognition,
            )
        }

    /** The owner's phone, 2026-11-20: Japanese asked for and never installed; Update opens the service's page. */
    @Test
    fun aLanguageNeverInstalledOffersTheServiceUpdateAndCanBeAskedAgainAfter() =
        runTest(dispatcher) {
            speechIn.support["ja-JP"] = RecognitionSupport.CAN_DOWNLOAD
            speechIn.updatable = true
            val model = model()
            model.get(ja)
            waitPast(SpeechDownloads.ASKED_MILLIS)
            assertEquals(
                RecognitionSupport.NOT_INSTALLED,
                model.uiState.value.parts[ja]
                    ?.recognition,
            )
            assertTrue(model.uiState.value.serviceUpdatable)
            model.update()
            assertEquals(1, speechIn.serviceUpdatesOpened)
            visible.value = false
            runCurrent()
            visible.value = true
            runCurrent()
            assertEquals(
                RecognitionSupport.CAN_DOWNLOAD,
                model.uiState.value.parts[ja]
                    ?.recognition,
            )
        }

    @Test
    fun aVoiceComesFromTheVoiceAppAndCountsWhenTheScreenComesBack() =
        runTest(dispatcher) {
            speechOut.voices["ja-JP"] = VoiceSupport.NO_OFFLINE_VOICE
            val model = model()
            model.voice()
            assertEquals(1, speechOut.voiceSettingsOpened)
            visible.value = false
            runCurrent()
            speechOut.voices["ja-JP"] = VoiceSupport.READY
            visible.value = true
            runCurrent()
            assertEquals(
                VoiceSupport.READY,
                model.uiState.value.parts[ja]
                    ?.voice,
            )
        }

    /** Spec 082 US3: where both can, "Phone" at first; "App" is kept and shown while its model downloads. */
    @Test
    fun aLanguageBothCanDoOffersTheChoiceAndKeepsIt() =
        runTest(dispatcher) {
            packs =
                FakeContentPacks(
                    FakeContentPacks.notInstalled(SpeechCatalog.recognitionPack),
                    FakeContentPacks.installed(SpeechCatalog.voiceDataPack),
                )
            speechIn.support["ja-JP"] = RecognitionSupport.READY
            speechIn.support["ar-SA"] = RecognitionSupport.UNSUPPORTED
            val model = model()
            val japanese =
                model.uiState.value.uses
                    .getValue(ja)
            assertEquals(EngineUse(SpeechEngine.PHONE, null, choosable = true), japanese.recognition)
            assertEquals(SpeechEngine.PHONE, japanese.recognition.shown)
            // Arabic: only the app can, so it's used without asking (US3 scenario 3).
            val arabic =
                model.uiState.value.uses
                    .getValue(ar)
            assertEquals(EngineUse(SpeechEngine.APP, null, choosable = false), arabic.recognition)
            model.choose(ja, SpeechKind.RECOGNITION, SpeechEngine.APP)
            runCurrent()
            assertEquals(
                SpeechEngine.APP,
                settings.settings.value.speech[ja]
                    ?.recognition,
            )
            val chosen =
                model.uiState.value.uses
                    .getValue(ja)
                    .recognition
            assertEquals(SpeechEngine.PHONE, chosen.used, "the phone's until the app's is in (US3 scenario 2)")
            assertEquals(SpeechEngine.APP, chosen.shown)
        }
}
