package tech.onova.offlinetranslator.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Test
import org.robolectric.annotation.Config
import tech.onova.offlinetranslator.core.designsystem.components.UiRole
import tech.onova.offlinetranslator.core.designsystem.components.UiRoleKey
import tech.onova.offlinetranslator.core.designsystem.components.fieldLabelSpace
import tech.onova.offlinetranslator.core.designsystem.theme.PocketTheme
import tech.onova.offlinetranslator.core.domain.permissions.AppPermission
import tech.onova.offlinetranslator.core.domain.permissions.PermissionStatus
import tech.onova.offlinetranslator.support.AppUiTest
import tech.onova.offlinetranslator.tool.ToolScreen
import java.io.File
import kotlin.math.abs
import kotlin.math.min
import kotlin.test.assertTrue

/**
 * The placement rules of `docs/ui-rules.md` that a screen can be measured against, in every mode of every tool at the
 * owner's phone size (2026-11-08: "we need that decisions made continue to be respected so the app stays cohesive"):
 * - T1: a toggle beside a field is on the field's right, exactly as tall as the field's outline;
 * - T3: the toggles beside fields on one screen share one width;
 * - A1: at most one main (filled) action on a screen;
 * - A2: the main action is the last of its row, on the right;
 * - A3: no control below the actions: fields, toggles, sliders, selectors and "More options" go above them.
 * New tools join through [LayoutFitTest.TOOL_IDS]; the table of a run is in `androidApp/build/ui-rules/`.
 */
@Config(qualifiers = "w411dp-h850dp-port-xhdpi")
class UiRulesTest : AppUiTest() {
    private var current by mutableStateOf("")
    private var labelSpace = 0f

    private fun visit(
        node: SemanticsNode,
        found: MutableList<SemanticsNode>,
    ) {
        node.children.forEach { child ->
            found += child
            visit(child, found)
        }
    }

    private fun nodes(): List<SemanticsNode> {
        val found = mutableListOf<SemanticsNode>()
        compose.onAllNodes(isRoot()).fetchSemanticsNodes().forEach { visit(it, found) }
        return found
    }

