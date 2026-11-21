package tech.onova.offlinetranslator.feature.toolstranslate.fakes

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.PackFailure
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.PackState
import tech.onova.offlinetranslator.core.domain.ports.Route
import tech.onova.offlinetranslator.core.domain.ports.SpeechEnd
import tech.onova.offlinetranslator.core.domain.ports.SpeechEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The fakes behave as contracts/ports.md says, so the story tests can lean on them. */
class FakesTest {
    private val pt = LanguageCode("pt")
    private val es = LanguageCode("es")
    private val packPt = PackId("translate_pt")
    private val packEs = PackId("translate_es")

    @Test
    fun theEngineMarksTheLanguageAndGoesThroughEnglishInTwoSteps() =
        runTest {
            val engine = FakeTranslationEngine()
            assertEquals("[en] olá", engine.translate("olá", Route.Direct(pt, LanguageCode.ENGLISH, packPt)))
            assertEquals("[es] [en] olá", engine.translate("olá", Route.ThroughEnglish(pt, es, packPt, packEs)))
            assertEquals("olá", engine.translate("olá", Route.Same))
        }

    @Test
    fun aHeldEngineAnswersOnlyOnRelease() =
        runTest {
            val engine = FakeTranslationEngine()
            engine.hold()
            val answer = async { engine.translate("a", Route.Direct(LanguageCode.ENGLISH, pt, packPt)) }
            runCurrent()
            assertFalse(answer.isCompleted)
            engine.release()
            assertEquals("[pt] a", answer.await())
        }

    @Test
    fun aDownloadMovesStepByStepAndAFailureLeavesNoLocation() =
        runTest {
            val packs = FakeContentPacks(FakeContentPacks.notInstalled(packPt))
            packs.download(packPt, wifiOnly = true)
            assertEquals(PackState.Downloading(0, FakeContentPacks.PACK_BYTES), packs.stateOf(packPt))
            assertNull(packs.location(packPt))
            packs.progress(packPt, 1_000)
            assertEquals(PackState.Downloading(1_000, FakeContentPacks.PACK_BYTES), packs.stateOf(packPt))
            packs.finish(packPt)
            assertEquals("packs/translate_pt", packs.location(packPt))
            packs.fail(packPt, PackFailure.Damaged)
            assertNull(packs.location(packPt))
            assertEquals(listOf(packPt to true), packs.downloads)
        }

    @Test
    fun listeningEndsAfterTheFinalSentenceAndClosesTheMicrophone() =
        runTest {
            val speech = FakeSpeechInput()
            speech.hear(SpeechEvent.Partial("bom"), SpeechEvent.Final("bom dia"), SpeechEvent.Final("later"))
            val events = speech.listen("pt-BR").toList()
            assertEquals(listOf(SpeechEvent.Partial("bom"), SpeechEvent.Final("bom dia")), events)
            assertEquals(0, speech.listening)
            assertEquals(listOf("pt-BR"), speech.listenedTo)
        }

    @Test
    fun speakingLastsUntilTheTestSaysDone() =
        runTest {
            val voice = FakeSpeechOutput()
            var spoke: SpeechEnd? = null
            launch { spoke = voice.speak("Good morning", "en-US") }
            runCurrent()
            assertTrue(voice.speaking)
            assertNull(spoke)
            voice.done()
            runCurrent()
            assertEquals(SpeechEnd.DONE, spoke)
            assertEquals(listOf("Good morning" to "en-US"), voice.said)
        }
}
