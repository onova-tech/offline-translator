package tech.onova.offlinetranslator.core.domain.ports

import kotlinx.coroutines.flow.Flow
import tech.onova.offlinetranslator.core.domain.catalog.Favorite
import tech.onova.offlinetranslator.core.domain.catalog.RecentUse
import tech.onova.offlinetranslator.core.domain.catalog.ToolId
import tech.onova.offlinetranslator.core.domain.settings.AppSettings

public interface AppSettingsRepository {
    public val settings: Flow<AppSettings>

    public suspend fun update(transform: (AppSettings) -> AppSettings)
}

public interface CatalogStateRepository {
    public val favorites: Flow<List<Favorite>>
    public val recents: Flow<List<RecentUse>>

    public suspend fun toggleFavorite(id: ToolId)

    public suspend fun recordOpened(id: ToolId)
}
