package tech.onova.offlinetranslator.core.speech

import java.util.concurrent.atomic.AtomicLong

/**
 * A text read aloud sentence by sentence, the first heard while the next is made (research R3). Each reading has its
 * own number: a stop ends the reading that was going on when it came, never a later one. One shared "stopped" flag was
 * cleared by the next reading, so a Stop then a quick Hear again let the rest of the old reading play first, while its
 * last sentence was still being made (spec 082, the final review).
 */
internal class SentenceReader<A>(
    private val make: (String) -> A,
    private val play: (audio: A, stillReading: () -> Boolean) -> Unit,
) {
    private val current = AtomicLong()

    /** A new reading; whatever was being read is over. */
    fun begin(): Long = current.incrementAndGet()

    /** Ends the reading going on now. */
    fun stop() {
        current.incrementAndGet()
    }

    /** Reads [sentences] for [reading], as long as it is the one going on. */
    fun read(
        reading: Long,
        sentences: List<String>,
    ) {
        val stillReading = { current.get() == reading }
        for (sentence in sentences) {
            if (!stillReading()) return
            val audio = make(sentence)
            if (!stillReading()) return
            play(audio, stillReading)
        }
    }
}
