package tech.onova.offlinetranslator.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertNotEquals
import org.junit.Test
import tech.onova.offlinetranslator.core.designsystem.components.WholeWordText
import tech.onova.offlinetranslator.support.AppUiTest

/**
 * Spec 074 (owner's phone pass, 2026-11-13): the World clock's and the Tip's steppers kept showing their old words. A
 * text whose room doesn't change, as in a stepper's middle, must still be drawn again when its words do.
 */
class WholeWordTextTest : AppUiTest() {
    @Test
    fun newWordsInTheSameRoomAreDrawn() {
        var text by mutableStateOf("People: 1")
        show {
            Box(modifier = Modifier.width(SLOT).testTag(TAG)) {
                WholeWordText(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(SLOT),
                )
            }
        }
        val before = compose.onNodeWithTag(TAG).captureToImage().toPixelMap()
        text = "People: 10"
        compose.waitForIdle()
        val after = compose.onNodeWithTag(TAG).captureToImage().toPixelMap()
        val changed =
            (0 until before.width).any { x ->
                (0 until before.height).any { y -> before[x, y] != after[x, y] }
            }
        assertNotEquals("the old words are still drawn", false, changed)
    }

    private companion object {
        const val TAG = "slot"
        val SLOT = 200.dp
    }
}
