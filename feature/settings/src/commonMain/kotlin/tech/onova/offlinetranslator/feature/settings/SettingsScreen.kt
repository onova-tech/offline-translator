package tech.onova.offlinetranslator.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tech.onova.offlinetranslator.core.designsystem.components.ChoiceOption
import tech.onova.offlinetranslator.core.designsystem.components.InfoRow
import tech.onova.offlinetranslator.core.designsystem.components.SectionHeader
import tech.onova.offlinetranslator.core.designsystem.components.SegmentedChoice
import tech.onova.offlinetranslator.core.designsystem.components.SettingsRow
import tech.onova.offlinetranslator.core.designsystem.components.SwitchRow
import tech.onova.offlinetranslator.core.designsystem.components.ToolFrame
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing
import tech.onova.offlinetranslator.core.domain.AppLinks
import tech.onova.offlinetranslator.core.domain.settings.AppLanguage
import tech.onova.offlinetranslator.core.domain.settings.ThemeMode
import tech.onova.offlinetranslator.core.toolapi.openedInBrowser
import tech.onova.offlinetranslator.feature.settings.resources.Res
import tech.onova.offlinetranslator.feature.settings.resources.app_language
import tech.onova.offlinetranslator.feature.settings.resources.dialog_cancel
import tech.onova.offlinetranslator.feature.settings.resources.dynamic_color
import tech.onova.offlinetranslator.feature.settings.resources.dynamic_color_supporting
import tech.onova.offlinetranslator.feature.settings.resources.language_en
import tech.onova.offlinetranslator.feature.settings.resources.language_es
import tech.onova.offlinetranslator.feature.settings.resources.language_pt_br
import tech.onova.offlinetranslator.feature.settings.resources.language_system
import tech.onova.offlinetranslator.feature.settings.resources.licenses_supporting
import tech.onova.offlinetranslator.feature.settings.resources.licenses_title
import tech.onova.offlinetranslator.feature.settings.resources.section_about
import tech.onova.offlinetranslator.feature.settings.resources.section_appearance
import tech.onova.offlinetranslator.feature.settings.resources.section_language
import tech.onova.offlinetranslator.feature.settings.resources.settings_back
import tech.onova.offlinetranslator.feature.settings.resources.settings_title
import tech.onova.offlinetranslator.feature.settings.resources.source_code
import tech.onova.offlinetranslator.feature.settings.resources.source_code_supporting
import tech.onova.offlinetranslator.feature.settings.resources.source_code_unavailable
import tech.onova.offlinetranslator.feature.settings.resources.theme_dark
import tech.onova.offlinetranslator.feature.settings.resources.theme_label
import tech.onova.offlinetranslator.feature.settings.resources.theme_light
import tech.onova.offlinetranslator.feature.settings.resources.theme_system
import tech.onova.offlinetranslator.feature.settings.resources.version_label

/**
 * Settings (spec 081): Appearance, Language, the Translator's Offline languages, and About with the version, the
 * source code (GPL-3.0) and the open-source licenses. No ads, no purchases, nothing to consent to.
 */
@Composable
public fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** The Translator's "Offline languages" (spec 012 story 3), passed in by the app: features stay apart. */
    offlineLanguages: (@Composable (Modifier) -> Unit)? = null,
    /** Rows the app adds under About, after the licenses: the debug build's phone probe (spec 012 T032). */
    about: (@Composable (Modifier) -> Unit)? = null,
    /** Opens About → Open-source licenses (spec 012 T057), when the app has the list. */
    onOpenLicenses: (() -> Unit)? = null,
) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    var languageDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val actions =
        SettingsActions(
            onThemeChange = viewModel::setTheme,
            onDynamicColorChange = viewModel::setDynamicColor,
            onLanguageClick = { languageDialog = true },
            onSourceCode = rememberSourceOpener(snackbarHostState),
        )

    ToolFrame(
        title = stringResource(Res.string.settings_title),
        backLabel = stringResource(Res.string.settings_back),
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    ) { insets ->
        if (state.loaded) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(insets),
                verticalArrangement = Arrangement.spacedBy(PocketSpacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                settingsItems(state, actions, SettingsSlots(offlineLanguages, about, onOpenLicenses))
            }
        }
    }

    if (languageDialog) {
        ChoiceDialog(
            title = stringResource(Res.string.app_language),
            options = AppLanguage.entries.map { ChoiceOption(it, stringResource(it.label)) },
            selected = state.language,
            onSelect = { language ->
                languageDialog = false
                viewModel.setLanguage(language)
            },
            onDismiss = { languageDialog = false },
        )
    }
}

private class SettingsActions(
    val onThemeChange: (ThemeMode) -> Unit,
    val onDynamicColorChange: (Boolean) -> Unit,
    val onLanguageClick: () -> Unit,
    val onSourceCode: () -> Unit,
)

