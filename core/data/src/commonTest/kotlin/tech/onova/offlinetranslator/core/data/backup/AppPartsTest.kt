package tech.onova.offlinetranslator.core.data.backup

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import tech.onova.offlinetranslator.core.data.datastore.JsonDataStoreFactory
import tech.onova.offlinetranslator.core.data.settings.DataStoreAppSettingsRepository
import tech.onova.offlinetranslator.core.domain.ports.AppLanguageController
import tech.onova.offlinetranslator.core.domain.settings.AppLanguage
import tech.onova.offlinetranslator.core.domain.settings.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals

class AppPartsTest {
    private class FakeLanguages(
        var language: AppLanguage = AppLanguage.SYSTEM,
    ) : AppLanguageController {
        override fun current(): AppLanguage = language

        override fun apply(language: AppLanguage) {
            this.language = language
        }
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
}
