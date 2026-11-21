package tech.onova.offlinetranslator.core.domain.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class AppLanguageTest {
    @Test
    fun parsesSupportedTagsIgnoringCase() {
        assertEquals(AppLanguage.PT_BR, AppLanguage.fromTag("pt-BR"))
        assertEquals(AppLanguage.PT_BR, AppLanguage.fromTag("pt-br"))
        assertEquals(AppLanguage.EN, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.ES, AppLanguage.fromTag("es"))
    }

    @Test
    fun unknownOrMissingTagsFollowTheSystem() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag("fr"))
    }

    @Test
    fun defaultSettingsFollowTheContract() {
        assertEquals(AppSettings(theme = ThemeMode.SYSTEM, dynamicColor = true, unitSystem = null), AppSettings())
    }
}
