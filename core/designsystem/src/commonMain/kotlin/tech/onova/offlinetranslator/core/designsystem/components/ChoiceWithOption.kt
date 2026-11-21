package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing

/**
 * A top [choice] with a small [option] button on its right, a [ToggleChoice] or a [MenuChoice] (spec 070, owner,
 * 2026-11-12: GPS info's DD beside Position | Places, Area & volume's ( mm ▾ ) beside Area | Volume). The option is
 * as wide as it needs and exactly as tall as the choice, top and bottom aligned; the choice takes the rest of the
 * width, scrolling sideways when its labels don't fit (S1).
 */
@Composable
public fun ChoiceWithOption(
    choice: @Composable () -> Unit,
    option: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gap = PocketSpacing.xs
    Layout(contents = listOf(choice, option), modifier = modifier.fillMaxWidth()) { (choices, options), constraints ->
        val space = gap.roundToPx()
        val button = options.single()
        // The option first takes its own width, never more than half the row, so the choice keeps the rest.
        val optionWidth = button.maxIntrinsicWidth(Constraints.Infinity).coerceAtMost(constraints.maxWidth / 2)
        val choiceWidth = (constraints.maxWidth - optionWidth - space).coerceAtLeast(0)
        val shownChoice = choices.single().measure(Constraints(minWidth = choiceWidth, maxWidth = choiceWidth))
        val height = shownChoice.height
        val shownOption = button.measure(Constraints.fixed(optionWidth, height))
        layout(constraints.maxWidth, height) {
            shownChoice.placeRelative(0, 0)
            shownOption.placeRelative(choiceWidth + space, 0)
        }
    }
}
