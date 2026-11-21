package tech.onova.offlinetranslator.screens

import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import tech.onova.offlinetranslator.core.domain.permissions.AppPermission
import tech.onova.offlinetranslator.core.domain.permissions.PermissionStatus
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.SpeechEvent
import tech.onova.offlinetranslator.feature.settings.SettingsScreen
import tech.onova.offlinetranslator.feature.toolstranslate.presentation.OfflineLanguagesSection
import tech.onova.offlinetranslator.support.AccessibilityAudit
import tech.onova.offlinetranslator.support.AppUiTest
import tech.onova.offlinetranslator.support.FakeContentPacks
import tech.onova.offlinetranslator.tool.ToolScreen
import java.time.Duration
import kotlin.test.assertTrue

/**
 * Principle X for the Translator (spec 012 T059): labels, touch targets and overlaps on both modes, the pack offer and
 * Settings → Offline languages, in light, dark and 200% text. Beside `AccessibilityAuditTest`, which is long enough.
 */
class TranslatorAccessibilityAuditTest : AppUiTest() {
    private lateinit var density: Density

    private fun assertAccessible(
        darkTheme: Boolean = false,
        navigate: () -> Unit = {},
        content: @Composable () -> Unit,
    ) {
        show(darkTheme = darkTheme) {
            density = LocalDensity.current
            content()
        }
        navigate()
        compose.waitForIdle()
        val violations =
            compose.onAllNodes(isRoot()).fetchSemanticsNodes().flatMap { root ->
                AccessibilityAudit.violations(root, density)
            }
        assertTrue(violations.isEmpty(), violations.joinToString("\n"))
    }

    private fun waitForText(text: String) {
        compose.waitUntil(WAIT_MILLIS) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun translated() {
        // The screen shows once its settings are read.
        waitForText("Text")
        compose.onNodeWithText("Text").performTextInput("Good morning")
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(PAUSE_MILLIS))
        waitForText("[es] Good morning")
    }

    /** A turn heard on the near half and translated on the far one. */
    private fun conversationWithATurn() {
        fakes.permissions.statuses[AppPermission.RECORD_AUDIO] = PermissionStatus.GRANTED
        // The modes show once the settings are read, on another thread.
        waitForText("Conversation")
        compose.onNodeWithText("Conversation").performClick()
        compose.waitUntil(WAIT_MILLIS) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodesWithText("Your language", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() ||
                compose
                    .onAllNodes(
                        androidx.compose.ui.test
                            .hasContentDescription("Speak English"),
                    ).fetchSemanticsNodes()
                    .isNotEmpty()
        }
        compose.onNodeWithContentDescription("Speak English").performClick()
        compose.waitUntil(WAIT_MILLIS) { fakes.speechInput.listenedTo.isNotEmpty() }
        fakes.speechInput.hear(SpeechEvent.Final("Good morning"))
        waitForText("[es] Good morning")
        fakes.speechOutput.done()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun typeOrSpeak() =
        assertAccessible(navigate = { waitForText("Type or speak") }) { ToolScreen(toolId = "translator", onOpenSettings = {}) }

    @Test
    fun typeOrSpeakTranslatedLargeTextDark() {
        RuntimeEnvironment.setFontScale(2f)
        assertAccessible(darkTheme = true, navigate = ::translated) { ToolScreen(toolId = "translator", onOpenSettings = {}) }
    }

    @Test
    fun thePackOffer() {
        fakes.contentPacks.put(FakeContentPacks.notInstalled(PackId("translate_es")))
        assertAccessible(navigate = { waitForText("Download") }) { ToolScreen(toolId = "translator", onOpenSettings = {}) }
    }

    @Test
    fun thePackOfferDownloadingLargeTextDark() {
        RuntimeEnvironment.setFontScale(2f)
        fakes.contentPacks.put(FakeContentPacks.notInstalled(PackId("translate_es")))
        assertAccessible(darkTheme = true, navigate = {
            waitForText("Download")
            compose.onNodeWithText("Download").performClick()
            waitForText("Cancel")
        }) { ToolScreen(toolId = "translator", onOpenSettings = {}) }
    }

    @Test
    fun conversation() =
        assertAccessible(navigate = ::conversationWithATurn) { ToolScreen(toolId = "translator", onOpenSettings = {}) }

    @Test
    fun conversationLargeTextDark() {
        RuntimeEnvironment.setFontScale(2f)
        assertAccessible(darkTheme = true, navigate = ::conversationWithATurn) {
            ToolScreen(toolId = "translator", onOpenSettings = {})
        }
    }

    @Test
    fun offlineLanguages() =
        assertAccessible(navigate = ::scrollToOfflineLanguages) {
            SettingsScreen(onBack = {}, offlineLanguages = { OfflineLanguagesSection(it) })
        }

    @Test
    fun offlineLanguagesLargeTextDark() {
        RuntimeEnvironment.setFontScale(2f)
        assertAccessible(darkTheme = true, navigate = ::scrollToOfflineLanguages) {
            SettingsScreen(onBack = {}, offlineLanguages = { OfflineLanguagesSection(it) })
        }
    }

    /** Spec 080: the language page, from From's list. */
    private fun languagePage() {
        waitForText("Type or speak")
        compose.onNodeWithContentDescription("From").performClick()
        waitForText("Manage languages…")
        compose.onNode(hasText("Manage languages…") and hasAnyAncestor(isPopup())).performClick()
        waitForText("Downloaded languages")
    }

    @Test
    fun theLanguagePage() =
        assertAccessible(navigate = ::languagePage) { ToolScreen(toolId = "translator", onOpenSettings = {}) }

    /** Spec 080 SC-005: in landscape too, where the page is shortest. */
    @Test
    @Config(qualifiers = "+land")
    fun theLanguagePageLandscape() =
        assertAccessible(navigate = ::languagePage) { ToolScreen(toolId = "translator", onOpenSettings = {}) }

    @Test
    fun theLanguagePageLargeTextDark() {
        RuntimeEnvironment.setFontScale(2f)
        assertAccessible(darkTheme = true, navigate = ::languagePage) { ToolScreen(toolId = "translator", onOpenSettings = {}) }
    }

    private fun scrollToOfflineLanguages() {
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Manage languages"))
    }

    private companion object {
        const val PAUSE_MILLIS = 600L
        const val WAIT_MILLIS = 5_000L
    }
}
