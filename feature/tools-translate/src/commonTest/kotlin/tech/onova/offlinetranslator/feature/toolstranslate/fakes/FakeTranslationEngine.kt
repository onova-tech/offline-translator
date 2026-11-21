package tech.onova.offlinetranslator.feature.toolstranslate.fakes

import kotlinx.coroutines.CompletableDeferred
import tech.onova.offlinetranslator.core.domain.ports.EngineOpen
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.Route
import tech.onova.offlinetranslator.core.domain.ports.TranslationEngine

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

    /** Packs whose files the engine can't read: [open] names those it's given and opens the others. */
    val damagedPacks = mutableSetOf<PackId>()

    override suspend fun open(packs: Map<PackId, String>): EngineOpen {
        opened += packs
        val damaged = packs.keys intersect damagedPacks
        return if (damaged.isEmpty()) openAnswer else EngineOpen.Damaged(damaged)
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
