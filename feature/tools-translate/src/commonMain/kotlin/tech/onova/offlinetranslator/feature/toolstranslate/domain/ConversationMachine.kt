package tech.onova.offlinetranslator.feature.toolstranslate.domain

import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.SpeechFailure

/** The two halves of a conversation: the phone owner's, and the far one facing the other person. */
internal enum class Side {
    NEAR,
    FAR,
    ;

    val other: Side get() = if (this == NEAR) FAR else NEAR
}

/** One spoken turn: what was heard in [from], and its translation into [to] once ready (spec 012 data-model.md). */
internal data class Turn(
    val id: Long,
    val side: Side,
    val from: LanguageCode,
    val to: LanguageCode,
    val heard: String,
    val translation: String? = null,
    val edited: Boolean = false,
)

internal enum class PauseReason {
    /** A call or an alarm took the audio. */
    CALL_OR_ALARM,

    /** The tool's screen was hidden. */
    HIDDEN,
}

/** What went wrong, shown on the half it concerns. */
internal enum class ConversationProblem {
    NOTHING_HEARD,
    LANGUAGE_MISSING,
    MICROPHONE_BUSY,
    NO_PERMISSION,
    LISTENING_FAILED,
    TRANSLATION_FAILED,
}

internal data class SideMessage(
    val side: Side,
    val problem: ConversationProblem,
)

/** Where the conversation is (spec 012 data-model.md, "ConversationPhase"). */
internal sealed interface ConversationPhase {
    data object Idle : ConversationPhase

    data class Listening(
        val side: Side,
        val partial: String,
    ) : ConversationPhase

    data class Translating(
        val turnId: Long,
    ) : ConversationPhase

    data class Speaking(
        val turnId: Long,
    ) : ConversationPhase

    data class Paused(
        val reason: PauseReason,
    ) : ConversationPhase
}

internal sealed interface ConversationEvent {
    /** A half's microphone; with [discard], the square button beside it while it listens (owner, 2026-11-19). */
    data class TapMicrophone(
        val side: Side,
        val discard: Boolean = false,
    ) : ConversationEvent

    data class Heard(
        val partial: String,
    ) : ConversationEvent

    data class HeardFinal(
        val text: String,
    ) : ConversationEvent

    data class ListeningFailed(
        val reason: SpeechFailure,
    ) : ConversationEvent

    /**
     * [speak]: whether the phone reads it aloud now (off with TalkBack, without a voice, or when the person said so).
     */
    data class Translated(
        val turnId: Long,
        val text: String,
        val speak: Boolean,
    ) : ConversationEvent

    data class TranslationFailed(
        val turnId: Long,
    ) : ConversationEvent

    data class SpeechDone(
        val turnId: Long,
    ) : ConversationEvent

    data class Interrupted(
        val reason: PauseReason,
    ) : ConversationEvent

    /** A misheard turn, corrected by the person (story 5, scenario 4). */
    data class Edited(
        val turnId: Long,
        val text: String,
    ) : ConversationEvent

    data class LanguageChanged(
        val side: Side,
        val language: LanguageCode,
    ) : ConversationEvent

    data object Clear : ConversationEvent
}

/** A conversation: its two languages, every turn (Copy keeps them all) and its phase. Never stored (FR-027). */
internal data class ConversationState(
    val near: LanguageCode,
    val far: LanguageCode,
    val turns: List<Turn>,
    val phase: ConversationPhase,
    val message: SideMessage? = null,
    val nextId: Long = 1,
) {
    /** What the halves show: the latest turns only. */
    val shownTurns: List<Turn> get() = turns.takeLast(ConversationMachine.MAX_TURNS_SHOWN)

    fun languageOf(side: Side): LanguageCode = if (side == Side.NEAR) near else far

    companion object {
        fun start(
            near: LanguageCode,
            far: LanguageCode,
        ): ConversationState = ConversationState(near, far, turns = emptyList(), phase = ConversationPhase.Idle)
    }
}

/**
 * The conversation's turns as a state machine (spec 012 research R8). A microphone opens only from Idle, so nothing
 * listens while the phone speaks (FR-025); a pause, a call or a hidden screen stops everything until the next tap.
 */
internal object ConversationMachine {
    const val MAX_TURNS_SHOWN: Int = 200

    /** A turn ends after this long with nothing new heard (FR-023). */
    const val SILENCE_MILLIS: Long = 2_000

