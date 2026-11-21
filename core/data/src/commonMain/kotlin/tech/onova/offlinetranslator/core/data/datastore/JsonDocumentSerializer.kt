package tech.onova.offlinetranslator.core.data.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okio.BufferedSink
import okio.BufferedSource
import tech.onova.offlinetranslator.core.data.backup.PartRefusal
import tech.onova.offlinetranslator.core.data.backup.PartRefusedException

/**
 * Reads and writes one versioned JSON document (contracts/persistence-schema.md). Unreadable
 * documents, and documents written by a newer app version, are reported as corruption so the
 * store falls back to [defaultValue] instead of crashing.
 */
public class JsonDocumentSerializer<T>(
    private val serializer: KSerializer<T>,
    override val defaultValue: T,
    public val currentSchemaVersion: Int,
    private val schemaVersionOf: (T) -> Int,
) : OkioSerializer<T> {
    override suspend fun readFrom(source: BufferedSource): T {
        val document =
            try {
                JSON.decodeFromString(serializer, source.readUtf8())
            } catch (e: SerializationException) {
                throw CorruptionException("Unreadable document", e)
            } catch (e: IllegalArgumentException) {
                throw CorruptionException("Invalid document", e)
            }
        if (schemaVersionOf(document) > currentSchemaVersion) {
            throw CorruptionException(
                "Document schema ${schemaVersionOf(document)} is newer than $currentSchemaVersion",
            )
        }
        return document
    }

    override suspend fun writeTo(
        t: T,
        sink: BufferedSink,
    ) {
        sink.writeUtf8(JSON.encodeToString(serializer, t))
    }

    /** The document as JSON, exactly as it is stored (spec 010: a data file holds the same JSON). */
    public fun encodeToElement(value: T): JsonElement = JSON.encodeToJsonElement(serializer, value)

    /**
     * Reads a document from a data file. Throws [PartRefusedException] when it is newer than this app
     * knows ([PartRefusal.NEWER_SCHEMA]) or doesn't match the document's shape ([PartRefusal.UNREADABLE]).
     */
    public fun decodeFromElement(element: JsonElement): T {
        val document =
            try {
                JSON.decodeFromJsonElement(serializer, element)
            } catch (e: IllegalArgumentException) {
                // SerializationException, the usual failure, is an IllegalArgumentException too.
                throw PartRefusedException(PartRefusal.UNREADABLE, e)
            }
        if (schemaVersionOf(document) > currentSchemaVersion) throw PartRefusedException(PartRefusal.NEWER_SCHEMA)
        return document
    }

    private companion object {
        val JSON =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
    }
}
