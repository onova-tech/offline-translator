package tech.onova.offlinetranslator.screens

import android.os.Looper
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import tech.onova.offlinetranslator.core.domain.permissions.AppPermission
import tech.onova.offlinetranslator.core.domain.permissions.PermissionStatus
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.SpeechEvent
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport
import tech.onova.offlinetranslator.support.AppUiTest
import tech.onova.offlinetranslator.support.FakeContentPacks
import tech.onova.offlinetranslator.tool.ToolScreen
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Spec 012 story 5 (T053): the Conversation's two halves, their microphones, and the actions at the bottom edge. */
class ConversationScreenTest : AppUiTest() {
    private val packPt = PackId("translate_pt")
    private val packEs = PackId("translate_es")

    /** English here is the near half's language, so Spanish faces the other person. */
    private fun showConversation() {
        fakes.contentPacks.put(FakeContentPacks.installed(packPt))
        fakes.contentPacks.put(FakeContentPacks.installed(packEs))
        fakes.permissions.statuses[AppPermission.RECORD_AUDIO] = PermissionStatus.GRANTED
        show { ToolScreen(toolId = "translator", onOpenSettings = {}) }
        // The screen shows once its settings are read.
        waitForText("Conversation")
        compose.onNodeWithText("Conversation").performClick()
        waitForText("Speak English")
    }

