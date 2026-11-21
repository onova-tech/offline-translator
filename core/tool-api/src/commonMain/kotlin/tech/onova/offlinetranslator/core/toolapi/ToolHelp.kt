package tech.onova.offlinetranslator.core.toolapi

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tech.onova.offlinetranslator.core.designsystem.components.HelpBody

/**
 * A tool's [ToolEntry.HelpContent], from one string: in the XML, `\n` starts a new paragraph and `\n- ` a list item.
 * Every language has the same blocks (`HelpTextsTest`).
 */
@Composable
public fun ToolHelp(text: StringResource) {
    HelpBody(stringResource(text))
}
