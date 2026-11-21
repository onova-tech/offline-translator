package tech.onova.offlinetranslator.core.domain.ports

/** Copies text to the system clipboard (spec 004 research.md R13). */
public fun interface ClipboardWriter {
    /** Returns true when the platform shows its own copy confirmation (Android 13 and later). */
    public fun copy(
        label: String,
        text: String,
    ): Boolean

    /**
     * Copies a password or a key (spec 034): the phone keeps it out of its copy preview and its keyboard's
     * suggestions where it can. Same return as [copy].
     */
    public fun copySecret(
        label: String,
        text: String,
    ): Boolean = copy(label, text)
}
