package tech.onova.offlinetranslator.core.domain.ports

import kotlinx.coroutines.flow.Flow
import tech.onova.offlinetranslator.core.domain.settings.AppSettings

public interface AppSettingsRepository {
    public val settings: Flow<AppSettings>

    public suspend fun update(transform: (AppSettings) -> AppSettings)
}
