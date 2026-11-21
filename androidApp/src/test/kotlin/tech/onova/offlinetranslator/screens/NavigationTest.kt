package tech.onova.offlinetranslator.screens

import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Test
import tech.onova.offlinetranslator.navigation.AppNavHost
import tech.onova.offlinetranslator.support.AppUiTest

/** Spec 081: the app opens on the Translator; its gear opens Settings, which leads to the licenses and back. */
class NavigationTest : AppUiTest() {
    private fun waitForText(text: String) {
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun theAppOpensOnTheTranslatorWithoutABackArrow() {
        show { AppNavHost() }
        waitForText("Type or speak")
        compose.onNodeWithText("Translator").assertExists()
        compose.onNodeWithContentDescription("Back").assertDoesNotExist()
        compose.onNodeWithContentDescription("How to use").assertExists()
    }

    @Test
    fun theGearOpensSettingsAndBackReturns() {
        show { AppNavHost() }
        waitForText("Type or speak")
        compose.onNodeWithContentDescription("Settings").performClick()
        waitForText("App language")
        compose.onNodeWithContentDescription("Back").performClick()
        waitForText("Type or speak")
    }

    @Test
    fun settingsHasTheOfflineLanguagesAndTheLicenses() {
        show { AppNavHost() }
        waitForText("Type or speak")
        compose.onNodeWithContentDescription("Settings").performClick()
        waitForText("App language")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Manage languages"))
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Open-source licenses"))
        compose.onNodeWithText("Open-source licenses").performClick()
        waitForText("Thank you", substring = true)
    }

    private fun waitForText(
        text: String,
        substring: Boolean,
    ) {
        compose.waitUntil(WAIT_MILLIS) {
            compose.onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val WAIT_MILLIS = 5_000L
    }
}
