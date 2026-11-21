package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize

/**
 * A touch and hold on something that already has its own tap, such as a chip or a result row: [action] runs when the
 * finger stays down for the long-press time without moving, and the release that follows is swallowed, so the tap
 * doesn't run too. It watches the press before the element's own tap, which Material's chips need since they take
 * every press for themselves. A screen reader offers it as the long-press action named [label] (spec 039).
 *
 * The hold survives what [action] changes: a favourite that moves its row or chip recomposes the element in the middle
 * of the press, and the node keeps tracking that press with the newest [action] until the finger lifts.
 */
public fun Modifier.onLongPress(
    label: String,
    action: () -> Unit,
): Modifier =
    semantics {
        onLongClick(label) {
            action()
            true
        }
    }.then(LongPressElement(action))

private data class LongPressElement(
    val action: () -> Unit,
) : ModifierNodeElement<LongPressNode>() {
    override fun create(): LongPressNode = LongPressNode(action)

    override fun update(node: LongPressNode) {
        node.action = action
    }
}

private class LongPressNode(
    var action: () -> Unit,
) : DelegatingNode(),
    PointerInputModifierNode {
    private val input = delegate(SuspendingPointerInputModifierNode { detectHold { action() } })

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) = input.onPointerEvent(pointerEvent, pass, bounds)

    override fun onCancelPointerInput() = input.onCancelPointerInput()
}

@Suppress("SwallowedException") // The timeout is the long press itself.
private suspend fun PointerInputScope.detectHold(onHold: () -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val held =
            try {
                withTimeout(viewConfiguration.longPressTimeoutMillis) {
                    var lifted = false
                    while (!lifted) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id }
                        val distance = change?.let { (it.position - down.position).getDistance() } ?: 0f
                        lifted = change == null || !change.pressed || distance > viewConfiguration.touchSlop
                    }
                }
                false
            } catch (timeout: PointerEventTimeoutCancellationException) {
                true
            }
        if (held) {
            onHold()
            // The rest of this press is ours: the element's own tap must not run on the release.
            var pressed = true
            while (pressed) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.forEach { it.consume() }
                pressed = event.changes.any { it.pressed }
            }
        }
    }
}
