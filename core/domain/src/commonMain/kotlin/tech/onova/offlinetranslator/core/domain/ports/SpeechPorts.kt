package tech.onova.offlinetranslator.core.domain.ports

import kotlinx.coroutines.flow.Flow

/** What the phone's own recognizer can do for one language, on the device only (spec 012 research R1). */
public enum class RecognitionSupport {
    /** Installed and ready. */
    READY,

    /** The phone's own speech service can download it, with its own notice. */
    CAN_DOWNLOAD,

    /** The phone's own speech service is getting it now; it's ready by itself when done. */
    DOWNLOADING,

    /**
     * Asked for, and never installed: the phone's speech service took the request and still lists it as to download
     * (the owner's phone, 2026-11-20: a service last updated in 2024 downloaded Japanese and couldn't use it). The
     * phone never says so; the Translator reads it from a request that never lands.
     */
    NOT_INSTALLED,

    /** The recognizer doesn't know this language. */
    UNSUPPORTED,

    /** The phone has no on-device recognizer: the microphone says so when tapped (spec 012 story 4, scenario 5). */
    NO_RECOGNIZER,

    /** The system can't recognize speech on the device at all (Android older than 12): no voice controls show. */
    SYSTEM_TOO_OLD,

    /** The phone can't tell before trying (Android 12 and 12L). */
    UNKNOWN,
}

/** Whether the phone can read a language aloud without the network (spec 012 research R2). */
public enum class VoiceSupport {
    READY,

    /** An engine exists but has no offline voice for the language: the person can add one in the phone's settings. */
    NO_OFFLINE_VOICE,

    /** No text-to-speech engine at all. */
    NO_ENGINE,
}

/** Why listening stopped without a result. */
public enum class SpeechFailure {
    NOTHING_HEARD,

    /** The language isn't installed on the device after all. */
    LANGUAGE_MISSING,

    /** Another app is using the recognizer or the microphone (a call). */
    BUSY,

    NO_PERMISSION,
    OTHER,
}

/** One event while listening. */
public sealed interface SpeechEvent {
    /** What has been heard so far; it may still change. */
    public data class Partial(
        val text: String,
    ) : SpeechEvent

    /** The sentence as heard; listening ends after it. */
    public data class Final(
        val text: String,
    ) : SpeechEvent

    public data class Failed(
        val reason: SpeechFailure,
    ) : SpeechEvent
}

/**
 * The phone's own speech recognizer, on the device only: audio is never stored or sent (spec 012 FR-021, FR-022).
 * Languages are BCP-47 tags with a region, e.g. `pt-BR` (research R9).
 */
public interface SpeechInput {
    /** What the phone can do for [speechTag]; never opens the microphone. */
    public suspend fun support(speechTag: String): RecognitionSupport

    /** Asks the phone's own speech service to get [speechTag]; false when this phone can't be asked (Android < 13). */
    public fun requestLanguage(speechTag: String): Boolean

    /** Whether the phone's speech service has a page in the phone's app store, where it can be updated. */
    public fun canUpdateService(): Boolean

    /** Opens that page: an out-of-date service may not install a newer language (the owner's phone, 2026-11-20). */
    public fun openServiceUpdate()

    /** Listens while collected and ends after the final result, a failure or cancellation. */
    public fun listen(speechTag: String): Flow<SpeechEvent>
}

/** How reading a text aloud ended. */
public enum class SpeechEnd {
    /** Said to the end, or stopped by the app. */
    DONE,

    /** A call or an alarm took the sound: it isn't resumed by itself (spec 012 edge case). */
    INTERRUPTED,

    /** The voice couldn't speak. */
    FAILED,
}

/** The phone's own text-to-speech, with offline voices only (spec 012 FR-024). */
public interface SpeechOutput {
    /** Whether an offline voice exists for [speechTag]. */
    public suspend fun voice(speechTag: String): VoiceSupport

    /** Speaks [text] and returns when it ends, is stopped or is interrupted, saying which. */
    public suspend fun speak(
        text: String,
        speechTag: String,
    ): SpeechEnd

    /** Stops speaking at once. */
    public fun stop()

    /** Opens where the phone's voice engine downloads voices: its own list, or the text-to-speech settings. */
    public fun openVoiceSettings()
}
