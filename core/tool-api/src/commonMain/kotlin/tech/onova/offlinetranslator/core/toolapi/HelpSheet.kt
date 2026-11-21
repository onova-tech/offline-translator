package tech.onova.offlinetranslator.core.toolapi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.stringResource
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing
import tech.onova.offlinetranslator.core.toolapi.resources.Res
import tech.onova.offlinetranslator.core.toolapi.resources.host_help
import tech.onova.offlinetranslator.core.toolapi.resources.host_help_close

/**
 * "How to use", over the screen: the host's help sheet for a tool's [ToolEntry.HelpContent], and for a page of a tool
 * that has its own help (spec 080: Manage languages). [content] is usually a [ToolHelp].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun HelpSheet(
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PocketSpacing.lg, vertical = PocketSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PocketSpacing.md),
        ) {
            Text(
                text = stringResource(Res.string.host_help),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            content()
            TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(Res.string.host_help_close))
            }
        }
    }
}
