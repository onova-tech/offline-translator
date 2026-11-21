package tech.onova.offlinetranslator.core.domain.catalog

/** Platform-independent facts about a tool; localized texts and icons live in the presentation layer. */
public data class ToolDescriptor(
    val id: ToolId,
    val category: ToolCategory,
    val requiredHardware: Set<HardwareRequirement> = emptySet(),
)
