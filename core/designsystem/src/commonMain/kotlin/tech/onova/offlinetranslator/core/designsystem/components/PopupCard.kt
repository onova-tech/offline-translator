package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketShapes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing

/**
 * A short task in a card in the middle of the screen, the screen dimmed behind it: the Conversation's "Correct what was
 * heard" (owner, 2026-11-23: "should become a popup on the middle of the screen"; `docs/ui-rules.md` L5). With [turned]
 * the whole card faces the person across the phone, as a turned half does (C1); the keyboard stays where Android puts
 * it, upright at the bottom edge.
 */
@Composable
public fun PopupCard(
    onDismissRequest: () -> Unit,
    turned: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Its own width, as Material's dialogs do: at the platform's default dialog width a text field inside never let
    // the screen settle (found by the correction's screen tests, 2026-11-23).
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = PocketShapes.card,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier =
                modifier
                    .padding(horizontal = PocketSpacing.lg)
                    .widthIn(max = POPUP_MAX_WIDTH)
                    .fillMaxWidth()
                    .graphicsLayer { rotationZ = if (turned) HALF_TURN else 0f },
        ) {
            Column(
                modifier = Modifier.padding(PocketSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(PocketSpacing.md),
                content = content,
            )
        }
    }
}

/** As wide as Material's dialogs at most, so a tablet's popup stays a popup. */
private val POPUP_MAX_WIDTH = 560.dp
