package tech.onova.offlinetranslator.feature.settings

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

public val settingsModule: Module =
    module {
        viewModelOf(::SettingsViewModel)
    }
