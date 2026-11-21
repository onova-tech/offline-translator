package tech.onova.offlinetranslator.support

import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import tech.onova.offlinetranslator.core.designsystem.components.UiRole
import tech.onova.offlinetranslator.core.designsystem.components.UiRoleKey

/**
 * Whether a screen fits its window (owner, 2026-10-31: tools work at every screen size mostly without scrolling),
 * and whether all of it can be reached (spec 015). Read from the semantics of a screen at rest:
 * - a scroll container whose range is more than zero means the content does not fit;
 * - an element outside the window, or cut by a parent that does not scroll, is clipped;
 * - a text that overflows its box (ellipsis, cut off, taller than its box or broken mid-word) is not readable.
 *   This comes from the unmerged tree, because a merged control keeps only its first text's layout;
 * - a scroller showing less than one touch target is squeezed, and a control with no size has no room;
 * - a control without a touch area of its own loses taps to its neighbour.
 */
object LayoutFit {
    private const val TOLERANCE_PX = 1.5f

    data class Report(
        val scrolls: List<String> = emptyList(),
        val clipped: List<String> = emptyList(),
        val overflowingText: List<String> = emptyList(),
        val squeezed: List<String> = emptyList(),
        val smallTargets: List<String> = emptyList(),
    ) {
        val fits: Boolean get() = lines().isEmpty()

        fun lines(): List<String> =
            scrolls.map { "scrolls: $it" } + clipped.map { "clipped: $it" } +
                overflowingText.map { "text overflows: $it" } + squeezed.map { "squeezed: $it" } +
                smallTargets.map { "target: $it" }

        /** What walking a list adds: its other rows' text. The scrollers and the targets were read at rest. */
        fun merge(other: Report) =
            copy(
                clipped = (clipped + other.clipped).distinct(),
                overflowingText = (overflowingText + other.overflowingText).distinct(),
                squeezed = (squeezed + other.squeezed).distinct(),
            )
    }

    private class Found {
        val scrolls = mutableListOf<String>()
        val clipped = mutableListOf<String>()
        val overflowing = mutableListOf<String>()
        val squeezed = mutableListOf<String>()
    }

    private class Place(
        val underScroller: Boolean = false,
        val underSideways: Boolean = false,
    )

    fun inspect(
        merged: List<SemanticsNode>,
        unmerged: List<SemanticsNode>,
        density: Density,
        touchTarget: Dp,
    ): Report {
        val found = Found()
        val minScrollerPx = touchTarget.value * density.density
        // A bottom sheet starts part of the way up and is dragged the rest, so what's under it can all be reached.
        merged.forEach { root -> visit(root, root, Place(underScroller = root.holdsASheet()), found, minScrollerPx) }
        unmerged.forEach { root -> visitText(root, found) }
        val targets =
            merged.flatMap {
                AccessibilityAudit.violations(it, density, judgeClicks = false, minTargetDp = touchTarget.value) +
                    AccessibilityAudit.touchAreasThatOverlap(it, density, touchTarget)
            }
        return Report(found.scrolls, found.clipped, found.overflowing.distinct(), found.squeezed, targets.distinct())
    }

