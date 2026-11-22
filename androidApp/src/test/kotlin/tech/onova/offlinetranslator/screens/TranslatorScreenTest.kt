package tech.onova.offlinetranslator.screens

import android.os.Looper
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import tech.onova.offlinetranslator.core.domain.permissions.PermissionStatus
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.PackState
import tech.onova.offlinetranslator.core.domain.ports.PackStatus
import tech.onova.offlinetranslator.core.domain.ports.PackWait
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.SettingsPage
import tech.onova.offlinetranslator.core.domain.ports.SpeechEvent
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport
import tech.onova.offlinetranslator.support.AppUiTest
import tech.onova.offlinetranslator.support.FakeContentPacks
import tech.onova.offlinetranslator.tool.ToolScreen
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Spec 012 story 1 (T035, T038): Type or speak, with the fake engine ("[es] text") and both packs installed. */
class TranslatorScreenTest : AppUiTest() {
    private val packPt = PackId("translate_pt")
    private val packEs = PackId("translate_es")

    private fun showTool() {
        fakes.contentPacks.put(FakeContentPacks.installed(packPt))
        fakes.contentPacks.put(FakeContentPacks.installed(packEs))
        show { ToolScreen(toolId = "translator", onOpenSettings = {}) }
        waitForText("Type or speak")
    }

