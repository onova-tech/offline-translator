package tech.onova.offlinetranslator.core.content

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Whether the phone is on Wi-Fi now, for "Download only on Wi-Fi" (spec 012 FR-011). Ethernet counts too: like Wi-Fi,
 * it isn't the mobile data the switch protects.
 */
internal class WifiCheck(
    context: Context,
) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    fun isOnWifi(): Boolean {
        val manager = connectivity ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return countsAsWifi(
            wifiOrEthernet =
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET),
            vpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN),
            notMetered = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
        )
    }

    internal companion object {
        /**
         * A VPN (Tailscale, a work VPN) is the active network on top of the Wi-Fi: it counts when what it runs over
         * isn't metered, which Android carries up to the VPN (the owner's phone, 2026-11-21: every download waited
         * for Wi-Fi).
         */
        fun countsAsWifi(
            wifiOrEthernet: Boolean,
            vpn: Boolean,
            notMetered: Boolean,
        ): Boolean = wifiOrEthernet || (vpn && notMetered)
    }
}
