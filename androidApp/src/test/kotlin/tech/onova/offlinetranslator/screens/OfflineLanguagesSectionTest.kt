package tech.onova.offlinetranslator.screens

import android.os.Looper
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.feature.settings.SettingsScreen
import tech.onova.offlinetranslator.feature.toolstranslate.presentation.OfflineLanguagesSection
import tech.onova.offlinetranslator.support.AppUiTest
import tech.onova.offlinetranslator.support.FakeContentPacks
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Settings → Offline languages (spec 012 story 3; spec 080 T047, owner 2026-11-20): the Wi-Fi switch, and "Manage
 * languages" with the space the packs use, which opens the Manage languages page.
 */
class OfflineLanguagesSectionTest : AppUiTest() {
    private val packPt = PackId("translate_pt")
    private val packEs = PackId("translate_es")
    private val inDialog = hasAnyAncestor(isDialog())

    private fun showSettings() {
        fakes.contentPacks.put(FakeContentPacks.installed(packPt))
        fakes.contentPacks.put(FakeContentPacks.notInstalled(packEs))
        show { SettingsScreen(onBack = {}, offlineLanguages = { OfflineLanguagesSection(it) }) }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(MANAGE))
        compose.waitForIdle()
    }

    /** The packs' actions start on the main looper, after a DataStore read on another thread. */
    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(WAIT_MILLIS) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    private fun waitForText(text: String) {
        waitFor { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun theSwitchAndManageLanguagesWithTheSpaceUsed() {
        showSettings()
        compose.onNodeWithText("Offline languages").assertExists()
        compose.onNodeWithText("Download only on Wi-Fi").assertExists()
        waitForText("48 MB used")
        compose.onNodeWithText("Portuguese").assertDoesNotExist()
    }

    @Test
    fun manageLanguagesOpensThePage() {
        showSettings()
        compose.onNodeWithText(MANAGE).performClick()
        waitFor { compose.onAllNodes(hasText("Downloaded languages") and inDialog).fetchSemanticsNodes().isNotEmpty() }
    }

    /** Spec 081: after Language, before About. */
    @Test
    fun theSectionSitsAfterLanguageAndBeforeAbout() {
        showSettings()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Offline languages"))
        compose.waitForIdle()
        val languageRow = compose.onNodeWithText("App language").getUnclippedBoundsInRoot()
        val section = compose.onNodeWithText("Offline languages").getUnclippedBoundsInRoot()
        assertTrue(languageRow.bottom <= section.top, "after Language: $languageRow $section")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("About"))
        compose.waitForIdle()
        val manage = compose.onNodeWithText(MANAGE).getUnclippedBoundsInRoot()
        val about = compose.onNodeWithText("About").getUnclippedBoundsInRoot()
        assertTrue(manage.bottom <= about.top, "before About: $manage $about")
    }

    @Test
    fun theWifiSwitchIsOnAtFirstAndADownloadFollowsIt() {
        showSettings()
        compose.onNodeWithText("Download only on Wi-Fi").assertIsOn().performClick()
        compose.waitUntil(WAIT_MILLIS) {
            compose.onAllNodesWithText("Download only on Wi-Fi").fetchSemanticsNodes().any {
                it.config.getOrNull(SemanticsProperties.ToggleableState) == ToggleableState.Off
            }
        }
        compose.onNodeWithText("Download only on Wi-Fi").assertIsOff()
        // Kept in the Translator's settings, so a download on the page may use mobile data.
        compose.onNodeWithText(MANAGE).performClick()
        waitFor { compose.onAllNodes(hasText("Downloaded languages") and inDialog).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasSetTextAction() and inDialog).performTextReplacement("spanish")
        compose.waitForIdle()
        // Not downloaded here: in "Other languages", opened by its header.
        compose.onNode(hasText("Spanish") and hasClickAction() and inDialog).performClick()
        compose.waitForIdle()
        val menu = hasContentDescription("More actions for Spanish, Translation") and inDialog
        compose.onNode(hasScrollToNodeAction() and inDialog).performScrollToNode(menu)
        compose.onNode(menu).performClick()
        compose.waitForIdle()
        compose.onAllNodesWithText("Download").onLast().performClick()
        waitFor { fakes.contentPacks.downloads.isNotEmpty() }
        assertEquals(listOf(packEs to false), fakes.contentPacks.downloads)
    }

    private companion object {
        const val MANAGE = "Manage languages"
        const val WAIT_MILLIS = 5_000L
    }
}
