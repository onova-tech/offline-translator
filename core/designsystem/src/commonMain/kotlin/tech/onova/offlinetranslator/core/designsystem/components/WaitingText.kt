package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketMotion

/**
 * A [word] whose dots count up every second and start again, "Listening." "Listening.." "Listening...", while
 * something is awaited: the Translator's turn before its first word is heard (owner, 2026-11-19). It keeps the room of
 * its longest form, so nothing beside it moves, and screen readers hear the word once, not every step. An infinite
 * animation: tests hold it still, and with animations off it doesn't step.
 */
@Composable
public fun WaitingText(
    word: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
) {
    val steps = rememberInfiniteTransition(label = "waiting")
    val step =
        steps.animateFloat(
            initialValue = 0f,
            targetValue = MAX_DOTS.toFloat(),
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = CYCLE_MILLIS, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "dots",
        )
    // One more dot each step; recomposed only when the count changes, not on every frame.
    val dots by remember { derivedStateOf { (step.value.toInt() + 1).coerceAtMost(MAX_DOTS) } }
    Box(modifier = modifier.clearAndSetSemantics { contentDescription = word }) {
        // The longest form, never seen, holds the room.
        Text(text = word + DOT.toString().repeat(MAX_DOTS), style = style, modifier = Modifier.alpha(0f))
        Text(text = word + DOT.toString().repeat(dots), style = style)
    }
}

private const val DOT = '.'
private const val MAX_DOTS = 3
private const val CYCLE_MILLIS = PocketMotion.WAITING_STEP_MILLIS * MAX_DOTS
