package tech.onova.offlinetranslator.core.domain.catalog

/** Hardware a tool may need. A tool is unavailable when any of its requirements is missing. */
public enum class HardwareRequirement {
    ACCELEROMETER,
    GYROSCOPE,
    MAGNETOMETER,
    LIGHT_SENSOR,
    STEP_COUNTER,
    CAMERA,
    FRONT_CAMERA,
    FLASH,
    MICROPHONE,
    GPS,
    NFC,
    VIBRATOR,
}
