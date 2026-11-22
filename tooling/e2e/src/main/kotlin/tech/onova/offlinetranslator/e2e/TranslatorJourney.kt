package tech.onova.offlinetranslator.e2e

import org.junit.Test

/**
 * Spec 012 T064: a language pack installed through bundletool's local testing (`scripts/ci.sh` installs the app from
 * the bundle and keeps the packs in [LOCAL_PACKS]), then the real engine in airplane mode: a sentence typed, its
 * translation, the swap, Copy; at the end the pack deleted from Settings → Offline languages.
 */
internal class TranslatorJourney : Journey() {
    /** Downloads Spanish; on a network the phone doesn't count as Wi-Fi, "Download only on Wi-Fi" goes off first. */
    private fun downloadSpanish() {
        app.see("To translate Spanish, download its language pack (")
        app.tap("Download")
        val outcome =
            app.firstShown(listOf(WAITING_FOR_WIFI, "Downloading Spanish", "Installing Spanish", READY), PACK_MILLIS)
        if (outcome == WAITING_FOR_WIFI) {
            app.backToHome()
            app.openSettings()
            app.scrollTo("Download only on Wi-Fi")
            app.tap("Download only on Wi-Fi")
            app.backToHome()
            app.openTool("Translator")
            app.tap("Download")
        }
        app.see(READY, timeoutMillis = PACK_MILLIS)
    }

    @Test
    fun aPackInstallsThenTheRealEngineTranslatesInAirplaneMode() {
        app.restoreLocalPacks()
        app.openTool("Translator")
        downloadSpanish()
        app.airplaneMode(on = true)
        try {
            app.type("Text", SENTENCE)
            app.see("estación", timeoutMillis = ENGINE_MILLIS)
            app.tapDescribed("Swap languages")
            app.see("station", timeoutMillis = ENGINE_MILLIS)
            app.tap("Copy")
        } finally {
            app.airplaneMode(on = false)
        }
        app.backToHome()
        app.openSettings()
        // Settings → Offline languages opens Manage languages, where each language's pack is deleted (spec 080).
        app.scrollTo("Manage languages")
        app.tap("Manage languages")
        // In lower case, so the search field doesn't read "Spanish" too: the tap below opens the language's card.
        app.type("Search languages", "spanish")
        app.tap("Spanish")
        app.tapDescribed("More actions for Spanish, Translation")
        app.tap("Delete")
        app.see("Delete Spanish?")
        app.tap("Delete")
        app.see("MB to download")
    }

    private companion object {
        /** Local testing copies about 70 MB and unpacks it. */
        const val PACK_MILLIS = 120_000L

        /** The model loads the first time, on the slow emulator. */
        const val ENGINE_MILLIS = 60_000L

        const val SENTENCE = "Good morning! Where is the nearest train station?"
        const val WAITING_FOR_WIFI = "Connect to Wi-Fi to download Spanish"
        const val READY = "Type, paste or speak a text"
    }
}
