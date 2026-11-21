package tech.onova.offlinetranslator.core.domain.catalog

import kotlin.time.Instant

/** A tool the user pinned; [addedAt] defines the display order. */
public data class Favorite(
    val toolId: ToolId,
    val addedAt: Instant,
)

/** The last time a tool was opened; drives the "Recently used" row. */
public data class RecentUse(
    val toolId: ToolId,
    val lastOpenedAt: Instant,
)

/** Pure rules for favorites and recents (data-model.md "Favorite", "RecentUse"). */
public object CatalogStateRules {
    public const val MAX_RECENTS: Int = 6

    public fun toggleFavorite(
        favorites: List<Favorite>,
        id: ToolId,
        now: Instant,
    ): List<Favorite> =
        if (favorites.any { it.toolId == id }) {
            favorites.filterNot { it.toolId == id }
        } else {
            favorites + Favorite(id, now)
        }

    /** Favorites in the order added; tools no longer registered are hidden but stay stored. */
    public fun visibleFavorites(
        favorites: List<Favorite>,
        registered: Set<ToolId>,
    ): List<ToolId> =
        favorites
            .sortedBy { it.addedAt }
            .map { it.toolId }
            .distinct()
            .filter { it in registered }

    public fun recordOpened(
        recents: List<RecentUse>,
        id: ToolId,
        now: Instant,
    ): List<RecentUse> =
        (listOf(RecentUse(id, now)) + recents.filterNot { it.toolId == id })
            .sortedByDescending { it.lastOpenedAt }
            .take(MAX_RECENTS)

    public fun visibleRecents(
        recents: List<RecentUse>,
        registered: Set<ToolId>,
    ): List<ToolId> =
        recents
            .sortedByDescending { it.lastOpenedAt }
            .map { it.toolId }
            .distinct()
            .filter { it in registered }
            .take(MAX_RECENTS)
}
