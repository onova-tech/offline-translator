package tech.onova.offlinetranslator.feature.toolstranslate.fakes

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.SpeechEvent
import tech.onova.offlinetranslator.core.domain.ports.SpeechInput

/**
 * The phone's recognizer as a script (spec 012 contracts/ports.md): the test sets [support] per tag and feeds what is
 * "heard" with [hear], before or while listening. Listening ends after a `Final` or a `Failed`, like the real one.
 */
class FakeSpeechInput : SpeechInput {
    val support = mutableMapOf<String, RecognitionSupport>()
    var defaultSupport = RecognitionSupport.READY

    /** What [requestLanguage] answers: false is a phone older than Android 13. */
    var canRequest = true
    val requested = mutableListOf<String>()
    val listenedTo = mutableListOf<String>()

    /** How many are listening now: the microphone is open while this is above zero. */
    var listening = 0
        private set

    private val heard = Channel<SpeechEvent>(Channel.UNLIMITED)

    fun hear(vararg events: SpeechEvent) {
        events.forEach { heard.trySend(it) }
    }

    /** Whether the phone's speech service has a store page to be updated from. */
    var updatable = false
    var serviceUpdatesOpened = 0
        private set

    override suspend fun support(speechTag: String): RecognitionSupport = support[speechTag] ?: defaultSupport

    override fun canUpdateService(): Boolean = updatable

    override fun openServiceUpdate() {
        serviceUpdatesOpened++
    }

    override fun requestLanguage(speechTag: String): Boolean {
        requested += speechTag
        return canRequest
    }

    override fun listen(speechTag: String): Flow<SpeechEvent> =
        flow {
            listenedTo += speechTag
            listening++
            try {
                while (true) {
                    val event = heard.receive()
                    emit(event)
                    if (event !is SpeechEvent.Partial) break
                }
            } finally {
                listening--
            }
        }
}
