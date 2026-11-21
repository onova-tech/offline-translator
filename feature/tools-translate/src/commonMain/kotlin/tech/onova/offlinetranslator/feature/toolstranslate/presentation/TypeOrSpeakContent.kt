package tech.onova.offlinetranslator.feature.toolstranslate.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource
import tech.onova.offlinetranslator.core.designsystem.components.AdaptiveToolLayout
import tech.onova.offlinetranslator.core.designsystem.components.BottomAction
import tech.onova.offlinetranslator.core.designsystem.components.BottomActionRow
import tech.onova.offlinetranslator.core.designsystem.components.BottomActionStyle
import tech.onova.offlinetranslator.core.designsystem.components.ChoiceOption
import tech.onova.offlinetranslator.core.designsystem.components.IconAction
import tech.onova.offlinetranslator.core.designsystem.components.MainReading
import tech.onova.offlinetranslator.core.designsystem.components.MenuChoice
import tech.onova.offlinetranslator.core.designsystem.components.MenuEntry
import tech.onova.offlinetranslator.core.designsystem.components.NO_READING
import tech.onova.offlinetranslator.core.designsystem.components.PocketSnackbarHost
import tech.onova.offlinetranslator.core.designsystem.components.PocketTextField
import tech.onova.offlinetranslator.core.designsystem.components.ReservedSlot
import tech.onova.offlinetranslator.core.designsystem.components.ShortNote
import tech.onova.offlinetranslator.core.designsystem.components.WaitingText
import tech.onova.offlinetranslator.core.designsystem.icons.PocketIcons
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing
import tech.onova.offlinetranslator.core.domain.format.DecimalFormatter
import tech.onova.offlinetranslator.core.domain.ports.EngineUnavailable
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.toolapi.rememberNumberSymbols
import tech.onova.offlinetranslator.feature.toolstranslate.domain.PackOffer
import tech.onova.offlinetranslator.feature.toolstranslate.domain.Routes
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TextLimit
import tech.onova.offlinetranslator.feature.toolstranslate.resources.Res
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_basic_quality
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_copied
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_copy
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_counter
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_damaged
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_empty
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_empty_speak
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_failed
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_from
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_listening
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_not_now_caption
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_note_over_limit
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_note_same_language
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_note_through_english
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_over_limit
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_read_aloud
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_share
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_stop
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_swap
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_text
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_to
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_to_download
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_translation
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_unsupported
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_working

/**
 * Type or speak (spec 012 story 1, contracts/ui.md): the translation in the middle, the only part that scrolls; at the
 * bottom the languages with their swap, the text with its counter, then Copy and Share outlined and Read aloud filled
 * and last (A1–A4).
 */
@Composable
internal fun TypeOrSpeakContent(
    ui: TranslatorUi,
    top: @Composable ColumnScope.() -> Unit,
    actions: TypeOrSpeakActions,
    packActions: PackActions,
    speech: SpeechControls,
    effects: Flow<TranslatorEffect>,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val copied = stringResource(Res.string.translator_copied)
    LaunchedEffect(effects, copied) { effects.collect { snackbarHostState.showSnackbar(copied) } }
    val clipLabel = stringResource(Res.string.translator_translation)
    AdaptiveToolLayout(
        top = top,
        bottom = {
            LanguageRow(ui, actions.onLanguage, actions.onSwap, actions.moreLanguages)
            speech.permissionGate?.invoke()
            TextInput(
                ui = ui,
                onText = actions.onText,
                listening = speech.ui.listening,
                trailing = if (speech.canSpeak) ({ MicrophoneAction(speech) }) else null,
            )
            TranslationActions(ui.translation != null, speech, onSend = { actions.onSend(it, clipLabel) })
        },
        snackbarHost = { PocketSnackbarHost(hostState = snackbarHostState) },
    ) {
        speech.ui.problem?.let { problem ->
            SpeechNotice(problem, languageName(ui.source), languageName(ui.target), speech)
        }
        // "Not now" puts the offer away until the person picks other languages.
        var notNow by rememberSaveable(ui.source.value, ui.target.value) { mutableStateOf(false) }
        val view = ui.view
        if (view is TranslationView.NeedsPack && view.offer is PackOffer.Offer && notNow) {
            val language = Routes.languageOf(view.offer.pack)?.let { languageName(it) } ?: view.offer.pack.value
            MainReading(value = NO_READING, caption = stringResource(Res.string.translator_not_now_caption, language))
        } else if (view is TranslationView.NeedsPack) {
            PackOfferNotice(view.offer, packActions, onNotNow = { notNow = true })
        } else {
            TranslationArea(view, canSpeak = speech.canSpeak)
        }
    }
}

