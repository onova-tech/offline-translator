package tech.onova.offlinetranslator.core.domain.translation

import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.PackId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Spec 080 research R1: the generated catalog of the Translator's languages. */
class LanguageCatalogTest {
    private fun of(code: String) = LanguageCatalog.of(LanguageCode(code))

    @Test
    fun englishAndFiftyThreeMoreEachWithItsOwnPack() {
        assertEquals(54, LanguageCatalog.all.size)
        assertEquals(LanguageCode.ENGLISH, LanguageCatalog.all.first().code)
        assertNull(LanguageCatalog.all.first().pack, "English is built in")
        val others = LanguageCatalog.all.drop(1)
        assertEquals(others.size, others.map { it.code }.toSet().size)
        assertTrue(others.all { it.pack == PackId("translate_${it.code.value}") })
    }

    @Test
    fun norwegianIsBokmalOnly() {
        assertEquals("nb", of("nb")?.tag)
        assertNull(of("no"), "the same written language as nb (owner, 2026-11-19)")
    }

    @Test
    fun chineseInBothScriptsHasItsOwnNamesAndRegions() {
        assertEquals("zh-Hans", of("zh")?.tag)
        assertEquals("zh-CN", of("zh")?.speechRegion)
        assertEquals("zh-Hant", of("zh_hant")?.tag)
        assertEquals("zh-TW", of("zh_hant")?.speechRegion)
    }

    @Test
    fun theSmallerModelsAreBasicQuality() {
        assertEquals(LanguageQuality.BASIC, of("tr")?.quality)
        assertEquals(LanguageQuality.FULL, of("ja")?.quality)
        assertEquals(LanguageQuality.FULL, of("pt")?.quality)
        assertNull(of("xx"))
    }
}
