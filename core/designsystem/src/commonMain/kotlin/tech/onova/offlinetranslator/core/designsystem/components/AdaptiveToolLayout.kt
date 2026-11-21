package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import tech.onova.offlinetranslator.core.designsystem.icons.PocketIcons
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing
import tech.onova.offlinetranslator.core.designsystem.tokens.isSmallWindow

/**
 * A tool's screen (CLAUDE.md screen layout rules), like [ToolLayout]: [top] under the title, [bottom] fixed at
 * the bottom edge and [content] scrolling between them. In landscape the screen is too short for the controls
 * under the content, so they go beside it: [top] and [content] on the left, [bottom] on the right, each side
 * scrolling on its own. With [scrollContent] false the content gets the height as is, for a [FitColumn]
 * that shares it among its parts. [bottomHeader] sits on top of [bottom] without scrolling with it, as in
 * [ToolLayout].
 */
@Suppress("ContentSlotReused") // One branch shows at a time; a rotation rebuilds the screen anyway.
@Composable
public fun AdaptiveToolLayout(
    top: (@Composable ColumnScope.() -> Unit)?,
    bottom: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    scrollContent: Boolean = true,
    snackbarHost: @Composable () -> Unit = {},
    endContent: (@Composable ColumnScope.() -> Unit)? = null,
    bottomHeader: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (isTwoPane(maxWidth, maxHeight)) {
            // The mode selectors go on the left with the content, so the controls get the full height beside it.
            ToolLayout(scrollContent = false, snackbarHost = snackbarHost) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(PocketSpacing.lg),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .then(if (scrollContent) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(PocketSpacing.md),
                    ) {
                        top?.invoke(this)
                        content()
                    }
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(PocketSpacing.sm),
                    ) {
                        if (endContent != null) {
                            // What would sit above the controls in portrait takes the height they leave.
                            Column(modifier = Modifier.weight(1f).fillMaxWidth(), content = endContent)
                        }
                        bottomHeader?.invoke()
                        Column(
                            modifier =
                                (if (endContent != null) Modifier else Modifier.weight(1f))
                                    .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(PocketSpacing.sm),
                            content = bottom,
                        )
                    }
                }
            }
        } else {
            ToolLayout(
                top = top,
                bottom = bottom,
                scrollContent = scrollContent,
                snackbarHost = snackbarHost,
                bottomHeader = bottomHeader,
            ) {
                content()
                endContent?.invoke(this)
            }
        }
    }
}

/**
 * Whether a window is wide enough to put a tool's controls beside its content: wider than tall and at least
 * [PocketSizes.twoPaneMinWidth]. A phone in landscape qualifies; a half-height split window does not.
 */
public fun isTwoPane(
    width: Dp,
    height: Dp,
): Boolean = width > height && width >= PocketSizes.twoPaneMinWidth

/** − and + icon buttons around a value, with the spoken labels [decreaseLabel] and [increaseLabel]. */
@Composable
public fun StepperRow(
    text: String,
    decreaseLabel: String,
    increaseLabel: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    // In a small window what follows the stepper goes under it, so the text keeps some room (spec 015).
    val below = isSmallWindow && trailing != null
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PocketSpacing.xs),
        ) {
            FilledTonalIconButton(onClick = onDecrease, modifier = PocketSizes.iconButtonSize) {
                Icon(PocketIcons.Minus, contentDescription = decreaseLabel)
            }
            WholeWordText(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            FilledTonalIconButton(onClick = onIncrease, modifier = PocketSizes.iconButtonSize) {
                Icon(PocketIcons.Plus, contentDescription = increaseLabel)
            }
            if (!below) trailing?.invoke()
        }
        if (below) trailing?.invoke()
    }
}

/**
 * A whole number typed by the user. The text is the user's while they type; Done or leaving the field applies
 * it through [onCommit] (which clamps it), and the field then shows [value] again.
 */
@Composable
public fun WholeNumberField(
    value: Int,
    label: String,
    onCommit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf(value.toString()) }
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(value, focused) {
        if (!focused) text = value.toString()
    }
    val commit = {
        text.toIntOrNull()?.let(onCommit)
        text = value.toString()
    }
    PocketTextField(
        value = text,
        onValueChange = { typed -> text = typed.filter { it.isDigit() }.take(MAX_DIGITS) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier =
            modifier.onFocusChanged { state ->
                if (focused && !state.isFocused) commit()
                focused = state.isFocused
            },
    )
}

private const val MAX_DIGITS = 5
