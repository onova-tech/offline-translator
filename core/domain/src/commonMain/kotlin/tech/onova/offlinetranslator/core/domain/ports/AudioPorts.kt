package tech.onova.offlinetranslator.core.domain.ports

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** One event of the microphone: a chunk of samples, or the news that the microphone is not available. */
public sealed interface AudioInputEvent {
    /** Mono float samples in −1…1. The array is owned by the receiver. */
    public class Chunk(
        public val samples: FloatArray,
    ) : AudioInputEvent

    /** The microphone could not be opened or was taken (a call); the flow retries every second. */
    public data object Unavailable : AudioInputEvent
}

/** The microphone (spec 007). Audio is analyzed as it arrives and never stored or sent. */
public interface AudioInput {
    public val sampleRate: Int

    /**
     * Cold flow: the microphone opens when collected and is released when the collector is cancelled.
     * [frames] samples per chunk.
     */
    public fun chunks(frames: Int): Flow<AudioInputEvent>
}

/** Fills [buffer] with [frames] mono samples; called on the audio thread, must not block or allocate. */
public fun interface SampleGenerator {
    public fun fill(
        buffer: FloatArray,
        frames: Int,
    )
}

/** A running sound from a [SampleGenerator]; closing it releases the audio output. */
public interface AudioPlayback : AutoCloseable {
    /** True after [stop], when another app takes the audio, or after a write error. */
    public val stopped: StateFlow<Boolean>

    public fun stop()
}

/** The speaker (spec 007). */
public interface AudioOutput {
    public val sampleRate: Int

    public fun start(generator: SampleGenerator): AudioPlayback
}
