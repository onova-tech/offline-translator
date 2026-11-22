package tech.onova.offlinetranslator.feature.toolstranslate.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.onova.offlinetranslator.core.domain.ports.AppVisibility
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.SpeechEvent
import tech.onova.offlinetranslator.core.domain.ports.SpeechFailure
import tech.onova.offlinetranslator.core.domain.ports.SpeechInput
import tech.onova.offlinetranslator.core.domain.ports.SpeechOutput
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport
import tech.onova.offlinetranslator.feature.toolstranslate.domain.AppSpeechParts
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationMachine
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SpeechDownloads

/** Why the phone can't listen or read aloud now, with what fixes it where something does. */
internal enum class SpeechProblem {
    NOTHING_HEARD,

    /** The phone's own speech service can get the language (on the Manage languages page). */
    CAN_DOWNLOAD,

    /** The phone's own speech service is getting it now. */
    DOWNLOADING,

    /** Asked for and never installed: its speech service may need an update (on the Manage languages page). */
    NOT_INSTALLED,

    /** The phone can't recognize the language offline; typing stays. */
    UNSUPPORTED,

    /** The phone has no on-device recognizer at all; typing stays. */
    NO_RECOGNIZER,

    /** It said it could, then didn't have it. */
    LANGUAGE_MISSING,

    /** Another app has the microphone, or a call. */
    BUSY,
    NO_PERMISSION,
    LISTENING_FAILED,

    /** No offline voice for the language: the voice app can download one (on the Manage languages page). */
    NO_VOICE,

    /** No text-to-speech at all. */
    NO_ENGINE,

    /** The phone can't, and the app's own recognition can, once downloaded (spec 082; on Manage languages). */
    APP_CAN_DOWNLOAD,

    /** The app's own recognition is downloading (spec 082). */
    APP_DOWNLOADING,

    /** No voice on the phone, and the app's own has one, once downloaded (spec 082; on Manage languages). */
    APP_VOICE_CAN_DOWNLOAD,

    /** The app's own voice is downloading (spec 082). */
    APP_VOICE_DOWNLOADING,
}

internal data class SpeechUi(
    /** For the text's language; below Android 12 the microphone isn't shown (FR-021). */
    val recognition: RecognitionSupport = RecognitionSupport.UNKNOWN,
    /** For the translation's language. */
    val voice: VoiceSupport = VoiceSupport.READY,
    val listening: Boolean = false,
    val speaking: Boolean = false,
    val problem: SpeechProblem? = null,
)

/**
 * Speak instead of typing, and hear the translation (spec 012 story 4), with the phone's own recognizer and voices
 * only. What is heard goes to [heard], for the text field; a turn ends after [ConversationMachine.SILENCE_MILLIS]
 * without new words, a second tap, or the recognizer's own end (FR-023). Nothing listens or speaks while the screen
 * is hidden (FR-022), and nothing is kept: audio is never stored.
 */
