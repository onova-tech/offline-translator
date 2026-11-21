package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import tech.onova.offlinetranslator.core.designsystem.icons.PocketIcons
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketColorSchemes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketMotion
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketShapes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing

/**
 * A card whose header opens and closes what it holds (owner, 2026-11-20: Manage languages): the header's title and
 * supporting line, an optional [status] mark, and ⌄ turning to ⌃ while open; then a divider and the content, folding
 * like [FoldAway]. The header is one touch target, and screen readers hear it as expanded or collapsed.
 */
@Composable
public fun ExpandableCard(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    status: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val turn by animateFloatAsState(if (expanded) HALF_TURN else 0f, tween(PocketMotion.MEDIUM_MILLIS))
    Surface(
        shape = PocketShapes.card,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            ListItem(
                headlineContent = { Text(title) },
                supportingContent = supporting?.let { { Text(it) } },
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PocketSpacing.xs),
                    ) {
                        status?.invoke()
                        Icon(PocketIcons.ChevronDown, contentDescription = null, modifier = Modifier.rotate(turn))
                    }
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier =
                    Modifier
                        .clip(PocketShapes.card)
                        .clickable { onExpandedChange(!expanded) }
                        .semantics {
                            if (expanded) {
                                collapse {
                                    onExpandedChange(false)
                                    true
                                }
                            } else {
                                expand {
                                    onExpandedChange(true)
                                    true
                                }
                            }
                        },
            )
            FoldAway(visible = expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier =
                        Modifier.padding(
                            start = PocketSpacing.xs,
                            end = PocketSpacing.xs,
                            bottom = PocketSpacing.xs,
                        ),
                    verticalArrangement = Arrangement.spacedBy(PocketSpacing.xxs),
                    content = content,
                )
            }
        }
    }
}

/**
 * Whether something is all there ([complete]) or only in part, and whether it's in use ([enabled]): a check in a
 * circle in the primary color, or gray when not in use; a warning triangle in amber, or gray when not in use (owner,
 * 2026-11-20: Manage languages' ✅ ☑ ⚠). [label] says it to screen readers; null where the same words are beside it.
 */
@Composable
public fun StatusMark(
    complete: Boolean,
    enabled: Boolean,
    label: String?,
    modifier: Modifier = Modifier,
) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE
    val tint =
        when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
            complete -> MaterialTheme.colorScheme.primary
            dark -> PocketColorSchemes.warningDark
            else -> PocketColorSchemes.warningLight
        }
    Icon(
        imageVector = if (complete) PocketIcons.Complete else PocketIcons.Partial,
        contentDescription = label,
        tint = tint,
        modifier = modifier,
    )
}

/** The darkest a light theme's surface gets: below it, the surface is dark. */
private const val DARK_SURFACE = 0.5f
