package tech.onova.offlinetranslator.feature.toolstranslate.fakes

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.onova.offlinetranslator.core.domain.ports.ContentPacks
import tech.onova.offlinetranslator.core.domain.ports.PackFailure
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.PackState
import tech.onova.offlinetranslator.core.domain.ports.PackStatus

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

    /** A store that refuses to remove: [remove] leaves the pack as it was. */
    var keepsPacks = false

    override suspend fun remove(pack: PackId) {
        removed += pack
        if (keepsPacks) return
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
