package tech.onova.offlinetranslator.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import tech.onova.offlinetranslator.core.designsystem.tokens.LocalWindowFraction
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketColorSchemes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketShapes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketTypography
import tech.onova.offlinetranslator.core.designsystem.tokens.windowFraction

/** The app theme: wallpaper colors when requested and supported, otherwise the Teal palette. */
@Composable
public fun PocketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dynamicScheme = if (dynamicColor) platformDynamicColorScheme(darkTheme) else null
    val colorScheme = dynamicScheme ?: if (darkTheme) PocketColorSchemes.dark else PocketColorSchemes.light
    // The window, never the space a screen gets, so a keyboard can't change the type (spec 015). Sizes stay in sp, so
    // the phone's text setting keeps multiplying them: never override LocalDensity here (Principle X).
    val window = LocalWindowInfo.current
    val fraction by remember(window) {
        derivedStateOf { window.containerDpSize.let { windowFraction(it.width, it.height) } }
    }
    val typography = remember(fraction) { PocketTypography.material(fraction) }
    // Material's controls stop at 48 dp by themselves. Compose's touch areas may stay larger: they only take a tap that
    // lands on no control, so there's room to spare around a small one, in a dialog too.
    val touchTarget = PocketSizes.touchTargetAt(fraction, LocalDensity.current.fontScale)
    CompositionLocalProvider(
        LocalWindowFraction provides fraction,
        LocalMinimumInteractiveComponentSize provides touchTarget,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = PocketShapes.material,
            content = content,
        )
    }
}

/** Wallpaper-based colors, or null when the platform has none (research.md R6). */
@Composable
internal expect fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme?
