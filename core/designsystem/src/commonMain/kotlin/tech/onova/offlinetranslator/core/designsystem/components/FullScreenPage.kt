package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * A page over the whole screen, Back closing it: Manage languages, a confirmation, a correction (`docs/ui-rules.md` L5,
 * F3). It is a window of its own so it covers the tool's frame, but it opens like a page: a plain dialog window dimmed
 * the screen behind it, stayed transparent until its first frame and grew in from the middle, which read as a flash
 * (the owner's phone, 2026-11-22, spec 082 FR-013). Here nothing is dimmed, the window is opaque from the start in the
 * surface's colour, and it has no window animation.
 */
@Composable
public fun FullScreenPage(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val surface = MaterialTheme.colorScheme.surface
        PlainPageWindow(background = surface)
        Surface(modifier = Modifier.fillMaxSize(), color = surface) { content() }
    }
}

/** The page's own window: no dim behind it, no window animation, [background] from its first frame. */
@Composable
internal expect fun PlainPageWindow(background: Color)
