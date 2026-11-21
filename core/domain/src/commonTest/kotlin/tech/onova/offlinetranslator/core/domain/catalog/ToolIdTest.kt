package tech.onova.offlinetranslator.core.domain.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ToolIdTest {
    @Test
    fun acceptsLowercaseKebabCaseIds() {
        assertEquals("ruler", ToolId("ruler").value)
        assertEquals("zero-to-100-timer", ToolId("zero-to-100-timer").value)
    }

    @Test
    fun rejectsIdsOutsideThePattern() {
        listOf("", "r", "Ruler", "1ruler", "ruler_tool", "ruler tool", "-ruler", "a".repeat(41)).forEach { invalid ->
            assertFailsWith<IllegalArgumentException>(invalid) { ToolId(invalid) }
        }
    }

    @Test
    fun acceptsTheMaximumLength() {
        assertEquals(40, ToolId("a".repeat(40)).value.length)
    }

    @Test
    fun categoriesFollowTheFixedDisplayOrder() {
        assertEquals(
            listOf(
                ToolCategory.DIY,
                ToolCategory.MEASURE,
                ToolCategory.LOCATION,
                ToolCategory.LIGHT_AND_CAMERA,
                ToolCategory.SOUND,
                ToolCategory.CALCULATION,
                ToolCategory.TIME,
                ToolCategory.HEALTH,
                ToolCategory.UTILITIES,
                ToolCategory.DEVICE,
            ),
            ToolCategory.entries.sortedBy { it.displayOrder },
        )
        assertEquals((1..10).toList(), ToolCategory.entries.map { it.displayOrder }.sorted())
    }
}
