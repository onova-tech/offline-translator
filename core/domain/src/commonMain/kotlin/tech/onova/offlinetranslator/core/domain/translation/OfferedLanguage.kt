package tech.onova.offlinetranslator.core.domain.translation

import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.PackId

/** How good a language's translation models are (spec 080 FR-006): BASIC when either direction is the small kind. */
public enum class LanguageQuality { FULL, BASIC }

/** A language the Translator offers (spec 080 data-model.md), one line of [LanguageCatalog]. */
public data class OfferedLanguage(
    val code: LanguageCode,
    /** Its pack, both directions with English; null for English, which needs none. */
    val pack: PackId?,
    val quality: LanguageQuality,
    /** The language tag the phone names it by: "zh-Hans", "zh-Hant", "fil". */
    val tag: String,
    /** Its default speech region; the phone's own region wins where it matches the language (research R5). */
    val speechRegion: String,
)
