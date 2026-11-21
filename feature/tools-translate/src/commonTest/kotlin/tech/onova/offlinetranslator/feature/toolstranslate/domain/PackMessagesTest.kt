package tech.onova.offlinetranslator.feature.toolstranslate.domain

import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.PackFailure
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.PackState
import tech.onova.offlinetranslator.core.domain.ports.PackStatus
import tech.onova.offlinetranslator.core.domain.ports.PackUnavailable
import tech.onova.offlinetranslator.core.domain.ports.PackWait
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Spec 012 story 2 and contracts/ui.md "The pack offer": what the person sees for each pack state. */
class PackMessagesTest {
    private val pt = PackId("translate_pt")
    private val es = PackId("translate_es")

    private fun status(
        state: PackState,
        id: PackId = pt,
    ) = PackStatus(id, downloadBytes = 47_600_000, installedBytes = null, state = state)

    @Test
    fun eachStateHasItsOffer() {
        assertEquals(PackOffer.Offer(pt, 48), PackMessages.offerFor(status(PackState.NotInstalled)))
        assertEquals(PackOffer.Checking(pt), PackMessages.offerFor(status(PackState.Unknown)))
        assertEquals(
            PackOffer.Downloading(pt, doneMegabytes = 24, totalMegabytes = 48, percent = 50),
            PackMessages.offerFor(status(PackState.Downloading(23_800_000, 47_600_000))),
        )
        assertEquals(PackOffer.Installing(pt), PackMessages.offerFor(status(PackState.Installing)))
        assertEquals(PackOffer.WaitingForWifi(pt), PackMessages.offerFor(status(PackState.Waiting(PackWait.FOR_WIFI))))
        assertEquals(
            PackOffer.WaitingForConfirmation(pt),
            PackMessages.offerFor(status(PackState.Waiting(PackWait.FOR_CONFIRMATION))),
        )
        assertEquals(
            PackOffer.NoSpace(pt, neededMegabytes = 75),
            PackMessages.offerFor(status(PackState.Failed(PackFailure.NoSpace(75_000_000)))),
        )
        assertEquals(PackOffer.Failed(pt), PackMessages.offerFor(status(PackState.Failed(PackFailure.Network))))
        assertEquals(PackOffer.Failed(pt), PackMessages.offerFor(status(PackState.Failed(PackFailure.Damaged))))
        assertEquals(
            PackOffer.NoStore(pt),
            PackMessages.offerFor(status(PackState.Unavailable(PackUnavailable.NO_STORE))),
        )
        assertEquals(
            PackOffer.Unsupported(pt),
            PackMessages.offerFor(status(PackState.Unavailable(PackUnavailable.UNSUPPORTED_DEVICE))),
        )
        assertNull(PackMessages.offerFor(status(PackState.Installed)))
    }

    @Test
    fun sizesRoundUpToWholeMegabytes() {
        assertEquals(0, PackMessages.megabytes(0))
        assertEquals(1, PackMessages.megabytes(1))
        assertEquals(1, PackMessages.megabytes(1_000_000))
        assertEquals(2, PackMessages.megabytes(1_000_001))
        assertEquals(48, PackMessages.megabytes(47_600_000))
    }

    @Test
    fun anUnknownSizeStillOffersTheDownload() {
        val offer =
            PackMessages.offerFor(
                PackStatus(pt, downloadBytes = null, installedBytes = null, PackState.NotInstalled),
            )
        assertEquals(PackOffer.Offer(pt, megabytes = null), offer)
    }

    @Test
    fun aRouteThroughEnglishOffersOnlyTheMissingPack() {
        val route = Routes.between(LanguageCode("pt"), LanguageCode("es"))
        val statuses = listOf(status(PackState.Installed, pt), status(PackState.NotInstalled, es))
        assertEquals(listOf(es), PackMessages.missing(route, statuses))
        assertEquals(PackOffer.Offer(es, 48), PackMessages.firstOffer(route, statuses))
        val bothIn = listOf(status(PackState.Installed, pt), status(PackState.Installed, es))
        assertNull(PackMessages.firstOffer(route, bothIn))
    }

    @Test
    fun aPackTheStoreDoesntListIsCheckedFirst() {
        val route = Routes.between(LanguageCode("en"), LanguageCode("pt"))
        assertEquals(PackOffer.Checking(pt), PackMessages.firstOffer(route, emptyList()))
    }
}
