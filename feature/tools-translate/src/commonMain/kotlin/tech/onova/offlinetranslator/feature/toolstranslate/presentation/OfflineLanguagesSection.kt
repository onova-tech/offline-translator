package tech.onova.offlinetranslator.feature.toolstranslate.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tech.onova.offlinetranslator.core.designsystem.components.SectionHeader
import tech.onova.offlinetranslator.core.designsystem.components.SettingsRow
import tech.onova.offlinetranslator.core.designsystem.components.SwitchRow
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing
import tech.onova.offlinetranslator.feature.toolstranslate.domain.PackMessages
import tech.onova.offlinetranslator.feature.toolstranslate.domain.Routes
import tech.onova.offlinetranslator.feature.toolstranslate.resources.Res
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_manage_languages
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_offline_languages
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_space_used_value
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_wifi_only
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_wifi_only_supporting

/**
 * Settings → Offline languages (spec 012 story 3; spec 080, owner 2026-11-20): the Wi-Fi switch, then "Manage
 * languages" with the space the language packs use, which opens the Manage languages page, where each language's
 * pack, speech and voice are got or deleted. The app wires it into Settings (feature modules don't depend on each
 * other).
 */
@Composable
public fun OfflineLanguagesSection(modifier: Modifier = Modifier) {
    val viewModel: PacksViewModel = koinViewModel()
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    var managing by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.refresh() }
    val used = ui.packs.filter { Routes.languageOf(it.id) != null }.sumOf { it.installedBytes ?: 0 }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(PocketSpacing.xs)) {
        SectionHeader(
            text = stringResource(Res.string.translator_offline_languages),
            modifier = Modifier.padding(top = PocketSpacing.sm),
        )
        SwitchRow(
            title = stringResource(Res.string.translator_wifi_only),
            supporting = stringResource(Res.string.translator_wifi_only_supporting),
            checked = ui.wifiOnly,
            onCheckedChange = viewModel::setWifiOnly,
            modifier = Modifier.fillMaxWidth(),
        )
        SettingsRow(
            title = stringResource(Res.string.translator_manage_languages),
            supporting =
                stringResource(
                    Res.string.translator_space_used_value,
                    PackMessages.megabytes(used).toString(),
                ),
            onClick = { managing = true },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (managing) ManageLanguagesPage(focus = null, onClose = { managing = false })
}
