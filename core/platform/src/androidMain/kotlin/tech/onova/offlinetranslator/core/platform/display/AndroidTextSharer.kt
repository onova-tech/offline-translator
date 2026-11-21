package tech.onova.offlinetranslator.core.platform.display

import android.content.Context
import android.content.Intent
import tech.onova.offlinetranslator.core.domain.ports.TextSharer

/** Hands text to the system share sheet; the app itself never opens what is shared (spec 006 FR-052). */
public class AndroidTextSharer(
    private val context: Context,
) : TextSharer {
    override fun share(text: String) {
        val send =
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, text)
        val chooser = Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
