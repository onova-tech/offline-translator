package tech.onova.offlinetranslator.core.platform

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tech.onova.offlinetranslator.core.domain.ports.ProcessVisibility

/** Visible while any activity is started, from the process lifecycle (research.md R15). */
public class ProcessAppVisibility : ProcessVisibility {
    override val isVisible: StateFlow<Boolean> =
        ProcessLifecycleOwner
            .get()
            .lifecycle.currentStateFlow
            .map { it.isAtLeast(Lifecycle.State.STARTED) }
            .stateIn(CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate), SharingStarted.Eagerly, true)
}
