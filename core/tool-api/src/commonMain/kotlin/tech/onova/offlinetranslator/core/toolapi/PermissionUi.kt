package tech.onova.offlinetranslator.core.toolapi

import tech.onova.offlinetranslator.core.domain.permissions.PermissionStep

/** What the tool says and does at the current step of the permission flow. */
internal class PermissionUi(
    val step: PermissionStep,
    val text: String,
    val request: () -> Unit,
    val openSettings: () -> Unit,
)