/** Copy and Share outlined, then Read aloud filled and last, which reads Stop while it speaks (A1–A3, FR-024). */
@Composable
private fun TranslationActions(
    ready: Boolean,
    speech: SpeechControls,
    onSend: (Send) -> Unit,
) {
    BottomActionRow {
        BottomAction(
            label = stringResource(Res.string.translator_copy),
            onClick = { onSend(Send.COPY) },
            enabled = ready,
            compact = true,
            style = BottomActionStyle.Outlined,
            modifier = Modifier.weight(1f),
        )
        BottomAction(
            label = stringResource(Res.string.translator_share),
            onClick = { onSend(Send.SHARE) },
            enabled = ready,
            compact = true,
            style = BottomActionStyle.Outlined,
            modifier = Modifier.weight(1f),
        )
        BottomAction(
            label =
                stringResource(
                    if (speech.ui.speaking) Res.string.translator_stop else Res.string.translator_read_aloud,
                ),
            onClick = speech.onReadAloud,
            enabled = ready || speech.ui.speaking,
            compact = true,
            modifier = Modifier.weight(1f),
        )
    }
}

/** From ( Portuguese ▾ )  ⇄  To ( English ▾ ): each list has the installed languages first. */
@Composable
private fun LanguageRow(
    ui: TranslatorUi,
    onLanguage: (LanguageSide, LanguageCode) -> Unit,
    onSwap: () -> Unit,
    more: MenuEntry,
) {
    val options = languageOptions(ui.languages)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PocketSpacing.xs),
    ) {
        MenuChoice(
            options = options,
            selected = ui.source,
            onSelect = { onLanguage(LanguageSide.SOURCE, it) },
            name = stringResource(Res.string.translator_from),
            fillWidth = true,
            modifier = Modifier.weight(1f),
            last = more,
        )
        IconAction(icon = PocketIcons.Swap, label = stringResource(Res.string.translator_swap), onClick = onSwap)
        MenuChoice(
            options = options,
            selected = ui.target,
            onSelect = { onLanguage(LanguageSide.TARGET, it) },
            name = stringResource(Res.string.translator_to),
            fillWidth = true,
            modifier = Modifier.weight(1f),
            last = more,
        )
    }
}

/**
 * Each language with its name, those on the phone first, each group by name (spec 080); one to download shows its size
 * in the list (not in the button).
 */
@Composable
internal fun languageOptions(languages: List<LanguageOption>): List<ChoiceOption<LanguageCode>> =
    languages
        .map { option ->
            val size =
                option.downloadMegabytes?.let {
                    stringResource(
                        Res.string.translator_to_download,
                        it.toString(),
                    )
                }
            option to ChoiceOption(option.code, languageName(option.code), detail = size)
        }.sortedWith(compareBy({ !it.first.installed }, { sortable(it.second.label) }))
        .map { it.second }

/**
 * A few lines, always as tall, with the counter under it in the error colour past the limit (FR-006). While it listens
 * and nothing is heard yet: "Listening." "Listening.." "Listening..." (owner, 2026-11-19).
 */
