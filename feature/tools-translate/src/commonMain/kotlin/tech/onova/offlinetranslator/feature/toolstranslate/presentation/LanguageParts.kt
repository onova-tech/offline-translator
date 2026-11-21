package tech.onova.offlinetranslator.feature.toolstranslate.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.onova.offlinetranslator.core.designsystem.components.CheckRow
import tech.onova.offlinetranslator.core.designsystem.components.ExpandableCard
import tech.onova.offlinetranslator.core.designsystem.components.InfoRow
import tech.onova.offlinetranslator.core.designsystem.components.MenuSettingsRow
import tech.onova.offlinetranslator.core.designsystem.components.RowMenuItem
import tech.onova.offlinetranslator.core.designsystem.components.StatusMark
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.PackState
import tech.onova.offlinetranslator.core.domain.ports.PackStatus
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport
import tech.onova.offlinetranslator.core.toolapi.ToolHelp
import tech.onova.offlinetranslator.feature.toolstranslate.resources.Res
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_basic_quality
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_built_in
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_enable_language
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_enable_language_supporting
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_get_language
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_manage_help
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_manage_marks
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_part_checking
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_part_menu
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_part_ready
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_part_speech
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_part_translation
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_part_voice
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_speech_can_get
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_speech_getting
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_speech_none
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_speech_not_got
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_speech_old_phone
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_speech_tried
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_status_partial_off
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_status_partial_on
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_status_ready_off
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_status_ready_on
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_update_speech_service
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_voice_no_app
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_voice_none
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_voice_settings

/** A pack's actions on the Manage languages page. */
internal class PackPartActions(
    val onDownload: (PackId) -> Unit,
    val onCancel: (PackId) -> Unit,
    val onDelete: (PackId) -> Unit,
)

/** Speech and voice: what the phone's own speech service and voice app are asked for. */
internal class SpeechPartActions(
    val onGet: (LanguageCode) -> Unit,
    val onUpdate: () -> Unit,
    val onVoice: () -> Unit,
)

/** What the Manage languages page's rows do. */
internal class PartActions(
    val onToggle: (LanguageCode) -> Unit,
    val pack: PackPartActions,
    val speech: SpeechPartActions,
)

/** What the phone has for one language: its pack (none for English), its speech and voice once asked. */
internal class LanguageState(
    val pack: PackStatus?,
    val speech: SpeechParts?,
    val serviceUpdatable: Boolean,
)

/** A language that is downloaded: all there or only in part, and whether it's in the lists. */
internal data class LanguageStatus(
    val complete: Boolean,
    val enabled: Boolean,
)

/**
 * The mark beside a downloaded language (owner, 2026-11-20): none until its pack is on the phone (English is built in)
 * and the phone has answered for its speech and voice; then complete, or partial when the phone can still get its
 * speech or a voice. What the phone can't have at all (Arabic speech on the owner's phone) doesn't count.
 */
internal fun statusOf(
    enabled: Boolean,
    pack: PackStatus?,
    speech: SpeechParts?,
): LanguageStatus? {
    val downloaded = pack == null || pack.state == PackState.Installed
    if (!downloaded || speech == null) return null
    val missing =
        speech.recognition in GETTABLE_SPEECH || speech.voice == VoiceSupport.NO_OFFLINE_VOICE
    return LanguageStatus(complete = !missing, enabled = enabled)
}

private val GETTABLE_SPEECH =
    setOf(RecognitionSupport.CAN_DOWNLOAD, RecognitionSupport.DOWNLOADING, RecognitionSupport.NOT_INSTALLED)

/**
 * One language (owner, 2026-11-20): its name, its own name and "Basic quality" for the small models, its status mark
 * and ⌄; opened, "Enable language" (in the lists; the last two enabled are locked, FR-005), then its translation
 * pack, the phone's speech recognition and its offline voice, each with its state and, behind ⋮, what gets or removes
 * it (A6).
 */
@Composable
internal fun LanguageCard(
    row: LanguageRow,
    state: LanguageState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    actions: PartActions,
    modifier: Modifier = Modifier,
) {
    val basic = stringResource(Res.string.translator_basic_quality)
    val status = statusOf(row.checked, state.pack, state.speech)
    ExpandableCard(
        title = row.name,
        supporting = if (row.basic) "${row.ownName}$SEPARATOR$basic" else row.ownName,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        status = status?.let { { StatusMark(it.complete, it.enabled, statusLabel(it)) } },
        modifier = modifier,
    ) {
        CheckRow(
            title = stringResource(Res.string.translator_enable_language),
            supporting = stringResource(Res.string.translator_enable_language_supporting),
            checked = row.checked,
            onCheckedChange = { actions.onToggle(row.code) },
            enabled = !row.locked,
            modifier = Modifier.fillMaxWidth(),
        )
        TranslationPart(row, state.pack, actions)
        SpeechPart(row, state, actions)
        VoicePart(row, state.speech?.voice, actions)
    }
}

