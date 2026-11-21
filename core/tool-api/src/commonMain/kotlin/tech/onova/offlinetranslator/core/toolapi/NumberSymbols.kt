package tech.onova.offlinetranslator.core.toolapi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.stringResource
import tech.onova.offlinetranslator.core.domain.format.NumberSymbols
import tech.onova.offlinetranslator.core.toolapi.resources.Res
import tech.onova.offlinetranslator.core.toolapi.resources.decimal_separator
import tech.onova.offlinetranslator.core.toolapi.resources.grouping_separator

/** The app language's number separators, for formatting and reading numbers (research.md R12). */
@Composable
public fun rememberNumberSymbols(): NumberSymbols {
    val decimal = stringResource(Res.string.decimal_separator).first()
    val grouping = stringResource(Res.string.grouping_separator).first()
    return remember(decimal, grouping) { NumberSymbols(decimal = decimal, grouping = grouping) }
}
