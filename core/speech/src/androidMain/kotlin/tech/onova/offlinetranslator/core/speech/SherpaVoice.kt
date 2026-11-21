package tech.onova.offlinetranslator.core.speech

import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File

/** Speech made by the app: mono samples from -1 to 1 at [sampleRate]. */
public class VoiceAudio(
    public val samples: FloatArray,
    public val sampleRate: Int,
) {
    public val seconds: Double get() = samples.size.toDouble() / sampleRate
}

/**
 * A voice of the app's own (spec 082): Kokoro, one model for several languages, or one Piper voice for its language,
 * through sherpa-onnx (research R3, R4). Both read espeak-ng's data from a folder. Not thread-safe: one caller at a
 * time.
 */
public class SherpaVoice private constructor(
    private val tts: OfflineTts,
) {
    public val sampleRate: Int get() = tts.sampleRate()

    /** Speaks [text] with speaker [speaker] (Kokoro's voice number; 0 for a Piper voice), at [speed] (1 is normal). */
    public fun speak(
        text: String,
        speaker: Int = 0,
        speed: Float = 1f,
    ): VoiceAudio {
        val audio = tts.generate(text, speaker, speed)
        return VoiceAudio(audio.samples, audio.sampleRate)
    }

    public fun release() {
        tts.release()
    }

    public companion object {
        /**
         * Kokoro's multilingual int8 model, as unpacked from `kokoro-int8-multi-lang-v1_0.tar.bz2`, for [language]
         * (espeak-ng's name, such as "pt-br", "es", "ja"; empty for English and Chinese, which use their lexicons).
         */
        public fun kokoro(
            folder: File,
            language: String = "",
            threads: Int = DEFAULT_THREADS,
        ): SherpaVoice {
            val lexicons = listOf("lexicon-us-en.txt", "lexicon-zh.txt").map { File(folder, it) }.filter { it.exists() }
            val kokoro =
                OfflineTtsKokoroModelConfig(
                    model = File(folder, "model.int8.onnx").path,
                    voices = File(folder, "voices.bin").path,
                    tokens = File(folder, "tokens.txt").path,
                    dataDir = File(folder, "espeak-ng-data").path,
                    lexicon = lexicons.joinToString(",") { it.path },
                    dictDir = File(folder, "dict").path.takeIf { File(it).isDirectory }.orEmpty(),
                    lang = language,
                )
            return SherpaVoice(
                OfflineTts(
                    null,
                    OfflineTtsConfig(model = OfflineTtsModelConfig(kokoro = kokoro, numThreads = threads)),
                ),
            )
        }

        /**
         * One Piper voice, as unpacked from `vits-piper-<voice>-medium-int8.tar.bz2`, reading espeak-ng's data from
         * [espeakData] (its own pack in the app, spec 082; inside the voice's folder in the spike).
         */
        public fun piper(
            folder: File,
            espeakData: File = File(folder, "espeak-ng-data"),
            threads: Int = DEFAULT_THREADS,
        ): SherpaVoice {
            val model =
                folder.listFiles { file -> file.name.endsWith(".onnx") }?.single() ?: error("no model in $folder")
            val vits =
                OfflineTtsVitsModelConfig(
                    model = model.path,
                    tokens = File(folder, "tokens.txt").path,
                    dataDir = espeakData.path,
                )
            return SherpaVoice(
                OfflineTts(null, OfflineTtsConfig(model = OfflineTtsModelConfig(vits = vits, numThreads = threads))),
            )
        }

        private const val DEFAULT_THREADS = 2
    }
}
