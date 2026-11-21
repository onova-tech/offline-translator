package tech.onova.offlinetranslator.core.translation

/**
 * The JNI functions of `libbergamot.so` (core/translation/native/bergamot_jni.cpp). Text crosses as UTF-8 bytes; an
 * error comes back as an [IllegalStateException]. Not thread-safe: [BergamotEngine] calls it from one thread.
 */
internal object BergamotNative {
    /** Registers the directions (one entry per direction in each array); returns the engine's handle. */
    @JvmStatic
    external fun open(
        names: Array<String>,
        models: Array<String>,
        shortlists: Array<String>,
        sourceVocabs: Array<String>,
        targetVocabs: Array<String>,
    ): Long

    /** Translates with the direction [first], then [second] when given (through English). */
    @JvmStatic
    external fun translate(
        handle: Long,
        first: String,
        second: String?,
        text: ByteArray,
    ): ByteArray

    @JvmStatic
    external fun close(handle: Long)
}
