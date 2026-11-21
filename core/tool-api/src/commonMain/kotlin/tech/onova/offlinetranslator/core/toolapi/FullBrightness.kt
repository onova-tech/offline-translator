package tech.onova.offlinetranslator.core.toolapi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import org.koin.compose.koinInject
import tech.onova.offlinetranslator.core.domain.ports.ScreenBrightness

/**
 * The screen at full brightness while the calling composable is shown, for a view read in a reflection or in
 * daylight (the Speedometer's windshield view, spec 043). Leaving it gives the brightness back to the system; a
 * rotation sets it again on the new window.
 */
@Composable
public fun FullBrightness() {
    val brightness: ScreenBrightness = koinInject()
    DisposableEffect(brightness) {
        brightness.set(FULL)
        onDispose { brightness.set(null) }
    }
}

private const val FULL = 1f
