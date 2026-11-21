package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals

/** A tip shown like a toast, with an (i) at its start so the eye goes to it (owner, 2026-11-09). */
public class HintVisuals(
    override val message: String,
) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Long
}
