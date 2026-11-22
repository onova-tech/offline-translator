package tech.onova.offlinetranslator.feature.toolstranslate.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import tech.onova.offlinetranslator.core.domain.ports.EngineOpen
import tech.onova.offlinetranslator.core.domain.ports.EngineUnavailable
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.PackState
import tech.onova.offlinetranslator.core.domain.ports.Route
import tech.onova.offlinetranslator.core.domain.speech.SpeechCatalog
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeContentPacks
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeTranslationEngine
import kotlin.test.Test
import kotlin.test.assertEquals

/** Spec 012 edge case "Memory": the engine holds the installed packs only while a Translator screen uses it. */
@OptIn(ExperimentalCoroutinesApi::class)
class EngineSessionTest {
    private val packPt = PackId("translate_pt")
    private val packEs = PackId("translate_es")
    private val pt = LanguageCode("pt")
    private val es = LanguageCode("es")

    private fun TestScope.session(
        packs: FakeContentPacks,
        engine: FakeTranslationEngine,
    ) = EngineSession(packs, engine, StandardTestDispatcher(testScheduler))

    @Test
    fun theEngineOpensWithTheInstalledPacksOnlyWhileAScreenUsesIt() =
        runTest {
            val packs = FakeContentPacks(FakeContentPacks.installed(packPt), FakeContentPacks.notInstalled(packEs))
            val engine = FakeTranslationEngine()
            val session = session(packs, engine)
            assertEquals(EngineSession.State.Closed, session.state.value)

            session.acquire()
            runCurrent()
            assertEquals(listOf(mapOf(packPt to "packs/translate_pt")), engine.opened)
            assertEquals(EngineSession.State.Ready(setOf(packPt)), session.state.value)
            assertEquals(1, packs.refreshes, "the store is asked for the packs' states once")

            // The other mode's screen: the same models, not loaded twice.
            session.acquire()
            runCurrent()
            assertEquals(1, engine.opened.size)
            session.release()
            assertEquals(0, engine.closes)

            session.release()
            runCurrent()
            assertEquals(1, engine.closes)
            assertEquals(EngineSession.State.Closed, session.state.value)
        }

    /**
     * The emulator's journey, 2026-11-22: the app's speech packs, once in, were opened as translation packs, called
     * damaged and removed. The engine opens the translation packs only, and keeps its hands off the others.
     */
    @Test
    fun theAppsSpeechPacksAreNoTranslationPacks() =
        runTest {
            val packs =
                FakeContentPacks(
                    FakeContentPacks.installed(packPt),
                    FakeContentPacks.installed(SpeechCatalog.recognitionPack),
                    FakeContentPacks.installed(SpeechCatalog.voiceDataPack),
                )
            val engine = FakeTranslationEngine()
            session(packs, engine).acquire()
            runCurrent()
            assertEquals(listOf(mapOf(packPt to "packs/translate_pt")), engine.opened)
            assertEquals(emptyList(), packs.removed)
        }

    @Test
    fun aPackInstalledWhileOpenIsLoadedWithTheOthers() =
        runTest {
            val packs = FakeContentPacks(FakeContentPacks.installed(packPt), FakeContentPacks.notInstalled(packEs))
            val engine = FakeTranslationEngine()
            val session = session(packs, engine)
            session.acquire()
            runCurrent()

            packs.download(packEs, wifiOnly = true)
            packs.progress(packEs, bytes = 1_000)
            runCurrent()
            assertEquals(1, engine.opened.size, "progress alone doesn't reload the models")
            packs.finish(packEs)
            runCurrent()
            assertEquals(mapOf(packPt to "packs/translate_pt", packEs to "packs/translate_es"), engine.opened.last())
            assertEquals(EngineSession.State.Ready(setOf(packPt, packEs)), session.state.value)
        }

    @Test
    fun aPhoneWithoutTheEngineSaysSo() =
        runTest {
            val engine = FakeTranslationEngine()
            engine.openAnswer = EngineOpen.Unavailable(EngineUnavailable.UNSUPPORTED_DEVICE)
            val session = session(FakeContentPacks(FakeContentPacks.installed(packPt)), engine)
            session.acquire()
            runCurrent()
            assertEquals(EngineSession.State.Unavailable(EngineUnavailable.UNSUPPORTED_DEVICE), session.state.value)
        }

    @Test
    fun aDamagedPackIsRemovedSoItIsOfferedAgainAndTheOthersStayOpen() =
        runTest {
            val packs = FakeContentPacks(FakeContentPacks.installed(packPt), FakeContentPacks.installed(packEs))
            val engine = FakeTranslationEngine()
            engine.damagedPacks += packEs
            val session = session(packs, engine)
            session.acquire()
            runCurrent()
            assertEquals(listOf(packEs), packs.removed)
            assertEquals(PackState.NotInstalled, packs.stateOf(packEs))
            assertEquals(mapOf(packPt to "packs/translate_pt"), engine.opened.last(), "opened again without it")
            assertEquals(EngineSession.State.Ready(setOf(packPt)), session.state.value)
        }

    @Test
    fun aDamagedPackTheStoreKeepsIsReported() =
        runTest {
            val packs = FakeContentPacks(FakeContentPacks.installed(packPt), FakeContentPacks.installed(packEs))
            packs.keepsPacks = true
            val engine = FakeTranslationEngine()
            engine.damagedPacks += packEs
            val session = session(packs, engine)
            session.acquire()
            runCurrent()
            assertEquals(EngineSession.State.Unavailable(EngineUnavailable.DAMAGED_PACK), session.state.value)
        }

    @Test
    fun aRouteIsReadyOnlyOnceItsPacksAreLoaded() {
        val direct = Route.Direct(pt, LanguageCode.ENGLISH, packPt)
        val through = Route.ThroughEnglish(pt, es, packPt, packEs)
        val ptOnly = EngineSession.State.Ready(setOf(packPt))
        assertEquals(EngineSession.Readiness.READY, ptOnly.readiness(direct))
        assertEquals(EngineSession.Readiness.OPENING, ptOnly.readiness(through))
        assertEquals(EngineSession.Readiness.READY, EngineSession.State.Closed.readiness(Route.Same))
        assertEquals(EngineSession.Readiness.OPENING, EngineSession.State.Opening.readiness(direct))
        assertEquals(
            EngineSession.Readiness.UNAVAILABLE,
            EngineSession.State.Unavailable(EngineUnavailable.DAMAGED_PACK).readiness(direct),
        )
    }

    @Test
    fun translationsGoToTheEngine() =
        runTest {
            val engine = FakeTranslationEngine()
            val session = session(FakeContentPacks(FakeContentPacks.installed(packPt)), engine)
            assertEquals("[en] olá", session.translate("olá", Route.Direct(pt, LanguageCode.ENGLISH, packPt)))
        }
}
