package tech.onova.offlinetranslator.screens

import android.os.Looper
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import tech.onova.offlinetranslator.core.domain.permissions.AppPermission
import tech.onova.offlinetranslator.core.domain.permissions.PermissionStatus
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.SpeechEvent
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport
import tech.onova.offlinetranslator.feature.settings.SettingsScreen
import tech.onova.offlinetranslator.feature.toolstranslate.presentation.OfflineLanguagesSection
import tech.onova.offlinetranslator.support.AppUiTest
import tech.onova.offlinetranslator.support.FakeContentPacks
import tech.onova.offlinetranslator.tool.ToolScreen
import java.time.Duration

/**
 * Spec 012 T061: the Translator's screens in light, dark and 200% text: Type or speak empty and translated, the pack
 * offer downloading, a conversation with turns, and Settings → Offline languages.
 */
class TranslatorScreenshotTest : AppUiTest() {
    private fun showTool(darkTheme: Boolean = false) {
        show(darkTheme = darkTheme) { ToolScreen(toolId = "translator", onOpenSettings = {}) }
        waitFor("Type or speak")
    }

    private fun waitFor(text: String) {
        compose.waitUntil(WAIT_MILLIS) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodes(hasContentDescription(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun translate(text: String) {
        compose.onNodeWithText("Text").performTextInput(text)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(PAUSE_MILLIS))
        waitFor("[es] $text")
    }

    /** Two turns: one said on each half. */
    private fun converse() {
        fakes.permissions.statuses[AppPermission.RECORD_AUDIO] = PermissionStatus.GRANTED
        compose.onNodeWithText("Conversation").performClick()
        waitFor("Speak English")
        say("Speak English", "Where is the train station?", "[es] Where is the train station?")
        say("Speak Spanish", "Está a dos calles de aquí.", "[en] Está a dos calles de aquí.")
    }

    private fun say(
        microphone: String,
        words: String,
        translation: String,
    ) {
        compose.onNodeWithContentDescription(microphone).performClick()
        compose.waitUntil(WAIT_MILLIS) { fakes.speechInput.listening > 0 }
        fakes.speechInput.hear(SpeechEvent.Final(words))
        waitFor(translation)
        compose.waitUntil(WAIT_MILLIS) { fakes.speechOutput.speaking }
        fakes.speechOutput.done()
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
    }

    @Test
    fun typeOrSpeakEmpty() {
        showTool()
        capture("translator_empty_light")
    }

    @Test
    fun typeOrSpeakTranslated() {
        showTool()
        translate("Good morning! Where is the nearest train station?")
        capture("translator_translated_light")
    }

    @Test
    fun typeOrSpeakTranslatedDark() {
        showTool(darkTheme = true)
        translate("Good morning! Where is the nearest train station?")
        capture("translator_translated_dark")
    }

    @Test
    fun typeOrSpeakTranslatedLargeText() {
        RuntimeEnvironment.setFontScale(2f)
        showTool()
        translate("Good morning")
        capture("translator_translated_font200")
    }

    @Test
    fun thePackOfferDownloading() {
        fakes.contentPacks.put(FakeContentPacks.notInstalled(PackId("translate_es")))
        showTool()
        waitFor("Download")
        compose.onNodeWithText("Download").performClick()
        waitFor("Cancel")
        fakes.contentPacks.progress(PackId("translate_es"), bytes = 23_000_000)
        waitFor("Downloading Spanish: 23 of 48 MB")
        capture("translator_pack_downloading_light")
    }

    @Test
    fun aConversation() {
        showTool()
        converse()
        capture("translator_conversation_light")
    }

    @Test
    fun aConversationDark() {
        showTool(darkTheme = true)
        converse()
        capture("translator_conversation_dark")
    }

    @Test
    fun aConversationLargeText() {
        RuntimeEnvironment.setFontScale(2f)
        showTool()
        converse()
        capture("translator_conversation_font200")
    }

    @Test
    fun offlineLanguages() {
        fakes.contentPacks.put(FakeContentPacks.notInstalled(PackId("translate_es")))
        show { SettingsScreen(onBack = {}, offlineLanguages = { OfflineLanguagesSection(it) }) }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Manage languages"))
        compose.waitForIdle()
        capture("translator_offline_languages_light")
    }

    @Test
    fun offlineLanguagesLargeTextDark() {
        RuntimeEnvironment.setFontScale(2f)
        show(darkTheme = true) { SettingsScreen(onBack = {}, offlineLanguages = { OfflineLanguagesSection(it) }) }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Manage languages"))
        compose.waitForIdle()
        capture("translator_offline_languages_dark_font200")
    }

    /** Spec 080: From's list, which ends with "Manage languages…", and the page it opens. */
    private fun openLanguages(search: String? = null) {
        compose.onNodeWithContentDescription("From").performClick()
        waitFor(OTHER)
        compose.onNode(hasText(OTHER) and hasAnyAncestor(isPopup())).performClick()
        waitFor("Downloaded languages")
        search?.let { compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextReplacement(it) }
        compose.waitForIdle()
    }

    /** A popup or a dialog is a window of its own: the last root. */
    private fun captureTop(name: String) {
        compose.onAllNodes(isRoot()).onLast().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun aLanguageListEndsWithOtherLanguages() {
        showTool()
        compose.onNodeWithContentDescription("From").performClick()
        waitFor(OTHER)
        captureTop("translator_from_list_light")
    }

    /** Owner, 2026-11-20: Manage languages' help, with its marks drawn beside their meanings. */
    @Test
    fun theManageLanguagesHelp() {
        showTool()
        openLanguages()
        compose.onNode(hasContentDescription("How to use") and hasAnyAncestor(isDialog())).performClick()
        waitFor("The mark beside each downloaded language:")
        captureTop("translator_manage_help_light")
    }

    /** Owner, 2026-11-20: a speech notice with "Manage languages" and a gray "Ignore", which covers no button. */
    @Test
    fun aConversationNotice() {
        fakes.speechInput.support["es-ES"] = RecognitionSupport.CAN_DOWNLOAD
        fakes.permissions.statuses[AppPermission.RECORD_AUDIO] = PermissionStatus.GRANTED
        showTool()
        compose.onNodeWithText("Conversation").performClick()
        waitFor("Manage languages")
        capture("translator_conversation_notice_light")
    }

    /** Owner, 2026-11-20: "Your languages", each a card with its check, pack, speech and voice. */
    @Test
    fun theManageLanguagesPage() {
        fakes.speechOutput.voices["es-ES"] = VoiceSupport.NO_OFFLINE_VOICE
        showTool()
        openLanguages()
        // Spanish opened on its parts; English and Portuguese closed, with their marks.
        compose.onNode(hasText("Spanish") and hasClickAction() and hasAnyAncestor(isDialog())).performClick()
        waitFor("No offline voice")
        captureTop("translator_manage_languages_light")
    }

    @Test
    fun theLanguagePageWithASearch() {
        showTool()
        openLanguages(search = "ch")
        captureTop("translator_languages_search_light")
    }

    @Test
    fun theLanguagePageWithTwoLockedLargeTextDark() {
        RuntimeEnvironment.setFontScale(2f)
        showTool(darkTheme = true)
        openLanguages(search = "spanish")
        compose.onNode(hasText("Spanish") and hasClickAction() and hasAnyAncestor(isDialog())).performClick()
        compose.onNode(hasText("Enable language") and isToggleable() and hasAnyAncestor(isDialog())).performClick()
        waitFor("At least two languages stay in the lists.")
        // English among others, checked and locked.
        compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextReplacement("en")
        compose.waitForIdle()
        captureTop("translator_languages_two_dark_font200")
    }

    private companion object {
        const val OTHER = "Manage languages…"
        const val PAUSE_MILLIS = 600L
        const val WAIT_MILLIS = 5_000L
    }
}
