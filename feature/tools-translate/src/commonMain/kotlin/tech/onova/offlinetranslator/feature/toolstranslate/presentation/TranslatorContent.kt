package tech.onova.offlinetranslator.feature.toolstranslate.presentation

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import tech.onova.offlinetranslator.core.designsystem.components.ChoiceOption
import tech.onova.offlinetranslator.core.designsystem.components.MenuEntry
import tech.onova.offlinetranslator.core.designsystem.components.SegmentedChoice
import tech.onova.offlinetranslator.core.domain.permissions.AppPermission
import tech.onova.offlinetranslator.core.domain.permissions.PermissionController
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.LanguageNames
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.SettingsPage
import tech.onova.offlinetranslator.core.domain.translation.LanguageCatalog
import tech.onova.offlinetranslator.core.toolapi.PermissionGate
import tech.onova.offlinetranslator.core.toolapi.rememberPermissionGranted
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SpeechRegions
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorMode
import tech.onova.offlinetranslator.feature.toolstranslate.resources.Res
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_language_en
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_language_es
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_language_pt
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_microphone_rationale
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_mode_conversation
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_mode_type_or_speak
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_modes
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_other_languages

/**
 * The Translator (spec 012): its two modes at the top (FR-008), then the chosen mode's screen. The app's language, as
 * the screen shows it, is the first source language until the person chooses one.
 */
@Composable
internal fun TranslatorContent(
    viewModel: TranslatorViewModel = koinViewModel(),
    packs: PacksViewModel = koinViewModel(),
    voice: SpeakAndHearViewModel = koinViewModel(),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val speechUi by voice.uiState.collectAsStateWithLifecycle()
    val appLanguage = Locale.current.language
    LaunchedEffect(appLanguage) { viewModel.setAppLanguage(appLanguage) }
    // What is heard goes into the text, and is translated as typed text is (FR-023).
    LaunchedEffect(voice) { voice.heard.collect(viewModel::setText) }
    if (!ui.loaded) return
    val phoneLocales = LocaleList.current.localeList.map { it.toLanguageTag() }
    LaunchedEffect(ui.source, ui.target, phoneLocales) {
        voice.setLanguages(SpeechRegions.tagFor(ui.source, phoneLocales), SpeechRegions.tagFor(ui.target, phoneLocales))
    }
    val top: @Composable ColumnScope.() -> Unit = { ModeChoice(ui.mode, viewModel::setMode) }
    // "Manage languages…" in every list, and the speech messages at their language, open the Manage languages page
    // (spec 080, owner 2026-11-20). Null: closed; empty: open; else open at that language.
    var managing by rememberSaveable { mutableStateOf<String?>(null) }
    val manage: (LanguageCode?) -> Unit = { managing = it?.value.orEmpty() }
    val moreLanguages = MenuEntry(stringResource(Res.string.translator_other_languages)) { manage(null) }
    managing?.let { at ->
        ManageLanguagesPage(focus = at.takeIf { it.isNotEmpty() }?.let(::LanguageCode), onClose = { managing = null })
    }
    when (ui.mode) {
        TranslatorMode.TYPE_OR_SPEAK -> {
            TypeOrSpeakContent(
                ui = ui,
                top = top,
                actions =
                    TypeOrSpeakActions(
                        onText = viewModel::setText,
                        onLanguage = viewModel::choose,
                        onSwap = viewModel::swap,
                        onSend = viewModel::send,
                        moreLanguages = moreLanguages,
                    ),
                packActions = PackActions(onDownload = packs::download, onCancel = packs::cancel, onOpen = packs::open),
                speech =
                    rememberSpeechControls(speechUi, voice, translation = ui.translation) { problem ->
                        manage(if (problem == SpeechProblem.NO_VOICE) ui.target else ui.source)
                    },
                effects = viewModel.effects,
            )
        }

        TranslatorMode.CONVERSATION -> {
            ConversationContent(
                top = top,
                moreLanguages = moreLanguages,
                onManage = manage,
                packActions = PackActions(onDownload = packs::download, onCancel = packs::cancel, onOpen = packs::open),
            )
        }
    }
}

/**
 * The voice's controls. The microphone is asked for only through [PermissionGate], the first time the person taps it,
 * never when the tool opens (FR-020); its card shows above the text until granted or put away.
 */
@Composable
private fun rememberSpeechControls(
    ui: SpeechUi,
    voice: SpeakAndHearViewModel,
    translation: String?,
    /** A speech message's "Manage languages": the page, at the language the message is about. */
    onManage: (SpeechProblem?) -> Unit,
): SpeechControls {
    val permissions: PermissionController = koinInject()
    val granted = rememberPermissionGranted(AppPermission.RECORD_AUDIO, permissions)
    var asking by rememberSaveable { mutableStateOf(false) }
    val rationale = stringResource(Res.string.translator_microphone_rationale)
    val gate: (@Composable () -> Unit)? =
        if (asking) {
            {
                PermissionGate(
                    permission = AppPermission.RECORD_AUDIO,
                    rationale = rationale,
                    controller = permissions,
                    onGrant = {
                        asking = false
                        voice.tapMicrophone()
                    },
                    onDismiss = { asking = false },
                )
            }
        } else {
            null
        }
    // A phone that can't listen to this language says why at once: no permission is asked for nothing.
    val canListen = ui.recognition == RecognitionSupport.READY || ui.recognition == RecognitionSupport.UNKNOWN
    return SpeechControls(
        ui = ui,
        onMicrophone = { if (granted || ui.listening || !canListen) voice.tapMicrophone() else asking = true },
        onReadAloud = { if (ui.speaking) voice.readAloud("") else translation?.let(voice::readAloud) },
        onFix = { onManage(ui.problem) },
        onDismiss = voice::dismiss,
        permissionGate = gate,
    )
}

/** What the person does on Type or speak, passed down as lambdas. */
internal class TypeOrSpeakActions(
    val onText: (String) -> Unit,
    val onLanguage: (LanguageSide, LanguageCode) -> Unit,
    val onSwap: () -> Unit,
    val onSend: (Send, String) -> Unit,
    /** "Other languages…" at the end of each list (spec 080). */
    val moreLanguages: MenuEntry,
)

/** What the pack offer's buttons do; Not now belongs to the screen that shows it. */
internal class PackActions(
    val onDownload: (PackId) -> Unit,
    val onCancel: (PackId) -> Unit,
    val onOpen: (SettingsPage) -> Unit,
)

@Composable
private fun ModeChoice(
    mode: TranslatorMode,
    onSelect: (TranslatorMode) -> Unit,
) {
    SegmentedChoice(
        options =
            listOf(
                ChoiceOption(TranslatorMode.TYPE_OR_SPEAK, stringResource(Res.string.translator_mode_type_or_speak)),
                ChoiceOption(TranslatorMode.CONVERSATION, stringResource(Res.string.translator_mode_conversation)),
            ),
        selected = mode,
        onSelect = onSelect,
        groupLabel = stringResource(Res.string.translator_modes),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * A language's name in the app's language: the three spec 012 named keep their strings, the others come from the
 * phone (spec 080 research R4).
 */
@Composable
internal fun languageName(language: LanguageCode): String {
    val names: LanguageNames = koinInject()
    val appLanguage = Locale.current.language
    return when (language.value) {
        "en" -> stringResource(Res.string.translator_language_en)
        "pt" -> stringResource(Res.string.translator_language_pt)
        "es" -> stringResource(Res.string.translator_language_es)
        else -> names.nameOf(LanguageCatalog.of(language)?.tag ?: language.value, appLanguage)
    }
}
