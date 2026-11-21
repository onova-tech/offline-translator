package tech.onova.offlinetranslator.core.content

import android.app.Activity
import android.content.Context
import com.google.android.play.core.assetpacks.AssetPackException
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackErrorCode
import com.google.android.play.core.common.LocalTestingException
import com.google.android.play.core.ktx.requestFetch
import com.google.android.play.core.ktx.requestPackStates
import com.google.android.play.core.ktx.requestRemovePack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import tech.onova.offlinetranslator.core.domain.ports.ContentPacks
import tech.onova.offlinetranslator.core.domain.ports.PackFailure
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.PackState
import tech.onova.offlinetranslator.core.domain.ports.PackStatus
import tech.onova.offlinetranslator.core.domain.ports.PackWait
import java.io.File

/**
 * The language packs through Google Play's on-demand asset delivery (spec 012 research R5), the feature's only
 * network: a download starts only from [download], after the person confirmed the pack's size, and the store fetches
 * it. Packs are data only. A process-wide singleton: its listener follows a download wherever the person goes.
 * [screen] is the activity on screen, for Google Play's own confirmation dialog. [bundled] are the packs this build's
 * bundle holds (spec 080 research R3): every one in a release, only those with models in a debug build.
 */
public class PlayContentPacks(
    context: Context,
    private val known: List<PackId>,
    private val screen: () -> Activity?,
    private val bundled: Set<PackId> = known.toSet(),
) : ContentPacks {
    private val manager: AssetPackManager = AssetPackManagerFactory.getInstance(context.applicationContext)
    private val wifi = WifiCheck(context.applicationContext)
    private val space = FreeSpace(context.applicationContext)
    private val names = PlayPackMapping.asked(known, bundled)
    private val state = MutableStateFlow(PlayPackMapping.initial(known, bundled))
    override val packs: StateFlow<List<PackStatus>> = state.asStateFlow()

    private val listener = AssetPackStateUpdateListener { update(it) }

    init {
        manager.registerListener(listener)
    }

    override suspend fun refresh() {
        if (names.isEmpty()) return
        asking(
            refused = { code ->
                val answer = PlayPackMapping.failureOf(code, totalBytes = 0)
                state.update { list -> list.map { if (it.id in bundled) it.copy(state = answer) else it } }
            },
        ) {
            manager
                .requestPackStates(names)
                .packStates()
                .values
                .forEach(::update)
        }
    }

    override suspend fun download(
        pack: PackId,
        wifiOnly: Boolean,
    ) {
        val size = statusOf(pack)?.downloadBytes
        when {
            wifiOnly && !wifi.isOnWifi() -> {
                set(pack, PackState.Waiting(PackWait.FOR_WIFI))
            }

            size != null && space.availableBytes() < PlayPackMapping.spaceNeeded(size) -> {
                set(pack, PackState.Failed(PackFailure.NoSpace(PlayPackMapping.spaceNeeded(size))))
            }

            else -> {
                asking(refused = { code -> set(pack, PlayPackMapping.failureOf(code, size ?: 0)) }) {
                    manager
                        .requestFetch(listOf(pack.value))
                        .packStates()
                        .values
                        .forEach(::update)
                }
            }
        }
    }

    /**
     * Google Play asks before it goes on (an app it didn't install, research R5): its dialog, then its listener reports
     * the pack's new state. After an update from Play, the person downloads again.
     */
    override suspend fun confirm(pack: PackId) {
        val activity = screen() ?: return
        manager.showConfirmationDialog(activity)
    }

    /** The store stops it and removes what it had downloaded (FR-012). */
    override suspend fun cancel(pack: PackId) {
        asking(refused = {}) {
            manager
                .cancel(listOf(pack.value))
                .packStates()
                .values
                .forEach(::update)
        }
        if (statusOf(pack)?.state != PackState.Installed) set(pack, PackState.NotInstalled)
    }

    override suspend fun remove(pack: PackId) {
        var removed = true
        // When the store keeps it, its state stays as it was, and the caller sees that.
        asking(refused = { removed = false }) { manager.requestRemovePack(pack.value) }
        if (!removed) return
        state.update { list ->
            list.map {
                if (it.id ==
                    pack
                ) {
                    it.copy(installedBytes = null, state = PackState.NotInstalled)
                } else {
                    it
                }
            }
        }
        refresh()
    }

    /** The folder with the pack's `pack.json`, or null when it isn't installed or its files are missing. */
    override fun location(pack: PackId): String? {
        val root = manager.getPackLocation(pack.value)?.assetsPath() ?: return null
        val folder = File(root, pack.value)
        return folder.path.takeIf { File(folder, PACK_JSON).isFile }
    }

    private fun update(answer: AssetPackState) {
        val pack = PackId(answer.name())
        if (pack !in bundled || pack !in known) return
        val folder = location(pack)
        val packState =
            PlayPackMapping.stateOf(
                PlayPackMapping.Answer(
                    answer.status(),
                    answer.errorCode(),
                    answer.bytesDownloaded(),
                    answer.totalBytesToDownload(),
                ),
                filesOk = folder != null,
            )
        state.update { list ->
            list.map { status ->
                if (status.id != pack) return@map status
                status.copy(
                    downloadBytes = answer.totalBytesToDownload().takeIf { it > 0 } ?: status.downloadBytes,
                    installedBytes = if (packState == PackState.Installed) folder?.let(::bytesIn) else null,
                    state = packState,
                )
            }
        }
    }

    private fun statusOf(pack: PackId): PackStatus? = state.value.firstOrNull { it.id == pack }

    private fun set(
        pack: PackId,
        packState: PackState,
    ) {
        state.update { list -> list.map { if (it.id == pack) it.copy(state = packState) else it } }
    }

    private fun bytesIn(folder: String): Long =
        File(folder)
            .walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }

    private companion object {
        const val PACK_JSON = "pack.json"
    }
}

/**
 * Asks the store; a refusal comes back as Play's error code, never as a crash. Under bundletool's local testing,
 * a wiped app has lost the folder its packs came from: that reads as no store, since nothing can come from it.
 */
private inline fun asking(
    refused: (errorCode: Int) -> Unit,
    request: () -> Unit,
) {
    try {
        request()
    } catch (refusal: AssetPackException) {
        refused(refusal.errorCode)
    } catch (ignored: LocalTestingException) {
        refused(AssetPackErrorCode.APP_UNAVAILABLE)
    }
}
