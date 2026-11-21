package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import tech.onova.offlinetranslator.core.designsystem.icons.PocketIcons
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing
import tech.onova.offlinetranslator.core.designsystem.tokens.isSmallWindow
import kotlin.math.roundToInt

/**
 * One option of a chip row or segmented control. A [MenuChoice] shows [detail] under the label in its list only, such
 * as a language pack's size (spec 012), so it never widens the button.
 */
public data class ChoiceOption<K>(
    val key: K,
    val label: String,
    val detail: String? = null,
)

/**
 * Filter chips of which any can be on at once (spec 034), wrapping onto as many rows as needed; like
 * [FilterChipRow], a chosen chip is filled and announced as selected, with no check mark.
 */
@Composable
public fun <K> FilterChipSet(
    options: List<ChoiceOption<K>>,
    selected: Set<K>,
    onToggle: (K) -> Unit,
    groupLabel: String,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.semantics { contentDescription = groupLabel },
        horizontalArrangement = Arrangement.spacedBy(PocketSpacing.xs),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option.key in selected,
                onClick = { onToggle(option.key) },
                label = { Text(option.label) },
            )
        }
    }
}

/**
 * Filter chips; the selected chip is announced as selected (spec US1), and is filled instead of outlined. They
 * scroll sideways in one row, or with [wrap] fill as many rows as needed. They have no check mark, because its
 * width would move the chips after it each time the choice changes (owner, 2026-11-05). With [onLongPress], a touch
 * and hold on a chip marks it a favourite or unmarks it, and a [marked] chip shows a star; the caller puts the marked
 * ones first (spec 039). The star only comes and goes when the person holds a chip, never on a plain tap.
 */
@Composable
public fun <K> FilterChipRow(
    options: List<ChoiceOption<K>>,
    selected: K,
    onSelect: (K) -> Unit,
    groupLabel: String,
    modifier: Modifier = Modifier,
    wrap: Boolean = false,
    marked: Set<K> = emptySet(),
    onLongPress: ((K) -> Unit)? = null,
    longPressLabel: (option: ChoiceOption<K>, marked: Boolean) -> String = { _, _ -> "" },
) {
    val chips: @Composable () -> Unit = {
        options.forEach { option ->
            val isSelected = option.key == selected
            val isMarked = option.key in marked
            val requester = remember { BringIntoViewRequester() }
            // A long row opens on the chosen chip instead of hiding it off the edge.
            LaunchedEffect(isSelected) { if (isSelected && !wrap) requester.bringIntoView() }
            val held =
                onLongPress?.let { press ->
                    Modifier.onLongPress(longPressLabel(option, isMarked)) { press(option.key) }
                } ?: Modifier
            FilterChip(
                modifier = Modifier.bringIntoViewRequester(requester).then(held),
                selected = isSelected,
                onClick = { onSelect(option.key) },
                label = { Text(option.label) },
                leadingIcon = if (isMarked) ({ FavouriteMark() }) else null,
            )
        }
    }
    val grouped = modifier.semantics { contentDescription = groupLabel }
    if (wrap) {
        FlowRow(
            modifier = grouped,
            horizontalArrangement = Arrangement.spacedBy(PocketSpacing.xs),
        ) { chips() }
    } else {
        Row(
            modifier = grouped.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(PocketSpacing.xs),
        ) { chips() }
    }
}

/**
 * The small star of a favourite: a chip kept first, a result pinned to the top (spec 039). Silent: what it marks says
 * so itself, through its long-press action's name ("Stop keeping Cooking first") or its state ("Pinned").
 */
@Composable
public fun FavouriteMark(modifier: Modifier = Modifier) {
    Icon(
        PocketIcons.Star,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.size(FilterChipDefaults.IconSize),
    )
}

