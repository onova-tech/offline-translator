package tech.onova.offlinetranslator.e2e

import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import java.io.ByteArrayOutputStream
import kotlin.test.fail

internal const val APP_PACKAGE = "tech.onova.offlinetranslator"
private const val ACTIVITY = "$APP_PACKAGE/.MainActivity"
private const val EDIT_TEXT = "android.widget.EditText"
private const val HORIZONTAL_LIST = "android.widget.HorizontalScrollView"
private const val SIDEWAYS_RATIO = 2
private const val POLL_MILLIS = 250L
private const val MAX_SCROLLS = 40
private const val SCROLL_AMOUNT = 0.7f
private const val KEY_EFFECT_MILLIS = 6_000L
private const val LOG_TAG = "AppDriver"
private const val LOG_CHUNK = 3_500
private const val INPUT_DUMP_CHARS = 12_000

/** The longest a screen may take on the 2-core development machine with a software-rendered emulator. */
internal const val WAIT_MILLIS = 20_000L

/** How long to watch for something that must not appear. */
internal const val SETTLE_MILLIS = 2_000L

/**
 * Drives the installed app the way a person does: it only sees and taps what's on the screen, in English. Nothing here
 * waits for the screen to go idle: [see] polls, and an idle wait can outlast a short-lived message.
 */
