package tech.onova.offlinetranslator.core.domain.ports

/** A page of the phone's own settings, where the person fixes what a tool can't (spec 012 story 2). */
public enum class SettingsPage {
    /** Wi-Fi, for a download that waits for it. */
    NETWORK,

    /** Storage, for a download that needs more free space. */
    STORAGE,
}

/** Opens the phone's own settings pages; nothing is changed by the app. */
public fun interface SettingsPages {
    public fun open(page: SettingsPage)
}
