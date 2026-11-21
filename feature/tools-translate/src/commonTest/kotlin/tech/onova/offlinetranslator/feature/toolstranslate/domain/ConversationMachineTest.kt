package tech.onova.offlinetranslator.feature.toolstranslate.domain

import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.SpeechFailure
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent.Clear
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent.Edited
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent.Heard
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent.HeardFinal
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent.Interrupted
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent.ListeningFailed
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent.SpeechDone
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent.TapMicrophone
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent.Translated
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent.TranslationFailed
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationPhase.Idle
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationPhase.Listening
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationPhase.Paused
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationPhase.Speaking
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationPhase.Translating
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Spec 012 story 5, research R8 and data-model.md "ConversationPhase": the turns of a conversation. */
class ConversationMachineTest {
    private val pt = LanguageCode("pt")
    private val es = LanguageCode("es")
    private val start = ConversationState.start(near = pt, far = es)

    private fun ConversationState.after(vararg events: ConversationEvent) =
        events.fold(this) { state, event -> ConversationMachine.reduce(state, event) }

    @Test
    fun aTurnGoesFromListeningToTranslatingToSpeakingAndBack() {
        val listening = start.after(TapMicrophone(Side.NEAR))
        assertEquals(Listening(Side.NEAR, ""), listening.phase)
        val heard = listening.after(Heard("Onde fica"), Heard("Onde fica a estação?"))
        assertEquals(Listening(Side.NEAR, "Onde fica a estação?"), heard.phase)
        val translating = heard.after(HeardFinal("Onde fica a estação?"))
        val turn = translating.turns.single()
        assertEquals(Turn(id = 1, side = Side.NEAR, from = pt, to = es, heard = "Onde fica a estação?"), turn)
        assertEquals(Translating(1), translating.phase)
        val speaking = translating.after(Translated(1, "¿Dónde está la estación?", speak = true))
        assertEquals(Speaking(1), speaking.phase)
        assertEquals("¿Dónde está la estación?", speaking.turns.single().translation)
        assertEquals(Idle, speaking.after(SpeechDone(1)).phase)
    }

    @Test
    fun theFarSideTranslatesTheOtherWay() {
        val state = start.after(TapMicrophone(Side.FAR), HeardFinal("Hola"))
        assertEquals(Turn(id = 1, side = Side.FAR, from = es, to = pt, heard = "Hola"), state.turns.single())
    }

    @Test
    fun withoutSpeakingATranslatedTurnGoesStraightBackToIdle() {
        val state = start.after(TapMicrophone(Side.NEAR), HeardFinal("Olá"), Translated(1, "Hola", speak = false))
        assertEquals(Idle, state.phase)
    }

    @Test
    fun aMicrophoneOpensOnlyFromIdle() {
        val translating = start.after(TapMicrophone(Side.NEAR), HeardFinal("Olá"))
        assertEquals(Translating(1), translating.after(TapMicrophone(Side.FAR)).phase)
        val speaking = translating.after(Translated(1, "Hola", speak = true))
        assertEquals(Speaking(1), speaking.after(TapMicrophone(Side.FAR)).phase)
        assertEquals(Speaking(1), speaking.after(TapMicrophone(Side.NEAR)).phase)
    }

    @Test
    fun theOtherSidesMicrophoneWaitsWhileOneListens() {
        val listening = start.after(TapMicrophone(Side.NEAR), Heard("Olá"))
        assertEquals(Listening(Side.NEAR, "Olá"), listening.after(TapMicrophone(Side.FAR)).phase)
    }

    @Test
    fun aSecondTapEndsTheTurnWithWhatWasHeard() {
        val state = start.after(TapMicrophone(Side.NEAR), Heard("Bom dia"), TapMicrophone(Side.NEAR))
        assertEquals(Translating(1), state.phase)
        assertEquals("Bom dia", state.turns.single().heard)
    }

    @Test
    fun theSquareButtonStopsListeningAndDropsOnlyWhatWasBeingHeard() {
        val before = start.after(TapMicrophone(Side.FAR), HeardFinal("Hola"), Translated(1, "Olá", speak = false))
        val discarded =
            before.after(
                TapMicrophone(Side.NEAR),
                Heard("Onde fi"),
                TapMicrophone(Side.NEAR, discard = true),
            )
        assertEquals(Idle, discarded.phase)
        assertEquals(before.turns, discarded.turns, "the earlier turns stay; nothing new is translated")
        assertEquals(null, discarded.message)
        // Only the half that listens can drop its turn.
        val listening = start.after(TapMicrophone(Side.NEAR), Heard("Bom"))
        assertEquals(listening, listening.after(TapMicrophone(Side.FAR, discard = true)))
    }

    @Test
    fun withNothingHeardItReturnsToIdleAndSaysSo() {
        val tapped = start.after(TapMicrophone(Side.NEAR), TapMicrophone(Side.NEAR))
        assertEquals(Idle, tapped.phase)
        assertEquals(SideMessage(Side.NEAR, ConversationProblem.NOTHING_HEARD), tapped.message)
        assertEquals(emptyList(), tapped.turns)
        val blank = start.after(TapMicrophone(Side.FAR), HeardFinal("  "))
        assertEquals(SideMessage(Side.FAR, ConversationProblem.NOTHING_HEARD), blank.message)
        val silent = start.after(TapMicrophone(Side.FAR), ListeningFailed(SpeechFailure.NOTHING_HEARD))
        assertEquals(SideMessage(Side.FAR, ConversationProblem.NOTHING_HEARD), silent.message)
    }

