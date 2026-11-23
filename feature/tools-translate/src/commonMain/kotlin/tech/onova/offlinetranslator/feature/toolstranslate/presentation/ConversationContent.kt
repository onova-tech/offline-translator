package tech.onova.offlinetranslator.feature.toolstranslate.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import tech.onova.offlinetranslator.core.designsystem.components.AdaptiveToolLayout
import tech.onova.offlinetranslator.core.designsystem.components.BottomAction
import tech.onova.offlinetranslator.core.designsystem.components.BottomActionRow
import tech.onova.offlinetranslator.core.designsystem.components.BottomActionStyle
import tech.onova.offlinetranslator.core.designsystem.components.ConfirmScreen
import tech.onova.offlinetranslator.core.designsystem.components.FacingHalves
import tech.onova.offlinetranslator.core.designsystem.components.FullScreenPage
import tech.onova.offlinetranslator.core.designsystem.components.HearAgainButton
import tech.onova.offlinetranslator.core.designsystem.components.MainReading
import tech.onova.offlinetranslator.core.designsystem.components.MenuChoice
import tech.onova.offlinetranslator.core.designsystem.components.MenuEntry
import tech.onova.offlinetranslator.core.designsystem.components.MicrophoneButton
import tech.onova.offlinetranslator.core.designsystem.components.NO_READING
import tech.onova.offlinetranslator.core.designsystem.components.Notice
import tech.onova.offlinetranslator.core.designsystem.components.PocketSnackbarHost
import tech.onova.offlinetranslator.core.designsystem.components.PocketTextField
import tech.onova.offlinetranslator.core.designsystem.components.PopupCard
import tech.onova.offlinetranslator.core.designsystem.components.ReservedSlot
import tech.onova.offlinetranslator.core.designsystem.components.SendButton
import tech.onova.offlinetranslator.core.designsystem.components.StopButton
import tech.onova.offlinetranslator.core.designsystem.components.SwitchRow
import tech.onova.offlinetranslator.core.designsystem.components.ToolLayout
import tech.onova.offlinetranslator.core.designsystem.components.WaitingText
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketTypography
import tech.onova.offlinetranslator.core.domain.permissions.AppPermission
import tech.onova.offlinetranslator.core.domain.permissions.PermissionController
import tech.onova.offlinetranslator.core.domain.ports.EngineUnavailable
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.translation.TurnLabels
import tech.onova.offlinetranslator.core.toolapi.KeepScreenOn
import tech.onova.offlinetranslator.core.toolapi.MoreControlsPanel
import tech.onova.offlinetranslator.core.toolapi.MoreOptionsAction
import tech.onova.offlinetranslator.core.toolapi.PermissionGate
import tech.onova.offlinetranslator.core.toolapi.rememberPermissionGranted
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationPhase
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationProblem
import tech.onova.offlinetranslator.feature.toolstranslate.domain.ConversationText
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SetupNeed
import tech.onova.offlinetranslator.feature.toolstranslate.domain.Side
import tech.onova.offlinetranslator.feature.toolstranslate.domain.Turn
import tech.onova.offlinetranslator.feature.toolstranslate.resources.Res
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_cancel
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_clear
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_clear_text
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_clear_title
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_confirm_send
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_conversation_start
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_conversation_unsupported
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_copied
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_copy
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_correct
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_damaged
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_discard_turn
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_dismiss_notice
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_far_language
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_far_turned
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_far_turned_supporting
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_hear_again
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_keep_correction
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_listening
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_listening_failed
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_manage_languages
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_microphone_busy
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_microphone_denied
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_microphone_rationale
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_mode_conversation
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_near_language
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_no_voice
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_nothing_heard
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_paused
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_read_translations
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_send
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_speak_language
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_speech_app_can_download
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_speech_app_downloading
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_speech_can_download
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_speech_downloading
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_speech_not_installed
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_stop
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_translate_again
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_turn_failed
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_unsupported
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_voice_app_can_download
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_voice_app_downloading
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_what_was_said

/** A half's language list: choosing a language, and "Other languages…" at its end (spec 080), turned with its half. */
private class HalfLanguage(
    val onSelect: (Side, LanguageCode) -> Unit,
    val more: MenuEntry,
)

