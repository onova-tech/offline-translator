package tech.onova.offlinetranslator.core.domain.catalog

import kotlin.jvm.JvmInline

/**
 * Stable identifier of a tool. Permanent once shipped: favorites, recents, exports and usage
 * counts reference it, so an id is never renamed or reused. A tool merged into another keeps its id in
 * [MergedTools].
 */
@JvmInline
public value class ToolId(
    public val value: String,
) {
    init {
        require(PATTERN.matches(value)) { "Invalid tool id '$value': expected ${PATTERN.pattern}" }
    }

    override fun toString(): String = value

    private companion object {
        val PATTERN = Regex("^[a-z][a-z0-9-]{1,39}$")
    }
}
