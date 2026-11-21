package tech.onova.offlinetranslator.platform

import android.content.pm.PackageManager
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import tech.onova.offlinetranslator.core.domain.catalog.HardwareRequirement
import tech.onova.offlinetranslator.core.platform.AndroidDeviceCapabilities
import tech.onova.offlinetranslator.support.TestApplication
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Spec 006 (research R11): the Mirror tool needs a front camera, and the phone's features say whether it has one. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = TestApplication::class)
class AndroidDeviceCapabilitiesTest {
    @Test
    fun aPhoneWithAFrontCameraReportsIt() {
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app.packageManager).setSystemFeature(PackageManager.FEATURE_CAMERA_FRONT, true)
        assertTrue(HardwareRequirement.FRONT_CAMERA in AndroidDeviceCapabilities(app).presentHardware)
    }

    @Test
    fun aPhoneWithoutAFrontCameraDoesNotReportIt() {
        val app = RuntimeEnvironment.getApplication()
        assertFalse(HardwareRequirement.FRONT_CAMERA in AndroidDeviceCapabilities(app).presentHardware)
    }
}
