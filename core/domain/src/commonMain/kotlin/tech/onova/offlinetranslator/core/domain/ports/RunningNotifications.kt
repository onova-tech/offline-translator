package tech.onova.offlinetranslator.core.domain.ports

/** What a running notification's button does (spec 004 FR-046). */
public enum class NoticeCommand { PAUSE, RESUME, LAP, RESET, CANCEL }

/** One button of a running notification, with its label in the app language. */
public data class NoticeAction(
    val command: NoticeCommand,
    val label: String,
)

/**
 * A notification that stays while the stopwatch or a timer runs or is paused (spec 004 FR-046).
 * [key] names it: "stopwatch" or a timer's id. While [running], the system counts the time itself,
 * up from [shownMillis] or, with [countDown], down from it to zero; while paused, [text] shows it.
 */
public data class RunningNotice(
    val key: String,
    val title: String,
    val text: String,
    val shownMillis: Long,
    val running: Boolean,
    val countDown: Boolean,
    val actions: List<NoticeAction>,
)

/** The system's notifications for running stopwatches and timers. */
public interface RunningNotifications {
    /**
     * Shows exactly [notices], updating or removing the ones shown before. [channelName] names
     * their channel in the system settings. Nothing shows while notifications aren't allowed.
     */
    public fun show(
        notices: List<RunningNotice>,
        channelName: String,
    )
}

/** Runs a notification button's command, also when the app was closed. */
public fun interface NoticeCommands {
    public suspend fun run(
        key: String,
        command: NoticeCommand,
    )
}
