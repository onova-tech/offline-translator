package tech.onova.offlinetranslator.feature.toolstranslate.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.onova.offlinetranslator.core.domain.ports.AppVisibility
import tech.onova.offlinetranslator.core.domain.ports.ContentPacks
import tech.onova.offlinetranslator.core.domain.ports.EngineUnavailable
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.ScreenReader
import tech.onova.offlinetranslator.core.domain.ports.SpeechInput
import tech.onova.offlinetranslator.core.domain.ports.SpeechOutput
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport
import tech.onova.offlinetranslator.feature.toolstranslate.domain.AppSpeechParts
import tech.onova.offlinetranslator.feature.toolstranslate.domain.CheckedLanguages
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationEvent
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationMachine
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationPhase
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationState
import tech.onova.offlinetranslator.feature.toolstranslate.domain.EngineSession
import tech.onova.offlinetranslator.feature.toolstranslate.domain.PackMessages
import tech.onova.offlinetranslator.feature.toolstranslate.domain.PackOffer
import tech.onova.offlinetranslator.feature.toolstranslate.domain.PauseReason
import tech.onova.offlinetranslator.feature.toolstranslate.domain.Routes
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SetupKey
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SetupNeed
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SetupNeeds
import tech.onova.offlinetranslator.feature.toolstranslate.domain.Side
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SideSupport
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SpeechDownloads
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SpeechRegions
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorDefaults
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorSettingsRepository
import tech.onova.offlinetranslator.feature.toolstranslate.domain.Turn

/** The switches under More options; CONFIRM is "Confirm before sending" (spec 082 FR-016). */
internal enum class ConversationOption { FAR_TURNED, READ_ALOUD, CONFIRM }

/** The phone's speech and what decides when it may speak; [downloads] is shared with the Manage languages page. */
internal class ConversationSpeech(
    val input: SpeechInput,
    val output: SpeechOutput,
    val screenReader: ScreenReader,
    val visibility: AppVisibility,
    val downloads: SpeechDownloads,
)

internal data class ConversationUi(
    val loaded: Boolean = false,
    val conversation: ConversationState = ConversationState.start(LanguageCode.ENGLISH, LanguageCode.ENGLISH),
    val farTurned: Boolean = true,
    val readAloud: Boolean = true,
    val near: SideSupport = SideSupport(),
    val far: SideSupport = SideSupport(),
    /** The pack the pair needs, offered in the near half (owner, 2026-11-19). */
    val offer: PackOffer? = null,
    val unavailable: EngineUnavailable? = null,
    /** With TalkBack on, a translation is announced instead of read by the phone's voice (research R8). */
    val screenReader: Boolean = false,
    val languages: List<LanguageOption> = emptyList(),
    /** Speech notices closed with their × (owner, 2026-11-20), until the tool closes or the half's language changes. */
    val dismissed: Set<SetupKey> = emptySet(),
) {
    /** The pair can be translated here and nothing is being translated or said. */
    val canListen: Boolean
        get() {
            val phase = conversation.phase
            val free =
                phase is ConversationPhase.Idle || phase is ConversationPhase.Listening ||
                    phase is ConversationPhase.Paused
            return offer == null && unavailable == null && free
        }

    fun supportOf(side: Side): SideSupport = if (side == Side.NEAR) near else far

    /** The turn waiting for ^ (spec 082 FR-016), if one is. */
    val waiting: Turn? get() = conversation.waiting

    /** A translated turn's speaker button (FR-017): enabled with a ready voice while nothing else happens. */
    fun canReplay(side: Side): Boolean =
        conversation.phase == ConversationPhase.Idle && supportOf(side).voice == VoiceSupport.READY

    /** What the phone's owner sets up next, upright in the near half, one thing at a time (owner, 2026-11-19). */
    val setup: SetupNeed?
        get() = SetupNeeds.first(offer, near, far, voices = readAloud && !screenReader, dismissed = dismissed)

    /** The microphone of [side]: open to start, or to end its own turn; off while the other one listens. */
    fun microphoneEnabled(side: Side): Boolean {
        val phase = conversation.phase
        return canListen && (phase !is ConversationPhase.Listening || phase.side == side)
    }
}

