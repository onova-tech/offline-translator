package tech.onova.offlinetranslator.core.toolapi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import org.koin.compose.koinInject
import tech.onova.offlinetranslator.core.domain.ports.ScreenAwake

/**
 * Keeps the screen on while the calling composable is shown, for a tool that is watched
 * continuously (spec 005 research.md R9). Leaving the tool always clears it.
 */
@Composable
public fun KeepScreenOn() {
    val screen: ScreenAwake = koinInject()
    DisposableEffect(screen) {
        screen.keepOn(true)
        onDispose { screen.keepOn(false) }
    }
}
