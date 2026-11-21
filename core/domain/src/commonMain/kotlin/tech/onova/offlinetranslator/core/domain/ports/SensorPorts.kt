package tech.onova.offlinetranslator.core.domain.ports

import kotlinx.coroutines.flow.Flow

/** The sensors the motion tools read (spec 005 research.md R2). */
public enum class SensorKind { ACCELEROMETER, LINEAR_ACCELERATION, MAGNETIC_FIELD, STEP_COUNTER, LIGHT }

public enum class SensorAccuracy { UNRELIABLE, LOW, MEDIUM, HIGH }

/**
 * One reading. Units: m/s² for ACCELEROMETER and LINEAR_ACCELERATION, µT for MAGNETIC_FIELD.
 * For STEP_COUNTER, [x] is the cumulative step count since the phone started; [y] and [z] are 0.
 */
public data class SensorSample(
    val x: Float,
    val y: Float,
    val z: Float,
    val accuracy: SensorAccuracy,
    val timestampNanos: Long,
)

public interface SensorSource {
    public fun isAvailable(kind: SensorKind): Boolean

    /**
     * Cold flow: the sensor is registered when the flow is collected and released when the
     * collector is cancelled. Never completes. Emits nothing if the sensor is missing.
     */
    public fun samples(kind: SensorKind): Flow<SensorSample>

    /**
     * Like [samples], but as fast as Android allows without a permission, about 200 a second, for a vibration's
     * frequency (spec 058). A source that has no faster rate gives its usual one.
     */
    public fun fastSamples(kind: SensorKind): Flow<SensorSample> = samples(kind)
}

/** Keeps the screen on while a tool that is watched continuously is open. */
public interface ScreenAwake {
    public fun keepOn(on: Boolean)
}
