package tech.onova.offlinetranslator.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals

/** Spec 080 research R4: the phone names the languages, in the app's language and in their own. */
class AndroidLanguageNamesTest {
    private val names = AndroidLanguageNames()

    @Test
    fun inTheAppsLanguageCapitalisedForAList() {
        assertEquals("Japanese", names.nameOf("ja", "en"))
        assertEquals("Japonês", names.nameOf("ja", "pt"))
        assertEquals("Japonés", names.nameOf("ja", "es"))
        assertEquals("Chinese (Traditional)", names.nameOf("zh-Hant", "en"))
    }

    @Test
    fun inTheirOwnLanguage() {
        assertEquals("日本語", names.ownName("ja"))
        assertEquals("Français", names.ownName("fr"))
    }
}
