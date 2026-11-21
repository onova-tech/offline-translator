package tech.onova.offlinetranslator.feature.toolstranslate.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tech.onova.offlinetranslator.core.domain.ports.AppVisibility
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SetupNeeds

/**
 * Asks the phone what its own speech can do for the languages: when asked, and every few seconds while a language is
 * on its way (owner's phone pass, 2026-11-19). The phone downloads a language in the background and gives the app no
 * word when it ends (Android 13), so asking again is the only way the screen moves on by itself. Nothing is asked
 * while the screen is hidden; the screen asks once more when it comes back.
 */
internal class SpeechRecheck(
    private val scope: CoroutineScope,
    private val visibility: AppVisibility,
    /** Asks the phone and keeps the answers. */
    private val ask: suspend () -> Unit,
    /** A language is being downloaded now. */
    private val downloading: () -> Boolean,
    /** A language may still become ready: being downloaded, or asked for and not yet started. */
    private val onItsWay: () -> Boolean,
) {
    private var watching: Job? = null

    /** Asks now; a language being downloaded is then watched until it's ready. */
    suspend fun askNow() {
        ask()
        if (downloading()) watch()
    }

    /**
     * Asks again every [SetupNeeds.RECHECK_MILLIS] while a language is on its way, [SetupNeeds.RECHECK_TIMES] times at
     * most.
     */
    fun watch() {
        if (watching?.isActive == true) return
        watching =
            scope.launch {
                repeat(SetupNeeds.RECHECK_TIMES) {
                    delay(SetupNeeds.RECHECK_MILLIS)
                    if (visibility.isVisible.value) ask()
                    if (!onItsWay()) return@launch
                }
            }
    }
}
