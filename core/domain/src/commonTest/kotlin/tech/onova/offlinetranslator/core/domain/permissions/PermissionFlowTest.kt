package tech.onova.offlinetranslator.core.domain.permissions

import kotlin.test.Test
import kotlin.test.assertEquals

class PermissionFlowTest {
    @Test
    fun theExplanationComesBeforeAnyRequest() {
        assertEquals(PermissionStep.EXPLAIN, PermissionFlow.start(PermissionStatus.DENIED))
        assertEquals(PermissionStep.EXPLAIN, PermissionFlow.start(PermissionStatus.DENIED_PERMANENTLY))
    }

    @Test
    fun anAlreadyGrantedPermissionSkipsTheFlow() {
        assertEquals(PermissionStep.GRANTED, PermissionFlow.start(PermissionStatus.GRANTED))
    }

    @Test
    fun theRequestResultPicksTheNextStep() {
        assertEquals(PermissionStep.GRANTED, PermissionFlow.afterRequest(PermissionStatus.GRANTED))
        assertEquals(PermissionStep.DENIED, PermissionFlow.afterRequest(PermissionStatus.DENIED))
        assertEquals(PermissionStep.NEEDS_SETTINGS, PermissionFlow.afterRequest(PermissionStatus.DENIED_PERMANENTLY))
    }

    @Test
    fun comingBackGrantedFinishesTheFlowOtherwiseNothingChanges() {
        val fromSettings = PermissionStep.NEEDS_SETTINGS
        assertEquals(PermissionStep.GRANTED, PermissionFlow.onResume(PermissionStatus.GRANTED, fromSettings))
        assertEquals(fromSettings, PermissionFlow.onResume(PermissionStatus.DENIED, fromSettings))
        val denied = PermissionStep.DENIED
        assertEquals(denied, PermissionFlow.onResume(PermissionStatus.DENIED_PERMANENTLY, denied))
    }
}
