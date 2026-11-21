package tech.onova.offlinetranslator.debug

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Debug builds add the phone probe under Settings → About (spec 012 T032); release builds add nothing. */
internal val debugAboutRows: (@Composable (Modifier) -> Unit)? = { modifier -> PhoneProbeRow(modifier) }
