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
import tech.onova.offlinetranslator.core.designsystem.components.ChoiceOption
import tech.onova.offlinetranslator.core.designsystem.components.ExpandableCard
import tech.onova.offlinetranslator.core.designsystem.components.InfoRow
import tech.onova.offlinetranslator.core.designsystem.components.MenuSettingsRow
import tech.onova.offlinetranslator.core.designsystem.components.RowMenuItem
import tech.onova.offlinetranslator.core.designsystem.components.SegmentedChoice
import tech.onova.offlinetranslator.core.designsystem.components.StatusMark
import tech.onova.offlinetranslator.core.designsystem.icons.PocketIcons
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.PackState
import tech.onova.offlinetranslator.core.domain.ports.PackStatus
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport
import tech.onova.offlinetranslator.core.domain.speech.SpeechEngine
import tech.onova.offlinetranslator.core.toolapi.ToolHelp
import tech.onova.offlinetranslator.feature.toolstranslate.domain.EngineUse
import tech.onova.offlinetranslator.feature.toolstranslate.domain.PackMessages
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SpeechKind
import tech.onova.offlinetranslator.feature.toolstranslate.resources.Res
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_basic_quality
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_built_in
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_cancel
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_delete
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_download
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_enable_language
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_enable_language_supporting
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_engine_app
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_engine_group
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_engine_phone
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_get_language
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_manage_help
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_manage_marks
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_pack_installed
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_part_checking
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_part_menu
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_part_ready
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_part_speech
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_part_translation
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_part_voice
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_speech_app_can_get
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
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_to_download
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_update_speech_service
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_voice_app_can_get
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_voice_no_app
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_voice_none
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_voice_settings

/** A pack's actions on the Manage languages page. */
internal class PackPartActions(
    val onDownload: (PackId) -> Unit,
    val onCancel: (PackId) -> Unit,
    /** Asks first; the page adds espeak-ng's data to the last of the app's voices (spec 082). */
    val onDelete: (List<PackId>) -> Unit,
)

/** Speech and voice: what the phone's own speech service and voice app are asked for. */
internal class SpeechPartActions(
    val onGet: (LanguageCode) -> Unit,
    val onUpdate: () -> Unit,
    val onVoice: () -> Unit,
    /** "Phone" or "App" for a language's recognition or voice (spec 082 US3). */
    val onChoose: (LanguageCode, SpeechKind, SpeechEngine) -> Unit = { _, _, _ -> },
)

/** What the Manage languages page's rows do. */
internal class PartActions(
    val onToggle: (LanguageCode) -> Unit,
    val pack: PackPartActions,
    val speech: SpeechPartActions,
)

/**
 * What the phone has for one language: its pack (none for English), its speech and voice once asked, and the app's own
 * speech packs for it (spec 082): the recognition pack, and the voice with espeak-ng's data.
 */
internal class LanguageState(
    val pack: PackStatus?,
    val speech: SpeechParts?,
    val serviceUpdatable: Boolean,
    val appRecognition: PackStatus? = null,
    val appVoice: List<PackStatus> = emptyList(),
    val uses: SpeechUses? = null,
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
    val missing = speech.recognition in GETTABLE_SPEECH || speech.voice in GETTABLE_VOICES
    return LanguageStatus(complete = !missing, enabled = enabled)
}

private val GETTABLE_SPEECH =
    setOf(
        RecognitionSupport.CAN_DOWNLOAD,
        RecognitionSupport.DOWNLOADING,
        RecognitionSupport.NOT_INSTALLED,
        RecognitionSupport.APP_CAN_DOWNLOAD,
        RecognitionSupport.APP_DOWNLOADING,
    )

private val GETTABLE_VOICES =
    setOf(VoiceSupport.NO_OFFLINE_VOICE, VoiceSupport.APP_CAN_DOWNLOAD, VoiceSupport.APP_DOWNLOADING)

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
        VoicePart(row, state, actions)
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
            onDelete = { actions.pack.onDelete(listOf(pack.id)) },
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
    val title = stringResource(Res.string.translator_part_speech)
    val recognition = state.speech?.recognition
    val use = state.uses?.recognition
    val appSide =
        use?.app
            ?: (recognition == RecognitionSupport.APP_CAN_DOWNLOAD || recognition == RecognitionSupport.APP_DOWNLOADING)
    if (appSide) {
        val packs = listOfNotNull(state.appRecognition)
        AppPart(row, title, packs, stringResource(Res.string.translator_speech_app_can_get, toDownload(packs)), actions)
    } else {
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
        Part(row, title, speechText(recognition), items)
    }
    if (use?.choosable == true) EngineChoice(row, title, SpeechKind.RECOGNITION, use, actions)
}

@Composable
private fun VoicePart(
    row: LanguageRow,
    state: LanguageState,
    actions: PartActions,
) {
    val title = stringResource(Res.string.translator_part_voice)
    val voice = state.speech?.voice
    val use = state.uses?.voice
    val appSide = use?.app ?: (voice == VoiceSupport.APP_CAN_DOWNLOAD || voice == VoiceSupport.APP_DOWNLOADING)
    if (appSide) {
        val text = stringResource(Res.string.translator_voice_app_can_get, toDownload(state.appVoice))
        AppPart(row, title, state.appVoice, text, actions)
    } else {
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
        Part(row, title, voiceText(voice), items)
    }
    if (use?.choosable == true) EngineChoice(row, title, SpeechKind.VOICE, use, actions)
}

