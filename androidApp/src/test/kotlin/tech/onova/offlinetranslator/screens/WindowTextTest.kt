package tech.onova.offlinetranslator.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.junit.Test
import org.robolectric.annotation.Config
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketTypography
import tech.onova.offlinetranslator.support.AppUiTest
import kotlin.math.roundToInt
import kotlin.test.assertEquals

/** Spec 015 FR-004, FR-006: the type follows the window, also in a dialog, and never the space a screen gets. */
abstract class WindowTextTest(
    private val body: TextUnit,
    private val readout: TextUnit,
    private val window: DpSize,
) : AppUiTest() {
    private var seen = emptyList<Seen>()

    private data class Seen(
        val where: String,
        val window: DpSize,
        val body: TextUnit,
        val readout: TextUnit,
        val spacing: androidx.compose.ui.unit.Dp,
    )

    @Composable
    private fun Read(where: String) {
        seen =
            seen +
            Seen(
                where,
                LocalWindowInfo.current.containerDpSize,
                MaterialTheme.typography.bodyLarge.fontSize,
                PocketTypography.readout.fontSize,
                PocketSpacing.md,
            )
    }

    @Test
    fun theTypeFollowsTheWindowEverywhere() {
        show {
            Read("the screen")
            Box(Modifier.size(100.dp)) { Read("a smaller box") }
            Box(Modifier.imePadding()) { Read("under the keyboard's padding") }
            Dialog(onDismissRequest = {}) { Read("a dialog") }
        }
        compose.waitUntil { seen.size >= WHERE }
        seen.forEach {
            assertEquals(
                window.width.value.roundToInt(),
                it.window.width.value
                    .roundToInt(),
                it.where,
            )
            assertEquals(
                window.height.value.roundToInt(),
                it.window.height.value
                    .roundToInt(),
                it.where,
            )
            assertEquals(body, it.body, it.where)
            assertEquals(readout, it.readout, it.where)
        }
        assertEquals(1, seen.map { it.spacing }.distinct().size, "$seen")
    }

    private companion object {
        const val WHERE = 4
    }
}

@Config(qualifiers = "w240dp-h360dp-port-xhdpi")
class WindowTextSmallestTest : WindowTextTest(12.sp, 32.sp, DpSize(240.dp, 360.dp))

@Config(qualifiers = "w300dp-h460dp-port-xhdpi")
class WindowTextBetweenTest : WindowTextTest(14.sp, 54.sp, DpSize(300.dp, 460.dp))

class WindowTextPhoneTest : WindowTextTest(16.sp, 76.sp, DpSize(411.dp, 914.dp))

@Config(qualifiers = "w1280dp-h800dp-land-xhdpi")
class WindowTextTabletTest : WindowTextTest(16.sp, 76.sp, DpSize(1280.dp, 800.dp))