/**
 * A single-choice segmented control: a tool's modes or categories at the top, a theme, a unit. Labels never break
 * inside a word and never shrink. The segments share the width equally while the widest label fits in an equal share;
 * otherwise each is as wide as its own label, with an equal part of the room left, and when even that doesn't fit the
 * row scrolls sideways, opening on the chosen one (owner, 2026-11-09: the top selectors all look like this, the
 * Converter's categories and Random's modes too). [groupLabel] names the group for screen readers. With [onLongPress],
 * a touch and hold marks a segment a favourite or unmarks it, and a [marked] one shows a star (spec 039). A [menu] adds
 * a last segment that opens its list, as the Tuner's "Cordas ▾" (spec 073): it reads the option chosen from it, keeps
 * one width whichever it reads, and is chosen when one of its options is.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun <K> SegmentedChoice(
    options: List<ChoiceOption<K>>,
    selected: K,
    onSelect: (K) -> Unit,
    modifier: Modifier = Modifier,
    groupLabel: String? = null,
    marked: Set<K> = emptySet(),
    onLongPress: ((K) -> Unit)? = null,
    longPressLabel: (option: ChoiceOption<K>, marked: Boolean) -> String = { _, _ -> "" },
    menu: ChoiceMenu<K>? = null,
) {
    // A smaller padding in a small window, though not so small that a label touches its outline: the row scrolls.
    val padding =
        if (isSmallWindow) PaddingValues(horizontal = PocketSpacing.sm) else SegmentedButtonDefaults.ContentPadding
    // Equal heights: the row is as tall as its tallest segment and every segment fills it.
    // Under Material's own 40 dp the height has to be told: its minimum counts in the intrinsic height.
    val target = PocketSizes.touchTarget
    val height = if (target < PocketSizes.iconButton) Modifier.height(target) else Modifier.height(IntrinsicSize.Min)
    // Never narrower than a touch target: eight short time signatures in equal shares overlapped their touch areas.
    val own = segmentsOwnWidths(options, menu, padding, starred = marked.isNotEmpty()).map { maxOf(it, target) }
    val count = own.size
    val named = if (groupLabel == null) Modifier else Modifier.semantics { contentDescription = groupLabel }
    BoxWithConstraints(modifier = modifier.fillMaxWidth().then(named)) {
        val widths = segmentWidths(own, maxWidth)
        val total = widths.fold(0.dp) { sum, width -> sum + width }
        val fits = total <= maxWidth
        val scroll = rememberScrollState()
        val index = chosenSegment(options, menu, selected)
        val viewport = constraints.maxWidth
        val density = LocalDensity.current
        var opened by remember { mutableStateOf(false) }
        // Opens on the chosen segment, and follows a new choice there.
        LaunchedEffect(index, fits) {
            if (fits || index < 0) return@LaunchedEffect
            val start = with(density) { widths.take(index).fold(0.dp) { sum, width -> sum + width }.toPx() }
            val width = with(density) { widths[index].toPx() }
            val offset = (start - (viewport - width) / 2).roundToInt().coerceIn(0, scroll.maxValue)
            if (opened) scroll.animateScrollTo(offset) else scroll.scrollTo(offset)
            opened = true
        }
        val row = if (fits) Modifier.fillMaxWidth() else Modifier.horizontalScroll(scroll).width(total)
        SingleChoiceSegmentedButtonRow(modifier = row.then(height)) {
            options.forEachIndexed { position, option ->
                val isMarked = option.key in marked
                val held =
                    onLongPress?.let { press ->
                        Modifier.onLongPress(longPressLabel(option, isMarked)) { press(option.key) }
                    } ?: Modifier
                // Outside Material's own equal weight, so this one counts.
                val share = Modifier.weight(widths[position].value)
                Segment(
                    label = option.label,
                    chosen = option.key == selected,
                    marked = isMarked,
                    shape = SegmentedButtonDefaults.itemShape(index = position, count = count),
                    padding = padding,
                    onClick = { onSelect(option.key) },
                    modifier = share.then(held),
                )
            }
            if (menu != null) {
                MenuSegment(
                    menu = menu,
                    selected = selected,
                    onSelect = onSelect,
                    shape = SegmentedButtonDefaults.itemShape(index = options.size, count = count),
                    padding = padding,
                    modifier = Modifier.weight(widths.last().value),
                )
            }
        }
    }
}

/** The segments' own widths: each option's, then a [menu]'s if there is one. */
@Composable
private fun <K> segmentsOwnWidths(
    options: List<ChoiceOption<K>>,
    menu: ChoiceMenu<K>?,
    padding: PaddingValues,
    starred: Boolean,
): List<Dp> {
    val widths = labelWidths(options.map { it.label }, padding, starred)
    return if (menu == null) widths else widths + menuWidth(menu, padding)
}

/** Which segment is chosen: a [menu]'s, last, when [selected] is one of its options; -1 when none is. */
private fun <K> chosenSegment(
    options: List<ChoiceOption<K>>,
    menu: ChoiceMenu<K>?,
    selected: K,
): Int =
    if (menu?.options?.any { it.key == selected } == true) {
        options.size
    } else {
        options.indexOfFirst { it.key == selected }
    }

/** A [ChoiceMenu]'s width: its widest word, in bold, with the ▾. */
@Composable
private fun <K> menuWidth(
    menu: ChoiceMenu<K>,
    padding: PaddingValues,
): Dp {
    val words = labelWidths(listOf(menu.label) + menu.options.map { it.label }, padding, starred = false)
    return words.max() + PocketSizes.menuArrow + PocketSpacing.xxs
}