@Composable
private fun statusLabel(status: LanguageStatus): String =
    stringResource(
        when {
            status.complete && status.enabled -> Res.string.translator_status_ready_on
            status.complete -> Res.string.translator_status_ready_off
            status.enabled -> Res.string.translator_status_partial_on
            else -> Res.string.translator_status_partial_off
        },
    )

@Composable
private fun TranslationPart(
    row: LanguageRow,
    pack: PackStatus?,
    actions: PartActions,
) {
    val title = stringResource(Res.string.translator_part_translation)
    if (pack == null) {
        InfoRow(
            title = title,
            value = stringResource(Res.string.translator_built_in),
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        PackRow(
            status = pack,
            onDownload = { actions.pack.onDownload(pack.id) },
            onCancel = { actions.pack.onCancel(pack.id) },
            onDelete = { actions.pack.onDelete(pack.id) },
            title = title,
            menuLabel = stringResource(Res.string.translator_part_menu, row.name, title),
        )
    }
}

@Composable
private fun SpeechPart(
    row: LanguageRow,
    state: LanguageState,
    actions: PartActions,
) {
    val recognition = state.speech?.recognition
    val items =
        when {
            recognition == RecognitionSupport.CAN_DOWNLOAD -> {
                listOf(
                    RowMenuItem(
                        stringResource(Res.string.translator_get_language),
                        icon = null,
                    ) { actions.speech.onGet(row.code) },
                )
            }

            recognition == RecognitionSupport.NOT_INSTALLED && state.serviceUpdatable -> {
                listOf(
                    RowMenuItem(
                        stringResource(Res.string.translator_update_speech_service),
                        icon = null,
                        onClick = actions.speech.onUpdate,
                    ),
                )
            }

            else -> {
                emptyList()
            }
        }
    Part(row, stringResource(Res.string.translator_part_speech), speechText(recognition), items)
}

@Composable
private fun VoicePart(
    row: LanguageRow,
    voice: VoiceSupport?,
    actions: PartActions,
) {
    val items =
        if (voice == VoiceSupport.NO_OFFLINE_VOICE) {
            listOf(
                RowMenuItem(
                    stringResource(Res.string.translator_voice_settings),
                    icon = null,
                    onClick = actions.speech.onVoice,
                ),
            )
        } else {
            emptyList()
        }
    val text =
        when (voice) {
            null -> stringResource(Res.string.translator_part_checking)
            VoiceSupport.READY -> stringResource(Res.string.translator_part_ready)
            VoiceSupport.NO_OFFLINE_VOICE -> stringResource(Res.string.translator_voice_none)
            VoiceSupport.NO_ENGINE -> stringResource(Res.string.translator_voice_no_app)
        }
    Part(row, stringResource(Res.string.translator_part_voice), text, items)
}

/** A part with its actions behind ⋮, or its state alone when nothing can be done here. */
@Composable
private fun Part(
    row: LanguageRow,
    title: String,
    text: String,
    items: List<RowMenuItem>,
) {
    if (items.isEmpty()) {
        InfoRow(title = title, value = text, modifier = Modifier.fillMaxWidth())
    } else {
        MenuSettingsRow(
            title = title,
            supporting = text,
            menuLabel = stringResource(Res.string.translator_part_menu, row.name, title),
            items = items,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun speechText(recognition: RecognitionSupport?): String =
    stringResource(
        when (recognition) {
            null -> Res.string.translator_part_checking
            RecognitionSupport.READY -> Res.string.translator_part_ready
            RecognitionSupport.CAN_DOWNLOAD -> Res.string.translator_speech_can_get
            RecognitionSupport.DOWNLOADING -> Res.string.translator_speech_getting
            RecognitionSupport.NOT_INSTALLED -> Res.string.translator_speech_not_got
            RecognitionSupport.UNSUPPORTED, RecognitionSupport.NO_RECOGNIZER -> Res.string.translator_speech_none
            RecognitionSupport.SYSTEM_TOO_OLD -> Res.string.translator_speech_old_phone
            RecognitionSupport.UNKNOWN -> Res.string.translator_speech_tried
        },
    )

private const val SEPARATOR = " · "

/**
 * Manage languages' help (owner, 2026-11-20): its blocks, then the marks themselves beside what each means, rather
 * than their descriptions; each says its meaning once, beside it, so the mark is silent to screen readers.
 */
@Composable
internal fun ManageLanguagesHelp() {
    Column(verticalArrangement = Arrangement.spacedBy(PocketSpacing.sm)) {
        ToolHelp(Res.string.translator_manage_help)
        Text(text = stringResource(Res.string.translator_manage_marks), style = MaterialTheme.typography.bodyLarge)
        MARKS.forEach { status ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PocketSpacing.sm),
            ) {
                StatusMark(complete = status.complete, enabled = status.enabled, label = null)
                Text(text = statusLabel(status), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private val MARKS =
    listOf(
        LanguageStatus(complete = true, enabled = true),
        LanguageStatus(complete = true, enabled = false),
        LanguageStatus(complete = false, enabled = true),
        LanguageStatus(complete = false, enabled = false),
    )
