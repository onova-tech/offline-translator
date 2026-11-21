package tech.onova.offlinetranslator.core.data.backup

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import tech.onova.offlinetranslator.core.data.datastore.JsonDocumentSerializer
import tech.onova.offlinetranslator.core.domain.catalog.ToolId

/** What merging a list document gave, in the document's own type. */
public sealed interface DocumentMerge<out T> {
    public class Merged<T>(
        public val value: T,
        public val added: Int,
        public val droppedOld: Int = 0,
    ) : DocumentMerge<T>

    public class OverLimit(
        public val limit: Int,
        public val current: Int,
        public val incoming: Int,
    ) : DocumentMerge<Nothing>
}

/** How a [JsonDocumentPart] checks, counts, merges and calibrates its document. */
public class DocumentRules<T>(
    /** Brings an imported document within the app's own rules, usually through the domain model. */
    public val normalize: (T) -> T = { it },
    /** The number of items of a list document, or null. */
    public val count: (T) -> Int? = { null },
    /** Throws [PartRefusedException] with [PartRefusal.OVER_LIMIT] when a document holds more than allowed. */
    public val checkLimits: (T) -> Unit = {},
    /** Adds the file's items to the phone's; required for a [PartKind.LIST] part. */
    public val merge: ((current: T, incoming: T) -> DocumentMerge<T>)? = null,
    /** Keeps [current]'s calibration in [incoming]; set only for parts with calibrations. */
    public val keepCalibration: ((incoming: T, current: T) -> T)? = null,
    public val afterImport: suspend () -> Unit = {},
)

/**
 * The part of one DataStore document (spec 010 research R8). It uses the repository's own store, so
 * build it inside the repository that owns the store.
 */
public class JsonDocumentPart<T : Any>(
    override val id: String,
    override val order: Int,
    override val toolId: ToolId?,
    override val kind: PartKind,
    private val store: DataStore<T>,
    private val serializer: JsonDocumentSerializer<T>,
    private val rules: DocumentRules<T> = DocumentRules(),
) : DataPart {
    override val hasCalibration: Boolean get() = rules.keepCalibration != null
    override val schemaVersion: Int get() = serializer.currentSchemaVersion

    override suspend fun export(): PartExport {
        val current = store.data.first()
        return PartExport(serializer.encodeToElement(current), rules.count(current))
    }

    override suspend fun prepare(
        document: JsonElement,
        schemaVersion: Int,
    ): PreparedPart {
        if (schemaVersion > serializer.currentSchemaVersion) throw PartRefusedException(PartRefusal.NEWER_SCHEMA)
        val decoded = serializer.decodeFromElement(document)
        rules.checkLimits(decoded)
        val normalized = rules.normalize(decoded)
        return PreparedPart(normalized, rules.count(normalized))
    }

    override suspend fun merge(incoming: PreparedPart): MergeOutcome {
        val merge = checkNotNull(rules.merge) { "Part $id holds no list" }
        return when (val result = merge(store.data.first(), typed(incoming))) {
            is DocumentMerge.Merged -> {
                MergeOutcome.Merged(
                    PreparedPart(result.value, rules.count(result.value)),
                    result.added,
                    result.droppedOld,
                )
            }

            is DocumentMerge.OverLimit -> {
                MergeOutcome.OverLimit(result.limit, result.current, result.incoming)
            }
        }
    }

    override suspend fun withoutCalibration(incoming: PreparedPart): PreparedPart {
        val keep = rules.keepCalibration ?: return incoming
        val kept = keep(typed(incoming), store.data.first())
        return PreparedPart(kept, rules.count(kept))
    }

    override suspend fun write(prepared: PreparedPart) {
        val value = typed(prepared)
        store.updateData { value }
    }

    override suspend fun restore(document: JsonElement) {
        val value = serializer.decodeFromElement(document)
        store.updateData { value }
    }

    override suspend fun afterImport() {
        rules.afterImport()
    }

    @Suppress("UNCHECKED_CAST") // a prepared part only ever comes from this part's own prepare or merge
    private fun typed(prepared: PreparedPart): T = prepared.value as T
}

/** Throws [PartRefusedException] with [PartRefusal.OVER_LIMIT] unless [condition] holds. */
public fun requireWithinLimit(condition: Boolean) {
    if (!condition) throw PartRefusedException(PartRefusal.OVER_LIMIT)
}
