package tech.onova.offlinetranslator.core.domain.permissions

/**
 * Permissions a tool can ask for; each later tool pack adds its own (spec 004 research.md R7).
 * [FINE_LOCATION] is precise location, asked together with approximate location; approximate alone is not
 * granted (spec 008 R9).
 */
public enum class AppPermission {
    NOTIFICATIONS,
    EXACT_ALARMS,
    ACTIVITY_RECOGNITION,
    CAMERA,
    RECORD_AUDIO,
    FINE_LOCATION,
}

public enum class PermissionStatus { GRANTED, DENIED, DENIED_PERMANENTLY }

/** The platform's permission system; tools reach it only through this port. */
public interface PermissionController {
    public fun status(permission: AppPermission): PermissionStatus

    /** Shows the system request, or the settings page for special access. */
    public suspend fun request(permission: AppPermission): PermissionStatus

    public fun openSettings(permission: AppPermission)
}

/** The steps of the constitution's permission flow (Principle IX). */
public enum class PermissionStep {
    /** The tool explains why it needs the permission, with an "Allow" action. */
    EXPLAIN,
    REQUESTING,
    GRANTED,

    /** What is missing, with "Ask again". */
    DENIED,

    /** The system no longer asks: "Open settings". */
    NEEDS_SETTINGS,
}

/** Principle IX as a pure state machine; the explanation always comes before the first request. */
public object PermissionFlow {
    public fun start(status: PermissionStatus): PermissionStep =
        if (status == PermissionStatus.GRANTED) PermissionStep.GRANTED else PermissionStep.EXPLAIN

    public fun afterRequest(result: PermissionStatus): PermissionStep =
        when (result) {
            PermissionStatus.GRANTED -> PermissionStep.GRANTED
            PermissionStatus.DENIED -> PermissionStep.DENIED
            PermissionStatus.DENIED_PERMANENTLY -> PermissionStep.NEEDS_SETTINGS
        }

    /** After the user comes back, for example from the system settings. */
    public fun onResume(
        status: PermissionStatus,
        current: PermissionStep,
    ): PermissionStep = if (status == PermissionStatus.GRANTED) PermissionStep.GRANTED else current
}
