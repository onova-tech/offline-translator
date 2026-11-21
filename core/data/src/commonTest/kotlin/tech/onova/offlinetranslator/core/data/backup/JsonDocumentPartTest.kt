package tech.onova.offlinetranslator.core.data.backup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import tech.onova.offlinetranslator.core.data.datastore.JsonDataStoreFactory
import tech.onova.offlinetranslator.core.data.datastore.JsonDocumentSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class JsonDocumentPartTest {
    @Serializable
    data class Sample(
        val schemaVersion: Int = 1,
        val value: Int = 0,
        val calibration: Double = 1.0,
        val items: List<String> = emptyList(),
    )

    private val serializer =
        JsonDocumentSerializer(
            Sample.serializer(),
            Sample(),
            currentSchemaVersion = 1,
            schemaVersionOf = { it.schemaVersion },
        )

    private fun part(scope: CoroutineScope): Pair<JsonDocumentPart<Sample>, androidx.datastore.core.DataStore<Sample>> {
        val store = JsonDataStoreFactory(FakeFileSystem(), "/d".toPath(), scope).create("sample.json", serializer)
        val part =
            JsonDocumentPart(
                id = "sample",
                order = 1,
                toolId = null,
                kind = PartKind.LIST,
                store = store,
                serializer = serializer,
                rules =
                    DocumentRules(
                        normalize = { it.copy(value = it.value.coerceIn(0, 10)) },
                        count = { it.items.size },
                        checkLimits = { requireWithinLimit(it.items.size <= 3) },
                        merge = { current, incoming ->
                            val added = incoming.items.filter { it !in current.items }
                            DocumentMerge.Merged(current.copy(items = current.items + added), added.size)
                        },
                        keepCalibration = { incoming, current -> incoming.copy(calibration = current.calibration) },
                    ),
            )
        return part to store
    }

    private fun json(text: String): JsonElement = Json.parseToJsonElement(text)

    @Test
    fun exportGivesTheStoredJsonAndCount() =
        runTest {
            val (part, store) = part(backgroundScope)
            store.updateData { Sample(value = 4, items = listOf("a", "b")) }
            val export = part.export()
            assertEquals(2, export.items)
            assertEquals(Sample(value = 4, items = listOf("a", "b")), serializer.decodeFromElement(export.document))
        }

    @Test
    fun prepareRefusesNewerAndUnreadableDocuments() =
        runTest {
            val (part, _) = part(backgroundScope)
            assertEquals(
                PartRefusal.NEWER_SCHEMA,
                assertFailsWith<PartRefusedException> { part.prepare(json("""{"schemaVersion":2}"""), 1) }.reason,
            )
            assertEquals(
                PartRefusal.NEWER_SCHEMA,
                assertFailsWith<PartRefusedException> { part.prepare(json("""{"schemaVersion":1}"""), 2) }.reason,
            )
            assertEquals(
                PartRefusal.UNREADABLE,
                assertFailsWith<PartRefusedException> { part.prepare(json("""{"value":"many"}"""), 1) }.reason,
            )
        }

    @Test
    fun prepareNormalizesAndChecksLimits() =
        runTest {
            val (part, _) = part(backgroundScope)
            assertEquals(Sample(value = 10), part.prepare(json("""{"value":99}"""), 1).value)
            assertEquals(
                PartRefusal.OVER_LIMIT,
                assertFailsWith<PartRefusedException> {
                    part.prepare(json("""{"items":["a","b","c","d"]}"""), 1)
                }.reason,
            )
        }

    @Test
    fun writeReplacesAndMergeAddsOnlyNewItems() =
        runTest {
            val (part, store) = part(backgroundScope)
            store.updateData { Sample(items = listOf("a")) }
            val incoming = part.prepare(json("""{"items":["a","b"]}"""), 1)
            val merged = assertIs<MergeOutcome.Merged>(part.merge(incoming))
            assertEquals(1, merged.added)
            part.write(merged.prepared)
            assertEquals(listOf("a", "b"), store.data.first().items)
            part.write(part.prepare(json("""{"items":["z"]}"""), 1))
            assertEquals(listOf("z"), store.data.first().items)
        }

    @Test
    fun withoutCalibrationKeepsThePhonesValue() =
        runTest {
            val (part, store) = part(backgroundScope)
            store.updateData { Sample(calibration = 1.07) }
            val incoming = part.prepare(json("""{"value":3,"calibration":0.9}"""), 1)
            assertEquals(Sample(value = 3, calibration = 1.07), part.withoutCalibration(incoming).value)
            assertEquals(true, part.hasCalibration)
        }
}
