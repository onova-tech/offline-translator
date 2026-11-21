package tech.onova.offlinetranslator.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import tech.onova.offlinetranslator.core.designsystem.components.IconAction
import tech.onova.offlinetranslator.core.designsystem.components.ToolFrame
import tech.onova.offlinetranslator.core.designsystem.icons.PocketIcons
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketTypography
import tech.onova.offlinetranslator.support.AppUiTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Spec 015 FR-008, FR-009: the phone's text setting always multiplies what the window gives (Principle X). */
abstract class TextSettingTest : AppUiTest() {
    private fun roles(): Map<String, TextUnit> {
        var sizes = emptyMap<String, TextUnit>()
        show {
            val type = MaterialTheme.typography
            sizes =
                mapOf(
                    "readout" to PocketTypography.readout.fontSize,
                    "readoutMedium" to PocketTypography.readoutMedium.fontSize,
                    "displaySmall" to type.displaySmall.fontSize,
                    "headlineMedium" to type.headlineMedium.fontSize,
                    "headlineSmall" to type.headlineSmall.fontSize,
                    "titleLarge" to type.titleLarge.fontSize,
                    "titleMedium" to type.titleMedium.fontSize,
                    "titleSmall" to type.titleSmall.fontSize,
                    "bodyLarge" to type.bodyLarge.fontSize,
                    "bodyMedium" to type.bodyMedium.fontSize,
                    "bodySmall" to type.bodySmall.fontSize,
                    "labelLarge" to type.labelLarge.fontSize,
                    "labelMedium" to type.labelMedium.fontSize,
                    "labelSmall" to type.labelSmall.fontSize,
                )
        }
        return sizes
    }

    /** The platform's own curve keeps big text nearly flat, so a big reading may hold at 130 %; it never shrinks. */
    @Test
    fun aLargerSettingNeverGivesSmallerText() {
        roles().forEach { (role, size) ->
            val atDefault = with(Density(1f, 1f)) { size.toPx() }
            val larger = with(Density(1f, 1.3f)) { size.toPx() }
            val largest = with(Density(1f, 2f)) { size.toPx() }
            assertTrue(
                atDefault <= larger && larger <= largest && atDefault < largest,
                "$role: $atDefault, $larger, $largest",
            )
        }
    }

    @Test
    fun aLargerSettingAlwaysGivesLargerBodyText() {
        val body = roles().filterKeys { it.startsWith("body") || it.startsWith("label") }
        body.forEach { (role, size) ->
            val atDefault = with(Density(1f, 1f)) { size.toPx() }
            val larger = with(Density(1f, 1.3f)) { size.toPx() }
            val largest = with(Density(1f, 2f)) { size.toPx() }
            assertTrue(atDefault < larger && larger < largest, "$role: $atDefault, $larger, $largest")
        }
    }

    @Test
    fun theSettingDoesNotChangeARole() {
        val atDefault = roles()
        RuntimeEnvironment.setFontScale(2f)
        assertEquals(atDefault, roles())
    }
}

@Config(qualifiers = "w240dp-h320dp-port-xhdpi")
class TextSettingSmallestTest : TextSettingTest()

@Config(qualifiers = "w300dp-h460dp-port-xhdpi")
class TextSettingBetweenTest : TextSettingTest()

class TextSettingPhoneTest : TextSettingTest()

/** Spec 015 FR-020: at 200 % in a tiny window a title that can't fit beside the actions takes its own row, whole. */
@Config(qualifiers = "w240dp-h360dp-port-xhdpi")
class TitleRowTest : AppUiTest() {
    /** The frame as a tool with two actions draws it, with the app's longest title. */
    private fun titleAgainstBack(fontScale: Float): Pair<Float, Float> {
        RuntimeEnvironment.setFontScale(fontScale)
        show {
            ToolFrame(
                title = "Open-source licenses",
                backLabel = "Back",
                onBack = {},
                actions = {
                    IconAction(icon = PocketIcons.Info, label = "How to use", onClick = {})
                    IconAction(icon = PocketIcons.Settings, label = "Settings", onClick = {})
                },
            ) {}
        }
        val title = compose.onNode(hasText("Open-source licenses")).getUnclippedBoundsInRoot()
        val back = compose.onNodeWithContentDescription("Back").getUnclippedBoundsInRoot()
        return title.top.value to back.bottom.value
    }

    @Test
    fun atTheDefaultSizeTheTitleSitsBesideTheActions() {
        val (top, backBottom) = titleAgainstBack(1f)
        assertTrue(top < backBottom, "title at $top, back ends at $backBottom")
    }

    @Test
    fun atTwiceTheSizeTheTitleHasItsOwnRow() {
        val (top, backBottom) = titleAgainstBack(2f)
        assertTrue(top >= backBottom - 1f, "title at $top, back ends at $backBottom")
    }
}
