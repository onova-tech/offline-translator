package tech.onova.offlinetranslator.tool

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import tech.onova.offlinetranslator.core.designsystem.components.EmptyState
import tech.onova.offlinetranslator.core.designsystem.components.IconAction
import tech.onova.offlinetranslator.core.designsystem.components.ToolFrame
import tech.onova.offlinetranslator.core.designsystem.icons.PocketIcons
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing
import tech.onova.offlinetranslator.core.domain.catalog.ToolAvailability
import tech.onova.offlinetranslator.core.domain.catalog.ToolId
import tech.onova.offlinetranslator.core.domain.catalog.availability
import tech.onova.offlinetranslator.core.domain.ports.DeviceCapabilities
import tech.onova.offlinetranslator.core.toolapi.HelpSheet
import tech.onova.offlinetranslator.core.toolapi.LocalToolBack
import tech.onova.offlinetranslator.core.toolapi.ToolEntry
import tech.onova.offlinetranslator.core.toolapi.ToolRegistry
import tech.onova.offlinetranslator.core.toolapi.label
import tech.onova.offlinetranslator.core.toolapi.resources.Res
import tech.onova.offlinetranslator.core.toolapi.resources.host_help
import tech.onova.offlinetranslator.core.toolapi.resources.host_settings
import tech.onova.offlinetranslator.core.toolapi.resources.tool_unavailable_message
import tech.onova.offlinetranslator.core.toolapi.resources.tool_unavailable_title

/**
 * The frame the Translator runs in, where the app opens (spec 081): its title, then help and Settings on the top bar.
 * Nothing to go back to: system Back leaves the app.
 */
@Composable
fun ToolScreen(
    toolId: String,
    onOpenSettings: () -> Unit,
) {
    val registry: ToolRegistry = koinInject()
    val entry = remember(toolId) { registry.find(ToolId(toolId)) } ?: return
    val capabilities: DeviceCapabilities = koinInject()
    var showHelp by rememberSaveable { mutableStateOf(false) }
    val availability = remember(entry) { entry.descriptor.availability(capabilities.presentHardware) }

    ToolFrame(
        title = stringResource(entry.name),
        backLabel = "",
        onBack = null,
        actions = {
            IconAction(icon = PocketIcons.Info, label = stringResource(Res.string.host_help), onClick = {
                showHelp = true
            })
            IconAction(
                icon = PocketIcons.Settings,
                label = stringResource(Res.string.host_settings),
                onClick = onOpenSettings,
            )
        },
    ) { insets ->
        ToolBody(entry, availability, insets)
    }

    if (showHelp) {
        HelpSheet(onClose = { showHelp = false }) { entry.HelpContent() }
    }
}

@Composable
private fun ToolBody(
    entry: ToolEntry,
    availability: ToolAvailability,
    insets: PaddingValues,
) {
    val activity = LocalActivity.current
    // Consumed here, so the tool's keyboard padding counts only what the bars don't cover.
    Box(modifier = Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
        when (availability) {
            ToolAvailability.Available -> {
                CompositionLocalProvider(LocalToolBack provides { activity?.finish() }) { entry.Content() }
            }

            is ToolAvailability.Unavailable -> {
                UnavailableExplanation(availability)
            }
        }
    }
}

@Composable
private fun UnavailableExplanation(availability: ToolAvailability.Unavailable) {
    val missing =
        availability.missing
            .map { stringResource(it.label) }
            .sorted()
            .joinToString(", ")
    EmptyState(
        icon = PocketIcons.Unavailable,
        title = stringResource(Res.string.tool_unavailable_title),
        message = stringResource(Res.string.tool_unavailable_message, missing),
        modifier = Modifier.padding(top = PocketSpacing.xl),
    )
}
