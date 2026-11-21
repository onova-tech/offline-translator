package tech.onova.offlinetranslator.core.domain.ports

/**
 * Languages' names, from the phone (spec 080 research R4): in the app's language for the lists, and in the language
 * itself beside it. [tag] is a language tag such as "ja", "zh-Hant" or "fil" (`OfferedLanguage.tag`).
 */
public interface LanguageNames {
    /** "Japanese", "Japonês", "Japonés": in [appLanguage], capitalised as a list item. */
    public fun nameOf(
        tag: String,
        appLanguage: String,
    ): String

    /** "日本語", "Français": in the language itself, capitalised where the language does. */
    public fun ownName(tag: String): String
}
