package tech.onova.offlinetranslator.feature.toolstranslate.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.onova.offlinetranslator.core.domain.ports.AppVisibility
import tech.onova.offlinetranslator.core.domain.ports.ClipboardWriter
import tech.onova.offlinetranslator.core.domain.ports.ContentPacks
import tech.onova.offlinetranslator.core.domain.ports.EngineUnavailable
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.PackState
import tech.onova.offlinetranslator.core.domain.ports.PackStatus
import tech.onova.offlinetranslator.core.domain.ports.Route
import tech.onova.offlinetranslator.core.domain.translation.LanguageCatalog
import tech.onova.offlinetranslator.core.domain.translation.LanguageQuality
import tech.onova.offlinetranslator.feature.toolstranslate.domain.EngineSession
import tech.onova.offlinetranslator.feature.toolstranslate.domain.PackMessages
import tech.onova.offlinetranslator.feature.toolstranslate.domain.PackOffer
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ResolvedSettings
import tech.onova.offlinetranslator.feature.toolstranslate.domain.Routes
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TextLimit
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslationInput
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorDefaults
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorMode
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorSettingsRepository

/** What a translation's one short note says (spec 012 FR-004 to FR-006). */
internal enum class TranslationNote { THROUGH_ENGLISH, SAME_LANGUAGE, OVER_LIMIT, BASIC_QUALITY }

/** What the middle of Type or speak shows. */
internal sealed interface TranslationView {
    /** Nothing typed: no reading, with a caption. */
    data object Empty : TranslationView

    /** The first translation is on its way. */
    data object Working : TranslationView

    /** [pending]: a newer one is on its way, so this one isn't announced again. */
    data class Shown(
        val text: String,
        val notes: Set<TranslationNote>,
        val pending: Boolean = false,
    ) : TranslationView

    /** A pack the pair needs isn't installed: its offer takes the translation's place (story 2). */
    data class NeedsPack(
        val offer: PackOffer,
    ) : TranslationView

    data object Failed : TranslationView

    data class Unavailable(
        val reason: EngineUnavailable,
    ) : TranslationView
}

/** A language in the two lists: installed ones first, then those to download with their size. */
internal data class LanguageOption(
    val code: LanguageCode,
    val installed: Boolean,
    val downloadMegabytes: Int?,
)

internal enum class LanguageSide { SOURCE, TARGET }

internal sealed interface TranslatorEffect {
    /** The phone didn't confirm the copy itself, so the screen does. */
    data object Copied : TranslatorEffect
}

internal data class TranslatorUi(
    val loaded: Boolean = false,
    val mode: TranslatorMode = TranslatorMode.TYPE_OR_SPEAK,
    val source: LanguageCode = LanguageCode.ENGLISH,
    val target: LanguageCode = LanguageCode.ENGLISH,
    val languages: List<LanguageOption> = emptyList(),
    val text: String = "",
    val view: TranslationView = TranslationView.Empty,
) {
    val count: Int get() = TextLimit.count(text)
    val overLimit: Boolean get() = TextLimit.isOver(count)

    /** What Copy and Read aloud take: the translation on the screen. */
    val translation: String? get() = (view as? TranslationView.Shown)?.text
}

/** Where a translation goes besides the screen: the clipboard (Share left the Translator, spec 082 FR-015). */
internal class TranslatorOutputs(
    val clipboard: ClipboardWriter,
)

/** One translation's inputs, compared as a whole so only a change that matters starts another. */
private data class Step(
    val input: TranslationInput?,
    val route: Route,
    val offer: PackOffer?,
    val readiness: EngineSession.Readiness,
    val unavailable: EngineUnavailable?,
    val visible: Boolean,
)

/**
 * Type or speak (spec 012 story 1): the text waits [TextLimit.PAUSE_MILLIS] after the last key, the newest request
 * wins, and the engine is open while this screen exists. The languages and the mode are remembered; the text and the
 * translation never are (FR-003). A hidden screen translates nothing: what was running is dropped, and the text is
 * translated again when the screen shows (spec edge case "Memory").
 */
