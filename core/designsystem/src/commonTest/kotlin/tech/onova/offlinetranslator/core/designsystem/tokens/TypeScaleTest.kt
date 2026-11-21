package tech.onova.offlinetranslator.core.designsystem.tokens

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Spec 015 FR-001 to FR-003: each text role has a minimum and a maximum, in whole sp. */
class TypeScaleTest {
    private val limits: Map<String, Pair<(Float) -> TextStyle, Pair<Float, Float>>> =
        mapOf(
            "readout" to (PocketTypography::readoutAt to (32f to 76f)),
            "readoutMedium" to (PocketTypography::readoutMediumAt to (24f to 44f)),
            "displayLarge" to ({ t: Float -> PocketTypography.material(t).displayLarge } to (32f to 76f)),
            "displaySmall" to ({ t: Float -> PocketTypography.material(t).displaySmall } to (20f to 36f)),
            "headlineMedium" to ({ t: Float -> PocketTypography.material(t).headlineMedium } to (18f to 30f)),
            "headlineSmall" to ({ t: Float -> PocketTypography.material(t).headlineSmall } to (16f to 24f)),
            "titleLarge" to ({ t: Float -> PocketTypography.material(t).titleLarge } to (14f to 22f)),
            "titleMedium" to ({ t: Float -> PocketTypography.material(t).titleMedium } to (13f to 16f)),
            "titleSmall" to ({ t: Float -> PocketTypography.material(t).titleSmall } to (12f to 14f)),
            "bodyLarge" to ({ t: Float -> PocketTypography.material(t).bodyLarge } to (12f to 16f)),
            "bodyMedium" to ({ t: Float -> PocketTypography.material(t).bodyMedium } to (11f to 14f)),
            "bodySmall" to ({ t: Float -> PocketTypography.material(t).bodySmall } to (11f to 13f)),
            "labelLarge" to ({ t: Float -> PocketTypography.material(t).labelLarge } to (11f to 14f)),
            "labelMedium" to ({ t: Float -> PocketTypography.material(t).labelMedium } to (11f to 13f)),
            "labelSmall" to ({ t: Float -> PocketTypography.material(t).labelSmall } to (11f to 12f)),
        )

    @Test
    fun everyRoleGoesFromItsMinimumToItsMaximum() {
        limits.forEach { (role, entry) ->
            val (style, range) = entry
            assertEquals(range.first.sp, style(0f).fontSize, "$role at the smallest window")
            assertEquals(range.second.sp, style(1f).fontSize, "$role from phone size up")
            assertTrue(range.first >= MIN_ROLE_SIZE, "$role is under $MIN_ROLE_SIZE sp")
        }
    }

    @Test
    fun sizesAreWholeAndNeverLeaveTheirLimits() {
        limits.forEach { (role, entry) ->
            val (style, range) = entry
            for (step in 0..STEPS) {
                val size = style(step / STEPS.toFloat()).fontSize.value
                assertEquals(size.toInt().toFloat(), size, "$role at step $step")
                assertTrue(size in range.first..range.second, "$role at step $step is $size")
            }
        }
    }

    @Test
    fun aTieGoesUp() = assertEquals(15.sp, PocketTypography.material(0.5f).titleMedium.fontSize)

    @Test
    fun lineHeightKeepsItsRatio() {
        val atMax = PocketTypography.material(1f).titleLarge
        val small = PocketTypography.material(0f).titleLarge
        val expected = atMax.lineHeight.value * small.fontSize.value / atMax.fontSize.value
        assertTrue(abs(small.lineHeight.value - expected) <= ROUNDING, "${small.lineHeight} against $expected")
    }

    @Test
    fun displaySmallIsMaterialsFromPhoneSizeUp() =
        assertEquals(Typography().displaySmall.fontSize, PocketTypography.material(1f).displaySmall.fontSize)

    private companion object {
        const val MIN_ROLE_SIZE = 11f
        const val STEPS = 40
        const val ROUNDING = 0.5f
    }
}
