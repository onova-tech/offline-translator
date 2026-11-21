package tech.onova.offlinetranslator.core.domain.ports

import kotlin.jvm.JvmInline

/** A language as the Translator names it: `en`, `pt`, `es`… (spec 012 data-model.md). Regions are for speech only. */
@JvmInline
public value class LanguageCode(
    public val value: String,
) {
    public companion object {
        /** Built into the app: every pack translates to and from English (spec 012 FR-004). */
        public val ENGLISH: LanguageCode = LanguageCode("en")
    }
}

/** How a text gets from one language to another (spec 012 FR-004, FR-005). */
public sealed interface Route {
    /** The same language: the text is shown as typed. */
    public data object Same : Route

    /** One side is English: one model of [pack]. */
    public data class Direct(
        val from: LanguageCode,
        val to: LanguageCode,
        val pack: PackId,
    ) : Route

    /** Neither side is English: [from] to English with [first], then English to [to] with [second]. */
    public data class ThroughEnglish(
        val from: LanguageCode,
        val to: LanguageCode,
        val first: PackId,
        val second: PackId,
    ) : Route
}

/** Why the engine can't run. */
public enum class EngineUnavailable {
    /** No engine for this phone's processor (a 32-bit phone, spec 012 research R3). */
    UNSUPPORTED_DEVICE,

    /** A pack's files are missing or unreadable. */
    DAMAGED_PACK,
}

/** The answer to [TranslationEngine.open]. */
public sealed interface EngineOpen {
    public data object Ready : EngineOpen

    /** Open with the other packs: the files of [packs] are missing or unreadable (spec 012 edge case). */
    public data class Damaged(
        val packs: Set<PackId>,
    ) : EngineOpen

    public data class Unavailable(
        val reason: EngineUnavailable,
    ) : EngineOpen
}

/**
 * The on-device translation engine (spec 012 research R3): Bergamot on Android now, the same code on iPhone later.
 * It never uses the network; the text never leaves the phone (FR-003).
 */
public interface TranslationEngine {
    /**
     * Loads the models of the installed packs, named by the folder that holds each one. Slow: never on the main thread.
     */
    public suspend fun open(packs: Map<PackId, String>): EngineOpen

    /** Translates [text] along [route]. A newer call doesn't interrupt a running one; callers drop stale results. */
    public suspend fun translate(
        text: String,
        route: Route,
    ): String

    /** Frees the models; called when the tool is left. */
    public fun close()
}