    /** The view model's pause before translating waits on the Android main looper, not on Compose's clock. */
    private fun typeAndPause(text: String) {
        // The screen shows once its settings are read.
        waitForText("Text")
        compose.onNodeWithText("Text").performTextInput(text)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(PAUSE_MILLIS))
        compose.waitForIdle()
    }

    /** What the view models start runs on the main looper, some of it after a read on another thread. */
    private fun waitForText(text: String) {
        compose.waitUntil(WAIT_MILLIS) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun stateIs(state: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state)

    @Test
    fun theTwoModesAreTheTopChoice() {
        showTool()
        compose.onNodeWithText("Type or speak").assertIsSelected()
        compose.onNodeWithText("Conversation").assertHasClickAction()
    }

    @Test
    fun theLanguagesAreTwoMenusWithTheSwapBetweenThem() {
        showTool()
        // The phone is in English here, so English is the first source; the target is then Spanish.
        val from = compose.onNodeWithContentDescription("From").assert(stateIs("English")).getUnclippedBoundsInRoot()
        val swap = compose.onNodeWithContentDescription("Swap languages").getUnclippedBoundsInRoot()
        val to = compose.onNodeWithContentDescription("To").assert(stateIs("Spanish")).getUnclippedBoundsInRoot()
        assertTrue(from.right <= swap.left && swap.right <= to.left, "From, swap, To: $from $swap $to")
        val field = compose.onNodeWithText("Text").getUnclippedBoundsInRoot()
        assertTrue(swap.bottom <= field.top, "the languages above the text")
    }

    @Test
    fun aTypedTextIsCountedAndTranslatedInTheMiddle() {
        showTool()
        compose.onNodeWithText("Type, paste or speak a text").assertExists()
        typeAndPause("Good morning")
        compose.onNodeWithText("12 / 5,000", substring = true).assertExists()
        waitForText("[es] Good morning")
        val translation = compose.onNodeWithText("[es] Good morning").getUnclippedBoundsInRoot()
        val languages = compose.onNodeWithContentDescription("From").getUnclippedBoundsInRoot()
        assertTrue(translation.bottom <= languages.top, "the translation above the controls")
    }

    @Test
    fun aLineShowsWhileANewerTranslationReplacesTheShownOne() {
        val working =
            SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate)
        showTool()
        typeAndPause("Good morning")
        waitForText("[es] Good morning")
        compose.onNode(working).assertDoesNotExist()
        val before = compose.onNodeWithText("[es] Good morning").getUnclippedBoundsInRoot()
        fakes.translationEngine.hold()
        typeAndPause(" all")
        // The shown translation stays where it was, with the line under it, until the newer one comes.
        compose.onNode(working).assertExists()
        assertEquals(before, compose.onNodeWithText("[es] Good morning").getUnclippedBoundsInRoot())
        fakes.translationEngine.release()
        waitForText("[es] Good morning all")
        compose.onNode(working).assertDoesNotExist()
    }

    /** Spec 080 FR-011: Arabic typed in the field is laid out right to left, from the right. */
    @Test
    fun arabicIsWrittenRightToLeft() {
        showTool()
        typeAndPause(ARABIC)
        val layouts = mutableListOf<TextLayoutResult>()
        compose
            .onNode(hasSetTextAction())
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(ResolvedTextDirection.Rtl, layouts.single().getParagraphDirection(0))
        // The translation and the turns take the same style (inItsOwnDirection). The fake engine's "[es] " is Latin,
        // so its line rightly reads left to right and can't show it here.
    }

    @Test
    fun withoutATranslationTheActionsWaitAndReadAloudIsLast() {
        showTool()
        compose.onNodeWithText("Copy").assertIsNotEnabled()
        compose.onNodeWithText("Share").assertIsNotEnabled()
        val share = compose.onNodeWithText("Share").getUnclippedBoundsInRoot()
        val read = compose.onNodeWithText("Read aloud").assertIsNotEnabled().getUnclippedBoundsInRoot()
        val copy = compose.onNodeWithText("Copy").getUnclippedBoundsInRoot()
        assertTrue(copy.right <= share.left && share.right <= read.left, "Copy, Share, then Read aloud")
    }

    @Test
    fun copyConfirmsAndShareSendsTheTranslation() {
        showTool()
        typeAndPause("Good morning")
        waitForText("[es] Good morning")
        compose.onNodeWithText("Copy").assertIsEnabled().performClick()
        compose.waitForIdle()
        assertEquals(listOf("[es] Good morning"), fakes.clipboard.copied)
        compose.onNodeWithText("Translation copied").assertExists()
        compose.onNodeWithText("Share").performClick()
        assertEquals("[es] Good morning", fakes.textSharer.shared)
    }

    @Test
    fun swapPutsTheTranslationInTheTextAndTheLanguagesTradePlaces() {
        showTool()
        typeAndPause("Good morning")
        waitForText("[es] Good morning")
        compose.onNodeWithContentDescription("Swap languages").performClick()
        // The languages are kept in the real DataStore, written on its own thread.
        compose.waitUntil(WAIT_MILLIS) {
            compose.onAllNodes(hasContentDescription("From") and stateIs("Spanish")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("To").assert(stateIs("English"))
        val typed = SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("[es] Good morning"))
        compose.onNodeWithText("Text").assert(typed)
    }

    @Test
    fun aFinishedTranslationIsAnnouncedOnceNotWhileTyping() {
        showTool()
        typeAndPause("Good morning")
        waitForText("[es] Good morning")
        val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        compose.onNodeWithText("[es] Good morning").assert(polite)
        // A key after it: the old translation stays, quiet, until the new one is done.
        compose.onNodeWithText("Text").performTextInput("!")
        compose.waitForIdle()
        val quiet = SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion)
        compose.onNodeWithText("[es] Good morning").assert(quiet)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(PAUSE_MILLIS))
        waitForText("[es] Good morning!")
        compose.onNodeWithText("[es] Good morning!").assert(polite)
    }

    /** Spanish not on the phone: English to Spanish, the first pair here, needs it. */
    private fun showWithoutSpanish() {
        fakes.contentPacks.put(FakeContentPacks.installed(packPt))
        fakes.contentPacks.put(FakeContentPacks.notInstalled(packEs))
        show { ToolScreen(toolId = "translator", onOpenSettings = {}) }
        waitForText("To translate Spanish, download its language pack (48 MB).")
    }

    @Test
    fun theOfferShowsTheSizeAndOnlyDownloadDownloads() {
        showWithoutSpanish()
        assertTrue(fakes.contentPacks.downloads.isEmpty(), "nothing before Download (FR-010)")
        compose.onNodeWithText("Download").performClick()
        waitForText("Downloading Spanish: 0 of 48 MB")
        assertEquals(listOf(packEs to true), fakes.contentPacks.downloads)
        compose.onNodeWithText("Cancel").performClick()
        waitForText("To translate Spanish, download its language pack (48 MB).")
        assertEquals(listOf(packEs), fakes.contentPacks.cancelled)
    }

    @Test
    fun notNowPutsTheOfferAway() {
        showWithoutSpanish()
        compose.onNodeWithText("Not now").performClick()
        compose.waitForIdle()
        compose
            .onNodeWithText("Spanish isn't on this phone. Choose another language, or download it in Settings.")
            .assertExists()
        compose.onNodeWithText("Download").assertDoesNotExist()
    }

    @Test
    fun waitingForWifiOpensTheNetworkSettings() {
        showWithoutSpanish()
        fakes.contentPacks.put(
            PackStatus(packEs, FakeContentPacks.PACK_BYTES, null, PackState.Waiting(PackWait.FOR_WIFI)),
        )
        waitForText("Network settings")
        compose.onNodeWithText("Network settings").performClick()
        assertEquals(listOf(SettingsPage.NETWORK), fakes.settingsPages.opened)
    }

    @Test
    fun theMicrophoneIsAskedForOnlyWhenTappedThenWhatIsHeardIsTranslated() {
        showTool()
        compose.onNodeWithText(MICROPHONE_RATIONALE).assertDoesNotExist()
        compose.onNodeWithContentDescription("Speak").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(MICROPHONE_RATIONALE).assertExists()
        assertTrue(fakes.speechInput.listenedTo.isEmpty(), "nothing listens before the permission")
        fakes.permissions.answers += PermissionStatus.GRANTED
        compose.onNodeWithText("Allow").performClick()
        compose.waitUntil(WAIT_MILLIS) {
            shadowOf(Looper.getMainLooper()).idle()
            fakes.speechInput.listenedTo.isNotEmpty()
        }
        assertEquals(listOf("en-US"), fakes.speechInput.listenedTo)
        compose.onNodeWithContentDescription("Stop listening").assertExists()
        fakes.speechInput.hear(SpeechEvent.Final("Good morning"))
        compose.waitForIdle()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(PAUSE_MILLIS))
        waitForText("[es] Good morning")
        compose.onNodeWithContentDescription("Speak").assertExists()
    }

    @Test
    fun readAloudSpeaksTheTranslationAndReadsStopWhileItSpeaks() {
        showTool()
        typeAndPause("Good morning")
        waitForText("[es] Good morning")
        compose.onNodeWithText("Read aloud").assertIsEnabled().performClick()
        waitForText("Stop")
        assertEquals(listOf("[es] Good morning" to "es-ES"), fakes.speechOutput.said)
        compose.onNodeWithText("Stop").performClick()
        waitForText("Read aloud")
        assertEquals(1, fakes.speechOutput.stops)
    }

    @Test
    fun withoutAnOfflineVoiceReadAloudLeadsToManageLanguages() {
        fakes.speechOutput.voices["es-ES"] = VoiceSupport.NO_OFFLINE_VOICE
        showTool()
        typeAndPause("Good morning")
        waitForText("[es] Good morning")
        compose.onNodeWithText("Read aloud").performClick()
        waitForText("There's no offline voice for Spanish. The phone's voice app can download one.")
        compose.onNodeWithText("Manage languages").performClick()
        waitForText("No offline voice")
        assertTrue(fakes.speechOutput.said.isEmpty())
    }

    @Test
    fun beforeAndroid12ThereIsNoMicrophoneAndTypingStays() {
        fakes.speechInput.defaultSupport = RecognitionSupport.SYSTEM_TOO_OLD
        showTool()
        waitForText("Type or paste a text")
        compose.onNodeWithContentDescription("Speak").assertDoesNotExist()
    }

    @Test
    fun withoutAnOnDeviceRecognizerTheMicrophoneSaysWhyWithoutAskingForIt() {
        fakes.speechInput.defaultSupport = RecognitionSupport.NO_RECOGNIZER
        showTool()
        waitForText("Type, paste or speak a text")
        compose.onNodeWithContentDescription("Speak").performClick()
        waitForText("This phone has no offline speech recognition. You can still type.")
        compose.onNodeWithText(MICROPHONE_RATIONALE).assertDoesNotExist()
        assertTrue(fakes.speechInput.listenedTo.isEmpty())
    }

    private companion object {
        const val ARABIC = "أين محطة القطار؟"

        /** A little over the Translator's 500 ms pause (spec 012 FR-005). */
        const val PAUSE_MILLIS = 600L
        const val WAIT_MILLIS = 5_000L
        const val MICROPHONE_RATIONALE = "To write what you say, the Translator uses the microphone while it listens."
    }
}
