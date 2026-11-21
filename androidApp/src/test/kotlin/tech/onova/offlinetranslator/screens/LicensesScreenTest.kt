package tech.onova.offlinetranslator.screens

import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import tech.onova.offlinetranslator.R
import tech.onova.offlinetranslator.feature.settings.licenses.LicensesScreen
import tech.onova.offlinetranslator.support.AppUiTest
import kotlin.test.assertTrue

/** Spec 012 T056: Settings → About → Open-source licenses, from the list the build collected. */
class LicensesScreenTest : AppUiTest() {
    private val list: String by lazy {
        RuntimeEnvironment
            .getApplication()
            .resources
            .openRawResource(R.raw.aboutlibraries)
            .bufferedReader()
            .use { it.readText() }
    }

    private fun showLicenses() {
        show { LicensesScreen(onBack = {}, readList = { list }) }
        compose.waitUntil(WAIT_MILLIS) {
            compose
                .onAllNodesWithText(
                    "PolyTools is built with these open-source projects. Thank you.",
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun scrollTo(text: String) {
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
        compose.waitForIdle()
    }

    @Test
    fun theIconsTheEngineAndTheModelsAreCredited() {
        showLicenses()
        for (project in listOf(
            "Lucide",
            "Feather",
            "Bergamot translator",
            "Translation models (53 languages with English)",
        )) {
            scrollTo(project)
            compose.onNodeWithText(project).assertExists()
        }
    }

    @Test
    fun aTapShowsTheFullLicenseAndTheSourceLink() {
        showLicenses()
        scrollTo("Lucide")
        compose.onNodeWithText("Lucide").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Source code").assertExists()
        compose.onNodeWithText("Copyright (c) 2026 Lucide Icons and Contributors", substring = true).assertExists()
    }

    @Test
    fun everyShippedArtifactHasALicenseWithItsText() {
        val root = Json.parseToJsonElement(list).jsonObject
        val licenses = root.getValue("licenses").jsonObject
        val libraries = root.getValue("libraries").jsonArray.map { it.jsonObject }
        assertTrue(libraries.size > MANY, "the build collected the dependencies: ${libraries.size}")
        val without =
            libraries.filter { library ->
                val keys = (library["licenses"] as? JsonArray).orEmpty().map { (it as JsonPrimitive).content }
                keys.isEmpty() || keys.any { key -> (licenses[key] as? JsonObject)?.get("name") == null }
            }
        assertTrue(without.isEmpty(), "without a license: " + without.map { it["uniqueId"] })
    }

    private companion object {
        const val WAIT_MILLIS = 5_000L

        /** About 260 artifacts ship (spec 012 research R4); far fewer means the collection failed. */
        const val MANY = 100
    }
}
