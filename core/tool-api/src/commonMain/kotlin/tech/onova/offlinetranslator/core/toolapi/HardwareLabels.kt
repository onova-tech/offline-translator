package tech.onova.offlinetranslator.core.toolapi

import org.jetbrains.compose.resources.StringResource
import tech.onova.offlinetranslator.core.domain.catalog.HardwareRequirement
import tech.onova.offlinetranslator.core.toolapi.resources.Res
import tech.onova.offlinetranslator.core.toolapi.resources.hardware_accelerometer
import tech.onova.offlinetranslator.core.toolapi.resources.hardware_camera
import tech.onova.offlinetranslator.core.toolapi.resources.hardware_flash
import tech.onova.offlinetranslator.core.toolapi.resources.hardware_front_camera
import tech.onova.offlinetranslator.core.toolapi.resources.hardware_gps
import tech.onova.offlinetranslator.core.toolapi.resources.hardware_gyroscope
import tech.onova.offlinetranslator.core.toolapi.resources.hardware_light_sensor
import tech.onova.offlinetranslator.core.toolapi.resources.hardware_magnetometer
import tech.onova.offlinetranslator.core.toolapi.resources.hardware_microphone
import tech.onova.offlinetranslator.core.toolapi.resources.hardware_nfc
import tech.onova.offlinetranslator.core.toolapi.resources.hardware_step_counter
import tech.onova.offlinetranslator.core.toolapi.resources.hardware_vibrator

/** Localized name of a piece of hardware, for "not available" explanations (spec FR-006). */
public val HardwareRequirement.label: StringResource
    get() =
        when (this) {
            HardwareRequirement.ACCELEROMETER -> Res.string.hardware_accelerometer
            HardwareRequirement.GYROSCOPE -> Res.string.hardware_gyroscope
            HardwareRequirement.MAGNETOMETER -> Res.string.hardware_magnetometer
            HardwareRequirement.LIGHT_SENSOR -> Res.string.hardware_light_sensor
            HardwareRequirement.STEP_COUNTER -> Res.string.hardware_step_counter
            HardwareRequirement.CAMERA -> Res.string.hardware_camera
            HardwareRequirement.FRONT_CAMERA -> Res.string.hardware_front_camera
            HardwareRequirement.FLASH -> Res.string.hardware_flash
            HardwareRequirement.MICROPHONE -> Res.string.hardware_microphone
            HardwareRequirement.GPS -> Res.string.hardware_gps
            HardwareRequirement.NFC -> Res.string.hardware_nfc
            HardwareRequirement.VIBRATOR -> Res.string.hardware_vibrator
        }