internal class AppDriver(
    val device: UiDevice,
) {
    fun shell(command: String): String = device.executeShellCommand(command)

    // Starting and stopping.

    /** Starts the app, which opens on the Translator (spec 081). [fresh] is kept for the journeys' first start. */
    @Suppress("UNUSED_PARAMETER")
    fun launch(fresh: Boolean = false) {
        shell("am start -W -n $ACTIVITY")
        see(HOME_MARK)
    }

    fun restart() {
        shell("am force-stop $APP_PACKAGE")
        launch()
    }

    /** What a reinstall leaves: nothing. The tests refuse to run outside an emulator, so this can't reach a phone. */
    fun wipe() {
        // First: changing the language restarts the app. No list means "follow the phone"; this runs without a shell,
        // so quotes would arrive as a language.
        shell("cmd locale set-app-locales $APP_PACKAGE")
        shell("am force-stop $APP_PACKAGE")
        shell("pm clear $APP_PACKAGE")
    }

    // Reading the screen.

    private fun <T : Any> poll(
        timeoutMillis: Long,
        find: () -> T?,
    ): T? {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (true) {
            val found =
                try {
                    find()
                } catch (ignored: StaleObjectException) {
                    null // The screen changed under the search; look again.
                }
            if (found != null) return found
            if (SystemClock.uptimeMillis() >= deadline) return null
            SystemClock.sleep(POLL_MILLIS)
        }
    }

    private fun findVisible(text: String): UiObject2? =
        device.findObject(By.textContains(text)) ?: device.findObject(By.descContains(text))

    fun isShown(text: String): Boolean = findVisible(text) != null

    /** Text or a spoken description containing [text]. */
    fun see(
        text: String,
        timeoutMillis: Long = WAIT_MILLIS,
    ): UiObject2 = poll(timeoutMillis) { findVisible(text) } ?: fail("'$text' never appeared.\n${screen()}")

    fun seeGone(
        text: String,
        timeoutMillis: Long = WAIT_MILLIS,
    ) {
        poll(timeoutMillis) { true.takeIf { !isShown(text) } } ?: fail("'$text' never went away.\n${screen()}")
    }

    /** Watches for a while, so a result that arrives late can't slip past. */
    fun neverSee(
        text: String,
        forMillis: Long = SETTLE_MILLIS,
    ) {
        if (poll(forMillis) { findVisible(text) } != null) fail("'$text' must not appear.\n${screen()}")
    }

    /** Every text and description on screen, for a failure message. */
    fun screen(): String {
        val out = ByteArrayOutputStream()
        device.dumpWindowHierarchy(out)
        val labels = Regex("""(?:text|content-desc)="([^"]+)"""").findAll(out.toString()).map { it.groupValues[1] }
        return "In front: ${device.currentPackageName}. On screen: " + labels.joinToString(" | ")
    }

    fun windowHierarchy(): String {
        val out = ByteArrayOutputStream()
        device.dumpWindowHierarchy(out)
        return out.toString()
    }

    // Acting on it.

    private fun exact(text: String): BySelector = By.text(text)

    /** What the last [tap] hit and where, for a key that had no effect. */
    private var lastTap = ""

    /** Taps the exact [text] or description; the last match, since an open dialog is listed after the screen. */
    fun tap(text: String) {
        val tapped =
            poll(WAIT_MILLIS) {
                val matches = device.findObjects(exact(text)).ifEmpty { device.findObjects(By.desc(text)) }
                val target = matches.lastOrNull()
                if (target != null) {
                    val named = "${target.className} '${target.text}'/'${target.contentDescription}'"
                    lastTap =
                        "${matches.size} match(es); tapped $named at ${target.visibleBounds}, " +
                        "enabled=${target.isEnabled}, at ${SystemClock.uptimeMillis()}"
                }
                target?.click()
                target
            }
        tapped ?: fail("Nothing to tap called '$text'.\n${screen()}")
    }

    /** Taps by spoken description only: a field's label or a row's text can read like the button or check box. */
    fun tapDescribed(description: String) {
        poll(WAIT_MILLIS) { device.findObjects(By.desc(description)).lastOrNull()?.also { it.click() } }
            ?: fail("Nothing described as '$description'.\n${screen()}")
    }

    /** Waits for a check box, described as [description], to be checked. */
    fun seeChecked(description: String) {
        poll(WAIT_MILLIS) { device.findObject(By.desc(description).checked(true)) }
            ?: fail("'$description' isn't checked.\n${screen()}")
    }

    /**
     * For a key whose effect shows on screen: taps it again if nothing happened. On the emulator this journey lost one
     * tap in about 400, the same key in two full runs, always delivered to the right place, and never reproduced alone.
     * The rules behind the keys have their own tests; the second attempt is logged, so it can still be counted.
     */
    fun tapKeyUntilSeen(
        key: String,
        shown: String,
    ) {
        tap(key)
        if (poll(KEY_EFFECT_MILLIS) { findVisible(shown) } != null) return
        Log.w(LOG_TAG, "The key '$key' had no effect after ${KEY_EFFECT_MILLIS}ms; tapping it again.")
        // The evidence for the cause, still open: what was hit, the screen now, and what the system did with the touch.
        logLong("Lost tap: $lastTap")
        logLong("Lost tap, the screen now: ${screen()}")
        logLong(
            "Lost tap, input: " +
                shell("dumpsys input").substringAfter("Input Dispatcher State:").take(INPUT_DUMP_CHARS),
        )
        tap(key)
        see(shown)
    }

    /** Logcat cuts a line at about 4,000 characters. */
    private fun logLong(text: String) {
        text.chunked(LOG_CHUNK).forEach { Log.w(LOG_TAG, it) }
    }

    private fun verticalList(): UiObject2? =
        device
            .findObjects(By.scrollable(true))
            .filterNot { it.isSideways() }
            .maxByOrNull { it.visibleBounds.height() }

    /** The rows on screen that scroll sideways, top first. */
    private fun sidewaysRows(): List<UiObject2> =
        device.findObjects(By.scrollable(true)).filter { it.isSideways() }.sortedBy { it.visibleBounds.top }

    /** Scrolls the screen's list down until [text] shows, then taps it. */
    fun scrollAndTap(text: String) {
        scrollTo(text)
        tap(text)
    }

    fun scrollTo(text: String) {
        repeat(MAX_SCROLLS) {
            if (isShown(text)) return
            verticalList()?.scroll(Direction.DOWN, SCROLL_AMOUNT) ?: fail("No list to scroll.\n${screen()}")
        }
        see(text, SETTLE_MILLIS)
    }

    /**
     * Home's list back at its top, where the search field and the category row are. What `scroll` says about scrolling
     * further isn't reliable on a Compose list, so the screen itself tells when the top is reached.
     */
    fun scrollToTop() {
        repeat(MAX_SCROLLS) {
            if (device.hasObject(By.clazz(EDIT_TEXT)) || sidewaysRows().isNotEmpty()) return
            verticalList()?.scroll(Direction.UP, 1f) ?: return
        }
    }

    /** Scrolls the top sideways row, such as the converter's categories, until [text] shows, then taps it. */
    fun tapInRow(text: String) {
        repeat(MAX_SCROLLS) {
            if (device.hasObject(exact(text))) {
                tap(text)
                return
            }
            sidewaysRows().firstOrNull()?.scroll(Direction.RIGHT, SCROLL_AMOUNT)
                ?: fail("No sideways row.\n${screen()}")
        }
        fail("'$text' isn't in the row.\n${screen()}")
    }

    fun field(label: String): UiObject2 =
        poll(WAIT_MILLIS) { device.findObject(By.clazz(EDIT_TEXT).hasDescendant(exact(label))) }
            ?: fail("No field called '$label'.\n${screen()}")

    /** Fills a field through the accessibility action, so the on-screen keyboard stays down. */
    fun type(
        label: String,
        value: String,
    ) {
        field(label).text = value
    }

    /** The system's permission dialog. By its id, because some Android images write its buttons in capitals. */
    fun allowWhileUsingTheApp() {
        val allow = By.res("com.android.permissioncontroller", "permission_allow_foreground_only_button")
        poll(WAIT_MILLIS) { device.findObject(allow)?.also { it.click() } }
            ?: fail("No system permission dialog.\n${screen()}")
    }

    fun pressPaste() {
        device.pressKeyCode(KeyEvent.KEYCODE_PASTE)
    }

    fun pressBack() {
        device.pressBack()
    }

    // The Translator, where the app opens, and Settings behind its gear (spec 081).

    /** Back to the Translator, whatever was open on top of it. */
    fun backToHome() {
        repeat(MAX_BACKS) {
            if (isOnHome()) return
            pressBack()
            // Waits out the transition: one more Back on the Translator would leave the app.
            poll(SETTLE_MILLIS) { true.takeIf { isOnHome() } }
        }
        see(SETTINGS, SETTLE_MILLIS)
    }

    /** The Settings gear only exists on the Translator. */
    private fun isOnHome(): Boolean = device.hasObject(By.desc(SETTINGS)) && !device.hasObject(By.desc(BACK))

    /** The app has one tool, so opening it is going back to it. */
    @Suppress("UNUSED_PARAMETER")
    fun openTool(name: String) {
        backToHome()
    }

    fun openSettings() {
        tap(SETTINGS)
        see("Appearance")
    }

    companion object {
        const val HOME_MARK = "Type or speak"
        const val SETTINGS = "Settings"
        const val BACK = "Back"
        private const val MAX_BACKS = 4
    }
}

/**
 * A row that scrolls sideways, such as a row of modes. Compose names a segmented row's scroll a plain View, since
 * the row is a selectable group, so the shape tells it from a list: far wider than tall.
 */
internal fun UiObject2.isSideways(): Boolean =
    className == HORIZONTAL_LIST || visibleBounds.let { it.width() > it.height() * SIDEWAYS_RATIO }
