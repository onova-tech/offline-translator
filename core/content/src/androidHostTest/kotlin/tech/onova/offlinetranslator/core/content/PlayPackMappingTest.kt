package tech.onova.offlinetranslator.core.content

import com.google.android.play.core.assetpacks.model.AssetPackErrorCode
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import tech.onova.offlinetranslator.core.domain.ports.PackFailure
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.PackState
import tech.onova.offlinetranslator.core.domain.ports.PackUnavailable
import tech.onova.offlinetranslator.core.domain.ports.PackWait
import kotlin.test.Test
import kotlin.test.assertEquals

/** Spec 012 research R5: Google Play's pack states and errors, in the port's words. */
class PlayPackMappingTest {
    private val total = 48_000_000L

    private fun state(
        status: Int,
        error: Int = AssetPackErrorCode.NO_ERROR,
        bytes: Long = 0,
        filesOk: Boolean = true,
    ) = PlayPackMapping.stateOf(PlayPackMapping.Answer(status, error, bytes, total), filesOk)

    /**
     * Spec 080 research R3: Play refuses a question about a pack the bundle doesn't hold, and with it the whole list.
     * A debug bundle holds only the packs with models, so the others are unavailable from the start and never asked.
     */
    @Test
    fun aPackOutsideTheBundleIsUnavailableAndOnlyTheBundledOnesAreAsked() {
        val pt = PackId("translate_pt")
        val ja = PackId("translate_ja")
        val start = PlayPackMapping.initial(known = listOf(pt, ja), bundled = setOf(pt))
        assertEquals(listOf(PackState.Unknown, PackState.Unavailable(PackUnavailable.NO_STORE)), start.map { it.state })
        assertEquals(listOf(pt, ja), start.map { it.id })
        assertEquals(listOf("translate_pt"), PlayPackMapping.asked(listOf(pt, ja), setOf(pt)))
    }

    @Test
    fun notInstalledOrCancelledCanBeDownloaded() {
        assertEquals(PackState.NotInstalled, state(AssetPackStatus.NOT_INSTALLED))
        assertEquals(PackState.NotInstalled, state(AssetPackStatus.CANCELED))
        assertEquals(PackState.Unknown, state(AssetPackStatus.UNKNOWN))
    }

    @Test
    fun aDownloadShowsItsBytesThenUnpacks() {
        assertEquals(PackState.Downloading(0, total), state(AssetPackStatus.PENDING))
        assertEquals(PackState.Downloading(12_000_000, total), state(AssetPackStatus.DOWNLOADING, bytes = 12_000_000))
        assertEquals(PackState.Installing, state(AssetPackStatus.TRANSFERRING))
    }

    @Test
    fun aCompletedPackIsInstalledOnlyWhenItsFilesAreThere() {
        assertEquals(PackState.Installed, state(AssetPackStatus.COMPLETED))
        assertEquals(
            PackState.NotInstalled,
            state(AssetPackStatus.COMPLETED, filesOk = false),
            "damaged: offered again",
        )
    }

    @Test
    fun theStoreWaitingIsShownAsWaiting() {
        assertEquals(PackState.Waiting(PackWait.FOR_WIFI), state(AssetPackStatus.WAITING_FOR_WIFI))
        assertEquals(PackState.Waiting(PackWait.FOR_CONFIRMATION), state(AssetPackStatus.REQUIRES_USER_CONFIRMATION))
    }

    @Test
    fun failuresSayWhatHappened() {
        assertEquals(
            PackState.Failed(PackFailure.Network),
            state(AssetPackStatus.FAILED, AssetPackErrorCode.NETWORK_ERROR),
        )
        assertEquals(
            PackState.Failed(PackFailure.NoSpace(PlayPackMapping.spaceNeeded(total))),
            state(AssetPackStatus.FAILED, AssetPackErrorCode.INSUFFICIENT_STORAGE),
        )
        assertEquals(
            PackState.Failed(PackFailure.Other),
            state(AssetPackStatus.FAILED, AssetPackErrorCode.INTERNAL_ERROR),
        )
    }

    @Test
    fun anInstallThePlayStoreDoesntKnowCanNeverDownload() {
        for (code in listOf(
            AssetPackErrorCode.APP_UNAVAILABLE,
            AssetPackErrorCode.API_NOT_AVAILABLE,
            AssetPackErrorCode.APP_NOT_OWNED,
            AssetPackErrorCode.UNRECOGNIZED_INSTALLATION,
            AssetPackErrorCode.ACCESS_DENIED,
        )) {
            assertEquals(
                PackState.Unavailable(PackUnavailable.NO_STORE),
                PlayPackMapping.failureOf(code, total),
                "$code",
            )
        }
        assertEquals(
            PackState.Failed(PackFailure.Network),
            PlayPackMapping.failureOf(AssetPackErrorCode.NETWORK_ERROR, total),
        )
    }

    @Test
    fun theSpaceNeededLeavesRoomToUnpack() {
        assertEquals(96_000_000L, PlayPackMapping.spaceNeeded(total))
    }
}
