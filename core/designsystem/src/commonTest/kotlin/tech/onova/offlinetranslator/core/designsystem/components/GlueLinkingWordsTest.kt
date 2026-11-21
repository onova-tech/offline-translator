package tech.onova.offlinetranslator.core.designsystem.components

import kotlin.test.Test
import kotlin.test.assertEquals

class GlueLinkingWordsTest {
    private val nb = ' '

    @Test
    fun linkingWordStaysWithThePreviousWord() {
        assertEquals("Conversor${nb}de Medidas", glueLinkingWords("Conversor de Medidas"))
        assertEquals("Unit${nb}of Measure", glueLinkingWords("Unit of Measure"))
    }

    @Test
    fun lastWordAndCapitalisedWordsAreNotJoined() {
        assertEquals("Medidor${nb}de luz", glueLinkingWords("Medidor de luz"))
        assertEquals("Hora Mundial Sul", glueLinkingWords("Hora Mundial Sul"))
    }

    @Test
    fun shortTextsAreUntouched() {
        assertEquals("Régua", glueLinkingWords("Régua"))
        assertEquals("Contador de", glueLinkingWords("Contador de"))
    }
}
