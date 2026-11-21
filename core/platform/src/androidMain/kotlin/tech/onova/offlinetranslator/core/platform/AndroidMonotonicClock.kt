package tech.onova.offlinetranslator.core.platform

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import tech.onova.offlinetranslator.core.domain.ports.MonotonicClock
import tech.onova.offlinetranslator.core.domain.ports.MonotonicInstant

/**
 * `elapsedRealtime` keeps counting in deep sleep and ignores clock or time zone changes; the boot
 * count tells a time from before a restart apart (spec 004 research.md R5).
 */
public class AndroidMonotonicClock(
    context: Context,
) : MonotonicClock {
    private val bootId: Int = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)

    override fun now(): MonotonicInstant = MonotonicInstant(bootId, SystemClock.elapsedRealtime())
}
