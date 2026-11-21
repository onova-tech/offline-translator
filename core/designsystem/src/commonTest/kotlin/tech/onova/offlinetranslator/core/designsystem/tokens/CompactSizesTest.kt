package tech.onova.offlinetranslator.core.designsystem.tokens

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Spec 015 FR-012, FR-016, FR-017: what follows the window, and where it stops. */
class CompactSizesTest {
    /** Today's values, as of commit 77d9237: a later edit of a maximum fails here, not only in a screenshot. */
    private val todays =
        listOf(
            PocketSizes.GRAPH_MIN_HEIGHT to 56.dp,
            PocketSizes.PREVIEW_MIN_HEIGHT to 96.dp,
            PocketSizes.CONTENT_MIN_HEIGHT to 96.dp,
            PocketSizes.GAUGE_MIN to 64.dp,
            PocketSizes.DIAL_MIN to 120.dp,
            PocketSizes.PROGRESS_RING_MIN to 96.dp,
            PocketSizes.FAVORITE_CARD_HEIGHT to 108.dp,
            PocketSizes.LARGE_BUTTON_HEIGHT to 96.dp,
            PocketSizes.BUTTON_HEIGHT to 56.dp,
            PocketSizes.KEYPAD_KEY_MIN_HEIGHT to 56.dp,
            PocketSizes.CALCULATOR_DISPLAY_MIN_HEIGHT to 112.dp,
            PocketSizes.TIMER_FORM_RESERVED_HEIGHT to 320.dp,
            PocketSpacing.XXS to 4.dp,
            PocketSpacing.XS to 8.dp,
            PocketSpacing.SM to 12.dp,
            PocketSpacing.MD to 16.dp,
            PocketSpacing.LG to 24.dp,
            PocketSpacing.XL to 32.dp,
            PocketSpacing.XXL to 48.dp,
        )

    private val touched =
        listOf(
            PocketSizes.FAVORITE_CARD_HEIGHT,
            PocketSizes.LARGE_BUTTON_HEIGHT,
            PocketSizes.BUTTON_HEIGHT,
            PocketSizes.KEYPAD_KEY_MIN_HEIGHT,
            PocketSizes.TOP_BAR_HEIGHT,
        )

    @Test
    fun fromPhoneSizeUpEverySizeIsTodays() = todays.forEach { (size, today) -> assertEquals(today, size.at(1f)) }

    @Test
    fun theTopBarIsMaterialsFromPhoneSizeUp() = assertEquals(64.dp, PocketSizes.TOP_BAR_HEIGHT.at(1f))

    @Test
    fun nothingTouchedGoesUnder36Dp() {
        touched.forEach { size -> assertTrue(size.at(0f) >= 36.dp, "${size.at(0f)}") }
        assertEquals(36.dp, PocketSizes.touchTargetAt(0f, fontScale = 1f))
    }

    /** Principle X, amended on 2026-11-06. */
    @Test
    fun aControlStaysAt48DpFromPhoneSizeUpAndWithLargerText() {
        assertEquals(48.dp, PocketSizes.touchTargetAt(1f, fontScale = 1f))
        assertEquals(48.dp, PocketSizes.touchTargetAt(0f, fontScale = 1.1f))
        assertEquals(48.dp, PocketSizes.touchTargetAt(1f, fontScale = 2f))
    }

    @Test
    fun theTargetReachesItsSmallestHalfwayDown() {
        assertEquals(36.dp, PocketSizes.touchTargetAt(0.5f, fontScale = 1f))
        assertEquals(42.dp, PocketSizes.touchTargetAt(0.75f, fontScale = 1f))
    }

    @Test
    fun theContentKeepsAScrollerInsideItsPadding() {
        val scroller = PocketSizes.touchTargetAt(0f, fontScale = 2f)
        assertTrue(PocketSizes.CONTENT_MIN_HEIGHT.at(0f) >= scroller + PocketSpacing.MD.at(0f) * 2)
    }

    @Test
    fun theKeypadFitsTheSmallestWindow() {
        assertTrue(PocketSpacing.MD.at(0f) <= 8.dp)
        assertTrue(PocketSpacing.XS.at(0f) <= 4.dp)
    }

    @Test
    fun everySizeIsAWholeDp() {
        todays.forEach { (size, _) ->
            for (step in 0..39) {
                val value = size.at(step / 40f).value
                assertEquals(value.toInt().toFloat(), value)
            }
        }
    }
}