internal class SpeakAndHearViewModel(
    private val speechIn: SpeechInput,
    private val speechOut: SpeechOutput,
    private val visibility: AppVisibility,
    /** Shared with the Manage languages page, where a language is got (owner, 2026-11-20). */
    private val downloads: SpeechDownloads,
    /** The app's own speech packs (spec 082): when one is in or gone, what each language can do is asked again. */
    private val appSpeech: AppSpeechParts,
) : ViewModel() {
    private val state = MutableStateFlow(SpeechUi())
    val uiState: StateFlow<SpeechUi> = state.asStateFlow()

    private val heardWords = MutableSharedFlow<String>(extraBufferCapacity = HEARD_BUFFER)

    /** Each version of the sentence as it's heard: the screen puts it in the text field. */
    val heard: SharedFlow<String> = heardWords.asSharedFlow()

    private var sourceTag: String? = null
    private var targetTag: String? = null
    private var checking: Job? = null
    private var listening: Job? = null
    private var silence: Job? = null
    private var speaking: Job? = null

    private val support =
        SpeechRecheck(
            scope = viewModelScope,
            visibility = visibility,
            ask = {
                val source = sourceTag
                val target = targetTag
                if (source != null && target != null) {
                    val recognition = downloads.read(source, speechIn.support(source))
                    val voice = speechOut.voice(target)
                    // A language just found not installed says so by itself: the person was waiting for it.
                    val newlyRefused =
                        recognition == RecognitionSupport.NOT_INSTALLED &&
                            state.value.recognition != RecognitionSupport.NOT_INSTALLED
                    state.update { it.copy(recognition = recognition, voice = voice) }
                    if (newlyRefused) state.report(SpeechProblem.NOT_INSTALLED)
                }
            },
            downloading = { state.value.recognition == RecognitionSupport.DOWNLOADING },
            onItsWay = {
                state.value.recognition in
                    listOf(
                        RecognitionSupport.CAN_DOWNLOAD,
                        RecognitionSupport.DOWNLOADING,
                        RecognitionSupport.NOT_INSTALLED,
                    )
            },
        )

    init {
        viewModelScope.launch {
            var wasShown = true
            visibility.isVisible.collect { shown ->
                if (!shown) {
                    stopListening()
                    stopSpeaking()
                } else if (!wasShown) {
                    // Back from the phone's settings (a voice added, a language got): asked again, never guessed.
                    // A language the phone didn't install can be asked for again (back from the store, say).
                    downloads.forget()
                    if (state.value.problem == SpeechProblem.NOT_INSTALLED) state.report(null)
                    support.askNow()
                }
                wasShown = shown
            }
        }
        viewModelScope.launch {
            // A language asked for on the Manage languages page: asked about here too, until it's ready.
            downloads.requests.drop(1).collect {
                support.askNow()
                support.watch()
            }
        }
        viewModelScope.launch {
            // The app's speech packs in, gone or on their way (spec 082), the store's first answer among them.
            appSpeech.changes.collect { support.askNow() }
        }
    }

    /** The two languages' speech tags (research R9); what each one can do is asked of the phone, not guessed. */
    fun setLanguages(
        source: String,
        target: String,
    ) {
        if (source == sourceTag && target == targetTag) return
        sourceTag = source
        targetTag = target
        stopListening()
        state.update { it.copy(problem = null) }
        checking?.cancel()
        checking = viewModelScope.launch { support.askNow() }
    }

    /** Starts listening, or ends it; a language the phone can't recognize offline explains why instead. */
    fun tapMicrophone() {
        if (listening != null) {
            stopListening()
            return
        }
        val tag = sourceTag ?: return
        when (state.value.recognition) {
            RecognitionSupport.READY, RecognitionSupport.UNKNOWN -> {
                startListening(tag)
            }

            RecognitionSupport.CAN_DOWNLOAD -> {
                state.report(SpeechProblem.CAN_DOWNLOAD)
            }

            RecognitionSupport.DOWNLOADING -> {
                state.report(SpeechProblem.DOWNLOADING)
            }

            RecognitionSupport.NOT_INSTALLED -> {
                state.report(SpeechProblem.NOT_INSTALLED)
            }

            RecognitionSupport.UNSUPPORTED -> {
                state.report(SpeechProblem.UNSUPPORTED)
            }

            RecognitionSupport.NO_RECOGNIZER, RecognitionSupport.SYSTEM_TOO_OLD -> {
                state.report(SpeechProblem.NO_RECOGNIZER)
            }

            RecognitionSupport.APP_CAN_DOWNLOAD -> {
                state.report(SpeechProblem.APP_CAN_DOWNLOAD)
            }

            RecognitionSupport.APP_DOWNLOADING -> {
                state.report(SpeechProblem.APP_DOWNLOADING)
            }
        }
    }

    /** Reads [translation] in the target's offline voice, or stops it (Read aloud ↔ Stop). */
    fun readAloud(translation: String) {
        if (speaking != null) {
            stopSpeaking()
            return
        }
        val tag = targetTag ?: return
        when (state.value.voice) {
            VoiceSupport.NO_OFFLINE_VOICE -> {
                state.report(SpeechProblem.NO_VOICE)
            }

            VoiceSupport.NO_ENGINE -> {
                state.report(SpeechProblem.NO_ENGINE)
            }

            VoiceSupport.APP_CAN_DOWNLOAD -> {
                state.report(SpeechProblem.APP_VOICE_CAN_DOWNLOAD)
            }

            VoiceSupport.APP_DOWNLOADING -> {
                state.report(SpeechProblem.APP_VOICE_DOWNLOADING)
            }

            VoiceSupport.READY -> {
                state.update { it.copy(speaking = true, problem = null) }
                speaking =
                    viewModelScope.launch {
                        try {
                            speechOut.speak(translation, tag)
                        } finally {
                            if (speaking === currentCoroutineContext()[Job]) speaking = null
                            state.update { it.copy(speaking = false) }
                        }
                    }
            }
        }
    }

    fun dismiss() {
        state.report(null)
    }

    override fun onCleared() {
        stopSpeaking()
    }

    private fun startListening(tag: String) {
        state.update { it.copy(listening = true, problem = null) }
        listening =
            viewModelScope.launch {
                try {
                    speechIn.listen(tag).collect(::onEvent)
                } finally {
                    silence?.cancel()
                    if (listening === currentCoroutineContext()[Job]) listening = null
                    state.update { it.copy(listening = false) }
                }
            }
    }

    private fun onEvent(event: SpeechEvent) {
        when (event) {
            is SpeechEvent.Partial -> {
                if (event.text.isBlank()) return
                heardWords.tryEmit(event.text)
                silence?.cancel()
                silence =
                    viewModelScope.launch {
                        delay(ConversationMachine.SILENCE_MILLIS)
                        stopListening()
                    }
            }

            is SpeechEvent.Final -> {
                if (event.text.isNotBlank()) heardWords.tryEmit(event.text)
            }

            is SpeechEvent.Failed -> {
                state.report(problemOf(event.reason))
            }
        }
    }

    private fun stopListening() {
        silence?.cancel()
        listening?.cancel()
        listening = null
        state.update { it.copy(listening = false) }
    }

    private fun stopSpeaking() {
        if (speaking == null) return
        speechOut.stop()
        speaking?.cancel()
        speaking = null
        state.update { it.copy(speaking = false) }
    }

    private companion object {
        const val HEARD_BUFFER = 16
    }
}

private fun MutableStateFlow<SpeechUi>.report(problem: SpeechProblem?) = update { it.copy(problem = problem) }

private fun problemOf(reason: SpeechFailure): SpeechProblem =
    when (reason) {
        SpeechFailure.NOTHING_HEARD -> SpeechProblem.NOTHING_HEARD
        SpeechFailure.LANGUAGE_MISSING -> SpeechProblem.LANGUAGE_MISSING
        SpeechFailure.BUSY -> SpeechProblem.BUSY
        SpeechFailure.NO_PERMISSION -> SpeechProblem.NO_PERMISSION
        SpeechFailure.OTHER -> SpeechProblem.LISTENING_FAILED
    }
