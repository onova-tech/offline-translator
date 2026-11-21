package tech.onova.offlinetranslator.core.designsystem.components

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/** How a screen's spare height is shared among its flexible parts (graph, dial, preview). */
class FlexShareTest {
    private fun allocate(
        free: Int,
        mins: List<Int>,
        maxs: List<Int>,
        weights: List<Float> = mins.map { 1f },
    ) = FlexShare.allocate(free, mins, maxs, weights)

    @Test
    fun aShortScreenGivesEveryPartItsMinimum() {
        assertContentEquals(intArrayOf(100, 80), allocate(free = 150, mins = listOf(100, 80), maxs = listOf(300, 300)))
    }

    @Test
    fun aNegativeFreeHeightStillGivesTheMinimums() {
        assertContentEquals(intArrayOf(100), allocate(free = -40, mins = listOf(100), maxs = listOf(300)))
    }

    @Test
    fun spareHeightIsSharedByWeight() {
        val heights = allocate(free = 500, mins = listOf(100, 100), maxs = listOf(1000, 1000), weights = listOf(1f, 3f))
        // 300 spare: one quarter and three quarters.
        assertContentEquals(intArrayOf(175, 325), heights)
    }

    @Test
    fun aPartAtItsMaximumHandsTheRestToTheOthers() {
        val heights = allocate(free = 600, mins = listOf(100, 100), maxs = listOf(150, 1000))
        assertContentEquals(intArrayOf(150, 450), heights)
    }

    @Test
    fun aTallScreenStopsAtTheMaximums() {
        assertContentEquals(intArrayOf(160, 200), allocate(free = 2000, mins = listOf(80, 80), maxs = listOf(160, 200)))
    }

    @Test
    fun noRoundingLeavesPixelsBehind() {
        val heights = allocate(free = 100, mins = listOf(0, 0, 0), maxs = listOf(1000, 1000, 1000))
        assertEquals(100, heights.sum())
    }

    @Test
    fun noFlexibleChildrenIsFine() {
        assertContentEquals(intArrayOf(), allocate(free = 100, mins = emptyList(), maxs = emptyList()))
    }
}
