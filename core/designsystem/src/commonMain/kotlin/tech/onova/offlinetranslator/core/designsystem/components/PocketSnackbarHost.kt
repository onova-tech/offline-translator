package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import tech.onova.offlinetranslator.core.designsystem.icons.PocketIcons
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing

/**
 * The app's messages at the bottom of a screen, a [HintVisuals] with its (i). They wear the theme's own colors, dark
 * in the dark theme, and not Material's inverse ones, which drew a white bar on a dark screen (owner, 2026-11-10). An
 * action ("Undo") is in the primary color.
 */
@Composable
public fun PocketSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        val colors = MaterialTheme.colorScheme
        if (data.visuals is HintVisuals) {
            Snackbar(containerColor = colors.secondaryContainer, contentColor = colors.onSecondaryContainer) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PocketSpacing.sm),
                ) {
                    Icon(PocketIcons.Info, contentDescription = null)
                    Text(data.visuals.message)
                }
            }
        } else {
            Snackbar(
                snackbarData = data,
                containerColor = colors.secondaryContainer,
                contentColor = colors.onSecondaryContainer,
                actionColor = colors.primary,
                actionContentColor = colors.primary,
                dismissActionContentColor = colors.onSecondaryContainer,
            )
        }
    }
}
