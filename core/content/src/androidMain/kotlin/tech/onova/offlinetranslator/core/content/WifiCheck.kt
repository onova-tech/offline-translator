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
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