@OptIn(FlowPreview::class)
internal class TranslatorViewModel(
    private val repository: TranslatorSettingsRepository,
    private val packs: ContentPacks,
    private val session: EngineSession,
    private val outputs: TranslatorOutputs,
    private val visibility: AppVisibility,
) : ViewModel() {
    private val appLanguage = MutableStateFlow<LanguageCode?>(null)
    private val text = MutableStateFlow("")
    private val view = MutableStateFlow<TranslationView>(TranslationView.Empty)

    /** The step whose translation is on the screen, so showing the screen again doesn't translate it again. */
    private var finished: Step? = null

    /** The mode the person just chose: shown at once, while the store saves it. */
    private val chosenMode = MutableStateFlow<TranslatorMode?>(null)
    private val effectChannel = MutableSharedFlow<TranslatorEffect>(extraBufferCapacity = EFFECT_BUFFER)
    val effects: SharedFlow<TranslatorEffect> = effectChannel.asSharedFlow()

    private val resolved: Flow<ResolvedSettings> =
        combine(repository.settings, appLanguage.filterNotNull()) { settings, language ->
            TranslatorDefaults.resolve(settings, language)
        }

    val uiState: StateFlow<TranslatorUi> =
        combine(resolved, text, view, packs.packs, chosenMode) { settings, typed, shown, statuses, chosen ->
            TranslatorUi(
                loaded = true,
                mode = chosen ?: settings.mode,
                source = settings.source,
                target = settings.target,
                languages = languagesOf(statuses, settings.languages),
                text = typed,
                view = shown,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TranslatorUi())

    init {
        session.acquire()
        viewModelScope.launch {
            val paused = text.debounce { if (it.isBlank()) 0L else TextLimit.PAUSE_MILLIS }
            val pair = resolved.map { it.source to it.target }.distinctUntilChanged()
            val shown = visibility.isVisible
            combine(paused, pair, packs.packs, session.state, shown) { typed, (from, to), statuses, engine, visible ->
                val route = Routes.between(from, to)
                Step(
                    input = TextLimit.prepare(typed),
                    route = route,
                    offer = PackMessages.firstOffer(route, statuses),
                    readiness = engine.readiness(route),
                    unavailable = (engine as? EngineSession.State.Unavailable)?.reason,
                    visible = visible,
                )
            }.distinctUntilChanged().collectLatest(::show)
        }
    }

    /** The app's language as the screen shows it ("pt"): the default source until the person chooses one. */
    fun setAppLanguage(language: String) {
        appLanguage.value = LanguageCode(language)
    }

    /** A translation on the screen is marked as being replaced at once, so it isn't announced while typing. */
    fun setText(value: String) {
        val shown = view.value
        if (value != text.value && shown is TranslationView.Shown) view.value = shown.copy(pending = true)
        text.value = value
    }

    fun choose(
        side: LanguageSide,
        language: LanguageCode,
    ) {
        update { if (side == LanguageSide.SOURCE) it.copy(source = language) else it.copy(target = language) }
    }

    /** The languages trade places, and the translation becomes the text (FR-007). */
    fun swap() {
        val ui = uiState.value
        update { it.copy(source = ui.target, target = ui.source) }
        ui.translation?.let { text.value = it }
    }

    fun setMode(mode: TranslatorMode) {
        chosenMode.value = mode
        update { it.copy(mode = mode) }
    }

    /** Copies the translation; [label] names the clip for the system. */
    fun copy(label: String) {
        val translation = uiState.value.translation ?: return
        if (!outputs.clipboard.copy(label, translation)) effectChannel.tryEmit(TranslatorEffect.Copied)
    }

    override fun onCleared() {
        session.release()
    }

    /** Shows what [step] can show at once; then, when the engine is ready, its translation. */
    private suspend fun show(step: Step) {
        // Hidden: collectLatest has dropped what was running; nothing starts until the screen shows again.
        if (!step.visible) return
        val shownNow = view.value
        if (step == finished && shownNow is TranslationView.Shown && !shownNow.pending) return
        val input = step.input
        val settled = settledView(step)
        if (settled != null) {
            view.value = settled
        } else if (input != null) {
            view.value = pending(view.value)
            if (step.readiness == EngineSession.Readiness.READY) {
                view.value =
                    try {
                        val translation = session.translate(input.text, step.route)
                        // Keys typed while it ran: a newer one follows, so this one isn't announced (FR-050).
                        val stale = TextLimit.prepare(text.value) != input
                        finished = step
                        TranslationView.Shown(translation, notesFor(step.route, input), pending = stale)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (ignored: IllegalStateException) {
                        TranslationView.Failed
                    }
            }
        }
    }

    private fun update(change: (ResolvedSettings) -> ResolvedSettings) {
        val language = appLanguage.value
        viewModelScope.launch {
            repository.update { stored ->
                val now = TranslatorDefaults.resolve(stored, language)
                val next = change(now)
                stored.copy(source = next.source, target = next.target, mode = next.mode)
            }
        }
    }

    private companion object {
        const val EFFECT_BUFFER = 8
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * What the screen shows without asking the engine: a missing pack (offered before anything is typed, so it can be
 * downloaded first), nothing typed, the same language, no engine.
 */
private fun settledView(step: Step): TranslationView? {
    val input = step.input
    return when {
        step.offer != null -> TranslationView.NeedsPack(step.offer)
        input == null -> TranslationView.Empty
        step.route == Route.Same -> TranslationView.Shown(input.text, notesFor(step.route, input))
        step.unavailable != null -> TranslationView.Unavailable(step.unavailable)
        else -> null
    }
}

/** The translation on the screen stays, marked as being replaced; with none yet, "translating". */
private fun pending(current: TranslationView): TranslationView =
    if (current is TranslationView.Shown) current.copy(pending = true) else TranslationView.Working

private fun notesFor(
    route: Route,
    input: TranslationInput,
): Set<TranslationNote> =
    buildSet {
        if (route == Route.Same) add(TranslationNote.SAME_LANGUAGE)
        if (route is Route.ThroughEnglish) add(TranslationNote.THROUGH_ENGLISH)
        if (input.overLimit) add(TranslationNote.OVER_LIMIT)
        if (basic(route)) add(TranslationNote.BASIC_QUALITY)
    }

/** Spec 080 FR-006: a translation from or into a language with only the small model says so. */
private fun basic(route: Route): Boolean {
    val languages =
        when (route) {
            Route.Same -> emptyList()
            is Route.Direct -> listOf(route.from, route.to)
            is Route.ThroughEnglish -> listOf(route.from, route.to)
        }
    return languages.any { LanguageCatalog.of(it)?.quality == LanguageQuality.BASIC }
}

/**
 * The checked languages (spec 080), each with its pack's state: on the phone (English always), or to download with its
 * size. The screen orders them: those on the phone first, each group by name.
 */
internal fun languagesOf(
    statuses: List<PackStatus>,
    checked: List<LanguageCode>,
): List<LanguageOption> =
    checked.map { code ->
        val pack = LanguageCatalog.of(code)?.pack
        val status = pack?.let { id -> statuses.firstOrNull { it.id == id } }
        val installed = pack == null || status?.state == PackState.Installed
        val size = if (installed) null else status?.downloadBytes?.let(PackMessages::megabytes)
        LanguageOption(code, installed, size)
    }
