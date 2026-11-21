package tech.onova.offlinetranslator.screens

import android.os.Looper
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.ComposeTestRule
import org.robolectric.Shadows.shadowOf
import tech.onova.offlinetranslator.support.KoinFakesRule

/**
 * A tool's mode with the most on its screen, which `LayoutFitTest`'s idle cases don't open (specs 057 to 061), apart so
 * that test stays a readable size. [prepare] sets the fakes, [after] opens the mode once the tool shows, given the
 * test's way to open a surface by its label, and [cleanup] puts the fakes back.
 */
internal class ModeSurface(
    val name: String,
    val toolId: String,
    val prepare: (KoinFakesRule) -> Unit,
    val cleanup: (KoinFakesRule) -> Unit,
    val after: (open: (String) -> Unit, compose: ComposeTestRule) -> Unit = { _, _ -> },
)

internal val modeSurfaces: List<ModeSurface> =
    listOf(
        // Spec 080: the language page, from the end of Type or speak's From list. Before the Conversation, since the
        // mode a case leaves is kept for the next one.
        ModeSurface(
            name = "translator-languages",
            toolId = "translator",
            prepare = {},
            cleanup = {},
            after = { open, compose ->
                // The language chips show once the settings are read, on another thread.
                compose.waitUntil(SETTINGS_MILLIS) {
                    shadowOf(Looper.getMainLooper()).idle()
                    compose.onAllNodes(hasClickAction() and fromChip).fetchSemanticsNodes().isNotEmpty()
                }
                open("From")
                open("Manage languages…")
            },
        ),
        // Spec 012: two facing halves, each with its language, its turns and its microphone.
        ModeSurface(
            name = "translator-conversation",
            toolId = "translator",
            prepare = {},
            cleanup = {},
            after = { open, _ -> open("Conversation") },
        ),
    )

private const val SETTINGS_MILLIS = 5_000L

/** Type or speak's From chip, in English, Portuguese and Spanish. */
private val fromChip: SemanticsMatcher = hasContentDescription("From") or hasContentDescription("De")
