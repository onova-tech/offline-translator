package tech.onova.offlinetranslator.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import org.junit.Test
import org.robolectric.annotation.Config
import tech.onova.offlinetranslator.core.designsystem.components.WholeWordText
import tech.onova.offlinetranslator.support.AppUiTest
import kotlin.test.assertTrue

/** Spec 015 FR-005, SC-006: text that shrinks itself never goes under 9 sp at the default text size. */
@Config(qualifiers = "w240dp-h360dp-port-xhdpi")
class TextFloorsTest : AppUiTest() {
    @Test
    fun aLongWordInANarrowBoxStopsAtTheFloor() {
        show {
            Box(Modifier.width(20.dp)) {
                WholeWordText(
                    text = "Incomprehensibilities",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.testTag("word"),
                )
            }
        }
        val results = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithTag("word")
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(results)
        val size =
            results
                .single()
                .layoutInput.style.fontSize.value
        assertTrue(size >= FLOOR_SP - ROUNDING, "shrank to $size sp")
    }

    private companion object {
        const val FLOOR_SP = 9f
        const val ROUNDING = 0.01f
    }
}
