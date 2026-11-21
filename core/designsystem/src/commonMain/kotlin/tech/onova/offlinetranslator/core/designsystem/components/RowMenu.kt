package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import tech.onova.offlinetranslator.core.designsystem.icons.PocketIcons

/**
 * A row's actions behind one visible button, so no gesture is needed and icons don't crowd a list (spec 018). The
 * button is ⋮ unless [icon] says otherwise, such as the Notepad's format menu, "Aa" (spec 069).
 */
@Composable
public fun RowMenu(
    label: String,
    items: List<RowMenuItem>,
    modifier: Modifier = Modifier,
    icon: ImageVector = PocketIcons.MoreVertical,
    enabled: Boolean = true,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconAction(icon = icon, label = label, onClick = { open = true }, enabled = enabled)
        DropdownMenu(expanded = open && enabled, onDismissRequest = { open = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    leadingIcon = item.icon?.let { shown -> { Icon(shown, contentDescription = null) } },
                    enabled = item.enabled,
                    onClick = {
                        open = false
                        item.onClick()
                    },
                )
            }
        }
    }
}
