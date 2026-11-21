package tech.onova.offlinetranslator.core.speech

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.SpeechEnd
import tech.onova.offlinetranslator.core.domain.ports.SpeechOutput
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport
import tech.onova.offlinetranslator.core.domain.speech.SpeechTags
import java.io.File
import java.util.concurrent.Executors

/**
 * The app's own voices (spec 082 US2): the language's Piper voice, sentence by sentence, so the first is heard while
 * the next is made (research R3: about 0.3 s of work per second of speech). [voiceFolder] is a language's voice pack's
 * folder and [espeakFolder] espeak-ng's data pack's, each null until it's in.
 */
public class AppSpeechOutput(
    private val voiceFolder: (LanguageCode) -> String?,
    private val espeakFolder: () -> String?,
) : SpeechOutput {
    private val dispatcher = Executors.newSingleThreadExecutor { Thread(it, "piper") }.asCoroutineDispatcher()
    private var voice: SherpaVoice? = null
    private var voiceFrom: String? = null

    @Volatile private var stopped = false

    @Volatile private var track: AudioTrack? = null

    override suspend fun voice(speechTag: String): VoiceSupport =
        if (folders(speechTag) != null) VoiceSupport.READY else VoiceSupport.NO_OFFLINE_VOICE

    override suspend fun speak(
        text: String,
        speechTag: String,
    ): SpeechEnd {
        val (folder, espeak) = folders(speechTag) ?: return SpeechEnd.FAILED
        stopped = false
        return withContext(dispatcher) {
            runCatching {
                val piper = voiceFrom(folder, espeak)
                sentences(text).forEach { sentence ->
                    if (stopped) return@runCatching
                    play(piper.speak(sentence))
                }
            }.fold(onSuccess = { SpeechEnd.DONE }, onFailure = { SpeechEnd.FAILED })
        }
    }

    override fun stop() {
        stopped = true
        track?.run {
            pause()
            flush()
        }
    }

    /** The app has no settings page for its voices: they're managed on the Manage languages page. */
    override fun openVoiceSettings(): Unit = Unit

    private fun folders(speechTag: String): Pair<String, String>? {
        val folder = SpeechTags.languageOf(speechTag)?.let(voiceFolder)
        val espeak = espeakFolder()
        return if (folder != null && espeak != null) folder to File(espeak, "espeak-ng-data").path else null
    }

    private fun voiceFrom(
        folder: String,
        espeak: String,
    ): SherpaVoice {
        val current = voice
        if (current != null && voiceFrom == folder) return current
        current?.release()
        return SherpaVoice.piper(File(folder), File(espeak)).also {
            voice = it
            voiceFrom = folder
        }
    }

    /** Plays [audio] to its end, or until [stop]. */
    private fun play(audio: VoiceAudio) {
        if (audio.samples.isEmpty() || stopped) return
        val player =
            AudioTrack
                .Builder()
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                ).setAudioFormat(
                    AudioFormat
                        .Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(audio.sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                ).setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(audio.samples.size * Float.SIZE_BYTES)
                .build()
        track = player
        try {
            player.play()
            player.write(audio.samples, 0, audio.samples.size, AudioTrack.WRITE_BLOCKING)
            while (!stopped && player.playbackHeadPosition < audio.samples.size) Thread.sleep(POLL_MILLIS)
        } finally {
            track = null
            player.release()
        }
    }

    private companion object {
        const val POLL_MILLIS = 40L
        val SENTENCE_END = Regex("""(?<=[.!?。！？؟])\s+""")

        fun sentences(text: String): List<String> =
            text.split(SENTENCE_END).map { it.trim() }.filter { it.isNotEmpty() }
    }
}
