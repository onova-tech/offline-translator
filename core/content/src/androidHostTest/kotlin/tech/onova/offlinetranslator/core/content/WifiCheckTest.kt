package tech.onova.offlinetranslator.core.content

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** "Download only on Wi-Fi" (spec 012 FR-011): what counts as Wi-Fi. */
class WifiCheckTest {
    @Test
    fun wifiAndEthernetCount() {
        assertTrue(WifiCheck.countsAsWifi(wifiOrEthernet = true, vpn = false, notMetered = true))
        assertTrue(WifiCheck.countsAsWifi(wifiOrEthernet = true, vpn = false, notMetered = false))
    }

    /** The owner's phone on Tailscale, 2026-11-21: the VPN is the active network, over Wi-Fi. */
    @Test
    fun aVpnOverWifiCounts() {
        assertTrue(WifiCheck.countsAsWifi(wifiOrEthernet = false, vpn = true, notMetered = true))
    }

    @Test
    fun aVpnOverMobileDataAndMobileDataDoNot() {
        assertFalse(WifiCheck.countsAsWifi(wifiOrEthernet = false, vpn = true, notMetered = false))
        assertFalse(WifiCheck.countsAsWifi(wifiOrEthernet = false, vpn = false, notMetered = false))
    }
}
