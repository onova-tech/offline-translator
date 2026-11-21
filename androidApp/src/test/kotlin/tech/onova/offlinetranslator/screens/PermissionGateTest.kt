package tech.onova.offlinetranslator.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.junit.Test
import tech.onova.offlinetranslator.core.domain.permissions.AppPermission
import tech.onova.offlinetranslator.core.domain.permissions.PermissionStatus
import tech.onova.offlinetranslator.core.toolapi.PermissionGate
import tech.onova.offlinetranslator.support.AppUiTest
import kotlin.test.assertEquals

/** Spec 004 FR-042 / constitution Principle IX: the shared permission flow, step by step. */
class PermissionGateTest : AppUiTest() {
    private var grants = 0
    private var dismissals = 0

    /** Lets a test leave the screen and come back, as when the user visits the system settings. */
    private val lifecycleOwner =
        object : LifecycleOwner {
            val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
            override val lifecycle: Lifecycle get() = registry
        }

    private fun showGate() =
        show {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                PermissionGate(
                    permission = AppPermission.NOTIFICATIONS,
                    rationale = RATIONALE,
                    controller = fakes.permissions,
                    onGrant = { grants++ },
                    onDismiss = { dismissals++ },
                )
            }
        }

    @Test
    fun explainsBeforeAskingAndReportsTheGrant() {
        fakes.permissions.answers += PermissionStatus.GRANTED
        showGate()
        compose.onNodeWithText(RATIONALE).assertExists()
        assertEquals(PermissionStatus.DENIED, fakes.permissions.status(AppPermission.NOTIFICATIONS))
        compose.onNodeWithText("Allow").performClick()
        compose.waitForIdle()
        assertEquals(1, grants)
        compose.onNodeWithText(RATIONALE).assertDoesNotExist()
    }

    @Test
    fun aPermissionAlreadyGrantedNeedsNoExplanation() {
        fakes.permissions.statuses[AppPermission.NOTIFICATIONS] = PermissionStatus.GRANTED
        showGate()
        compose.onNodeWithText(RATIONALE).assertDoesNotExist()
        assertEquals(1, grants)
    }

    @Test
    fun aDenialOffersAskAgainThenTheSettings() {
        fakes.permissions.answers += listOf(PermissionStatus.DENIED, PermissionStatus.DENIED_PERMANENTLY)
        showGate()
        compose.onNodeWithText("Allow").performClick()
        compose.onNodeWithText("Without this permission", substring = true).assertExists()
        compose.onNodeWithText("Ask again").performClick()
        compose.onNodeWithText("Android no longer asks", substring = true).assertExists()
        compose.onNodeWithText("Open settings").performClick()
        assertEquals(listOf(AppPermission.NOTIFICATIONS), fakes.permissions.openedSettings)
        assertEquals(0, grants)
    }

    @Test
    fun comingBackFromTheSettingsWithThePermissionGrantsIt() {
        fakes.permissions.answers += PermissionStatus.DENIED_PERMANENTLY
        showGate()
        compose.onNodeWithText("Allow").performClick()
        compose.onNodeWithText("Open settings").performClick()
        compose.runOnIdle { lifecycleOwner.registry.currentState = Lifecycle.State.STARTED }
        fakes.permissions.statuses[AppPermission.NOTIFICATIONS] = PermissionStatus.GRANTED
        compose.runOnIdle { lifecycleOwner.registry.currentState = Lifecycle.State.RESUMED }
        compose.waitForIdle()
        assertEquals(1, grants)
    }

    @Test
    fun notNowLeavesThePermissionAlone() {
        showGate()
        compose.onNodeWithText("Not now").performClick()
        assertEquals(1, dismissals)
        assertEquals(PermissionStatus.DENIED, fakes.permissions.status(AppPermission.NOTIFICATIONS))
    }

    private companion object {
        const val RATIONALE = "Timers can alert you while you use other apps."
    }
}
