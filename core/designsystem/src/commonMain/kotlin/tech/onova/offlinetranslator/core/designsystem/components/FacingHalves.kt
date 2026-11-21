package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing

/**
 * Two people facing each other across the phone (spec 012, `docs/ui-rules.md` C1): the screen in two equal halves,
 * [far] on top, turned half a circle so it faces the person across, unless [farTurned] is off for someone beside you;
 * [near] below, the phone owner's. The turn is only drawn: touches follow it, and screen readers read [far] then [near]
 * in the normal order, never upside down. Each half gets at least [PocketSizes.facingHalfLeastHeight]: in a window too
 * short for both, they scroll. Inside the turned half [LocalHalfTurned] is true, for what opens in a window of its own.
 */
@Composable
public fun FacingHalves(
    far: @Composable ColumnScope.() -> Unit,
    near: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    farTurned: Boolean = true,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val least = PocketSizes.facingHalfLeastHeight * 2
        // A window too short for two halves scrolls them, rather than squeeze a microphone (L8).
        val roomy = maxHeight >= least
        val column =
            if (roomy) Modifier.fillMaxSize() else Modifier.verticalScroll(rememberScrollState()).height(least)
        Column(modifier = column) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer { rotationZ = if (farTurned) HALF_TURN else 0f }
                        .padding(vertical = PocketSpacing.xs),
            ) {
                val half = this
                CompositionLocalProvider(LocalHalfTurned provides farTurned) { half.far() }
            }
            HorizontalDivider()
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = PocketSpacing.xs),
                content = near,
            )
        }
    }
}

/**
 * Whether this content is in a half turned to face the person across ([FacingHalves]). A turn that's only drawn doesn't
 * reach what opens in a window of its own, such as a [MenuChoice]'s list, so that turns itself to match (owner,
 * 2026-11-19: the far half's language list was the wrong way up).
 */
@Suppress("CompositionLocalAllowlist") // A drawn turn doesn't reach a popup's window: only the half knows it's turned.
internal val LocalHalfTurned: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { false }

internal const val HALF_TURN = 180f
