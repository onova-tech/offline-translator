package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing
import tech.onova.offlinetranslator.core.designsystem.tokens.isSmallWindow

/**
 * A one-word toggle: it shows the chosen option and moves to the next one on a tap, like the Calculator's
 * Basic/Scientific (owner's phone pass, 2026-11-03: two segments beside a field cut their words). It is as tall as
 * a text field and as wide as its longest option, whichever is chosen, so a tap changes only the word and nothing
 * around it moves (`docs/ui-rules.md`, T2). Its word and outline wear the hint colour, the theme's primary, so it
 * reads as something to tap without looking like the main action (owner, 2026-11-12, T9). Given less room, a long
 * word shrinks rather than breaks. Beside a field, use [FieldWithToggle], which puts it on the field's right at the
 * height of the field's outline; beside a top choice, [ChoiceWithOption]. Screen readers hear "[name], [chosen
 * option]".
 */
@Composable
public fun <K> ToggleChoice(
    options: List<ChoiceOption<K>>,
    selected: K,
    onSelect: (K) -> Unit,
    name: String,
    modifier: Modifier = Modifier,
) {
    val index = options.indexOfFirst { it.key == selected }.coerceAtLeast(0)
    val chosen = options[index].label
    val padding = optionPadding()
    OutlinedButton(
        onClick = { onSelect(options[(index + 1) % options.size].key) },
        contentPadding = padding,
        colors = optionColors(),
        // The same thickness as the text field's border beside it, in the hint colour.
        border = optionBorder(),
        modifier =
            modifier
                .widthIn(min = toggleWidth(options.map { it.label }, padding))
                .heightIn(min = PocketSizes.buttonHeight)
                .uiRole(UiRole.Toggle)
                .semantics {
                    contentDescription = name
                    stateDescription = chosen
                },
    ) {
        WholeWordText(
            text = chosen,
            style = LocalTextStyle.current,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            singleLine = true,
        )
    }
}

/** A small option button's padding: a smaller one in a small window, so its word keeps its room. */
@Composable
internal fun optionPadding(): PaddingValues =
    if (isSmallWindow) PaddingValues(horizontal = PocketSpacing.xs) else ButtonDefaults.ContentPadding

/** A small option button's word in the hint colour (T9). */
@Composable
internal fun optionColors(): ButtonColors =
    ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)

/** A small option button's outline: a text field's thickness, in the hint colour (T9). */
@Composable
internal fun optionBorder(): BorderStroke =
    BorderStroke(OutlinedTextFieldDefaults.UnfocusedBorderThickness, MaterialTheme.colorScheme.primary)

/** The width a toggle needs for the longest of [words] on one line, with its [padding] on both sides. */
@Composable
internal fun toggleWidth(
    words: List<String>,
    padding: PaddingValues = ButtonDefaults.ContentPadding,
): Dp {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelLarge
    val direction = LocalLayoutDirection.current
    val widest = words.maxOfOrNull { measurer.measure(it, style).size.width } ?: 0
    val sides = padding.calculateStartPadding(direction) + padding.calculateEndPadding(direction)
    return with(LocalDensity.current) { widest.toDp() } + sides + PocketSpacing.xs
}
