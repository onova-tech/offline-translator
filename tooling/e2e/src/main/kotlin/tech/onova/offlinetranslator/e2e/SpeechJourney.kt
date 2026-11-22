package tech.onova.offlinetranslator.e2e

import org.junit.Test

/**
 * Spec 082 T020: the app's own speech, installed through local testing like the language packs ([TranslatorJourney]),
 * then used in airplane mode. The emulator has no voice app and no recognizer of its own, so the app's are used without
 * asking (US3 scenario 3): Spanish's Piper voice reads a translation aloud, and Whisper listens. The emulator's
 * microphone hears silence, so the model loads and the screen says nothing was heard; what it writes down is the
 * owner's phone pass (T022).
 */
internal class SpeechJourney : Journey() {
    /** Downloads Spanish's translation, the app's recognition and Spanish's voice on Manage languages. */
    private fun downloadSpanishWithItsSpeech() {
        app.tapDescribed("To")
        app.tap(MANAGE)
        app.type("Search languages", "spanish")
        app.tap("Spanish")
        app.see("The app can recognize it")
        app.see("The app has a voice for it")
        for (part in listOf("Translation", "Speech recognition", "Voice")) {
            app.tapDescribed("More actions for Spanish, $part")
            app.tap("Download")
        }
        // Each row says "Installed · … MB" once its packs are in (espeak-ng's data with the voice).
        app.seeNoneOf(listOf("to download", "Downloading", "Installing", "Checking", "Waiting"), PACK_MILLIS)
        app.pressBack()
    }

    @Test
    fun theAppsVoiceAndRecognitionWorkInAirplaneMode() {
        app.restoreLocalPacks()
        // Downloads wait for Wi-Fi by default, and the emulator's network may not count as Wi-Fi.
        app.openSettings()
        app.scrollTo("Download only on Wi-Fi")
        app.tap("Download only on Wi-Fi")
        app.backToHome()
        app.shell("pm grant $APP_PACKAGE android.permission.RECORD_AUDIO")
        app.openTool("Translator")
        downloadSpanishWithItsSpeech()
        app.airplaneMode(on = true)
        try {
            app.type("Text", SENTENCE)
            app.see("Buenos", timeoutMillis = ENGINE_MILLIS)
            // The translation read aloud by the app's voice: its Stop shows while it speaks, then goes.
            app.tap("Read aloud")
            app.see("Stop", timeoutMillis = ENGINE_MILLIS)
            app.seeGone("Stop", timeoutMillis = ENGINE_MILLIS)
            // Whisper listens to English: the model loads, hears silence and says so.
            app.tapDescribed("Speak")
            app.see("Nothing was heard", timeoutMillis = ENGINE_MILLIS)
        } finally {
            app.airplaneMode(on = false)
        }
    }

    private companion object {
        const val MANAGE = "Manage languages…"
        const val SENTENCE = "Good morning!"

        /** Local testing copies Whisper (160 MB), a voice and a translation pack, and unpacks them. */
        const val PACK_MILLIS = 300_000L

        /** A model loads the first time, on the slow emulator. */
        const val ENGINE_MILLIS = 90_000L
    }
}
