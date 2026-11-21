package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import tech.onova.offlinetranslator.core.designsystem.icons.PocketIcons
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing

/**
 * Screen frame with a back button and title, used by tools and Settings (spec FR-011). A tool opened from a shortcut
 * has nothing to go back to, so a null [onBack] draws no button (spec 016 FR-008). In a small window the title
 * shrinks to stay on two lines beside the actions, and takes a row of its own when even that can't hold its words
 * (spec 015).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun ToolFrame(
    title: String,
    backLabel: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    // Asked again whenever the window's width changes: a wider window may have room beside the actions after all.
    val windowWidth = LocalWindowInfo.current.containerDpSize.width
    var ownRow by remember(title, windowWidth) { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { PocketSnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = { if (!ownRow) BarTitle(title, onNoRoom = { ownRow = true }) },
                    navigationIcon = {
                        if (onBack != null) IconAction(icon = PocketIcons.Back, label = backLabel, onClick = onBack)
                    },
                    actions = actions,
                    expandedHeight = PocketSizes.topBarHeight,
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
                if (ownRow) {
                    WholeWordText(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PocketSpacing.md)
                                .padding(bottom = PocketSpacing.xs)
                                .semantics { heading() },
                    )
                }
            }
        },
        content = content,
    )
}

/** The title beside the back button and the actions. Text that fits draws as plain text, as it always has. */
@Composable
private fun BarTitle(
    title: String,
    onNoRoom: () -> Unit,
) {
    val style = LocalTextStyle.current
    val measurer = rememberTextMeasurer()
    BoxWithConstraints {
        val width = constraints.maxWidth
        val fit = remember(title, style, width) { titleFit(measurer, title, style, width) }
        when (fit) {
            TitleFit.AS_IS -> {
                Text(
                    text = title,
                    maxLines = TITLE_LINES,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
            }

            TitleFit.SHRUNK -> {
                WholeWordText(
                    text = title,
                    style = style,
                    maxLines = TITLE_LINES,
                    modifier = Modifier.semantics { heading() },
                )
            }

            TitleFit.NO_ROOM -> {
                SideEffect(onNoRoom)
            }
        }
    }
}

private enum class TitleFit { AS_IS, SHRUNK, NO_ROOM }

private fun titleFit(
    measurer: TextMeasurer,
    title: String,
    style: TextStyle,
    width: Int,
): TitleFit {
    fun fitsAt(style: TextStyle): Boolean {
        val layout =
            measurer.measure(
                title,
                style,
                overflow = TextOverflow.Ellipsis,
                maxLines = TITLE_LINES,
                constraints = Constraints(maxWidth = width),
            )
        val ellipsized = (0 until layout.lineCount).any(layout::isLineEllipsized)
        val broken =
            (0 until layout.lineCount - 1).any { line ->
                val end = layout.getLineEnd(line)
                end in 1 until title.length && !title[end - 1].isWhitespace() && !title[end].isWhitespace()
            }
        return !ellipsized && !broken
    }
    if (fitsAt(style)) return TitleFit.AS_IS
    // The same limit WholeWordText has: 60% of the size, and 9 sp unless the size is already under it.
    val floor = maxOf(TITLE_MIN_SCALE, minOf(1f, TITLE_MIN_SIZE / style.fontSize.value))
    val smallest = style.copy(fontSize = style.fontSize * floor, lineHeight = style.lineHeight * floor)
    return if (fitsAt(smallest)) TitleFit.SHRUNK else TitleFit.NO_ROOM
}

/** An icon-only button; [label] is what screen readers announce. */
@Composable
public fun IconAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tinted: Boolean = false,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, modifier = modifier.then(PocketSizes.iconButtonSize), enabled = enabled) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint =
                when {
                    !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ICON_ALPHA)
                    tinted -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

private const val TITLE_LINES = 2
private const val TITLE_MIN_SCALE = 0.6f
private const val TITLE_MIN_SIZE = 9f

/** Material's opacity for disabled content. */
private const val DISABLED_ICON_ALPHA = 0.38f
