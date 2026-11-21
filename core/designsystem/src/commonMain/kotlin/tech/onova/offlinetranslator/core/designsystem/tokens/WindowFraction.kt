package tech.onova.offlinetranslator.core.designsystem.tokens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.onova.offlinetranslator.core.designsystem.components.isTwoPane
import kotlin.math.roundToInt

/**
 * Where a window stands between the smallest one and phone size (spec 015): 0 at the smallest, 1 from phone size up.
 */
@Suppress("CompositionLocalAllowlist") // What the whole theme derives from, like the theme itself.
internal val LocalWindowFraction = compositionLocalOf { 1f }

/** Whether the window is under phone size. For a layout that must change there; sizes already follow the window. */
public val isSmallWindow: Boolean
    @Composable @ReadOnlyComposable
    get() = LocalWindowFraction.current < 1f

/**
 * Single pane, the tighter of width and height. Side by side only the height counts, so a landscape phone stays as it
 * is. Rounded to steps of 1/40, so a drag changes the sizes a limited number of times.
 */
public fun windowFraction(
    width: Dp,
    height: Dp,
): Float {
    val fraction =
        if (isTwoPane(width, height)) {
            between(height.value, SIDE_BY_SIDE_SMALLEST_HEIGHT, SIDE_BY_SIDE_PHONE_HEIGHT)
        } else {
            minOf(
                between(width.value, SMALLEST_WIDTH, PHONE_WIDTH),
                between(height.value, SMALLEST_HEIGHT, PHONE_HEIGHT),
            )
        }
    return (fraction * FRACTION_STEPS).roundToInt() / FRACTION_STEPS
}

private fun between(
    value: Float,
    smallest: Float,
    phone: Float,
): Float = ((value - smallest) / (phone - smallest)).coerceIn(0f, 1f)

/** A size in dp that follows the window: [max] from phone size up, exactly as declared, and [min] at the smallest. */
internal class FluidDp(
    val min: Dp,
    val max: Dp,
) {
    fun at(fraction: Float): Dp =
        if (fraction >= 1f) max else (min.value + (max.value - min.value) * fraction).roundToInt().dp
}

/** Same for text, in whole sp: the phone's text setting still multiplies whatever this gives. */
internal fun fluidSp(
    min: Float,
    max: Float,
    fraction: Float,
): TextUnit = if (fraction >= 1f) max.sp else (min + (max - min) * fraction).roundToInt().sp

private const val SMALLEST_WIDTH = 240f
private const val PHONE_WIDTH = 360f
private const val SMALLEST_HEIGHT = 320f
private const val PHONE_HEIGHT = 600f
private const val SIDE_BY_SIDE_SMALLEST_HEIGHT = 240f
private const val SIDE_BY_SIDE_PHONE_HEIGHT = 360f
private const val FRACTION_STEPS = 40f
