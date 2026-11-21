package tech.onova.offlinetranslator.feature.toolstranslate.fakes

import tech.onova.offlinetranslator.core.domain.ports.SettingsPage
import tech.onova.offlinetranslator.core.domain.ports.SettingsPages

/** The phone's settings pages: it records which were opened. */
class FakeSettingsPages : SettingsPages {
    val opened = mutableListOf<SettingsPage>()

    override fun open(page: SettingsPage) {
        opened += page
    }
}