/**
 * A part the app's own packs do (spec 082): installed with their size; on their way, waiting or failed, with the
 * store's words; otherwise the [offer] to download them. Behind ⋮: Download, Cancel or Delete. The first pack is the
 * language's own (Whisper, its voice); a voice's other one, espeak-ng's data, goes with the last voice (the page).
 */
@Composable
private fun AppPart(
    row: LanguageRow,
    title: String,
    packs: List<PackStatus>,
    offer: String,
    actions: PartActions,
) {
    val installed = packs.isNotEmpty() && packs.all { it.state == PackState.Installed }
    val text =
        when {
            installed -> {
                val bytes = packs.sumOf { it.installedBytes ?: 0 }
                stringResource(Res.string.translator_pack_installed, PackMessages.megabytes(bytes).toString())
            }

            else -> {
                packs.firstOrNull { it.state !in QUIET_PACK_STATES }?.let { stateText(it) } ?: offer
            }
        }
    val delete =
        RowMenuItem(stringResource(Res.string.translator_delete), icon = PocketIcons.Delete) {
            actions.pack.onDelete(listOfNotNull(packs.firstOrNull()?.id))
        }
    val items = if (installed) listOf(delete) else appPackItems(packs, actions)
    Part(row, title, text, items)
}

/** "Phone" or "App" under its part, where both can do it (spec 082 US3); "Phone" at first. */
@Composable
private fun EngineChoice(
    row: LanguageRow,
    title: String,
    kind: SpeechKind,
    use: EngineUse,
    actions: PartActions,
) {
    SegmentedChoice(
        options =
            listOf(
                ChoiceOption(SpeechEngine.PHONE, stringResource(Res.string.translator_engine_phone)),
                ChoiceOption(SpeechEngine.APP, stringResource(Res.string.translator_engine_app)),
            ),
        selected = use.shown,
        // Tapping what's already shown saves nothing: "Phone" at first is no choice (the owner's Japanese, 2026-11-22).
        onSelect = { engine -> if (engine != use.shown) actions.speech.onChoose(row.code, kind, engine) },
        groupLabel = stringResource(Res.string.translator_engine_group, title, row.name),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The app's own packs' actions while not all in (spec 082): Cancel the ones on their way, else Download the rest. */
@Composable
private fun appPackItems(
    packs: List<PackStatus>,
    actions: PartActions,
): List<RowMenuItem> {
    val coming =
        packs.filter {
            it.state is PackState.Downloading || it.state is PackState.Waiting ||
                it.state == PackState.Installing
        }
    val missing = packs.filter { it.state != PackState.Installed }
    return when {
        coming.isNotEmpty() -> {
            listOf(
                RowMenuItem(stringResource(Res.string.translator_cancel), icon = null) {
                    coming.forEach { actions.pack.onCancel(it.id) }
                },
            )
        }

        missing.isNotEmpty() -> {
            listOf(
                RowMenuItem(stringResource(Res.string.translator_download), icon = null) {
                    missing.forEach { actions.pack.onDownload(it.id) }
                },
            )
        }

        else -> {
            emptyList()
        }
    }
}

/** "153 MB to download": what the packs not in yet take, or "Checking…" before the store answers. */
@Composable
private fun toDownload(packs: List<PackStatus>): String {
    val bytes = packs.filter { it.state != PackState.Installed }.map { it.downloadBytes }
    return if (bytes.isEmpty() || bytes.any { it == null }) {
        stringResource(Res.string.translator_part_checking)
    } else {
        stringResource(Res.string.translator_to_download, PackMessages.megabytes(bytes.sumOf { it ?: 0 }).toString())
    }
}

@Composable
private fun voiceText(voice: VoiceSupport?): String =
    stringResource(
        when (voice) {
            null -> Res.string.translator_part_checking

            VoiceSupport.READY -> Res.string.translator_part_ready

            VoiceSupport.NO_OFFLINE_VOICE -> Res.string.translator_voice_none

            VoiceSupport.NO_ENGINE -> Res.string.translator_voice_no_app

            // Spec 082: the caller shows the app's packs instead.
            VoiceSupport.APP_CAN_DOWNLOAD,
            VoiceSupport.APP_DOWNLOADING,
            -> Res.string.translator_part_checking
        },
    )

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

            // Spec 082: the caller shows the app's pack instead.
            RecognitionSupport.APP_CAN_DOWNLOAD,
            RecognitionSupport.APP_DOWNLOADING,
            -> Res.string.translator_part_checking
        },
    )

private const val SEPARATOR = " · "

/** A pack's states that say nothing beyond the offer to download it. */
private val QUIET_PACK_STATES = setOf(PackState.Installed, PackState.NotInstalled, PackState.Unknown)

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
