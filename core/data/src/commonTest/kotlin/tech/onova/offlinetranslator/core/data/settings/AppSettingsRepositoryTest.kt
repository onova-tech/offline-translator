package tech.onova.offlinetranslator.core.data.settings

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import tech.onova.offlinetranslator.core.data.datastore.JsonDataStoreFactory
import tech.onova.offlinetranslator.core.domain.settings.AppSettings
import tech.onova.offlinetranslator.core.domain.settings.DateFormatChoice
import tech.onova.offlinetranslator.core.domain.settings.ThemeMode
import tech.onova.offlinetranslator.core.domain.settings.TimeFormatChoice
import tech.onova.offlinetranslator.core.domain.settings.UnitSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppSettingsRepositoryTest {
    private val fileSystem = FakeFileSystem()
    private val directory = "/data/datastore".toPath()

    @Test
    fun defaultsMatchTheContract() =
        runTest {
            val repository =
                DataStoreAppSettingsRepository(JsonDataStoreFactory(fileSystem, directory, backgroundScope))
            assertEquals(
                AppSettings(theme = ThemeMode.SYSTEM, dynamicColor = true, unitSystem = null),
                repository.settings.first(),
            )
        }

    @Test
    fun updatesArePersistedWithTheV1Schema() =
        runTest {
            val repository =
                DataStoreAppSettingsRepository(JsonDataStoreFactory(fileSystem, directory, backgroundScope))
            repository.update {
                it.copy(
                    theme = ThemeMode.DARK,
                    dynamicColor = false,
                    unitSystem = UnitSystem.IMPERIAL,
                )
            }
            assertEquals(
                AppSettings(ThemeMode.DARK, dynamicColor = false, unitSystem = UnitSystem.IMPERIAL),
                repository.settings.first(),
            )
            val stored = fileSystem.read(directory / DataStoreAppSettingsRepository.FILE_NAME) { readUtf8() }
            listOf("\"schemaVersion\":1", "\"theme\":\"DARK\"", "\"dynamicColor\":false", "\"unitSystem\":\"IMPERIAL\"")
                .forEach { assertTrue(it in stored, "$it missing from $stored") }
        }

    @Test
    fun unknownStoredValuesFallBackToDefaults() =
        runTest {
            fileSystem.createDirectories(directory)
            fileSystem.write(directory / DataStoreAppSettingsRepository.FILE_NAME) {
                writeUtf8("""{"schemaVersion":1,"theme":"SEPIA","dynamicColor":true,"unitSystem":"NAUTICAL"}""")
            }
            val repository =
                DataStoreAppSettingsRepository(JsonDataStoreFactory(fileSystem, directory, backgroundScope))
            assertEquals(AppSettings(), repository.settings.first())
        }

    @Test
    fun dateAndTimeFormatsArePersistedAndOldFilesReadAsDefaults() =
        runTest {
            fileSystem.createDirectories(directory)
            // A file from before spec 004's formats: the new choices read as their defaults.
            fileSystem.write(directory / DataStoreAppSettingsRepository.FILE_NAME) {
                writeUtf8("""{"schemaVersion":1,"theme":"DARK","dynamicColor":true}""")
            }
            val repository =
                DataStoreAppSettingsRepository(JsonDataStoreFactory(fileSystem, directory, backgroundScope))
            assertEquals(DateFormatChoice.FOLLOW_LANGUAGE, repository.settings.first().dateFormat)
            assertEquals(TimeFormatChoice.FOLLOW_DEVICE, repository.settings.first().timeFormat)
            repository.update {
                it.copy(dateFormat = DateFormatChoice.DAY_MONTH_YEAR, timeFormat = TimeFormatChoice.TWELVE_HOUR)
            }
            val stored = fileSystem.read(directory / DataStoreAppSettingsRepository.FILE_NAME) { readUtf8() }
            listOf("\"dateFormat\":\"DAY_MONTH_YEAR\"", "\"timeFormat\":\"TWELVE_HOUR\"", "\"theme\":\"DARK\"")
                .forEach { assertTrue(it in stored, "$it missing from $stored") }
        }
}
