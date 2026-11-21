package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import tech.onova.offlinetranslator.core.designsystem.icons.PocketIcons
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketShapes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing

/** Pill-shaped search field with a visible label-as-placeholder and a clear button. */
@Composable
public fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    clearLabel: String,
    modifier: Modifier = Modifier,
) {
    // Material's own keeps 48 dp for its icons, however small the rest gets.
    if (PocketSizes.textFieldHeight < OutlinedTextFieldDefaults.MinHeight) {
        CompactSearchField(query, onQueryChange, placeholder, clearLabel, modifier)
        return
    }
    TextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(PocketIcons.Search, contentDescription = null) },
        trailingIcon =
            if (query.isNotEmpty()) {
                { IconAction(icon = PocketIcons.Close, label = clearLabel, onClick = { onQueryChange("") }) }
            } else {
                null
            },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = PocketShapes.pill,
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun CompactSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    clearLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = LocalTextStyle.current.merge(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Row(
                modifier =
                    Modifier
                        .clip(PocketShapes.pill)
                        .background(colors.surfaceContainerHigh)
                        .heightIn(min = PocketSizes.textFieldHeight)
                        .padding(horizontal = PocketSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PocketSpacing.xs),
            ) {
                Icon(PocketIcons.Search, contentDescription = null, tint = colors.onSurfaceVariant)
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        WholeWordText(
                            text = placeholder,
                            style = LocalTextStyle.current,
                            color = colors.onSurfaceVariant,
                            singleLine = true,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    IconAction(icon = PocketIcons.Close, label = clearLabel, onClick = { onQueryChange("") })
                }
            }
        },
    )
}
