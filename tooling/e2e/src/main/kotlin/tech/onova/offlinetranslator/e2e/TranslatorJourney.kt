package tech.onova.offlinetranslator.e2e

import android.os.SystemClock
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.test.fail

/**
 * Spec 012 T064: a language pack installed through bundletool's local testing (`scripts/ci.sh` installs the app from
 * the bundle and keeps the packs in [LOCAL_PACKS]), then the real engine in airplane mode: a sentence typed, its
 * translation, the swap, Copy; at the end the pack deleted from Settings → Offline languages.
 */
internal class TranslatorJourney : Journey() {
    /** The wipe also deletes local testing's packs (they live in the app's external files): they're put back. */
    private fun restoreLocalPacks() {
        val kept = app.shell("ls $LOCAL_PACKS").trim()
        assumeTrue("The app wasn't installed through local testing: run scripts/ci.sh e2e", kept.isNotEmpty())
        val files = "/sdcard/Android/data/$APP_PACKAGE/files"
        app.shell("mkdir -p $files")
        app.shell("cp -r $LOCAL_PACKS $files/local_testing")
    }

    private fun airplaneMode(on: Boolean) {
        app.shell("cmd connectivity airplane-mode ${if (on) "enable" else "disable"}")
    }

    /** Downloads Spanish; on a network the phone doesn't count as Wi-Fi, "Download only on Wi-Fi" goes off first. */
    private fun downloadSpanish() {
        app.see("To translate Spanish, download its language pack (")
        app.tap("Download")
        val outcome = firstShown(listOf(WAITING_FOR_WIFI, "Downloading Spanish", "Installing Spanish", READY))
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

    /** Which of [texts] the screen shows first. */
    private fun firstShown(texts: List<String>): String {
        val deadline = SystemClock.uptimeMillis() + PACK_MILLIS
        while (SystemClock.uptimeMillis() < deadline) {
            texts.firstOrNull { app.isShown(it) }?.let { return it }
            SystemClock.sleep(POLL)
        }
        fail("None of $texts appeared.\n${app.screen()}")
    }

    @Test
    fun aPackInstallsThenTheRealEngineTranslatesInAirplaneMode() {
        restoreLocalPacks()
        app.openTool("Translator")
        downloadSpanish()
        airplaneMode(on = true)
        try {
            app.type("Text", SENTENCE)
            app.see("estación", timeoutMillis = ENGINE_MILLIS)
            app.tapDescribed("Swap languages")
            app.see("station", timeoutMillis = ENGINE_MILLIS)
            app.tap("Copy")
        } finally {
            airplaneMode(on = false)
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
        const val APP_PACKAGE = "tech.onova.offlinetranslator"

        /** Where scripts/ci.sh keeps local testing's packs, out of the wipe's reach. */
        const val LOCAL_PACKS = "/data/local/tmp/offlinetranslator-local-testing"

        /** Local testing copies about 70 MB and unpacks it. */
        const val PACK_MILLIS = 120_000L

        /** The model loads the first time, on the slow emulator. */
        const val ENGINE_MILLIS = 60_000L

        const val SENTENCE = "Good morning! Where is the nearest train station?"
        const val WAITING_FOR_WIFI = "Connect to Wi-Fi to download Spanish"
        const val READY = "Type, paste or speak a text"
        const val POLL = 250L
    }
}
