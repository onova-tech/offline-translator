package tech.onova.offlinetranslator

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.test.getAlignmentLinePosition
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tech.onova.offlinetranslator.core.designsystem.components.LiveReadout
import tech.onova.offlinetranslator.core.designsystem.theme.PocketTheme
import kotlin.math.abs

/**
 * What only a real Android's fonts show. The app's font has no ♯, so Android draws it from a fallback font with taller
 * metrics, and Robolectric has no such font. On the owner's phone that moved the tuner's whole text down on the sharps
 * and cut the bottom of its glyphs (2026-11-05).
 */
@RunWith(AndroidJUnit4::class)
class RealFontsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun everyNoteOfTheTunerSitsOnOneBaseline() {
        var text by mutableStateOf(NOTES.first())
        compose.setContent {
            PocketTheme {
                LiveReadout(text = text, widestText = "G♯4 (Sol♯)", modifier = Modifier.fillMaxWidth())
            }
        }
        val baselines =
            NOTES.map { note ->
                text = note
                compose.waitForIdle()
                val node = compose.onNodeWithText(note)
                note to (node.getBoundsInRoot().top + node.getAlignmentLinePosition(FirstBaseline)).value
            }
        val first = baselines.first().second
        assertTrue("The notes' baselines differ: $baselines", baselines.all { abs(it.second - first) < 0.5f })
    }

    private companion object {
        val NOTES = listOf("E4 (Mi)", "A♯2 (Lá♯)", "G♯4 (Sol♯)", "C3 (Dó)", "—")
    }
}
