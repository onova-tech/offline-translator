package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes
import tech.onova.offlinetranslator.core.designsystem.tokens.isSmallWindow

/**
 * A line that keeps its place when it has nothing to say, so a message that appears or clears moves nothing below
 * it. An empty line is an empty box: no text is drawn, so there is nothing to read. It reserves a touch target's
 * height, room for a message that wraps to two lines; a [compact] line reserves one line of [style], for a short
 * reading that never wraps (owner, 2026-10-31: tools fit their screen without scrolling). [maxLines] cuts a long
 * text with an ellipsis. In a small window an empty line takes no room, because a dial or a list needs it more: the
 * message then takes its room from the content, never from the controls at the bottom (spec 015).
 */
@Composable
public fun ReservedLine(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    compact: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    contentAlignment: Alignment = Alignment.TopStart,
) {
    val lineHeight = style.lineHeight
    val oneLine = with(LocalDensity.current) { (if (lineHeight.isSpecified) lineHeight else style.fontSize).toDp() }
    val reserved =
        when {
            isSmallWindow && text.isEmpty() -> 0.dp
            compact -> oneLine
            else -> PocketSizes.touchTarget
        }
    Box(modifier = modifier.heightIn(min = reserved), contentAlignment = contentAlignment) {
        if (text.isNotEmpty()) {
            Text(text = text, style = style, color = color, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
        }
    }
}
