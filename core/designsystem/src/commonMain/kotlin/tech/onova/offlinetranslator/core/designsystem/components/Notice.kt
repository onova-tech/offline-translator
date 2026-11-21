package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import tech.onova.offlinetranslator.core.designsystem.icons.PocketIcons
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketShapes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing

/**
 * An informational card: a disclaimer, an explanation before a permission request, or an offer
 * with actions (spec 004 research.md R16). Actions wrap onto new lines at large text sizes. [extra] goes between the
 * text and the actions, such as a download's progress bar (spec 012). With [onClose], a gray text button named
 * [closeLabel] ("Ignore") comes first in the actions' row and closes it (owner, 2026-11-20: a notice nothing can fix
 * mustn't stay in the way; a × beside the text squeezed the buttons, and at the row's start it felt strange).
 */
@Composable
public fun Notice(
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector = PocketIcons.Info,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    closeLabel: String = "",
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Surface(
        shape = PocketShapes.card,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(PocketSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PocketSpacing.xs),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(PocketSpacing.sm)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(PocketSpacing.xxs)) {
                    if (title != null) Text(text = title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            extra?.invoke(this)
            if (actions != null || onClose != null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(PocketSpacing.xs, Alignment.End),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row {
                        if (onClose != null) {
                            TextButton(
                                onClick = onClose,
                                colors =
                                    ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                            ) { Text(closeLabel) }
                        }
                        actions?.invoke(this)
                    }
                }
            }
        }
    }
}
