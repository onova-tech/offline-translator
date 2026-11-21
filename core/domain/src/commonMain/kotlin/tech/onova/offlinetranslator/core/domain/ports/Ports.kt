package tech.onova.offlinetranslator.core.domain.ports

import tech.onova.offlinetranslator.core.domain.catalog.HardwareRequirement
import tech.onova.offlinetranslator.core.domain.settings.AppLanguage

/** What this device offers; implemented per platform. */
public interface DeviceCapabilities {
    public val presentHardware: Set<HardwareRequirement>
    public val supportsDynamicColor: Boolean
    public val hasVibrator: Boolean
}

/** Reads and applies the per-app language through the platform API (research.md R5). */
public interface AppLanguageController {
    public fun current(): AppLanguage

    public fun apply(language: AppLanguage)
}

/**
 * Build information shown in Settings (spec FR-026). [deviceModel] ("samsung SM-G780G") goes into a data
 * file, so calibrations are restored only on the same model (spec 010 FR-006).
 */
public data class AppInfo(
    val versionName: String,
    val deviceModel: String = "",
)
