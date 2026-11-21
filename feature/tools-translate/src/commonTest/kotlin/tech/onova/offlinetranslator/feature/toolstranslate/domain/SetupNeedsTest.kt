package tech.onova.offlinetranslator.feature.toolstranslate.domain

import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Owner, 2026-11-19 (spec 012 story 5, scenario 6): what the phone's owner sets up shows upright in the near half, one
 * thing at a time; when it's done, the next one comes.
 */
class SetupNeedsTest {
    private val ready = SideSupport(RecognitionSupport.READY, VoiceSupport.READY)
    private val canGet = SideSupport(RecognitionSupport.CAN_DOWNLOAD, VoiceSupport.READY)
    private val getting = SideSupport(RecognitionSupport.DOWNLOADING, VoiceSupport.READY)
    private val noVoice = SideSupport(RecognitionSupport.READY, VoiceSupport.NO_OFFLINE_VOICE)
    private val cantRecognize = SideSupport(RecognitionSupport.UNSUPPORTED, VoiceSupport.READY)
    private val offer = PackOffer.Offer(PackId("translate_es"), megabytes = 48)

    private fun first(
        near: SideSupport,
        far: SideSupport,
        offer: PackOffer? = null,
        voices: Boolean = true,
    ) = SetupNeeds.first(offer, near, far, voices)

    @Test
    fun nothingMissingNeedsNothing() {
        assertNull(first(ready, ready))
    }

    @Test
    fun theTranslationPackComesFirstSinceNothingWorksWithoutIt() {
        assertEquals(SetupNeed.Pack(offer), first(canGet, noVoice, offer))
    }

    @Test
    fun speechThePhoneCanGetComesNearHalfFirstThenTheFarHalf() {
        assertEquals(SetupNeed.Recognition(Side.NEAR, downloading = false), first(canGet, canGet))
        assertEquals(SetupNeed.Recognition(Side.FAR, downloading = false), first(ready, canGet))
    }

    @Test
    fun aLanguageOnItsWayStaysUntilItIsReadyBeforeTheNextOne() {
        assertEquals(SetupNeed.Recognition(Side.NEAR, downloading = true), first(getting, canGet))
    }

    /** Owner's phone, 2026-11-20: Japanese asked for and never installed says so, and the phone is still asked. */
    @Test
    fun aLanguageThePhoneCouldntInstallSaysSoBeforeAVoiceAndKeepsBeingAsked() {
        val notInstalled = SideSupport(RecognitionSupport.NOT_INSTALLED, VoiceSupport.NO_OFFLINE_VOICE)
        assertEquals(SetupNeed.NotInstalled(Side.FAR), first(ready, notInstalled))
        assertEquals(SetupNeed.Recognition(Side.FAR, downloading = false), first(notInstalled, canGet))
        assertTrue(SetupNeeds.waiting(ready, notInstalled))
    }

    /**
     * Owner, 2026-11-20: a notice nothing can fix (Arabic, which the phone can't recognize) has a small ×. Dismissed,
     * it gives way to the next one; a language the phone can get never turns into "can't recognize" by being
     * dismissed; the translation pack can't be dismissed, since nothing works without it.
     */
    @Test
    fun aDismissedNoticeGivesWayToTheNextOne() {
        val dismissNear = setOf(SetupKey(SetupKind.RECOGNITION, Side.NEAR))
        assertEquals(SetupNeed.Voice(Side.FAR), SetupNeeds.first(null, canGet, noVoice, voices = true, dismissNear))
        assertNull(SetupNeeds.first(null, canGet, ready, voices = true, dismissNear))
        val dismissFar = setOf(SetupKey(SetupKind.NO_RECOGNITION, Side.FAR))
        assertNull(SetupNeeds.first(null, ready, cantRecognize, voices = true, dismissFar))
        assertEquals(SetupKey(SetupKind.NO_RECOGNITION, Side.FAR), SetupNeed.NoRecognition(Side.FAR).key)
        assertNull(SetupNeed.Pack(offer).key)
    }

    @Test
    fun aVoiceComesAfterSpeechAndOnlyWhenSomethingIsReadAloud() {
        assertEquals(SetupNeed.Voice(Side.FAR), first(ready, noVoice))
        assertEquals(SetupNeed.Recognition(Side.FAR, downloading = false), first(noVoice, canGet))
        assertNull(first(ready, noVoice, voices = false))
    }

    @Test
    fun whatCantBeHadComesLastSoItNeverHidesWhatCan() {
        assertEquals(SetupNeed.Voice(Side.FAR), first(cantRecognize, noVoice))
        assertEquals(SetupNeed.NoRecognition(Side.NEAR), first(cantRecognize, ready))
        val tooOld = SideSupport(RecognitionSupport.SYSTEM_TOO_OLD, VoiceSupport.READY)
        assertEquals(SetupNeed.NoRecognition(Side.FAR), first(ready, tooOld))
    }

    @Test
    fun androidTwelveFindsOutByTryingSoItNeedsNothing() {
        val unknown = SideSupport(RecognitionSupport.UNKNOWN, VoiceSupport.READY)
        assertTrue(unknown.canListen)
        assertNull(first(unknown, unknown))
    }

    @Test
    fun theLanguagesAreAskedAgainWhileOneIsOnItsWay() {
        assertTrue(SetupNeeds.waiting(getting, ready))
        assertTrue(SetupNeeds.waiting(ready, canGet))
        assertFalse(SetupNeeds.waiting(ready, noVoice))
        assertFalse(SetupNeeds.waiting(cantRecognize, ready))
    }
}
