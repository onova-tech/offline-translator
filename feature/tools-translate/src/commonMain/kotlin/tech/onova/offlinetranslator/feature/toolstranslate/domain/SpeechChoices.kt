package tech.onova.offlinetranslator.feature.toolstranslate.domain

import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport
import tech.onova.offlinetranslator.core.domain.speech.SpeechEngine

/** A language's speech recognition or its voice. */
internal enum class SpeechKind { RECOGNITION, VOICE }

/**
 * Who hears and who speaks each language, and the person's choice where both the phone and the app can (spec 082
 * FR-006, FR-007): read for the Manage languages page, written there, kept in the Translator's settings.
 */
internal class SpeechChoices(
    private val input: RoutedSpeechInput,
    private val output: RoutedSpeechOutput,
    private val settings: TranslatorSettingsRepository,
) {
    /** A language's recognition as the screens show it, and who hears it: the phone is asked once. */
    suspend fun recognition(speechTag: String): Pair<RecognitionSupport, EngineUse> = input.supportAndUse(speechTag)

    /** A language's voice as the screens show it, and who speaks it: the phone is asked once. */
    suspend fun voice(speechTag: String): Pair<VoiceSupport, EngineUse> = output.voiceAndUse(speechTag)

    suspend fun choose(
        language: LanguageCode,
        kind: SpeechKind,
        engine: SpeechEngine,
    ) {
        settings.update { current ->
            val choice = current.speech[language] ?: SpeechChoice()
            val chosen =
                when (kind) {
                    SpeechKind.RECOGNITION -> choice.copy(recognition = engine)
                    SpeechKind.VOICE -> choice.copy(voice = engine)
                }
            current.copy(speech = current.speech + (language to chosen))
        }
    }
}
