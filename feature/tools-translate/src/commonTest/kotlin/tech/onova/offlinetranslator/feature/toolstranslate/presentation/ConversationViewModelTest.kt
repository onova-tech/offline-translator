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
import tech.onova.offlinetranslator.core.domain.ports.ClipboardWriter
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.SpeechEnd
import tech.onova.offlinetranslator.core.domain.ports.SpeechEvent
import tech.onova.offlinetranslator.core.domain.ports.SpeechFailure
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationMachine
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationPhase
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationProblem
import tech.onova.offlinetranslator.feature.toolstranslate.domain.EngineSession
import tech.onova.offlinetranslator.feature.toolstranslate.domain.PackOffer
import tech.onova.offlinetranslator.feature.toolstranslate.domain.PauseReason
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SetupNeed
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SetupNeeds
import tech.onova.offlinetranslator.feature.toolstranslate.domain.Side
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SideMessage
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SpeechDownloads
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorSettings
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorSettingsRepository
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeContentPacks
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeScreenReader
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeSpeechInput
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeSpeechOutput
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeTranslationEngine
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Spec 012 story 5 (T050): two people, one language each, a spoken turn translated on the other half. */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val pt = LanguageCode("pt")
    private val es = LanguageCode("es")
    private val packPt = PackId("translate_pt")
    private val packEs = PackId("translate_es")

    private class InMemorySettings(
        initial: TranslatorSettings,
    ) : TranslatorSettingsRepository {
        override val settings = MutableStateFlow(initial)

        override suspend fun update(transform: (TranslatorSettings) -> TranslatorSettings) {
            settings.value = transform(settings.value)
        }
    }

    private val settings = InMemorySettings(TranslatorSettings(near = pt, far = es))
    private val packs = FakeContentPacks(FakeContentPacks.installed(packPt), FakeContentPacks.installed(packEs))
    private val engine = FakeTranslationEngine()
    private val speechIn = FakeSpeechInput()
    private val speechOut = FakeSpeechOutput()
    private val screenReader = FakeScreenReader()
    private val visible = MutableStateFlow(true)
    private val copied = mutableListOf<String>()

    /** Shared with the Manage languages page, which asks for a language: the tests ask through it as the page does. */
    private lateinit var downloads: SpeechDownloads

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.model(): ConversationViewModel {
        downloads = SpeechDownloads { testScheduler.currentTime }
        val visibility =
            object : AppVisibility {
                override val isVisible = visible
            }
        val clipboard =
            ClipboardWriter { _, text ->
                copied += text
                true
            }
        val model =
            ConversationViewModel(
                repository = settings,
                packs = packs,
                session = EngineSession(packs, engine, dispatcher),
                speech = ConversationSpeech(speechIn, speechOut, screenReader, visibility, downloads),
                outputs = TranslatorOutputs(clipboard),
            )
        model.start(appLanguage = "pt", phoneLocales = listOf("pt-BR"))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        runCurrent()
        return model
    }

    private fun ConversationViewModel.phase() = uiState.value.conversation.phase

    /** The near half says "Bom dia" and the phone reads its translation in Spanish. */
    private fun TestScope.nearSays(
        model: ConversationViewModel,
        text: String,
    ) {
        model.tap(Side.NEAR)
        runCurrent()
        speechIn.hear(SpeechEvent.Final(text))
        runCurrent()
    }

    @Test
    fun aTurnIsTranslatedOnTheOtherHalfAndSpokenInItsLanguage() =
        runTest(dispatcher) {
            val model = model()
            model.tap(Side.NEAR)
            runCurrent()
            assertEquals(ConversationPhase.Listening(Side.NEAR, ""), model.phase())
            assertEquals(listOf("pt-BR"), speechIn.listenedTo)
            speechIn.hear(SpeechEvent.Partial("Bom"))
            runCurrent()
            assertEquals(ConversationPhase.Listening(Side.NEAR, "Bom"), model.phase())
            speechIn.hear(SpeechEvent.Final("Bom dia"))
            runCurrent()

            val turn =
                model.uiState.value.conversation.turns
                    .single()
            assertEquals(Side.NEAR, turn.side)
            assertEquals("Bom dia", turn.heard)
            assertEquals("[es] [en] Bom dia", turn.translation)
            assertEquals(ConversationPhase.Speaking(turn.id), model.phase())
            assertEquals(listOf("[es] [en] Bom dia" to "es-ES"), speechOut.said)
            speechOut.done()
            runCurrent()
            assertEquals(ConversationPhase.Idle, model.phase())
        }

    @Test
    fun noMicrophoneListensWhileThePhoneSpeaks() =
        runTest(dispatcher) {
            val model = model()
            nearSays(model, "Bom dia")
            assertTrue(model.phase() is ConversationPhase.Speaking)
            assertFalse(model.uiState.value.canListen)
            model.tap(Side.FAR)
            runCurrent()
            assertTrue(model.phase() is ConversationPhase.Speaking)
            assertEquals(0, speechIn.listening)
            assertEquals(1, speechIn.listenedTo.size)
        }

    @Test
    fun aPauseEndsTheTurnOnceSomethingWasHeard() =
        runTest(dispatcher) {
            val model = model()
            model.tap(Side.FAR)
            runCurrent()
            assertEquals(listOf("es-ES"), speechIn.listenedTo)
            speechIn.hear(SpeechEvent.Partial("Buenos días"))
            runCurrent()
            advanceTimeBy(ConversationMachine.SILENCE_MILLIS + 1)
            runCurrent()
            assertEquals(
                "Buenos días",
                model.uiState.value.conversation.turns
                    .single()
                    .heard,
            )
            assertEquals(0, speechIn.listening)
        }

    @Test
    fun nothingHeardSaysSoOnThatHalf() =
        runTest(dispatcher) {
            val model = model()
            model.tap(Side.FAR)
            runCurrent()
            speechIn.hear(SpeechEvent.Failed(SpeechFailure.NOTHING_HEARD))
            runCurrent()
            assertEquals(ConversationPhase.Idle, model.phase())
            assertEquals(
                SideMessage(Side.FAR, ConversationProblem.NOTHING_HEARD),
                model.uiState.value.conversation.message,
            )
        }

    @Test
    fun aCorrectedTurnIsTranslatedAgain() =
        runTest(dispatcher) {
            val model = model()
            nearSays(model, "Bom dia")
            speechOut.done()
            runCurrent()
            val id =
                model.uiState.value.conversation.turns
                    .single()
                    .id
            model.edit(id, "Boa noite")
            runCurrent()
            val turn =
                model.uiState.value.conversation.turns
                    .single()
            assertEquals("Boa noite", turn.heard)
            assertEquals("[es] [en] Boa noite", turn.translation)
            assertTrue(turn.edited)
        }

    @Test
    fun withTheScreenReaderOnTheTranslationIsShownNotSpoken() =
        runTest(dispatcher) {
            screenReader.on.value = true
            val model = model()
            nearSays(model, "Bom dia")
            assertEquals(
                "[es] [en] Bom dia",
                model.uiState.value.conversation.turns
                    .single()
                    .translation,
            )
            assertEquals(ConversationPhase.Idle, model.phase())
            assertTrue(speechOut.said.isEmpty())
            assertTrue(model.uiState.value.screenReader, "so the screen announces it instead")
        }

    @Test
    fun withReadAloudOffNothingIsSpoken() =
        runTest(dispatcher) {
            val model = model()
            model.setOption(ConversationOption.READ_ALOUD, on = false)
            nearSays(model, "Bom dia")
            assertEquals(ConversationPhase.Idle, model.phase())
            assertTrue(speechOut.said.isEmpty())
        }

    @Test
    fun withoutAnOfflineVoiceTheTranslationIsOnlyShown() =
        runTest(dispatcher) {
            speechOut.voices["es-ES"] = VoiceSupport.NO_OFFLINE_VOICE
            val model = model()
            assertEquals(VoiceSupport.NO_OFFLINE_VOICE, model.uiState.value.far.voice)
            nearSays(model, "Bom dia")
            assertEquals(ConversationPhase.Idle, model.phase())
            assertTrue(speechOut.said.isEmpty())
        }

    @Test
    fun hidingTheScreenPausesEverythingUntilTheNextTap() =
        runTest(dispatcher) {
            val model = model()
            model.tap(Side.NEAR)
            runCurrent()
            visible.value = false
            runCurrent()
            assertEquals(ConversationPhase.Paused(PauseReason.HIDDEN), model.phase())
            assertEquals(0, speechIn.listening)
            visible.value = true
            model.tap(Side.NEAR)
            runCurrent()
            assertEquals(ConversationPhase.Idle, model.phase())
        }

    @Test
    fun aCallOrAnAlarmWhileThePhoneSpeaksPausesUntilTheNextTap() =
        runTest(dispatcher) {
            val model = model()
            nearSays(model, "Bom dia")
            assertTrue(model.phase() is ConversationPhase.Speaking)
            speechOut.done(SpeechEnd.INTERRUPTED)
            runCurrent()
            assertEquals(ConversationPhase.Paused(PauseReason.CALL_OR_ALARM), model.phase())
            assertEquals(0, speechIn.listening, "nothing starts by itself after the call")
            model.tap(Side.FAR)
            runCurrent()
            assertEquals(ConversationPhase.Idle, model.phase())
            assertEquals(
                "[es] [en] Bom dia",
                model.uiState.value.conversation.turns
                    .single()
                    .translation,
            )
        }

    @Test
    fun theSquareButtonStopsListeningAndNothingIsTranslated() =
        runTest(dispatcher) {
            val model = model()
            model.tap(Side.NEAR)
            runCurrent()
            speechIn.hear(SpeechEvent.Partial("Onde fi"))
            runCurrent()
            model.tap(Side.NEAR, discard = true)
            runCurrent()
            assertEquals(ConversationPhase.Idle, model.phase())
            assertEquals(0, speechIn.listening)
            assertTrue(
                model.uiState.value.conversation.turns
                    .isEmpty(),
            )
            assertTrue(engine.requests.isEmpty())
        }

    @Test
    fun copySendsTheTextTheScreenShows() =
        runTest(dispatcher) {
            val model = model()
            model.clipboard.copy(label = "Conversation", text = "Portuguese: Bom dia")
            assertEquals(listOf("Portuguese: Bom dia"), copied)
        }

    @Test
    fun clearEmptiesTheConversation() =
        runTest(dispatcher) {
            val model = model()
            nearSays(model, "Bom dia")
            speechOut.done()
            runCurrent()
            model.clear()
            runCurrent()
            assertTrue(
                model.uiState.value.conversation.turns
                    .isEmpty(),
            )
        }

    @Test
    fun theLanguagesAndTheFarHalfAreRemembered() =
        runTest(dispatcher) {
            val model = model()
            model.setLanguage(Side.FAR, LanguageCode.ENGLISH)
            model.setOption(ConversationOption.FAR_TURNED, on = false)
            runCurrent()
            assertEquals(LanguageCode.ENGLISH, settings.settings.value.far)
            assertEquals(false, settings.settings.value.farHalfTurned)
            assertEquals(LanguageCode.ENGLISH, model.uiState.value.conversation.far)
            assertFalse(model.uiState.value.farTurned)
            model.tap(Side.FAR)
            runCurrent()
            assertEquals("en-US", speechIn.listenedTo.last())
        }

    @Test
    fun theSetupComesOneThingAtATimeAndAGotLanguageMovesOnByItself() =
        runTest(dispatcher) {
            speechIn.support["pt-BR"] = RecognitionSupport.CAN_DOWNLOAD
            speechIn.support["es-ES"] = RecognitionSupport.CAN_DOWNLOAD
            val model = model()
            val near = SetupNeed.Recognition(Side.NEAR, downloading = false)
            assertEquals(near, model.uiState.value.setup)
            // Asked for on the Manage languages page, which the notice opens.
            downloads.asked("pt-BR")
            runCurrent()
            // The phone's own dialog, then its download: nothing tells the app, so it asks again.
            speechIn.support["pt-BR"] = RecognitionSupport.DOWNLOADING
            advanceTimeBy(SetupNeeds.RECHECK_MILLIS + 1)
            runCurrent()
            assertEquals(SetupNeed.Recognition(Side.NEAR, downloading = true), model.uiState.value.setup)
            // The owner's phone: done downloading, not yet listed as installed. It isn't offered again.
            speechIn.support["pt-BR"] = RecognitionSupport.CAN_DOWNLOAD
            advanceTimeBy(SetupNeeds.RECHECK_MILLIS + 1)
            runCurrent()
            assertEquals(SetupNeed.Recognition(Side.NEAR, downloading = true), model.uiState.value.setup)
            speechIn.support["pt-BR"] = RecognitionSupport.READY
            advanceTimeBy(SetupNeeds.RECHECK_MILLIS + 1)
            runCurrent()
            assertEquals(SetupNeed.Recognition(Side.FAR, downloading = false), model.uiState.value.setup)
            assertTrue(model.uiState.value.near.canListen)
        }

    /**
     * The owner's phone, 2026-11-20: its speech service (last updated in 2024) downloaded Japanese but never installed
     * it. Asked for on the Manage languages page and never installed, the half says so; back from the store, the
     * language can be asked for again.
     */
    @Test
    fun aLanguageThePhoneDownloadsButNeverInstallsSaysSo() =
        runTest(dispatcher) {
            speechIn.support["es-ES"] = RecognitionSupport.CAN_DOWNLOAD
            val model = model()
            val far = SetupNeed.Recognition(Side.FAR, downloading = false)
            assertEquals(far, model.uiState.value.setup)
            downloads.asked("es-ES")
            repeat((SpeechDownloads.ASKED_MILLIS / SetupNeeds.RECHECK_MILLIS + 2).toInt()) {
                advanceTimeBy(SetupNeeds.RECHECK_MILLIS + 1)
                runCurrent()
            }
            assertEquals(SetupNeed.NotInstalled(Side.FAR), model.uiState.value.setup)
            // Back from the store (the Manage languages page's Update): it can be asked for again.
            visible.value = false
            runCurrent()
            visible.value = true
            runCurrent()
            assertEquals(far, model.uiState.value.setup)
        }

    /**
     * Owner, 2026-11-20: a notice nothing can fix (Arabic, which the phone can't recognize) has a small ×. It stays
     * away while the Translator is open, the phone's settings and back included, and comes back when its half's
     * language is chosen again, so a wrong tap can be undone.
     */
    @Test
    fun aDismissedNoticeStaysAwayUntilItsLanguageIsChosenAgain() =
        runTest(dispatcher) {
            speechIn.support["es-ES"] = RecognitionSupport.UNSUPPORTED
            val model = model()
            val cant = SetupNeed.NoRecognition(Side.FAR)
            assertEquals(cant, model.uiState.value.setup)
            model.dismiss(cant)
            assertNull(model.uiState.value.setup)
            visible.value = false
            runCurrent()
            visible.value = true
            runCurrent()
            assertNull(model.uiState.value.setup)
            model.setLanguage(Side.FAR, LanguageCode.ENGLISH)
            runCurrent()
            model.setLanguage(Side.FAR, es)
            runCurrent()
            assertEquals(cant, model.uiState.value.setup)
        }

    @Test
    fun aVoiceAddedInTheSettingsCountsWhenTheScreenComesBack() =
        runTest(dispatcher) {
            speechOut.voices["es-ES"] = VoiceSupport.NO_OFFLINE_VOICE
            val model = model()
            val voice = SetupNeed.Voice(Side.FAR)
            assertEquals(voice, model.uiState.value.setup)
            visible.value = false
            runCurrent()
            speechOut.voices["es-ES"] = VoiceSupport.READY
            visible.value = true
            runCurrent()
            assertNull(model.uiState.value.setup)
        }

    @Test
    fun aMissingPackIsOfferedOnTheHalfThatNeedsItAndNothingListens() =
        runTest(dispatcher) {
            packs.put(FakeContentPacks.notInstalled(packEs))
            val model = model()
            assertEquals(PackOffer.Offer(packEs, megabytes = 48), model.uiState.value.offer)
            assertEquals(SetupNeed.Pack(PackOffer.Offer(packEs, megabytes = 48)), model.uiState.value.setup)
            assertFalse(model.uiState.value.canListen)
            model.tap(Side.NEAR)
            runCurrent()
            assertTrue(speechIn.listenedTo.isEmpty())
            assertNull(model.uiState.value.conversation.message)
        }
}
