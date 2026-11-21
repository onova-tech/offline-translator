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

    /**
     * Ready when every pack is in; downloading when one is on its way; none when the store can never deliver them, or
     * this build doesn't know them (the store lists every pack of its bundle, waiting for an answer or not).
     */
    private fun partOf(ids: List<PackId>): AppSpeechPart {
        val states =
            ids.map { id ->
                packs.packs.value
                    .firstOrNull { it.id == id }
                    ?.state ?: return AppSpeechPart.NONE
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
 * Who hears or speaks a language (spec 082 FR-006): the engine [used] now, the person's [chosen] one, and whether
 * both the phone and the app can do it (or get it), when the Manage languages page offers "Phone" or "App".
 */
internal data class EngineUse(
    val used: SpeechEngine,
    val chosen: SpeechEngine?,
    val choosable: Boolean,
) {
    /** What the row's Phone/App choice shows: the person's choice, else the one in use ("Phone" at first, US3). */
    val shown: SpeechEngine get() = if (choosable) chosen ?: used else used

    /** The row is about the app's model: in use, or chosen and still to download (US3 scenario 2). */
    val app: Boolean get() = shown == SpeechEngine.APP
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
    private val downloads: SpeechDownloads,
) : SpeechInput {
    private suspend fun route(speechTag: String): RecognitionRoute = decide(speechTag).first

    /** Who hears [speechTag]'s language, for the Manage languages page's Phone/App choice. */
    suspend fun use(speechTag: String): EngineUse = decide(speechTag).second

    private suspend fun decide(speechTag: String): Pair<RecognitionRoute, EngineUse> {
        val language = SpeechTags.languageOf(speechTag)
        val choice =
            language?.let {
                settings.settings
                    .first()
                    .speech[it]
                    ?.recognition
            }
        // A language the phone was asked for and never installed (the owner's Japanese, 2026-11-20) is one it can't do,
        // so the app's is offered (2026-11-21). Where the phone's stays, its own answer goes on to the screens.
        val said = phone.support(speechTag)
        val phoneSupport =
            if (said == RecognitionSupport.CAN_DOWNLOAD && downloads.neverInstalls(speechTag)) {
                RecognitionSupport.NOT_INSTALLED
            } else {
                said
            }
        val app = parts.recognition(language)
        val route = SpeechRoutes.recognition(phoneSupport, app, choice)
        val use = EngineUse(route.engine, choice, choosable = app != AppSpeechPart.NONE && phoneSupport in PHONE_CAN)
        return (if (route.engine == SpeechEngine.PHONE) route.copy(support = said) else route) to use
    }

    private companion object {
        /** The phone hears the language, or can still get it: then both can, and the person may choose. */
        val PHONE_CAN =
            setOf(
                RecognitionSupport.READY,
                RecognitionSupport.UNKNOWN,
                RecognitionSupport.CAN_DOWNLOAD,
                RecognitionSupport.DOWNLOADING,
            )
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
    private suspend fun route(speechTag: String): VoiceRoute = decide(speechTag).first

    /** Who speaks [speechTag]'s language, for the Manage languages page's Phone/App choice. */
    suspend fun use(speechTag: String): EngineUse = decide(speechTag).second

    private suspend fun decide(speechTag: String): Pair<VoiceRoute, EngineUse> {
        val language = SpeechTags.languageOf(speechTag)
        val choice =
            language?.let {
                settings.settings
                    .first()
                    .speech[it]
                    ?.voice
            }
        val said = phone.voice(speechTag)
        val app = parts.voice(language)
        val route = SpeechRoutes.voice(said, app, choice)
        return route to EngineUse(route.engine, choice, app != AppSpeechPart.NONE && said == VoiceSupport.READY)
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
