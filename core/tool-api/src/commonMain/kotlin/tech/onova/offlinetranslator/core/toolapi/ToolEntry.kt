package tech.onova.offlinetranslator.core.toolapi

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringArrayResource
import org.jetbrains.compose.resources.StringResource
import tech.onova.offlinetranslator.core.domain.catalog.ToolDescriptor

/**
 * How a tool joins the app (contracts/tool-contract.md §1). The host draws the shared frame
 * (back, title, favorite, help); a tool only provides its body and its help text.
 */
public interface ToolEntry {
    public val descriptor: ToolDescriptor

    /** Localized display name. */
    public val name: StringResource

    /** Localized search keywords. */
    public val keywords: StringArrayResource

    /** Single-color line icon built with the design system's `pocketIcon`. */
    public val icon: ImageVector

    /** "How to use" text shown from the shared frame. */
    @Composable
    public fun HelpContent()

    /** The tool body, rendered inside the host's tool screen. */
    @Composable
    public fun Content()
}
