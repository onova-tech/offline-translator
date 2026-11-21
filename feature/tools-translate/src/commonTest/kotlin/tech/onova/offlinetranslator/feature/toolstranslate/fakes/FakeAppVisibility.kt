package tech.onova.offlinetranslator.feature.toolstranslate.fakes

import kotlinx.coroutines.flow.MutableStateFlow
import tech.onova.offlinetranslator.core.domain.ports.AppVisibility

/** The screen's task, shown or hidden by the test. */
class FakeAppVisibility(
    shown: Boolean = true,
) : AppVisibility {
    override val isVisible: MutableStateFlow<Boolean> = MutableStateFlow(shown)
}