/**
 * The segment of a [ChoiceMenu]: the option chosen from it, or its label, then ▾; a tap opens the list under it, the
 * chosen option bold, with no check mark (M5).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <K> SingleChoiceSegmentedButtonRowScope.MenuSegment(
    menu: ChoiceMenu<K>,
    selected: K,
    onSelect: (K) -> Unit,
    shape: Shape,
    padding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val chosen = menu.options.firstOrNull { it.key == selected }
    val shown = chosen?.label ?: menu.label
    SegmentedButton(
        modifier =
            modifier
                .fillMaxHeight()
                .uiRole(UiRole.Menu)
                .semantics {
                    contentDescription = menu.label
                    stateDescription = shown
                },
        selected = chosen != null,
        onClick = { open = true },
        shape = shape,
        contentPadding = padding,
        icon = {},
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PocketSpacing.xxs),
            ) {
                WholeWordText(
                    text = shown,
                    style =
                        if (chosen != null) {
                            LocalTextStyle.current.copy(fontWeight = FontWeight.Bold)
                        } else {
                            LocalTextStyle.current
                        },
                    textAlign = TextAlign.Center,
                    singleLine = true,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(
                    imageVector = PocketIcons.MenuArrow,
                    contentDescription = null,
                    modifier = Modifier.size(PocketSizes.menuArrow),
                )
                MenuList(menu, selected, open, onClose = { open = false }, onSelect = onSelect)
            }
        },
    )
}

/** A [ChoiceMenu]'s list while [open], the chosen option bold; choosing one closes it. */
@Composable
private fun <K> MenuList(
    menu: ChoiceMenu<K>,
    selected: K,
    open: Boolean,
    onClose: () -> Unit,
    onSelect: (K) -> Unit,
) {
    DropdownMenu(expanded = open, onDismissRequest = onClose) {
        menu.options.forEach { option ->
            val isChosen = option.key == selected
            DropdownMenuItem(
                text = {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isChosen) FontWeight.Bold else null,
                    )
                },
                onClick = {
                    onClose()
                    onSelect(option.key)
                },
                modifier = Modifier.semantics { this.selected = isChosen },
            )
        }
    }
}

/** One segment: the background and the bold label show the choice, a favourite shows its star. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleChoiceSegmentedButtonRowScope.Segment(
    label: String,
    chosen: Boolean,
    marked: Boolean,
    shape: Shape,
    padding: PaddingValues,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SegmentedButton(
        modifier = modifier.fillMaxHeight(),
        selected = chosen,
        onClick = onClick,
        shape = shape,
        contentPadding = padding,
        // A check mark only moved and squeezed the labels; a favourite shows its star in that place.
        icon = { if (marked) FavouriteMark() },
        // A long word shrinks in an equal share instead of breaking ("Strob-e" at 200% text).
        label = {
            WholeWordText(
                text = label,
                style =
                    if (chosen) {
                        LocalTextStyle.current.copy(fontWeight = FontWeight.Bold)
                    } else {
                        LocalTextStyle.current
                    },
                textAlign = TextAlign.Center,
                singleLine = true,
            )
        },
    )
}

/**
 * The segments' widths in [room]: equal while the widest of [own] fits in an equal share; otherwise each its own, with
 * an equal part of the room left; when even their sum doesn't fit, their sum, and the row scrolls.
 */
private fun segmentWidths(
    own: List<Dp>,
    room: Dp,
): List<Dp> {
    val widest = own.maxOrNull() ?: return emptyList()
    if (widest * own.size <= room) return own.map { room / own.size }
    val total = own.fold(0.dp) { sum, width -> sum + width }
    val spare = if (total < room) (room - total) / own.size else 0.dp
    return own.map { it + spare }
}

/**
 * Each segment's own width: its label in bold, the chosen look, with its padding and a favourite's star. Measured in
 * bold for every segment, so a choice never changes a width.
 */
@Composable
private fun labelWidths(
    labels: List<String>,
    padding: PaddingValues,
    starred: Boolean,
): List<Dp> {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
    val density = LocalDensity.current
    val widths =
        remember(labels, style, density) {
            labels.map { measurer.measure(it, style, maxLines = 1, softWrap = false).size.width }
        }
    val sides = padding.calculateLeftPadding(LayoutDirection.Ltr) + padding.calculateRightPadding(LayoutDirection.Ltr)
    val star = if (starred) FilterChipDefaults.IconSize + PocketSpacing.xs else 0.dp
    // No slack: the width is whole pixels rounded up, as WholeWordText measures it, and the outlines' overlap gives
    // each segment a little more. A margin made two labels at 200 % text scroll where they fit.
    return widths.map { with(density) { it.toDp() } + sides + star }
}
