package tech.onova.offlinetranslator.core.speech

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Spec 082 FR-017, the final review: Stop, then a quick Hear again, let the rest of the stopped reading play first,
 * because the next reading cleared the one shared "stopped" flag while the old one was still making a sentence.
 */
class SentenceReaderTest {
    private val heard = mutableListOf<String>()

    @Test
    fun aStopEndsTheReadingItCameDuringAndNotTheNextOne() {
        var next = 0L
        lateinit var reader: SentenceReader<String>
        reader =
            SentenceReader(
                make = { sentence ->
                    // Stop, then Hear again, while the old reading is still making its second sentence.
                    if (sentence == "Two.") {
                        reader.stop()
                        next = reader.begin()
                    }
                    sentence
                },
                play = { audio, stillReading -> if (stillReading()) heard += audio },
            )
        reader.read(reader.begin(), listOf("One.", "Two.", "Three."))
        reader.read(next, listOf("Again."))
        assertEquals(listOf("One.", "Again."), heard)
    }

    @Test
    fun aStopWhileASentencePlaysEndsItAtOnce() {
        lateinit var reader: SentenceReader<String>
        val stillReadingAfterStop = mutableListOf<Boolean>()
        reader =
            SentenceReader(
                make = { it },
                play = { audio, stillReading ->
                    heard += audio
                    reader.stop()
                    stillReadingAfterStop += stillReading()
                },
            )
        reader.read(reader.begin(), listOf("One.", "Two."))
        assertEquals(listOf("One."), heard)
        assertEquals(listOf(false), stillReadingAfterStop)
    }
}