/** A speech notice's "Manage languages", at its half's language, and its small × (owner, 2026-11-20). */
private class SetupActions(
    val onManage: (LanguageCode) -> Unit,
    val onDismiss: (SetupNeed) -> Unit,
)

/** What a half's controls do. */
private class HalfActions(
    val language: HalfLanguage,
    val onMicrophone: (Side) -> Unit,
    /** The square button beside a listening microphone: stop, and drop what was being heard. */
    val onDiscard: (Side) -> Unit,
    val setup: SetupActions,
    val turns: TurnActions,
    val packs: PackActions,
)

/** What a turn's taps do: one's own corrects it; the half's latest translation is read again or stopped (FR-017). */
private class TurnActions(
    val onCorrect: (Turn) -> Unit,
    val onReplay: (Long) -> Unit,
)

/**
 * The Conversation (spec 012 story 5, contracts/ui.md, `docs/ui-rules.md` C1): two facing halves, each with its
 * language, its turns and its large microphone; the far one turned to the person across. Below them, at the bottom
 * edge, More options and Copy, Share and Clear, all outlined: the microphones are the main actions (A3 exception).
 * The screen stays on while it's open (FR-028).
 */
@Composable
internal fun ConversationContent(
    top: @Composable ColumnScope.() -> Unit,
    packActions: PackActions,
    moreLanguages: MenuEntry,
    /** The Manage languages page, at a language (a speech notice's button). */
    onManage: (LanguageCode) -> Unit,
    viewModel: ConversationViewModel = koinViewModel(),
) {
    KeepScreenOn()
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val appLanguage = Locale.current.language
    val phoneLocales = LocaleList.current.localeList.map { it.toLanguageTag() }
    LaunchedEffect(appLanguage, phoneLocales) { viewModel.start(appLanguage, phoneLocales) }
    val snackbarHostState = remember { SnackbarHostState() }
    val copied = stringResource(Res.string.translator_copied)
    LaunchedEffect(viewModel) { viewModel.clipboard.effects.collect { snackbarHostState.showSnackbar(copied) } }
    var correcting by rememberSaveable { mutableStateOf<Long?>(null) }
    var clearing by rememberSaveable { mutableStateOf(false) }
    val microphone = rememberMicrophoneGate(viewModel::tap)
    val actions =
        HalfActions(
            language = HalfLanguage(onSelect = viewModel::setLanguage, more = moreLanguages),
            onMicrophone = microphone.onTap,
            onDiscard = { side -> viewModel.tap(side, discard = true) },
            setup = SetupActions(onManage = onManage, onDismiss = viewModel::dismiss),
            turns = TurnActions(onCorrect = { turn -> correcting = turn.id }, onReplay = viewModel::replay),
            packs = packActions,
        )
    val gate = microphone.gate
    AdaptiveToolLayout(
        top = top,
        bottom = {
            ConversationControls(
                ui = ui,
                onOption = viewModel::setOption,
                onCopy = viewModel.clipboard::copy,
                onClear = { clearing = true },
            )
        },
        scrollContent = false,
        snackbarHost = { PocketSnackbarHost(hostState = snackbarHostState) },
    ) {
        if (ui.loaded) {
            FacingHalves(
                farTurned = ui.farTurned,
                far = { Half(Side.FAR, ui, actions, gate) },
                near = { Half(Side.NEAR, ui, actions, gate) },
            )
        }
    }
    Correction(correcting, ui, onTranslate = viewModel::edit, onClose = { correcting = null })
    if (clearing) {
        ClearConfirmation(
            onClear = {
                clearing = false
                viewModel.clear()
            },
            onCancel = { clearing = false },
        )
    }
}

/** The turn being corrected, if it's still there. */
@Composable
private fun Correction(
    turnId: Long?,
    ui: ConversationUi,
    onTranslate: (Long, String) -> Unit,
    onClose: () -> Unit,
) {
    val id = turnId ?: return
    val turn = ui.conversation.turns.firstOrNull { it.id == id }
    // Cleared while open: a later turn may take its id, so the dialog closes.
    val close by rememberUpdatedState(onClose)
    if (turn == null) {
        LaunchedEffect(id) { close() }
        return
    }
    CorrectionDialog(
        heard = turn.heard,
        waiting = ui.waiting?.id == id,
        turned = turn.side == Side.FAR && ui.farTurned,
        onTranslate = { text ->
            onClose()
            onTranslate(id, text)
        },
        onCancel = onClose,
    )
}

