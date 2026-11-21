package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.ui.graphics.vector.ImageVector

/** One thing a [RowMenu] offers; an item that says what it does in its own words, as "UPPERCASE", has no [icon]. */
public class RowMenuItem(
    public val label: String,
    public val icon: ImageVector?,
    public val enabled: Boolean = true,
    public val onClick: () -> Unit,
)
