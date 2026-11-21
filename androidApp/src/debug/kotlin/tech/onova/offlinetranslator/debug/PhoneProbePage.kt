package tech.onova.offlinetranslator.debug

import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import tech.onova.offlinetranslator.core.designsystem.components.BottomAction
import tech.onova.offlinetranslator.core.designsystem.components.BottomActionRow
import tech.onova.offlinetranslator.core.designsystem.components.BottomActionStyle
import tech.onova.offlinetranslator.core.designsystem.components.SectionHeader
import tech.onova.offlinetranslator.core.designsystem.components.SettingsRow
import tech.onova.offlinetranslator.core.designsystem.components.ToolLayout
import tech.onova.offlinetranslator.core.designsystem.components.ValueRow
import tech.onova.offlinetranslator.core.domain.ports.EngineOpen
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.Route
import tech.onova.offlinetranslator.core.domain.ports.SpeechInput
import tech.onova.offlinetranslator.core.domain.ports.SpeechOutput
import tech.onova.offlinetranslator.core.domain.ports.TranslationEngine
import java.io.File

/**
 * Spec 012 T032: a debug-only page that answers research R1, R2 and R3 on a real phone (quickstart §2): for each
 * language, whether the phone recognizes it on the device and has an offline voice; then the engine's processor, its
 * time for the 30-word sentence directly and through English, and the app's memory after loading. Only the app's own
 * components are used, so it reads what the Translator will see. English only: release builds don't have it.
 *
 * The packs come from `files/engine-test-packs/`, copied there as for the engine test:
 * `adb push packs/translate_pt/src/main/assets/translate_pt /data/local/tmp/`, then
 * `adb shell run-as tech.onova.offlinetranslator cp -r /data/local/tmp/translate_pt files/engine-test-packs/` (and es).
 */
@Composable
internal fun PhoneProbeRow(modifier: Modifier = Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    SettingsRow(
        title = "Phone probe (debug)",
        supporting = "Speech, voices and the translation engine on this phone",
        onClick = { open = true },
        modifier = modifier,
    )
    if (open) {
        Dialog(onDismissRequest = { open = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                PhoneProbePage(onClose = { open = false })
            }
        }
    }
}

@Composable
private fun PhoneProbePage(onClose: () -> Unit) {
    val context = LocalContext.current
    val speechIn: SpeechInput = koinInject()
    val speechOut: SpeechOutput = koinInject()
    val engine: TranslationEngine = koinInject()
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    ToolLayout(
        bottom = {
            BottomActionRow {
                BottomAction(
                    label = "Close",
                    onClick = onClose,
                    compact = true,
                    style = BottomActionStyle.Outlined,
                    modifier = Modifier.weight(1f),
                )
                BottomAction(
                    label = if (running) "Running…" else "Run the probe",
                    onClick = {
                        if (!running) {
                            running = true
                            results = emptyList()
                            scope.launch {
                                results = probe(context, speechIn, speechOut, engine) { results = results + it }
                                running = false
                            }
                        }
                    },
                    compact = true,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        Text(
            text = "Phone probe",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth().semantics { heading() },
        )
        Text(
            text =
                "${Build.MANUFACTURER} ${Build.MODEL}, " +
                    "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            style = MaterialTheme.typography.bodyMedium,
        )
        results.forEach { (label, value) ->
            if (value.isEmpty()) SectionHeader(text = label) else ValueRow(label = label, value = value)
        }
    }
}

/** Runs every check in order, reporting each line as it comes; a header is a line with an empty value. */
private suspend fun probe(
    context: Context,
    speechIn: SpeechInput,
    speechOut: SpeechOutput,
    engine: TranslationEngine,
    report: (Pair<String, String>) -> Unit,
): List<Pair<String, String>> {
    val lines = mutableListOf<Pair<String, String>>()
    val add = { line: Pair<String, String> ->
        lines += line
        report(line)
    }
    add("Speech (research R1, R2)" to "")
    for (tag in SPEECH_TAGS) {
        add("$tag recognition" to speechIn.support(tag).name)
        add("$tag voice" to speechOut.voice(tag).name)
    }
    add("Voices by engine (research R2)" to "")
    voiceLines(context).forEach(add)
    add("Engine (research R3)" to "")
    add("Processor (ABI)" to Build.SUPPORTED_ABIS.joinToString())
    val folder = File(context.filesDir, PACKS)
    val packs = listOf(PACK_PT, PACK_ES).filter { File(folder, "${it.value}/pack.json").isFile }
    add("Packs found" to packs.joinToString { it.value }.ifEmpty { "none, see the page's KDoc" })
    val opening = SystemClock.elapsedRealtime()
    val opened = engine.open(packs.associateWith { File(folder, it.value).path })
    add("Open" to "$opened in ${SystemClock.elapsedRealtime() - opening} ms")
    add("Memory after opening" to memory())
    if (opened == EngineOpen.Ready && PACK_PT in packs) {
        timed(add, "pt→en") { engine.translate(SENTENCE, Route.Direct(PT, LanguageCode.ENGLISH, PACK_PT)) }
        add("Memory with pt→en" to memory())
        timed(add, "pt→en again") { engine.translate(SENTENCE, Route.Direct(PT, LanguageCode.ENGLISH, PACK_PT)) }
        if (PACK_ES in packs) {
            timed(add, "pt→es through English") {
                engine.translate(SENTENCE, Route.ThroughEnglish(PT, ES, PACK_PT, PACK_ES))
            }
            add("Memory with pt→en and en→es" to memory())
        }
    }
    engine.close()
    add("Memory after closing" to memory())
    return lines
}

/** The app's proportional memory (PSS) and the native heap in use, in MB. */
private fun memory(): String =
    "PSS ${Debug.getPss() / KILO} MB, native heap ${Debug.getNativeHeapAllocatedSize() / (KILO * KILO)} MB"

private suspend fun timed(
    add: (Pair<String, String>) -> Unit,
    label: String,
    translate: suspend () -> String,
) {
    val start = SystemClock.elapsedRealtime()
    val text =
        try {
            translate()
        } catch (e: IllegalStateException) {
            "failed: ${e.message}"
        }
    add("$label, ${SystemClock.elapsedRealtime() - start} ms" to text)
}

private const val PACKS = "engine-test-packs"
private const val KILO = 1024L
private val PT = LanguageCode("pt")
private val ES = LanguageCode("es")
private val PACK_PT = PackId("translate_pt")
private val PACK_ES = PackId("translate_es")

/** The speech tags the Translator asks for (research R9): Spain's and Latin America's Spanish both. */
private val SPEECH_TAGS = listOf("en-US", "pt-BR", "es-ES", "es-US")

/** quickstart §2's 30-word test sentence, the same as BergamotEngineTest's. */
private const val SENTENCE =
    "Bom dia! Onde fica a estação de trem mais próxima? Preciso chegar ao centro antes do meio-dia, " +
        "porque tenho uma reunião importante e não conheço bem esta cidade. Muito obrigado!"
