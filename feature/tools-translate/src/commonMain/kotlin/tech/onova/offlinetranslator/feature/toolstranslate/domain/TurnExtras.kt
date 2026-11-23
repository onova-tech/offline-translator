package tech.onova.offlinetranslator.feature.toolstranslate.domain

/**
 * What spec 082's story 5 adds to [ConversationMachine]: a heard turn that waits for ^ ("Confirm before sending",
 * FR-016) and a translated turn read again from its speaker button (FR-017).
 */
internal object TurnExtras {
    fun reduce(
        state: ConversationState,
        event: ConversationEvent,
    ): ConversationState =
        when (event) {
            is ConversationEvent.ConfirmChanged -> {
                state.copy(confirm = event.on)
            }

            is ConversationEvent.SendTurn -> {
                val waiting = state.waiting
                if (waiting != null && waiting.side == event.side) {
                    state.copy(phase = ConversationPhase.Translating(waiting.id))
                } else {
                    state
                }
            }

            is ConversationEvent.Replay -> {
                val said = state.turns.firstOrNull { it.id == event.turnId }?.translation != null
                if (state.phase == ConversationPhase.Idle && said) {
                    state.copy(phase = ConversationPhase.Speaking(event.turnId), message = null)
                } else {
                    state
                }
            }

            ConversationEvent.StopSpeaking -> {
                if (state.phase is ConversationPhase.Speaking) state.copy(phase = ConversationPhase.Idle) else state
            }

            else -> {
                state
            }
        }

    /** [waiting] goes and the conversation is Idle again: the square, or a new language in its half. */
    fun drop(
        state: ConversationState,
        waiting: Turn,
    ): ConversationState = state.copy(turns = state.turns - waiting, phase = ConversationPhase.Idle, message = null)

    /**
     * A half's new language. A turn waiting in that half was heard in the old one and goes; one waiting in the other
     * half is translated into the new one when sent.
     */
    fun languageChanged(
        state: ConversationState,
        event: ConversationEvent.LanguageChanged,
    ): ConversationState {
        val changed =
            if (event.side == Side.NEAR) state.copy(near = event.language) else state.copy(far = event.language)
        val waiting = state.waiting ?: return changed
        return if (waiting.side == event.side) {
            drop(changed, waiting)
        } else {
            changed.copy(turns = changed.turns.map { if (it.id == waiting.id) it.copy(to = event.language) else it })
        }
    }
}
