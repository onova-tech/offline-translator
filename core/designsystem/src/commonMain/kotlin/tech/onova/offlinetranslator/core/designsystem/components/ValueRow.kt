package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing

/** A label and its value on one line, read together ("Voltage, 3.87 V"); long values wrap at the end. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun ValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    // When label and value don't fit on one line (large text), the value drops under the label.
    FlowRow(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(PocketSpacing.xxs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.End,
        )
    }
}
