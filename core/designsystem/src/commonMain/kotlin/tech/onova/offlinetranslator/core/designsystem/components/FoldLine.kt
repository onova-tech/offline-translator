package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import tech.onova.offlinetranslator.core.designsystem.icons.PocketIcons
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketMotion
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing

/**
 * A thin line with a round button in its middle that folds what is under it away and back (spec 071, owner,
 * 2026-11-12: the Financial calculator's values, so the results and the graph get the room). It shows ∨ while open
 * and ∧ while folded; screen readers hear [foldLabel] or [unfoldLabel]. Put what it folds in a [FoldAway].
 */
@Composable
public fun FoldLine(
    folded: Boolean,
    onFold: (Boolean) -> Unit,
    foldLabel: String,
    unfoldLabel: String,
    modifier: Modifier = Modifier,
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        FilledTonalIconButton(onClick = { onFold(!folded) }, modifier = PocketSizes.iconButtonSize) {
            Icon(
                imageVector = if (folded) PocketIcons.ChevronUp else PocketIcons.ChevronDown,
                contentDescription = if (folded) unfoldLabel else foldLabel,
            )
        }
    }
}

/**
 * What a [FoldLine] folds: it shrinks away towards the line and grows back from it, animated rather than gone at once
 * (owner, 2026-11-12), at the design system's pace, which the phone's "Remove animations" makes instant.
 */
@Composable
public fun FoldAway(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically(tween(PocketMotion.MEDIUM_MILLIS)) + fadeIn(tween(PocketMotion.MEDIUM_MILLIS)),
        exit = shrinkVertically(tween(PocketMotion.MEDIUM_MILLIS)) + fadeOut(tween(PocketMotion.MEDIUM_MILLIS)),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PocketSpacing.sm), content = content)
    }
}
