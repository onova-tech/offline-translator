package tech.onova.offlinetranslator.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Test
import tech.onova.offlinetranslator.core.toolapi.MoreControls
import tech.onova.offlinetranslator.support.AppUiTest

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
}
