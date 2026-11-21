package tech.onova.offlinetranslator.core.platform.speech

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tech.onova.offlinetranslator.core.domain.ports.SpeechEnd
import tech.onova.offlinetranslator.core.domain.ports.SpeechOutput
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

/**
 * The phone's own text-to-speech with offline voices only (spec 012 research R2): a voice that needs the network, or
 * offers network synthesis, is never used, so the text never leaves the phone. Speaking takes transient audio focus
 * (others duck) and stops when a call takes it.
 */
public class AndroidSpeechOutput(
    context: Context,
) : SpeechOutput {
    private val context = context.applicationContext
    private val audioManager = this.context.getSystemService(AudioManager::class.java)
    private val engineLock = Mutex()
    private var engine: TextToSpeech? = null
    private val utterances = ConcurrentHashMap<String, Continuation<SpeechEnd>>()

    /** A call or an alarm took the sound while speaking: the sentence ends as interrupted. */
    @Volatile private var focusLost = false
    private val nextId = AtomicLong()

    private val attributes =
        AudioAttributes
            .Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

    private val focus: AudioFocusRequest =
        AudioFocusRequest
            .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    focusLost = true
                    stop()
                }
            }.build()

    override suspend fun voice(speechTag: String): VoiceSupport {
        val tts = engine() ?: return VoiceSupport.NO_ENGINE
        return if (offlineVoice(tts, speechTag) != null) VoiceSupport.READY else VoiceSupport.NO_OFFLINE_VOICE
    }

    override suspend fun speak(
        text: String,
        speechTag: String,
    ): SpeechEnd {
        val tts = engine()
        val voice = tts?.let { offlineVoice(it, speechTag) }
        focusLost = false
        return when {
            tts == null || voice == null -> {
                SpeechEnd.FAILED
            }

            // During a call the sound can't be had: the sentence isn't said.
            audioManager?.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                SpeechEnd.INTERRUPTED
            }

            else -> {
                try {
                    say(tts, voice, text)
                } finally {
                    audioManager?.abandonAudioFocusRequest(focus)
                }
            }
        }
    }

    /** Says [text] with [voice], holding the sound; returns when it ends. */
    private suspend fun say(
        tts: TextToSpeech,
        voice: Voice,
        text: String,
    ): SpeechEnd {
        val id = "offlinetranslator-${nextId.incrementAndGet()}"
        return suspendCancellableCoroutine { done ->
            utterances[id] = done
            done.invokeOnCancellation {
                utterances.remove(id)
                tts.stop()
            }
            tts.voice = voice
            tts.setAudioAttributes(attributes)
            val queued = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.ERROR
            if (!queued) finish(id, SpeechEnd.FAILED)
        }
    }

    override fun stop() {
        engine?.stop()
    }

    /**
     * The voice engine's own list of voices to download (owner's phone pass, 2026-11-20: Samsung's and Google's engines
     * both have one), one tap from the notice; the phone's text-to-speech settings when the engine has none. An app
     * can't download a voice itself: the engine does, at the person's tap there.
     */
    override fun openVoiceSettings() {
        val engine = Settings.Secure.getString(context.contentResolver, Settings.Secure.TTS_DEFAULT_SYNTH)
        val downloads =
            Intent(
                TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA,
            ).apply { if (!engine.isNullOrBlank()) setPackage(engine) }
        if (!open(downloads)) open(Intent(TTS_SETTINGS))
    }

    /** Whether [intent] opened; a phone without that page opens nothing. */
    private fun open(intent: Intent): Boolean =
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (ignored: ActivityNotFoundException) {
            false
        }

    /** The phone's default engine, started once; null when it has none or it fails to start. */
    private suspend fun engine(): TextToSpeech? =
        engineLock.withLock {
            engine?.let { return it }
            val started = CompletableDeferred<Boolean>()
            val tts =
                withContext(Dispatchers.Main) {
                    TextToSpeech(context) { status -> started.complete(status == TextToSpeech.SUCCESS) }
                }
            if (!started.await()) {
                tts.shutdown()
                return null
            }
            tts.setOnUtteranceProgressListener(progress)
            engine = tts
            tts
        }

    /** An offline voice for [speechTag]: its region first, then any of its language (research R2, R9). */
    private fun offlineVoice(
        tts: TextToSpeech,
        speechTag: String,
    ): Voice? =
        SpeechMapping.offlineVoice(speechTag, tts.voices.orEmpty()) {
            SpeechMapping.VoiceFacts(
                it.name,
                it.locale,
                it.isNetworkConnectionRequired,
                it.features.orEmpty(),
                it.quality,
            )
        }

    private fun finish(
        id: String?,
        end: SpeechEnd,
    ) {
        id?.let(utterances::remove)?.resume(end)
    }

    private val progress =
        object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) = finish(utteranceId, SpeechEnd.DONE)

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finish(utteranceId, SpeechEnd.FAILED)

            override fun onError(
                utteranceId: String?,
                errorCode: Int,
            ) = finish(utteranceId, SpeechEnd.FAILED)

            // Stopped by the person or a new sentence: said as far as it went. A call or an alarm: interrupted.
            override fun onStop(
                utteranceId: String?,
                interrupted: Boolean,
            ) = finish(utteranceId, if (focusLost) SpeechEnd.INTERRUPTED else SpeechEnd.DONE)
        }

    private companion object {
        const val TTS_SETTINGS = "com.android.settings.TTS_SETTINGS"
    }
}
