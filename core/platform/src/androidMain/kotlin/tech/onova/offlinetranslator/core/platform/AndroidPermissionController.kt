package tech.onova.offlinetranslator.core.platform

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import tech.onova.offlinetranslator.core.domain.permissions.AppPermission
import tech.onova.offlinetranslator.core.domain.permissions.PermissionController
import tech.onova.offlinetranslator.core.domain.permissions.PermissionStatus
import kotlin.coroutines.resume

/**
 * Runtime permissions through the resumed activity's result registry, and special access through
 * its settings page (spec 004 research.md R7). Below the Android version that introduced a
 * permission, it is granted.
 */
public class AndroidPermissionController(
    private val context: Context,
    private val activities: CurrentActivityTracker,
) : PermissionController {
    override fun status(permission: AppPermission): PermissionStatus =
        when (permission) {
            AppPermission.NOTIFICATIONS -> notificationStatus()
            AppPermission.EXACT_ALARMS -> exactAlarmStatus()
            AppPermission.ACTIVITY_RECOGNITION -> activityStatus()
            AppPermission.CAMERA -> runtimeStatus(Manifest.permission.CAMERA)
            AppPermission.RECORD_AUDIO -> runtimeStatus(Manifest.permission.RECORD_AUDIO)
            AppPermission.FINE_LOCATION -> runtimeStatus(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    override suspend fun request(permission: AppPermission): PermissionStatus =
        when (permission) {
            AppPermission.NOTIFICATIONS -> {
                requestNotifications()
            }

            AppPermission.EXACT_ALARMS -> {
                // Special access: the system settings page decides; the tool re-checks on resume.
                openSettings(permission)
                status(permission)
            }

            AppPermission.ACTIVITY_RECOGNITION -> {
                requestRuntime(Manifest.permission.ACTIVITY_RECOGNITION, ::activityStatus)
            }

            AppPermission.CAMERA -> {
                requestRuntime(Manifest.permission.CAMERA) { runtimeStatus(Manifest.permission.CAMERA) }
            }

            AppPermission.RECORD_AUDIO -> {
                requestRuntime(Manifest.permission.RECORD_AUDIO) { runtimeStatus(Manifest.permission.RECORD_AUDIO) }
            }

            AppPermission.FINE_LOCATION -> {
                requestLocation(context, activities.current())
            }
        }

    /**
     * Opens the page for [permission]. Some devices lack that page (the test emulator has no
     * Settings app at all), so the app's details page, then the main settings, are fallbacks;
     * when nothing can open, nothing happens instead of a crash.
     */
    override fun openSettings(permission: AppPermission) {
        val starter = activities.current()
        listOf(
            settingsPage(context, permission),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context)),
            Intent(Settings.ACTION_SETTINGS),
        ).any { start(it, starter) }
    }

    /** Starts [intent] from [starter], or as a new task without one; false when no app handles it. */
    private fun start(
        intent: Intent,
        starter: Activity?,
    ): Boolean =
        try {
            if (starter != null) {
                starter.startActivity(intent)
            } else {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            true
        } catch (_: ActivityNotFoundException) {
            false
        }

    private fun notificationStatus(): PermissionStatus {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        return when {
            enabled -> PermissionStatus.GRANTED

            // Before Android 13 there is no dialog: notifications are switched on in settings.
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> PermissionStatus.DENIED_PERMANENTLY

            else -> PermissionStatus.DENIED
        }
    }

    /** Before Android 10 there is no runtime permission for steps; the step counter needs none. */
    private fun activityStatus(): PermissionStatus =
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> PermissionStatus.GRANTED
            else -> runtimeStatus(Manifest.permission.ACTIVITY_RECOGNITION)
        }

    /** Granted when the system reports the runtime [permission] as granted. */
    private fun runtimeStatus(permission: String): PermissionStatus =
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }

    private fun exactAlarmStatus(): PermissionStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return PermissionStatus.GRANTED
        val alarms = context.getSystemService(AlarmManager::class.java)
        return if (alarms.canScheduleExactAlarms()) PermissionStatus.GRANTED else PermissionStatus.DENIED_PERMANENTLY
    }

    private suspend fun requestNotifications(): PermissionStatus {
        val activity = activities.current()
        val canAsk =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                activity != null &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        return if (canAsk && activity != null) {
            askForPermission(activity, Manifest.permission.POST_NOTIFICATIONS, ::notificationStatus)
        } else {
            notificationStatus()
        }
    }

    /** Asks for the runtime [permission] when it is missing and an activity can show the dialog. */
    private suspend fun requestRuntime(
        permission: String,
        status: () -> PermissionStatus,
    ): PermissionStatus {
        val activity = activities.current()
        return if (status() != PermissionStatus.GRANTED && activity != null) {
            askForPermission(activity, permission, status)
        } else {
            status()
        }
    }
}

