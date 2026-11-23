package tech.onova.offlinetranslator.feature.toolstranslate.presentation

import androidx.lifecycle.ViewModelStore
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
import tech.onova.offlinetranslator.core.domain.ports.ClipboardWriter
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.Route
import tech.onova.offlinetranslator.feature.toolstranslate.domain.EngineSession
import tech.onova.offlinetranslator.feature.toolstranslate.domain.PackOffer
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TextLimit
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorMode
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorSettings
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorSettingsRepository
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeAppVisibility
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeContentPacks
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeTranslationEngine
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Spec 012 story 1 (T034): type or paste a text and read it translated, with the fakes. */
@OptIn(ExperimentalCoroutinesApi::class)
class TranslatorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val en = LanguageCode.ENGLISH
    private val pt = LanguageCode("pt")
    private val es = LanguageCode("es")
    private val packPt = PackId("translate_pt")
    private val packEs = PackId("translate_es")

    private class InMemorySettings(
        initial: TranslatorSettings = TranslatorSettings(),
    ) : TranslatorSettingsRepository {
        override val settings = MutableStateFlow(initial)

        override suspend fun update(transform: (TranslatorSettings) -> TranslatorSettings) {
            settings.value = transform(settings.value)
        }
    }

    private class Outputs : ClipboardWriter {
        val copied = mutableListOf<String>()
        var confirmsCopy = false

        override fun copy(
            label: String,
            text: String,
        ): Boolean {
            copied += text
            return confirmsCopy
        }
    }

    private val settings = InMemorySettings(TranslatorSettings(source = pt, target = en))
    private val packs = FakeContentPacks(FakeContentPacks.installed(packPt), FakeContentPacks.installed(packEs))
    private val engine = FakeTranslationEngine()
    private val outputs = Outputs()
    private val visibility = FakeAppVisibility()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.model(repository: TranslatorSettingsRepository = settings): TranslatorViewModel {
        val session = EngineSession(packs, engine, dispatcher)
        val model = TranslatorViewModel(repository, packs, session, TranslatorOutputs(outputs), visibility)
        model.setAppLanguage("en")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        runCurrent()
        return model
    }

    private fun TestScope.typeAndWait(
        model: TranslatorViewModel,
        text: String,
    ) {
        model.setText(text)
        advanceTimeBy(TextLimit.PAUSE_MILLIS + 1)
        runCurrent()
    }

    @Test
    fun aTranslationStartsAfterHalfASecondWithoutTyping() =
        runTest(dispatcher) {
            val model = model()
            model.setText("Bom dia")
            advanceTimeBy(TextLimit.PAUSE_MILLIS - 1)
            runCurrent()
            assertTrue(engine.requests.isEmpty(), "nothing is translated while the person may still be typing")
            advanceTimeBy(2)
            runCurrent()
            assertEquals(listOf<Pair<String, Route>>("Bom dia" to Route.Direct(pt, en, packPt)), engine.requests)
            assertEquals(TranslationView.Shown("[en] Bom dia", emptySet()), model.uiState.value.view)
        }

    @Test
    fun aNewerTextReplacesAnOlderOneAndTheOlderResultIsDropped() =
        runTest(dispatcher) {
            val model = model()
            val seen = mutableListOf<TranslationView>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                model.uiState.collect { seen += it.view }
            }
            engine.hold()
            typeAndWait(model, "Bom")
            typeAndWait(model, "Bom dia")
            engine.release()
            runCurrent()
            assertEquals(listOf("Bom", "Bom dia"), engine.requests.map { it.first })
            assertEquals(TranslationView.Shown("[en] Bom dia", emptySet()), model.uiState.value.view)
            assertTrue(seen.none { it is TranslationView.Shown && it.text == "[en] Bom" }, "the older result: $seen")
        }

    @Test
    fun aResultThatArrivesWhileThePersonTypesIsMarkedAsBeingReplaced() =
        runTest(dispatcher) {
            val model = model()
            typeAndWait(model, "Bom")
            assertEquals(TranslationView.Shown("[en] Bom", emptySet()), model.uiState.value.view)
            model.setText("Bom d")
            runCurrent()
            assertEquals(TranslationView.Shown("[en] Bom", emptySet(), pending = true), model.uiState.value.view)

            engine.hold()
            advanceTimeBy(TextLimit.PAUSE_MILLIS + 1)
            runCurrent()
            model.setText("Bom di")
            engine.release()
            runCurrent()
            assertEquals(
                TranslationView.Shown("[en] Bom d", emptySet(), pending = true),
                model.uiState.value.view,
                "a key came after it: not announced",
            )
            advanceTimeBy(TextLimit.PAUSE_MILLIS + 1)
            runCurrent()
            assertEquals(TranslationView.Shown("[en] Bom di", emptySet()), model.uiState.value.view)
        }

    @Test
    fun theSameLanguageShowsTheTextAsTypedWithItsNote() =
        runTest(dispatcher) {
            val model = model()
            model.choose(LanguageSide.TARGET, pt)
            typeAndWait(model, "  Bom dia  ")
            assertEquals(
                TranslationView.Shown("Bom dia", setOf(TranslationNote.SAME_LANGUAGE)),
                model.uiState.value.view,
            )
            assertTrue(engine.requests.isEmpty())
        }

    @Test
    fun overTheLimitOnlyTheFirst5000CharactersAreTranslatedWithTheNote() =
        runTest(dispatcher) {
            val model = model()
            val long = "a".repeat(TextLimit.MAX_CHARACTERS) + "bcd"
            typeAndWait(model, long)
            assertEquals("a".repeat(TextLimit.MAX_CHARACTERS), engine.requests.single().first)
            val view = model.uiState.value.view as TranslationView.Shown
            assertEquals(setOf(TranslationNote.OVER_LIMIT), view.notes)
            assertEquals(TextLimit.MAX_CHARACTERS + 3, model.uiState.value.count)
            assertTrue(model.uiState.value.overLimit)
        }

    @Test
    fun aPairWithoutEnglishGoesThroughEnglishAndSaysSo() =
        runTest(dispatcher) {
            val model = model()
            model.choose(LanguageSide.TARGET, es)
            typeAndWait(model, "Bom dia")
            assertEquals(
                TranslationView.Shown("[es] [en] Bom dia", setOf(TranslationNote.THROUGH_ENGLISH)),
                model.uiState.value.view,
            )
        }

    @Test
    fun swapExchangesTheLanguagesAndPutsTheTranslationInTheField() =
        runTest(dispatcher) {
            val model = model()
            typeAndWait(model, "Bom dia")
            model.swap()
            runCurrent()
            assertEquals(en, model.uiState.value.source)
            assertEquals(pt, model.uiState.value.target)
            assertEquals("[en] Bom dia", model.uiState.value.text)
            advanceTimeBy(TextLimit.PAUSE_MILLIS + 1)
            runCurrent()
            assertEquals(TranslationView.Shown("[pt] [en] Bom dia", emptySet()), model.uiState.value.view)
        }

    @Test
    fun theLanguagesAndTheModeAreRememberedButNotTheText() =
        runTest(dispatcher) {
            val model = model()
            model.choose(LanguageSide.SOURCE, es)
            model.choose(LanguageSide.TARGET, pt)
            model.setMode(TranslatorMode.CONVERSATION)
            typeAndWait(model, "Buenos días")
            assertEquals(
                TranslatorSettings(source = es, target = pt, mode = TranslatorMode.CONVERSATION),
                settings.settings.value,
            )

            val again = model()
            assertEquals(es, again.uiState.value.source)
            assertEquals(pt, again.uiState.value.target)
            assertEquals(TranslatorMode.CONVERSATION, again.uiState.value.mode)
            assertEquals("", again.uiState.value.text)
            assertEquals(TranslationView.Empty, again.uiState.value.view)
        }

    @Test
    fun aChosenModeShowsAtOnceWhileItIsSaved() =
        runTest(dispatcher) {
            val slow =
                object : TranslatorSettingsRepository {
                    override val settings = MutableStateFlow(TranslatorSettings(source = pt, target = en))

                    override suspend fun update(transform: (TranslatorSettings) -> TranslatorSettings) {
                        kotlinx.coroutines.delay(SAVE_MILLIS)
                        settings.value = transform(settings.value)
                    }
                }
            val model = model(slow)
            model.setMode(TranslatorMode.CONVERSATION)
            runCurrent()
            assertEquals(TranslatorMode.CONVERSATION, model.uiState.value.mode)
            advanceTimeBy(SAVE_MILLIS + 1)
            runCurrent()
            assertEquals(TranslatorMode.CONVERSATION, slow.settings.value.mode)
        }

    @Test
    fun theEngineOpensWithTheInstalledPacksAndClosesWhenTheToolIsLeft() =
        runTest(dispatcher) {
            val model = model()
            assertEquals(
                listOf(mapOf(packPt to "packs/translate_pt", packEs to "packs/translate_es")),
                engine.opened,
            )
            val store = ViewModelStore()
            store.put("translator", model)
            store.clear()
            runCurrent()
            assertEquals(1, engine.closes)
        }

    @Test
    fun aMissingPackIsOfferedInsteadOfATranslation() =
        runTest(dispatcher) {
            packs.put(FakeContentPacks.notInstalled(packEs))
            val model = model()
            model.choose(LanguageSide.TARGET, es)
            typeAndWait(model, "Bom dia")
            assertEquals(
                TranslationView.NeedsPack(PackOffer.Offer(packEs, megabytes = 48)),
                model.uiState.value.view,
            )
            assertTrue(engine.requests.isEmpty())
            assertEquals(
                listOf(
                    LanguageOption(en, installed = true, downloadMegabytes = null),
                    LanguageOption(pt, installed = true, downloadMegabytes = null),
                    LanguageOption(es, installed = false, downloadMegabytes = 48),
                ),
                model.uiState.value.languages,
            )
        }

    @Test
    fun aFailedTranslationSaysSo() =
        runTest(dispatcher) {
            engine.failure = IllegalStateException("damaged model")
            val model = model()
            typeAndWait(model, "Bom dia")
            assertEquals(TranslationView.Failed, model.uiState.value.view)
        }

    @Test
    fun copyPutsTheTranslationOnTheClipboard() =
        runTest(dispatcher) {
            val model = model()
            val effects = mutableListOf<TranslatorEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.effects.collect { effects += it } }
            typeAndWait(model, "Bom dia")
            model.copy(label = "Translation")
            assertEquals(listOf("[en] Bom dia"), outputs.copied)
            assertEquals(listOf<TranslatorEffect>(TranslatorEffect.Copied), effects, "no system confirmation: ours")
        }

    @Test
    fun aHiddenScreenDropsARunningTranslationAndTranslatesAgainWhenShown() =
        runTest(dispatcher) {
            val model = model()
            engine.hold()
            typeAndWait(model, "Bom dia")
            assertEquals(1, engine.requests.size)
            visibility.isVisible.value = false
            runCurrent()
            engine.release()
            runCurrent()
            assertEquals(TranslationView.Working, model.uiState.value.view, "a dropped result isn't shown")
            visibility.isVisible.value = true
            runCurrent()
            assertEquals(2, engine.requests.size)
            assertEquals(TranslationView.Shown("[en] Bom dia", emptySet()), model.uiState.value.view)
        }

    @Test
    fun aTranslationDoneBeforeTheScreenHidIsntDoneAgain() =
        runTest(dispatcher) {
            val model = model()
            typeAndWait(model, "Bom dia")
            visibility.isVisible.value = false
            runCurrent()
            visibility.isVisible.value = true
            runCurrent()
            assertEquals(1, engine.requests.size)
            assertEquals(TranslationView.Shown("[en] Bom dia", emptySet()), model.uiState.value.view)
        }

    /** Spec 080 FR-006: Turkish has only the small model, so its translations say "Basic quality". */
    @Test
    fun aBasicQualityLanguageSaysSoUnderItsTranslation() =
        runTest(dispatcher) {
            packs.put(FakeContentPacks.installed(PackId("translate_tr")))
            val model = model(InMemorySettings(TranslatorSettings(source = en, target = LanguageCode("tr"))))
            typeAndWait(model, "Good morning")
            val view = model.uiState.value.view as TranslationView.Shown
            assertEquals(setOf(TranslationNote.BASIC_QUALITY), view.notes)
            model.choose(LanguageSide.TARGET, pt)
            advanceTimeBy(SAVE_MILLIS)
            runCurrent()
            assertEquals(emptySet(), (model.uiState.value.view as TranslationView.Shown).notes)
        }

    @Test
    fun withNothingChosenTheTextIsInTheAppsLanguage() =
        runTest(dispatcher) {
            val fresh = InMemorySettings()
            val session = EngineSession(packs, engine, dispatcher)
            val model = TranslatorViewModel(fresh, packs, session, TranslatorOutputs(outputs), visibility)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
            runCurrent()
            assertEquals(false, model.uiState.value.loaded, "the defaults wait for the app's language")
            model.setAppLanguage("pt")
            runCurrent()
            assertEquals(true, model.uiState.value.loaded)
            assertEquals(pt, model.uiState.value.source)
            assertEquals(en, model.uiState.value.target)
        }

    private companion object {
        const val SAVE_MILLIS = 1_000L
    }
}
