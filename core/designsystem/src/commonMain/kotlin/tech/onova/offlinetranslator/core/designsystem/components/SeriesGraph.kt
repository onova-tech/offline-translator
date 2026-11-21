package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketShapes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing

/**
 * Lines over one pair of axes (spec 071, the Financial calculator's balance month by month): y from zero at the bottom
 * to [high] at the top, labelled [highLabel]. The current line is solid in the primary colour, a compared one dashed in
 * the tertiary, so they read apart without their colours; with two or more lines a legend names them, the compared
 * first. Each of the [marks] (spec 072) has a tick on the x axis and a dot on each line, and under the axis, ending at
 * its tick, what it is and each line's value there in its line's colour, the current line's first. [summary] is what a
 * screen reader says for the whole graph; the drawing itself is not announced. Give it a height, such as
 * `Modifier.flex(PocketSizes.graphMinHeight, PocketSizes.graphMaxHeight)`. Where the marks' words or rows don't fit,
 * the graph goes without them, its lines and top value alone; where even those and a graph's least height don't fit,
 * it isn't drawn at all (owner, 2026-11-13: hidden until there is room).
 */
@Composable
public fun SeriesGraph(
    lines: List<GraphLine>,
    high: Float,
    highLabel: String,
    marks: List<GraphMark>,
    summary: String,
    modifier: Modifier = Modifier,
) {
    val label = MaterialTheme.typography.labelSmall
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val inner = with(density) { (maxWidth - PocketSpacing.xs * 2).toPx() }
        val line = measurer.measure(highLabel, label).size.height
        val frame = PocketSpacing.xs * 2 + PocketSizes.graphMinHeight

        // The room for the top label and the legend, and with the marks their two lines and one per line's value.
        fun needs(rows: Int) = with(density) { (line * rows).toDp() } + frame
        val plain = 1 + (if (lines.size > 1) 1 else 0)
        val marked =
            marks.isNotEmpty() &&
                maxHeight >= needs(plain + MARK_LABEL_LINES + lines.size) &&
                marksFit(marks, inner, wholeWordSmallest(label), measurer)
        // Too low for the marks, the lines alone; too low even for those, nothing until the values fold away.
        if (maxHeight < needs(plain)) return@BoxWithConstraints
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer, PocketShapes.tile)
                    .padding(PocketSpacing.xs)
                    .clearAndSetSemantics { contentDescription = summary },
        ) {
            Text(text = highLabel, style = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Plot(lines, high, if (marked) marks else emptyList(), Modifier.fillMaxWidth().weight(1f))
            if (marked) MarkColumns(lines, marks, label)
            if (lines.size > 1) Legend(lines.sortedByDescending { it.compared })
        }
    }
}

/** The axes, the lines, and each mark's tick and dots. */
@Composable
private fun Plot(
    lines: List<GraphLine>,
    high: Float,
    marks: List<GraphMark>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val dash = PocketSizes.graphDash
    val tick = PocketSizes.graphTick
    val dot = PocketSizes.graphDot
    Canvas(modifier = modifier) {
        drawAxes(colors.outline)
        val span = high.takeIf { it > 0f } ?: 1f
        lines.forEach { line ->
            val color = if (line.compared) colors.tertiary else colors.primary
            drawSeries(line.points, span, color, if (line.compared) dash.toPx() else null)
        }
        marks.forEach { mark ->
            val x = mark.position.coerceIn(0f, 1f) * size.width
            drawLine(colors.outline, Offset(x, size.height), Offset(x, size.height - tick.toPx()), tick.toPx() / 2)
            lines.forEach { line ->
                val at = line.points.minByOrNull { kotlin.math.abs(it.position - mark.position) } ?: return@forEach
                if (kotlin.math.abs(at.position - mark.position) > NEAR) return@forEach
                val y = size.height - (at.value / span).coerceIn(0f, 1f) * size.height
                drawCircle(if (line.compared) colors.tertiary else colors.primary, dot.toPx(), Offset(x, y))
            }
        }
    }
}

