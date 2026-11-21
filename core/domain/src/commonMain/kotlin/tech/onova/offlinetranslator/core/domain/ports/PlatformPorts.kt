package tech.onova.offlinetranslator.core.domain.ports

import kotlinx.coroutines.flow.StateFlow

/**
 * A moment on a clock that ignores wall-clock and time zone changes (spec 004 research.md R5).
 * [bootId] changes when the device restarts, which makes earlier moments meaningless.
 */
public data class MonotonicInstant(
    val bootId: Int,
    val elapsedMillis: Long,
)

public fun interface MonotonicClock {
    public fun now(): MonotonicInstant
}

/**
 * Whether the screen this belongs to is visible (research.md R15). A ViewModel gets its own task's, so a tool stops
 * when it's hidden even if another task of this app is in front (spec 016 research R2). What outlives one screen wants
 * [ProcessVisibility].
 */
public interface AppVisibility {
    public val isVisible: StateFlow<Boolean>
}

/** Whether any screen of the app is visible, for what serves the whole process: the store, or a timer's alert. */
public interface ProcessVisibility : AppVisibility
