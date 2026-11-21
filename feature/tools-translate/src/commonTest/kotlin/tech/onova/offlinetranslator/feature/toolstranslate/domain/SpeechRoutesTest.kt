package tech.onova.offlinetranslator.feature.toolstranslate.domain

import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport
import tech.onova.offlinetranslator.core.domain.speech.AppSpeechPart
import tech.onova.offlinetranslator.core.domain.speech.SpeechEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport as Recognition
import tech.onova.offlinetranslator.core.domain.speech.AppSpeechPart as Part

/** Spec 082 T010: the phone's or the app's speech, for one language (FR-001, FR-002, FR-006, US3). */
class SpeechRoutesTest {
    private fun phone(support: RecognitionSupport) = RecognitionRoute(SpeechEngine.PHONE, support)

    private fun app(support: RecognitionSupport) = RecognitionRoute(SpeechEngine.APP, support)

    private fun phone(support: VoiceSupport) = VoiceRoute(SpeechEngine.PHONE, support)

    private fun app(support: VoiceSupport) = VoiceRoute(SpeechEngine.APP, support)

    private fun hear(
        phone: RecognitionSupport,
        app: AppSpeechPart,
        choice: SpeechEngine? = null,
    ) = SpeechRoutes.recognition(phone, app, choice)

    private fun say(
        phone: VoiceSupport,
        app: AppSpeechPart,
        choice: SpeechEngine? = null,
    ) = SpeechRoutes.voice(phone, app, choice)

    @Test
    fun thePhonesOwnIsUsedWhenItWorks() {
        assertEquals(phone(Recognition.READY), hear(Recognition.READY, Part.READY))
        assertEquals(phone(Recognition.UNKNOWN), hear(Recognition.UNKNOWN, Part.NOT_DOWNLOADED))
        assertEquals(phone(VoiceSupport.READY), say(VoiceSupport.READY, Part.READY))
    }

    /** The owner's phone, 2026-11-20: no Arabic recognizer, and a Japanese one that never installs. */
    @Test
    fun theAppsIsOfferedWhereThePhoneCant() {
        for (phone in listOf(
            Recognition.UNSUPPORTED,
            Recognition.NOT_INSTALLED,
            Recognition.NO_RECOGNIZER,
            Recognition.SYSTEM_TOO_OLD,
        )) {
            assertEquals(app(Recognition.APP_CAN_DOWNLOAD), hear(phone, Part.NOT_DOWNLOADED))
            assertEquals(app(Recognition.APP_DOWNLOADING), hear(phone, Part.DOWNLOADING))
            assertEquals(app(Recognition.READY), hear(phone, Part.READY))
        }
        assertEquals(app(VoiceSupport.APP_CAN_DOWNLOAD), say(VoiceSupport.NO_OFFLINE_VOICE, Part.NOT_DOWNLOADED))
        assertEquals(app(VoiceSupport.READY), say(VoiceSupport.NO_ENGINE, Part.READY))
    }

    @Test
    fun withNothingFromTheAppThePhonesStateStays() {
        assertEquals(phone(Recognition.UNSUPPORTED), hear(Recognition.UNSUPPORTED, Part.NONE))
        assertEquals(phone(VoiceSupport.NO_OFFLINE_VOICE), say(VoiceSupport.NO_OFFLINE_VOICE, Part.NONE))
    }

    /** Ready now beats ready later: the phone can still get it, but the app's is already here. */
    @Test
    fun theAppsReadyModelWinsOverOneThePhoneMustStillGet() {
        assertEquals(app(Recognition.READY), hear(Recognition.CAN_DOWNLOAD, Part.READY))
        assertEquals(phone(Recognition.CAN_DOWNLOAD), hear(Recognition.CAN_DOWNLOAD, Part.NOT_DOWNLOADED))
    }

    /** US3: "App" is used once it's ready, the phone's until then; "Phone" keeps the phone's while it works. */
    @Test
    fun thePersonsChoiceWinsWhereBothCan() {
        assertEquals(app(Recognition.READY), hear(Recognition.READY, Part.READY, SpeechEngine.APP))
        assertEquals(phone(Recognition.READY), hear(Recognition.READY, Part.NOT_DOWNLOADED, SpeechEngine.APP))
        assertEquals(phone(Recognition.CAN_DOWNLOAD), hear(Recognition.CAN_DOWNLOAD, Part.READY, SpeechEngine.PHONE))
        assertEquals(app(VoiceSupport.READY), say(VoiceSupport.READY, Part.READY, SpeechEngine.APP))
        // "Phone" can't keep a language the phone has lost: the app's takes over.
        assertEquals(app(Recognition.READY), hear(Recognition.UNSUPPORTED, Part.READY, SpeechEngine.PHONE))
    }
}
