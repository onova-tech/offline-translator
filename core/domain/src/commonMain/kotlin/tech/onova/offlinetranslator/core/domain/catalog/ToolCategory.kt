package tech.onova.offlinetranslator.core.domain.catalog

/** Groups shown on Home, in a fixed display order (spec FR-001). */
public enum class ToolCategory(
    public val displayOrder: Int,
) {
    DIY(displayOrder = 1),
    MEASURE(displayOrder = 2),

    /** The tools that use the GPS, and the Compass (owner, 2026-11-13, spec 075). */
    LOCATION(displayOrder = 3),
    LIGHT_AND_CAMERA(displayOrder = 4),
    SOUND(displayOrder = 5),
    CALCULATION(displayOrder = 6),
    TIME(displayOrder = 7),
    HEALTH(displayOrder = 8),
    UTILITIES(displayOrder = 9),
    DEVICE(displayOrder = 10),
}