/** The settings page that controls [permission]; the app's details page for the others. */
private fun settingsPage(
    context: Context,
    permission: AppPermission,
): Intent =
    when {
        permission == AppPermission.EXACT_ALARMS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri(context))
        }

        permission == AppPermission.NOTIFICATIONS -> {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }

        else -> {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context))
        }
    }

/** Shows the system dialog for [permission]; [grantedStatus] reads the status after a grant. */
private suspend fun askForPermission(
    activity: ComponentActivity,
    permission: String,
    grantedStatus: () -> PermissionStatus,
): PermissionStatus {
    val granted =
        suspendCancellableCoroutine { continuation ->
            lateinit var launcher: ActivityResultLauncher<String>
            launcher =
                activity.activityResultRegistry.register(REQUEST_KEY, ActivityResultContracts.RequestPermission()) {
                    launcher.unregister()
                    if (continuation.isActive) continuation.resume(it)
                }
            continuation.invokeOnCancellation { launcher.unregister() }
            launcher.launch(permission)
        }
    return when {
        granted -> grantedStatus()

        // After a denial, no rationale means the system will not ask again.
        activity.shouldShowRequestPermissionRationale(permission) -> PermissionStatus.DENIED

        else -> PermissionStatus.DENIED_PERMANENTLY
    }
}

/**
 * Precise and approximate location together, as Android 12 and later require (spec 008 R9). Approximate alone
 * is not enough: it reads as denied, and the tool's explanation says why.
 */
private suspend fun requestLocation(
    context: Context,
    activity: ComponentActivity?,
): PermissionStatus {
    val fine = Manifest.permission.ACCESS_FINE_LOCATION
    val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
    val granted = { permission: String ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    if (granted(fine)) return PermissionStatus.GRANTED
    if (activity == null) return PermissionStatus.DENIED
    val answers = askForPermissions(activity, arrayOf(fine, coarse))
    return when {
        answers[fine] == true -> PermissionStatus.GRANTED

        // With approximate granted, or a rationale to show, the system can still be asked for precise.
        activity.shouldShowRequestPermissionRationale(fine) || granted(coarse) -> PermissionStatus.DENIED

        else -> PermissionStatus.DENIED_PERMANENTLY
    }
}

/** Shows the system dialog for several [permissions] at once; the answer for each. */
private suspend fun askForPermissions(
    activity: ComponentActivity,
    permissions: Array<String>,
): Map<String, Boolean> =
    suspendCancellableCoroutine { continuation ->
        lateinit var launcher: ActivityResultLauncher<Array<String>>
        launcher =
            activity.activityResultRegistry.register(
                REQUEST_KEY,
                ActivityResultContracts.RequestMultiplePermissions(),
            ) {
                launcher.unregister()
                if (continuation.isActive) continuation.resume(it)
            }
        continuation.invokeOnCancellation { launcher.unregister() }
        launcher.launch(permissions)
    }

private fun packageUri(context: Context): Uri = Uri.fromParts("package", context.packageName, null)

private const val REQUEST_KEY = "offlinetranslator-permission"