    private fun name(node: SemanticsNode): String =
        listOfNotNull(
            node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" "),
            node.config.getOrNull(SemanticsProperties.Text)?.joinToString(" "),
        ).firstOrNull { it.isNotBlank() } ?: "unlabelled"

    private val SemanticsNode.role get() = config.getOrNull(UiRoleKey)

    private fun SemanticsNode.isField() = config.getOrNull(SemanticsProperties.EditableText) != null

    private fun SemanticsNode.isSlider() =
        config.getOrNull(SemanticsProperties.ProgressBarRangeInfo) != null &&
            config.getOrNull(SemanticsActions.SetProgress) != null

    private fun SemanticsNode.isSelector() =
        config.getOrNull(SemanticsProperties.Role) in SELECTOR_ROLES &&
            config.getOrNull(SemanticsActions.OnClick) != null

    private fun sameRow(
        a: Rect,
        b: Rect,
    ): Boolean {
        val overlap = min(a.bottom, b.bottom) - maxOf(a.top, b.top)
        return overlap > min(a.height, b.height) / 2
    }

    /** What breaks a rule on the screen as it is now. */
    private fun problems(where: String): List<String> {
        val all = nodes()
        val actions = all.filter { it.role == UiRole.Action || it.role == UiRole.MainAction }
        return (toggleProblems(all) + actionProblems(all, actions) + belowActions(all, actions))
            .map { "$where: $it" }
            .distinct()
    }

    /** T1 and T3: each toggle beside a field is on its right, as tall as its outline, all of them one width. */
    private fun toggleProblems(all: List<SemanticsNode>): List<String> {
        val fields = all.filter { it.isField() }
        val found = mutableListOf<String>()
        val besideWidths = mutableListOf<Float>()
        all.filter { it.role == UiRole.Toggle }.forEach { toggle ->
            val box = toggle.boundsInRoot
            val field =
                fields.firstOrNull { it.boundsInRoot.top < box.bottom && it.boundsInRoot.bottom > box.top }
                    ?: return@forEach
            val fieldBox = field.boundsInRoot
            val outlineHigh =
                abs(box.bottom - fieldBox.bottom) <= TOLERANCE_PX &&
                    abs(box.top - (fieldBox.top + labelSpace)) <= TOLERANCE_PX
            when {
                box.left < fieldBox.right - TOLERANCE_PX -> {
                    found += "T1 toggle '${name(toggle)}' is not on the right of field '${name(field)}'"
                }

                !outlineHigh -> {
                    found += "T1 toggle '${name(toggle)}' is not as tall as the outline of '${name(field)}'"
                }

                else -> {
                    besideWidths += box.width
                }
            }
        }
        if (besideWidths.isNotEmpty() && besideWidths.max() - besideWidths.min() > TOLERANCE_PX) {
            found += "T3 the toggles beside fields have different widths: $besideWidths"
        }
        return found
    }

    /** A1 and A2: one main action, the last of its row. */
    private fun actionProblems(
        all: List<SemanticsNode>,
        actions: List<SemanticsNode>,
    ): List<String> {
        val mains = all.filter { it.role == UiRole.MainAction }
        val found = mutableListOf<String>()
        if (mains.size > 1) found += "A1 ${mains.size} main actions: ${mains.map(::name)}"
        mains.forEach { main ->
            actions
                .filter { it.id != main.id && sameRow(it.boundsInRoot, main.boundsInRoot) }
                .filter { it.boundsInRoot.left > main.boundsInRoot.left + TOLERANCE_PX }
                .forEach { found += "A2 '${name(it)}' is on the right of the main action '${name(main)}'" }
        }
        return found
    }

    /** A3: no field, toggle, slider, selector or "More options" below the lowest row of actions. */
    private fun belowActions(
        all: List<SemanticsNode>,
        actions: List<SemanticsNode>,
    ): List<String> {
        val lowest = actions.maxByOrNull { it.boundsInRoot.bottom } ?: return emptyList()
        val rowTop = actions.filter { sameRow(it.boundsInRoot, lowest.boundsInRoot) }.minOf { it.boundsInRoot.top }
        return all
            .filter { it.isControl() && it.boundsInRoot.top > rowTop + TOLERANCE_PX }
            .map { "A3 '${name(it)}' is below the actions" }
    }

    private fun SemanticsNode.isControl() =
        role == UiRole.MoreOptions || role == UiRole.Toggle || isField() || isSlider() || isSelector()

    /** The options of the tool's mode selectors: chips and segments. */
    private fun modeOptions(): List<String> =
        compose
            .onAllNodes(isSelectable() and hasClickAction())
            .fetchSemanticsNodes()
            .filter { it.config.getOrNull(SemanticsProperties.Role) in SELECTOR_ROLES }
            .filter { it.config.getOrNull(UiRoleKey) != UiRole.Menu }
            .mapNotNull { node ->
                node.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ")
                    ?: node.children.firstNotNullOfOrNull {
                        it.config
                            .getOrNull(
                                SemanticsProperties.Text,
                            )?.joinToString(" ")
                    }
            }

    /** Taps each of [options] and checks the mode it opens; a selector shown only inside a mode, one level deep. */
    private fun walkOptions(
        options: List<String>,
        where: String,
        found: MutableList<String>,
        nested: Boolean = false,
    ) {
        options.forEach { option ->
            if (!choose(option)) return@forEach
            val here = "$where > $option"
            found += problems(here)
            val inside = if (nested) emptyList() else modeOptions().filterNot { it in options }.distinct()
            if (inside.isNotEmpty()) walkOptions(inside, here, found, nested = true)
        }
    }

    /** Taps the selector [option], if this mode has it. */
    private fun choose(option: String): Boolean {
        val selector = hasText(option) and isSelectable()
        if (compose.onAllNodes(selector).fetchSemanticsNodes().isEmpty()) return false
        runCatching { compose.onAllNodes(selector).onFirst().performScrollTo() }
        compose.onAllNodes(selector).onFirst().performClick()
        compose.waitForIdle()
        return true
    }

    @Test
    fun everyScreenKeepsThePlacementRules() {
        AppPermission.entries.forEach { fakes.permissions.statuses[it] = PermissionStatus.GRANTED }
        compose.setContent {
            PocketTheme(darkTheme = false, dynamicColor = false) {
                labelSpace = with(LocalDensity.current) { fieldLabelSpace().toPx() }
                key(current) { if (current.isNotEmpty()) ToolScreen(toolId = current, onOpenSettings = {}) }
            }
        }
        val table = mutableListOf<String>()
        val broken = mutableListOf<String>()
        LayoutFitTest.TOOL_IDS.forEach { tool ->
            compose.runOnIdle { current = tool }
            compose.waitForIdle()
            val found = problems(tool).toMutableList()
            walkOptions(modeOptions(), tool, found)
            val kept = found.distinct().filterNot { line -> ALLOWED.any { it in line } }
            table += "$tool: ${if (kept.isEmpty()) "keeps the rules" else "${kept.size} broken"}"
            broken += kept
        }
        File("build/ui-rules")
            .apply { mkdirs() }
            .resolve("phone-411x850.txt")
            .writeText((table + "" + broken).joinToString("\n"))
        assertTrue(broken.isEmpty(), "Screens that break docs/ui-rules.md:\n" + broken.joinToString("\n"))
    }

    private companion object {
        const val TOLERANCE_PX = 1.5f
        val SELECTOR_ROLES = setOf(Role.Checkbox, Role.RadioButton)

        /** Exceptions, each with its reason (and its row in `docs/ui-rules.md`, "Exceptions"). */
        val ALLOWED = emptyList<String>()
    }
}
