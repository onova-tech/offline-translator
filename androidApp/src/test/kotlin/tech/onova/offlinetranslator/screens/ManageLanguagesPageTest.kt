package tech.onova.offlinetranslator.screens

import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.view.WindowManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog
import tech.onova.offlinetranslator.core.domain.permissions.AppPermission
import tech.onova.offlinetranslator.core.domain.permissions.PermissionStatus
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport
import tech.onova.offlinetranslator.core.domain.speech.SpeechCatalog
import tech.onova.offlinetranslator.support.AppUiTest
import tech.onova.offlinetranslator.support.FakeContentPacks
import tech.onova.offlinetranslator.tool.ToolScreen
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Spec 080 story 4 (owner, 2026-11-20): "Manage languages…" ends every list and opens one page: "Downloaded languages"
 * and "Other languages", each language a card that opens on its "Enable language", translation pack, speech recognition
 * and voice, with a mark for what's there.
 */
class ManageLanguagesPageTest : AppUiTest() {
    private val inDialog = hasAnyAncestor(isDialog())
    private val inList = hasAnyAncestor(isPopup())
    private val japanese = PackId("translate_ja")

    private fun showTool() {
        fakes.contentPacks.put(FakeContentPacks.installed(PackId("translate_pt")))
        fakes.contentPacks.put(FakeContentPacks.installed(PackId("translate_es")))
        fakes.contentPacks.put(FakeContentPacks.notInstalled(japanese))
        fakes.permissions.statuses[AppPermission.RECORD_AUDIO] = PermissionStatus.GRANTED
        show { ToolScreen(toolId = "translator", onOpenSettings = {}) }
        waitFor(hasText("Type or speak"))
    }

