package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * Text that never splits a word across lines (owner's phone pass: "Calculador-a"). It wraps only between words; when
 * its longest word is wider than the space, the whole text shrinks just enough for that word to fit. With
 * [singleLine] the whole text is that word: it shrinks to stay on one line. With [maxLines] it also shrinks until
 * its lines fit. It never goes under 60% of its size, nor under 9 sp unless its size already is. Intrinsic sizes are
 * supported, so equal-height grids can measure it.
 */
@Composable
public fun WholeWordText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Start,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
) {
    val measurer = rememberTextMeasurer()
    val drawColor = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }
    val base = style.copy(textAlign = textAlign, hyphens = Hyphens.None)
    // The last layout, read by the draw pass that follows each measure pass.
    val shown = remember { arrayOfNulls<TextLayoutResult>(1) }
    val policy =
        remember(text, base, measurer, singleLine, maxLines) {
            WholeWordPolicy(text, base, measurer, shown, singleLine, maxLines)
        }
    // A new draw for every new policy: when new words keep the old room, as in a stepper's middle, a remeasure alone
    // draws nothing again, and a lambda that only read [shown] stayed the same one (owner, 2026-11-13, spec 074).
    val draw: DrawScope.() -> Unit =
        remember(policy, drawColor) { { shown[0]?.let { drawText(it, color = drawColor) } } }
    Layout(
        modifier =
            modifier
                .semantics {
                    this.text = AnnotatedString(text)
                    getTextLayoutResult { results -> shown[0]?.let(results::add) != null }
                }.drawBehind(draw),
        measurePolicy = policy,
    )
}

private class WholeWordPolicy(
    private val text: String,
    private val style: TextStyle,
    private val measurer: TextMeasurer,
    private val shown: Array<TextLayoutResult?>,
    singleLine: Boolean,
    private val maxLines: Int,
) : MeasurePolicy {
    // Past this share of the asked size a word may break rather than become unreadable.
    private val minScale = smallestScale(style)

    // Small linking words stay with the word before them ("Conversor de" / "Medidas"), in any language.
    private val glued = if (singleLine) text else glueLinkingWords(text)

    // A single-line text is one unit: it shrinks to fit the width before it ever wraps.
    private val words = if (singleLine) listOf(text) else text.split(' ', ' ').filter { it.isNotEmpty() }

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val result = fit(constraints.maxWidth, constraints.minWidth)
        shown[0] = result
        val width = result.size.width.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = result.size.height.coerceIn(constraints.minHeight, constraints.maxHeight)
        return layout(width, height) {}
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int = fit(width).size.height

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int = fit(width).size.height

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = words.maxOfOrNull { widthOf(it, style) } ?: 0

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = widthOf(glued, style)

    /**
     * With [minWidth], the layout is at least that wide, so a text aligned to the center or the end moves within it.
     */
    private fun fit(
        maxWidth: Int,
        minWidth: Int = 0,
    ): TextLayoutResult {
        if (maxWidth == Constraints.Infinity) return measurer.measure(glued, style)
        var fitted = style
        var total = 1f
        // Text width isn't exactly proportional to its size (rounding, hinting), so the size is
        // checked again after each step down, and never goes below minScale.
        repeat(MAX_STEPS) {
            val widest = words.maxOfOrNull { widthOf(it, fitted) } ?: 0
            val step = (maxWidth.toFloat() / widest * STEP_MARGIN).coerceAtLeast(minScale / total)
            if (widest <= maxWidth || step >= 1f) return@repeat
            fitted = scaled(fitted, step)
            total *= step
        }
        var result = layout(fitted, maxWidth, minWidth)
        // A word or a glued pair that still got split (the measured width and the line breaker can
        // disagree by a few pixels) is fixed by shrinking a little more, never past the minimum.
        // A text over its line limit is fixed the same way.
        while (total > minScale && (result.splitsAGluedPair() || result.lineCount > maxLines)) {
            fitted = scaled(fitted, GLUE_RETRY_STEP)
            total *= GLUE_RETRY_STEP
            result = layout(fitted, maxWidth, minWidth)
        }
        return result
    }

    /** A line that ends on anything but a plain space broke inside a word or between glued words. */
    private fun TextLayoutResult.splitsAGluedPair(): Boolean =
        (0 until lineCount - 1).any { line -> glued.getOrNull(getLineEnd(line) - 1) != ' ' }

    private fun layout(
        style: TextStyle,
        maxWidth: Int,
        minWidth: Int,
    ): TextLayoutResult =
        measurer.measure(glued, style, constraints = Constraints(minWidth = minWidth, maxWidth = maxWidth))

    private fun scaled(
        style: TextStyle,
        scale: Float,
    ): TextStyle =
        style.copy(
            fontSize = style.fontSize * scale,
            lineHeight = if (style.lineHeight.isSpecified) style.lineHeight * scale else style.lineHeight,
        )

    private fun widthOf(
        part: String,
        style: TextStyle,
    ): Int = measurer.measure(part, style, softWrap = false, maxLines = 1).size.width
}

/** The least [WholeWordText] shrinks [style] to, for a layout that must know whether a word can fit. */
public fun wholeWordSmallest(style: TextStyle): TextStyle = style.copy(fontSize = style.fontSize * smallestScale(style))

private fun smallestScale(style: TextStyle): Float = maxOf(MIN_SCALE, minOf(1f, MIN_SIZE.value / style.fontSize.value))

/** Attempts at shrinking before giving up; one or two are enough in practice. */
private const val MAX_STEPS = 4

/** The smallest share of the asked size the text shrinks to. */
private const val MIN_SCALE = 0.6f

/** The smallest size the text shrinks to, at the default text setting. */
private val MIN_SIZE = 9.sp

/** Each step aims a little below the exact fit, so rounding can't push the word over. */
private const val STEP_MARGIN = 0.97f

/** Shrink step when a linking word still breaks away from its neighbour. */
private const val GLUE_RETRY_STEP = 0.94f

/**
 * Joins each short all-lowercase word (de, da, of, la, y, and...) to the word before it with a
 * no-break space, so a line never starts with one: "Conversor de / Medidas", not "Conversor / de Medidas".
 * The first and last words are never joined, and capitalised or longer words keep their normal breaks.
 */
internal fun glueLinkingWords(text: String): String {
    val parts = text.split(' ')
    if (parts.size < MIN_PARTS_TO_GLUE) return text
    return buildString {
        parts.forEachIndexed { index, part ->
            if (index > 0) append(if (index < parts.lastIndex && part.isLinkingWord()) NO_BREAK_SPACE else ' ')
            append(part)
        }
    }
}

private fun String.isLinkingWord(): Boolean = length in 1..LINKING_WORD_MAX && all { it.isLetter() && it.isLowerCase() }

private const val NO_BREAK_SPACE = '\u00A0'
private const val LINKING_WORD_MAX = 3
private const val MIN_PARTS_TO_GLUE = 3
