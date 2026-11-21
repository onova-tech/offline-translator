package tech.onova.offlinetranslator.core.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import tech.onova.offlinetranslator.core.domain.ports.AppVisibility
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TaskVisibilityTest {
    private val process =
        object : AppVisibility {
            override val isVisible = MutableStateFlow(true)
        }

    private fun TestScope.visibility() =
        TaskVisibility(
            process,
            CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        )

    private fun TaskVisibility.open(task: Int): AppVisibility {
        onTaskStarted(task)
        onTaskResumed(task)
        return forCurrentTask()
    }

    @Test
    fun aViewModelCreatedBeforeAnyActivityFollowsTheProcess() =
        runTest {
            assertSame(process, visibility().forCurrentTask())
        }

    @Test
    fun aHiddenTaskIsHiddenWhileAnotherIsOnScreen() =
        runTest {
            val tasks = visibility()
            val home = tasks.open(1)
            val shortcut = tasks.open(2)
            tasks.onTaskStopped(1)
            advanceTimeBy(1_000)
            assertFalse(home.isVisible.value)
            assertTrue(shortcut.isVisible.value)
        }

    @Test
    fun eachTaskKeepsItsOwnSignalAcrossManyViewModels() =
        runTest {
            val tasks = visibility()
            val first = tasks.open(1)
            assertSame(first, tasks.forCurrentTask())
            tasks.open(2)
            tasks.onTaskResumed(1)
            assertSame(first, tasks.forCurrentTask())
        }

    @Test
    fun aRecreatedActivityNeverLooksHidden() =
        runTest {
            val tasks = visibility()
            val task = tasks.open(1)
            tasks.onTaskStopped(1)
            advanceTimeBy(100)
            tasks.onTaskStarted(1)
            advanceTimeBy(1_000)
            assertTrue(task.isVisible.value)
        }

    @Test
    fun aStartBeforeTheOldStopStillCounts() =
        runTest {
            val tasks = visibility()
            val task = tasks.open(1)
            tasks.onTaskStarted(1)
            tasks.onTaskStopped(1)
            advanceTimeBy(1_000)
            assertTrue(task.isVisible.value)
        }

    @Test
    fun hidingTakesAMomentAndComingBackShowsAtOnce() =
        runTest {
            val tasks = visibility()
            val task = tasks.open(1)
            tasks.onTaskStopped(1)
            advanceTimeBy(600)
            assertTrue(task.isVisible.value)
            advanceTimeBy(200)
            assertFalse(task.isVisible.value)
            tasks.onTaskStarted(1)
            assertTrue(task.isVisible.value)
        }

    @Test
    fun aSignalMadeForAHiddenTaskStartsHidden() =
        runTest {
            val tasks = visibility()
            tasks.onTaskResumed(1)
            assertFalse(tasks.forCurrentTask().isVisible.value)
        }
}
