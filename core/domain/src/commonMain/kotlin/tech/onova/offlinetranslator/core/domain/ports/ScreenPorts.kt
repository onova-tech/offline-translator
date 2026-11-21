package tech.onova.offlinetranslator.core.domain.ports

import kotlinx.coroutines.flow.StateFlow

/**
 * Whether a screen reader (TalkBack, VoiceOver) reads the screen (spec 012 contracts/ports.md). With it on, the
 * Translator announces a translation instead of speaking it in its own voice, so the two never talk over each other.
 */
public interface ScreenReader {
    public val on: StateFlow<Boolean>
}

public interface ScreenBrightness {
    /** 0.1f..1f, or null to give the brightness back to the system. */
    public fun set(level: Float?)
}

/** The system's share sheet for an image the app made (spec 019). */
public interface ImageSharer {
    public fun sharePng(
        bytes: ByteArray,
        fileName: String,
    )
}

public interface TextSharer {
    public fun share(text: String)
}
