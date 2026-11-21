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

/** Closes every tool opened from a shortcut, so nothing live writes during a data import (spec 016 FR-018). */
public fun interface ToolSessions {
    public fun closeAll()
}

/** The device's 12- or 24-hour preference (spec 004 FR-034). */
public fun interface ClockFormat {
    public fun is24Hour(): Boolean
}

/** The in-app alert: looping alarm sound at the alarm volume, plus vibration (research.md R6). */
public interface AlertPlayer {
    public fun start()

    public fun stop()
}

/** One system alarm for a running timer, with its texts already in the app language. */
public data class TimerAlarm(
    val timerId: String,
    val endsAt: MonotonicInstant,
    val title: String,
    val text: String,
    val stopLabel: String,
    /** The notification channel's name in the system settings, e.g. "Timer alerts". */
    val channelName: String,
)

/** System alarms for running timers while the app is not visible (research.md R6). */
public interface TimerAlarmScheduler {
    /** Replaces every scheduled alarm with [alarms]; exact when the app may, otherwise inexact. */
    public fun schedule(alarms: List<TimerAlarm>)

    public fun cancelAll()
}

/** A short sound and vibration that marks a moment, unlike an alert, which rings until it's stopped (spec 022). */
public enum class CueSound { TICK, STEP, FINISH }

/** Plays a cue at once. What's off stays silent or still. */
public interface CuePlayer {
    public fun play(
        cue: CueSound,
        sound: Boolean,
        vibration: Boolean,
    )
}

/** One cue for later, while the app isn't in front. */
public data class CueAlarm(
    val at: MonotonicInstant,
    val cue: CueSound,
    val sound: Boolean,
    val vibration: Boolean,
)

/** System alarms that play a cue and show nothing (spec 022 FR-008). */
public interface CueAlarmScheduler {
    /** Replaces every scheduled cue with [alarms]; exact when the app may, otherwise they may come late. */
    public fun schedule(alarms: List<CueAlarm>)

    public fun cancelAll()
}