/**
 * Under the axis, a column per mark that ends at its tick: what it is, then each line's value in its colour, the
 * current line's first.
 */
@Composable
private fun MarkColumns(
    lines: List<GraphLine>,
    marks: List<GraphMark>,
    style: TextStyle,
) {
    val colors = MaterialTheme.colorScheme
    val order = lines.indices.sortedBy { lines[it].compared }
    Row(modifier = Modifier.fillMaxWidth()) {
        var start = 0f
        marks.forEach { mark ->
            val share = (mark.position - start).coerceAtLeast(MIN_SHARE)
            start = mark.position
            Column(modifier = Modifier.weight(share), horizontalAlignment = Alignment.End) {
                WholeWordText(
                    text = mark.label,
                    style = style,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = MARK_LABEL_LINES,
                    modifier = Modifier.fillMaxWidth(),
                )
                order.forEach { index ->
                    WholeWordText(
                        text = mark.values.getOrElse(index) { "" },
                        style = style,
                        color = if (lines[index].compared) colors.tertiary else colors.primary,
                        textAlign = TextAlign.End,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** Whether every mark's words fit its column, each word of what it is and each value whole, at the [smallest] size. */
private fun marksFit(
    marks: List<GraphMark>,
    width: Float,
    smallest: TextStyle,
    measurer: TextMeasurer,
): Boolean {
    var start = 0f
    return marks.all { mark ->
        val room = (mark.position - start) * width
        start = mark.position
        val words = mark.label.split(' ').filter { it.isNotEmpty() } + mark.values
        words.all { measurer.measure(it, smallest, softWrap = false, maxLines = 1).size.width <= room }
    }
}

/** The left and bottom axes, in [color], half a line's width. */
private fun DrawScope.drawAxes(color: Color) {
    val axis = PocketSizes.graphStroke.toPx() / 2
    drawLine(color, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = axis)
    drawLine(color, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = axis)
}

/** A line's points from the bottom (zero) to the top ([span]); a [dash] length makes it dashed. */
private fun DrawScope.drawSeries(
    points: List<GraphPoint>,
    span: Float,
    color: Color,
    dash: Float?,
) {
    if (points.size < 2) return
    val path = Path()
    points.forEachIndexed { index, point ->
        val x = point.position.coerceIn(0f, 1f) * size.width
        val y = size.height - (point.value / span).coerceIn(0f, 1f) * size.height
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    val effect = dash?.let { PathEffect.dashPathEffect(floatArrayOf(it, it)) }
    drawPath(path, color, style = Stroke(width = PocketSizes.graphStroke.toPx(), pathEffect = effect))
}

/** Each line's sample, solid or dashed, then its name: "- - Kept   —— Now". */
@Composable
private fun Legend(lines: List<GraphLine>) {
    val colors = MaterialTheme.colorScheme
    val dash = PocketSizes.graphDash
    Row(
        horizontalArrangement = Arrangement.spacedBy(PocketSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = PocketSpacing.xxs),
    ) {
        lines.forEach { line ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(PocketSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(modifier = Modifier.width(PocketSizes.legendSample).height(PocketSizes.graphStroke)) {
                    val y = size.height / 2
                    val effect =
                        if (line.compared) PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), dash.toPx())) else null
                    drawLine(
                        color = if (line.compared) colors.tertiary else colors.primary,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = size.height,
                        pathEffect = effect,
                    )
                }
                Box { Text(text = line.name, style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

/** What a mark is may take two lines in its column, "1 ano e 6 meses". */
private const val MARK_LABEL_LINES = 2

/** A point this close to a mark, across, is the mark's own: every month is a point (spec 072). */
private const val NEAR = 0.001f

/** A column is never given no width at all. */
private const val MIN_SHARE = 0.01f
