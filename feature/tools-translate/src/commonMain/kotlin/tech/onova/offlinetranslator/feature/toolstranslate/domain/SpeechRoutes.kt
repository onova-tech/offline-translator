package tech.onova.offlinetranslator.feature.toolstranslate.domain

import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport
import tech.onova.offlinetranslator.core.domain.speech.AppSpeechPart
import tech.onova.offlinetranslator.core.domain.speech.SpeechEngine

/** Who hears a language, and the state the Translator shows for it (spec 082). */
internal data class RecognitionRoute(
    val engine: SpeechEngine,
    val support: RecognitionSupport,
)

/** Who speaks a language, and the state the Translator shows for it (spec 082). */
internal data class VoiceRoute(
    val engine: SpeechEngine,
    val support: VoiceSupport,
)

/**
 * The phone's own speech or the app's, for one language (spec 082 FR-001, FR-002, FR-006):
 * - the phone's while it works or can still get the language, unless the app's is ready now and the phone's isn't;
 * - the app's where the phone can't, offering its download first;
 * - where both work, the person's choice, the app's only once it's ready (US3 scenario 2).
 */
internal object SpeechRoutes {
    fun recognition(
        phone: RecognitionSupport,
        app: AppSpeechPart,
        choice: SpeechEngine?,
    ): RecognitionRoute {
        val phoneRoute = RecognitionRoute(SpeechEngine.PHONE, phone)
        val phoneWorks = phone == RecognitionSupport.READY || phone == RecognitionSupport.UNKNOWN
        val phoneCanGet = phone == RecognitionSupport.CAN_DOWNLOAD || phone == RecognitionSupport.DOWNLOADING
        // "Phone" keeps the phone's while it works or can still get the language; otherwise the app's ready model wins.
        val appWins =
            app == AppSpeechPart.READY && (choice == SpeechEngine.APP || !phoneWorks) &&
                !(choice == SpeechEngine.PHONE && (phoneWorks || phoneCanGet))
        return when {
            app == AppSpeechPart.NONE -> phoneRoute
            appWins -> RecognitionRoute(SpeechEngine.APP, RecognitionSupport.READY)
            phoneWorks || phoneCanGet -> phoneRoute
            app == AppSpeechPart.DOWNLOADING -> RecognitionRoute(SpeechEngine.APP, RecognitionSupport.APP_DOWNLOADING)
            else -> RecognitionRoute(SpeechEngine.APP, RecognitionSupport.APP_CAN_DOWNLOAD)
        }
    }

    fun voice(
        phone: VoiceSupport,
        app: AppSpeechPart,
        choice: SpeechEngine?,
    ): VoiceRoute {
        val phoneRoute = VoiceRoute(SpeechEngine.PHONE, phone)
        val phoneWorks = phone == VoiceSupport.READY
        return when {
            app == AppSpeechPart.NONE -> {
                phoneRoute
            }

            app == AppSpeechPart.READY && (choice == SpeechEngine.APP || !phoneWorks) -> {
                VoiceRoute(SpeechEngine.APP, VoiceSupport.READY)
            }

            phoneWorks -> {
                phoneRoute
            }

            app == AppSpeechPart.DOWNLOADING -> {
                VoiceRoute(SpeechEngine.APP, VoiceSupport.APP_DOWNLOADING)
            }

            else -> {
                VoiceRoute(SpeechEngine.APP, VoiceSupport.APP_CAN_DOWNLOAD)
            }
        }
    }
}
