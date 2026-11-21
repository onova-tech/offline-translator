package tech.onova.offlinetranslator.core.domain.catalog

/**
 * Tools merged into another (spec 032). An id stays permanent: a merged one keeps pointing here at the tool that
 * took it in, so stored favorites, recents, launcher shortcuts and data files still reach it. A key is never
 * reused for a new tool.
 */
public object MergedTools {
    private val into: Map<String, String> =
        mapOf(
            // The Plumb bob became the Bubble level's Plumb mode.
            "plumb-bob" to "bubble-level",
        )

    /** The tool that [id] is now: itself, or the one it was merged into. */
    public fun current(id: ToolId): ToolId = into[id.value]?.let(::ToolId) ?: id
}