    @Test
    fun aFailureReturnsToIdleWithAMessageOnThatSide() {
        val missing = start.after(TapMicrophone(Side.FAR), ListeningFailed(SpeechFailure.LANGUAGE_MISSING))
        assertEquals(Idle, missing.phase)
        assertEquals(SideMessage(Side.FAR, ConversationProblem.LANGUAGE_MISSING), missing.message)
        val busy = start.after(TapMicrophone(Side.NEAR), ListeningFailed(SpeechFailure.BUSY))
        assertEquals(SideMessage(Side.NEAR, ConversationProblem.MICROPHONE_BUSY), busy.message)
        val engine = start.after(TapMicrophone(Side.NEAR), HeardFinal("Olá"), TranslationFailed(1))
        assertEquals(Idle, engine.phase)
        assertEquals(SideMessage(Side.FAR, ConversationProblem.TRANSLATION_FAILED), engine.message)
    }

    @Test
    fun theNextTapClearsTheMessage() {
        val failed = start.after(TapMicrophone(Side.NEAR), TapMicrophone(Side.NEAR))
        assertNull(failed.after(TapMicrophone(Side.NEAR)).message)
    }

    @Test
    fun aCallAnAlarmOrAHiddenScreenPausesAndOnlyATapResumes() {
        val listening = start.after(TapMicrophone(Side.NEAR), Heard("Olá"))
        val paused = listening.after(Interrupted(PauseReason.CALL_OR_ALARM))
        assertEquals(Paused(PauseReason.CALL_OR_ALARM), paused.phase)
        assertEquals(emptyList(), paused.turns)
        assertEquals(Paused(PauseReason.CALL_OR_ALARM), paused.after(SpeechDone(1)).phase)
        assertEquals(Idle, paused.after(TapMicrophone(Side.FAR)).phase)

        val speaking = start.after(TapMicrophone(Side.NEAR), HeardFinal("Olá"), Translated(1, "Hola", speak = true))
        assertEquals(Paused(PauseReason.HIDDEN), speaking.after(Interrupted(PauseReason.HIDDEN)).phase)
        assertEquals(Idle, start.after(Interrupted(PauseReason.HIDDEN)).phase)
    }

    @Test
    fun aTranslationArrivingWhilePausedIsKeptButNotSpoken() {
        val state =
            start.after(
                TapMicrophone(Side.NEAR),
                HeardFinal("Olá"),
                Interrupted(PauseReason.HIDDEN),
                Translated(1, "Hola", speak = true),
            )
        assertEquals(Paused(PauseReason.HIDDEN), state.phase)
        assertEquals("Hola", state.turns.single().translation)
    }

    @Test
    fun aCorrectedTurnIsTranslatedAgain() {
        val done = start.after(TapMicrophone(Side.NEAR), HeardFinal("Onde fica a estacão"), Translated(1, "x", false))
        val edited = done.after(Edited(1, "Onde fica a estação?"))
        assertEquals(Translating(1), edited.phase)
        val turn = edited.turns.single()
        assertEquals("Onde fica a estação?", turn.heard)
        assertNull(turn.translation)
        assertEquals(true, turn.edited)
    }

    @Test
    fun editingWaitsForIdle() {
        val listening = start.after(TapMicrophone(Side.NEAR), HeardFinal("Olá"))
        assertEquals(
            "Olá",
            listening
                .after(Edited(1, "Oi"))
                .turns
                .single()
                .heard,
        )
    }

    @Test
    fun clearEmptiesTheConversation() {
        val state = start.after(TapMicrophone(Side.NEAR), HeardFinal("Olá"), Translated(1, "Hola", false), Clear)
        assertEquals(emptyList(), state.turns)
        assertEquals(Idle, state.phase)
        assertEquals(
            1,
            state
                .after(TapMicrophone(Side.NEAR), HeardFinal("Oi"))
                .turns
                .single()
                .id,
        )
    }

    @Test
    fun theScreenShowsTheLatestTwoHundredTurnsAndCopyKeepsThemAll() {
        var state = start
        repeat(ConversationMachine.MAX_TURNS_SHOWN + 5) { index ->
            state = state.after(TapMicrophone(Side.NEAR), HeardFinal("turn $index"), Translated(index + 1L, "t", false))
        }
        assertEquals(ConversationMachine.MAX_TURNS_SHOWN + 5, state.turns.size)
        assertEquals(ConversationMachine.MAX_TURNS_SHOWN, state.shownTurns.size)
        assertEquals("turn 5", state.shownTurns.first().heard)
    }

    @Test
    fun changingASidesLanguageKeepsEarlierTurnsAsTheyWere() {
        val first = start.after(TapMicrophone(Side.NEAR), HeardFinal("Olá"), Translated(1, "Hola", false))
        val english = LanguageCode("en")
        val switched = ConversationMachine.reduce(first, ConversationEvent.LanguageChanged(Side.FAR, english))
        assertEquals(es, switched.turns.single().to)
        val next = switched.after(TapMicrophone(Side.NEAR), HeardFinal("Tchau"))
        assertEquals(english, next.turns.last().to)
    }
}
