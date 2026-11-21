package tech.onova.offlinetranslator.core.designsystem.tokens

/**
 * Animation durations. Compose scales animations by the system animator duration scale, so the
 * "remove animations" setting makes them instant (Principle X).
 */
public object PocketMotion {
    public const val SHORT_MILLIS: Int = 150
    public const val MEDIUM_MILLIS: Int = 300

    /** One step of a [tech.onova.offlinetranslator.core.designsystem.components.WaitingText]'s dots (owner, 2026-11-19). */
    public const val WAITING_STEP_MILLIS: Int = 1_000
}
