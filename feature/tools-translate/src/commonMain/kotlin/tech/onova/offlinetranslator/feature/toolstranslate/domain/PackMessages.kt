package tech.onova.offlinetranslator.feature.toolstranslate.domain

import tech.onova.offlinetranslator.core.domain.ports.PackFailure
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.PackState
import tech.onova.offlinetranslator.core.domain.ports.PackStatus
import tech.onova.offlinetranslator.core.domain.ports.PackUnavailable
import tech.onova.offlinetranslator.core.domain.ports.PackWait
import tech.onova.offlinetranslator.core.domain.ports.Route

/** What the pack offer shows for one pack (spec 012 story 2, contracts/ui.md "The pack offer"). */
internal sealed interface PackOffer {
    val pack: PackId

    /** The store hasn't answered yet. */
    data class Checking(
        override val pack: PackId,
    ) : PackOffer

    /** Not now and Download; [megabytes] is null when the store gave no size. */
    data class Offer(
        override val pack: PackId,
        val megabytes: Int?,
    ) : PackOffer

    /** A progress bar with Cancel. */
    data class Downloading(
        override val pack: PackId,
        val doneMegabytes: Int,
        val totalMegabytes: Int,
        val percent: Int,
    ) : PackOffer

    data class Installing(
        override val pack: PackId,
    ) : PackOffer

    /** "Connect to Wi-Fi to download" with Network settings. */
    data class WaitingForWifi(
        override val pack: PackId,
    ) : PackOffer

    /** The store asks to confirm a download on mobile data. */
    data class WaitingForConfirmation(
        override val pack: PackId,
    ) : PackOffer

    /** "Needs N MB free" with Storage settings. */
    data class NoSpace(
        override val pack: PackId,
        val neededMegabytes: Int,
    ) : PackOffer

    /** What happened, with Try again. */
    data class Failed(
        override val pack: PackId,
    ) : PackOffer

    /** "Language packs can't be downloaded on this phone." No button. */
    data class NoStore(
        override val pack: PackId,
    ) : PackOffer

    /** The phone's processor has no engine. No button. */
    data class Unsupported(
        override val pack: PackId,
    ) : PackOffer
}

internal object PackMessages {
    /**
     * Megabytes as the phone's own storage screens count them (1,000,000 bytes), rounded up so a size is never
     * understated.
     */
    private const val BYTES_PER_MEGABYTE = 1_000_000L
    private const val PERCENT = 100

    /** Null once the pack is installed: then there's nothing to offer. */
    fun offerFor(status: PackStatus): PackOffer? {
        val pack = status.id
        return when (val state = status.state) {
            PackState.Unknown -> PackOffer.Checking(pack)
            PackState.NotInstalled -> PackOffer.Offer(pack, status.downloadBytes?.let(::megabytes))
            is PackState.Downloading -> downloading(pack, state)
            PackState.Installing -> PackOffer.Installing(pack)
            PackState.Installed -> null
            is PackState.Waiting -> waiting(pack, state.reason)
            is PackState.Failed -> failed(pack, state.reason)
            is PackState.Unavailable -> unavailable(pack, state.reason)
        }
    }

    fun megabytes(bytes: Long): Int = ((bytes + BYTES_PER_MEGABYTE - 1) / BYTES_PER_MEGABYTE).toInt()

    /** The packs [route] needs that aren't installed, in its order (story 2, scenario 6). */
    fun missing(
        route: Route,
        statuses: List<PackStatus>,
    ): List<PackId> {
        val installed = statuses.filter { it.state == PackState.Installed }.map { it.id }.toSet()
        return Routes.packsFor(route).filterNot { it in installed }
    }

    /** The offer for the first missing pack, or null when [route] can translate now. */
    fun firstOffer(
        route: Route,
        statuses: List<PackStatus>,
    ): PackOffer? {
        val pack = missing(route, statuses).firstOrNull() ?: return null
        val status = statuses.firstOrNull { it.id == pack } ?: return PackOffer.Checking(pack)
        return offerFor(status)
    }

    private fun downloading(
        pack: PackId,
        state: PackState.Downloading,
    ): PackOffer {
        val percent = if (state.total > 0) (state.bytes * PERCENT / state.total).toInt() else 0
        return PackOffer.Downloading(pack, megabytes(state.bytes), megabytes(state.total), percent)
    }

    private fun waiting(
        pack: PackId,
        reason: PackWait,
    ): PackOffer =
        when (reason) {
            PackWait.FOR_WIFI -> PackOffer.WaitingForWifi(pack)
            PackWait.FOR_CONFIRMATION -> PackOffer.WaitingForConfirmation(pack)
        }

    private fun failed(
        pack: PackId,
        reason: PackFailure,
    ): PackOffer =
        when (reason) {
            is PackFailure.NoSpace -> PackOffer.NoSpace(pack, megabytes(reason.bytesNeeded))
            PackFailure.Network, PackFailure.Damaged, PackFailure.Other -> PackOffer.Failed(pack)
        }

    private fun unavailable(
        pack: PackId,
        reason: PackUnavailable,
    ): PackOffer =
        when (reason) {
            PackUnavailable.NO_STORE -> PackOffer.NoStore(pack)
            PackUnavailable.UNSUPPORTED_DEVICE -> PackOffer.Unsupported(pack)
        }
}
