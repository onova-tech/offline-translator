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
import tech.onova.offlinetranslator.core.domain.ports.ClipboardWriter
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.PackFailure
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.PackState
import tech.onova.offlinetranslator.core.domain.ports.PackStatus
import tech.onova.offlinetranslator.core.domain.ports.PackUnavailable
import tech.onova.offlinetranslator.core.domain.ports.PackWait
import tech.onova.offlinetranslator.core.domain.ports.SettingsPage
import tech.onova.offlinetranslator.feature.toolstranslate.domain.EngineSession
import tech.onova.offlinetranslator.feature.toolstranslate.domain.PackOffer
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TextLimit
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorSettings
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorSettingsRepository
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeAppVisibility
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeContentPacks
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeSettingsPages
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeTranslationEngine
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Spec 012 story 2 (T039): a language needed the first time shows its size; nothing downloads before Download. */
@OptIn(ExperimentalCoroutinesApi::class)
class PackOfferTest {
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

    private val settings = InMemorySettings(TranslatorSettings(source = pt, target = es))
    private val packs = FakeContentPacks(FakeContentPacks.installed(packPt), FakeContentPacks.notInstalled(packEs))
    private val engine = FakeTranslationEngine()
    private val pages = FakeSettingsPages()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** The translator with "Bom dia" typed, and the packs' actions. */
    private fun TestScope.screen(): Pair<TranslatorViewModel, PacksViewModel> {
        val outputs = TranslatorOutputs(ClipboardWriter { _, _ -> false })
        val session = EngineSession(packs, engine, dispatcher)
        val translator = TranslatorViewModel(settings, packs, session, outputs, FakeAppVisibility())
        translator.setAppLanguage("pt")
        val actions = PacksViewModel(packs, settings, pages)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { translator.uiState.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { actions.uiState.collect {} }
        translator.setText("Bom dia")
        advanceTimeBy(TextLimit.PAUSE_MILLIS + 1)
        runCurrent()
        return translator to actions
    }

    private fun TranslatorViewModel.offer(): PackOffer? = (uiState.value.view as? TranslationView.NeedsPack)?.offer

    @Test
    fun theOfferShowsAsSoonAsThePairNeedsItBeforeAnythingIsTyped() =
        runTest(dispatcher) {
            val (translator, _) = screen()
            translator.setText("")
            advanceTimeBy(TextLimit.PAUSE_MILLIS + 1)
            runCurrent()
            assertEquals(PackOffer.Offer(packEs, megabytes = 48), translator.offer())
        }

    @Test
    fun theOfferShowsTheSizeAndNothingIsFetchedBeforeDownload() =
        runTest(dispatcher) {
            val (translator, _) = screen()
            assertEquals(PackOffer.Offer(packEs, megabytes = 48), translator.offer())
            assertTrue(packs.downloads.isEmpty())
            assertTrue(engine.requests.isEmpty())
        }

    @Test
    fun downloadFollowsTheWifiSwitchAndShowsItsProgress() =
        runTest(dispatcher) {
            val (translator, actions) = screen()
            actions.download(packEs)
            runCurrent()
            assertEquals(listOf(packEs to true), packs.downloads)
            assertEquals(PackOffer.Downloading(packEs, 0, 48, 0), translator.offer())
            packs.progress(packEs, bytes = 24_000_000)
            runCurrent()
            assertEquals(PackOffer.Downloading(packEs, 24, 48, 50), translator.offer())

            settings.settings.value = settings.settings.value.copy(wifiOnly = false)
            actions.download(packEs)
            runCurrent()
            assertEquals(packEs to false, packs.downloads.last(), "with the switch off, mobile data too")
        }

    @Test
    fun cancelStopsItAndLeavesNothing() =
        runTest(dispatcher) {
            val (translator, actions) = screen()
            actions.download(packEs)
            runCurrent()
            packs.progress(packEs, bytes = 1_000_000)
            actions.cancel(packEs)
            runCurrent()
            assertEquals(listOf(packEs), packs.cancelled)
            assertEquals(PackOffer.Offer(packEs, megabytes = 48), translator.offer())
            assertNull(packs.location(packEs))
        }

    @Test
    fun waitingForWifiOffersTheNetworkSettings() =
        runTest(dispatcher) {
            val (translator, actions) = screen()
            packs.put(PackStatus(packEs, FakeContentPacks.PACK_BYTES, null, PackState.Waiting(PackWait.FOR_WIFI)))
            runCurrent()
            assertEquals(PackOffer.WaitingForWifi(packEs), translator.offer())
            actions.open(SettingsPage.NETWORK)
            assertEquals(listOf(SettingsPage.NETWORK), pages.opened)
        }

    @Test
    fun aPackGooglePlayHoldsForAConfirmationOpensItsDialogInsteadOfFetchingAgain() =
        runTest(dispatcher) {
            val (translator, actions) = screen()
            val waiting = PackState.Waiting(PackWait.FOR_CONFIRMATION)
            packs.put(PackStatus(packEs, FakeContentPacks.PACK_BYTES, null, waiting))
            runCurrent()
            assertEquals(PackOffer.WaitingForConfirmation(packEs), translator.offer())
            actions.download(packEs)
            runCurrent()
            assertEquals(listOf(packEs), packs.confirmed)
            assertTrue(packs.downloads.isEmpty(), "fetching again would only wait again")
        }

    @Test
    fun tooLittleSpaceSaysHowMuchIsNeededAndOffersTheStorageSettings() =
        runTest(dispatcher) {
            val (translator, actions) = screen()
            packs.fail(packEs, PackFailure.NoSpace(bytesNeeded = 75_000_000))
            runCurrent()
            assertEquals(PackOffer.NoSpace(packEs, neededMegabytes = 75), translator.offer())
            actions.open(SettingsPage.STORAGE)
            assertEquals(listOf(SettingsPage.STORAGE), pages.opened)
        }

    @Test
    fun aFailedDownloadCanBeTriedAgain() =
        runTest(dispatcher) {
            val (translator, actions) = screen()
            actions.download(packEs)
            runCurrent()
            packs.fail(packEs, PackFailure.Network)
            runCurrent()
            assertEquals(PackOffer.Failed(packEs), translator.offer())
            actions.download(packEs)
            runCurrent()
            assertEquals(2, packs.downloads.size)
        }

    @Test
    fun withoutAStoreThereIsNothingToDownload() =
        runTest(dispatcher) {
            val (translator, _) = screen()
            packs.put(PackStatus(packEs, null, null, PackState.Unavailable(PackUnavailable.NO_STORE)))
            runCurrent()
            assertEquals(PackOffer.NoStore(packEs), translator.offer())
        }

    @Test
    fun aCompletedDownloadContinuesTheTranslationThePersonWanted() =
        runTest(dispatcher) {
            val (translator, actions) = screen()
            actions.download(packEs)
            runCurrent()
            packs.finish(packEs)
            runCurrent()
            assertEquals(
                TranslationView.Shown("[es] [en] Bom dia", setOf(TranslationNote.THROUGH_ENGLISH)),
                translator.uiState.value.view,
            )
        }

    @Test
    fun aPackDeletedWhileInUseStopsItsTranslationAndIsOfferedAgain() =
        runTest(dispatcher) {
            settings.settings.value = settings.settings.value.copy(target = LanguageCode.ENGLISH)
            val (translator, actions) = screen()
            assertEquals(TranslationView.Shown("[en] Bom dia", emptySet()), translator.uiState.value.view)
            engine.hold()
            translator.setText("Bom dia!")
            advanceTimeBy(TextLimit.PAUSE_MILLIS + 1)
            runCurrent()
            actions.remove(packPt)
            runCurrent()
            engine.release()
            runCurrent()
            assertEquals(listOf(packPt), packs.removed)
            assertEquals(PackOffer.Offer(packPt, megabytes = 48), translator.offer())
            assertEquals(mapOf<PackId, String>(), engine.opened.last(), "reloaded without it")
        }

    @Test
    fun theWifiSwitchIsRemembered() =
        runTest(dispatcher) {
            val (_, actions) = screen()
            assertEquals(true, actions.uiState.value.wifiOnly)
            actions.setWifiOnly(false)
            runCurrent()
            assertEquals(false, settings.settings.value.wifiOnly)
            assertEquals(false, actions.uiState.value.wifiOnly)
        }
}
