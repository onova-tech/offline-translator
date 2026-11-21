package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics

/**
 * What a shared control is, in the app's own rules (`docs/ui-rules.md`). Screen readers never hear it; the tests that
 * walk every tool read it to check the rules a screen must keep, such as the main action being the last of its row.
 */
public enum class UiRole {
    /** The screen's one filled [BottomAction]. */
    MainAction,

    /** Any other [BottomAction]: outlined or text, beside or above the main one. */
    Action,

    /** A [ToggleChoice]. */
    Toggle,

    /**
     * What opens a list rather than choosing at once: a [MenuChoice], or a [SegmentedChoice]'s [ChoiceMenu] segment
     * (spec 073). A test walking a screen's choices leaves it closed.
     */
    Menu,

    /** The button that opens a tool's "More options". */
    MoreOptions,

    /** A line cut with "…" on purpose, such as a history row's detail: the row's Copy or Share has all of it. */
    Summary,
}

/** The [UiRole] of a node, for tests. */
public val UiRoleKey: SemanticsPropertyKey<UiRole> = SemanticsPropertyKey("PocketUiRole")

private var SemanticsPropertyReceiver.uiRole by UiRoleKey

/** Marks this control with its [role]. */
public fun Modifier.uiRole(role: UiRole): Modifier = semantics { uiRole = role }
