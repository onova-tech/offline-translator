package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import tech.onova.offlinetranslator.core.designsystem.tokens.isSmallWindow

/**
 * Holds the room of [content] whether it shows or not, so what's around it keeps its place when a tool changes mode
 * (owner, 2026-11-05). When it doesn't show, [content] is measured but never placed: nothing is drawn, touched or read
 * by a screen reader. A small window gives the room up (spec 015), unless [keepInSmallWindow]: then what's around it
 * keeps its place there too, and the screen scrolls instead (owner, 2026-11-08: the QR creator's kinds).
 */
@Composable
public fun ReservedSlot(
    shown: Boolean,
    modifier: Modifier = Modifier,
    keepInSmallWindow: Boolean = false,
    content: @Composable () -> Unit,
) {
    // A small window can't afford room for what isn't there (spec 015).
    if (!shown && isSmallWindow && !keepInSmallWindow) return
    Layout(
        content = content,
        modifier =
            if (shown) {
                modifier
            } else {
                modifier.clearAndSetSemantics {
                }
            },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val width = placeables.maxOfOrNull { it.width } ?: constraints.minWidth
        val height = placeables.maxOfOrNull { it.height } ?: constraints.minHeight
        layout(width, height) {
            if (shown) placeables.forEach { it.place(0, 0) }
        }
    }
}
