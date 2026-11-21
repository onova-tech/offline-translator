package tech.onova.offlinetranslator.feature.toolstranslate.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import tech.onova.offlinetranslator.core.domain.ports.ContentPacks
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.PackState
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.SpeechEnd
import tech.onova.offlinetranslator.core.domain.ports.SpeechEvent
import tech.onova.offlinetranslator.core.domain.ports.SpeechInput
import tech.onova.offlinetranslator.core.domain.ports.SpeechOutput
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport
import tech.onova.offlinetranslator.core.domain.speech.AppSpeechPart
import tech.onova.offlinetranslator.core.domain.speech.SpeechCatalog
import tech.onova.offlinetranslator.core.domain.speech.SpeechEngine
import tech.onova.offlinetranslator.core.domain.speech.SpeechTags

/** Where the app's own models stand for each language, from their packs' states (spec 082). */
internal class AppSpeechParts(
    private val packs: ContentPacks,
) {
    fun recognition(language: LanguageCode?): AppSpeechPart =
        if (language == null || language !in SpeechCatalog.recognition) {
            AppSpeechPart.NONE
        } else {
            partOf(listOf(SpeechCatalog.recognitionPack))
        }

    fun voice(language: LanguageCode?): AppSpeechPart {
        val voice = language?.let { SpeechCatalog.voices[it] } ?: return AppSpeechPart.NONE
        return partOf(listOf(voice.pack, SpeechCatalog.voiceDataPack))
    }

    /** Ready when every pack is in; downloading when one is on its way; none when the store can never deliver them. */
    private fun partOf(ids: List<PackId>): AppSpeechPart {
        val states =
            ids.map { id ->
                packs.packs.value
                    .firstOrNull { it.id == id }
                    ?.state ?: PackState.Unknown
            }
        return when {
            states.any { it is PackState.Unavailable } -> {
                AppSpeechPart.NONE
            }

            states.all { it == PackState.Installed } -> {
                AppSpeechPart.READY
            }

            states.any { it is PackState.Downloading || it is PackState.Waiting || it == PackState.Installing } -> {
                AppSpeechPart.DOWNLOADING
            }

            else -> {
                AppSpeechPart.NOT_DOWNLOADED
            }
        }
    }
}

/**
 * The Translator's speech recognition (spec 082): the phone's own or the app's, per language, by [SpeechRoutes] from
 * what the phone says, the app's pack and the person's choice. Bound as the Translator's [SpeechInput], so every screen
 * gets the app's recognition where the phone has none (FR-001, FR-008).
 */
internal class RoutedSpeechInput(
    private val phone: SpeechInput,
    private val app: SpeechInput,
    private val parts: AppSpeechParts,
    private val settings: TranslatorSettingsRepository,
) : SpeechInput {
    private suspend fun route(speechTag: String): RecognitionRoute {
        val language = SpeechTags.languageOf(speechTag)
        val choice =
            language?.let {
                settings.settings
                    .first()
                    .speech[it]
                    ?.recognition
            }
        return SpeechRoutes.recognition(phone.support(speechTag), parts.recognition(language), choice)
    }

    override suspend fun support(speechTag: String): RecognitionSupport = route(speechTag).support

    override fun requestLanguage(speechTag: String): Boolean = phone.requestLanguage(speechTag)

    override fun canUpdateService(): Boolean = phone.canUpdateService()

    override fun openServiceUpdate() = phone.openServiceUpdate()

    override fun listen(speechTag: String): Flow<SpeechEvent> =
        flow {
            val route = route(speechTag)
            val engine =
                if (route.engine == SpeechEngine.APP &&
                    route.support == RecognitionSupport.READY
                ) {
                    app
                } else {
                    phone
                }
            emitAll(engine.listen(speechTag))
        }
}

/** The Translator's voices (spec 082): the phone's own or the app's, per language, as [RoutedSpeechInput] (FR-002). */
internal class RoutedSpeechOutput(
    private val phone: SpeechOutput,
    private val app: SpeechOutput,
    private val parts: AppSpeechParts,
    private val settings: TranslatorSettingsRepository,
) : SpeechOutput {
    private suspend fun route(speechTag: String): VoiceRoute {
        val language = SpeechTags.languageOf(speechTag)
        val choice =
            language?.let {
                settings.settings
                    .first()
                    .speech[it]
                    ?.voice
            }
        return SpeechRoutes.voice(phone.voice(speechTag), parts.voice(language), choice)
    }

    override suspend fun voice(speechTag: String): VoiceSupport = route(speechTag).support

    override suspend fun speak(
        text: String,
        speechTag: String,
    ): SpeechEnd {
        val route = route(speechTag)
        val engine = if (route.engine == SpeechEngine.APP && route.support == VoiceSupport.READY) app else phone
        return engine.speak(text, speechTag)
    }

    override fun stop() {
        phone.stop()
        app.stop()
    }

    override fun openVoiceSettings() = phone.openVoiceSettings()
}
