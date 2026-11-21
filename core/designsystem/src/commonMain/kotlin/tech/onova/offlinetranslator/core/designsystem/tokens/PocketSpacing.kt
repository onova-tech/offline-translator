package tech.onova.offlinetranslator.core.designsystem.tokens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing scale from the mockups. Exactly these values from phone size up; in a small window each step goes down
 * towards half of it (spec 015). So it can be read only in composition: in a draw lambda, read it just before the
 * lambda.
 */
public object PocketSpacing {
    internal val XXS = FluidDp(1.dp, 4.dp)
    internal val XS = FluidDp(4.dp, 8.dp)
    internal val SM = FluidDp(6.dp, 12.dp)
    internal val MD = FluidDp(8.dp, 16.dp)
    internal val LG = FluidDp(12.dp, 24.dp)
    internal val XL = FluidDp(16.dp, 32.dp)
    internal val XXL = FluidDp(24.dp, 48.dp)

    public val xxs: Dp
        @Composable @ReadOnlyComposable
        get() = XXS.at(LocalWindowFraction.current)
    public val xs: Dp
        @Composable @ReadOnlyComposable
        get() = XS.at(LocalWindowFraction.current)
    public val sm: Dp
        @Composable @ReadOnlyComposable
        get() = SM.at(LocalWindowFraction.current)
    public val md: Dp
        @Composable @ReadOnlyComposable
        get() = MD.at(LocalWindowFraction.current)
    public val lg: Dp
        @Composable @ReadOnlyComposable
        get() = LG.at(LocalWindowFraction.current)
    public val xl: Dp
        @Composable @ReadOnlyComposable
        get() = XL.at(LocalWindowFraction.current)
    public val xxl: Dp
        @Composable @ReadOnlyComposable
        get() = XXL.at(LocalWindowFraction.current)
}
