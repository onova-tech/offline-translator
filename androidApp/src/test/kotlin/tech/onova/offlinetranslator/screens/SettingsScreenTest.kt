package tech.onova.offlinetranslator.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Test
import tech.onova.offlinetranslator.core.domain.settings.AppLanguage
import tech.onova.offlinetranslator.core.domain.settings.ThemeMode
import tech.onova.offlinetranslator.feature.settings.SettingsScreen
import tech.onova.offlinetranslator.support.AppUiTest
import kotlin.test.assertEquals

/** Spec 081: Settings keeps Appearance, Language and About, with the code's address; nothing about ads. */
class SettingsScreenTest : AppUiTest() {
    @Test
    fun theThemeIsSaved() {
        show { SettingsScreen(onBack = {}) }
        compose.onNodeWithText("Dark").performClick()
        compose.waitForIdle()
        assertEquals(ThemeMode.DARK, appSettings.settings.value.theme)
    }

    @Test
    fun theLanguageApplies() {
        show { SettingsScreen(onBack = {}) }
        compose.onNodeWithText("App language").performClick()
        compose.onNodeWithText("Español").performClick()
        compose.waitForIdle()
        assertEquals(AppLanguage.ES, language.language)
    }

    @Test
    fun aboutHasTheVersionAndTheSourceCode() {
        val opened = mutableListOf<String>()
        val handler =
            object : UriHandler {
                override fun openUri(uri: String) {
                    opened += uri
                }
            }
        show { CompositionLocalProvider(LocalUriHandler provides handler) { SettingsScreen(onBack = {}) } }
        compose.onNodeWithText("Dynamic color").assertDoesNotExist()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Source code"))
        compose.onNodeWithText("1.0.0-test").assertExists()
        compose.onNodeWithText("Source code").performClick()
        assertEquals(listOf("https://github.com/onova-tech/offline-translator"), opened)
    }

    @Test
    fun nothingAboutAdsUnitsOrData() {
        show { SettingsScreen(onBack = {}) }
        listOf("Privacy", "Remove ads", "Units & formats", "Your data", "Export data").forEach {
            compose.onNodeWithText(it).assertDoesNotExist()
        }
    }
}
