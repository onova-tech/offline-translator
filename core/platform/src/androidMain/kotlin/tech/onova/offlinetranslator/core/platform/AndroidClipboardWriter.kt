package tech.onova.offlinetranslator.core.platform

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import tech.onova.offlinetranslator.core.domain.ports.ClipboardWriter

/** The system clipboard; Android 13 and later confirm a copy themselves (research.md R13). */
public class AndroidClipboardWriter(
    private val context: Context,
) : ClipboardWriter {
    override fun copy(
        label: String,
        text: String,
    ): Boolean {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    /** Marked sensitive on Android 13 and later, which then shows dots in the copy preview. */
    override fun copySecret(
        label: String,
        text: String,
    ): Boolean {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val clip = ClipData.newPlainText(label, text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
        }
        clipboard.setPrimaryClip(clip)
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }
}
