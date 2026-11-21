package tech.onova.offlinetranslator.core.domain.ports

import kotlinx.coroutines.flow.StateFlow
import kotlin.jvm.JvmInline

/** A content pack's permanent name, as the store knows it, e.g. `translate_pt` (spec 012). */
@JvmInline
public value class PackId(
    public val value: String,
)

/** Why a pack can never be downloaded on this phone. */
public enum class PackUnavailable {
    /** No Google Play (a sideloaded install): nothing can deliver the pack. */
    NO_STORE,

    /** The phone's processor has no engine for it (a 32-bit phone, spec 012 research R3). */
    UNSUPPORTED_DEVICE,
}

/** Why a download is waiting. */
public enum class PackWait {
    /** "Download only on Wi-Fi" is on and the phone isn't on Wi-Fi (spec 012 FR-011). */
    FOR_WIFI,

    /** The store asks the person to confirm a download on mobile data. */
    FOR_CONFIRMATION,
}

/** Why a download failed; every one leaves no partial pack and can be tried again (spec 012 FR-012). */
public sealed interface PackFailure {
    /** The phone has too little free space; [bytesNeeded] is what the pack needs. */
    public data class NoSpace(
        val bytesNeeded: Long,
    ) : PackFailure

    public data object Network : PackFailure

    /** An installed pack's files are missing or unreadable; it reads as not installed. */
    public data object Damaged : PackFailure

    public data object Other : PackFailure
}

/** Where a pack is in its life (spec 012 data-model.md, "PackState"). */
public sealed interface PackState {
    /** The store hasn't answered yet. */
    public data object Unknown : PackState

    public data object NotInstalled : PackState

    public data class Waiting(
        val reason: PackWait,
    ) : PackState

    public data class Downloading(
        val bytes: Long,
        val total: Long,
    ) : PackState

    public data object Installing : PackState

    public data object Installed : PackState

    public data class Failed(
        val reason: PackFailure,
    ) : PackState

    public data class Unavailable(
        val reason: PackUnavailable,
    ) : PackState
}

/** One pack with its sizes: [downloadBytes] before a download, [installedBytes] once it's in. */
public data class PackStatus(
    val id: PackId,
    val downloadBytes: Long?,
    val installedBytes: Long?,
    val state: PackState,
)

/**
 * On-demand content packs, such as the Translator's language packs (constitution Principle I, spec 012). The only
 * network in the feature: a download starts only from [download], after the person confirmed the pack's size, and it
 * is delivered by the platform's store. Packs hold data only.
 */
public interface ContentPacks {
    /** Every pack the app knows, with its state; it updates while a download runs. */
    public val packs: StateFlow<List<PackStatus>>

    /** Asks the store for the packs' sizes and states; nothing is downloaded. */
    public suspend fun refresh()

    /** Starts the download the person confirmed; checks Wi-Fi (when [wifiOnly]) and free space first. */
    public suspend fun download(
        pack: PackId,
        wifiOnly: Boolean,
    )

    /**
     * Shows the store's own confirmation for a pack in `Waiting(FOR_CONFIRMATION)` (Google Play's dialog); once the
     * person accepts it there, the store goes on by itself and the state follows. Nothing when no screen is showing.
     */
    public suspend fun confirm(pack: PackId)

    /** Stops a download and leaves nothing behind. */
    public suspend fun cancel(pack: PackId)

    /** Deletes an installed pack and frees its space; when the store refuses, the pack's state stays as it was. */
    public suspend fun remove(pack: PackId)

    /** The folder holding an installed pack's files, or null when it isn't installed. */
    public fun location(pack: PackId): String?
}
