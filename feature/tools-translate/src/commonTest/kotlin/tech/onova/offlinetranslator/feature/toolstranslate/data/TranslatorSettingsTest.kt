package tech.onova.offlinetranslator.feature.toolstranslate.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import tech.onova.offlinetranslator.core.data.backup.PartKind
import tech.onova.offlinetranslator.core.data.datastore.JsonDataStoreFactory
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorDefaults
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorMode
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Spec 012 data-model.md "TranslatorSettings": what is kept, and the only thing a data file carries (FR-015). */
class TranslatorSettingsTest {
    private val en = LanguageCode("en")
    private val pt = LanguageCode("pt")
    private val es = LanguageCode("es")

    @Test
    fun theDefaultsFollowTheAppsLanguage() {
        val fresh = TranslatorSettings()
        assertEquals(TranslatorMode.TYPE_OR_SPEAK, fresh.mode)
        assertTrue(fresh.wifiOnly)
        assertTrue(fresh.farHalfTurned)
        val inPortuguese = TranslatorDefaults.resolve(fresh, appLanguage = pt)
        assertEquals(pt, inPortuguese.source)
        assertEquals(en, inPortuguese.target)
        val inEnglish = TranslatorDefaults.resolve(fresh, appLanguage = en)
        assertEquals(en, inEnglish.source)
        assertEquals(es, inEnglish.target)
        val elsewhere = TranslatorDefaults.resolve(fresh, appLanguage = LanguageCode("de"))
        assertEquals(en, elsewhere.source)
        assertEquals(es, elsewhere.target)
        assertEquals(inPortuguese.source, inPortuguese.near)
        assertEquals(inPortuguese.target, inPortuguese.far)
    }

    @Test
    fun aChosenLanguageIsKeptWhateverTheAppsLanguage() {
        val chosen = TranslatorSettings(source = es, target = pt, near = pt, far = en)
        val resolved = TranslatorDefaults.resolve(chosen, appLanguage = en)
        assertEquals(es, resolved.source)
        assertEquals(pt, resolved.target)
        assertEquals(pt, resolved.near)
        assertEquals(en, resolved.far)
    }

    /** Spec 080: the checked languages are kept; a file from before them reads as no choice yet. */
    @Test
    fun theCheckedLanguagesAreKeptAndAnOlderFileHasNone() {
        val chosen = TranslatorSettings(languages = listOf(LanguageCode("ja"), pt))
        assertEquals(chosen.languages, TranslatorDocument.from(chosen).toDomain().languages)
        assertEquals(null, TranslatorDocument().toDomain().languages)
    }

    @Test
    fun settingsSurviveARestart() =
        runTest {
            val fileSystem = FakeFileSystem()
            val first = DataStoreTranslatorSettings(JsonDataStoreFactory(fileSystem, "/a".toPath(), backgroundScope))
            first.update {
                it.copy(
                    source = es,
                    target = pt,
                    mode = TranslatorMode.CONVERSATION,
                    wifiOnly = false,
                    farHalfTurned = false,
                )
            }
            val saved = first.settings.first()
            assertEquals(es, saved.source)
            assertEquals(TranslatorMode.CONVERSATION, saved.mode)
            assertEquals(false, saved.wifiOnly)
            assertEquals(false, saved.farHalfTurned)
        }

    @Test
    fun theDataFileCarriesTheSettingsAndNothingElse() =
        runTest {
            val repository =
                DataStoreTranslatorSettings(JsonDataStoreFactory(FakeFileSystem(), "/a".toPath(), backgroundScope))
            repository.update { it.copy(source = pt, target = es) }
            val part = repository.dataPart()
            assertEquals("translator", part.id)
            assertEquals(PartKind.SETTINGS, part.kind)
            val document = part.export().document as JsonObject
            assertEquals(
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
                ),
                document.keys,
            )
            assertEquals("pt", document.getValue("source").jsonPrimitive.content)
            assertEquals(
                1,
                document
                    .getValue("schemaVersion")
                    .jsonPrimitive.content
                    .toInt(),
            )
        }

    @Test
    fun anUnknownModeFallsBackToTypeOrSpeak() =
        runTest {
            val repository =
                DataStoreTranslatorSettings(JsonDataStoreFactory(FakeFileSystem(), "/a".toPath(), backgroundScope))
            val part = repository.dataPart()
            val prepared =
                part.prepare(
                    kotlinx.serialization.json.Json
                        .parseToJsonElement("""{"schemaVersion":1,"mode":"LISTEN"}"""),
                    1,
                )
            part.write(prepared)
            assertEquals(TranslatorMode.TYPE_OR_SPEAK, repository.settings.first().mode)
        }
}
