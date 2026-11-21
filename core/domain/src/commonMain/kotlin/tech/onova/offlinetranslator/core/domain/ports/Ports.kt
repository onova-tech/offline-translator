package tech.onova.offlinetranslator.core.domain.ports

import tech.onova.offlinetranslator.core.domain.catalog.HardwareRequirement
import tech.onova.offlinetranslator.core.domain.settings.AppLanguage

/** What this device offers; implemented per platform. */
public interface DeviceCapabilities {
    public val presentHardware: Set<HardwareRequirement>
    public val supportsDynamicColor: Boolean
    public val hasVibrator: Boolean
}

/** The device region (ISO 3166 alpha-2), used for the unit-system default. */
public fun interface RegionProvider {
    public fun regionCode(): String?
}

/** A currency by its ISO 4217 code and the symbol people of the device's region write (`BRL`, `R$`). */
public data class CurrencyInfo(
    public val code: String,
    public val symbol: String,
)

/**
 * The currency of the device's region (not the per-app language), shown next to amounts in the Financial
 * calculator (spec 014 FR-009). Only a label: nothing is converted. Null when the region has no currency.
 */
public fun interface RegionCurrency {
    public fun current(): CurrencyInfo?
}

/** A currency as the app's language names it: "US dollar", "Dólar americano" (spec 038). */
public data class CurrencyName(
    public val code: String,
    public val name: String,
)

/** The world's current currencies, named in the app's language and sorted by name, for the converter (spec 038). */
public fun interface CurrencyNames {
    public fun all(): List<CurrencyName>
}

public enum class DisplayAxis { HORIZONTAL, VERTICAL }

/** Physical pixel density along an axis, as reported by the display (research.md R11). */
public fun interface DisplayMetricsProvider {
    public fun physicalDpi(axis: DisplayAxis): Double
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
