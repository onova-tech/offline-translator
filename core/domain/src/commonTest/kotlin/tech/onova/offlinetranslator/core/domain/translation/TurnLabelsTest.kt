package tech.onova.offlinetranslator.core.domain.translation

import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Spec 082 story 5 (FR-014): each Conversation half names who spoke in its own language. */
class TurnLabelsTest {
    private fun of(code: String) = TurnLabels.of(LanguageCode(code))

    @Test
    fun everyLanguageOfTheCatalogHasTwoDifferentWordsEndingInAColon() {
        LanguageCatalog.all.forEach { language ->
            val label = TurnLabels.of(language.code)
            val code = language.code.value
            assertTrue(label.me.isNotBlank() && label.you.isNotBlank(), "$code has both words")
            assertNotEquals(label.me, label.you, "$code tells the two apart")
            assertTrue(label.me.last() in COLONS && label.you.last() in COLONS, "$code ends each with its colon")
        }
    }

    @Test
    fun eachLanguageWritesItsOwnWords() {
        assertEquals(TurnLabel("Me:", "You:"), of("en"))
        assertEquals(TurnLabel("Eu:", "Você:"), of("pt"))
        assertEquals(TurnLabel("أنا:", "أنت:"), of("ar"))
    }

    @Test
    fun theOtherPersonIsAddressedPolitely() {
        assertEquals("Sie:", of("de").you)
        assertEquals("Usted:", of("es").you)
        // French puts a no-break space before its colon.
        assertEquals("Vous :", of("fr").you)
    }

    @Test
    fun whereABareYouSoundsRudeTheOtherPersonIsNamed() {
        assertEquals(TurnLabel("私：", "相手："), of("ja"))
        assertEquals(TurnLabel("나:", "상대:"), of("ko"))
    }

    @Test
    fun aLanguageOutsideTheCatalogReadsInEnglish() {
        assertEquals(of("en"), of("xx"))
    }

    private companion object {
        val COLONS = setOf(':', '：')
    }
}
