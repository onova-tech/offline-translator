package tech.onova.offlinetranslator.feature.toolstranslate.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.onova.offlinetranslator.core.domain.ports.AppVisibility
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.SpeechInput
import tech.onova.offlinetranslator.core.domain.ports.SpeechOutput
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SpeechDownloads
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SpeechRegions

/** What the phone has for one language: its own speech recognition, and an offline voice. */
internal data class SpeechParts(
    val recognition: RecognitionSupport,
    val voice: VoiceSupport,
)

internal data class SpeechPartsUi(
    /** Each language shown, once the phone answered for it. */
    val parts: Map<LanguageCode, SpeechParts> = emptyMap(),
    /** The phone's speech service can be updated from its store page (a language it didn't install). */
    val serviceUpdatable: Boolean = false,
)

/**
 * The Manage languages page's speech and voice (spec 080 story 4): for each of the person's languages, what the phone's
 * own recognizer and voices can do, asked of the phone, never guessed; again every few seconds while one is on its way,
 * and when the screen comes back (from the store, the voice app). The downloads are the phone's: the app only asks.
 */
internal class SpeechPartsViewModel(
    private val speechIn: SpeechInput,
    private val speechOut: SpeechOutput,
    private val downloads: SpeechDownloads,
    visibility: AppVisibility,
) : ViewModel() {
    private val state = MutableStateFlow(SpeechPartsUi())
    val uiState: StateFlow<SpeechPartsUi> = state.asStateFlow()

    private var languages: List<LanguageCode> = emptyList()
    private var phoneLocales: List<String> = emptyList()
    private var checking: Job? = null

    private val support =
        SpeechRecheck(
            scope = viewModelScope,
            visibility = visibility,
            ask = {
                val asked =
                    languages.associateWith { language ->
                        val tag = tagOf(language)
                        SpeechParts(downloads.read(tag, speechIn.support(tag)), speechOut.voice(tag))
                    }
                state.update { it.copy(parts = asked) }
            },
            downloading = {
                state.value.parts.values
                    .any { it.recognition == RecognitionSupport.DOWNLOADING }
            },
            onItsWay = {
                state.value.parts.values.any {
                    it.recognition == RecognitionSupport.DOWNLOADING ||
                        it.recognition == RecognitionSupport.NOT_INSTALLED
                }
            },
        )

    init {
        viewModelScope.launch {
            var wasShown = true
            visibility.isVisible.collect { shown ->
                if (shown && !wasShown) {
                    // Back from the store or the voice app: asked again, and a language not installed can be asked for.
                    downloads.forget()
                    support.askNow()
                }
                wasShown = shown
            }
        }
        state.update { it.copy(serviceUpdatable = speechIn.canUpdateService()) }
    }

    /** The languages shown, with the phone's languages for their speech regions; asked again when they change. */
    fun show(
        languages: List<LanguageCode>,
        phoneLocales: List<String>,
    ) {
        if (languages == this.languages && phoneLocales == this.phoneLocales) return
        this.languages = languages
        this.phoneLocales = phoneLocales
        checking?.cancel()
        checking = viewModelScope.launch { support.askNow() }
    }

    /** Asks the phone's own speech service to get [language]; the page then shows it on its way, then ready. */
    fun get(language: LanguageCode) {
        val tag = tagOf(language)
        if (!speechIn.requestLanguage(tag)) return
        downloads.asked(tag)
        viewModelScope.launch {
            support.askNow()
            support.watch()
        }
    }

    /** The store page of the phone's speech service, which may install a language it couldn't. */
    fun update() = speechIn.openServiceUpdate()

    /** The voice app's own download list. */
    fun voice() = speechOut.openVoiceSettings()

    private fun tagOf(language: LanguageCode): String = SpeechRegions.tagFor(language, phoneLocales)
}
