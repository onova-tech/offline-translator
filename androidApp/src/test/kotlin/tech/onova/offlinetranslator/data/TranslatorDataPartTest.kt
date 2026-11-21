package tech.onova.offlinetranslator.data

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.qualifier.named
import tech.onova.offlinetranslator.core.data.backup.DataPart
import tech.onova.offlinetranslator.support.AppUiTest
import kotlin.test.assertTrue

/** Spec 012 T063 (FR-015): a data file carries the Translator's settings, never a language pack. */
class TranslatorDataPartTest : AppUiTest() {
    @Test
    fun aDataFileHoldsTheTranslatorsSettingsAndNoPack() =
        runBlocking {
            val part = GlobalContext.get().get<DataPart>(named("data-part:translator"))
            val document = part.export().document as JsonObject
            val settings =
                setOf(
                    "schemaVersion",
                    "source",
                    "target",
                    "mode",
                    "near",
                    "far",
                    "wifiOnly",
                    "farHalfTurned",
                    // Spec 080 FR-007: the checked languages travel with the settings.
                    "languages",
                    // Spec 082 FR-007: the phone's or the app's speech per language, a setting, never a pack.
                    "speech",
                )
            assertTrue(document.keys.isNotEmpty() && settings.containsAll(document.keys), "keys: ${document.keys}")
            val text = document.toString()
            assertTrue("translate_" !in text && "pack" !in text.lowercase(), text)
        }
}
