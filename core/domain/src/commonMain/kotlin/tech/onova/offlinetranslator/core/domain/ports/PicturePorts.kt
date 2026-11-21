package tech.onova.offlinetranslator.core.domain.ports

/** A picture decoded into memory: [pixels] holds [width] × [height] ARGB colors in sRGB, row by row. */
public class Picture(
    public val width: Int,
    public val height: Int,
    public val pixels: IntArray,
)

/**
 * Opens a picture the user chose in the system photo picker (owner's phone pass, 2026-10-31: the
 * Color picker reads pictures too). The picture lives in memory only; nothing is saved or sent.
 */
public interface PictureLoader {
    /**
     * Decodes the picture at [reference] (on Android, the picker's content URI), scaled down so its
     * longer side is at most [maxSide] pixels and turned upright. Null when it cannot be read.
     */
    public suspend fun load(
        reference: String,
        maxSide: Int,
    ): Picture?
}
