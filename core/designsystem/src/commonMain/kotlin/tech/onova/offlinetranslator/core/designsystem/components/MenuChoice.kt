package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import tech.onova.offlinetranslator.core.designsystem.icons.PocketIcons
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing

/**
 * A small option button that lists its options, such as Area & volume's unit, **( mm ▾ )** (owner, 2026-11-12, spec
 * 070): it looks like a [ToggleChoice], in the hint colour, with a small downward triangle after the chosen option, and
 * a tap opens the list under it, the chosen option bold. Like a toggle it keeps one width whichever option is shown
 * (`docs/ui-rules.md`, T2). Beside a top choice, use [ChoiceWithOption], which makes it exactly as tall. Screen readers
 * hear "[name], [chosen option], drop-down list". With [fillWidth] it takes the width it's given, as the Translator's
 * two languages do beside their swap (spec 012); an option's [ChoiceOption.detail] shows under it in the list.
 * In a half turned to the person across ([LocalHalfTurned]), its list is turned too, and opens under the chip as they
 * see it.
 */
@Composable
public fun <K> MenuChoice(
    options: List<ChoiceOption<K>>,
    selected: K,
    onSelect: (K) -> Unit,
    name: String,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = false,
    last: MenuEntry? = null,
) {
    var open by remember { mutableStateOf(false) }
    val chosen = options.firstOrNull { it.key == selected }?.label.orEmpty()
    val padding = optionPadding()
    val hint = MaterialTheme.colorScheme.primary
    val turned = LocalHalfTurned.current
    // The chip as drawn, turn included: a turned list opens from it.
    var chip by remember { mutableStateOf(IntRect.Zero) }
    val placed =
        if (turned) Modifier.onGloballyPositioned { chip = it.boundsInWindow().roundToIntRect() } else Modifier
    Box(modifier = modifier.then(placed)) {
        OutlinedButton(
            onClick = { open = true },
            contentPadding = padding,
            colors = optionColors(),
            border = optionBorder(),
            modifier =
                Modifier
                    .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                    .widthIn(min = toggleWidth(options.map { it.label }, padding) + PocketSizes.menuArrow)
                    .heightIn(min = PocketSizes.buttonHeight)
                    .uiRole(UiRole.Menu)
                    .semantics {
                        contentDescription = name
                        stateDescription = chosen
                        role = Role.DropdownList
                    },
        ) {
            WholeWordText(
                text = chosen,
                style = LocalTextStyle.current,
                color = hint,
                textAlign = TextAlign.Center,
                singleLine = true,
            )
            Spacer(modifier = Modifier.width(PocketSpacing.xxs))
            Icon(
                imageVector = PocketIcons.MenuArrow,
                contentDescription = null,
                tint = hint,
                modifier = Modifier.size(PocketSizes.menuArrow),
            )
        }
        val close = { open = false }
        if (turned) {
            if (open) TurnedOptionList(chip, close) { OptionItems(options, selected, close, onSelect, last) }
        } else {
            DropdownMenu(expanded = open, onDismissRequest = close) {
                OptionItems(options, selected, close, onSelect, last)
            }
        }
    }
}

/**
 * A turned half's list ([LocalHalfTurned]): Android places a list from its chip's unturned corner and draws it upright,
 * so this one is placed from the [chip] as drawn and turned with it. For the person across, it opens towards the middle
 * of the phone, lined up with the chip's start.
 */
@Composable
private fun TurnedOptionList(
    chip: IntRect,
    onClose: () -> Unit,
    items: @Composable () -> Unit,
) {
    Popup(
        popupPositionProvider = remember(chip) { UnderTurnedChip(chip) },
        onDismissRequest = onClose,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            shape = MenuDefaults.shape,
            color = MenuDefaults.containerColor,
            tonalElevation = MenuDefaults.TonalElevation,
            shadowElevation = MenuDefaults.ShadowElevation,
            modifier = Modifier.graphicsLayer { rotationZ = HALF_TURN },
        ) {
            Column(modifier = Modifier.width(IntrinsicSize.Max).padding(vertical = PocketSpacing.xs)) { items() }
        }
    }
}

/** Below the chip on the screen, which is above it for the person across; its start is the screen's other side. */
private class UnderTurnedChip(
    private val chip: IntRect,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val widest = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val x = if (layoutDirection == LayoutDirection.Ltr) chip.right - popupContentSize.width else chip.left
        val y = chip.bottom.coerceAtMost((windowSize.height - popupContentSize.height).coerceAtLeast(0))
        return IntOffset(x.coerceIn(0, widest), y)
    }
}

/** A [MenuChoice]'s options: the chosen one bold, each option's detail under it. */
@Composable
private fun <K> OptionItems(
    options: List<ChoiceOption<K>>,
    selected: K,
    onClose: () -> Unit,
    onSelect: (K) -> Unit,
    last: MenuEntry?,
) {
    options.forEach { option ->
        val isChosen = option.key == selected
        DropdownMenuItem(
            text = {
                Column {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isChosen) FontWeight.Bold else null,
                    )
                    option.detail?.let { detail ->
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            onClick = {
                onClose()
                onSelect(option.key)
            },
            modifier = Modifier.semantics { this.selected = isChosen },
        )
    }
    last?.let { entry ->
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(text = entry.label, style = MaterialTheme.typography.bodyLarge) },
            onClick = {
                onClose()
                entry.onClick()
            },
        )
    }
}

/**
 * An action at the end of a [MenuChoice]'s list, after a divider, such as the Translator's "Other languages…"
 * (spec 080), which opens a page rather than choosing an option.
 */
public class MenuEntry(
    public val label: String,
    public val onClick: () -> Unit,
)
