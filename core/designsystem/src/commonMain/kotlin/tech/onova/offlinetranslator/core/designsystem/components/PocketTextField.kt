package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing

/**
 * The app's outlined text field: Material's, assembled here because its own keeps 56 dp and its paddings however small
 * the text gets (owner, 2026-11-06). From phone size up and with a larger text setting it looks exactly like
 * Material's; in a reduced window it goes down with the touch target. With [errorText] it shows as wrong, and that's
 * what a screen reader says. What is typed takes its own direction, so Arabic or Hebrew starts at the right (spec 080
 * FR-011); in the app's languages it reads as before.
 */
@Composable
public fun PocketTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    errorText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors()
    val focused by interactionSource.collectIsFocusedAsState()
    val height = PocketSizes.textFieldHeight
    val isError = errorText != null
    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                modifier
                    .then(if (label != null) Modifier.semantics(mergeDescendants = true) {} else Modifier)
                    .then(if (label != null) Modifier.padding(top = fieldLabelSpace()) else Modifier)
                    .semantics { if (errorText != null) error(errorText) }
                    .defaultMinSize(minWidth = OutlinedTextFieldDefaults.MinWidth, minHeight = height),
            enabled = enabled,
            readOnly = readOnly,
            textStyle =
                LocalTextStyle.current.merge(
                    color = colors.textColor(enabled, isError, focused),
                    textDirection = TextDirection.Content,
                ),
            cursorBrush = SolidColor(if (isError) colors.errorCursorColor else colors.cursorColor),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value,
                    innerTextField = innerTextField,
                    enabled = enabled,
                    singleLine = singleLine,
                    visualTransformation = visualTransformation,
                    interactionSource = interactionSource,
                    isError = isError,
                    label = label,
                    placeholder = placeholder,
                    suffix = suffix,
                    trailingIcon = trailingIcon,
                    supportingText = supportingText,
                    colors = colors,
                    contentPadding = fieldPadding(height),
                )
            },
        )
    }
}

/** What a field keeps above its outline for its floating label (Material's `minimizedLabelHalfHeight`). */
@Composable
@ReadOnlyComposable
public fun fieldLabelSpace(): Dp {
    val line = MaterialTheme.typography.bodySmall.lineHeight
    return with(LocalDensity.current) { if (line.isSp) line.toDp() / 2 else PocketSpacing.xs }
}

/** The same as Material's at its size; smaller, it follows the height, with the text centered in it. */
@Composable
@ReadOnlyComposable
private fun fieldPadding(height: Dp): PaddingValues {
    if (!isCompactField()) return OutlinedTextFieldDefaults.contentPadding()
    val line = with(LocalDensity.current) { LocalTextStyle.current.lineHeight.toDp() }
    return PaddingValues(horizontal = height / 3, vertical = ((height - line) / 2).coerceAtLeast(0.dp))
}

@Composable
@ReadOnlyComposable
private fun isCompactField(): Boolean = PocketSizes.textFieldHeight < OutlinedTextFieldDefaults.MinHeight

private fun TextFieldColors.textColor(
    enabled: Boolean,
    isError: Boolean,
    focused: Boolean,
): Color =
    when {
        !enabled -> disabledTextColor
        isError -> errorTextColor
        focused -> focusedTextColor
        else -> unfocusedTextColor
    }
