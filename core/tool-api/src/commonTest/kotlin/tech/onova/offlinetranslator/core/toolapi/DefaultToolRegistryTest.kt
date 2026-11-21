package tech.onova.offlinetranslator.core.toolapi

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringArrayResource
import org.jetbrains.compose.resources.StringResource
import tech.onova.offlinetranslator.core.domain.catalog.ToolCategory
import tech.onova.offlinetranslator.core.domain.catalog.ToolDescriptor
import tech.onova.offlinetranslator.core.domain.catalog.ToolId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class DefaultToolRegistryTest {
    private class FakeEntry(
        id: String,
    ) : ToolEntry {
        override val descriptor = ToolDescriptor(ToolId(id), ToolCategory.UTILITIES)
        override val name: StringResource get() = error("unused")
        override val keywords: StringArrayResource get() = error("unused")
        override val icon: ImageVector get() = error("unused")

        @Composable
        override fun HelpContent() = Unit

        @Composable
        override fun Content() = Unit
    }

    /** Spec 032: a shortcut or a link to the Plumb bob opens the Bubble level, which took it in. */
    @Test
    fun aMergedToolsIdFindsTheToolItJoined() {
        val level = FakeEntry("bubble-level")
        val registry = DefaultToolRegistry(listOf(level))
        assertSame(level, registry.find(ToolId("plumb-bob")))
        assertNull(registry.find(ToolId("compass")))
    }

    @Test
    fun findsRegisteredEntriesById() {
        val counter = FakeEntry("counter")
        val registry = DefaultToolRegistry(listOf(FakeEntry("ruler"), counter))
        assertSame(counter, registry.find(ToolId("counter")))
        assertNull(registry.find(ToolId("compass")))
        assertEquals(2, registry.entries.size)
    }

    @Test
    fun duplicateIdsFailFast() {
        assertFailsWith<IllegalArgumentException> {
            DefaultToolRegistry(listOf(FakeEntry("ruler"), FakeEntry("ruler")))
        }
    }
}
