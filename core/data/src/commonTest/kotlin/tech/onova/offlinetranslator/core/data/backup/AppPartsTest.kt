package tech.onova.offlinetranslator.core.data.backup

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import tech.onova.offlinetranslator.core.data.catalog.DataStoreCatalogStateRepository
import tech.onova.offlinetranslator.core.data.datastore.JsonDataStoreFactory
import tech.onova.offlinetranslator.core.data.settings.DataStoreAppSettingsRepository
import tech.onova.offlinetranslator.core.domain.catalog.ToolId
import tech.onova.offlinetranslator.core.domain.ports.AppLanguageController
import tech.onova.offlinetranslator.core.domain.settings.AppLanguage
import tech.onova.offlinetranslator.core.domain.settings.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant

class AppPartsTest {
    private class FakeLanguages(
        var language: AppLanguage = AppLanguage.SYSTEM,
    ) : AppLanguageController {
        override fun current(): AppLanguage = language

        override fun apply(language: AppLanguage) {
            this.language = language
        }
    }

    private class FixedClock(
        var now: Instant,
    ) : Clock {
        override fun now(): Instant = now
    }

    @Test
    fun settingsCarryTheLanguageAndApplyItAfterTheImport() =
        runTest {
            val source =
                DataStoreAppSettingsRepository(JsonDataStoreFactory(FakeFileSystem(), "/a".toPath(), backgroundScope))
            source.update { it.copy(theme = ThemeMode.DARK) }
            val exported = source.dataPart(FakeLanguages(AppLanguage.PT_BR)).export()
            assertEquals(JsonPrimitive("pt-BR"), (exported.document as JsonObject)["language"])

            val languages = FakeLanguages()
            val target =
                DataStoreAppSettingsRepository(JsonDataStoreFactory(FakeFileSystem(), "/b".toPath(), backgroundScope))
            val part = target.dataPart(languages)
            part.write(part.prepare(exported.document, 1))
            assertEquals(ThemeMode.DARK, target.settings.first().theme)
            assertEquals(AppLanguage.SYSTEM, languages.language)
            part.afterImport()
            assertEquals(AppLanguage.PT_BR, languages.language)
        }

    @Test
    fun favoritesMergeByToolIdAndTwiceAddsNothing() =
        runTest {
            val clock = FixedClock(Instant.parse("2026-11-02T10:00:00Z"))
            val source =
                DataStoreCatalogStateRepository(
                    JsonDataStoreFactory(FakeFileSystem(), "/a".toPath(), backgroundScope),
                    clock,
                )
            source.toggleFavorite(ToolId("ruler"))
            source.toggleFavorite(ToolId("counter"))
            val exported = source.dataPart().export()
            assertEquals(2, exported.items)

            val target =
                DataStoreCatalogStateRepository(
                    JsonDataStoreFactory(FakeFileSystem(), "/b".toPath(), backgroundScope),
                    clock,
                )
            target.toggleFavorite(ToolId("ruler"))
            val part = target.dataPart()
            repeat(2) { round ->
                val merged = assertIs<MergeOutcome.Merged>(part.merge(part.prepare(exported.document, 1)))
                assertEquals(if (round == 0) 1 else 0, merged.added)
                part.write(merged.prepared)
            }
            assertEquals(listOf("ruler", "counter"), target.favorites.first().map { it.toolId.value })
        }
}
