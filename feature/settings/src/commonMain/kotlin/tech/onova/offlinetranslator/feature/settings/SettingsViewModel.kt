package tech.onova.offlinetranslator.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.onova.offlinetranslator.core.domain.ports.AppInfo
import tech.onova.offlinetranslator.core.domain.ports.AppLanguageController
import tech.onova.offlinetranslator.core.domain.ports.DeviceCapabilities
import tech.onova.offlinetranslator.core.domain.settings.AppLanguage
import tech.onova.offlinetranslator.core.domain.settings.AppSettings
import tech.onova.offlinetranslator.core.domain.ports.AppSettingsRepository
import tech.onova.offlinetranslator.core.domain.settings.ThemeMode

internal data class SettingsUiState(
    val loaded: Boolean = false,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val dynamicColorAvailable: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val versionName: String = "",
)

/** Appearance and the app's language (spec 081: what Offline Translator keeps of PolyTools' Settings). */
internal class SettingsViewModel(
    private val appSettings: AppSettingsRepository,
    private val languageController: AppLanguageController,
    capabilities: DeviceCapabilities,
    appInfo: AppInfo,
) : ViewModel() {
    private val language = MutableStateFlow(languageController.current())

    val uiState: StateFlow<SettingsUiState> =
        combine(appSettings.settings, language) { settings, language ->
            SettingsUiState(
                loaded = true,
                theme = settings.theme,
                dynamicColor = settings.dynamicColor,
                dynamicColorAvailable = capabilities.supportsDynamicColor,
                language = language,
                versionName = appInfo.versionName,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SettingsUiState())

    fun setTheme(theme: ThemeMode) = update { it.copy(theme = theme) }

    fun setDynamicColor(enabled: Boolean) = update { it.copy(dynamicColor = enabled) }

    /** The platform applies the language immediately (the activity is recreated on Android). */
    fun setLanguage(choice: AppLanguage) {
        languageController.apply(choice)
        language.value = choice
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { appSettings.update(transform) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
