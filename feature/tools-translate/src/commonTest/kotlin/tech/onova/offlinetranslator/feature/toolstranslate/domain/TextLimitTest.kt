package tech.onova.offlinetranslator.feature.toolstranslate.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Spec 012 FR-006: at most 5,000 characters are translated, and the rest is reported. */
class TextLimitTest {
    @Test
    fun emptyOrBlankTextMakesNoRequest() {
        assertNull(TextLimit.prepare(""))
        assertNull(TextLimit.prepare("  \n\t "))
    }

    @Test
    fun textIsTrimmed() {
        val input = TextLimit.prepare("  Olá, mundo!\n")!!
        assertEquals("Olá, mundo!", input.text)
        assertFalse(input.overLimit)
    }

    @Test
    fun onlyTheFirstFiveThousandCharactersAreTranslated() {
        val exactly = "a".repeat(TextLimit.MAX_CHARACTERS)
        assertFalse(TextLimit.prepare(exactly)!!.overLimit)
        val over = TextLimit.prepare(exactly + "bcd")!!
        assertTrue(over.overLimit)
        assertEquals(TextLimit.MAX_CHARACTERS, over.text.length)
        assertEquals(exactly, over.text)
    }

    @Test
    fun theCutNeverSplitsACharacterMadeOfTwoCodeUnits() {
        val emoji = "😀" // one character, two UTF-16 units
        val text = "a".repeat(TextLimit.MAX_CHARACTERS - 1) + emoji + "b"
        val input = TextLimit.prepare(text)!!
        assertTrue(input.overLimit)
        assertEquals(TextLimit.MAX_CHARACTERS - 1, input.text.length)
    }

    @Test
    fun theCounterCountsWhatWasTyped() {
        assertEquals(0, TextLimit.count(""))
        assertEquals(5, TextLimit.count(" Olá "))
        assertTrue(TextLimit.isOver(TextLimit.MAX_CHARACTERS + 1))
        assertFalse(TextLimit.isOver(TextLimit.MAX_CHARACTERS))
    }
}
