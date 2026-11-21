package tech.onova.offlinetranslator.core.domain.settings

public enum class ThemeMode { SYSTEM, LIGHT, DARK }

public enum class UnitSystem { METRIC, IMPERIAL }

/** How dates are written (spec 004 FR-045); the language's own order by default. */
public enum class DateFormatChoice { FOLLOW_LANGUAGE, DAY_MONTH_YEAR, MONTH_DAY_YEAR, YEAR_MONTH_DAY }

/** How times are written (spec 004 FR-045); the phone's 12- or 24-hour setting by default. */
public enum class TimeFormatChoice { FOLLOW_DEVICE, TWELVE_HOUR, TWENTY_FOUR_HOUR }

/** App language choice; [tag] is the BCP 47 language tag, or null to follow the system. */
public enum class AppLanguage(
    public val tag: String?,
) {
    SYSTEM(tag = null),
    EN(tag = "en"),
    PT_BR(tag = "pt-BR"),
    ES(tag = "es"),
    ;

    public companion object {
        public fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag != null && it.tag.equals(tag, ignoreCase = true) } ?: SYSTEM
    }
}

/** Persisted user preferences (contracts/persistence-schema.md "app_settings.json"). */
public data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val unitSystem: UnitSystem? = null,
    val dateFormat: DateFormatChoice = DateFormatChoice.FOLLOW_LANGUAGE,
    val timeFormat: TimeFormatChoice = TimeFormatChoice.FOLLOW_DEVICE,
)
