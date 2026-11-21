package tech.onova.offlinetranslator.core.toolapi

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import tech.onova.offlinetranslator.core.designsystem.components.BottomAction
import tech.onova.offlinetranslator.core.designsystem.components.BottomActionStyle
import tech.onova.offlinetranslator.core.designsystem.components.Notice
import tech.onova.offlinetranslator.core.designsystem.components.ToolLayout
import tech.onova.offlinetranslator.core.domain.permissions.AppPermission
import tech.onova.offlinetranslator.core.domain.permissions.PermissionController
import tech.onova.offlinetranslator.core.domain.permissions.PermissionFlow
import tech.onova.offlinetranslator.core.domain.permissions.PermissionStatus
import tech.onova.offlinetranslator.core.domain.permissions.PermissionStep
import tech.onova.offlinetranslator.core.toolapi.resources.Res
import tech.onova.offlinetranslator.core.toolapi.resources.permission_allow
import tech.onova.offlinetranslator.core.toolapi.resources.permission_ask_again
import tech.onova.offlinetranslator.core.toolapi.resources.permission_denied
import tech.onova.offlinetranslator.core.toolapi.resources.permission_needs_settings
import tech.onova.offlinetranslator.core.toolapi.resources.permission_not_now
import tech.onova.offlinetranslator.core.toolapi.resources.permission_open_settings

/** Runs the flow; null once the permission is there (and [onGrant] was called). */
@Composable
private fun rememberPermissionUi(
    permission: AppPermission,
    rationale: String,
    controller: PermissionController,
    onGrant: () -> Unit,
): PermissionUi? {
    val currentOnGrant by rememberUpdatedState(onGrant)
    var step by rememberSaveable(permission) { mutableStateOf(PermissionFlow.start(controller.status(permission))) }
    val scope = rememberCoroutineScope()
    // A request that was running when the screen was recreated is gone: ask the user again.
    LaunchedEffect(permission) { if (step == PermissionStep.REQUESTING) step = PermissionStep.EXPLAIN }
    LifecycleResumeEffect(permission, step) {
        step = PermissionFlow.onResume(controller.status(permission), step)
        onPauseOrDispose { }
    }
    if (step == PermissionStep.GRANTED) {
        LaunchedEffect(permission) { currentOnGrant() }
        return null
    }
    val text =
        when (step) {
            PermissionStep.DENIED -> stringResource(Res.string.permission_denied, rationale)
            PermissionStep.NEEDS_SETTINGS -> stringResource(Res.string.permission_needs_settings, rationale)
            else -> rationale
        }
    return PermissionUi(
        step = step,
        text = text,
        request = {
            step = PermissionStep.REQUESTING
            scope.launch { step = PermissionFlow.afterRequest(controller.request(permission)) }
        },
        openSettings = { controller.openSettings(permission) },
    )
}

/**
 * The constitution's permission flow (Principle IX), shared by every tool (spec 004 research.md
 * R7): the tool's explanation with Allow and Not now, then the system request; if denied, what is
 * missing and Ask again; if the system no longer asks, Open settings. It checks again when the
 * user comes back, and calls [onGrant] once the permission is there. This card is for an offer
 * inside a screen that works without it; a tool that cannot work without the permission uses
 * [PermissionPane].
 */
@Composable
public fun PermissionGate(
    permission: AppPermission,
    rationale: String,
    controller: PermissionController,
    onGrant: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui = rememberPermissionUi(permission, rationale, controller, onGrant) ?: return
    Notice(
        text = ui.text,
        modifier = modifier,
        actions = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.permission_not_now)) }
            when (ui.step) {
                PermissionStep.NEEDS_SETTINGS -> {
                    TextButton(onClick = ui.openSettings) { Text(stringResource(Res.string.permission_open_settings)) }
                }

                PermissionStep.DENIED -> {
                    TextButton(onClick = ui.request) { Text(stringResource(Res.string.permission_ask_again)) }
                }

                else -> {
                    TextButton(onClick = ui.request, enabled = ui.step != PermissionStep.REQUESTING) {
                        Text(stringResource(Res.string.permission_allow))
                    }
                }
            }
        },
    )
}

/**
 * A whole tool behind a permission it cannot work without: the explanation in the content, and at the bottom
 * the action (Allow, Ask again or Open settings) with Not now, which leaves the tool (owner, 2026-11-01).
 * [top] keeps a tool's selectors visible, for example the Color picker's Camera | Picture.
 */
@Composable
public fun PermissionPane(
    permission: AppPermission,
    rationale: String,
    controller: PermissionController,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
    top: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val ui = rememberPermissionUi(permission, rationale, controller, onGrant) ?: return
    val leave = LocalToolBack.current
    ToolLayout(
        modifier = modifier,
        top = top,
        bottom = {
            when (ui.step) {
                PermissionStep.NEEDS_SETTINGS -> {
                    BottomAction(stringResource(Res.string.permission_open_settings), ui.openSettings)
                }

                PermissionStep.DENIED -> {
                    BottomAction(stringResource(Res.string.permission_ask_again), ui.request)
                }

                else -> {
                    BottomAction(
                        stringResource(Res.string.permission_allow),
                        ui.request,
                        enabled = ui.step != PermissionStep.REQUESTING,
                    )
                }
            }
            BottomAction(
                label = stringResource(Res.string.permission_not_now),
                onClick = leave,
                style = BottomActionStyle.Text,
            )
        },
    ) {
        Text(text = ui.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
    }
}

/** Leaves the tool; the host provides it. A pane's Not now uses it. */
@Suppress("CompositionLocalAllowlist") // The host's back action, needed by any pane deep inside a tool.
public val LocalToolBack: ProvidableCompositionLocal<() -> Unit> = staticCompositionLocalOf { {} }

/**
 * Whether the permission is granted, read again each time the screen comes back to the front, so a permission
 * taken away in the phone's settings shows the pane again instead of a tool that cannot work.
 */
@Composable
public fun rememberPermissionGranted(
    permission: AppPermission,
    controller: PermissionController,
): Boolean {
    var granted by remember(permission) { mutableStateOf(controller.status(permission) == PermissionStatus.GRANTED) }
    LifecycleResumeEffect(permission) {
        granted = controller.status(permission) == PermissionStatus.GRANTED
        onPauseOrDispose { }
    }
    return granted
}
