package tech.onova.offlinetranslator.e2e

import android.os.SystemClock
import org.junit.Assume.assumeTrue
import kotlin.test.fail

/** Where scripts/ci.sh keeps local testing's packs, out of the wipe's reach. */
internal const val LOCAL_PACKS = "/data/local/tmp/offlinetranslator-local-testing"

/**
 * The wipe also deletes local testing's packs (they live in the app's external files): they're put back, then the app
 * starts again, so the store reads them whole (with the speech packs, about 900 MB: the store asked while they were
 * still being copied, and called every pack unavailable).
 */
internal fun AppDriver.restoreLocalPacks() {
    val kept = shell("ls $LOCAL_PACKS").trim()
    assumeTrue("The app wasn't installed through local testing: run scripts/ci.sh e2e", kept.isNotEmpty())
    val files = "/sdcard/Android/data/$APP_PACKAGE/files"
    shell("mkdir -p $files")
    shell("cp -r $LOCAL_PACKS $files/local_testing")
    restart()
}

internal fun AppDriver.airplaneMode(on: Boolean) {
    shell("cmd connectivity airplane-mode ${if (on) "enable" else "disable"}")
}

/** Which of [texts] the screen shows first. */
internal fun AppDriver.firstShown(
    texts: List<String>,
    timeoutMillis: Long,
): String {
    val deadline = SystemClock.uptimeMillis() + timeoutMillis
    while (SystemClock.uptimeMillis() < deadline) {
        texts.firstOrNull { isShown(it) }?.let { return it }
        SystemClock.sleep(POLL_MILLIS)
    }
    fail("None of $texts appeared.\n${screen()}")
}

/** Waits until none of [texts] is on the screen. */
internal fun AppDriver.seeNoneOf(
    texts: List<String>,
    timeoutMillis: Long,
) {
    val deadline = SystemClock.uptimeMillis() + timeoutMillis
    while (SystemClock.uptimeMillis() < deadline) {
        if (texts.none { isShown(it) }) return
        SystemClock.sleep(POLL_MILLIS)
    }
    fail("Still on screen: ${texts.filter { isShown(it) }}.\n${screen()}")
}

private const val POLL_MILLIS = 250L