    fun reduce(
        state: ConversationState,
        event: ConversationEvent,
    ): ConversationState =
        when (event) {
            is ConversationEvent.TapMicrophone -> {
                tap(state, event)
            }

            is ConversationEvent.Heard -> {
                heard(state, event.partial)
            }

            is ConversationEvent.HeardFinal -> {
                finish(state, event.text)
            }

            is ConversationEvent.ListeningFailed -> {
                listeningFailed(state, event.reason)
            }

            is ConversationEvent.Translated -> {
                translated(state, event)
            }

            is ConversationEvent.TranslationFailed -> {
                translationFailed(state, event.turnId)
            }

            is ConversationEvent.SpeechDone -> {
                val done = state.phase == ConversationPhase.Speaking(event.turnId)
                if (done) state.copy(phase = ConversationPhase.Idle) else state
            }

            is ConversationEvent.Interrupted -> {
                interrupted(state, event.reason)
            }

            is ConversationEvent.Edited -> {
                edited(state, event)
            }

            is ConversationEvent.LanguageChanged -> {
                if (event.side == Side.NEAR) state.copy(near = event.language) else state.copy(far = event.language)
            }

            ConversationEvent.Clear -> {
                state.copy(
                    turns = emptyList(),
                    phase = ConversationPhase.Idle,
                    message = null,
                    nextId = 1,
                )
            }
        }

    private fun tap(
        state: ConversationState,
        event: ConversationEvent.TapMicrophone,
    ): ConversationState {
        val phase = state.phase
        val listensHere = phase is ConversationPhase.Listening && phase.side == event.side
        return when {
            // The square button: listening stops, and what was heard goes untranslated.
            event.discard -> {
                if (listensHere) state.copy(phase = ConversationPhase.Idle, message = null) else state
            }

            phase == ConversationPhase.Idle -> {
                state.copy(phase = ConversationPhase.Listening(event.side, ""), message = null)
            }

            phase is ConversationPhase.Listening -> {
                if (listensHere) finish(state, phase.partial) else state
            }

            phase is ConversationPhase.Paused -> {
                state.copy(phase = ConversationPhase.Idle, message = null)
            }

            else -> {
                state
            }
        }
    }

    private fun heard(
        state: ConversationState,
        partial: String,
    ): ConversationState {
        val phase = state.phase as? ConversationPhase.Listening ?: return state
        return state.copy(phase = phase.copy(partial = partial))
    }

    /** Ends listening with [text]: a new turn to translate, or "nothing heard" on that side. */
    private fun finish(
        state: ConversationState,
        text: String,
    ): ConversationState {
        val side = (state.phase as? ConversationPhase.Listening)?.side ?: return state
        val heard = text.trim()
        if (heard.isEmpty()) return idleWith(state, SideMessage(side, ConversationProblem.NOTHING_HEARD))
        val turn = Turn(state.nextId, side, state.languageOf(side), state.languageOf(side.other), heard)
        return state.copy(
            turns = state.turns + turn,
            phase = ConversationPhase.Translating(turn.id),
            nextId = state.nextId + 1,
        )
    }

    private fun listeningFailed(
        state: ConversationState,
        reason: SpeechFailure,
    ): ConversationState {
        val side = (state.phase as? ConversationPhase.Listening)?.side ?: return state
        val problem =
            when (reason) {
                SpeechFailure.NOTHING_HEARD -> ConversationProblem.NOTHING_HEARD
                SpeechFailure.LANGUAGE_MISSING -> ConversationProblem.LANGUAGE_MISSING
                SpeechFailure.BUSY -> ConversationProblem.MICROPHONE_BUSY
                SpeechFailure.NO_PERMISSION -> ConversationProblem.NO_PERMISSION
                SpeechFailure.OTHER -> ConversationProblem.LISTENING_FAILED
            }
        return idleWith(state, SideMessage(side, problem))
    }

    private fun translated(
        state: ConversationState,
        event: ConversationEvent.Translated,
    ): ConversationState {
        val turns = state.turns.map { if (it.id == event.turnId) it.copy(translation = event.text) else it }
        val phase =
            when {
                state.phase != ConversationPhase.Translating(event.turnId) -> state.phase
                event.speak -> ConversationPhase.Speaking(event.turnId)
                else -> ConversationPhase.Idle
            }
        return state.copy(turns = turns, phase = phase)
    }

    private fun translationFailed(
        state: ConversationState,
        turnId: Long,
    ): ConversationState {
        if (state.phase != ConversationPhase.Translating(turnId)) return state
        val side = state.turns.firstOrNull { it.id == turnId }?.side ?: return state
        return idleWith(state, SideMessage(side.other, ConversationProblem.TRANSLATION_FAILED))
    }

    private fun interrupted(
        state: ConversationState,
        reason: PauseReason,
    ): ConversationState =
        if (state.phase == ConversationPhase.Idle) state else state.copy(phase = ConversationPhase.Paused(reason))

    private fun edited(
        state: ConversationState,
        event: ConversationEvent.Edited,
    ): ConversationState {
        val text = event.text.trim()
        if (state.phase != ConversationPhase.Idle || text.isEmpty()) return state
        if (state.turns.none { it.id == event.turnId }) return state
        val turns =
            state.turns.map {
                if (it.id == event.turnId) it.copy(heard = text, translation = null, edited = true) else it
            }
        return state.copy(turns = turns, phase = ConversationPhase.Translating(event.turnId), message = null)
    }

    private fun idleWith(
        state: ConversationState,
        message: SideMessage,
    ): ConversationState = state.copy(phase = ConversationPhase.Idle, message = message)
}
