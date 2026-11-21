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
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.SpeechEvent
import tech.onova.offlinetranslator.core.domain.ports.SpeechFailure
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationMachine
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SetupNeeds
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SpeechDownloads
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeSpeechInput
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeSpeechOutput
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Spec 012 story 4 (T046): speak instead of typing, and hear the translation, with the phone's own speech. */
@OptIn(ExperimentalCoroutinesApi::class)
class SpeakAndHearTest {
    private val dispatcher = StandardTestDispatcher()
    private val speechIn = FakeSpeechInput()
    private val speechOut = FakeSpeechOutput()
    private val visible = MutableStateFlow(true)
    private val visibility =
        object : AppVisibility {
            override val isVisible = visible
        }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Shared with the Manage languages page, which asks for a language: the tests ask through it as the page does. */
    private lateinit var downloads: SpeechDownloads

    /** The model with Portuguese (Brazil) to English (US), and everything it has put into the text field. */
    private fun TestScope.model(): Pair<SpeakAndHearViewModel, MutableList<String>> {
        downloads = SpeechDownloads { testScheduler.currentTime }
        val model = SpeakAndHearViewModel(speechIn, speechOut, visibility, downloads)
        val heard = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.heard.collect { heard += it } }
        model.setLanguages(source = "pt-BR", target = "en-US")
        runCurrent()
        return model to heard
    }

    @Test
    fun theMicrophoneNeverOpensByItself() =
        runTest(dispatcher) {
            val (model, _) = model()
            assertTrue(speechIn.listenedTo.isEmpty())
            assertFalse(model.uiState.value.listening)
            assertEquals(RecognitionSupport.READY, model.uiState.value.recognition)
        }

    @Test
    fun partialWordsGoIntoTheFieldAndTheSentenceEndsListening() =
        runTest(dispatcher) {
            val (model, heard) = model()
            model.tapMicrophone()
            runCurrent()
            assertTrue(model.uiState.value.listening)
            assertEquals(listOf("pt-BR"), speechIn.listenedTo)
            speechIn.hear(SpeechEvent.Partial("Bom"))
            runCurrent()
            assertEquals(listOf("Bom"), heard)
            speechIn.hear(SpeechEvent.Final("Bom dia"))
            runCurrent()
            assertEquals(listOf("Bom", "Bom dia"), heard)
            assertFalse(model.uiState.value.listening)
            assertEquals(0, speechIn.listening, "the microphone is closed")
        }

    @Test
    fun aSecondTapEndsListeningAndKeepsWhatWasHeard() =
        runTest(dispatcher) {
            val (model, heard) = model()
            model.tapMicrophone()
            runCurrent()
            speechIn.hear(SpeechEvent.Partial("Bom dia"))
            runCurrent()
            model.tapMicrophone()
            runCurrent()
            assertFalse(model.uiState.value.listening)
            assertEquals(0, speechIn.listening)
            assertEquals(listOf("Bom dia"), heard)
        }

    @Test
    fun twoSecondsWithoutNewWordsEndListening() =
        runTest(dispatcher) {
            val (model, _) = model()
            model.tapMicrophone()
            runCurrent()
            speechIn.hear(SpeechEvent.Partial("Bom dia"))
            runCurrent()
            advanceTimeBy(ConversationMachine.SILENCE_MILLIS - 1)
            runCurrent()
            assertTrue(model.uiState.value.listening)
            advanceTimeBy(2)
            runCurrent()
            assertFalse(model.uiState.value.listening)
            assertEquals(0, speechIn.listening)
        }

    @Test
    fun nothingHeardSaysSo() =
        runTest(dispatcher) {
            val (model, heard) = model()
            model.tapMicrophone()
            runCurrent()
            speechIn.hear(SpeechEvent.Failed(SpeechFailure.NOTHING_HEARD))
            runCurrent()
            assertFalse(model.uiState.value.listening)
            assertEquals(SpeechProblem.NOTHING_HEARD, model.uiState.value.problem)
            assertTrue(heard.isEmpty())
            model.dismiss()
            runCurrent()
            assertNull(model.uiState.value.problem)
        }

    /** Its message leads to the Manage languages page (owner, 2026-11-20), where the language is asked for. */
    @Test
    fun aLanguageThePhoneCanGetSaysSoInsteadOfListening() =
        runTest(dispatcher) {
            speechIn.support["pt-BR"] = RecognitionSupport.CAN_DOWNLOAD
            val (model, _) = model()
            model.tapMicrophone()
            runCurrent()
            assertTrue(speechIn.listenedTo.isEmpty())
            assertEquals(SpeechProblem.CAN_DOWNLOAD, model.uiState.value.problem)
        }

    @Test
    fun aLanguageThePhoneGotWorksWithoutLeavingTheTool() =
        runTest(dispatcher) {
            speechIn.support["pt-BR"] = RecognitionSupport.CAN_DOWNLOAD
            val (model, _) = model()
            model.tapMicrophone()
            // Asked for on the Manage languages page: the phone's own dialog, then its download in the background.
            downloads.asked("pt-BR")
            runCurrent()
            speechIn.support["pt-BR"] = RecognitionSupport.DOWNLOADING
            advanceTimeBy(SetupNeeds.RECHECK_MILLIS + 1)
            runCurrent()
            assertEquals(RecognitionSupport.DOWNLOADING, model.uiState.value.recognition)
            model.tapMicrophone()
            assertEquals(SpeechProblem.DOWNLOADING, model.uiState.value.problem)
            speechIn.support["pt-BR"] = RecognitionSupport.READY
            advanceTimeBy(SetupNeeds.RECHECK_MILLIS + 1)
            runCurrent()
            assertEquals(RecognitionSupport.READY, model.uiState.value.recognition)
            model.tapMicrophone()
            runCurrent()
            assertEquals(listOf("pt-BR"), speechIn.listenedTo)
        }

    /** The owner's phone, 2026-11-20: a language asked for and never installed says so by itself. */
    @Test
    fun aLanguageThePhoneNeverInstallsSaysSo() =
        runTest(dispatcher) {
            speechIn.support["pt-BR"] = RecognitionSupport.CAN_DOWNLOAD
            val (model, _) = model()
            model.tapMicrophone()
            downloads.asked("pt-BR")
            repeat((SpeechDownloads.ASKED_MILLIS / SetupNeeds.RECHECK_MILLIS + 2).toInt()) {
                advanceTimeBy(SetupNeeds.RECHECK_MILLIS + 1)
                runCurrent()
            }
            assertEquals(RecognitionSupport.NOT_INSTALLED, model.uiState.value.recognition)
            assertEquals(SpeechProblem.NOT_INSTALLED, model.uiState.value.problem)
            visible.value = false
            runCurrent()
            visible.value = true
            runCurrent()
            model.tapMicrophone()
            assertEquals(SpeechProblem.CAN_DOWNLOAD, model.uiState.value.problem, "it can be asked for again")
        }

    @Test
    fun aVoiceAddedInTheSettingsCountsWhenTheScreenComesBack() =
        runTest(dispatcher) {
            speechOut.voices["en-US"] = VoiceSupport.NO_OFFLINE_VOICE
            val (model, _) = model()
            assertEquals(VoiceSupport.NO_OFFLINE_VOICE, model.uiState.value.voice)
            visible.value = false
            runCurrent()
            speechOut.voices["en-US"] = VoiceSupport.READY
            visible.value = true
            runCurrent()
            assertEquals(VoiceSupport.READY, model.uiState.value.voice)
        }

    @Test
    fun aLanguageThePhoneCantRecognizeExplainsAndTypingStays() =
        runTest(dispatcher) {
            speechIn.support["pt-BR"] = RecognitionSupport.UNSUPPORTED
            val (model, _) = model()
            model.tapMicrophone()
            runCurrent()
            assertTrue(speechIn.listenedTo.isEmpty())
            assertEquals(SpeechProblem.UNSUPPORTED, model.uiState.value.problem)
        }

    @Test
    fun withoutAnOnDeviceRecognizerTheMicrophoneExplainsAndTypingStays() =
        runTest(dispatcher) {
            speechIn.defaultSupport = RecognitionSupport.NO_RECOGNIZER
            val (model, _) = model()
            model.tapMicrophone()
            runCurrent()
            assertTrue(speechIn.listenedTo.isEmpty())
            assertEquals(SpeechProblem.NO_RECOGNIZER, model.uiState.value.problem)
        }

    @Test
    fun beforeAndroid12TheScreenKnowsToHideTheMicrophone() =
        runTest(dispatcher) {
            speechIn.defaultSupport = RecognitionSupport.SYSTEM_TOO_OLD
            val (model, _) = model()
            assertEquals(RecognitionSupport.SYSTEM_TOO_OLD, model.uiState.value.recognition)
        }

    @Test
    fun readAloudSpeaksTheTranslationInTheTargetsVoiceAndBecomesStop() =
        runTest(dispatcher) {
            val (model, _) = model()
            model.readAloud("Good morning")
            runCurrent()
            assertEquals(listOf("Good morning" to "en-US"), speechOut.said)
            assertTrue(model.uiState.value.speaking)
            model.readAloud("Good morning")
            runCurrent()
            assertEquals(1, speechOut.stops)
            assertFalse(model.uiState.value.speaking)
        }

    @Test
    fun withoutAnOfflineVoiceReadAloudSaysSo() =
        runTest(dispatcher) {
            speechOut.voices["en-US"] = VoiceSupport.NO_OFFLINE_VOICE
            val (model, _) = model()
            model.readAloud("Good morning")
            runCurrent()
            assertTrue(speechOut.said.isEmpty())
            assertEquals(SpeechProblem.NO_VOICE, model.uiState.value.problem)
        }

    @Test
    fun listeningAndSpeakingStopWhenTheScreenIsHidden() =
        runTest(dispatcher) {
            val (model, _) = model()
            model.tapMicrophone()
            runCurrent()
            visible.value = false
            runCurrent()
            assertFalse(model.uiState.value.listening)
            assertEquals(0, speechIn.listening)

            visible.value = true
            model.readAloud("Good morning")
            runCurrent()
            visible.value = false
            runCurrent()
            assertFalse(model.uiState.value.speaking)
            assertEquals(1, speechOut.stops)
        }

    @Test
    fun anotherLanguageClearsTheMessage() =
        runTest(dispatcher) {
            speechIn.support["pt-BR"] = RecognitionSupport.UNSUPPORTED
            val (model, _) = model()
            model.tapMicrophone()
            runCurrent()
            model.setLanguages(source = "es-ES", target = "en-US")
            runCurrent()
            assertNull(model.uiState.value.problem)
            assertEquals(RecognitionSupport.READY, model.uiState.value.recognition)
        }
}