@Composable
private fun TextInput(
    ui: TranslatorUi,
    onText: (String) -> Unit,
    listening: Boolean,
    trailing: (@Composable () -> Unit)?,
) {
    val listeningWord = stringResource(Res.string.translator_listening)
    val grouping = rememberNumberSymbols().grouping
    val limit = DecimalFormatter.groupInteger(TextLimit.MAX_CHARACTERS.toString(), grouping)
    val count = DecimalFormatter.groupInteger(ui.count.toString(), grouping)
    PocketTextField(
        value = ui.text,
        onValueChange = onText,
        label = { Text(stringResource(Res.string.translator_text)) },
        supportingText = {
            Text(
                text = stringResource(Res.string.translator_counter, count, limit),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        errorText = if (ui.overLimit) stringResource(Res.string.translator_over_limit, limit) else null,
        minLines = TEXT_LINES,
        maxLines = TEXT_LINES,
        placeholder = if (listening && ui.text.isEmpty()) ({ WaitingText(word = listeningWord) }) else null,
        trailingIcon = trailing,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The translation, large and selectable, announced once when it's done (FR-050); or why there's none. */
@Composable
private fun TranslationArea(
    view: TranslationView,
    canSpeak: Boolean,
) {
    when (view) {
        TranslationView.Empty -> {
            val caption = if (canSpeak) Res.string.translator_empty_speak else Res.string.translator_empty
            MainReading(value = NO_READING, caption = stringResource(caption))
        }

        TranslationView.Working -> {
            MainReading(value = NO_READING, caption = stringResource(Res.string.translator_working))
        }

        is TranslationView.Shown -> {
            SelectionContainer {
                Text(
                    text = view.text,
                    style = MaterialTheme.typography.headlineSmall.inItsOwnDirection(),
                    modifier =
                        Modifier.fillMaxWidth().semantics {
                            if (!view.pending) liveRegion = LiveRegionMode.Polite
                        },
                )
            }
            notesText(view.notes)?.let { ShortNote(text = it, modifier = Modifier.fillMaxWidth()) }
            // A newer translation is on its way (story 1, scenario 7): a thin line under this one, in room that is
            // always kept, so nothing moves when it comes and goes (M1–M5).
            val working = stringResource(Res.string.translator_working)
            ReservedSlot(shown = view.pending, keepInSmallWindow = true) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().semantics { contentDescription = working })
            }
        }

        // Shown by the caller, with its actions.
        is TranslationView.NeedsPack -> {}

        TranslationView.Failed -> {
            MainReading(value = NO_READING, caption = stringResource(Res.string.translator_failed))
        }

        is TranslationView.Unavailable -> {
            val reason =
                when (view.reason) {
                    EngineUnavailable.UNSUPPORTED_DEVICE -> Res.string.translator_unsupported
                    EngineUnavailable.DAMAGED_PACK -> Res.string.translator_damaged
                }
            MainReading(value = NO_READING, caption = stringResource(reason))
        }
    }
}

/**
 * Text in any language takes its own direction (spec 080 FR-011, research R6): Arabic, Persian, Hebrew, Urdu or Uyghur
 * is laid out right to left and starts at the right; Latin text reads as before.
 */
internal fun TextStyle.inItsOwnDirection(): TextStyle = copy(textDirection = TextDirection.Content)

/** The one short line under a translation: what was left out first, then how it was translated (R3). */
@Composable
private fun notesText(notes: Set<TranslationNote>): String? {
    val limit = DecimalFormatter.groupInteger(TextLimit.MAX_CHARACTERS.toString(), rememberNumberSymbols().grouping)
    val overLimit = stringResource(Res.string.translator_note_over_limit, limit)
    val throughEnglish = stringResource(Res.string.translator_note_through_english)
    val sameLanguage = stringResource(Res.string.translator_note_same_language)
    val basicQuality = stringResource(Res.string.translator_basic_quality)
    val lines =
        buildList {
            if (TranslationNote.OVER_LIMIT in notes) add(overLimit)
            if (TranslationNote.THROUGH_ENGLISH in notes) add(throughEnglish)
            if (TranslationNote.SAME_LANGUAGE in notes) add(sameLanguage)
            if (TranslationNote.BASIC_QUALITY in notes) add(basicQuality)
        }
    return lines.takeIf { it.isNotEmpty() }?.joinToString(NOTE_SEPARATOR)
}

private const val TEXT_LINES = 3
private const val NOTE_SEPARATOR = " · "
