package tech.onova.offlinetranslator.core.toolapi

import tech.onova.offlinetranslator.core.domain.catalog.MergedTools
import tech.onova.offlinetranslator.core.domain.catalog.ToolId

/** Every tool shipped in this build (contracts/tool-contract.md §3). */
public interface ToolRegistry {
    public val entries: List<ToolEntry>

    public fun find(id: ToolId): ToolEntry?
}

/** Registry over the entries contributed by tool packs; duplicate ids fail fast. */
public class DefaultToolRegistry(
    entries: List<ToolEntry>,
) : ToolRegistry {
    override val entries: List<ToolEntry> = entries.toList()

    private val byId: Map<ToolId, ToolEntry> = this.entries.associateBy { it.descriptor.id }

    init {
        val duplicates =
            this.entries
                .groupBy { it.descriptor.id }
                .filterValues { it.size > 1 }
                .keys
        require(duplicates.isEmpty()) { "Duplicate tool ids registered: $duplicates" }
    }

    /** A merged tool's id finds the tool it became (spec 032): a shortcut or a link to it still opens. */
    override fun find(id: ToolId): ToolEntry? = byId[id] ?: byId[MergedTools.current(id)]
}
