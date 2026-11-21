package tech.onova.offlinetranslator.feature.toolstranslate.domain

import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.Route
import kotlin.test.Test
import kotlin.test.assertEquals

/** Spec 012 FR-004, FR-005: which models a translation needs. */
class RoutesTest {
    private val en = LanguageCode("en")
    private val pt = LanguageCode("pt")
    private val es = LanguageCode("es")
    private val ptPack = PackId("translate_pt")
    private val esPack = PackId("translate_es")

    @Test
    fun theSameLanguageNeedsNoModel() {
        assertEquals(Route.Same, Routes.between(pt, pt))
        assertEquals(emptyList(), Routes.packsFor(Route.Same))
    }

    @Test
    fun englishOnOneSideUsesTheOtherLanguagesPack() {
        assertEquals(Route.Direct(en, pt, ptPack), Routes.between(en, pt))
        assertEquals(Route.Direct(pt, en, ptPack), Routes.between(pt, en))
        assertEquals(Route.Direct(es, en, esPack), Routes.between(es, en))
        assertEquals(listOf(ptPack), Routes.packsFor(Routes.between(en, pt)))
    }

    @Test
    fun twoOtherLanguagesGoThroughEnglishInOrder() {
        assertEquals(Route.ThroughEnglish(pt, es, ptPack, esPack), Routes.between(pt, es))
        assertEquals(Route.ThroughEnglish(es, pt, esPack, ptPack), Routes.between(es, pt))
        assertEquals(listOf(esPack, ptPack), Routes.packsFor(Routes.between(es, pt)))
    }

    @Test
    fun aPacksNameComesFromItsLanguage() {
        assertEquals(PackId("translate_pt"), Routes.packOf(pt))
        assertEquals(pt, Routes.languageOf(PackId("translate_pt")))
        assertEquals(null, Routes.languageOf(PackId("something_else")))
    }
}