    private fun waitFor(matcher: SemanticsMatcher) {
        waitUntil { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }

    /** The view models' work starts on the main looper, some after a DataStore read on another thread. */
    private fun waitUntil(condition: () -> Boolean) {
        compose.waitUntil(WAIT_MILLIS) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    /** From Type or speak's From list. */
    private fun openPage() {
        compose.onNodeWithContentDescription("From").performClick()
        waitFor(hasText(MANAGE) and inList)
        compose.onNode(hasText(MANAGE) and inList).performClick()
        waitFor(hasText(DOWNLOADED) and inDialog)
    }

    private fun search(text: String) {
        compose.onAllNodes(hasSetTextAction() and inDialog).onFirst().performTextReplacement(text)
        compose.waitForIdle()
    }

    /** Opens a language's card by its header. */
    private fun open(language: String) {
        // The card's header, not the search field holding the same word.
        compose.onNode(hasText(language) and hasClickAction() and !hasSetTextAction() and inDialog).performClick()
        compose.waitForIdle()
    }

    private fun scrollTo(matcher: SemanticsMatcher) {
        compose.onNode(hasScrollToNodeAction() and inDialog).performScrollToNode(matcher)
        compose.waitForIdle()
    }

    private fun menu(
        language: String,
        part: String,
    ) {
        val label = "More actions for $language, $part"
        scrollTo(hasContentDescription(label))
        compose.onNode(hasContentDescription(label) and inDialog).performClick()
        compose.waitForIdle()
    }

    /**
     * Spec 082 FR-013 (the owner's phone, 2026-11-22: "the screen seems to flash when going to Gerenciar Idiomas"): the
     * page was a dialog window that dimmed the screen behind it, stayed transparent until its first frame and grew in
     * with the dialog animation (`fl=DIM_BEHIND fmt=TRANSPARENT wanim=...` on the emulator). It opens as a page: no
     * dim, no window animation, its own opaque background from the start.
     */
    @Test
    fun thePageOpensWithoutDimmingOrAnimatingItsWindow() {
        showTool()
        openPage()
        val window = ShadowDialog.getLatestDialog().window!!
        assertEquals(0, window.attributes.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND, "nothing dimmed behind")
        assertEquals(0, window.attributes.windowAnimations, "no dialog animation")
        val background = window.decorView.background as? ColorDrawable
        assertEquals(OPAQUE, background?.alpha, "an opaque background before the first frame")
    }

    @Test
    fun bothListsEndWithManageLanguagesWhichOpensThePageWithItsHelp() {
        showTool()
        compose.onNodeWithContentDescription("To").performClick()
        waitFor(hasText(MANAGE) and inList)
        compose.onNode(hasText(MANAGE) and inList).performClick()
        waitFor(hasText(TITLE) and inDialog)
        compose.onNode(hasSetTextAction() and inDialog).assertExists()
        compose.onNode(hasContentDescription("How to use") and inDialog).performClick()
        waitFor(hasText("Enable language", substring = true))
        // The marks themselves, each beside its meaning (owner, 2026-11-20).
        waitFor(hasText("Partly installed, not enabled"))
    }

    @Test
    fun downloadedLanguagesComeFirstWithTheirMarkAndOpenOnTheirParts() {
        showTool()
        openPage()
        waitFor(hasContentDescription("Installed and enabled") and inDialog)
        open("English")
        waitFor(hasText("Built into the app") and inDialog)
        compose.onNode(hasText("Enable language") and inDialog).assertIsOn()
        scrollTo(hasText(OTHERS))
        compose.onNode(hasText(OTHERS) and inDialog).assertExists()
    }

    /** Owner, 2026-11-20: downloading enables the language, and it stays where it was until the page opens again. */
    @Test
    fun aPackDownloadedHereEnablesItsLanguageWhichKeepsItsPlace() {
        showTool()
        openPage()
        search("jap")
        open("Japanese")
        waitFor(hasText("48 MB to download") and inDialog)
        menu("Japanese", "Translation")
        compose.onAllNodesWithText("Download").onLast().performClick()
        waitUntil { fakes.contentPacks.downloads.isNotEmpty() }
        assertEquals(
            japanese,
            fakes.contentPacks.downloads
                .single()
                .first,
        )
        waitUntil {
            compose.onAllNodes(hasText("Enable language") and isToggleable() and inDialog).fetchSemanticsNodes().any {
                it.config.getOrNull(SemanticsProperties.ToggleableState) == ToggleableState.On
            }
        }
        compose.onNode(hasText(OTHERS) and inDialog).assertExists()
        compose.onNode(hasText(DOWNLOADED) and inDialog).assertDoesNotExist()
    }

    @Test
    fun deletingAPackAsksFirst() {
        showTool()
        openPage()
        search("portu")
        open("Portuguese")
        menu("Portuguese", "Translation")
        compose.onAllNodesWithText("Delete").onLast().performClick()
        waitFor(hasText("Delete Portuguese?"))
        assertTrue(fakes.contentPacks.removed.isEmpty(), "nothing deleted before the confirmation")
        compose.onAllNodesWithText("Delete").onLast().performClick()
        waitUntil { fakes.contentPacks.removed.isNotEmpty() }
        assertEquals(listOf(PackId("translate_pt")), fakes.contentPacks.removed)
    }

    @Test
    fun speechThePhoneCanGetIsAskedForHereAndMarksTheLanguagePartial() {
        fakes.speechInput.support["es-ES"] = RecognitionSupport.CAN_DOWNLOAD
        showTool()
        openPage()
        search("spanish")
        waitFor(hasContentDescription("Partly installed, enabled") and inDialog)
        open("Spanish")
        waitFor(hasText("The phone can download it") and inDialog)
        menu("Spanish", "Speech recognition")
        compose.onAllNodesWithText("Get this language").onLast().performClick()
        waitUntil { fakes.speechInput.requested.isNotEmpty() }
        assertEquals(listOf("es-ES"), fakes.speechInput.requested)
        waitFor(hasText("The phone is downloading it") and inDialog)
    }

    @Test
    fun aMissingVoiceOpensTheVoiceApp() {
        fakes.speechOutput.voices["es-ES"] = VoiceSupport.NO_OFFLINE_VOICE
        showTool()
        openPage()
        search("spanish")
        open("Spanish")
        waitFor(hasText("No offline voice") and inDialog)
        menu("Spanish", "Voice")
        compose.onAllNodesWithText("Get a voice").onLast().performClick()
        compose.waitForIdle()
        assertEquals(1, fakes.speechOutput.voiceSettingsOpened)
    }

    @Test
    fun withTwoLeftTheyAreLockedAndThePageSaysWhy() {
        showTool()
        openPage()
        search("Spanish")
        open("Spanish")
        compose.onNode(hasText("Enable language") and isToggleable() and inDialog).performClick()
        waitFor(hasText(AT_LEAST_TWO))
        waitFor(hasContentDescription("Installed, not enabled") and inDialog)
        search("Portuguese")
        open("Portuguese")
        compose.onNode(hasText("Enable language") and isToggleable() and inDialog).assertIsNotEnabled()
    }

    @Test
    fun theFarHalfsListHasTheEntryToo() {
        showTool()
        compose.onNodeWithText("Conversation").performClick()
        waitFor(hasContentDescription("The other person's language"))
        compose.onNodeWithContentDescription("The other person's language").performClick()
        waitFor(hasText(MANAGE) and inList)
    }

    /** A Conversation notice's "Manage languages" opens the page at its language, open (owner, 2026-11-20). */
    @Test
    fun aSpeechNoticeOpensThePageAtItsLanguage() {
        fakes.speechInput.support["es-ES"] = RecognitionSupport.UNSUPPORTED
        showTool()
        compose.onNodeWithText("Conversation").performClick()
        waitFor(hasText("This phone can't recognize Spanish offline."))
        compose.onNodeWithText("Manage languages").performClick()
        waitFor(hasText(TITLE) and inDialog)
        waitFor(hasText("Not on this phone") and inDialog)
    }

    @Test
    fun aSearchWithoutAMatchSaysSo() {
        showTool()
        openPage()
        search("zzzz")
        waitFor(hasText("No language matches"))
        assertTrue(compose.onAllNodesWithText("Japanese").fetchSemanticsNodes().isEmpty())
    }

    /**
     * Spec 082 US3: where the device and the app both recognize a language, its part offers "Device" or "App", inside
     * the part's own card, lined up with its title (owner, 2026-11-22: "It should be inside them").
     */
    @Test
    fun bothCanRecognizeSoTheCardOffersDeviceOrAppAndTheAppsDownload() {
        fakes.contentPacks.put(FakeContentPacks.notInstalled(SpeechCatalog.recognitionPack))
        showTool()
        openPage()
        search("portu")
        open("Portuguese")
        val choice = hasAnyAncestor(hasContentDescription("Speech recognition for Portuguese: device or app"))
        waitFor(hasText("Device") and choice and inDialog)
        // The whole choice, not a segment's word (which sits inside the segment's own padding); the part's row is one
        // node (Material merges a list item), whose text starts 16 dp in from its edge. Under the row, as before, the
        // choice started at the edge itself.
        val row =
            compose
                .onNode(hasContentDescription("Speech recognition for Portuguese: device or app") and inDialog)
                .getUnclippedBoundsInRoot()
        val part =
            compose.onNode(hasText("Speech recognition", substring = true) and inDialog).getUnclippedBoundsInRoot()
        assertTrue(
            abs((row.left - part.left).value - ROW_TEXT_INSET) < 2f && row.top >= part.bottom,
            "the choice is inside its part's card, under its text and lined up with it: " +
                "${row.left} in a part at ${part.left}",
        )
        compose.onNode(hasText("App") and choice and inDialog).performClick()
        waitFor(hasText("The app can recognize it · 48 MB to download") and inDialog)
        menu("Portuguese", "Speech recognition")
        compose.onAllNodesWithText("Download").onLast().performClick()
        waitUntil { fakes.contentPacks.downloads.any { it.first == SpeechCatalog.recognitionPack } }
    }

    /**
     * Spec 082 story 5 (FR-012): a voice the device has can't be deleted by the app; its ⋮ opens the voice app's own
     * list, where it can (owner, 2026-11-22).
     */
    @Test
    fun aVoiceTheDeviceHasIsManagedInItsVoiceApp() {
        showTool()
        openPage()
        search("spanish")
        open("Spanish")
        menu("Spanish", "Voice")
        compose.onAllNodesWithText("Manage voices on the device").onLast().performClick()
        compose.waitForIdle()
        assertEquals(1, fakes.speechOutput.voiceSettingsOpened)
    }

    /** Spec 082 US4: the app's recognition is every language's, and the confirmation says so before it goes. */
    @Test
    fun theAppsRecognitionIsDeletedAfterAsking() {
        fakes.contentPacks.put(FakeContentPacks.installed(SpeechCatalog.recognitionPack))
        fakes.speechInput.support["es-ES"] = RecognitionSupport.UNSUPPORTED
        showTool()
        openPage()
        search("spanish")
        open("Spanish")
        menu("Spanish", "Speech recognition")
        compose.onAllNodesWithText("Delete").onLast().performClick()
        waitFor(hasText("Delete the app's speech recognition?"))
        assertTrue(fakes.contentPacks.removed.isEmpty(), "nothing deleted before the confirmation")
        compose.onAllNodesWithText("Delete").onLast().performClick()
        waitUntil { fakes.contentPacks.removed.isNotEmpty() }
        assertEquals(listOf(SpeechCatalog.recognitionPack), fakes.contentPacks.removed)
    }

    private companion object {
        const val MANAGE = "Manage languages…"
        const val TITLE = "Manage languages"
        const val DOWNLOADED = "Downloaded languages"
        const val OTHERS = "Other languages"
        const val AT_LEAST_TWO = "At least two languages stay in the lists."
        const val WAIT_MILLIS = 5_000L

        const val OPAQUE = 255

        /** How far a list item's text starts from its edge (Material's ListItem). */
        const val ROW_TEXT_INSET = 16f
    }
}
