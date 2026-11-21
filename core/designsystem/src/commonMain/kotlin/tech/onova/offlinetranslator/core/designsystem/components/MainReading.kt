package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketTypography

/**
 * The one answer a tool gives, with the same look in every tool: [value] large, its [unit] smaller beside it, and
 * [caption], what it is, below; all centered (spec 018). With no reading yet, show [NO_READING] and say why in the
 * caption, so a sentence never takes the reading's place. Give [widestValue] when values differ a lot in length: they
 * then keep one size and one baseline.
 */
@Composable
public fun MainReading(
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    caption: String? = null,
    widestValue: String? = null,
    contentDescription: String? = null,
    announceChanges: Boolean = true,
    alert: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    // A short window gives the dial or the controls what the large size would take.
    val short = LocalWindowInfo.current.containerDpSize.height < PocketSizes.mainReadingRoomyHeight
    val valueStyle = if (short) PocketTypography.readoutMedium else PocketTypography.readout
    val unitStyle =
        SpanStyle(
            fontSize = UNIT_SHARE.em,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
            color = colors.onSurfaceVariant,
        )
    val line: (String) -> AnnotatedString = { number ->
        buildAnnotatedString {
            append(number)
            if (unit != null) {
                append(' ')
                withStyle(unitStyle) { append(unit) }
            }
        }
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PocketSpacing.xxs),
    ) {
        ReadingLine(
            text = line(value),
            modifier = Modifier.fillMaxWidth(),
            style = valueStyle,
            contentDescription = contentDescription,
            announceChanges = announceChanges,
            widestText = widestValue?.let(line),
            // Over a limit (the Speedometer's alert, spec 043): the number in the alert color; a caption says why.
            color = if (alert) colors.error else Color.Unspecified,
        )
        // A reading with no caption has nothing to reserve; an empty one keeps its place for what comes.
        if (caption != null) {
            ReservedLine(
                text = caption,
                style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                color = colors.onSurfaceVariant,
                compact = true,
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** What a reading shows until there is one. */
public const val NO_READING: String = "—"

/** The unit beside the number: small enough that the number is what the eye finds first. */
private const val UNIT_SHARE = 0.4f
