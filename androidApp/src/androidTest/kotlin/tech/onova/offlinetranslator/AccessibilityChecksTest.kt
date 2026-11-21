package tech.onova.offlinetranslator

import android.content.res.Configuration
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tech.onova.offlinetranslator.core.designsystem.icons.PocketIcons
import tech.onova.offlinetranslator.core.designsystem.theme.PocketTheme
import tech.onova.offlinetranslator.feature.settings.SettingsScreen
import tech.onova.offlinetranslator.tool.ToolScreen
import java.util.Locale

/**
 * The same accessibility checks on a real Android runtime (device or emulator), using the app's
 * real Koin graph and storage. Run with `./gradlew connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityChecksTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun enableChecks() = compose.enableAccessibilityChecks()

    /**
     * Checks only once [readyText] is shown, so screens that load stored data are not empty. App
     * strings are pinned to English, so the waits and clicks find their text on a device set to
     * any language.
     */
    private fun verify(
        readyText: String,
        darkTheme: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            val english = Configuration(LocalConfiguration.current).apply { setLocale(Locale.US) }
            CompositionLocalProvider(LocalConfiguration provides english) {
                PocketTheme(darkTheme = darkTheme, dynamicColor = false, content = content)
            }
        }
        compose.waitUntil(
            LOAD_TIMEOUT_MILLIS,
        ) { compose.onAllNodesWithText(readyText).fetchSemanticsNodes().isNotEmpty() }
        compose.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun settings() = verify("Appearance") { SettingsScreen(onBack = {}) }

    @Test
    fun settingsDark() = verify("Appearance", darkTheme = true) { SettingsScreen(onBack = {}) }

    // Without Google Play (the ATD image) the pack offer says packs can't be downloaded here (spec 012).
    @Test
    fun translator() = verify("Type or speak") { ToolScreen(toolId = "translator", onOpenSettings = {}) }

    @Test
    fun translatorDark() =
        verify("Type or speak", darkTheme = true) { ToolScreen(toolId = "translator", onOpenSettings = {}) }

    /** Negative control: proves the checks really inspect Compose nodes on this runtime. */
    @Test
    fun checksCatchAnUnlabeledIconButton() {
        compose.setContent {
            PocketTheme(darkTheme = false, dynamicColor = false) {
                IconButton(onClick = {}) { Icon(PocketIcons.Back, contentDescription = null) }
            }
        }
        val error = assertThrows(Throwable::class.java) { compose.onRoot().tryPerformAccessibilityChecks() }
        Log.i(TAG, "Negative control caught: $error")
    }

    private companion object {
        const val TAG = "AccessibilityChecks"
        const val LOAD_TIMEOUT_MILLIS = 10_000L
    }
}
