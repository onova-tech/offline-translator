package tech.onova.offlinetranslator.support

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Principle X audit over the merged semantics tree. Every element a user can act on (click,
 * toggle, adjust, type) must:
 * - have something for a screen reader to announce;
 * - have a touch target of at least 48×48 dp, or what the window allows (Principle X);
 * - not be partly covered by another actionable element, whose part would take its taps.
 *
 * Text a screen reader stops on must also have something to say: an empty display line is
 * reported as "no label" by the on-device checks (seen on the calculator's first launch).
 */
object AccessibilityAudit {
    private const val MIN_TOUCH_TARGET_DP = 48f
    private const val ROUNDING_TOLERANCE_PX = 1f

    // Segmented buttons overlap by their 1 dp border on purpose, so the borders merge.
    private const val SHARED_BORDER_DP = 2f

    /**
     * With [judgeClicks] false, a clickable control isn't held to 48 dp here: Compose inflates its touch bounds, and a
     * scroller's edge cuts that margin off, so [touchAreasThatOverlap] judges those instead.
     */
    fun violations(
        root: SemanticsNode,
        density: Density,
        judgeClicks: Boolean = true,
        minTargetDp: Float = MIN_TOUCH_TARGET_DP,
    ): List<String> {
        val minPx = with(density) { minTargetDp * this.density } - ROUNDING_TOLERANCE_PX
        val interactive = root.descendants().filter { it.isInteractive() }
        val perElement =
            interactive.flatMap { node ->
                val label = node.label()
                val bounds = node.touchBoundsInRoot
                // Scrolled out of view: clipped to nothing on screen, but laid out at full size.
                val offScreen = node.boundsInRoot.isEmpty && node.size.width > 0 && node.size.height > 0
                // Partly scrolled out at a list's edge: judged by its laid-out size, as the user
                // scrolls it into view to use it.
                val clipped = bounds.height < node.size.height - ROUNDING_TOLERANCE_PX
                // The same at a sideways row's edge, for a chip partly cut off.
                val clippedSideways = bounds.width < node.size.width - ROUNDING_TOLERANCE_PX
                val width =
                    if (clipped ||
                        clippedSideways
                    ) {
                        maxOf(bounds.width, node.size.width.toFloat())
                    } else {
                        bounds.width
                    }
                val height = if (clipped) node.size.height.toFloat() else bounds.height
                listOfNotNull(
                    "element #${node.id} has no label for screen readers".takeIf { label.isBlank() },
                    "'$label' touch target ${width}x${height}px is below 48dp"
                        .takeIf {
                            !offScreen && (width < minPx || height < minPx) &&
                                (judgeClicks || !node.config.contains(SemanticsActions.OnClick))
                        },
                )
            }
        val emptyText =
            root.descendants().filter { it.readsAsEmptyText() }.map { "element #${it.id} is text with nothing to read" }
        val borderPx = with(density) { SHARED_BORDER_DP * this.density }
        val canOverlap = if (judgeClicks) interactive else interactive.filterNot { it.isScrim(root) }
        return perElement + emptyText + overlaps(canOverlap, borderPx)
    }

    /**
     * Nesting is fine (a row and its switch), and so is a shared border; any other partial
     * overlap means one control takes taps meant for the other.
     */
    private fun overlaps(
        nodes: List<SemanticsNode>,
        borderPx: Float,
    ): List<String> =
        nodes.flatMapIndexed { index, a ->
            nodes.drop(index + 1).mapNotNull { b ->
                val shared = a.boundsInRoot.intersect(b.boundsInRoot)
                // intersect() of disjoint rects has a negative width or height.
                val beyondBorder = shared.width > borderPx && shared.height > borderPx
                val partial = beyondBorder && shared != a.boundsInRoot && shared != b.boundsInRoot
                "'${a.label()}' and '${b.label()}' partly overlap at $shared"
                    .takeIf { partial }
            }
        }

    /**
     * A control squeezed under the window's [touchTarget] shows when its area, inflated to that size, runs into its
     * neighbour's. Compose's own inflation stays at 48 dp, but it only takes a tap that lands on no control, so two
     * controls at the target with a small gap between them are fine (Principle X).
     */
    fun touchAreasThatOverlap(
        root: SemanticsNode,
        density: Density,
        touchTarget: Dp = MIN_TOUCH_TARGET_DP.dp,
    ): List<String> {
        val borderPx = with(density) { SHARED_BORDER_DP * this.density }
        val targetPx = with(density) { touchTarget.toPx() }
        // A control cut by a scroller's edge isn't in the way of anything until it's scrolled into view.
        val clickable =
            root.descendants().filter {
                it.config.contains(SemanticsActions.OnClick) && !it.boundsInRoot.isEmpty && !it.isScrim(root) &&
                    it.boundsInRoot.width >= it.size.width - ROUNDING_TOLERANCE_PX &&
                    it.boundsInRoot.height >= it.size.height - ROUNDING_TOLERANCE_PX
            }
        return clickable.flatMapIndexed { index, a ->
            clickable.drop(index + 1).mapNotNull { b ->
                val areaOfA = a.touchArea(targetPx)
                val areaOfB = b.touchArea(targetPx)
                val shared = areaOfA.intersect(areaOfB)
                val beyondBorder = shared.width > borderPx && shared.height > borderPx
                val nested = shared == areaOfA || shared == areaOfB
                "touch areas of '${a.label()}' and '${b.label()}' overlap".takeIf { beyondBorder && !nested }
            }
        }
    }

    /** Where Compose inflates a node at all, the same area but up to [targetPx] only. */
    private fun SemanticsNode.touchArea(targetPx: Float): Rect {
        val bounds = boundsInRoot
        if (touchBoundsInRoot == bounds) return bounds
        val sideways = ((targetPx - bounds.width) / 2).coerceAtLeast(0f)
        val upAndDown = ((targetPx - bounds.height) / 2).coerceAtLeast(0f)
        return Rect(bounds.left - sideways, bounds.top - upAndDown, bounds.right + sideways, bounds.bottom + upAndDown)
    }

    /** A bottom sheet's scrim covers its whole window, under the sheet, and takes only the taps outside it. */
    private fun SemanticsNode.isScrim(root: SemanticsNode): Boolean =
        size.width >= root.size.width - ROUNDING_TOLERANCE_PX && size.height >= root.size.height - ROUNDING_TOLERANCE_PX

    /**
     * What a screen reader can reach: a lazy list keeps items it scrolled away, deactivated or not placed, for reuse,
     * with stale bounds; they aren't on screen, and Android's own accessibility tree leaves them out (spec 078, where
     * Settings first scrolled in an audit).
     */
    private fun SemanticsNode.descendants(): List<SemanticsNode> =
        listOf(this) +
            children
                .filter { it.layoutInfo.isPlaced && !it.layoutInfo.isDeactivated }
                .flatMap { it.descendants() }

    private fun SemanticsNode.isInteractive(): Boolean =
        config.contains(SemanticsActions.OnClick) ||
            config.contains(SemanticsActions.SetProgress) ||
            config.contains(SemanticsActions.SetText) ||
            config.contains(SemanticsProperties.ToggleableState)

    private fun SemanticsNode.readsAsEmptyText(): Boolean {
        val texts = config.getOrNull(SemanticsProperties.Text) ?: return false
        return texts.all { it.text.isBlank() } && label().isBlank()
    }

    private fun SemanticsNode.label(): String =
        listOfNotNull(
            config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" "),
            config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text },
            config.getOrNull(SemanticsProperties.EditableText)?.text,
        ).joinToString(" ").trim()
}
