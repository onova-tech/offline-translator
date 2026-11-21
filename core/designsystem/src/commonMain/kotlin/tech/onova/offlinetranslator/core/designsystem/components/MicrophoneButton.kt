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
import androidx.compose.ui.semantics.stateDescription
import tech.onova.offlinetranslator.core.designsystem.icons.PocketIcons
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes

/**
 * The Conversation's large microphone (spec 012), one per half: named with its language ("Speak Portuguese"), filled
 * with the strong colour while it listens, and [listeningLabel] then said to screen readers. Disabled while the phone
 * speaks, so nothing listens to it (FR-025). It isn't a [BottomAction], so it takes no [UiRole]: a Conversation has
 * no filled main action at the bottom, its microphones are its main actions (`docs/ui-rules.md`, A3 exception).
 */
@Composable
public fun MicrophoneButton(
    label: String,
    listening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    listeningLabel: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = if (listening) colors.primary else colors.primaryContainer,
                contentColor = if (listening) colors.onPrimary else colors.onPrimaryContainer,
            ),
        modifier =
            modifier
                .size(PocketSizes.microphoneButton)
                .semantics {
                    contentDescription = label
                    if (listening && listeningLabel != null) stateDescription = listeningLabel
                },
    ) {
        Icon(imageVector = PocketIcons.Microphone, contentDescription = null)
    }
}