/** Opens the code's page in the phone's browser, or says where it is when no app opens links. */
@Composable
private fun rememberSourceOpener(snackbarHostState: SnackbarHostState): () -> Unit {
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val unavailable = stringResource(Res.string.source_code_unavailable)
    return {
        if (!uriHandler.openedInBrowser(AppLinks.SOURCE_CODE)) {
            scope.launch { snackbarHostState.showSnackbar(unavailable) }
        }
    }
}

/** The sections other modules add, each in its place. */
private class SettingsSlots(
    val offlineLanguages: (@Composable (Modifier) -> Unit)?,
    val about: (@Composable (Modifier) -> Unit)?,
    val onOpenLicenses: (() -> Unit)?,
)

/** The sections in order: Appearance, Language, Offline languages, About. */
private fun LazyListScope.settingsItems(
    state: SettingsUiState,
    actions: SettingsActions,
    slots: SettingsSlots,
) {
    item { SettingsSection(Res.string.section_appearance) }
    item { ThemeChoice(state.theme, actions.onThemeChange) }
    if (state.dynamicColorAvailable) {
        item {
            SwitchRow(
                title = stringResource(Res.string.dynamic_color),
                supporting = stringResource(Res.string.dynamic_color_supporting),
                checked = state.dynamicColor,
                onCheckedChange = actions.onDynamicColorChange,
                modifier = Modifier.settingsWidth(),
            )
        }
    }
    item { SettingsSection(Res.string.section_language) }
    item {
        SettingsRow(
            title = stringResource(Res.string.app_language),
            supporting = stringResource(state.language.label),
            onClick = actions.onLanguageClick,
            modifier = Modifier.settingsWidth(),
        )
    }
    slots.offlineLanguages?.let { section -> item { section(Modifier.settingsWidth()) } }
    aboutItems(state, actions, slots)
}

private fun LazyListScope.aboutItems(
    state: SettingsUiState,
    actions: SettingsActions,
    slots: SettingsSlots,
) {
    item { SettingsSection(Res.string.section_about) }
    item {
        InfoRow(
            title = stringResource(Res.string.version_label),
            value = state.versionName,
            modifier = Modifier.settingsWidth(),
        )
    }
    item {
        SettingsRow(
            title = stringResource(Res.string.source_code),
            supporting = stringResource(Res.string.source_code_supporting),
            onClick = actions.onSourceCode,
            modifier = Modifier.settingsWidth(),
        )
    }
    slots.onOpenLicenses?.let { open ->
        item {
            SettingsRow(
                title = stringResource(Res.string.licenses_title),
                supporting = stringResource(Res.string.licenses_supporting),
                onClick = open,
                modifier = Modifier.settingsWidth(),
            )
        }
    }
    slots.about?.let { rows -> item { rows(Modifier.settingsWidth()) } }
}

@Composable
private fun ThemeChoice(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    LabeledChoice(
        label = stringResource(Res.string.theme_label),
        options =
            listOf(
                ChoiceOption(ThemeMode.SYSTEM, stringResource(Res.string.theme_system)),
                ChoiceOption(ThemeMode.LIGHT, stringResource(Res.string.theme_light)),
                ChoiceOption(ThemeMode.DARK, stringResource(Res.string.theme_dark)),
            ),
        selected = selected,
        onSelect = onSelect,
    )
}

@Composable
private fun Modifier.settingsWidth(): Modifier =
    fillMaxWidth().widthIn(max = PocketSizes.contentMaxWidth).padding(horizontal = PocketSpacing.md)

@Composable
private fun SettingsSection(title: StringResource) {
    SectionHeader(text = stringResource(title), modifier = Modifier.settingsWidth().padding(top = PocketSpacing.sm))
}

@Composable
private fun <K> LabeledChoice(
    label: String,
    options: List<ChoiceOption<K>>,
    selected: K,
    onSelect: (K) -> Unit,
) {
    Column(
        modifier = Modifier.settingsWidth(),
        verticalArrangement = Arrangement.spacedBy(PocketSpacing.xs),
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        SegmentedChoice(options = options, selected = selected, onSelect = onSelect, modifier = Modifier.fillMaxWidth())
    }
}

private val AppLanguage.label: StringResource
    get() =
        when (this) {
            AppLanguage.SYSTEM -> Res.string.language_system
            AppLanguage.EN -> Res.string.language_en
            AppLanguage.PT_BR -> Res.string.language_pt_br
            AppLanguage.ES -> Res.string.language_es
        }

@Composable
private fun <K> ChoiceDialog(
    title: String,
    options: List<ChoiceOption<K>>,
    selected: K,
    onSelect: (K) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            // Scrolls when the dialog is taller than a short window (landscape, large text).
            Column(modifier = Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    ChoiceRow(
                        label = option.label,
                        selected = option.key == selected,
                        onSelect = { onSelect(option.key) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.dialog_cancel)) } },
    )
}

@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = PocketSizes.touchTarget)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = PocketSpacing.sm),
        )
    }
}
