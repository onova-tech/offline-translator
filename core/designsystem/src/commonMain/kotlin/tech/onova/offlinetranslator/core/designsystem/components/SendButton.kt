package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
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
 * ^ in a Conversation half's microphone place while its heard turn waits (spec 082 FR-016; owner, 2026-11-22: "the
 * microphone buttons turns into an arrow pointing up"): the microphone's size and listening colours, so the half
 * doesn't move when it comes; [label] ("Send") is said to screen readers.
 */
@Composable
public fun SendButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    FilledIconButton(
        onClick = onClick,
        shape = CircleShape,
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
            ),
        modifier = modifier.size(PocketSizes.microphoneButton).semantics { contentDescription = label },
    ) {
        Icon(imageVector = PocketIcons.ArrowUp, contentDescription = null)
    }
}
