package tech.onova.offlinetranslator.core.domain.ports

/**
 * Tips that show once (spec 045), such as the Converter's touch and hold: the first ask for a key says yes and notes
 * it, every later ask says no. Kept on the phone only; a data file doesn't carry them.
 */
public fun interface SeenHints {
    public suspend fun firstTime(key: String): Boolean
}

/**
 * Notices the person must acknowledge once, such as the first-launch privacy notice (spec 078): unlike a tip, one
 * counts only after "Got it", so a notice closed by the system comes back. Kept on the phone only; a data file doesn't
 * carry them, so importing one can't bring a notice back.
 */
public interface AcknowledgedNotices {
    public suspend fun isAcknowledged(key: String): Boolean

    public suspend fun acknowledge(key: String)
}
