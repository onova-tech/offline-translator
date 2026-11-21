package tech.onova.offlinetranslator.core.domain.catalog

import kotlin.test.Test
import kotlin.test.assertEquals

class ToolAvailabilityTest {
    private fun descriptor(vararg required: HardwareRequirement) =
        ToolDescriptor(id = ToolId("sample"), category = ToolCategory.MEASURE, requiredHardware = required.toSet())

    @Test
    fun toolWithoutRequirementsIsAlwaysAvailable() {
        assertEquals(ToolAvailability.Available, descriptor().availability(present = emptySet()))
    }

    @Test
    fun toolIsAvailableWhenEveryRequirementIsPresent() {
        val present = setOf(HardwareRequirement.MAGNETOMETER, HardwareRequirement.ACCELEROMETER)
        assertEquals(ToolAvailability.Available, descriptor(HardwareRequirement.MAGNETOMETER).availability(present))
    }

    @Test
    fun unavailableToolReportsExactlyTheMissingHardware() {
        val availability =
            descriptor(HardwareRequirement.MAGNETOMETER, HardwareRequirement.CAMERA)
                .availability(present = setOf(HardwareRequirement.CAMERA))
        assertEquals(ToolAvailability.Unavailable(missing = setOf(HardwareRequirement.MAGNETOMETER)), availability)
    }
}
