package tech.onova.offlinetranslator.support

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.onova.offlinetranslator.core.designsystem.theme.PocketTheme

/** Application without Koin of its own: each test starts its Koin graph through [KoinFakesRule]. */
class TestApplication : Application()

/** Base for Robolectric UI tests of single screens: real screens, ViewModels and tool registry. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = RobolectricDeviceQualifiers.Pixel7, application = TestApplication::class)
abstract class AppUiTest {
    @get:Rule(order = 0)
    val fakes = KoinFakesRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    val appSettings get() = fakes.appSettings
    val language get() = fakes.language

    fun show(
        darkTheme: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        compose.setContent { PocketTheme(darkTheme = darkTheme, dynamicColor = false, content = content) }
        compose.waitForIdle()
    }

    /**
     * Presses the control called [text] without a touch, so no ripple is left fading under a baseline taken right
     * after: a ripple runs on the wall clock, and a slow machine catches it halfway.
     */
    fun pressWithoutRipple(text: String) {
        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    fun capture(name: String) {
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}
