package tech.onova.offlinetranslator.feature.toolstranslate.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport

/**
 * What the phone's speech service says about a language, read across checks, keyed by speech tag (`ja-JP`). One for
 * the process: the Manage languages page and both modes read it, so they agree (owner's phone pass, 2026-11-20).
 * [now] is a monotonic clock in milliseconds; its windows are in time, so many readers don't shorten them.
 *
 * - The service drops a language from "being downloaded" a moment before it lists it as installed (the owner's
 *   phone, 2026-11-19: 48 ms), so for [FINISHING_MILLIS] after a download was last seen, a language that reads "can
 *   download" is still finishing, and its notice doesn't offer the download again.
 * - After the person asked for a language ([asked]), it reads as on its way for [ASKED_MILLIS], while the phone
 *   starts. A language asked for that never gets installed is [RecognitionSupport.NOT_INSTALLED] (the owner's phone,
 *   2026-11-20: a speech service last updated in 2024 downloaded Japanese, couldn't use it, and went on saying "can
 *   download"), rather than the same download offered forever. [forget] offers it again.
 */
internal class SpeechDownloads(
    private val now: () -> Long,
) {
    private val finishingUntil = mutableMapOf<String, Long>()
    private val startingUntil = mutableMapOf<String, Long>()
    private val asked = mutableSetOf<String>()
    private val refused = mutableSetOf<String>()
    private val requestCount = MutableStateFlow(0)

    /** How many languages were asked for: a screen that shows speech asks the phone again and watches. */
    val requests: StateFlow<Int> = requestCount.asStateFlow()

    /** The person asked the phone's speech service for [tag]. */
    fun asked(tag: String) {
        asked += tag
        refused -= tag
        startingUntil[tag] = now() + ASKED_MILLIS
        requestCount.value += 1
    }

    /**
     * Whether the phone was asked for [tag] and its download ended without it (the owner's Japanese, 2026-11-20): the
     * router then offers the app's recognition (spec 082, 2026-11-21). It only reads; the screens' [read] keeps the
     * rest.
     */
    fun neverInstalls(tag: String): Boolean {
        if (tag in refused) return true
        if (tag !in asked) return false
        val until = listOfNotNull(finishingUntil[tag], startingUntil[tag]).maxOrNull()
        return until == null || now() >= until
    }

    /** One of the app's own speech packs changed (spec 082): the screens that show speech ask again. */
    fun appSpeechChanged() {
        requestCount.value += 1
    }

    /** Back from elsewhere (the store, the settings): a language the phone didn't install can be asked for again. */
    fun forget() {
        asked -= refused
        refused.clear()
    }

    fun read(
        tag: String,
        support: RecognitionSupport,
    ): RecognitionSupport =
        when (support) {
            RecognitionSupport.DOWNLOADING -> {
                finishingUntil[tag] = now() + FINISHING_MILLIS
                startingUntil -= tag
                support
            }

            RecognitionSupport.CAN_DOWNLOAD -> {
                canDownload(tag)
            }

            // The app's speech (spec 082, routed before the screens read it): what was learned about the phone stays,
            // so a language the phone never installs keeps going to the app.
            RecognitionSupport.APP_CAN_DOWNLOAD,
            RecognitionSupport.APP_DOWNLOADING,
            -> {
                support
            }

            else -> {
                finishingUntil -= tag
                startingUntil -= tag
                asked -= tag
                refused -= tag
                support
            }
        }

    /** [support] for one language's [tag], read the same way. */
    fun read(
        tag: String,
        support: SideSupport,
    ): SideSupport = support.copy(recognition = read(tag, support.recognition))

    private fun canDownload(tag: String): RecognitionSupport {
        val until = listOfNotNull(finishingUntil[tag], startingUntil[tag]).maxOrNull()
        return when {
            until != null && now() < until -> {
                RecognitionSupport.DOWNLOADING
            }

            tag in asked -> {
                finishingUntil -= tag
                startingUntil -= tag
                refused += tag
                RecognitionSupport.NOT_INSTALLED
            }

            else -> {
                finishingUntil -= tag
                startingUntil -= tag
                RecognitionSupport.CAN_DOWNLOAD
            }
        }
    }

    companion object {
        /** Longer than the gap before "installed", short enough that a stuck download is offered again. */
        const val FINISHING_MILLIS: Long = 15_000

        /** For the phone to start a download it was asked for, its own dialog included. */
        const val ASKED_MILLIS: Long = 30_000
    }
}
