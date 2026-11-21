package tech.onova.offlinetranslator.core.domain.ports

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** One position from the satellite receiver (spec 008 contracts/ports.md). Never stored or sent. */
public data class GpsFix(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    /** True when the altitude is above sea level; false when it is the GPS ellipsoid height. */
    val altitudeIsSeaLevel: Boolean,
    val speedMetersPerSecond: Float?,
    val bearingDegrees: Float?,
    val horizontalAccuracyMeters: Float?,
    /** Monotonic time of the fix. */
    val elapsedRealtimeNanos: Long,
    /** Wall-clock time of the fix, for display only. */
    val epochMillis: Long,
)

/** The satellite systems a receiver may report. */
public enum class Constellation { GPS, GLONASS, GALILEO, BEIDOU, QZSS, SBAS, IRNSS, UNKNOWN }

/** The satellites the receiver sees now (spec 008 research R7). */
public data class SatelliteStatus(
    val inView: Int,
    val usedInFix: Int,
    val byConstellation: Map<Constellation, Int>,
    val averageSignalDbHz: Float?,
)

/** The phone's satellite receiver; the network and fused providers are never used (Principle I). */
public interface LocationSource {
    /** False while location is switched off in the phone's settings. */
    public val isGpsEnabled: StateFlow<Boolean>

    /**
     * Cold flow of satellite-provider fixes only (never the network or fused provider). Registered
     * when collected, removed when the collector is cancelled. Completes if the permission is lost.
     */
    public fun fixes(): Flow<GpsFix>

    /** Cold flow of the satellite status, registered while collected. */
    public fun satellites(): Flow<SatelliteStatus>

    /** Opens the phone's location settings. PolyTools never switches location on itself. */
    public fun openLocationSettings()
}

/** The Earth's magnetic declination at a place, from a model the phone already has (spec 024). */
public fun interface MagneticDeclination {
    public fun degreesAt(
        latitude: Double,
        longitude: Double,
        altitudeMeters: Double,
        epochMillis: Long,
    ): Double
}
