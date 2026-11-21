package tech.onova.offlinetranslator.core.speech

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.SpeechEvent
import tech.onova.offlinetranslator.core.domain.ports.SpeechFailure
import tech.onova.offlinetranslator.core.domain.ports.SpeechInput
import tech.onova.offlinetranslator.core.domain.speech.SpeechTags
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.sqrt

/**
 * The app's own speech recognition (spec 082 US1): the microphone at 16 kHz into memory until the person stops speaking
 * (or taps stop, which drops it), then Whisper Base writes it down. The sound is never written anywhere (FR-005), and
 * Whisper stays loaded once used, on its own thread. [models] is the recognition pack's folder, null until it's in.
 */
public class AppSpeechInput(
    private val context: Context,
    private val models: () -> String?,
) : SpeechInput {
    private val dispatcher = Executors.newSingleThreadExecutor { Thread(it, "whisper") }.asCoroutineDispatcher()
    private var recognizer: SherpaRecognizer? = null
    private var loadedFrom: String? = null

    override suspend fun support(speechTag: String): RecognitionSupport =
        if (models() != null && SpeechTags.whisperCodeOf(speechTag) != null) {
            RecognitionSupport.READY
        } else {
            RecognitionSupport.UNSUPPORTED
        }

    override fun requestLanguage(speechTag: String): Boolean = false

    override fun canUpdateService(): Boolean = false

    override fun openServiceUpdate(): Unit = Unit

    override fun listen(speechTag: String): Flow<SpeechEvent> =
        flow {
            val code = SpeechTags.whisperCodeOf(speechTag)
            val folder = models()
            val granted =
                context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            val outcome =
                when {
                    code == null || folder == null -> SpeechEvent.Failed(SpeechFailure.LANGUAGE_MISSING)
                    !granted -> SpeechEvent.Failed(SpeechFailure.NO_PERMISSION)
                    else -> hear(folder, code)
                }
            emit(outcome)
        }

    private suspend fun hear(
        folder: String,
        code: String,
    ): SpeechEvent {
        val samples =
            withContext(Dispatchers.IO) { Microphone.record() } ?: return SpeechEvent.Failed(SpeechFailure.BUSY)
        if (samples.isEmpty()) return SpeechEvent.Failed(SpeechFailure.NOTHING_HEARD)
        val text = withContext(dispatcher) { recognizerFrom(folder).transcribe(samples, Microphone.RATE, code) }
        return if (text.isBlank()) SpeechEvent.Failed(SpeechFailure.NOTHING_HEARD) else SpeechEvent.Final(text)
    }

    private fun recognizerFrom(folder: String): SherpaRecognizer {
        val current = recognizer
        if (current != null && loadedFrom == folder) return current
        current?.release()
        return SherpaRecognizer(File(folder)).also {
            recognizer = it
            loadedFrom = folder
        }
    }
}

/** The microphone, read into memory only (spec 082 FR-005). */
internal object Microphone {
    const val RATE = 16_000
    private const val CHUNK = RATE / 10
    private const val SPEECH_LEVEL = 0.02
    private const val END_SILENCE_MILLIS = 1_500
    private const val NOTHING_MILLIS = 7_000
    private const val LONGEST_MILLIS = 30_000
    private const val MILLIS = 1_000

    /** The recording until 1.5 s of quiet after speech; empty when nothing was said; null when the microphone is busy. */
    suspend fun record(): FloatArray? {
        val size =
            maxOf(
                AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT),
                CHUNK * 4,
            )
        val audio =
            try {
                AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_FLOAT,
                    size,
                )
            } catch (refused: SecurityException) {
                return null
            }
        if (audio.state != AudioRecord.STATE_INITIALIZED) {
            audio.release()
            return null
        }
        return try {
            audio.startRecording()
            read(audio)
        } finally {
            audio.stop()
            audio.release()
        }
    }

    private suspend fun read(audio: AudioRecord): FloatArray {
        val heard = ArrayList<FloatArray>()
        val chunk = FloatArray(CHUNK)
        var spoke = false
        var quiet = 0
        var total = 0
        while (true) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            val count = audio.read(chunk, 0, chunk.size, AudioRecord.READ_BLOCKING)
            if (count <= 0) break
            heard += chunk.copyOf(count)
            val millis = count * MILLIS / RATE
            total += millis
            if (level(chunk, count) > SPEECH_LEVEL) {
                spoke = true
                quiet = 0
            } else {
                quiet += millis
            }
            if ((spoke && quiet >= END_SILENCE_MILLIS) || total >= LONGEST_MILLIS) break
            if (!spoke && total >= NOTHING_MILLIS) return FloatArray(0)
        }
        return if (spoke) heard.flatten() else FloatArray(0)
    }

    private fun level(
        chunk: FloatArray,
        count: Int,
    ): Double {
        var sum = 0.0
        for (i in 0 until count) sum += chunk[i] * chunk[i]
        return sqrt(sum / count)
    }

    private fun List<FloatArray>.flatten(): FloatArray {
        val all = FloatArray(sumOf { it.size })
        var at = 0
        forEach {
            it.copyInto(all, at)
            at += it.size
        }
        return all
    }
}
