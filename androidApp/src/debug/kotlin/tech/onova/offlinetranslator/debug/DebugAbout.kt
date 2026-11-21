package tech.onova.offlinetranslator.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing

/** Debug builds add the probes under Settings → About (spec 012 T032, spec 082 T003); release builds add nothing. */
internal val debugAboutRows: (@Composable (Modifier) -> Unit)? = { modifier ->
    Column(verticalArrangement = Arrangement.spacedBy(PocketSpacing.xs), modifier = modifier) {
        PhoneProbeRow()
        // Spec 082 T003: the app's own speech, timed on the phone.
        SpeechProbeRow()
    }
}
