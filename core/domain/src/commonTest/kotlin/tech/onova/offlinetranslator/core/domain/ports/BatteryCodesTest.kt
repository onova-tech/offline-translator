package tech.onova.offlinetranslator.core.domain.ports

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Spec 009 research R1–R2: Android's raw battery values. */
class BatteryCodesTest {
    @Test
    fun everyStatusMapsAndOthersAreUnknown() {
        assertEquals(BatteryStatus.UNKNOWN, BatteryCodes.status(1))
        assertEquals(BatteryStatus.CHARGING, BatteryCodes.status(2))
        assertEquals(BatteryStatus.DISCHARGING, BatteryCodes.status(3))
        assertEquals(BatteryStatus.NOT_CHARGING, BatteryCodes.status(4))
        assertEquals(BatteryStatus.FULL, BatteryCodes.status(5))
        assertEquals(BatteryStatus.UNKNOWN, BatteryCodes.status(99))
    }

    @Test
    fun everyHealthMapsAndOthersAreUnknown() {
        assertEquals(BatteryHealth.UNKNOWN, BatteryCodes.health(1))
        assertEquals(BatteryHealth.GOOD, BatteryCodes.health(2))
        assertEquals(BatteryHealth.OVERHEAT, BatteryCodes.health(3))
        assertEquals(BatteryHealth.DEAD, BatteryCodes.health(4))
        assertEquals(BatteryHealth.OVER_VOLTAGE, BatteryCodes.health(5))
        assertEquals(BatteryHealth.UNSPECIFIED_FAILURE, BatteryCodes.health(6))
        assertEquals(BatteryHealth.COLD, BatteryCodes.health(7))
        assertEquals(BatteryHealth.UNKNOWN, BatteryCodes.health(-3))
    }

    @Test
    fun thePluggedMaskNamesEverySource() {
        assertEquals(setOf(PowerSource.AC), BatteryCodes.powerSources(1))
        assertEquals(setOf(PowerSource.USB), BatteryCodes.powerSources(2))
        assertEquals(setOf(PowerSource.WIRELESS), BatteryCodes.powerSources(4))
        assertEquals(setOf(PowerSource.DOCK), BatteryCodes.powerSources(8))
        assertEquals(setOf(PowerSource.AC, PowerSource.USB), BatteryCodes.powerSources(3))
        assertEquals(emptySet(), BatteryCodes.powerSources(0))
    }

    @Test
    fun theLevelNeedsAValidLevelAndScale() {
        assertEquals(87, BatteryCodes.levelPercent(87, 100))
        assertEquals(50, BatteryCodes.levelPercent(128, 256))
        assertNull(BatteryCodes.levelPercent(-1, 100))
        assertNull(BatteryCodes.levelPercent(50, 0))
    }

    @Test
    fun theTemperatureIsInTenthsAndColdIsKept() {
        assertEquals(36.6f, BatteryCodes.temperatureCelsius(366))
        assertEquals(-5f, BatteryCodes.temperatureCelsius(-50))
        assertEquals(0f, BatteryCodes.temperatureCelsius(0))
        assertNull(BatteryCodes.temperatureCelsius(BatteryCodes.MISSING))
        assertNull(BatteryCodes.temperatureCelsius(-500))
        assertNull(BatteryCodes.temperatureCelsius(1_500))
    }

    @Test
    fun theVoltageIsInMillivoltsAndSomePhonesReportVolts() {
        assertEquals(3_870, BatteryCodes.voltageMillivolts(3_870))
        assertEquals(4_000, BatteryCodes.voltageMillivolts(4))
        assertNull(BatteryCodes.voltageMillivolts(0))
        assertNull(BatteryCodes.voltageMillivolts(-1))
    }

    @Test
    fun aCurrentOfZeroOrTheMissingMarkerIsNotReported() {
        assertEquals(-450_000L, BatteryCodes.currentMicroamps(-450_000L))
        assertNull(BatteryCodes.currentMicroamps(0L))
        assertNull(BatteryCodes.currentMicroamps(Long.MIN_VALUE))
    }

    @Test
    fun aBlankTechnologyIsNotReported() {
        assertEquals("Li-ion", BatteryCodes.technology("Li-ion"))
        assertNull(BatteryCodes.technology(" "))
        assertNull(BatteryCodes.technology(null))
    }
}
