package tech.onova.offlinetranslator.e2e

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File
import kotlin.test.fail

/** Starts each test on a wiped app, fails it if the app crashed, and saves the screen and any crash when it fails. */
internal class JourneyRule : TestWatcher() {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    val app = AppDriver(UiDevice.getInstance(instrumentation))

    fun startFresh() {
        requireEmulator()
        app.device.setOrientationNatural()
        app.shell("logcat -b crash -c")
        StandInFiles.clear(context)
        app.wipe()
        app.launch(fresh = true)
    }

    /** These tests wipe the app. The owner's phone is usually on adb too, and its data must never be touched. */
    private fun requireEmulator() {
        val qemu = app.shell("getprop ro.kernel.qemu").trim() + app.shell("getprop ro.boot.qemu").trim()
        if ('1' !in qemu || Build.HARDWARE !in EMULATOR_HARDWARE) {
            fail("E2E tests run only on an emulator, because they wipe the app's data. This is ${Build.MODEL}.")
        }
    }

    override fun succeeded(description: Description) = failOnAppCrash()

    override fun failed(
        e: Throwable,
        description: Description,
    ) {
        val directory = File(context.getExternalFilesDir(null), "failures").apply { mkdirs() }
        val name = "${description.testClass.simpleName}.${description.methodName}"
        File(directory, "$name.xml").writeText(app.windowHierarchy())
        File(directory, "$name.crash.txt").writeText(app.shell("logcat -b crash -d"))
    }

    override fun finished(description: Description) {
        app.device.setOrientationNatural()
        app.device.unfreezeRotation()
    }

    private fun failOnAppCrash() {
        val crashes = app.shell("logcat -b crash -d")
        if ("Process: $APP_PACKAGE" in crashes) fail("The app crashed during the test:\n$crashes")
    }

    private companion object {
        val EMULATOR_HARDWARE = setOf("ranchu", "goldfish")
    }
}

/** One person's journey through the real app: real storage, real navigation, real process restarts. */
internal abstract class Journey {
    @get:Rule
    val rule = JourneyRule()

    val app: AppDriver get() = rule.app

    /** In @Before rather than the rule, so a failure here stops the test instead of letting it run on regardless. */
    @Before
    fun startFresh() = rule.startFresh()
}
