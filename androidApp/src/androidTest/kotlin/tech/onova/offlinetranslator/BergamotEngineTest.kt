package tech.onova.offlinetranslator

import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tech.onova.offlinetranslator.core.domain.ports.EngineOpen
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.Route
import tech.onova.offlinetranslator.core.translation.BergamotEngine
import java.io.File

/**
 * Spec 012 T031: the real engine with the real packs, on the emulator (x86_64). The packs are too big for a test APK:
 * `scripts/ci.sh engine` pushes them to /data/local/tmp, and this test copies them into the app's files with run-as.
 * Without them, it's skipped, so the accessibility stage, which runs every class here, isn't held up.
 */
@RunWith(AndroidJUnit4::class)
class BergamotEngineTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext
    private val folder = File(app.filesDir, "engine-test-packs")
    private val engine = BergamotEngine()

    private val pt = LanguageCode("pt")
    private val es = LanguageCode("es")
    private val packPt = PackId("translate_pt")
    private val packEs = PackId("translate_es")

    @Before
    fun makeTheFolder() {
        shell("run-as ${app.packageName} mkdir -p ${folder.path}")
    }

    @After
    fun closeTheEngine() {
        engine.close()
    }

    @Test
    fun portugueseBecomesEnglishAndSpanishThroughEnglishInUnderFiveSeconds() =
        runBlocking {
            for (pack in listOf(packPt, packEs)) {
                shell("run-as ${app.packageName} cp -r $PUSHED/${pack.value} ${folder.path}/")
            }
            assumeTrue(
                "The packs aren't on the emulator: run scripts/ci.sh engine",
                File(folder, "translate_pt/pack.json").isFile && File(folder, "translate_es/pack.json").isFile,
            )
            val opened = engine.open(mapOf(packPt to "$folder/translate_pt", packEs to "$folder/translate_es"))
            assertEquals(EngineOpen.Ready, opened)

            val (english, direct) =
                timed { engine.translate(SENTENCE, Route.Direct(pt, LanguageCode.ENGLISH, packPt)) }
            val (spanish, pivot) =
                timed { engine.translate(SENTENCE, Route.ThroughEnglish(pt, es, packPt, packEs)) }
            Log.i(TAG, "pt→en in $direct ms: $english")
            Log.i(TAG, "pt→es through English in $pivot ms: $spanish")

            assertTrue("pt→en was empty", english.isNotBlank())
            assertTrue("pt→es was empty", spanish.isNotBlank())
            assertTrue("pt→en took $direct ms, model loading included", direct < LIMIT_MILLIS)
            assertTrue("pt→es took $pivot ms, model loading included", pivot < LIMIT_MILLIS)
        }

    /**
     * Spec 080 SC-002: every pack named in the instrumentation argument `packs` (`scripts/ci.sh engine --all` pushes
     * them five at a time to [BATCH]) loads and translates a sentence from English and back. Skipped without them.
     */
    @Test
    fun everyGivenLanguageTranslatesBothWaysWithEnglish() =
        runBlocking {
            val packs =
                InstrumentationRegistry
                    .getArguments()
                    .getString(
                        "packs",
                    ).orEmpty()
                    .split(',')
                    .filter { it.isNotBlank() }
            assumeTrue("No packs given: run scripts/ci.sh engine --all", packs.isNotEmpty())
            val failed = mutableListOf<String>()
            for (pack in packs) {
                val code = LanguageCode(pack.removePrefix("translate_"))
                val target = File(folder, pack)
                shell("run-as ${app.packageName} cp -r $BATCH/$pack ${folder.path}/")
                try {
                    val opened = engine.open(mapOf(PackId(pack) to target.path))
                    val there = engine.translate(ENGLISH, Route.Direct(LanguageCode.ENGLISH, code, PackId(pack)))
                    val back = engine.translate(there, Route.Direct(code, LanguageCode.ENGLISH, PackId(pack)))
                    Log.i(TAG, "$pack: $opened | $there | $back")
                    if (opened != EngineOpen.Ready || there.isBlank() || back.isBlank()) failed += pack
                } catch (error: IllegalStateException) {
                    Log.e(TAG, "$pack: ${error.message}")
                    failed += pack
                } finally {
                    engine.close()
                    shell("run-as ${app.packageName} rm -rf ${target.path}")
                }
            }
            assertTrue("These languages didn't translate: $failed", failed.isEmpty())
        }

    private suspend fun timed(block: suspend () -> String): Pair<String, Long> {
        val start = SystemClock.elapsedRealtime()
        val text = block()
        return text to SystemClock.elapsedRealtime() - start
    }

    /** Runs a shell command as the instrumentation's shell user and waits for it to end. */
    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use {
            it.readBytes()
        }
    }

    private companion object {
        const val TAG = "BergamotEngineTest"
        const val PUSHED = "/data/local/tmp"

        /** Where `scripts/ci.sh engine --all` pushes a batch of packs. */
        const val BATCH = "/data/local/tmp/engine-batch"

        /** A sentence for every language, from English and back (spec 080 SC-002). */
        const val ENGLISH = "Good morning! Where is the nearest train station? Thank you very much."
        const val LIMIT_MILLIS = 5_000L

        /** quickstart §2's 30-word test sentence: what a traveller asks, in everyday Portuguese. */
        const val SENTENCE =
            "Bom dia! Onde fica a estação de trem mais próxima? Preciso chegar ao centro antes do meio-dia, " +
                "porque tenho uma reunião importante e não conheço bem esta cidade. Muito obrigado!"
    }
}
