package tech.onova.offlinetranslator.core.data.backup

import kotlinx.serialization.json.JsonElement
import tech.onova.offlinetranslator.core.domain.catalog.ToolId

/**
 * One owner's data in a data file (spec 010, contracts/data-part.md). Every module that stores user data
 * contributes its parts through Koin (`single<DataPart>(named("data-part:<id>"))`); the export feature
 * lists whatever is bound and never reads files itself.
 */
public interface DataPart {
    /** Permanent id in the file ("notes", "level"). Never renamed; a new id is a new part. */
    public val id: String

    /** Where the part appears in the export and import lists. */
    public val order: Int

    /** The tool the part belongs to, whose name the pages show; null for the two app-wide parts. */
    public val toolId: ToolId?

    public val kind: PartKind

    /** Calibrations are restored only on the same phone model (spec 010 FR-006). */
    public val hasCalibration: Boolean

    /** The document version this app writes. */
    public val schemaVersion: Int

    /** The current data as JSON, with its item count for a [PartKind.LIST] part. */
    public suspend fun export(): PartExport

    /**
     * Decodes [document], written with [schemaVersion], and checks it against the part's rules. Nothing
     * is written. Throws [PartRefusedException] when the document can't be imported.
     */
    public suspend fun prepare(
        document: JsonElement,
        schemaVersion: Int,
    ): PreparedPart

    /** Adds a prepared document's items to the current data ([PartKind.LIST] only). Nothing is written. */
    public suspend fun merge(incoming: PreparedPart): MergeOutcome

    /** Keeps this phone's calibration and everything else from [incoming] (calibration parts only). */
    public suspend fun withoutCalibration(incoming: PreparedPart): PreparedPart

    /** Writes [prepared] in place of the current data, in one write. */
    public suspend fun write(prepared: PreparedPart)

    /**
     * Writes back [document], which this part's own [export] gave before an import, exactly as it was. It
     * skips [prepare]'s checks and clean-up, which are for files from elsewhere: a rollback must not stop a
     * running stopwatch or drop a snooze (spec 010 research R10).
     */
    public suspend fun restore(document: JsonElement)

    /** Runs after every chosen part is written, such as scheduling restored reminders. */
    public suspend fun afterImport() {}
}

/** A [PartKind.SETTINGS] part is always replaced; a [PartKind.LIST] part may be replaced or merged. */
public enum class PartKind { SETTINGS, LIST }

public class PartExport(
    public val document: JsonElement,
    public val items: Int?,
)

/** A decoded, checked document, ready to write. Its [value] belongs to the part that made it. */
public class PreparedPart(
    public val value: Any,
    public val items: Int?,
)

public sealed interface MergeOutcome {
    /** [added] new items; [droppedOld] older log entries that no longer fit. */
    public class Merged(
        public val prepared: PreparedPart,
        public val added: Int,
        public val droppedOld: Int,
    ) : MergeOutcome

    /** The list would hold more than [limit] items: [current] on the phone plus [incoming] new ones. */
    public data class OverLimit(
        val limit: Int,
        val current: Int,
        val incoming: Int,
    ) : MergeOutcome
}

public enum class PartRefusal {
    /** Written by a newer app than this one. */
    NEWER_SCHEMA,

    /** Not the shape of this part's document. */
    UNREADABLE,

    /** More items or longer text than the tool allows: only a changed file can hold that. */
    OVER_LIMIT,

    /** A value the tool can't hold. */
    INVALID_VALUE,
}

public class PartRefusedException(
    public val reason: PartRefusal,
    cause: Throwable? = null,
) : Exception("Part refused: $reason", cause)
