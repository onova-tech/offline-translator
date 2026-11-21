package tech.onova.offlinetranslator.core.data.usage

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import tech.onova.offlinetranslator.core.data.datastore.JsonDataStoreFactory
import tech.onova.offlinetranslator.core.domain.ads.DailyUsage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DailyUsageRepositoryTest {
    private val fileSystem = FakeFileSystem()
    private val directory = "/data/datastore".toPath()
    private val file = directory / DataStoreDailyUsageRepository.FILE_NAME

    private fun storeText(text: String) {
        fileSystem.createDirectories(directory)
        fileSystem.write(file) { writeUtf8(text) }
    }

    @Test
    fun nothingIsRecordedAtFirst() =
        runTest {
            val repository = DataStoreDailyUsageRepository(JsonDataStoreFactory(fileSystem, directory, backgroundScope))
            assertNull(repository.read())
        }

    @Test
    fun writesArePersistedWithTheV1Schema() =
        runTest {
            val repository = DataStoreDailyUsageRepository(JsonDataStoreFactory(fileSystem, directory, backgroundScope))
            repository.write(DailyUsage(LocalDate(2026, 10, 21), 2))
            assertEquals(DailyUsage(LocalDate(2026, 10, 21), 2), repository.read())
            val stored = fileSystem.read(file) { readUtf8() }
            listOf("\"schemaVersion\":1", "\"date\":\"2026-10-21\"", "\"count\":2")
                .forEach { assertTrue(it in stored, "$it missing from $stored") }
        }

    @Test
    fun anUnreadableDocumentMeansNothingRecorded() =
        runTest {
            storeText("{ not json")
            val repository = DataStoreDailyUsageRepository(JsonDataStoreFactory(fileSystem, directory, backgroundScope))
            assertNull(repository.read())
        }

    @Test
    fun anImpossibleCountMeansNothingRecorded() =
        runTest {
            storeText("""{"schemaVersion":1,"date":"2026-10-21","count":-4}""")
            val repository = DataStoreDailyUsageRepository(JsonDataStoreFactory(fileSystem, directory, backgroundScope))
            assertNull(repository.read())
        }
}
