package tech.onova.offlinetranslator.feature.toolstranslate.domain

import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent.Clear
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent.ConfirmChanged
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent.Edited
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent.HeardFinal
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent.Interrupted
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent.LanguageChanged
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent.Replay
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent.SendTurn
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent.SpeechDone
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent.StopSpeaking
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent.TapMicrophone
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent.Translated
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationPhase.Idle
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationPhase.Listening
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationPhase.Speaking
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationPhase.Translating
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationPhase.Waiting
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Spec 082 story 5 (FR-016, FR-017): a heard turn waits for ^ when "Confirm before sending" is on, and a translated
 * turn can be heard again.
 */
class ConversationWaitingTest {
    private val pt = LanguageCode("pt")
    private val es = LanguageCode("es")
    private val en = LanguageCode("en")
    private val start = ConversationState.start(near = pt, far = es)
    private val confirming = start.after(ConfirmChanged(on = true))

    private fun ConversationState.after(vararg events: ConversationEvent) =
        events.fold(this) { state, event -> ConversationMachine.reduce(state, event) }

    /** The near half said "Olá" with "Confirm before sending" on. */
    private val waiting = confirming.after(TapMicrophone(Side.NEAR), HeardFinal("Olá"))

    @Test
    fun withConfirmOnAHeardTurnWaitsInsteadOfBeingTranslated() {
        assertEquals(Waiting(1), waiting.phase)
        assertEquals(Turn(id = 1, side = Side.NEAR, from = pt, to = es, heard = "Olá"), waiting.turns.single())
    }

    @Test
    fun withConfirmOffATurnIsTranslatedAtOnceAsBefore() {
        assertEquals(Translating(1), start.after(TapMicrophone(Side.NEAR), HeardFinal("Olá")).phase)
    }

    @Test
    fun aPauseAfterSpeakingEndsTheTurnIntoWaitingToo() {
        val heard = confirming.after(TapMicrophone(Side.NEAR), ConversationEvent.Heard("Olá"), TapMicrophone(Side.NEAR))
        assertEquals(Waiting(1), heard.phase)
    }

    @Test
    fun sendingTheWaitingTurnTranslatesIt() {
        assertEquals(Translating(1), waiting.after(SendTurn(Side.NEAR)).phase)
    }

    @Test
    fun onlyTheWaitingHalfSendsIt() {
        assertEquals(Waiting(1), waiting.after(SendTurn(Side.FAR)).phase)
        assertEquals(Waiting(1), waiting.after(TapMicrophone(Side.FAR)).phase)
    }

    @Test
    fun theSquareDropsTheWaitingTurnAndTheMicrophoneIsBack() {
        val dropped = waiting.after(TapMicrophone(Side.NEAR, discard = true))
        assertEquals(Idle, dropped.phase)
        assertTrue(dropped.turns.isEmpty())
        assertEquals(Listening(Side.NEAR, ""), dropped.after(TapMicrophone(Side.NEAR)).phase)
    }

    @Test
    fun aCorrectedWaitingTurnKeepsWaitingWithItsNewWords() {
        val corrected = waiting.after(Edited(1, "Olá, tudo bem?"))
        assertEquals(Waiting(1), corrected.phase)
        assertEquals("Olá, tudo bem?", corrected.turns.single().heard)
        assertEquals(true, corrected.turns.single().edited)
    }

    @Test
    fun changingTheWaitingHalfsLanguageDropsTheTurnHeardInTheOldOne() {
        val changed = waiting.after(LanguageChanged(Side.NEAR, en))
        assertEquals(Idle, changed.phase)
        assertTrue(changed.turns.isEmpty())
    }

    @Test
    fun changingTheOtherHalfsLanguageSendsTheWaitingTurnThere() {
        val changed = waiting.after(LanguageChanged(Side.FAR, en))
        assertEquals(Waiting(1), changed.phase)
        assertEquals(en, changed.turns.single().to)
    }

    @Test
    fun aCallLeavesTheTurnWaiting() {
        assertEquals(Waiting(1), waiting.after(Interrupted(PauseReason.CALL_OR_ALARM)).phase)
    }

    @Test
    fun switchingConfirmOffNeverSendsTheWaitingTurn() {
        val off = waiting.after(ConfirmChanged(on = false))
        assertEquals(Waiting(1), off.phase)
        assertEquals(Translating(1), off.after(SendTurn(Side.NEAR)).phase)
    }

    @Test
    fun clearWhileATurnWaitsEmptiesEverythingButKeepsTheSwitch() {
        val cleared = waiting.after(Clear)
        assertEquals(Idle, cleared.phase)
        assertTrue(cleared.turns.isEmpty())
        assertEquals(true, cleared.confirm)
    }

    /** A turn translated and read, back to Idle. */
    private val said =
        start.after(TapMicrophone(Side.NEAR), HeardFinal("Olá"), Translated(1, "Hola", speak = true), SpeechDone(1))

    @Test
    fun aTranslatedTurnIsReadAgainFromIdle() {
        assertEquals(Speaking(1), said.after(Replay(1)).phase)
    }

    @Test
    fun aTurnIsReadAgainOnlyWhenNothingElseIsHappening() {
        val listening = said.after(TapMicrophone(Side.FAR))
        assertEquals(listening, listening.after(Replay(1)))
        val untranslated = confirming.after(TapMicrophone(Side.NEAR), HeardFinal("Olá"))
        assertEquals(untranslated, untranslated.after(Replay(1)))
    }

    @Test
    fun stopEndsTheReadingAtOnce() {
        assertEquals(Idle, said.after(Replay(1), StopSpeaking).phase)
        assertEquals(said, said.after(StopSpeaking))
    }
}
