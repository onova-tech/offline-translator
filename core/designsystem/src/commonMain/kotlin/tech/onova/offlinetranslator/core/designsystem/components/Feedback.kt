package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import tech.onova.offlinetranslator.core.designsystem.tokens.LocalWindowFraction
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketTypography
import tech.onova.offlinetranslator.core.designsystem.tokens.fluidSp
import kotlin.math.roundToInt

/** Centered message with an optional action, for empty results and unavailable tools. */
@Composable
public fun EmptyState(
    icon: ImageVector?,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(PocketSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PocketSpacing.sm),
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(PocketSizes.emptyStateIcon),
            )
        }
        Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (message.isNotEmpty()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/**
 * A large live reading, used for values read at a glance: the level's angle, the compass bearing, a camera
 * preview. It always takes one line of [style], so a reading that changes moves nothing below it. The text shrinks to
 * stay on one line at large text sizes instead of being cut off. Give [widestText] when readings differ a lot in
 * length, such as a tuner's notes: sized for it, the text keeps one size and one baseline as the reading changes
 * (owner, 2026-11-05).
 */
@Composable
public fun LiveReadout(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = PocketTypography.readout,
    contentDescription: String? = null,
    announceChanges: Boolean = true,
    textAlign: TextAlign = TextAlign.Center,
    widestText: String? = null,
) {
    ReadingLine(
        text = AnnotatedString(text),
        modifier = modifier,
        style = style,
        contentDescription = contentDescription,
        announceChanges = announceChanges,
        textAlign = textAlign,
        widestText = widestText?.let { AnnotatedString(it) },
    )
}

/** [LiveReadout] for a text with parts in another style, such as [MainReading]'s unit. */
@Composable
internal fun ReadingLine(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = PocketTypography.readout,
    contentDescription: String? = null,
    announceChanges: Boolean = true,
    textAlign: TextAlign = TextAlign.Center,
    widestText: AnnotatedString? = null,
    color: Color = Color.Unspecified,
) {
    val density = LocalDensity.current
    val line = with(density) { (if (style.lineHeight.isSpecified) style.lineHeight else style.fontSize).toDp() }
    val shown = style.copy(color = color.takeOrElse { MaterialTheme.colorScheme.onSurface }, textAlign = textAlign)
    val alignment =
        when (textAlign) {
            TextAlign.Center -> Alignment.Center
            TextAlign.End -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    val spoken =
        Modifier.semantics {
            if (announceChanges) liveRegion = LiveRegionMode.Polite
            if (contentDescription != null) this.contentDescription = contentDescription
        }
    if (widestText == null) {
        Box(modifier = modifier.height(line), contentAlignment = alignment) {
            BasicText(
                text = text,
                style = shown,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = minReadoutSize(style), maxFontSize = style.fontSize),
                modifier = spoken,
            )
        }
    } else {
        SteadyReadout(widestText, shown, alignment, modifier.height(line)) { sized ->
            BasicText(text = text, style = sized, maxLines = 1, modifier = spoken)
        }
    }
}

/**
 * One text size and one baseline for every reading. A character the app's font doesn't have, such as the tuner's ♯,
 * comes from a fallback font whose taller metrics push the line's baseline down, which moved the whole text and cut the
 * bottom of its glyphs (owner's phone pass, 2026-11-05). So the text may be as tall as it needs, and is placed by its
 * baseline: where a line of plain text in this style, centered in the space, has its own.
 */
@Composable
private fun SteadyReadout(
    widestText: AnnotatedString,
    style: TextStyle,
    alignment: Alignment,
    modifier: Modifier = Modifier,
    content: @Composable (TextStyle) -> Unit,
) {
    val measurer = rememberTextMeasurer()
    val minSize = minReadoutSize(style)
    BoxWithConstraints(modifier = modifier) {
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val sized =
            remember(widestText, style, width) {
                style.copy(fontSize = sizeThatFits(measurer, widestText, style, width, minSize))
            }
        val baseline =
            remember(sized, height) {
                val plain = measurer.measure(PLAIN_TEXT, sized, maxLines = 1)
                (height - plain.size.height) / 2 + plain.firstBaseline.roundToInt()
            }
        Layout(
            content = { content(sized) },
            modifier = Modifier.fillMaxSize(),
        ) { measurables, space ->
            val reading = measurables.single().measure(Constraints(maxWidth = space.maxWidth))
            layout(space.maxWidth, space.maxHeight) {
                val x = alignment.align(IntSize(reading.width, 0), IntSize(space.maxWidth, 0), layoutDirection).x
                reading.place(x, baseline - reading[FirstBaseline])
            }
        }
    }
}

private fun sizeThatFits(
    measurer: TextMeasurer,
    text: AnnotatedString,
    style: TextStyle,
    maxWidth: Int,
    minSize: TextUnit,
): TextUnit {
    var size = style.fontSize.value
    while (size > minSize.value &&
        measurer.measure(text, style.copy(fontSize = size.sp), maxLines = 1).size.width > maxWidth
    ) {
        size -= READOUT_SIZE_STEP
    }
    return size.sp
}

/** Any character of the app's own font: it is measured, never shown. */
private const val PLAIN_TEXT = "0"
private const val READOUT_SIZE_STEP = 2f
private val MIN_READOUT_SIZE = 20.sp
private val MIN_READOUT_SIZE_SMALL_WINDOW = 12.sp

/** A reading shrinks down to 20 sp from phone size up, to 12 sp in the smallest window, and never past its own size. */
@Composable
@ReadOnlyComposable
private fun minReadoutSize(style: TextStyle): TextUnit {
    val floor = fluidSp(MIN_READOUT_SIZE_SMALL_WINDOW.value, MIN_READOUT_SIZE.value, LocalWindowFraction.current)
    return if (style.fontSize < floor) style.fontSize else floor
}
