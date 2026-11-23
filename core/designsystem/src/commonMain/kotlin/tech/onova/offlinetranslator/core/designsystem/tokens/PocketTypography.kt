package tech.onova.offlinetranslator.core.designsystem.tokens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Type scale from the approved mockups on the platform sans family (research.md R7). Sizes are in sp, so the
 * phone's text setting always multiplies them (Principle X). Each role has a maximum, the size declared here, kept
 * exactly from phone size up, and a minimum for the smallest windows; in between it follows the window, in whole sp
 * (spec 015).
 */
public object PocketTypography {
    private val family = FontFamily.Default
    private const val TABULAR_FIGURES = "tnum"

    private val readoutAtMax: TextStyle =
        TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = 76.sp,
            lineHeight = 84.sp,
            letterSpacing = (-1.5).sp,
            fontFeatureSettings = TABULAR_FIGURES,
        )

    private val readoutMediumAtMax: TextStyle =
        readoutAtMax.copy(fontSize = 44.sp, lineHeight = 52.sp, letterSpacing = (-0.5).sp)

    /** Large numeric readings (Counter value, Ruler length). */
    public val readout: TextStyle
        @Composable @ReadOnlyComposable
        get() = readoutAt(LocalWindowFraction.current)

    /** Medium numeric readings that must fit beside other content. */
    public val readoutMedium: TextStyle
        @Composable @ReadOnlyComposable
        get() = readoutMediumAt(LocalWindowFraction.current)

    /**
     * A password or a code (spec 034): a headline's size, one width per character, so each one can be read and
     * counted; 16 characters fit a phone's width. It follows the window through the headline role.
     */
    public val code: TextStyle
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace)

    /**
     * Who said a Conversation turn, before its words ("Me:", "You:"): a label's size, bold (spec 082, owner,
     * 2026-11-22: "add just a small bold You:/Me:"). It follows the window through the label role.
     */
    public val speakerLabel: TextStyle
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)

    internal fun readoutAt(fraction: Float): TextStyle = readoutAtMax.at(READOUT_MIN, fraction)

    internal fun readoutMediumAt(fraction: Float): TextStyle = readoutMediumAtMax.at(READOUT_MEDIUM_MIN, fraction)

    /** Every role at [fraction]. */
    internal fun material(fraction: Float): Typography {
        if (fraction >= 1f) return materialAtMax
        return with(materialAtMax) {
            Typography(
                displayLarge = readoutAt(fraction),
                displaySmall = displaySmall.at(DISPLAY_SMALL_MIN, fraction),
                headlineMedium = headlineMedium.at(HEADLINE_MEDIUM_MIN, fraction),
                headlineSmall = headlineSmall.at(HEADLINE_SMALL_MIN, fraction),
                titleLarge = titleLarge.at(TITLE_LARGE_MIN, fraction),
                titleMedium = titleMedium.at(TITLE_MEDIUM_MIN, fraction),
                titleSmall = titleSmall.at(TITLE_SMALL_MIN, fraction),
                bodyLarge = bodyLarge.at(BODY_LARGE_MIN, fraction),
                bodyMedium = bodyMedium.at(BODY_MEDIUM_MIN, fraction),
                bodySmall = bodySmall.at(BODY_SMALL_MIN, fraction),
                labelLarge = labelLarge.at(LABEL_LARGE_MIN, fraction),
                labelMedium = labelMedium.at(LABEL_MEDIUM_MIN, fraction),
                labelSmall = labelSmall.at(LABEL_SMALL_MIN, fraction),
            )
        }
    }

    /**
     * The style at [fraction]: exactly itself at 1, and otherwise a whole sp, with line height and spacing in ratio.
     */
    private fun TextStyle.at(
        min: Float,
        fraction: Float,
    ): TextStyle {
        if (fraction >= 1f) return this
        val size = fluidSp(min, fontSize.value, fraction)
        val ratio = size.value / fontSize.value
        return copy(
            fontSize = size,
            lineHeight = (lineHeight.value * ratio).roundToInt().sp,
            letterSpacing = if (letterSpacing.isSpecified) letterSpacing * ratio else letterSpacing,
        )
    }

    private val materialAtMax: Typography =
        Typography(
            displayLarge = readoutAtMax,
            headlineMedium =
                TextStyle(
                    fontFamily = family,
                    fontWeight = FontWeight(750),
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    letterSpacing = (-0.5).sp,
                ),
            headlineSmall =
                TextStyle(
                    fontFamily = family,
                    fontWeight = FontWeight(700),
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                ),
            titleLarge =
                TextStyle(
                    fontFamily = family,
                    fontWeight = FontWeight(650),
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                ),
            titleMedium =
                TextStyle(
                    fontFamily = family,
                    fontWeight = FontWeight(650),
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                ),
            titleSmall =
                TextStyle(
                    fontFamily = family,
                    fontWeight = FontWeight(650),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
            bodyLarge =
                TextStyle(
                    fontFamily = family,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                ),
            bodyMedium =
                TextStyle(
                    fontFamily = family,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
            bodySmall =
                TextStyle(
                    fontFamily = family,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                ),
            labelLarge =
                TextStyle(
                    fontFamily = family,
                    fontWeight = FontWeight(600),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
            labelMedium =
                TextStyle(
                    fontFamily = family,
                    fontWeight = FontWeight(550),
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                ),
            labelSmall =
                TextStyle(
                    fontFamily = family,
                    fontWeight = FontWeight(500),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                ),
        )

    private const val READOUT_MIN = 32f
    private const val READOUT_MEDIUM_MIN = 24f
    private const val DISPLAY_SMALL_MIN = 20f
    private const val HEADLINE_MEDIUM_MIN = 18f
    private const val HEADLINE_SMALL_MIN = 16f
    private const val TITLE_LARGE_MIN = 14f
    private const val TITLE_MEDIUM_MIN = 13f
    private const val TITLE_SMALL_MIN = 12f
    private const val BODY_LARGE_MIN = 12f
    private const val BODY_MEDIUM_MIN = 11f
    private const val BODY_SMALL_MIN = 11f
    private const val LABEL_LARGE_MIN = 11f
    private const val LABEL_MEDIUM_MIN = 11f
    private const val LABEL_SMALL_MIN = 11f
}
