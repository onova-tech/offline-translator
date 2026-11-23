package tech.onova.offlinetranslator.core.domain.ports

import kotlinx.coroutines.flow.StateFlow

/**
 * Whether a screen reader (TalkBack, VoiceOver) reads the screen (spec 012 contracts/ports.md). With it on, the
 * Translator announces a translation instead of speaking it in its own voice, so the two never talk over each other.
 */
public interface ScreenReader {
    public val on: StateFlow<Boolean>
}