/** The microphones' tap, and the permission card shown inside the half that was tapped until it's answered (FR-020). */
private class MicrophoneGate(
    val onTap: (Side) -> Unit,
    val gate: @Composable (Side) -> Unit,
)

@Composable
private fun rememberMicrophoneGate(onListen: (Side) -> Unit): MicrophoneGate {
    val permissions: PermissionController = koinInject()
    val granted = rememberPermissionGranted(AppPermission.RECORD_AUDIO, permissions)
    var asking by rememberSaveable { mutableStateOf<Side?>(null) }
    val rationale = stringResource(Res.string.translator_microphone_rationale)
    return MicrophoneGate(
        onTap = { side -> if (granted) onListen(side) else asking = side },
        gate = { side ->
            if (asking == side) {
                PermissionGate(
                    permission = AppPermission.RECORD_AUDIO,
                    rationale = rationale,
                    controller = permissions,
                    onGrant = {
                        asking = null
                        onListen(side)
                    },
                    onDismiss = { asking = null },
                )
            }
        },
    )
}

@Composable
private fun ClearConfirmation(
    onClear: () -> Unit,
    onCancel: () -> Unit,
) {
    ConfirmScreen(
        title = stringResource(Res.string.translator_clear_title),
        text = stringResource(Res.string.translator_clear_text),
        confirmLabel = stringResource(Res.string.translator_clear),
        cancelLabel = stringResource(Res.string.translator_cancel),
        onConfirm = onClear,
        onCancel = onCancel,
    )
}

/** One half, from its own speaker's point of view: its language, its turns, its microphone (C1). */
@Composable
private fun ColumnScope.Half(
    side: Side,
    ui: ConversationUi,
    actions: HalfActions,
    gate: @Composable (Side) -> Unit,
) {
    val language = ui.conversation.languageOf(side)
    val name = languageName(language)
    val label = if (side == Side.NEAR) Res.string.translator_near_language else Res.string.translator_far_language
    val nameLabel = stringResource(label)
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = PocketSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PocketSpacing.xs),
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            HalfMiddle(side, ui, actions, gate)
        }
        // At the speaker's own edge: their language, and their microphone beside it.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PocketSpacing.sm),
        ) {
            MenuChoice(
                options = languageOptions(ui.languages),
                selected = language,
                onSelect = { actions.language.onSelect(side, it) },
                name = nameLabel,
                modifier = Modifier.weight(1f),
                last = actions.language.more,
            )
            val phase = ui.conversation.phase
            val listening = phase is ConversationPhase.Listening && phase.side == side
            val waiting = ui.waiting?.side == side
            HalfSlot(side, ui, listening || waiting, actions)
            if (waiting) {
                // In the microphone's place, so the microphone's tap sends it (spec 082 FR-016).
                SendButton(label = stringResource(Res.string.translator_send), onClick = { actions.onMicrophone(side) })
            } else {
                MicrophoneButton(
                    label = stringResource(Res.string.translator_speak_language, name),
                    listening = listening,
                    enabled = ui.microphoneEnabled(side) && ui.supportOf(side).canListen,
                    onClick = { actions.onMicrophone(side) },
                    listeningLabel = stringResource(Res.string.translator_listening),
                )
            }
        }
    }
}

/**
 * Beside the microphone, in room always kept so nothing moves (owner, 2026-11-19): the red square while the half
 * listens or its turn waits (stop and drop it), or while its translation is read (stop reading); otherwise the half's
 * latest translation heard again (spec 082 FR-017, owner 2026-11-22: "Make it appear on the same position as the
 * square button does, but with other color"), enabled only with the language's voice ready.
 */
