package tech.onova.offlinetranslator.core.domain.speech

/** Who hears or speaks a language (spec 082): the phone's own service, or the app's models. */
public enum class SpeechEngine {
    PHONE,
    APP,
}

/** Where the app's own model for a language stands (spec 082): its pack's state, or none for the language. */
public enum class AppSpeechPart {
    /** The app has no model for this language. */
    NONE,
    NOT_DOWNLOADED,
    DOWNLOADING,
    READY,
}
