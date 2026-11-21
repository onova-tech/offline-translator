package tech.onova.offlinetranslator.feature.toolstranslate.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.translation.LanguageCatalog
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorSettings
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorSettingsRepository
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeLanguageNames
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Spec 080 story 1 (T017): the language page's rows, its search, at least two, and a replaced choice. */
@OptIn(ExperimentalCoroutinesApi::class)
class LanguagesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val en = LanguageCode.ENGLISH
    private val pt = LanguageCode("pt")
    private val es = LanguageCode("es")
    private val ja = LanguageCode("ja")

    private class InMemorySettings(
        initial: TranslatorSettings = TranslatorSettings(),
    ) : TranslatorSettingsRepository {
        override val settings = MutableStateFlow(initial)

        override suspend fun update(transform: (TranslatorSettings) -> TranslatorSettings) {
            settings.value = transform(settings.value)
        }
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.model(settings: InMemorySettings = InMemorySettings()): LanguagesViewModel {
        val model = LanguagesViewModel(settings, FakeLanguageNames())
        model.setAppLanguage("en")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        runCurrent()
        return model
    }

    private fun LanguagesViewModel.checked() =
        uiState.value.rows
            .filter { it.checked }
            .map { it.code }
            .toSet()

    @Test
    fun everyLanguageIsListedByNameWithEnglishPortugueseAndSpanishChecked() =
        runTest(dispatcher) {
            val rows = model().uiState.value.rows
            assertEquals(LanguageCatalog.all.size, rows.size)
            assertEquals(rows.map { it.name }.sorted(), rows.map { it.name })
            assertEquals(setOf(en, pt, es), rows.filter { it.checked }.map { it.code }.toSet())
            assertTrue(rows.single { it.code.value == "tr" }.basic)
            assertFalse(rows.single { it.code == ja }.basic)
        }

    @Test
    fun theSearchFindsEitherName() =
        runTest(dispatcher) {
            val model = model()
            model.search("name(ja")
            runCurrent()
            assertEquals(
                listOf(ja),
                model.uiState.value.rows
                    .map { it.code },
            )
            model.search("OWN(JA")
            runCurrent()
            assertEquals(
                listOf(ja),
                model.uiState.value.rows
                    .map { it.code },
            )
            model.search("nothing like it")
            runCurrent()
            assertTrue(
                model.uiState.value.rows
                    .isEmpty(),
            )
        }

    @Test
    fun aCheckAddsALanguageAndTheLastTwoCantGo() =
        runTest(dispatcher) {
            val model = model()
            model.toggle(ja)
            runCurrent()
            assertEquals(setOf(en, pt, es, ja), model.checked())
            listOf(pt, es).forEach { model.toggle(it) }
            runCurrent()
            assertEquals(setOf(en, ja), model.checked())
            assertTrue(model.uiState.value.atLeast)
            assertTrue(
                model.uiState.value.rows
                    .filter { it.checked }
                    .all { it.locked },
            )
            model.toggle(ja)
            runCurrent()
            assertEquals(setOf(en, ja), model.checked(), "two stay")
        }

    /** Owner, 2026-11-20: downloading a pack on Manage languages enables its language; twice changes nothing. */
    @Test
    fun enablingAddsALanguageOnce() =
        runTest(dispatcher) {
            val model = model()
            model.enable(ja)
            runCurrent()
            model.enable(ja)
            runCurrent()
            assertEquals(setOf(en, pt, es, ja), model.checked())
        }

    @Test
    fun anUncheckedChoiceTakesTheFirstCheckedByNameThatIsntTheOtherSide() =
        runTest(dispatcher) {
            val settings = InMemorySettings(TranslatorSettings(source = es, target = en, near = es, far = pt))
            val model = model(settings)
            model.toggle(es)
            runCurrent()
            val stored = settings.settings.value
            assertEquals(listOf(en, pt), stored.languages)
            // By name ("Name(en)" before "Name(pt)"): the source can't be English, the target's, so Portuguese.
            assertEquals(pt, stored.source)
            assertEquals(en, stored.target)
            assertEquals(en, stored.near, "the far half has Portuguese, so the near half takes English")
            assertEquals(pt, stored.far)
        }
}
