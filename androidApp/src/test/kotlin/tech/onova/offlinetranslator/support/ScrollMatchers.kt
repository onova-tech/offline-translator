package tech.onova.offlinetranslator.support

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher

/**
 * The vertical list of a screen. A plain scroll match also finds the sideways rows (the Home category
 * filter), so a test that scrolls a list matches this instead.
 */
val verticalList: SemanticsMatcher = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
