package tech.onova.offlinetranslator.core.content

import android.content.Context
import android.os.StatFs

/** The free space where the store unpacks a pack: the app's own storage (spec 012 FR-012). */
internal class FreeSpace(
    private val context: Context,
) {
    fun availableBytes(): Long = StatFs(context.filesDir.path).availableBytes
}
