package tech.onova.offlinetranslator.feature.toolstranslate.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import tech.onova.offlinetranslator.core.domain.ports.ContentPacks
import tech.onova.offlinetranslator.core.domain.ports.EngineOpen
import tech.onova.offlinetranslator.core.domain.ports.EngineUnavailable
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.PackState
import tech.onova.offlinetranslator.core.domain.ports.Route
import tech.onova.offlinetranslator.core.domain.ports.TranslationEngine

/**
 * The engine with the packs installed now, open only while a Translator screen uses it (spec 012 edge case "Memory":
 * each model takes about 335 MB of native memory on the owner's phone, research R3). Both modes' screens [acquire] it;
 * the last [release] frees the models. A pack installed meanwhile is loaded with the others, and a damaged one is
 * removed so it's offered again. Called from the main thread only, as a ViewModel's start and end are.
 */
internal class EngineSession(
    private val packs: ContentPacks,
    private val engine: TranslationEngine,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    sealed interface State {
        data object Closed : State

        data object Opening : State

        /** Open, with the models of [loaded]. */
        data class Ready(
            val loaded: Set<PackId>,
        ) : State

        data class Unavailable(
            val reason: EngineUnavailable,
        ) : State

        /** Whether [route] can be translated now. */
        fun readiness(route: Route): Readiness =
            when {
                route == Route.Same -> Readiness.READY
                this is Unavailable -> Readiness.UNAVAILABLE
                this is Ready && loaded.containsAll(Routes.packsFor(route)) -> Readiness.READY
                else -> Readiness.OPENING
            }
    }

    enum class Readiness { READY, OPENING, UNAVAILABLE }

    private val current = MutableStateFlow<State>(State.Closed)
    val state: StateFlow<State> = current.asStateFlow()

    private var users = 0
    private var scope: CoroutineScope? = null

    fun acquire() {
        users++
        if (users == 1) start()
    }

    fun release() {
        users--
        if (users == 0) stop()
    }

    /** Throws [IllegalStateException] when the engine fails; a cancelled caller drops the result. */
    suspend fun translate(
        text: String,
        route: Route,
    ): String = engine.translate(text, route)

    private fun start() {
        val running = CoroutineScope(SupervisorJob() + dispatcher)
        scope = running
        running.launch {
            packs.refresh()
            packs.packs
                // Only the translation packs: the app's speech packs (spec 082) hold no translation model, and the
                // engine would call them damaged, which removed them right after their download (2026-11-22).
                .map { statuses ->
                    statuses
                        .filter { it.state == PackState.Installed && Routes.languageOf(it.id) != null }
                        .map { it.id }
                        .toSet()
                }.distinctUntilChanged()
                .collect { installed -> open(installed) }
        }
    }

    private suspend fun open(installed: Set<PackId>) {
        current.value = State.Opening
        val folders = installed.mapNotNull { pack -> packs.location(pack)?.let { pack to it } }.toMap()
        current.value =
            when (val answer = engine.open(folders)) {
                EngineOpen.Ready -> State.Ready(folders.keys)
                is EngineOpen.Damaged -> forget(answer.packs, opened = folders.keys - answer.packs)
                is EngineOpen.Unavailable -> State.Unavailable(answer.reason)
            }
    }

    /**
     * A damaged pack is treated as not installed (spec 012 edge case): the store removes it, so the Translator offers
     * it again, and the other packs stay open. When the store keeps it, the person is told to delete it.
     */
    private suspend fun forget(
        damaged: Set<PackId>,
        opened: Set<PackId>,
    ): State {
        damaged.forEach { packs.remove(it) }
        val kept = packs.packs.value.any { it.id in damaged && it.state == PackState.Installed }
        return if (kept) State.Unavailable(EngineUnavailable.DAMAGED_PACK) else State.Ready(opened)
    }

    private fun stop() {
        scope?.cancel()
        scope = null
        engine.close()
        current.value = State.Closed
    }
}
