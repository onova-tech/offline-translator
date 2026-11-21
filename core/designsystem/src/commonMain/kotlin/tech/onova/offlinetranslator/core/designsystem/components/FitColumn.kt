package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing

/**
 * A tool's content that shares the height of its screen instead of scrolling (owner, 2026-10-31: tools work
 * at different screen sizes mostly without scrolling). Children are laid out top to bottom at their own
 * height. A child marked [flex] (a graph, a dial, a preview) takes a share of what the others
 * leave, between its own minimum and maximum, so it grows on a tall screen and shrinks on a short one. Only
 * when the others plus the minimums are taller than the screen does the column scroll.
 *
 * With [centerLeftover], space that no child wants (all of them at their maximum) is split above and below,
 * which centers a dial between the reading and the controls. A child marked [attached] sits [attachedSpacing] under
 * the one before it rather than [spacing], as the Notepad's counts under its text (spec 072). Place it where the
 * screen gives it a height: inside `ToolLayout(scrollContent = false)` with `Modifier.weight(1f)`.
 */
@Composable
public fun FitColumn(
    modifier: Modifier = Modifier,
    spacing: Dp = PocketSpacing.md,
    centerLeftover: Boolean = false,
    attachedSpacing: Dp = PocketSpacing.xs,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val viewport = if (constraints.hasBoundedHeight) constraints.maxHeight else 0
        val policy =
            remember(viewport, spacing, centerLeftover, attachedSpacing) {
                FitPolicy(viewport, spacing, centerLeftover, attachedSpacing)
            }
        Layout(
            content = content,
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            measurePolicy = policy,
        )
    }
}

/**
 * Marks a child of [FitColumn] as flexible: its height is a share of the space the other children leave,
 * at least [min], at most [max], and in proportion to [weight] among the flexible children. The child gets
 * exactly that height and the column's width as its limit, so a square dial can use `Modifier.aspectRatio(1f)`.
 */
public fun Modifier.flex(
    min: Dp,
    max: Dp = Dp.Infinity,
    weight: Float = 1f,
): Modifier = layoutId(Flex(min, max, weight))

/**
 * Marks a child of [FitColumn] that belongs with the one before it, such as a footnote under its text: it sits the
 * column's small `attachedSpacing` away from it (spec 072).
 */
public fun Modifier.attached(): Modifier = layoutId(Attached)

internal object Attached

internal data class Flex(
    val min: Dp,
    val max: Dp,
    val weight: Float,
)

private class FitPolicy(
    private val viewport: Int,
    private val spacing: Dp,
    private val centerLeftover: Boolean,
    private val attachedSpacing: Dp,
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val width = constraints.maxWidth
        // The gap above each child after the first: the column's, or the small one of an attached child.
        val gaps = measurables.map { if (it.layoutId == Attached) attachedSpacing.roundToPx() else spacing.roundToPx() }
        val placeables = arrayOfNulls<Placeable>(measurables.size)
        var fixed = gaps.drop(1).sum()
        val flexible = mutableListOf<Pair<Int, Flex>>()
        measurables.forEachIndexed { index, measurable ->
            val flex = measurable.layoutId as? Flex
            if (flex == null) {
                val placeable = measurable.measure(Constraints(maxWidth = width))
                placeables[index] = placeable
                fixed += placeable.height
            } else {
                flexible += index to flex
            }
        }
        val heights =
            FlexShare.allocate(
                free = viewport - fixed,
                mins = flexible.map { (_, flex) -> flex.min.pxOrMax(this) },
                maxs = flexible.map { (_, flex) -> flex.max.pxOrMax(this) },
                weights = flexible.map { (_, flex) -> flex.weight },
            )
        flexible.forEachIndexed { slot, (index, _) ->
            val height = heights[slot]
            placeables[index] =
                measurables[index].measure(Constraints(maxWidth = width, minHeight = height, maxHeight = height))
        }
        val placed = placeables.requireNoNulls()
        val total = fixed + heights.sum()
        val height = maxOf(total, viewport)
        val top = if (centerLeftover) (height - total) / 2 else 0
        return layout(width, height) {
            var y = top
            placed.forEachIndexed { index, placeable ->
                if (index > 0) y += gaps[index]
                placeable.place((width - placeable.width) / 2, y)
                y += placeable.height
            }
        }
    }

    private fun Dp.pxOrMax(density: Density): Int =
        if (this == Dp.Infinity) Int.MAX_VALUE else with(density) { roundToPx() }
}

/** Shares the height left over among flexible children; pure, so it is tested without a screen. */
internal object FlexShare {
    /**
     * Heights for children with [mins], [maxs] and [weights]: every child gets its minimum, then [free] minus
     * the minimums is shared by weight, a child that reaches its maximum handing the rest to the others.
     */
    fun allocate(
        free: Int,
        mins: List<Int>,
        maxs: List<Int>,
        weights: List<Float>,
    ): IntArray {
        val heights = IntArray(mins.size) { mins[it] }
        var extra = free - heights.sum()
        val open = mins.indices.filter { heights[it] < maxs[it] }.toMutableList()
        while (extra > 0 && open.isNotEmpty()) {
            val totalWeight = open.sumOf { weights[it].toDouble() }
            var given = 0
            val capped = mutableListOf<Int>()
            open.forEach { index ->
                val share = (extra * weights[index] / totalWeight).toInt()
                val room = maxs[index] - heights[index]
                val taken = minOf(share, room)
                heights[index] += taken
                given += taken
                if (heights[index] >= maxs[index]) capped += index
            }
            open.removeAll(capped)
            extra -= given
            // Whole pixels left by the rounding go to the first open child; the loop ends once none is open.
            if (given == 0 && open.isNotEmpty()) {
                val index = open.first()
                val taken = minOf(extra, maxs[index] - heights[index])
                heights[index] += taken
                extra -= taken
                if (heights[index] >= maxs[index]) open.remove(index)
            }
        }
        return heights
    }
}
