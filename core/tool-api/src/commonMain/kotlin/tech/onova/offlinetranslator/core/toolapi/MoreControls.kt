package tech.onova.offlinetranslator.core.toolapi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.onova.offlinetranslator.core.designsystem.components.UiRole
import tech.onova.offlinetranslator.core.designsystem.components.uiRole
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing
import tech.onova.offlinetranslator.core.toolapi.resources.Res
import tech.onova.offlinetranslator.core.toolapi.resources.host_fewer_options
import tech.onova.offlinetranslator.core.toolapi.resources.host_more_options

/**
 * The seldom-used controls (smoothing, resetting a calibration) behind one "More options" button, so the
 * dial tools keep their height for the dial (owner, 2026-11-01). Closed until asked, and it stays as the
 * user left it through a rotation. It goes in the bottom area above the actions, never below them, so opening it
 * leaves the actions at the bottom edge (`docs/ui-rules.md`, A3).
 *
 * Each of [options] is one control: a switch, a row of chips, a stepper. A button that opens a single control only adds
 * a tap, so one option shows from the start, without the button (owner, 2026-11-08; A4). Keep a control that is only
 * sometimes of use in the list, disabled, rather than leaving it out, so the button doesn't come and go with it.
 */
@Composable
public fun MoreControls(
    options: List<@Composable ColumnScope.() -> Unit>,
    modifier: Modifier = Modifier,
) {
    if (options.size == 1) {
        Column(modifier = modifier.fillMaxWidth()) { options.single()(this) }
        return
    }
    var shown by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PocketSpacing.xs)) {
        TextButton(onClick = { shown = !shown }, modifier = Modifier.fillMaxWidth().uiRole(UiRole.MoreOptions)) {
            Text(stringResource(if (shown) Res.string.host_fewer_options else Res.string.host_more_options))
        }
        if (shown) options.forEach { it(this) }
    }
}
