package tech.onova.offlinetranslator.feature.toolstranslate.fakes

import kotlinx.coroutines.flow.MutableStateFlow
import tech.onova.offlinetranslator.core.domain.ports.ScreenReader

/** TalkBack, off unless the test turns it on (spec 012: with it on, a translation is announced, not spoken). */
class FakeScreenReader : ScreenReader {
    override val on = MutableStateFlow(false)
}
