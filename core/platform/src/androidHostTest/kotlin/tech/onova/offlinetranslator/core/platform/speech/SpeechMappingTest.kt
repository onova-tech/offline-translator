package tech.onova.offlinetranslator.core.platform.speech

import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.SpeechFailure
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Spec 012 research R1 and R2: the recognizer's lists and errors in the port's words, and the voice chosen. */
class SpeechMappingTest {
    @Test
    fun anInstalledLanguageIsReadyWhateverItsSpelling() {
        assertEquals(
            RecognitionSupport.READY,
            SpeechMapping.support("pt-BR", listOf("pt_br"), emptyList(), emptyList()),
        )
    }

    @Test
    fun aLanguageOnlySupportedCanBeAskedFor() {
        assertEquals(
            RecognitionSupport.CAN_DOWNLOAD,
            SpeechMapping.support("es-ES", listOf("en-US"), emptyList(), listOf("es-ES", "pt-BR")),
        )
    }

    @Test
    fun aLanguageBeingDownloadedIsOnItsWay() {
        assertEquals(
            RecognitionSupport.DOWNLOADING,
            SpeechMapping.support("es-ES", emptyList(), listOf("es-ES"), listOf("es-ES")),
        )
    }

    @Test
    fun anotherRegionIsNotTheSameLanguageForTheRecognizer() {
        assertEquals(
            RecognitionSupport.UNSUPPORTED,
            SpeechMapping.support("es-ES", listOf("es-US"), emptyList(), emptyList()),
        )
    }

    @Test
    fun errorsBecomeWhatThePersonIsTold() {
        assertEquals(SpeechFailure.NOTHING_HEARD, SpeechMapping.failure(SpeechRecognizer.ERROR_NO_MATCH))
        assertEquals(SpeechFailure.NOTHING_HEARD, SpeechMapping.failure(SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
        assertEquals(SpeechFailure.LANGUAGE_MISSING, SpeechMapping.failure(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE))
        assertEquals(
            SpeechFailure.LANGUAGE_MISSING,
            SpeechMapping.failure(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED),
        )
        assertEquals(SpeechFailure.BUSY, SpeechMapping.failure(SpeechRecognizer.ERROR_RECOGNIZER_BUSY))
        assertEquals(SpeechFailure.BUSY, SpeechMapping.failure(SpeechRecognizer.ERROR_AUDIO))
        assertEquals(
            SpeechFailure.NO_PERMISSION,
            SpeechMapping.failure(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS),
        )
        assertEquals(SpeechFailure.OTHER, SpeechMapping.failure(SpeechRecognizer.ERROR_SERVER_DISCONNECTED))
        assertEquals(SpeechFailure.OTHER, SpeechMapping.failure(SpeechRecognizer.ERROR_NETWORK))
    }

    @Test
    fun samsungsThreeLetterLocalesAreTheSameLanguages() {
        // As the owner's Samsung (SM-G780G, Android 13) listed them on 2026-11-18.
        val voices =
            listOf(
                facts("en-US-SMTf00", locale("eng", "USA", "f00")),
                facts("pt-PT-SMTf00", locale("por", "PRT", "f00")),
                facts("pt-BR-SMTf00", locale("por", "BRA", "f00")),
                facts("es-ES-SMTf00", locale("spa", "ESP", "f00")),
            )
        assertEquals("pt-BR-SMTf00", SpeechMapping.offlineVoice("pt-BR", voices) { it }?.name)
        assertEquals("en-US-SMTf00", SpeechMapping.offlineVoice("en-US", voices) { it }?.name)
        assertEquals("es-ES-SMTf00", SpeechMapping.offlineVoice("es-ES", voices) { it }?.name)
    }

    @Test
    fun theRegionComesFirstThenAnyVoiceOfTheLanguage() {
        val voices =
            listOf(
                facts("es-MX-SMTf00", locale("spa", "MEX", "f00")),
                facts("es-ES-SMTf00", locale("spa", "ESP", "f00")),
                facts("pt-pt-x-jfb-local", locale("pt", "PT")),
            )
        assertEquals("es-ES-SMTf00", SpeechMapping.offlineVoice("es-ES", voices) { it }?.name)
        assertEquals("es-MX-SMTf00", SpeechMapping.offlineVoice("es-MX", voices) { it }?.name)
        assertEquals("pt-pt-x-jfb-local", SpeechMapping.offlineVoice("pt-BR", voices) { it }?.name)
        assertEquals(
            "es-ES-SMTf00",
            SpeechMapping.offlineVoice("es-US", voices.drop(1)) { it }?.name,
        )
    }

    @Test
    fun theBetterVoiceOfTheRegionIsChosen() {
        val voices =
            listOf(
                facts("en-US-default", locale("eng", "USA", "default"), quality = 300),
                facts("en-US-SMTf00", locale("eng", "USA", "f00"), quality = 400),
            )
        assertEquals("en-US-SMTf00", SpeechMapping.offlineVoice("en-US", voices) { it }?.name)
    }

    @Test
    fun aVoiceThatNeedsTheNetworkOrIsNotOnThePhoneIsNeverChosen() {
        val voices =
            listOf(
                facts("es-es-x-eea-network", locale("es", "ES"), network = true),
                facts("es-es-x-eeb-local", locale("es", "ES"), features = setOf(NETWORK_SYNTHESIS)),
                facts("es-es-x-eea-local", locale("es", "ES"), features = setOf(NOT_INSTALLED)),
                facts("pt-BR-SMTf00", locale("por", "BRA", "f00")),
            )
        assertNull(SpeechMapping.offlineVoice("es-ES", voices) { it })
        assertNull(SpeechMapping.offlineVoice("en-US", voices) { it })
    }

    /** Engines build locales this way, with three-letter codes too (`Locale.of` is newer than the app's minimum). */
    @Suppress("DEPRECATION")
    private fun locale(
        language: String,
        country: String,
        variant: String = "",
    ) = Locale(language, country, variant)

    private fun facts(
        name: String,
        locale: Locale,
        network: Boolean = false,
        quality: Int = 400,
        features: Set<String> = emptySet(),
    ) = SpeechMapping.VoiceFacts(name, locale, network, features, quality)

    private companion object {
        @Suppress("DEPRECATION")
        const val NETWORK_SYNTHESIS = TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS
        const val NOT_INSTALLED = TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED
    }
}
