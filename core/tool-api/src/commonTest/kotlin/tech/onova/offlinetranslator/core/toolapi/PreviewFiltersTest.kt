package tech.onova.offlinetranslator.core.toolapi

import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Spec 052: the reading filters, applied as Android applies a color matrix, each channel kept within 0…255. */
class PreviewFiltersTest {
    private fun gray(
        filter: PreviewFilter,
        level: Int,
    ): Int {
        val m = checkNotNull(PreviewFilters.matrix(filter))
        val red = m[0] * level + m[1] * level + m[2] * level + m[4]
        return red.roundToInt().coerceIn(0, 255)
    }

    @Test
    fun noFilterLeavesThePictureAlone() {
        assertNull(PreviewFilters.matrix(PreviewFilter.NONE))
    }

    @Test
    fun contrastMakesDarkDarkerAndLightLighterAroundTheMiddle() {
        assertEquals(0, gray(PreviewFilter.CONTRAST, 40))
        assertEquals(128, gray(PreviewFilter.CONTRAST, 128))
        assertEquals(255, gray(PreviewFilter.CONTRAST, 220))
        assertEquals(32, gray(PreviewFilter.CONTRAST, 80))
    }

    @Test
    fun invertedTurnsDarkPrintLightOnDark() {
        assertEquals(255, gray(PreviewFilter.INVERTED, 40))
        assertEquals(127, gray(PreviewFilter.INVERTED, 128))
        assertEquals(0, gray(PreviewFilter.INVERTED, 220))
    }

    @Test
    fun theFiltersAreGrayAndKeepTheAlpha() {
        val m = checkNotNull(PreviewFilters.matrix(PreviewFilter.CONTRAST))
        assertEquals(m.slice(0..4), m.slice(5..9))
        assertEquals(m.slice(0..4), m.slice(10..14))
        assertEquals(listOf(0f, 0f, 0f, 1f, 0f), m.slice(15..19))
    }
}
