package tech.onova.offlinetranslator.feature.toolstranslate.domain

import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import kotlin.test.Test
import kotlin.test.assertEquals

/** Spec 080 data-model.md "CheckedLanguages": the languages the lists show, at least two. */
class CheckedLanguagesTest {
    private val en = LanguageCode.ENGLISH
    private val pt = LanguageCode("pt")
    private val es = LanguageCode("es")
    private val ja = LanguageCode("ja")
    private val fr = LanguageCode("fr")

    @Test
    fun nothingStoredGivesEnglishPortugueseAndSpanish() {
        assertEquals(listOf(en, pt, es), CheckedLanguages.resolve(null))
    }

    @Test
    fun unknownAndRepeatedLanguagesAreDroppedAndFewerThanTwoGiveTheDefault() {
        assertEquals(listOf(ja, fr), CheckedLanguages.resolve(listOf(ja, LanguageCode("xx"), fr, ja)))
        assertEquals(listOf(en, pt, es), CheckedLanguages.resolve(listOf(ja, LanguageCode("xx"))))
        assertEquals(listOf(en, pt, es), CheckedLanguages.resolve(emptyList()))
    }

    @Test
    fun aToggleAddsOrRemovesButNeverBelowTwo() {
        assertEquals(listOf(en, pt, es, ja), CheckedLanguages.toggle(listOf(en, pt, es), ja))
        assertEquals(listOf(en, es), CheckedLanguages.toggle(listOf(en, pt, es), pt))
        assertEquals(listOf(en, es), CheckedLanguages.toggle(listOf(en, es), es), "unchanged: two stay")
    }

    @Test
    fun withTwoCheckedBothAreLocked() {
        assertEquals(setOf(en, es), CheckedLanguages.locked(listOf(en, es)))
        assertEquals(emptySet(), CheckedLanguages.locked(listOf(en, pt, es)))
    }

    @Test
    fun anUncheckedChoiceTakesTheFirstByNameThatIsntTheOtherSide() {
        // In name order (the app's language), Japanese before Portuguese.
        assertEquals(ja, CheckedLanguages.replacementFor(other = pt, byName = listOf(ja, pt)))
        assertEquals(pt, CheckedLanguages.replacementFor(other = ja, byName = listOf(ja, pt)))
    }
}
