package tech.onova.offlinetranslator.feature.toolstranslate.domain

import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import kotlin.test.Test
import kotlin.test.assertEquals

/** Spec 012 FR-027 and ui-rules A6: Copy and Share send the conversation as the screen shows it. */
class ConversationTextTest {
    private val pt = LanguageCode("pt")
    private val es = LanguageCode("es")
    private val names = mapOf(pt to "Português", es to "Español")

    @Test
    fun eachTurnIsWhatWasHeardThenItsTranslation() {
        val turns =
            listOf(
                Turn(1, Side.NEAR, pt, es, heard = "Onde fica a estação?", translation = "¿Dónde está la estación?"),
                Turn(2, Side.FAR, es, pt, heard = "A la derecha.", translation = "À direita."),
            )
        assertEquals(
            "Português: Onde fica a estação?\nEspañol: ¿Dónde está la estación?\n" +
                "Español: A la derecha.\nPortuguês: À direita.",
            ConversationText.format(turns) { names.getValue(it) },
        )
    }

    @Test
    fun aTurnStillTranslatingHasOnlyWhatWasHeard() {
        val turns = listOf(Turn(1, Side.NEAR, pt, es, heard = "Obrigado"))
        assertEquals("Português: Obrigado", ConversationText.format(turns) { names.getValue(it) })
    }

    @Test
    fun noTurnsIsEmpty() {
        assertEquals("", ConversationText.format(emptyList()) { names.getValue(it) })
    }
}
