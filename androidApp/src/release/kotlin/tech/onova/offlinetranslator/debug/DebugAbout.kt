package tech.onova.offlinetranslator.debug

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Release builds add nothing under Settings → About; debug builds add the phone probe (spec 012 T032). */
internal val debugAboutRows: (@Composable (Modifier) -> Unit)? = null
