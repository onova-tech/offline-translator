package tech.onova.offlinetranslator.core.platform.speech

import android.annotation.SuppressLint
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.SpeechFailure
import java.util.Locale
import java.util.MissingResourceException

/** The phone's answers in the port's words (spec 012 research R1 and R2). Pure, so it is tested alone. */
internal object SpeechMapping {
    /** What an engine says about one of its voices: the parts the choice reads. */
    data class VoiceFacts(
        val name: String,
        val locale: Locale,
        val networkRequired: Boolean,
        val features: Set<String>,
        val quality: Int,
    )

    /**
     * The voice that reads [speechTag] aloud: offline only (it needs no network, offers no network synthesis and is on
     * the phone), of the same language, its region first, then the best quality. Languages and regions compare by
     * their three-letter codes, since Samsung's engine names its voices' locales `por_BRA_f00` (research R2,
     * 2026-11-18).
     */
    fun <V> offlineVoice(
        speechTag: String,
        voices: Collection<V>,
        facts: (V) -> VoiceFacts,
    ): V? {
        val wanted = Locale.forLanguageTag(speechTag)
        val language = wanted.iso3Language() ?: return null
        val ofLanguage =
            voices.map { it to facts(it) }.filter { (_, f) -> f.isOffline() && f.locale.iso3Language() == language }
        val country = wanted.iso3Country()
        val ofRegion = ofLanguage.filter { (_, f) -> country != null && f.locale.iso3Country() == country }
        return (ofRegion.ifEmpty { ofLanguage }).maxByOrNull { (_, f) -> f.quality }?.first
    }

    /** The network-synthesis key is deprecated since Android 9, but an older engine may still offer it. */
    @Suppress("DEPRECATION")
    private fun VoiceFacts.isOffline(): Boolean =
        !networkRequired &&
            TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS !in features &&
            TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in features

    /** The three-letter language, or null when there is none or it is unknown. */
    private fun Locale.iso3Language(): String? =
        when (language.length) {
            0 -> null
            THREE -> language.lowercase()
            else -> iso3 { isO3Language.lowercase() }
        }

    /**
     * The three-letter (or three-digit) region, or null when there is none or it is unknown. A region already in three
     * letters is kept as it is: Android's `isO3Country` does so, the JDK's throws.
     */
    private fun Locale.iso3Country(): String? =
        when (country.length) {
            0 -> null
            THREE -> country.uppercase()
            else -> iso3 { isO3Country.uppercase() }
        }

    private inline fun iso3(code: () -> String): String? =
        try {
            code().ifEmpty { null }
        } catch (ignored: MissingResourceException) {
            null
        }

    private const val THREE = 3

    /**
     * One language in the recognizer's lists: installed is ready, pending is on its way, and only supported is
     * something the phone's own speech service can get. Tags compare without case, with `_` read as `-`.
     */
    fun support(
        speechTag: String,
        installed: List<String>,
        pending: List<String>,
        supported: List<String>,
    ): RecognitionSupport {
        val tag = normalize(speechTag)
        return when {
            installed.any { normalize(it) == tag } -> RecognitionSupport.READY
            pending.any { normalize(it) == tag } -> RecognitionSupport.DOWNLOADING
            supported.any { normalize(it) == tag } -> RecognitionSupport.CAN_DOWNLOAD
            else -> RecognitionSupport.UNSUPPORTED
        }
    }

    /**
     * A recognizer error code. The language errors and "too many requests" arrived with Android 12; on older phones
     * they never come, so naming them is harmless (they are plain numbers).
     */
    @SuppressLint("InlinedApi")
    fun failure(error: Int): SpeechFailure =
        when (error) {
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            -> SpeechFailure.NOTHING_HEARD

            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
            -> SpeechFailure.LANGUAGE_MISSING

            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_AUDIO,
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS,
            -> SpeechFailure.BUSY

            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechFailure.NO_PERMISSION

            else -> SpeechFailure.OTHER
        }

    private fun normalize(tag: String) = tag.trim().replace('_', '-').lowercase()
}
