package tech.onova.offlinetranslator.core.domain.catalog

/** Whether a tool can run on this device (spec FR-006). */
public sealed interface ToolAvailability {
    public data object Available : ToolAvailability

    public data class Unavailable(
        val missing: Set<HardwareRequirement>,
    ) : ToolAvailability
}

public fun ToolDescriptor.availability(present: Set<HardwareRequirement>): ToolAvailability {
    val missing = requiredHardware - present
    return if (missing.isEmpty()) ToolAvailability.Available else ToolAvailability.Unavailable(missing)
}