    private fun visit(
        node: SemanticsNode,
        root: SemanticsNode,
        place: Place,
        found: Found,
        minScrollerPx: Float,
    ) {
        val vertical = node.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
        val horizontal = node.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange)
        readScrollers(node, vertical, horizontal, found, minScrollerPx)
        // A scroller already moved to its end (a chip row showing its last chip) still reaches what lies before.
        val under =
            Place(
                underScroller = place.underScroller || vertical.hasRange() || horizontal.hasRange(),
                underSideways = place.underSideways || horizontal.hasRange(),
            )
        readCuts(node, root, under, found)
        node.children.forEach { visit(it, root, under, found, minScrollerPx) }
    }

    private fun readScrollers(
        node: SemanticsNode,
        vertical: ScrollAxisRange?,
        horizontal: ScrollAxisRange?,
        found: Found,
        minScrollerPx: Float,
    ) {
        if (vertical.hasMoreAhead()) {
            val more = vertical?.maxValue()?.toInt()
            found.scrolls += "vertical, ${node.size.height} px viewport, $more px more, ${describe(node)}"
            if (node.size.height < minScrollerPx) {
                found.squeezed += "a ${node.size.height} px viewport, ${describe(node)}"
            }
        }
        if (horizontal.hasMoreAhead()) {
            val kind = if (node.isSelectorRow()) "sideways" else "sideways, not a selector row"
            val more = horizontal?.maxValue()?.toInt()
            found.scrolls += "$kind, ${node.size.width} px viewport, $more px more, ${describe(node)}"
        }
    }

    private fun readCuts(
        node: SemanticsNode,
        root: SemanticsNode,
        place: Place,
        found: Found,
    ) {
        if (!place.underScroller && isCut(node, root)) {
            found.clipped += "${describe(node)} ${node.size} of ${root.size}"
        }
        if (place.underScroller && !place.underSideways && isCutSideways(node, root)) {
            found.clipped += "${describe(node)} cut sideways, ${node.size} of ${root.size}"
        }
        if (node.isControl() && (node.size.width <= 0 || node.size.height <= 0)) {
            found.squeezed += "no room for ${describe(node)}"
        }
    }

    private fun ScrollAxisRange?.hasMoreAhead(): Boolean = this != null && maxValue() > value() + TOLERANCE_PX

    private fun visitText(
        node: SemanticsNode,
        found: Found,
    ) {
        if (textOverflows(node)) found.overflowing += describe(node)
        node.children.forEach { visitText(it, found) }
    }

    private fun SemanticsNode.isControl(): Boolean =
        config.contains(SemanticsActions.OnClick) || config.contains(SemanticsActions.SetProgress) ||
            config.contains(SemanticsActions.SetText)

    /** The owner's one exception to scrolling sideways: a row of selectors (CLAUDE.md). */
    private fun SemanticsNode.isSelectorRow(): Boolean =
        children.any { child ->
            child.config.contains(SemanticsProperties.Selected) ||
                child.children.any { it.config.contains(SemanticsProperties.Selected) }
        }

    private fun ScrollAxisRange?.hasRange(): Boolean = this != null && maxValue() > TOLERANCE_PX

    /** The element is outside the window or smaller on screen than laid out, and no scroller explains it. */
    private fun isCut(
        node: SemanticsNode,
        root: SemanticsNode,
    ): Boolean {
        if (node.size.width <= 0 || node.size.height <= 0) return false
        val bounds = node.boundsInRoot
        val outside =
            bounds.right > root.size.width + TOLERANCE_PX || bounds.bottom > root.size.height + TOLERANCE_PX ||
                bounds.left < -TOLERANCE_PX || bounds.top < -TOLERANCE_PX
        // A slider's box is wider than the track by the thumb's overhang, which its parent clips on purpose.
        val slider = node.config.contains(SemanticsProperties.ProgressBarRangeInfo)
        val shrunk = bounds.width < node.size.width - TOLERANCE_PX || bounds.height < node.size.height - TOLERANCE_PX
        return outside || (shrunk && !slider)
    }

    private fun SemanticsNode.holdsASheet(): Boolean =
        config.contains(SemanticsActions.Expand) || config.contains(SemanticsActions.Collapse) ||
            children.any { it.holdsASheet() }

    /** Wider than the window under a scroller that only goes up and down: its side can't be reached. */
    private fun isCutSideways(
        node: SemanticsNode,
        root: SemanticsNode,
    ): Boolean {
        if (node.size.width <= 0 || node.size.height <= 0) return false
        if (node.config.contains(SemanticsProperties.ProgressBarRangeInfo)) return false
        // The node's own box as drawn in the root, unclipped: a node turned half a circle (the Conversation's far
        // half, spec 012) has its top-left corner on its right, so its position alone would put it past the edge.
        val box = root.layoutInfo.coordinates.localBoundingBoxOf(node.layoutInfo.coordinates, clipBounds = false)
        return box.left < -TOLERANCE_PX || box.right > root.size.width + TOLERANCE_PX
    }

    private fun textOverflows(node: SemanticsNode): Boolean {
        // A history row's detail is one line cut with "…" on purpose; its menu copies or shares all of it (A6).
        if (node.config.getOrNull(UiRoleKey) == UiRole.Summary) return false
        val action = node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action ?: return false
        val results = mutableListOf<TextLayoutResult>()
        action(results)
        // A text with no room at all is a test double's, such as the fake camera preview's caption.
        val shown = results.map { it to it.layoutInput.text.text }.filterNot { it.second.startsWith(CAMERA_CAPTION) }
        val noRoom = node.size.width <= 0 || node.size.height <= 0
        return shown.any { (layout, text) ->
            if (noRoom) text.isNotBlank() else layout.isCut() || layout.isTallerThan(node)
        }
    }

    /** Lines that draw past the node, as the Calculator's result did when its display was squeezed. */
    private fun TextLayoutResult.isTallerThan(node: SemanticsNode): Boolean =
        multiParagraph.height > node.size.height + TOLERANCE_PX

    /** Ellipsized, cut after its last visible word, or broken in the middle of a word. */
    private fun TextLayoutResult.isCut(): Boolean {
        val last = lineCount - 1
        if (last < 0) return false
        val text = layoutInput.text.text
        val ellipsized = (0..last).any { isLineEllipsized(it) }
        val unseen = getLineEnd(last, visibleEnd = true) < text.trimEnd().length
        val broken =
            (0 until last).any { line ->
                val end = getLineEnd(line)
                end in 1 until text.length && !text[end - 1].isWhitespace() && !text[end].isWhitespace()
            }
        return ellipsized || unseen || broken
    }

    private fun describe(node: SemanticsNode): String {
        val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }
        val description = node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ")
        val label = (text ?: description).orEmpty().replace('\n', ' ').take(MAX_LABEL)
        return "'$label'"
    }

    private const val MAX_LABEL = 40
    private const val CAMERA_CAPTION = "frozen="
}
