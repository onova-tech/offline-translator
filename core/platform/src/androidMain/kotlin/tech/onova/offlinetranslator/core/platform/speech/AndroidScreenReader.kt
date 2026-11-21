package tech.onova.offlinetranslator.core.platform.speech

import android.content.Context
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.onova.offlinetranslator.core.domain.ports.ScreenReader

/**
 * Whether TalkBack (or another screen reader) explores the screen by touch (spec 012 contracts/ports.md). A singleton:
 * its listener lives as long as the app.
 */
public class AndroidScreenReader(
    context: Context,
) : ScreenReader {
    private val manager = context.applicationContext.getSystemService(AccessibilityManager::class.java)
    private val state = MutableStateFlow(manager?.isTouchExplorationEnabled == true)
    override val on: StateFlow<Boolean> = state.asStateFlow()

    init {
        manager?.addTouchExplorationStateChangeListener { enabled -> state.value = enabled }
    }
}
