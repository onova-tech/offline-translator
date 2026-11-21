package tech.onova.offlinetranslator.core.data.datastore

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonDataStoreFactoryTest {
    @Serializable
    data class SampleDocument(
        val schemaVersion: Int = 1,
        val value: Int = 0,
    )

    private val fileSystem = FakeFileSystem()
    private val directory = "/data/datastore".toPath()

    private val serializer =
        JsonDocumentSerializer(
            serializer = SampleDocument.serializer(),
            defaultValue = SampleDocument(),
            currentSchemaVersion = 1,
            schemaVersionOf = { it.schemaVersion },
        )

    private fun write(
        fileName: String,
        content: String,
    ) {
        fileSystem.createDirectories(directory)
        fileSystem.write(directory / fileName) { writeUtf8(content) }
    }

    @Test
    fun roundTripsADocumentAsJson() =
        runTest {
            val store =
                JsonDataStoreFactory(
                    fileSystem,
                    directory,
                    backgroundScope,
                ).create("round-trip.json", serializer)
            store.updateData { it.copy(value = 5) }
            assertEquals(SampleDocument(value = 5), store.data.first())
            val stored = fileSystem.read(directory / "round-trip.json") { readUtf8() }
            assertTrue("\"schemaVersion\":1" in stored && "\"value\":5" in stored, stored)
        }

    @Test
    fun ignoresUnknownFields() =
        runTest {
            write("unknown.json", """{"schemaVersion":1,"value":3,"addedLater":true}""")
            val store = JsonDataStoreFactory(fileSystem, directory, backgroundScope).create("unknown.json", serializer)
            assertEquals(SampleDocument(value = 3), store.data.first())
        }

    @Test
    fun corruptFileFallsBackToDefaults() =
        runTest {
            write("corrupt.json", "not json at all")
            val store = JsonDataStoreFactory(fileSystem, directory, backgroundScope).create("corrupt.json", serializer)
            assertEquals(SampleDocument(), store.data.first())
        }

    @Test
    fun documentFromANewerSchemaFallsBackToDefaults() =
        runTest {
            write("newer.json", """{"schemaVersion":2,"value":9}""")
            val store = JsonDataStoreFactory(fileSystem, directory, backgroundScope).create("newer.json", serializer)
            assertEquals(SampleDocument(), store.data.first())
        }
}
