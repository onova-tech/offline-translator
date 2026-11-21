package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketShapes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing
import tech.onova.offlinetranslator.core.designsystem.tokens.isSmallWindow

/**
 * The owner's screen layout for tools (CLAUDE.md "Screen layout rules", spec 004 FR-044).
 *
 * - [top] holds only mode or category selectors and search fields.
 * - [bottom] holds every other control. It stays on the bottom edge and rises above the keyboard,
 *   so results and messages never move it.
 * - [content] fills the space between and is the only part that scrolls. With [scrollContent]
 *   false it gets the space as is, for a lazy list or a layout that measures itself.
 *   [contentArrangement] places it, for example at the bottom, just above the controls.
 * - [snackbarHost] shows at the bottom of the content, just above the controls.
 * - [bottomHeader] sits on top of [bottom] and never scrolls with it, such as the Financial calculator's fold line
 *   (spec 072): when a short window makes the controls scroll, it stays where it is. The controls' share of the
 *   window is the scrolling part's.
 */
@Composable
public fun ToolLayout(
    modifier: Modifier = Modifier,
    top: (@Composable ColumnScope.() -> Unit)? = null,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    scrollContent: Boolean = true,
    contentArrangement: Arrangement.Vertical = Arrangement.spacedBy(PocketSpacing.md),
    snackbarHost: @Composable () -> Unit = {},
    contentMinHeight: Dp = PocketSizes.contentMinHeight,
    bottomHeader: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().imePadding()) {
        var topHeight by remember { mutableStateOf(0.dp) }
        var measuredHeader by remember { mutableStateOf(0.dp) }
        val headerHeight = if (bottom != null && bottomHeader != null) measuredHeader else 0.dp
        val density = LocalDensity.current
        val bottomLimit = bottomLimit(maxHeight, topHeight + headerHeight, contentMinHeight)
        // Too short even for the selectors, the content's minimum and one control: the whole layout scrolls, and each
        // part keeps its size, rather than the content being squeezed to nothing (spec 015 FR-018).
        val tooShort = maxHeight < topHeight + headerHeight + contentMinHeight + PocketSizes.touchTarget
        Column(
            modifier =
                if (tooShort) {
                    Modifier.fillMaxWidth().verticalScroll(
                        rememberScrollState(),
                    )
                } else {
                    Modifier.fillMaxSize()
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (top != null) {
                Column(
                    modifier =
                        Modifier
                            .onSizeChanged { topHeight = with(density) { it.height.toDp() } }
                            .regionWidth()
                            .padding(start = PocketSpacing.md, top = PocketSpacing.md, end = PocketSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(PocketSpacing.sm),
                    content = top,
                )
            }
            ToolContent(
                modifier =
                    if (tooShort) {
                        Modifier.height(contentMinHeight).fillMaxWidth()
                    } else {
                        Modifier.weight(1f).fillMaxWidth()
                    },
                scrollContent = scrollContent,
                contentArrangement = contentArrangement,
                snackbarHost = snackbarHost,
                content = content,
            )
            if (bottom != null) {
                BottomRegion(
                    header = bottomHeader,
                    onHeaderHeight = { measuredHeader = with(density) { it.toDp() } },
                    limit = if (tooShort) null else bottomLimit,
                    content = bottom,
                )
            }
        }
    }
}

/**
 * The controls at the bottom edge, under their pinned [header] if there is one; they scroll past [limit], or with the
 * whole layout when it is null.
 */
@Composable
private fun BottomRegion(
    header: (@Composable () -> Unit)?,
    onHeaderHeight: (Int) -> Unit,
    limit: Dp?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrolling = limit?.let { Modifier.heightIn(max = it).verticalScroll(rememberScrollState()) } ?: Modifier
    Column(modifier = Modifier.regionWidth()) {
        if (header != null) {
            Box(
                modifier =
                    Modifier
                        .onSizeChanged { onHeaderHeight(it.height) }
                        .fillMaxWidth()
                        .padding(horizontal = PocketSpacing.md),
            ) { header() }
        }
        Column(
            modifier = Modifier.fillMaxWidth().then(scrolling).padding(PocketSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PocketSpacing.sm),
            content = content,
        )
    }
}

/**
 * The controls never take more than a share of the window: on a short one they scroll instead of being cut off, and the
 * content keeps the rest. It also leaves the content its minimum height below the top selectors (spec group 1, review
 * 2026-11-01).
 */
@Composable
@ReadOnlyComposable
private fun bottomLimit(
    maxHeight: Dp,
    topHeight: Dp,
    contentMinHeight: Dp,
): Dp =
    minOf(
        maxHeight * (if (isSmallWindow) BOTTOM_SHARE_SMALL_WINDOW else BOTTOM_SHARE),
        maxHeight - topHeight - contentMinHeight,
    ).coerceAtLeast(PocketSizes.touchTarget)

/** The middle of a tool: the only part that scrolls, with snackbars just above the controls. */
@Composable
private fun ToolContent(
    scrollContent: Boolean,
    contentArrangement: Arrangement.Vertical,
    snackbarHost: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(if (scrollContent) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(PocketSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = contentArrangement,
        ) {
            Column(
                modifier = Modifier.regionWidth().then(if (scrollContent) Modifier else Modifier.weight(1f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = contentArrangement,
                content = content,
            )
        }
        // Snackbars appear just above the controls, never over them.
        Box(modifier = Modifier.align(Alignment.BottomCenter)) { snackbarHost() }
    }
}

/** Largest share of the window the bottom controls take before they scroll. */
private const val BOTTOM_SHARE = 0.6f

/** In a small window the controls scroll sooner, so a dial or a list stays on the screen. */
private const val BOTTOM_SHARE_SMALL_WINDOW = 0.45f

// The cap comes first: widthIn after fillMaxWidth would be overruled by the incoming width.
private fun Modifier.regionWidth(): Modifier = widthIn(max = PocketSizes.contentMaxWidth).fillMaxWidth()

/**
 * A tool's main action, full width in the bottom area (FR-044); [icon] sits before the label. A
 * [compact] action is only a touch target high and never splits a word, for [BottomActionRow].
 */
@Composable
public fun BottomAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    compact: Boolean = false,
    style: BottomActionStyle = BottomActionStyle.Primary,
) {
    val size =
        modifier
            .fillMaxWidth()
            .then(if (compact) Modifier.fillMaxHeight() else Modifier)
            .heightIn(min = if (compact) PocketSizes.touchTarget else PocketSizes.buttonHeight)
            .uiRole(if (style == BottomActionStyle.Primary) UiRole.MainAction else UiRole.Action)
    // Side by side, the buttons give up some padding so a label keeps its size.
    val padding = if (compact) PaddingValues(horizontal = PocketSpacing.sm) else ButtonDefaults.ContentPadding
    val body: @Composable RowScope.() -> Unit = { ActionBody(label, icon, compact) }
    when (style) {
        BottomActionStyle.Primary -> {
            Button(onClick, size, enabled, PocketShapes.card, contentPadding = padding, content = body)
        }

        BottomActionStyle.Outlined -> {
            OutlinedButton(onClick, size, enabled, PocketShapes.card, contentPadding = padding, content = body)
        }

        BottomActionStyle.Text -> {
            TextButton(onClick, size, enabled, PocketShapes.card, contentPadding = padding, content = body)
        }
    }
}

/** How a [BottomAction] looks: at most one [Primary] on a screen (spec 018), and outlined beside it. */
public enum class BottomActionStyle { Primary, Outlined, Text }

@Composable
private fun ActionBody(
    label: String,
    icon: ImageVector?,
    compact: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.width(PocketSpacing.xs))
        }
        if (compact) {
            WholeWordText(text = label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        } else {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * Short actions side by side in the bottom area, each an equal share of the width, so they take one
 * row instead of one each (owner's phone pass, 2026-10-31: Bubble level and Protractor). Give each
 * [BottomAction] `compact = true` and `Modifier.weight(1f)`. The main (filled) action comes last, on the right,
 * where every tool has it (`docs/ui-rules.md`, A2).
 */
@Composable
public fun BottomActionRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    // Equal heights: a label that wraps makes its neighbour as tall, instead of one small and one big button.
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(PocketSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
