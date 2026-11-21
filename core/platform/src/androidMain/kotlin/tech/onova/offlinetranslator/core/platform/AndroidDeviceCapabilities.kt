package tech.onova.offlinetranslator.core.platform

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import tech.onova.offlinetranslator.core.domain.catalog.HardwareRequirement
import tech.onova.offlinetranslator.core.domain.ports.DeviceCapabilities

/** Detects sensors and features once; no permission is needed for any of these queries. */
public class AndroidDeviceCapabilities(
    context: Context,
) : DeviceCapabilities {
    private val appContext = context.applicationContext

    override val supportsDynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    override val hasVibrator: Boolean by lazy { vibrator()?.hasVibrator() == true }

    override val presentHardware: Set<HardwareRequirement> by lazy { detectHardware() }

    private fun detectHardware(): Set<HardwareRequirement> {
        val sensors = appContext.getSystemService(SensorManager::class.java)
        val packages = appContext.packageManager

        fun hasSensor(type: Int) = sensors?.getDefaultSensor(type) != null
        return buildSet {
            if (hasSensor(Sensor.TYPE_ACCELEROMETER)) add(HardwareRequirement.ACCELEROMETER)
            if (hasSensor(Sensor.TYPE_GYROSCOPE)) add(HardwareRequirement.GYROSCOPE)
            if (hasSensor(Sensor.TYPE_MAGNETIC_FIELD)) add(HardwareRequirement.MAGNETOMETER)
            if (hasSensor(Sensor.TYPE_LIGHT)) add(HardwareRequirement.LIGHT_SENSOR)
            if (hasSensor(Sensor.TYPE_STEP_COUNTER)) add(HardwareRequirement.STEP_COUNTER)
            if (packages.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) add(HardwareRequirement.CAMERA)
            if (packages.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) add(HardwareRequirement.FRONT_CAMERA)
            if (packages.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) add(HardwareRequirement.FLASH)
            if (packages.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) add(HardwareRequirement.MICROPHONE)
            if (packages.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) add(HardwareRequirement.GPS)
            if (packages.hasSystemFeature(PackageManager.FEATURE_NFC)) add(HardwareRequirement.NFC)
            if (hasVibrator) add(HardwareRequirement.VIBRATOR)
        }
    }

    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION") // VibratorManager only exists from API 31.
            appContext.getSystemService(Vibrator::class.java)
        }
}
