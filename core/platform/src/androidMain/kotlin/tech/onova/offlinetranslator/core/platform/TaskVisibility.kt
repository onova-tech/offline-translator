package tech.onova.offlinetranslator.core.platform

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import tech.onova.offlinetranslator.core.domain.ports.AppVisibility
import kotlin.time.Duration.Companion.milliseconds

/**
 * Which of the app's tasks are on screen (spec 016 research R2). A tool opened from a shortcut has a task of its own in
 * the same process, and going from one task to the other never stops the process, so a process-wide signal would leave
 * a tone playing or the microphone open in a screen nobody can see.
 *
 * A ViewModel is created while its screen composes, so the task resumed at that moment is its own. This holds task ids
 * only: it keeps no activity alive, and it still means the same after the activity is recreated. Like the process's
 * lifecycle, it reports hidden only after [HIDE_DELAY], so a rotation doesn't look like leaving.
 */
public class TaskVisibility(
    private val process: AppVisibility,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
    private val started = MutableStateFlow<Map<Int, Int>>(emptyMap())
    private val tasks = HashMap<Int, AppVisibility>()
    private var resumedTask: Int? = null

    /** To register with the application before any screen starts. */
    public val activityCallbacks: Application.ActivityLifecycleCallbacks = Callbacks()

    /** What a ViewModel being created now follows; the process's until the first activity is resumed. */
    public fun forCurrentTask(): AppVisibility {
        val task = resumedTask ?: return process
        return tasks.getOrPut(task) { OneTask(task) }
    }

    internal fun onTaskResumed(task: Int) {
        resumedTask = task
    }

    internal fun onTaskStarted(task: Int) {
        started.update { it + (task to it.startedIn(task) + 1) }
    }

    internal fun onTaskStopped(task: Int) {
        started.update { it + (task to (it.startedIn(task) - 1).coerceAtLeast(0)) }
    }

    private inner class OneTask(
        task: Int,
    ) : AppVisibility {
        @OptIn(ExperimentalCoroutinesApi::class)
        override val isVisible: StateFlow<Boolean> =
            started
                .map { it.startedIn(task) > 0 }
                .distinctUntilChanged()
                .transformLatest { visible ->
                    if (!visible) delay(HIDE_DELAY)
                    emit(visible)
                }.stateIn(scope, SharingStarted.Eagerly, started.value.startedIn(task) > 0)
    }

    private inner class Callbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity): Unit = onTaskResumed(activity.taskId)

        override fun onActivityStarted(activity: Activity): Unit = onTaskStarted(activity.taskId)

        override fun onActivityStopped(activity: Activity): Unit = onTaskStopped(activity.taskId)

        override fun onActivityCreated(
            activity: Activity,
            savedInstanceState: Bundle?,
        ): Unit = Unit

        override fun onActivityPaused(activity: Activity): Unit = Unit

        override fun onActivitySaveInstanceState(
            activity: Activity,
            outState: Bundle,
        ): Unit = Unit

        override fun onActivityDestroyed(activity: Activity): Unit = Unit
    }

    private companion object {
        /** As long as `ProcessLifecycleOwner` waits. */
        val HIDE_DELAY = 700.milliseconds
    }
}

private fun Map<Int, Int>.startedIn(task: Int): Int = this[task] ?: 0
