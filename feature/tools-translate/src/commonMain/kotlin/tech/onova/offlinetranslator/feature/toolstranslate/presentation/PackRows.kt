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
import tech.onova.offlinetranslator.core.designsystem.components.ConfirmScreen
import tech.onova.offlinetranslator.core.designsystem.components.InfoRow
import tech.onova.offlinetranslator.core.designsystem.components.MenuSettingsRow
import tech.onova.offlinetranslator.core.designsystem.components.RowMenuItem
import tech.onova.offlinetranslator.core.designsystem.components.SectionHeader
import tech.onova.offlinetranslator.core.designsystem.components.SwitchRow
import tech.onova.offlinetranslator.core.designsystem.icons.PocketIcons
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing
import tech.onova.offlinetranslator.core.domain.ports.PackFailure
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.PackState
import tech.onova.offlinetranslator.core.domain.ports.PackStatus
import tech.onova.offlinetranslator.core.domain.ports.PackUnavailable
import tech.onova.offlinetranslator.core.domain.ports.PackWait
import tech.onova.offlinetranslator.feature.toolstranslate.domain.PackMessages
import tech.onova.offlinetranslator.feature.toolstranslate.domain.Routes
import tech.onova.offlinetranslator.feature.toolstranslate.resources.Res
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_cancel
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_delete
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_delete_text
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_delete_title
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_download
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_megabytes
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_offline_languages
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_pack_checking_row
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_pack_downloading_row
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_pack_failed_row
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_pack_installed
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_pack_installing_row
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_pack_menu
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_pack_no_space_row
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_pack_no_store_row
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_pack_not_downloaded
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_pack_unsupported_row
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_pack_waiting_confirmation_row
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_pack_waiting_wifi_row
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_space_used
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_to_download
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_wifi_only
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_wifi_only_supporting

/** A language pack: its title (its language), its state and size, and what can be done with it now. */
@Composable
internal fun PackRow(
    status: PackStatus,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    title: String = packName(status.id),
    menuLabel: String = stringResource(Res.string.translator_pack_menu, title),
) {
    val download = RowMenuItem(stringResource(Res.string.translator_download), icon = null, onClick = onDownload)
    val cancel = RowMenuItem(stringResource(Res.string.translator_cancel), icon = PocketIcons.Close, onClick = onCancel)
    val delete =
        RowMenuItem(stringResource(Res.string.translator_delete), icon = PocketIcons.Delete, onClick = onDelete)
    val items =
        when (status.state) {
            PackState.Installed -> listOf(delete)
            is PackState.Downloading, PackState.Installing -> listOf(cancel)
            PackState.NotInstalled, is PackState.Waiting, is PackState.Failed -> listOf(download)
            PackState.Unknown, is PackState.Unavailable -> emptyList()
        }
    MenuSettingsRow(
        title = title,
        supporting = stateText(status),
        menuLabel = menuLabel,
        items = items,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun packName(pack: PackId): String = Routes.languageOf(pack)?.let { languageName(it) } ?: pack.value

/** "Installed · 75 MB", "48 MB to download", "Downloading 23 of 48 MB"… */
@Composable
private fun stateText(status: PackStatus): String {
    val megabytes = { bytes: Long -> PackMessages.megabytes(bytes).toString() }
    return when (val state = status.state) {
        PackState.Installed -> {
            stringResource(Res.string.translator_pack_installed, megabytes(status.installedBytes ?: 0))
        }

        PackState.NotInstalled -> {
            status.downloadBytes?.let { stringResource(Res.string.translator_to_download, megabytes(it)) }
                ?: stringResource(Res.string.translator_pack_not_downloaded)
        }

        is PackState.Downloading -> {
            stringResource(Res.string.translator_pack_downloading_row, megabytes(state.bytes), megabytes(state.total))
        }

        PackState.Installing -> {
            stringResource(Res.string.translator_pack_installing_row)
        }

        PackState.Unknown -> {
            stringResource(Res.string.translator_pack_checking_row)
        }

        is PackState.Waiting, is PackState.Failed, is PackState.Unavailable -> {
            stoppedText(state)
        }
    }
}

/** Why a pack isn't coming: waiting, failed, or never on this phone. */
@Composable
private fun stoppedText(state: PackState): String =
    when (state) {
        PackState.Waiting(PackWait.FOR_WIFI) -> {
            stringResource(Res.string.translator_pack_waiting_wifi_row)
        }

        PackState.Waiting(PackWait.FOR_CONFIRMATION) -> {
            stringResource(Res.string.translator_pack_waiting_confirmation_row)
        }

        PackState.Unavailable(PackUnavailable.NO_STORE) -> {
            stringResource(Res.string.translator_pack_no_store_row)
        }

        PackState.Unavailable(PackUnavailable.UNSUPPORTED_DEVICE) -> {
            stringResource(Res.string.translator_pack_unsupported_row)
        }

        else -> {
            val failure = (state as? PackState.Failed)?.reason
            if (failure is PackFailure.NoSpace) {
                val needed = PackMessages.megabytes(failure.bytesNeeded).toString()
                stringResource(Res.string.translator_pack_no_space_row, needed)
            } else {
                stringResource(Res.string.translator_pack_failed_row)
            }
        }
    }