    private fun waitForText(text: String) {
        compose.waitUntil(WAIT_MILLIS) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() ||
                compose
                    .onAllNodes(
                        androidx.compose.ui.test
                            .hasContentDescription(text),
                    ).fetchSemanticsNodes()
                    .isNotEmpty()
        }
    }

    @Test
    fun eachHalfHasItsLanguageAndItsMicrophoneTheFarOneOnTop() {
        showConversation()
        val far = compose.onNodeWithContentDescription("Speak Spanish").getUnclippedBoundsInRoot()
        val near = compose.onNodeWithContentDescription("Speak English").getUnclippedBoundsInRoot()
        assertTrue(far.bottom <= near.top, "the far half above the near one: $far $near")
        compose.onNodeWithContentDescription("The other person's language").assertExists()
        compose.onNodeWithContentDescription("Your language").assertExists()
        assertTrue(fakes.screenAwake.keptOn, "the screen stays on (FR-028)")
    }

    /**
     * Owner, 2026-11-19: what the phone's owner sets up shows upright in the near half, one thing at a time, for both
     * languages; a language the phone got moves the notice on without leaving the tool.
     */
    @Test
    fun whatToSetUpShowsUprightInTheNearHalfOneThingAtATime() {
        fakes.speechInput.support["es-ES"] = RecognitionSupport.CAN_DOWNLOAD
        fakes.speechOutput.voices["en-US"] = VoiceSupport.NO_OFFLINE_VOICE
        showConversation()
        val getSpanish = "Recognizing Spanish isn't on this phone yet. The phone can download it."
        waitForText(getSpanish)
        val far = compose.onNodeWithContentDescription("Speak Spanish").getUnclippedBoundsInRoot()
        val near = compose.onNodeWithContentDescription("Speak English").getUnclippedBoundsInRoot()
        val notice = compose.onNodeWithText(getSpanish).getUnclippedBoundsInRoot()
        assertTrue(far.bottom <= notice.top && notice.bottom <= near.top, "in the near half: $far $notice $near")
        compose.onNodeWithText(NO_ENGLISH_VOICE).assertDoesNotExist()
        // Got on the Manage languages page, which the notice opens (owner, 2026-11-20).
        compose.onNodeWithText("Manage languages").performClick()
        waitForText("The phone can download it")
        compose.onNodeWithContentDescription("More actions for Spanish, Speech recognition").performClick()
        compose.onNodeWithText("Get this language").performClick()
        assertEquals(listOf("es-ES"), fakes.speechInput.requested)
        compose.onNode(hasContentDescription("Back") and hasAnyAncestor(isDialog())).performClick()
        compose.waitForIdle()

        fakes.speechInput.support["es-ES"] = RecognitionSupport.READY
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(RECHECK_MILLIS))
        waitForText(NO_ENGLISH_VOICE)
        compose.onNodeWithText(getSpanish).assertDoesNotExist()
        compose.onNodeWithContentDescription("Speak Spanish").assertIsEnabled()
    }

    /** Owner, 2026-11-19: "Listening." while nothing is heard yet, and a square button that drops the turn. */
    @Test
    fun theSquareButtonStopsListeningAndDropsOnlyWhatWasBeingHeard() {
        showConversation()
        compose.onNodeWithContentDescription(DISCARD).assertDoesNotExist()
        compose.onNodeWithContentDescription("Speak English").performClick()
        compose.waitUntil(WAIT_MILLIS) { fakes.speechInput.listenedTo.isNotEmpty() }
        waitForText(DISCARD)
        // Screen readers hear the waiting word once, not each of its dots.
        compose.onNodeWithContentDescription("Listening").assertExists()
        fakes.speechInput.hear(SpeechEvent.Partial("Good mor"))
        waitForText("Good mor")
        compose.onNodeWithContentDescription(DISCARD).performClick()
        compose.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
        compose.onNodeWithContentDescription(DISCARD).assertDoesNotExist()
        compose.onNodeWithText("Good mor").assertDoesNotExist()
        assertTrue(fakes.translationEngine.requests.isEmpty(), "nothing is translated")
    }

    /** Owner, 2026-11-19: the far half's language list opens in its own window, turned like the half. */
    @Test
    fun theFarHalfsLanguageListIsTurnedToThePersonAcrossAndTheNearOneIsUpright() {
        showConversation()
        val inList = hasAnyAncestor(isPopup()) and hasClickAction()
        compose.onNodeWithContentDescription("The other person's language").performClick()
        compose.waitForIdle()
        val far = compose.onAllNodes(inList).fetchSemanticsNodes().map { it.boundsInRoot }
        // Three languages, then "Manage languages…" (spec 080).
        assertEquals(4, far.size)
        assertTrue(far.first().top > far.last().top, "turned: the first language is lowest on the screen: $far")
        compose.onNode(inList and hasText("Portuguese")).performClick()
        waitForText("Speak Portuguese")
        compose.onNodeWithContentDescription("Your language").performClick()
        compose.waitForIdle()
        val near = compose.onAllNodes(inList).fetchSemanticsNodes().map { it.boundsInRoot }
        assertTrue(near.first().top < near.last().top, "upright: $near")
    }

    @Test
    fun theActionsAreAtTheBottomEdgeBelowBothHalvesAndNoneIsFilled() {
        showConversation()
        val near = compose.onNodeWithContentDescription("Speak English").getUnclippedBoundsInRoot()
        for (action in listOf("Copy", "Share", "Clear")) {
            val bounds = compose.onNodeWithText(action).assertIsNotEnabled().getUnclippedBoundsInRoot()
            assertTrue(near.bottom <= bounds.top, "$action below the halves")
        }
        compose.onNodeWithText("More options").assertExists()
    }

    @Test
    fun aSpokenTurnIsTranslatedOnTheOtherHalfAndReadThere() {
        showConversation()
        compose.onNodeWithContentDescription("Speak English").performClick()
        compose.waitUntil(WAIT_MILLIS) { fakes.speechInput.listenedTo.isNotEmpty() }
        assertEquals(listOf("en-US"), fakes.speechInput.listenedTo)
        // While one half listens, the other microphone waits.
        compose.onNodeWithContentDescription("Speak Spanish").assertIsNotEnabled()
        fakes.speechInput.hear(SpeechEvent.Final("Good morning"))
        waitForText("[es] Good morning")
        compose.onNodeWithText("Good morning").assertExists()
        compose.waitUntil(WAIT_MILLIS) { fakes.speechOutput.said.isNotEmpty() }
        assertEquals(listOf("[es] Good morning" to "es-ES"), fakes.speechOutput.said)
        compose.onNodeWithContentDescription("Speak English").assertIsNotEnabled()
        fakes.speechOutput.done()
        waitForText("Copy")
        compose.waitUntil(WAIT_MILLIS) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodesWithText("Copy").fetchSemanticsNodes().any {
                !it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)
            }
        }
        compose.onNodeWithText("Copy").assertIsEnabled().performClick()
        assertEquals(listOf("English: Good morning\nSpanish: [es] Good morning"), fakes.clipboard.copied)
    }

    /**
     * The owner's phone, 2026-11-20: in a short half the notice's text and × squeezed its button to a sliver. The ×
     * is in the buttons' row now, and a notice taller than its room scrolls: its button keeps its height.
     */
    @Test
    @Config(qualifiers = "w360dp-h560dp")
    fun aNoticesButtonKeepsItsHeightInAShortWindow() {
        fakes.speechInput.support["es-ES"] = RecognitionSupport.CAN_DOWNLOAD
        showConversation()
        waitForText("Manage languages")
        val button = compose.onNodeWithText("Manage languages").getUnclippedBoundsInRoot()
        assertTrue(button.bottom - button.top >= 36.dp, "the button keeps its height: $button")
        val close = compose.onNodeWithText("Ignore").getUnclippedBoundsInRoot()
        assertTrue(close.right <= button.left, "Ignore is beside the button, never over it: $close $button")
    }

    /** Owner, 2026-11-20: a notice nothing can fix closes with a gray "Ignore", and the next one shows. */
    @Test
    fun aSpeechNoticeClosesWithIgnore() {
        fakes.speechInput.support["es-ES"] = RecognitionSupport.UNSUPPORTED
        fakes.speechOutput.voices["en-US"] = VoiceSupport.NO_OFFLINE_VOICE
        showConversation()
        waitForText(NO_ENGLISH_VOICE)
        compose.onNodeWithText("Ignore").performClick()
        waitForText("This phone can't recognize Spanish offline.")
        compose.onNodeWithText(NO_ENGLISH_VOICE).assertDoesNotExist()
        compose.onNodeWithText("Ignore").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Ignore").assertDoesNotExist()
    }

    @Test
    fun clearAsksFirst() {
        showConversation()
        compose.onNodeWithContentDescription("Speak English").performClick()
        compose.waitUntil(WAIT_MILLIS) { fakes.speechInput.listenedTo.isNotEmpty() }
        fakes.speechInput.hear(SpeechEvent.Final("Good morning"))
        waitForText("[es] Good morning")
        compose.waitUntil(WAIT_MILLIS) { fakes.speechOutput.said.isNotEmpty() }
        fakes.speechOutput.done()
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
        compose.onNodeWithText("Clear").performClick()
        compose.onNodeWithText("Clear the conversation?").assertExists()
        // The confirmation's own Clear, over the screen's.
        compose.onAllNodesWithText("Clear").onLast().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("[es] Good morning").assertDoesNotExist()
    }

    private companion object {
        const val DISCARD = "Stop and discard"
        const val NO_ENGLISH_VOICE = "There's no offline voice for English. The phone's voice app can download one."

        /** A little over the time the phone is asked again while a language is on its way. */
        const val RECHECK_MILLIS = 3_100L
        const val WAIT_MILLIS = 5_000L
    }
}
