package tech.onova.offlinetranslator.screens

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.robolectric.annotation.Config
import tech.onova.offlinetranslator.MainActivity
import tech.onova.offlinetranslator.core.designsystem.components.BottomAction
import tech.onova.offlinetranslator.core.designsystem.components.FitColumn
import tech.onova.offlinetranslator.core.designsystem.components.ReservedLine
import tech.onova.offlinetranslator.core.designsystem.components.ToolLayout
import tech.onova.offlinetranslator.core.designsystem.components.flex
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes
import tech.onova.offlinetranslator.support.AppUiTest
import tech.onova.offlinetranslator.tool.ToolScreen
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Spec 015 FR-018, FR-034: a very short window keeps a usable content area, and messages don't move the controls. */
@Config(qualifiers = "w240dp-h360dp-port-xhdpi")
class ToolLayoutHeightTest : AppUiTest() {
    @Test
    fun inAWindowTooShortForEverythingTheWholeLayoutScrolls() {
        // What 220x220 leaves under the top bar, with a status bar and a gesture bar on top of that.
        show {
            Box(Modifier.height(124.dp)) {
                ToolLayout(
                    top = { Text("Selector", Modifier.height(PocketSizes.touchTarget)) },
                    bottom = { BottomAction(label = "Act", onClick = {}) },
                    scrollContent = false,
                ) {
                    FitColumn(modifier = Modifier.weight(1f)) {
                        Box(Modifier.flex(PocketSizes.touchTarget).testTag("flexible"))
                    }
                }
            }
        }
        assertTrue(compose.onNodeWithTag("flexible").getUnclippedBoundsInRoot().height >= SMALLEST_TARGET)
        compose.onNodeWithText("Act").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun aMessageThatAppearsMovesNoControl() {
        var message by mutableStateOf("")
        val long = "A long message that wraps to several lines in a window this narrow, and then some more."
        show {
            ToolLayout(bottom = { BottomAction(label = "Act", onClick = {}) }) {
                Text("Content")
                ReservedLine(text = message, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            }
        }
        val before = compose.onNodeWithText("Act").getBoundsInRoot()
        compose.runOnIdle { message = long }
        compose.waitForIdle()
        assertEquals(before, compose.onNodeWithText("Act").getBoundsInRoot())
    }
}

/** Principle X in a reduced window. */
private val SMALLEST_TARGET = 36.dp

/** Spec 015 FR-023, FR-025: a resize changes the configuration, without recreating the activity. */
class ManifestWindowTest : AppUiTest() {
    private val info: ActivityInfo =
        ApplicationProvider.getApplicationContext<android.content.Context>().let { context ->
            context.packageManager.getActivityInfo(
                ComponentName(context, MainActivity::class.java),
                PackageManager.GET_META_DATA,
            )
        }

    @Test
    fun aResizeDoesNotRecreateTheActivity() {
        val wanted =
            ActivityInfo.CONFIG_SCREEN_SIZE or ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE or
                ActivityInfo.CONFIG_SCREEN_LAYOUT
        assertEquals(wanted, info.configChanges and wanted)
    }

    @Test
    fun rotationStaysAsTested() = assertEquals(0, info.configChanges and ActivityInfo.CONFIG_ORIENTATION)

    @Test
    fun theManifestSetsNoMinimumWindow() = assertEquals(null, info.windowLayout)
}
