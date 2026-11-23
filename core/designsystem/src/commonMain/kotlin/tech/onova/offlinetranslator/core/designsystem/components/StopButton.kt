package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import tech.onova.offlinetranslator.core.designsystem.icons.PocketIcons
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes

/**
 * A round button with a square, beside a [MicrophoneButton]: stop, and drop what was being heard (owner, 2026-11-19),
 * or stop a translation being read. Smaller than the microphone, and red, since it means stop (owner, 2026-11-22).
 */
@Composable
public fun StopButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors =
            IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        modifier = modifier.size(PocketSizes.touchTarget).semantics { contentDescription = label },
    ) {
        Icon(imageVector = PocketIcons.Stop, contentDescription = null)
    }
}

/**
 * A round button with a speaker, in the [StopButton]'s place beside a [MicrophoneButton] when nothing is heard or read:
 * hear the half's latest translation again (spec 082 FR-017; owner, 2026-11-22). The stop button's size, in another
 * colour; [enabled] only when the language's voice is ready.
 */
@Composable
public fun HearAgainButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors =
            IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
        modifier = modifier.size(PocketSizes.touchTarget).semantics { contentDescription = label },
    ) {
        Icon(imageVector = PocketIcons.Speaker, contentDescription = null)
    }
}
