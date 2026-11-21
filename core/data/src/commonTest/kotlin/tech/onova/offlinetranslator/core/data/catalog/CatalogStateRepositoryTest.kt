package tech.onova.offlinetranslator.core.data.catalog

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import tech.onova.offlinetranslator.core.data.datastore.JsonDataStoreFactory
import tech.onova.offlinetranslator.core.domain.catalog.Favorite
import tech.onova.offlinetranslator.core.domain.catalog.RecentUse
import tech.onova.offlinetranslator.core.domain.catalog.ToolId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class CatalogStateRepositoryTest {
    private val fileSystem = FakeFileSystem()
    private val directory = "/data/datastore".toPath()
    private var nowSeconds = 1_000L
    private val clock =
        object : Clock {
            override fun now(): Instant = Instant.fromEpochSeconds(nowSeconds)
        }
    private val ruler = ToolId("ruler")
    private val counter = ToolId("counter")

    @Test
    fun startsEmpty() =
        runTest {
            val repository =
                DataStoreCatalogStateRepository(JsonDataStoreFactory(fileSystem, directory, backgroundScope), clock)
            assertEquals(emptyList(), repository.favorites.first())
            assertEquals(emptyList(), repository.recents.first())
        }

    @Test
    fun togglesFavoritesWithTheCurrentTime() =
        runTest {
            val repository =
                DataStoreCatalogStateRepository(JsonDataStoreFactory(fileSystem, directory, backgroundScope), clock)
            repository.toggleFavorite(ruler)
            assertEquals(listOf(Favorite(ruler, Instant.fromEpochSeconds(1_000))), repository.favorites.first())
            repository.toggleFavorite(ruler)
            assertEquals(emptyList(), repository.favorites.first())
        }

    @Test
    fun recordsRecentsMostRecentFirstAndPersistsIsoTimestamps() =
        runTest {
            val repository =
                DataStoreCatalogStateRepository(JsonDataStoreFactory(fileSystem, directory, backgroundScope), clock)
            repository.recordOpened(ruler)
            nowSeconds = 2_000
            repository.recordOpened(counter)
            assertEquals(
                listOf(
                    RecentUse(counter, Instant.fromEpochSeconds(2_000)),
                    RecentUse(ruler, Instant.fromEpochSeconds(1_000)),
                ),
                repository.recents.first(),
            )
            val stored = fileSystem.read(directory / DataStoreCatalogStateRepository.FILE_NAME) { readUtf8() }
            assertTrue("\"lastOpenedAt\":\"1970-01-01T00:33:20Z\"" in stored, stored)
        }

    @Test
    fun entriesWithInvalidIdsAreDropped() =
        runTest {
            fileSystem.createDirectories(directory)
            fileSystem.write(directory / DataStoreCatalogStateRepository.FILE_NAME) {
                writeUtf8(
                    """{"schemaVersion":1,"favorites":[{"toolId":"Not Valid","addedAt":"2026-10-20T14:03:11Z"},""" +
                        """{"toolId":"ruler","addedAt":"2026-10-20T14:03:12Z"}],"recents":[]}""",
                )
            }
            val repository =
                DataStoreCatalogStateRepository(JsonDataStoreFactory(fileSystem, directory, backgroundScope), clock)
            assertEquals(listOf(ruler), repository.favorites.first().map { it.toolId })
        }

    /** Spec 032: the Plumb bob's entries count as the Bubble level, once, at the first addition. */
    @Test
    fun aMergedToolsEntriesCountAsTheToolItJoined() =
        runTest {
            fileSystem.createDirectories(directory)
            fileSystem.write(directory / DataStoreCatalogStateRepository.FILE_NAME) {
                writeUtf8(
                    """{"schemaVersion":1,"favorites":[{"toolId":"plumb-bob","addedAt":"2026-10-20T14:03:11Z"},""" +
                        """{"toolId":"bubble-level","addedAt":"2026-10-21T14:03:12Z"}],""" +
                        """"recents":[{"toolId":"plumb-bob","lastOpenedAt":"2026-10-22T10:00:00Z"},""" +
                        """{"toolId":"bubble-level","lastOpenedAt":"2026-10-21T10:00:00Z"}]}""",
                )
            }
            val repository =
                DataStoreCatalogStateRepository(JsonDataStoreFactory(fileSystem, directory, backgroundScope), clock)
            val favorites = repository.favorites.first()
            assertEquals(listOf(ToolId("bubble-level")), favorites.map { it.toolId })
            assertEquals(Instant.parse("2026-10-20T14:03:11Z"), favorites.single().addedAt)
            assertEquals(listOf(ToolId("bubble-level")), repository.recents.first().map { it.toolId })
        }
}
