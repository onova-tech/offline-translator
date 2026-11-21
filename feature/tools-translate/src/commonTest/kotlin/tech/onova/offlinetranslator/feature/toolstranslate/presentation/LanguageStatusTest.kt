package tech.onova.offlinetranslator.feature.toolstranslate.presentation

import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.PackState
import tech.onova.offlinetranslator.core.domain.ports.PackStatus
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Owner, 2026-11-20: Manage languages' marks, installed and enabled (✅), installed (☑), in part (⚠), gray when off. */
class LanguageStatusTest {
    private val installed = PackStatus(PackId("translate_ar"), 70, 70, PackState.Installed)
    private val notInstalled = PackStatus(PackId("translate_ar"), 70, null, PackState.NotInstalled)
    private val ready = SpeechParts(RecognitionSupport.READY, VoiceSupport.READY)

    @Test
    fun aDownloadedLanguageWithEverythingIsComplete() {
        assertEquals(LanguageStatus(complete = true, enabled = true), statusOf(true, installed, ready))
        assertEquals(LanguageStatus(complete = true, enabled = false), statusOf(false, installed, ready))
    }

    @Test
    fun englishIsBuiltIn() {
        assertEquals(LanguageStatus(complete = true, enabled = true), statusOf(true, null, ready))
    }

    @Test
    fun speechOrAVoiceThePhoneCanStillGetMakesItPartial() {
        for (recognition in listOf(
            RecognitionSupport.CAN_DOWNLOAD,
            RecognitionSupport.DOWNLOADING,
            RecognitionSupport.NOT_INSTALLED,
        )) {
            assertEquals(false, statusOf(true, installed, SpeechParts(recognition, VoiceSupport.READY))?.complete)
        }
        assertEquals(
            LanguageStatus(complete = false, enabled = false),
            statusOf(false, installed, SpeechParts(RecognitionSupport.READY, VoiceSupport.NO_OFFLINE_VOICE)),
        )
    }

    /** Arabic on the owner's phone: no speech recognition to get, and no voice app counts the same. */
    @Test
    fun whatThePhoneCantHaveDoesntCount() {
        val arabic = SpeechParts(RecognitionSupport.UNSUPPORTED, VoiceSupport.NO_ENGINE)
        assertEquals(LanguageStatus(complete = true, enabled = true), statusOf(true, installed, arabic))
    }

    @Test
    fun noMarkUntilThePackIsOnThePhoneAndThePhoneHasAnswered() {
        assertNull(statusOf(true, notInstalled, ready))
        assertNull(statusOf(true, installed, speech = null))
    }
}
