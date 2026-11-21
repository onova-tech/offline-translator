package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * A full-screen notice or confirmation (a safety notice, "Clear all?"): the title and the text scroll in
 * the middle, and the confirm and cancel buttons sit at the bottom, like every tool's controls (CLAUDE.md
 * screen layout rules). Back cancels.
 */
@Composable
public fun ConfirmScreen(
    title: String,
    text: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            ToolLayout(
                bottom = {
                    BottomAction(label = confirmLabel, onClick = onConfirm)
                    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text(cancelLabel) }
                },
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth().semantics { heading() },
                )
                Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
