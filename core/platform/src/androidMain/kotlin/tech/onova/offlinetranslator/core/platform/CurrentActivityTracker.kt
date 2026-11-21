package tech.onova.offlinetranslator.core.platform

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

/**
 * The resumed activity, which permission requests need for their system dialog (research.md R7) and the
 * NFC reader needs for reader mode (spec 009 research R4).
 */
public class CurrentActivityTracker : Application.ActivityLifecycleCallbacks {
    private var resumed: WeakReference<ComponentActivity>? = null
    private val resumedFlow = MutableStateFlow<WeakReference<ComponentActivity>?>(null)

    /** The resumed activity as it changes: null between a pause and the next resume. */
    public val resumedActivity: StateFlow<WeakReference<ComponentActivity>?> = resumedFlow.asStateFlow()

    public fun current(): ComponentActivity? = resumed?.get()

    override fun onActivityResumed(activity: Activity) {
        if (activity is ComponentActivity) {
            resumed = WeakReference(activity)
            resumedFlow.value = resumed
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumed?.get() === activity) {
            resumed = null
            resumedFlow.value = null
        }
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ): Unit = Unit

    override fun onActivityStarted(activity: Activity): Unit = Unit

    override fun onActivityStopped(activity: Activity): Unit = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ): Unit = Unit

    override fun onActivityDestroyed(activity: Activity): Unit = Unit
}
