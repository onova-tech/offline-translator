package tech.onova.offlinetranslator.core.platform.sensors

import android.view.WindowManager
import tech.onova.offlinetranslator.core.domain.ports.ScreenAwake
import tech.onova.offlinetranslator.core.platform.CurrentActivityTracker

/** Keeps the resumed activity's screen on (spec 005 research.md R9). Called from the main thread. */
public class AndroidScreenAwake(
    private val activities: CurrentActivityTracker,
) : ScreenAwake {
    override fun keepOn(on: Boolean) {
        val window = activities.current()?.window ?: return
        if (on) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
