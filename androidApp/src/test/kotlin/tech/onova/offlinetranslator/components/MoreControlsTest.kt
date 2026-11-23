package tech.onova.offlinetranslator.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Test
import tech.onova.offlinetranslator.core.designsystem.components.BottomAction
import tech.onova.offlinetranslator.core.designsystem.components.BottomActionRow
import tech.onova.offlinetranslator.core.toolapi.MoreControls
import tech.onova.offlinetranslator.core.toolapi.MoreControlsPanel
import tech.onova.offlinetranslator.core.toolapi.MoreOptionsAction
import tech.onova.offlinetranslator.support.AppUiTest
import kotlin.test.assertTrue

/** `docs/ui-rules.md`, A4: "More options" holds two or more controls; a single one shows from the start. */
class MoreControlsTest : AppUiTest() {
    @Test
    fun aSingleOptionShowsWithoutTheButton() {
        show { MoreControls(listOf({ Text("Only option") })) }
        compose.onNodeWithText("Only option").assertExists()
        compose.onNodeWithText("More options").assertDoesNotExist()
    }

    @Test
    fun twoOptionsWaitBehindTheButton() {
        show { MoreControls(listOf({ Text("First") }, { Text("Second") })) }
        compose.onNodeWithText("First").assertDoesNotExist()
        compose.onNodeWithText("More options").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("First").assertExists()
        compose.onNodeWithText("Second").assertExists()
        compose.onNodeWithText("Fewer options").assertExists()
    }

    /**
     * A3 (owner, 2026-11-22): "More options" may be a button in the actions' row; its options open above the row, so
     * the actions stay at the bottom edge.
     */
    @Test
    fun theButtonInTheActionsRowOpensItsOptionsAboveTheRow() {
        show {
            var shown by remember { mutableStateOf(false) }
            Column {
                MoreControlsPanel(shown = shown, options = listOf({ Text("First") }, { Text("Second") }))
                BottomActionRow {
                    BottomAction(label = "Copy", onClick = {}, compact = true, modifier = Modifier.weight(1f))
                    MoreOptionsAction(shown = shown, onToggle = { shown = !shown })
                }
            }
        }
        compose.onNodeWithText("First").assertDoesNotExist()
        compose.onNodeWithText("More options").performClick()
        compose.waitForIdle()
        val second = compose.onNodeWithText("Second").getUnclippedBoundsInRoot()
        val button = compose.onNodeWithText("Fewer options").getUnclippedBoundsInRoot()
        assertTrue(second.bottom <= button.top, "the options open above the actions' row")
    }
}
