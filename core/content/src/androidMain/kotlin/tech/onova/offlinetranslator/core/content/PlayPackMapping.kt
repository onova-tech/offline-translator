package tech.onova.offlinetranslator.core.content

import com.google.android.play.core.assetpacks.model.AssetPackErrorCode
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import tech.onova.offlinetranslator.core.domain.ports.PackFailure
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.PackState
import tech.onova.offlinetranslator.core.domain.ports.PackStatus
import tech.onova.offlinetranslator.core.domain.ports.PackUnavailable
import tech.onova.offlinetranslator.core.domain.ports.PackWait

/** Google Play's pack states and errors in the port's words (spec 012 research R5). Pure, so it is tested alone. */
internal object PlayPackMapping {
    /** What the store says of one pack. */
    data class Answer(
        val status: Int,
        val errorCode: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long,
    )

    /** A pack needs room for its download and its unpacked files at once. */
    private const val SPACE_FACTOR = 2

    /** [filesOk]: an installed pack's `pack.json` is where it should be; without it the pack reads as not installed. */
    fun stateOf(
        answer: Answer,
        filesOk: Boolean,
    ): PackState =
        when (answer.status) {
            AssetPackStatus.NOT_INSTALLED, AssetPackStatus.CANCELED -> PackState.NotInstalled
            AssetPackStatus.PENDING -> PackState.Downloading(0, answer.totalBytes)
            AssetPackStatus.DOWNLOADING -> PackState.Downloading(answer.bytesDownloaded, answer.totalBytes)
            AssetPackStatus.TRANSFERRING -> PackState.Installing
            AssetPackStatus.COMPLETED -> if (filesOk) PackState.Installed else PackState.NotInstalled
            AssetPackStatus.WAITING_FOR_WIFI -> PackState.Waiting(PackWait.FOR_WIFI)
            AssetPackStatus.REQUIRES_USER_CONFIRMATION -> PackState.Waiting(PackWait.FOR_CONFIRMATION)
            AssetPackStatus.FAILED -> failureOf(answer.errorCode, answer.totalBytes)
            else -> PackState.Unknown
        }

    /** A failed answer or a refused request: an install the store doesn't know can never download (spec edge case). */
    fun failureOf(
        errorCode: Int,
        totalBytes: Long,
    ): PackState =
        when (errorCode) {
            AssetPackErrorCode.APP_UNAVAILABLE,
            AssetPackErrorCode.API_NOT_AVAILABLE,
            AssetPackErrorCode.APP_NOT_OWNED,
            AssetPackErrorCode.UNRECOGNIZED_INSTALLATION,
            AssetPackErrorCode.ACCESS_DENIED,
            -> PackState.Unavailable(PackUnavailable.NO_STORE)

            AssetPackErrorCode.NETWORK_ERROR -> PackState.Failed(PackFailure.Network)

            AssetPackErrorCode.INSUFFICIENT_STORAGE -> PackState.Failed(PackFailure.NoSpace(spaceNeeded(totalBytes)))

            else -> PackState.Failed(PackFailure.Other)
        }

    fun spaceNeeded(downloadBytes: Long): Long = downloadBytes * SPACE_FACTOR

    /**
     * The packs' first states (spec 080 research R3): a pack outside this build's bundle can never come, and Play would
     * refuse every question that names it, so it is unavailable at once; the others wait for the store's answer.
     */
    fun initial(
        known: List<PackId>,
        bundled: Set<PackId>,
    ): List<PackStatus> =
        known.map { pack ->
            val state = if (pack in bundled) PackState.Unknown else PackState.Unavailable(PackUnavailable.NO_STORE)
            PackStatus(pack, null, null, state)
        }

    /** The names Play is asked about: only the bundle's. */
    fun asked(
        known: List<PackId>,
        bundled: Set<PackId>,
    ): List<String> = known.filter { it in bundled }.map { it.value }
}
