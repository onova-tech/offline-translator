package tech.onova.offlinetranslator.support

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import tech.onova.offlinetranslator.core.domain.ports.ContentPacks
import tech.onova.offlinetranslator.core.domain.ports.EngineOpen
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.PackFailure
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.PackState
import tech.onova.offlinetranslator.core.domain.ports.PackStatus
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.Route
import tech.onova.offlinetranslator.core.domain.ports.ScreenReader
import tech.onova.offlinetranslator.core.domain.ports.SpeechEnd
import tech.onova.offlinetranslator.core.domain.ports.SpeechEvent
import tech.onova.offlinetranslator.core.domain.ports.SpeechInput
import tech.onova.offlinetranslator.core.domain.ports.SpeechOutput
import tech.onova.offlinetranslator.core.domain.ports.TranslationEngine
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport

// The Translator's fakes for app-level UI tests (spec 012 T023), the same as the feature's own in
// feature/tools-translate's commonTest/fakes: keep the two alike.

/**
 * The store, driven by the test (spec 012 contracts/ports.md): it sets each pack's state; [download] starts at
 * `Downloading(0, total)` and the test moves it on with [progress], [finish] or [fail].
 */
class FakeContentPacks(
    vararg initial: PackStatus,
) : ContentPacks {
    private val state = MutableStateFlow(initial.toList())
    override val packs: StateFlow<List<PackStatus>> = state.asStateFlow()

    var refreshes = 0
        private set
    val downloads = mutableListOf<Pair<PackId, Boolean>>()
    val cancelled = mutableListOf<PackId>()

    /** Packs whose store confirmation (Google Play's dialog) was asked for. */
    val confirmed = mutableListOf<PackId>()
    val removed = mutableListOf<PackId>()

    /** Adds [pack] or replaces the one with its id. */
    fun put(pack: PackStatus) {
        state.value = state.value.filterNot { it.id == pack.id } + pack
    }

    fun stateOf(pack: PackId): PackState? = state.value.firstOrNull { it.id == pack }?.state

    fun progress(
        pack: PackId,
        bytes: Long,
    ) = update(pack) { it.copy(state = PackState.Downloading(bytes, it.downloadBytes ?: bytes)) }

    fun finish(pack: PackId) = update(pack) { it.copy(state = PackState.Installed, installedBytes = it.downloadBytes) }

    fun fail(
        pack: PackId,
        reason: PackFailure,
    ) = update(pack) { it.copy(state = PackState.Failed(reason)) }

    override suspend fun refresh() {
        refreshes++
    }

    override suspend fun download(
        pack: PackId,
        wifiOnly: Boolean,
    ) {
        downloads += pack to wifiOnly
        update(pack) { it.copy(state = PackState.Downloading(0, it.downloadBytes ?: 0)) }
    }

    override suspend fun confirm(pack: PackId) {
        confirmed += pack
    }

    override suspend fun cancel(pack: PackId) {
        cancelled += pack
        update(pack) { it.copy(state = PackState.NotInstalled) }
    }

    override suspend fun remove(pack: PackId) {
        removed += pack
        update(pack) { it.copy(state = PackState.NotInstalled, installedBytes = null) }
    }

    override fun location(pack: PackId): String? = "packs/${pack.value}".takeIf { stateOf(pack) == PackState.Installed }

    private fun update(
        pack: PackId,
        change: (PackStatus) -> PackStatus,
    ) {
        state.value = state.value.map { if (it.id == pack) change(it) else it }
    }

    companion object {
        /** About the size of a base-memory pack (spec 012 research R4). */
        const val PACK_BYTES: Long = 48_000_000

        fun notInstalled(pack: PackId): PackStatus = PackStatus(pack, PACK_BYTES, null, PackState.NotInstalled)

        fun installed(pack: PackId): PackStatus = PackStatus(pack, PACK_BYTES, PACK_BYTES, PackState.Installed)
    }
}

/**
 * An engine that answers `"[to] text"` (spec 012 contracts/ports.md); through English it composes two direct calls, so
 * pt to es reads `"[es] [en] text"`. A test makes it slow with [hold] and [release], or makes it fail with [failure].
 */
class FakeTranslationEngine : TranslationEngine {
    var openAnswer: EngineOpen = EngineOpen.Ready
    val opened = mutableListOf<Map<PackId, String>>()
    var closes = 0
        private set
    val requests = mutableListOf<Pair<String, Route>>()

    /** When set, [translate] throws it, as the engine does on a damaged model. */
    var failure: Throwable? = null

    private var gate: CompletableDeferred<Unit>? = null

    /** Every translation from now on waits for [release]: a slow engine, for the newest-wins rule. */
    fun hold() {
        gate = CompletableDeferred()
    }

    fun release() {
        gate?.complete(Unit)
        gate = null
    }

    override suspend fun open(packs: Map<PackId, String>): EngineOpen {
        opened += packs
        return openAnswer
    }

    override suspend fun translate(
        text: String,
        route: Route,
    ): String {
        requests += text to route
        gate?.await()
        failure?.let { throw it }
        return when (route) {
            Route.Same -> text
            is Route.Direct -> direct(text, route.to)
            is Route.ThroughEnglish -> direct(direct(text, LanguageCode.ENGLISH), route.to)
        }
    }

    override fun close() {
        closes++
    }

    private fun direct(
        text: String,
        to: LanguageCode,
    ) = "[${to.value}] $text"
}

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

/** TalkBack, off unless the test turns it on (spec 012: with it on, a translation is announced, not spoken). */
class FakeScreenReader : ScreenReader {
    override val on = MutableStateFlow(false)
}
