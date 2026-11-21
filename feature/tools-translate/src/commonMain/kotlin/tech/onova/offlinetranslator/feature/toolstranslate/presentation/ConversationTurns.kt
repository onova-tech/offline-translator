package tech.onova.offlinetranslator.feature.toolstranslate.presentation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.SpeechEnd
import tech.onova.offlinetranslator.core.domain.ports.SpeechEvent
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationMachine
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationPhase
import tech.onova.offlinetranslator.feature.toolstranslate.domain.EngineSession
import tech.onova.offlinetranslator.feature.toolstranslate.domain.PauseReason
import tech.onova.offlinetranslator.feature.toolstranslate.domain.Routes
import tech.onova.offlinetranslator.feature.toolstranslate.domain.Side
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SideSupport

/**
 * What a conversation's phases start and stop (spec 012 research R8): listening for the half whose turn it is,
 * translating the turn, reading it aloud in the other half's language. Every result goes back through [dispatch], so
 * [ConversationMachine] alone decides what happens next.
 */
internal class ConversationTurns(
    private val scope: CoroutineScope,
    private val speech: ConversationSpeech,
    private val session: EngineSession,
    private val dispatch: (ConversationEvent) -> Unit,
    private val state: () -> ConversationUi,
    private val tagOf: (LanguageCode) -> String,
) {
    private var listening: Job? = null
    private var silence: Job? = null
    private var speaking: Job? = null

    /** What the phone can do for [language]: asked, never guessed. */
    suspend fun supportOf(language: LanguageCode): SideSupport {
        val tag = tagOf(language)
        return SideSupport(speech.input.support(tag), speech.output.voice(tag))
    }

    /** The phase moved from [old] to [new]: stop what [old] did, and start what [new] needs. */
    fun follow(
        old: ConversationPhase,
        new: ConversationPhase,
    ) {
        if (old is ConversationPhase.Listening && new !is ConversationPhase.Listening) stopListening()
        if (old is ConversationPhase.Speaking && new !is ConversationPhase.Speaking) stopSpeaking()
        when (new) {
            is ConversationPhase.Listening -> if (old !is ConversationPhase.Listening) listen(new.side)
            is ConversationPhase.Translating -> translate(new.turnId)
            is ConversationPhase.Speaking -> speak(new.turnId)
            ConversationPhase.Idle, is ConversationPhase.Paused -> Unit
        }
    }

    fun stopAll() {
        stopListening()
        stopSpeaking()
    }

    private fun listen(side: Side) {
        val tag = tagOf(state().conversation.languageOf(side))
        listening =
            scope.launch {
                speech.input.listen(tag).collect { event ->
                    when (event) {
                        is SpeechEvent.Partial -> {
                            dispatch(ConversationEvent.Heard(event.text))
                            if (event.text.isNotBlank()) endAfterSilence(side)
                        }

                        is SpeechEvent.Final -> {
                            dispatch(ConversationEvent.HeardFinal(event.text))
                        }

                        is SpeechEvent.Failed -> {
                            dispatch(ConversationEvent.ListeningFailed(event.reason))
                        }
                    }
                }
            }
    }

    /** A pause after something was heard ends the turn, as a second tap does (FR-023). */
    private fun endAfterSilence(side: Side) {
        silence?.cancel()
        silence =
            scope.launch {
                delay(ConversationMachine.SILENCE_MILLIS)
                dispatch(ConversationEvent.TapMicrophone(side))
            }
    }

    private fun translate(turnId: Long) {
        val turn = state().conversation.turns.firstOrNull { it.id == turnId } ?: return
        val route = Routes.between(turn.from, turn.to)
        scope.launch {
            val readiness =
                session.state
                    .first {
                        it.readiness(
                            route,
                        ) != EngineSession.Readiness.OPENING
                    }.readiness(route)
            val event =
                if (readiness == EngineSession.Readiness.UNAVAILABLE) {
                    ConversationEvent.TranslationFailed(turnId)
                } else {
                    try {
                        val translation = session.translate(turn.heard, route)
                        ConversationEvent.Translated(turnId, translation, speak = mayRead(turn.to))
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (ignored: IllegalStateException) {
                        ConversationEvent.TranslationFailed(turnId)
                    }
                }
            dispatch(event)
        }
    }

    /** Read aloud is on, TalkBack isn't (it announces instead), and [language] has an offline voice. */
    private fun mayRead(language: LanguageCode): Boolean {
        val ui = state()
        val side = if (language == ui.conversation.near) Side.NEAR else Side.FAR
        return ui.readAloud && !ui.screenReader && ui.supportOf(side).voice == VoiceSupport.READY
    }

    private fun speak(turnId: Long) {
        val turn = state().conversation.turns.firstOrNull { it.id == turnId }
        val text = turn?.translation
        if (turn == null || text == null) {
            dispatch(ConversationEvent.SpeechDone(turnId))
            return
        }
        speaking =
            scope.launch {
                val end = speech.output.speak(text, tagOf(turn.to))
                speaking = null
                // A call or an alarm pauses the conversation until a tap, rather than going on by itself.
                if (end == SpeechEnd.INTERRUPTED) {
                    dispatch(ConversationEvent.Interrupted(PauseReason.CALL_OR_ALARM))
                } else {
                    dispatch(ConversationEvent.SpeechDone(turnId))
                }
            }
    }

    private fun stopListening() {
        silence?.cancel()
        listening?.cancel()
        listening = null
    }

    private fun stopSpeaking() {
        if (speaking?.isActive == true) {
            speech.output.stop()
            speaking?.cancel()
        }
        speaking = null
    }
}
