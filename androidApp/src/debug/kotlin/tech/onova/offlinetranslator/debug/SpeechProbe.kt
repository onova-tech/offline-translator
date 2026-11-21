package tech.onova.offlinetranslator.debug

import android.content.Context
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import tech.onova.offlinetranslator.core.designsystem.components.BottomAction
import tech.onova.offlinetranslator.core.designsystem.components.BottomActionRow
import tech.onova.offlinetranslator.core.designsystem.components.BottomActionStyle
import tech.onova.offlinetranslator.core.designsystem.components.SectionHeader
import tech.onova.offlinetranslator.core.designsystem.components.SettingsRow
import tech.onova.offlinetranslator.core.designsystem.components.ToolLayout
import tech.onova.offlinetranslator.core.designsystem.components.ValueRow
import tech.onova.offlinetranslator.core.speech.SherpaRecognizer
import tech.onova.offlinetranslator.core.speech.SherpaVoice
import java.io.File

/**
 * Spec 082 T003: a debug-only page that times the app's own speech on a real phone (research R2, R3). For each
 * language it makes a sentence with the app's voice (Kokoro or Piper), then writes it back with Whisper Base, so one
 * run measures both and shows whether the voice can be understood. Nothing is played aloud. English only: release
 * builds don't have it.
 *
 * The models come from `files/speech-test/`: `whisper/` (the int8 encoder and decoder and `base-tokens.txt`),
 * `kokoro/` (`kokoro-int8-multi-lang-v1_0` unpacked) and `piper-ar/` (`vits-piper-ar_JO-kareem-medium-int8` unpacked),
 * pushed with `adb push` to /data/local/tmp and copied with `run-as`, as the engine probe's packs.
 */
@Composable
internal fun SpeechProbeRow(modifier: Modifier = Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    SettingsRow(
        title = "Speech probe (debug)",
        supporting = "Whisper, Kokoro and Piper on this phone",
        onClick = { open = true },
        modifier = modifier,
    )
    if (open) {
        Dialog(onDismissRequest = { open = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                SpeechProbePage(onClose = { open = false })
            }
        }
    }
}

@Composable
private fun SpeechProbePage(onClose: () -> Unit) {
    val context = LocalContext.current
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
                                withContext(Dispatchers.Default) { speechProbe(context) { results = results + it } }
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
            text = "Speech probe",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth().semantics { heading() },
        )
        results.forEach { (label, value) ->
            if (value.isEmpty()) SectionHeader(text = label) else ValueRow(label = label, value = value)
        }
    }
}

/** One language of the probe: the app's voice that speaks it, and the sentence. */
private class ProbeCase(
    val whisper: String,
    val voice: String,
    val kokoroLanguage: String,
    val speaker: Int,
    val sentence: String,
)

private val CASES =
    listOf(
        ProbeCase("en", "kokoro", "", speaker = 3, sentence = "Good morning! Where is the nearest train station?"),
        ProbeCase(
            "pt",
            "kokoro",
            "pt-br",
            speaker = 42,
            sentence = "Bom dia! Onde fica a estação de trem mais próxima?",
        ),
        ProbeCase("ja", "kokoro", "ja", speaker = 37, sentence = "おはようございます。一番近い駅はどこですか。"),
        ProbeCase("ar", "piper-ar", "", speaker = 0, sentence = "صباح الخير! أين أقرب محطة قطار؟"),
    )

/** Runs every case in order, reporting each line as it comes; a header is a line with an empty value. */
private fun speechProbe(
    context: Context,
    report: (Pair<String, String>) -> Unit,
) {
    val root = File(context.filesDir, "speech-test")
    report("Models (research R2, R3)" to "")
    report(
        "Folders found" to (
            root
                .list()
                ?.sorted()
                ?.joinToString()
                .orEmpty()
                .ifEmpty { "none, see the page's KDoc" }
        ),
    )
    report("Memory at the start" to speechMemory())
    var start = SystemClock.elapsedRealtime()
    val whisper = SherpaRecognizer(File(root, "whisper"))
    report("Whisper Base loaded" to "${SystemClock.elapsedRealtime() - start} ms, ${speechMemory()}")
    try {
        for (case in CASES) {
            report("${case.whisper}: ${case.voice}" to "")
            start = SystemClock.elapsedRealtime()
            val folder = File(root, case.voice)
            val voice =
                if (case.voice == "kokoro") {
                    SherpaVoice.kokoro(folder, case.kokoroLanguage)
                } else {
                    SherpaVoice.piper(folder)
                }
            report("Voice loaded" to "${SystemClock.elapsedRealtime() - start} ms")
            start = SystemClock.elapsedRealtime()
            val audio = voice.speak(case.sentence, case.speaker)
            val made = SystemClock.elapsedRealtime() - start
            report("Voice made" to "%.1f s of speech in %d ms".format(audio.seconds, made))
            report("Memory with the voice" to speechMemory())
            voice.release()
            start = SystemClock.elapsedRealtime()
            val heard = whisper.transcribe(audio.samples, audio.sampleRate, case.whisper)
            report("Whisper wrote (${SystemClock.elapsedRealtime() - start} ms)" to heard.ifEmpty { "(nothing)" })
        }
    } catch (failure: Throwable) {
        report("Failed" to "${failure::class.simpleName}: ${failure.message}")
    } finally {
        whisper.release()
    }
    report("Memory at the end" to speechMemory())
}

/** The app's proportional memory (PSS) and the native heap in use, in MB. */
private fun speechMemory(): String =
    "PSS ${Debug.getPss() / SPEECH_KILO} MB, " +
        "native heap ${Debug.getNativeHeapAllocatedSize() / (SPEECH_KILO * SPEECH_KILO)} MB"

private const val SPEECH_KILO = 1024L
