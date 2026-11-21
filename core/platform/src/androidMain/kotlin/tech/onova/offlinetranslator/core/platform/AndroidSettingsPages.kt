package tech.onova.offlinetranslator.core.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import tech.onova.offlinetranslator.core.domain.ports.SettingsPage
import tech.onova.offlinetranslator.core.domain.ports.SettingsPages

/** The phone's Wi-Fi and storage settings (spec 012 story 2); a phone without that page opens its settings' start. */
public class AndroidSettingsPages(
    context: Context,
) : SettingsPages {
    private val context = context.applicationContext

    override fun open(page: SettingsPage) {
        val action =
            when (page) {
                SettingsPage.NETWORK -> Settings.ACTION_WIFI_SETTINGS
                SettingsPage.STORAGE -> Settings.ACTION_INTERNAL_STORAGE_SETTINGS
            }
        if (!start(action)) start(Settings.ACTION_SETTINGS)
    }

    private fun start(action: String): Boolean =
        try {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (ignored: ActivityNotFoundException) {
            false
        }
}
