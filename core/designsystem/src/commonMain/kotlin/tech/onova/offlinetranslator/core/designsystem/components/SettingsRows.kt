package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import tech.onova.offlinetranslator.core.designsystem.icons.PocketIcons
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketShapes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing

/** A section title; exposed as a heading so screen-reader users can jump between sections. */
@Composable
public fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            modifier
                .semantics { heading() }
                .padding(horizontal = PocketSpacing.xxs, vertical = PocketSpacing.xs),
    )
}

/** A tappable settings entry with an optional current value and a chevron. */
@Composable
public fun SettingsRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    RowSurface(
        title = title,
        supporting = supporting,
        trailing = { Icon(PocketIcons.ChevronRight, contentDescription = null) },
        modifier = modifier.clip(PocketShapes.tile).clickable(role = Role.Button, onClick = onClick),
    )
}

/** A whole-row switch: the row is the touch target and is announced as a switch. */
@Composable
public fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    RowSurface(
        title = title,
        supporting = supporting,
        trailing = { Switch(checked = checked, onCheckedChange = null) },
        modifier =
            modifier
                .clip(PocketShapes.tile)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}

/**
 * A whole-row check box, the row the touch target and announced as a check box (spec 080: the Translator's language
 * page). Disabled, it keeps its state and says why elsewhere (a [ShortNote] above the list).
 */
@Composable
public fun CheckRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    enabled: Boolean = true,
) {
    RowSurface(
        title = title,
        supporting = supporting,
        trailing = { Checkbox(checked = checked, onCheckedChange = null, enabled = enabled) },
        modifier =
            modifier
                .clip(PocketShapes.tile)
                .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange),
    )
}

/**
 * A read-only row showing a label and its value, such as the app version. [below] goes on the same surface under the
 * value, lined up with the title: a choice that belongs to the row (Manage languages' Device | App, spec 082).
 */
@Composable
public fun InfoRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    below: (@Composable ColumnScope.() -> Unit)? = null,
) {
    RowSurface(title = title, supporting = value, modifier = modifier.clip(PocketShapes.tile), below = below)
}

/**
 * A Settings row whose actions are behind ⋮ on its right (`docs/ui-rules.md`, A6), such as a language pack's Download
 * and Delete (spec 012); [menuLabel] names the menu for screen readers. With no [items], the menu is off.
 */
@Composable
public fun MenuSettingsRow(
    title: String,
    menuLabel: String,
    items: List<RowMenuItem>,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    /** On the same surface under [supporting], lined up with the title (as [InfoRow]'s). */
    below: (@Composable ColumnScope.() -> Unit)? = null,
) {
    RowSurface(
        title = title,
        supporting = supporting,
        trailing = { RowMenu(label = menuLabel, items = items, enabled = items.isNotEmpty()) },
        modifier = modifier.clip(PocketShapes.tile),
        below = below,
    )
}

/**
 * The tonal row surface, rounded like the tool tiles so rows match the rest of the app; [below], when given, under the
 * row's text on the same surface, inset as the text is (owner, 2026-11-22: a part's choice "should be inside them").
 */
@Composable
private fun RowSurface(
    title: String,
    supporting: String?,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    below: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val container = MaterialTheme.colorScheme.surfaceContainerLow
    val item: @Composable (Modifier) -> Unit = { itemModifier ->
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = supporting?.let { { Text(it) } },
            trailingContent = trailing,
            colors = ListItemDefaults.colors(containerColor = container),
            modifier = itemModifier,
        )
    }
    if (below == null) {
        item(modifier)
        return
    }
    Column(modifier = modifier.background(container)) {
        item(Modifier)
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = LIST_ITEM_INSET, end = LIST_ITEM_INSET, bottom = PocketSpacing.md),
            content = below,
        )
    }
}

/** Material's list item keeps its text this far from its edges: what goes under the text lines up with it. */
private val LIST_ITEM_INSET = 16.dp
