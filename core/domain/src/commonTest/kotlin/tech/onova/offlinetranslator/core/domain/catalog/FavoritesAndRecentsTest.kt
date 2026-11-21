package tech.onova.offlinetranslator.core.domain.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class FavoritesAndRecentsTest {
    private val ruler = ToolId("ruler")
    private val counter = ToolId("counter")
    private val level = ToolId("level")
    private val compass = ToolId("compass")
    private val flashlight = ToolId("flashlight")
    private val mirror = ToolId("mirror")
    private val magnifier = ToolId("magnifier")

    private fun at(seconds: Long) = Instant.fromEpochSeconds(seconds)

    @Test
    fun toggleAddsAMissingFavoriteAndRemovesAnExistingOne() {
        val added = CatalogStateRules.toggleFavorite(emptyList(), ruler, at(10))
        assertEquals(listOf(Favorite(ruler, at(10))), added)
        assertEquals(emptyList(), CatalogStateRules.toggleFavorite(added, ruler, at(20)))
    }

    @Test
    fun favoritesAreShownInTheOrderTheyWereAdded() {
        val favorites = listOf(Favorite(counter, at(30)), Favorite(ruler, at(10)), Favorite(level, at(20)))
        assertEquals(
            listOf(ruler, level, counter),
            CatalogStateRules.visibleFavorites(favorites, registered = setOf(ruler, level, counter)),
        )
    }

    @Test
    fun unregisteredFavoritesAreHiddenButKept() {
        val favorites = listOf(Favorite(ruler, at(10)), Favorite(compass, at(20)))
        assertEquals(listOf(ruler), CatalogStateRules.visibleFavorites(favorites, registered = setOf(ruler)))
        assertEquals(favorites, CatalogStateRules.toggleFavorite(favorites, counter, at(30)).dropLast(1))
    }

    @Test
    fun openingAToolMovesItToTheFrontWithoutDuplicates() {
        val recents = listOf(RecentUse(counter, at(20)), RecentUse(ruler, at(10)))
        assertEquals(
            listOf(RecentUse(ruler, at(30)), RecentUse(counter, at(20))),
            CatalogStateRules.recordOpened(recents, ruler, at(30)),
        )
    }

    @Test
    fun recentsKeepAtMostSixEntries() {
        val recents =
            listOf(ruler, counter, level, compass, flashlight, mirror)
                .mapIndexed { index, id -> RecentUse(id, at(100L - index)) }
        val updated = CatalogStateRules.recordOpened(recents, magnifier, at(200))
        assertEquals(6, updated.size)
        assertEquals(listOf(magnifier, ruler, counter, level, compass, flashlight), updated.map { it.toolId })
    }

    @Test
    fun visibleRecentsAreMostRecentFirstAndSkipUnregisteredTools() {
        val recents = listOf(RecentUse(ruler, at(10)), RecentUse(compass, at(30)), RecentUse(counter, at(20)))
        assertEquals(
            listOf(counter, ruler),
            CatalogStateRules.visibleRecents(recents, registered = setOf(ruler, counter)),
        )
    }
}
