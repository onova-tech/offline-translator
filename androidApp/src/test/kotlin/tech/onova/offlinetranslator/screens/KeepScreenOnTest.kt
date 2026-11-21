package tech.onova.offlinetranslator.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.junit.Test
import tech.onova.offlinetranslator.core.designsystem.theme.PocketTheme
import tech.onova.offlinetranslator.core.toolapi.KeepScreenOn
import tech.onova.offlinetranslator.support.AppUiTest
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Spec 005 research.md R9: the screen stays on while a tool shows, and leaving clears it. */
class KeepScreenOnTest : AppUiTest() {
    @Test
    fun theScreenStaysOnWhileShownAndIsReleasedAfterwards() {
        var shown by mutableStateOf(true)
        compose.setContent { PocketTheme(darkTheme = false, dynamicColor = false) { if (shown) KeepScreenOn() } }
        compose.waitForIdle()
        assertTrue(fakes.screenAwake.keptOn)
        shown = false
        compose.waitForIdle()
        assertFalse(fakes.screenAwake.keptOn)
    }
}
