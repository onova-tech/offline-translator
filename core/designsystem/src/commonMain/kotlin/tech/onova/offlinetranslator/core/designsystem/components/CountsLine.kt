package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing

/**
 * Counts grouped at the end of a line, "Characters: 123 | Words: 20 | Sentences: 8", with a thin line between them
 * (owner, 2026-11-13, the Notepad's footnote). Each number has the room of [widestValue], so a count growing never
 * moves the others. At large text they shrink together, down to [WholeWordText]'s least; past that they go one under
 * the other, at the end. Each count is read as one: "Characters: 123".
 */
@Composable
public fun CountsLine(
    counts: List<Count>,
    widestValue: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    if (counts.isEmpty()) return
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val room = constraints.maxWidth
        val gaps = with(density) { (PocketSpacing.xs * counts.size + PocketSpacing.sm * 2 * (counts.size - 1)).toPx() }

        fun widthAt(size: TextStyle): Float {
            val number = measurer.measure(widestValue, size, softWrap = false, maxLines = 1).size.width
            val labels = counts.sumOf { measurer.measure(it.label, size, softWrap = false, maxLines = 1).size.width }
            return labels + number * counts.size + gaps
        }
        val full = widthAt(style)
        val smallest = wholeWordSmallest(style)
        val least = smallest.scaleOf(style)
        val fitted =
            when {
                full <= room -> style
                widthAt(smallest) <= room -> scaled(style, (room / full * FIT_MARGIN).coerceIn(least, 1f))
                else -> null
            }
        if (fitted == null) {
            // Not even the smallest size fits on one line (a small window at 200 % text): one under the other.
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                counts.forEach { count ->
                    WholeWordText(
                        text = count.label + " " + count.value,
                        style = style,
                        color = color,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            val slot =
                with(density) {
                    measurer
                        .measure(widestValue, fitted, softWrap = false)
                        .size.width
                        .toDp()
                }
            Row(
                horizontalArrangement = Arrangement.spacedBy(PocketSpacing.sm, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            ) {
                counts.forEachIndexed { index, count ->
                    if (index > 0) VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    CountText(count, fitted, color, slot)
                }
            }
        }
    }
}

/** "Characters:" then the number in its [slot], so a longer number doesn't move what follows. */
@Composable
private fun CountText(
    count: Count,
    style: TextStyle,
    color: Color,
    slot: Dp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(PocketSpacing.xs),
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        Text(text = count.label, style = style, color = color, maxLines = 1, softWrap = false)
        Box(modifier = Modifier.width(slot)) {
            Text(text = count.value, style = style, color = color, maxLines = 1, softWrap = false)
        }
    }
}

private fun TextStyle.scaleOf(base: TextStyle): Float = fontSize.value / base.fontSize.value

private fun scaled(
    style: TextStyle,
    scale: Float,
): TextStyle =
    style.copy(
        fontSize = style.fontSize * scale,
        lineHeight = if (style.lineHeight.isSpecified) style.lineHeight * scale else style.lineHeight,
    )

/** A little below the exact fit, so rounding can't push the line past its room. */
private const val FIT_MARGIN = 0.97f
