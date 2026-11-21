package tech.onova.offlinetranslator.feature.toolstranslate.domain

import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What the phone's speech service says about a language, read across checks by every Translator screen at once (the
 * Manage languages page and both modes share one, owner's phone pass 2026-11-20), its windows in time.
 */
class SpeechDownloadsTest {
    private var now = 0L
    private val downloads = SpeechDownloads { now }

    private fun later(millis: Long) {
        now += millis
    }

    /**
     * The owner's phone, 2026-11-19: Android's speech service dropped Spanish from "being downloaded" 48 ms before it
     * listed it as installed, and the notice offered the download again in between.
     */
    @Test
    fun aLanguageJustDownloadedIsStillFinishingRatherThanOfferedAgain() {
        assertEquals(RecognitionSupport.CAN_DOWNLOAD, downloads.read("es-ES", RecognitionSupport.CAN_DOWNLOAD))
        assertEquals(RecognitionSupport.DOWNLOADING, downloads.read("es-ES", RecognitionSupport.DOWNLOADING))
        later(SECOND)
        assertEquals(RecognitionSupport.DOWNLOADING, downloads.read("es-ES", RecognitionSupport.CAN_DOWNLOAD))
        assertEquals(RecognitionSupport.READY, downloads.read("es-ES", RecognitionSupport.READY))
    }

    @Test
    fun aDownloadThatNeverArrivesIsOfferedAgainOnceItsWindowEnds() {
        downloads.read("es-ES", RecognitionSupport.DOWNLOADING)
        later(SpeechDownloads.FINISHING_MILLIS - 1)
        assertEquals(RecognitionSupport.DOWNLOADING, downloads.read("es-ES", RecognitionSupport.CAN_DOWNLOAD))
        later(1)
        assertEquals(RecognitionSupport.CAN_DOWNLOAD, downloads.read("es-ES", RecognitionSupport.CAN_DOWNLOAD))
    }

    /**
     * The owner's phone, 2026-11-20: its speech service, last updated in 2024, downloaded Japanese but couldn't use the
     * new package, so it went on saying "can download"; asked again, it did nothing. Asked for and never installed, the
     * language reads "not installed" instead of offering the same download forever.
     */
    @Test
    fun aLanguageAskedForThatNeverInstallsIsNotInstalledRatherThanOfferedAgain() {
        downloads.asked("ja-JP")
        later(SpeechDownloads.ASKED_MILLIS - 1)
        assertEquals(RecognitionSupport.DOWNLOADING, downloads.read("ja-JP", RecognitionSupport.CAN_DOWNLOAD))
        later(1)
        assertEquals(RecognitionSupport.NOT_INSTALLED, downloads.read("ja-JP", RecognitionSupport.CAN_DOWNLOAD))
        assertEquals(RecognitionSupport.NOT_INSTALLED, downloads.read("ja-JP", RecognitionSupport.CAN_DOWNLOAD))
    }

    /** Two screens reading it every few seconds don't shorten the window, as counting checks did. */
    @Test
    fun manyReadsDontShortenTheWindow() {
        downloads.asked("ja-JP")
        repeat(READS) {
            assertEquals(RecognitionSupport.DOWNLOADING, downloads.read("ja-JP", RecognitionSupport.CAN_DOWNLOAD))
        }
    }

    @Test
    fun aDownloadSeenThatNeverLandsIsNotInstalledWhenItWasAskedFor() {
        downloads.asked("ja-JP")
        downloads.read("ja-JP", RecognitionSupport.DOWNLOADING)
        later(SpeechDownloads.FINISHING_MILLIS)
        assertEquals(RecognitionSupport.NOT_INSTALLED, downloads.read("ja-JP", RecognitionSupport.CAN_DOWNLOAD))
    }

    @Test
    fun notInstalledEndsWhenItArrivesOrWhenAScreenComesBack() {
        downloads.asked("ja-JP")
        later(SpeechDownloads.ASKED_MILLIS)
        downloads.read("ja-JP", RecognitionSupport.CAN_DOWNLOAD)
        // Back from updating the phone's speech service: offered again, so the person can ask once more.
        downloads.forget()
        assertEquals(RecognitionSupport.CAN_DOWNLOAD, downloads.read("ja-JP", RecognitionSupport.CAN_DOWNLOAD))
        downloads.asked("ja-JP")
        later(SpeechDownloads.ASKED_MILLIS)
        downloads.read("ja-JP", RecognitionSupport.CAN_DOWNLOAD)
        assertEquals(RecognitionSupport.READY, downloads.read("ja-JP", RecognitionSupport.READY))
        assertEquals(RecognitionSupport.CAN_DOWNLOAD, downloads.read("ja-JP", RecognitionSupport.CAN_DOWNLOAD))
    }

    @Test
    fun askingAgainAfterAnOldDownloadWaitsItsFullWindow() {
        downloads.read("ja-JP", RecognitionSupport.DOWNLOADING)
        later(SpeechDownloads.FINISHING_MILLIS)
        downloads.read("ja-JP", RecognitionSupport.CAN_DOWNLOAD)
        downloads.asked("ja-JP")
        assertEquals(RecognitionSupport.DOWNLOADING, downloads.read("ja-JP", RecognitionSupport.CAN_DOWNLOAD))
    }

    @Test
    fun eachLanguageCountsOnItsOwn() {
        downloads.read("es-ES", RecognitionSupport.DOWNLOADING)
        assertEquals(RecognitionSupport.CAN_DOWNLOAD, downloads.read("en-US", RecognitionSupport.CAN_DOWNLOAD))
        assertEquals(RecognitionSupport.UNSUPPORTED, downloads.read("es-ES", RecognitionSupport.UNSUPPORTED))
        assertEquals(RecognitionSupport.CAN_DOWNLOAD, downloads.read("es-ES", RecognitionSupport.CAN_DOWNLOAD))
    }

    private companion object {
        const val SECOND = 1_000L
        const val READS = 100
    }
}