@Composable
private fun HalfSlot(
    side: Side,
    ui: ConversationUi,
    hearing: Boolean,
    actions: HalfActions,
) {
    val turns = ui.conversation.turns
    val reading = (ui.conversation.phase as? ConversationPhase.Speaking)?.turnId
    val readHere = reading != null && turns.firstOrNull { it.id == reading }?.side == side.other
    val latest = turns.lastOrNull { it.side == side.other && it.translation != null }
    ReservedSlot(shown = hearing || readHere || latest != null, keepInSmallWindow = true) {
        when {
            hearing -> {
                StopButton(
                    label = stringResource(Res.string.translator_discard_turn),
                    onClick = { actions.onDiscard(side) },
                )
            }

            readHere && reading != null -> {
                StopButton(
                    label = stringResource(Res.string.translator_stop),
                    onClick = { actions.turns.onReplay(reading) },
                )
            }

            latest != null -> {
                HearAgainButton(
                    label = stringResource(Res.string.translator_hear_again),
                    onClick = { actions.turns.onReplay(latest.id) },
                    enabled = ui.canReplay(side),
                )
            }

            // Nothing to show yet: the button's size still holds the room, hidden, so the language button keeps its
            // width when the square or the speaker comes (M1–M3; the final review of spec 082).
            else -> {
                HearAgainButton(label = stringResource(Res.string.translator_hear_again), onClick = {}, enabled = false)
            }
        }
    }
}

/**
 * Between the language and the microphone: the turns, and their speaker's own messages. What the phone's owner sets up
 * shows in the near half only, upright and one thing at a time, for both languages (owner, 2026-11-19).
 */
