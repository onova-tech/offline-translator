package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing

/** The inside of a [FieldToggleColumn]: its rows share one toggle width, or stack when a label wouldn't fit. */
public class FieldToggleScope internal constructor(
    internal val toggleWidth: Dp?,
    column: ColumnScope,
) : ColumnScope by column

/**
 * A form whose fields have a one-word [ToggleChoice] beside them, like the Financial calculator's "per year" or the
 * Date calculator's "Add" (owner's phone pass, 2026-11-03 and 2026-11-05; `docs/ui-rules.md`, T1–T3). Each row is a
 * [FieldWithToggle]. Every toggle is as wide as the longest of [toggleWords], so the fields line up and keep their
 * width whatever a toggle shows; give the words of every toggle in every mode of the screen, so switching modes
 * doesn't move the fields either. When the longest of [fieldLabels] wouldn't fit beside a toggle (a narrow window,
 * 200 % text), each control takes the whole width instead, so no label wraps.
 */
@Composable
public fun FieldToggleColumn(
    toggleWords: List<String>,
    fieldLabels: List<String>,
    modifier: Modifier = Modifier,
    content: @Composable FieldToggleScope.() -> Unit,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val typography = MaterialTheme.typography
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        fun widest(
            texts: List<String>,
            style: TextStyle,
        ): Dp = with(density) { (texts.maxOfOrNull { measurer.measure(it, style).size.width } ?: 0).toDp() }
        // A button pads its word by 24 dp on each side (plus a little room); a field's label by 16 dp.
        val toggleWidth = widest(toggleWords, typography.labelLarge) + PocketSpacing.xxl + PocketSpacing.xs
        val fieldWidth = maxWidth - PocketSpacing.xs - toggleWidth
        val fits =
            toggleWidth <= maxWidth / 2 && widest(fieldLabels, typography.bodyLarge) + PocketSpacing.xl <= fieldWidth
        Column(verticalArrangement = Arrangement.spacedBy(PocketSpacing.xs)) {
            FieldToggleScope(toggleWidth.takeIf { fits }, this).content()
        }
    }
}

/**
 * A field and its toggle (`docs/ui-rules.md`, T1): the [field] on the left, taking the rest of the width, and the
 * [toggle] on the right, exactly as tall as the field's outline. A field leaves half a small label's line above its
 * outline for its floating label, so the toggle starts that much lower (owner's phone pass). When the form stacks,
 * each takes the whole width, the toggle below. The [field] may also be a [ToggleCaption], for a toggle that chooses
 * something no field types.
 */
@Composable
public fun FieldToggleScope.FieldWithToggle(
    field: @Composable () -> Unit,
    toggle: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val gap = with(density) { PocketSpacing.xs.roundToPx() }
    val togglePx = toggleWidth?.let { with(density) { it.roundToPx() } }
    val labelSpace = with(density) { fieldLabelSpace().roundToPx() }
    Layout(contents = listOf(field, toggle), modifier = modifier.fillMaxWidth()) { (fields, toggles), constraints ->
        val width = constraints.maxWidth

        fun exactly(childWidth: Int) = constraints.copy(minWidth = childWidth, maxWidth = childWidth, minHeight = 0)
        if (togglePx != null) {
            val fieldPlaceable = fields.single().measure(exactly(width - gap - togglePx))
            val outline = (fieldPlaceable.height - labelSpace).coerceAtLeast(0)
            val button = toggles.single().measure(Constraints.fixed(togglePx, outline))
            layout(width, fieldPlaceable.height) {
                fieldPlaceable.place(0, 0)
                button.place(width - togglePx, labelSpace)
            }
        } else {
            val fieldPlaceable = fields.single().measure(exactly(width))
            val button = toggles.single().measure(exactly(width))
            layout(width, fieldPlaceable.height + gap + button.height) {
                fieldPlaceable.place(0, 0)
                button.place(0, fieldPlaceable.height + gap)
            }
        }
    }
}

/**
 * What a toggle with no field chooses, in the field's place: as tall as a field, level with its outline, starting where
 * a field's text starts, like the Financial calculator's "Amortization system:" (owner's phone pass, 2026-11-05). It
 * stays on one line, shrinking to fit, and goes on two only past the smallest size (owner, 2026-11-13). Screen readers
 * already hear it as the toggle's name.
 */
@Composable
public fun ToggleCaption(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clearAndSetSemantics { }
                .padding(top = fieldLabelSpace())
                .heightIn(min = PocketSizes.textFieldHeight)
                .padding(horizontal = PocketSpacing.md),
        contentAlignment = Alignment.CenterStart,
    ) {
        WholeWordText(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
