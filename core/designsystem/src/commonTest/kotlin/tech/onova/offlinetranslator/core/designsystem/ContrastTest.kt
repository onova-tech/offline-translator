package tech.onova.offlinetranslator.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketColorSchemes
import kotlin.test.Test
import kotlin.test.assertTrue

/** Principle X: WCAG 2.2 AA contrast for the color pairs the screens actually use. */
class ContrastTest {
    private val schemes = mapOf("light" to PocketColorSchemes.light, "dark" to PocketColorSchemes.dark)

    private fun contrast(
        a: Color,
        b: Color,
    ): Double {
        val (lighter, darker) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun assertPairs(
        minimum: Double,
        pairs: (ColorScheme) -> Map<String, Pair<Color, Color>>,
    ) {
        val failures =
            schemes.flatMap { (name, scheme) ->
                pairs(scheme).mapNotNull { (label, colors) ->
                    val ratio = contrast(colors.first, colors.second)
                    "$name $label: %.2f < $minimum".format(ratio).takeIf { ratio < minimum }
                }
            }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun textMeetsFourPointFiveToOne() =
        assertPairs(4.5) { s ->
            mapOf(
                "onSurface/surface" to (s.onSurface to s.surface),
                "onSurfaceVariant/surface" to (s.onSurfaceVariant to s.surface),
                "onSurface/surfaceContainerHigh" to (s.onSurface to s.surfaceContainerHigh),
                "onSurfaceVariant/surfaceContainerLow" to (s.onSurfaceVariant to s.surfaceContainerLow),
                "primary/surface (section headings)" to (s.primary to s.surface),
                "onPrimary/primary" to (s.onPrimary to s.primary),
                "onPrimaryContainer/primaryContainer" to (s.onPrimaryContainer to s.primaryContainer),
                "onSecondaryContainer/secondaryContainer" to (s.onSecondaryContainer to s.secondaryContainer),
            )
        }

    @Test
    fun iconsAndBoundariesMeetThreeToOne() =
        assertPairs(3.0) { s ->
            mapOf(
                "primary icon/surfaceContainerHigh tile" to (s.primary to s.surfaceContainerHigh),
                "onSurfaceVariant icon/surfaceContainer tile" to (s.onSurfaceVariant to s.surfaceContainer),
                "outline/surface (chip and field borders)" to (s.outline to s.surface),
            )
        }
}