@Composable
private fun HalfMiddle(
    side: Side,
    ui: ConversationUi,
    actions: HalfActions,
    gate: @Composable (Side) -> Unit,
) {
    val setup = ui.setup.takeIf { side == Side.NEAR }
    val name = languageName(ui.conversation.languageOf(side))
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(PocketSpacing.xs)) {
        gate(side)
        when {
            setup is SetupNeed.Pack -> {
                PackOfferNotice(setup.offer, actions.packs, onNotNow = null)
            }

            ui.unavailable != null -> {
                val reason =
                    if (ui.unavailable == EngineUnavailable.UNSUPPORTED_DEVICE) {
                        Res.string.translator_unsupported
                    } else {
                        Res.string.translator_damaged
                    }
                MainReading(value = NO_READING, caption = stringResource(reason))
            }

            else -> {
                setup?.let { SetupNotice(it, ui, actions.setup) }
                ui.conversation.message?.takeIf { it.side == side }?.let { message ->
                    Notice(text = problemText(message.problem, name))
                }
                if (ui.conversation.phase is ConversationPhase.Paused) {
                    Notice(text = stringResource(Res.string.translator_paused))
                }
                TurnList(side, ui, actions, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** What the phone's own speech can't do yet for one of the languages, named, with its fix where there is one. */
@Composable
private fun SetupNotice(
    need: SetupNeed,
    ui: ConversationUi,
    actions: SetupActions,
) {
    // The translation pack's offer shows in the turns' place, by the caller.
    val side = need.key?.side ?: return
    val name = languageName(ui.conversation.languageOf(side))
    val text =
        when (need) {
            is SetupNeed.Recognition -> {
                val words =
                    if (need.downloading) {
                        Res.string.translator_speech_downloading
                    } else {
                        Res.string.translator_speech_can_download
                    }
                stringResource(words, name)
            }

            is SetupNeed.NotInstalled -> {
                stringResource(Res.string.translator_speech_not_installed, name)
            }

            is SetupNeed.Voice -> {
                stringResource(Res.string.translator_no_voice, name)
            }

            is SetupNeed.AppRecognition -> {
                if (need.downloading) {
                    stringResource(Res.string.translator_speech_app_downloading)
                } else {
                    stringResource(Res.string.translator_speech_app_can_download, name)
                }
            }

            is SetupNeed.AppVoice -> {
                val words =
                    if (need.downloading) {
                        Res.string.translator_voice_app_downloading
                    } else {
                        Res.string.translator_voice_app_can_download
                    }
                stringResource(words, name)
            }

            is SetupNeed.NoRecognition, is SetupNeed.Pack -> {
                stringResource(Res.string.translator_conversation_unsupported, name)
            }
        }
    val language = ui.conversation.languageOf(side)
    // Owner, 2026-11-20: the language's speech and voice are got on the Manage languages page; a small × closes it
    // until the tool reopens or the half's language is chosen again.
    // A notice taller than the half's room scrolls inside, rather than squeeze its button (owner, 2026-11-20).
    Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Notice(
            text = text,
            onClose = { actions.onDismiss(need) },
            closeLabel = stringResource(Res.string.translator_dismiss_notice),
        ) {
            TextButton(
                onClick = { actions.onManage(language) },
            ) { Text(stringResource(Res.string.translator_manage_languages)) }
        }
    }
}

/**
 * The half's turns, the newest in view, all at one size after "Me:" or "You:" in the half's language (spec 082
 * FR-014): its own as heard (a tap corrects one), the other half's as translated into its language (announced once
 * with TalkBack on), and the words being heard now.
 */
@Composable
private fun TurnList(
    side: Side,
    ui: ConversationUi,
    actions: HalfActions,
    modifier: Modifier = Modifier,
) {
    val waiting = ui.waiting
    // A turn waiting for ^ shows only in its own half: the other person hasn't been sent it (FR-016).
    val turns = ui.conversation.shownTurns.filter { it.id != waiting?.id || it.side == side }
    val phase = ui.conversation.phase
    val partial = (phase as? ConversationPhase.Listening)?.takeIf { it.side == side }?.partial
    val labels = TurnLabels.of(ui.conversation.languageOf(side))
    val list = rememberLazyListState()
    LaunchedEffect(turns.size, partial) { if (turns.isNotEmpty()) list.animateScrollToItem(turns.size) }
    val start = stringResource(Res.string.translator_conversation_start)
    LazyColumn(
        state = list,
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PocketSpacing.xs),
    ) {
        // A list item, so it scrolls in a short half rather than overflow it.
        if (turns.isEmpty() && partial == null) {
            item {
                Text(
                    text = start,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        items(turns, key = { it.id }) { turn ->
            if (turn.side == side) {
                val waits = turn.id == waiting?.id
                val correctable = phase == ConversationPhase.Idle || waits
                OwnTurn(turn, labels.me, waits, correctable, actions.turns.onCorrect)
            } else {
                OtherTurn(
                    turn = turn,
                    label = labels.you,
                    newest = turn.id == turns.last().id,
                    settled = phase == ConversationPhase.Idle,
                )
            }
        }
        partial?.let { words ->
            item {
                if (words.isEmpty()) {
                    // Before the first word: "Listening." "Listening.." "Listening..." (owner, 2026-11-19).
                    val style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic)
                    WaitingText(word = stringResource(Res.string.translator_listening), style = style)
                } else {
                    TurnText(labels.me, words, italic = true)
                }
            }
        }
    }
}

/** What this half's speaker said, as heard, in italics while it waits for ^; a tap corrects it. */
@Composable
private fun OwnTurn(
    turn: Turn,
    label: String,
    waiting: Boolean,
    correctable: Boolean,
    onCorrect: (Turn) -> Unit,
) {
    TurnText(
        label = label,
        text = turn.heard,
        italic = waiting,
        modifier =
            Modifier.clickable(
                onClickLabel = stringResource(Res.string.translator_correct),
                enabled = correctable,
            ) { onCorrect(turn) },
    )
}

/** What the other person said, in this half's language; the newest is announced once it's translated. */
@Composable
private fun OtherTurn(
    turn: Turn,
    label: String,
    newest: Boolean,
    settled: Boolean,
) {
    val failed = stringResource(Res.string.translator_turn_failed)
    TurnText(
        label = label,
        text = turn.translation ?: if (settled) failed else NO_READING,
        modifier =
            Modifier.semantics {
                if (newest && turn.translation != null) liveRegion = LiveRegionMode.Polite
            },
    )
}

/**
 * A turn's words after who said them, in a small bold label of the half's language, every turn at one size (owner,
 * 2026-11-22: "Instead of displaying the other person text with a big font and in bold, add just a small bold
 * You:/Me:"). One text, so a screen reader reads the label first and both take the words' direction.
 */
@Composable
private fun TurnText(
    label: String,
    text: String,
    modifier: Modifier = Modifier,
    italic: Boolean = false,
) {
    val labelStyle = PocketTypography.speakerLabel.toSpanStyle()
    val style = MaterialTheme.typography.bodyLarge
    Text(
        text =
            buildAnnotatedString {
                withStyle(labelStyle) { append(label) }
                append(LABEL_GAP)
                append(text)
            },
        style = (if (italic) style.copy(fontStyle = FontStyle.Italic) else style).inItsOwnDirection(),
        modifier = modifier.fillMaxWidth(),
    )
}

private const val LABEL_GAP = " "

/**
 * More options' switches above the row Clear | More options | Copy: Copy the main action on the right, More options in
 * a colour of its own and in the row to make room (owner, 2026-11-22; Share left).
 */
@Composable
private fun ConversationControls(
    ui: ConversationUi,
    onOption: (ConversationOption, Boolean) -> Unit,
    onCopy: (String, String) -> Unit,
    onClear: () -> Unit,
) {
    val options =
        listOf(
            optionSwitch(
                Res.string.translator_far_turned,
                Res.string.translator_far_turned_supporting,
                ui.farTurned,
            ) { onOption(ConversationOption.FAR_TURNED, it) },
            optionSwitch(Res.string.translator_read_translations, null, ui.readAloud) {
                onOption(ConversationOption.READ_ALOUD, it)
            },
            optionSwitch(Res.string.translator_confirm_send, null, ui.conversation.confirm) {
                onOption(ConversationOption.CONFIRM, it)
            },
        )
    var more by rememberSaveable { mutableStateOf(false) }
    MoreControlsPanel(shown = more, options = options)
    val names = ui.languages.associate { it.code to languageName(it.code) }
    // A turn waiting for ^ hasn't been said: Copy leaves it out.
    val said = ui.conversation.turns.filter { it.id != ui.waiting?.id }
    val text = ConversationText.format(said) { names[it] ?: it.value }
    val label = stringResource(Res.string.translator_mode_conversation)
    val any = ui.conversation.turns.isNotEmpty()
    BottomActionRow {
        BottomAction(
            label = stringResource(Res.string.translator_clear),
            onClick = onClear,
            enabled = any,
            compact = true,
            style = BottomActionStyle.Outlined,
            modifier = Modifier.weight(1f),
        )
        MoreOptionsAction(shown = more, onToggle = { more = !more })
        BottomAction(
            label = stringResource(Res.string.translator_copy),
            onClick = { onCopy(label, text) },
            enabled = said.isNotEmpty(),
            compact = true,
            modifier = Modifier.weight(1f),
        )
    }
}

/** One of More options' switches. */
private fun optionSwitch(
    title: StringResource,
    supporting: StringResource?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
): @Composable ColumnScope.() -> Unit =
    {
        SwitchRow(
            title = stringResource(title),
            supporting = supporting?.let { stringResource(it) },
            checked = checked,
            onCheckedChange = onChange,
            modifier = Modifier.fillMaxWidth(),
        )
    }

/**
 * A misheard turn, corrected in a popup in the middle of the screen and translated again (owner, 2026-11-23, an
 * exception to L5's full-screen dialogs), turned to the person across for their own turn when their half is turned.
 */
@Composable
private fun CorrectionDialog(
    heard: String,
    /** A turn waiting for ^ keeps waiting once corrected: its button saves rather than translates (FR-016). */
    waiting: Boolean,
    /** The far half's turn, that half turned to the person across: the popup faces them too (owner, 2026-11-23). */
    turned: Boolean,
    onTranslate: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(heard) }
    PopupCard(onDismissRequest = onCancel, turned = turned) {
        Text(
            text = stringResource(Res.string.translator_correct),
            style = MaterialTheme.typography.headlineSmall,
        )
        PocketTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(Res.string.translator_what_was_said)) },
            modifier = Modifier.fillMaxWidth(),
        )
        BottomActionRow {
            BottomAction(
                label = stringResource(Res.string.translator_cancel),
                onClick = onCancel,
                compact = true,
                style = BottomActionStyle.Outlined,
                modifier = Modifier.weight(1f),
            )
            BottomAction(
                label =
                    stringResource(
                        if (waiting) Res.string.translator_keep_correction else Res.string.translator_translate_again,
                    ),
                onClick = { onTranslate(text) },
                enabled = text.isNotBlank(),
                compact = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun problemText(
    problem: ConversationProblem,
    name: String,
): String =
    when (problem) {
        ConversationProblem.NOTHING_HEARD -> stringResource(Res.string.translator_nothing_heard)
        ConversationProblem.LANGUAGE_MISSING -> stringResource(Res.string.translator_conversation_unsupported, name)
        ConversationProblem.MICROPHONE_BUSY -> stringResource(Res.string.translator_microphone_busy)
        ConversationProblem.NO_PERMISSION -> stringResource(Res.string.translator_microphone_denied)
        ConversationProblem.LISTENING_FAILED -> stringResource(Res.string.translator_listening_failed)
        ConversationProblem.TRANSLATION_FAILED -> stringResource(Res.string.translator_turn_failed)
    }
