package tech.onova.offlinetranslator.core.speech

import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import java.io.File

/**
 * Whisper Base through sherpa-onnx (spec 082 research R1, R2): the int8 encoder and decoder and the tokens, read from
 * [folder]. It writes down a whole recording once it ends; the samples stay in memory (spec FR-005). Not thread-safe:
 * one caller at a time.
 */
public class SherpaRecognizer(
    folder: File,
    threads: Int = DEFAULT_THREADS,
) {
    private val recognizer: OfflineRecognizer
    private var config: OfflineRecognizerConfig
    private var language = ""

    init {
        val model =
            OfflineModelConfig(
                whisper =
                    OfflineWhisperModelConfig(
                        encoder = File(folder, "base-encoder.int8.onnx").path,
                        decoder = File(folder, "base-decoder.int8.onnx").path,
                        // Set before each recording through the config, so one model serves every language.
                        language = "",
                        task = "transcribe",
                    ),
                tokens = File(folder, "base-tokens.txt").path,
                numThreads = threads,
                modelType = "whisper",
            )
        config =
            OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = WHISPER_RATE, featureDim = FEATURES),
                modelConfig = model,
            )
        recognizer = OfflineRecognizer(null, config)
    }

    /** What [samples] (mono, -1 to 1, at [sampleRate]) say in [language] (a Whisper code such as "pt", "ar", "ja"). */
    public fun transcribe(
        samples: FloatArray,
        sampleRate: Int,
        language: String,
    ): String {
        // The native side aborts the process on an empty recording or a rate of 0, which no catch can stop.
        if (samples.isEmpty() || sampleRate <= 0) return ""
        if (language != this.language) {
            val whisper = config.modelConfig.whisper
            config =
                OfflineRecognizerConfig(
                    featConfig = config.featConfig,
                    modelConfig =
                        OfflineModelConfig(
                            whisper =
                                OfflineWhisperModelConfig(
                                    whisper.encoder,
                                    whisper.decoder,
                                    language,
                                    whisper.task,
                                ),
                            tokens = config.modelConfig.tokens,
                            numThreads = config.modelConfig.numThreads,
                            modelType = config.modelConfig.modelType,
                        ),
                )
            recognizer.setConfig(config)
            this.language = language
        }
        val stream = recognizer.createStream()
        try {
            stream.acceptWaveform(samples, sampleRate)
            recognizer.decode(stream)
            return recognizer.getResult(stream).text.trim()
        } finally {
            stream.release()
        }
    }

    public fun release() {
        recognizer.release()
    }

    private companion object {
        const val WHISPER_RATE = 16_000
        const val FEATURES = 80
        const val DEFAULT_THREADS = 2
    }
}
