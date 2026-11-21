package tech.onova.offlinetranslator.feature.toolstranslate.fakes

import kotlinx.coroutines.CompletableDeferred
import tech.onova.offlinetranslator.core.domain.ports.SpeechEnd
import tech.onova.offlinetranslator.core.domain.ports.SpeechOutput
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport

/**
 * The phone's voices (spec 012 contracts/ports.md): it records what it said, and [speak] lasts until the test calls
 * [done] (or something calls [stop]), so a test can look at the screen while it talks.
 */
class FakeSpeechOutput : SpeechOutput {
    val voices = mutableMapOf<String, VoiceSupport>()
    var defaultVoice = VoiceSupport.READY

    /** What was said, with the speech tag it was said in. */
    val said = mutableListOf<Pair<String, String>>()
    var stops = 0
        private set
    var voiceSettingsOpened = 0
        private set

    private var talking: CompletableDeferred<SpeechEnd>? = null

    val speaking: Boolean get() = talking != null

    /** Ends the sentence being spoken: said, [SpeechEnd.INTERRUPTED] by a call, or [SpeechEnd.FAILED]. */
    fun done(end: SpeechEnd = SpeechEnd.DONE) {
        talking?.complete(end)
        talking = null
    }

    override suspend fun voice(speechTag: String): VoiceSupport = voices[speechTag] ?: defaultVoice

    override suspend fun speak(
        text: String,
        speechTag: String,
    ): SpeechEnd {
        said += text to speechTag
        val sentence = CompletableDeferred<SpeechEnd>()
        talking = sentence
        return sentence.await()
    }

    override fun stop() {
        stops++
        done()
    }

    override fun openVoiceSettings() {
        voiceSettingsOpened++
    }
}
