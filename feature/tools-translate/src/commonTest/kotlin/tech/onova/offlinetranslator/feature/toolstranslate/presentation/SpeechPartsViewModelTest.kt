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
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SetupNeeds
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SpeechDownloads
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeSpeechInput
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeSpeechOutput
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

    private fun TestScope.model(): SpeechPartsViewModel {
        val visibility =
            object : AppVisibility {
                override val isVisible = visible
            }
        val model = SpeechPartsViewModel(speechIn, speechOut, SpeechDownloads { testScheduler.currentTime }, visibility)
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
}