/** A half whose language changed shows its speech notices again, closed or not (owner, 2026-11-20). */
private fun ConversationUi.undismissed(side: Side): ConversationUi =
    copy(dismissed = dismissed.filterNot { it.side == side }.toSet())

/** The app's language and the phone's, given once by the screen. */
private data class Start(
    val appLanguage: LanguageCode,
    val phoneLocales: List<String>,
)

/**
 * The Conversation (spec 012 story 5): each spoken turn goes through [ConversationMachine], is translated into the
 * other half's language and read aloud there; a microphone opens only from Idle, so nothing listens while the phone
 * speaks (FR-025). A hidden screen pauses it all. The turns live only in this view model (FR-027).
 */
internal class ConversationViewModel(
    private val repository: TranslatorSettingsRepository,
    private val packs: ContentPacks,
    private val session: EngineSession,
    private val speech: ConversationSpeech,
    private val outputs: TranslatorOutputs,
) : ViewModel() {
    private val start = MutableStateFlow<Start?>(null)

    /** The checked languages (spec 080), for the halves' lists. */
    private val checked = MutableStateFlow(CheckedLanguages.DEFAULT)
    private val ui = MutableStateFlow(ConversationUi())
    val uiState: StateFlow<ConversationUi> = ui.asStateFlow()

    /** Copy, with the app's own "copied" when the system shows none. */
    val clipboard = CopyOutput(outputs)

    /** A language's speech tag, with the phone's languages for its region (research R9). */
    private val tagOf: (LanguageCode) -> String = { language ->
        SpeechRegions.tagFor(language, start.value?.phoneLocales.orEmpty())
    }

    private val turns =
        ConversationTurns(
            scope = viewModelScope,
            speech = speech,
            session = session,
            dispatch = ::dispatch,
            state = { ui.value },
            tagOf = tagOf,
        )

    private val downloads = speech.downloads

    private val support =
        SpeechRecheck(
            scope = viewModelScope,
            visibility = speech.visibility,
            ask = {
                val conversation = ui.value.conversation
                val near = downloads.read(tagOf(conversation.near), turns.supportOf(conversation.near))
                val far = downloads.read(tagOf(conversation.far), turns.supportOf(conversation.far))
                ui.update { it.copy(near = near, far = far) }
            },
            downloading = {
                listOf(ui.value.near, ui.value.far).any { it.recognition == RecognitionSupport.DOWNLOADING }
            },
            onItsWay = { SetupNeeds.waiting(ui.value.near, ui.value.far) },
        )

    init {
        session.acquire()
        viewModelScope.launch {
            combine(repository.settings, start.filterNotNull()) { settings, started ->
                TranslatorDefaults.resolve(settings, started.appLanguage)
            }.collect { resolved ->
                checked.value = resolved.languages
                val first = !ui.value.loaded
                ui.update { it.copy(loaded = true, farTurned = resolved.farHalfTurned) }
                if (first) ui.update { it.copy(conversation = ConversationState.start(resolved.near, resolved.far)) }
                if (resolved.confirmBeforeSending != ui.value.conversation.confirm) {
                    dispatch(ConversationEvent.ConfirmChanged(resolved.confirmBeforeSending))
                }
                if (resolved.near != ui.value.conversation.near) {
                    ui.update { it.undismissed(Side.NEAR) }
                    dispatch(ConversationEvent.LanguageChanged(Side.NEAR, resolved.near))
                }
                if (resolved.far != ui.value.conversation.far) {
                    ui.update { it.undismissed(Side.FAR) }
                    dispatch(ConversationEvent.LanguageChanged(Side.FAR, resolved.far))
                }
            }
        }
        viewModelScope.launch {
            val pair = ui.map { it.conversation.near to it.conversation.far }.distinctUntilChanged()
            combine(pair, packs.packs, session.state, checked) { (near, far), statuses, engine, languages ->
                val offer = PackMessages.firstOffer(Routes.between(near, far), statuses)
                val unavailable = (engine as? EngineSession.State.Unavailable)?.reason
                Triple(offer, unavailable, languagesOf(statuses, languages))
            }.collect { (offer, unavailable, languages) ->
                ui.update {
                    it.copy(
                        offer = offer,
                        unavailable = unavailable,
                        languages = languages,
                    )
                }
            }
        }
        viewModelScope.launch {
            ui
                .map { it.conversation.near to it.conversation.far }
                .distinctUntilChanged()
                .collect { support.askNow() }
        }
        viewModelScope.launch { speech.screenReader.on.collect { on -> ui.update { it.copy(screenReader = on) } } }
        viewModelScope.launch {
            var wasShown = true
            speech.visibility.isVisible.collect { shown ->
                // Back from the phone's settings (a voice added, a language got): asked again, never guessed.
                if (!shown) {
                    dispatch(ConversationEvent.Interrupted(PauseReason.HIDDEN))
                } else if (!wasShown) {
                    downloads.forget()
                    support.askNow()
                }
                wasShown = shown
            }
        }
        viewModelScope.launch {
            // A language asked for on the Manage languages page: asked about here too, until it's ready.
            downloads.requests.drop(1).collect {
                support.askNow()
                support.watch()
            }
        }
        viewModelScope.launch {
            // The app's speech packs in, gone or on their way (spec 082), the store's first answer among them.
            AppSpeechParts(packs).changes.collect { support.askNow() }
        }
    }

    /** The app's language (the near half's first language) and the phone's languages, for the speech regions. */
    fun start(
        appLanguage: String,
        phoneLocales: List<String>,
    ) {
        start.value = Start(LanguageCode(appLanguage), phoneLocales)
    }

    /**
     * A half's microphone: opens it from Idle, ends its own turn, or wakes a paused conversation. With [discard], its
     * square button: listening stops and what was being heard goes, untranslated (owner, 2026-11-19).
     */
    fun tap(
        side: Side,
        discard: Boolean = false,
    ) {
        val state = ui.value
        if (state.offer != null || state.unavailable != null) return
        // ^ stands in the microphone's place while the half's turn waits (spec 082 FR-016).
        val sends = !discard && state.waiting?.side == side
        dispatch(if (sends) ConversationEvent.SendTurn(side) else ConversationEvent.TapMicrophone(side, discard))
    }

    /** A misheard turn, corrected by the person: translated again (story 5, scenario 4). */
    fun edit(
        turnId: Long,
        text: String,
    ) = dispatch(ConversationEvent.Edited(turnId, text))

    fun setLanguage(
        side: Side,
        language: LanguageCode,
    ) {
        viewModelScope.launch {
            repository.update { if (side == Side.NEAR) it.copy(near = language) else it.copy(far = language) }
        }
    }

    fun setOption(
        option: ConversationOption,
        on: Boolean,
    ) {
        when (option) {
            ConversationOption.FAR_TURNED -> {
                viewModelScope.launch { repository.update { it.copy(farHalfTurned = on) } }
            }

            ConversationOption.READ_ALOUD -> {
                ui.update { it.copy(readAloud = on) }
            }

            ConversationOption.CONFIRM -> {
                viewModelScope.launch { repository.update { it.copy(confirmBeforeSending = on) } }
            }
        }
    }

    /** A translated turn's speaker button: read it again, or stop it while it reads (spec 082 FR-017). */
    fun replay(turnId: Long) {
        val phase = ui.value.conversation.phase
        val reading = phase == ConversationPhase.Speaking(turnId)
        dispatch(if (reading) ConversationEvent.StopSpeaking else ConversationEvent.Replay(turnId))
    }

    /**
     * A speech notice's small × (owner, 2026-11-20): the next notice shows, until the tool closes or the half's
     * language changes. Its other button opens the Manage languages page, where the language is got.
     */
    fun dismiss(need: SetupNeed) {
        val key = need.key ?: return
        ui.update { it.copy(dismissed = it.dismissed + key) }
    }

    fun clear() = dispatch(ConversationEvent.Clear)

    override fun onCleared() {
        turns.stopAll()
        session.release()
    }

    private fun dispatch(event: ConversationEvent) {
        val before = ui.value.conversation
        val after = ConversationMachine.reduce(before, event)
        ui.update { it.copy(conversation = after) }
        if (after.phase != before.phase) turns.follow(before.phase, after.phase)
    }
}
