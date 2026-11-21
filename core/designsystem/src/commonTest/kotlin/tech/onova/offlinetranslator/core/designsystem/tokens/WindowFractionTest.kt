package tech.onova.offlinetranslator.core.designsystem.tokens

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Spec 015 FR-004: where a window stands between the smallest one and phone size. */
class WindowFractionTest {
    private fun fraction(
        width: Int,
        height: Int,
    ) = windowFraction(width.dp, height.dp)

    @Test
    fun phonesTabletsAndLandscapeAreAtOne() {
        listOf(
            411 to 914,
            914 to 411,
            1280 to 800,
            411 to 850,
            800 to 1200,
            850 to 380,
            360 to 640,
            640 to 360,
            1280 to 740,
        ).forEach { (width, height) -> assertEquals(1f, fraction(width, height), "$width x $height") }
    }

    @Test
    fun theSmallestWindowsAreAtZero() {
        listOf(240 to 360, 240 to 320, 220 to 300, 220 to 220, 420 to 240, 559 to 300)
            .forEach { (width, height) -> assertEquals(0f, fraction(width, height), "$width x $height") }
    }

    @Test
    fun betweenThemItFollowsTheTighterSide() {
        assertEquals(0.5f, fraction(300, 460))
        assertEquals(0.5f, fraction(560, 300))
        assertEquals(0.675f, fraction(320, 520))
        assertEquals(0.975f, fraction(360, 592))
        assertEquals(0.275f, fraction(411, 400))
        assertEquals(0.325f, fraction(280, 420))
    }

    @Test
    fun itNeverLeavesZeroToOne() {
        for (width in 1..4000 step 97) {
            for (height in 1..4000 step 89) {
                assertTrue(fraction(width, height) in 0f..1f, "$width x $height")
            }
        }
    }
}
