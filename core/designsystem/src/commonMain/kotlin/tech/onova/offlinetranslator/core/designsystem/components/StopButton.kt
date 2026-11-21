package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import tech.onova.offlinetranslator.core.designsystem.icons.PocketIcons
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes

/**
 * A round button with a square, beside a [MicrophoneButton] while it listens: stop, and drop what was being heard
 * (owner, 2026-11-19). Smaller than the microphone, and quieter in colour, since the microphone stays the main action.
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
        modifier = modifier.size(PocketSizes.touchTarget).semantics { contentDescription = label },
    ) {
        Icon(imageVector = PocketIcons.Stop, contentDescription = null)
    }
}
