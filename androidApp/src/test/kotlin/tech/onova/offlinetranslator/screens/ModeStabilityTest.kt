package tech.onova.offlinetranslator.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Test
import org.robolectric.annotation.Config
import tech.onova.offlinetranslator.core.designsystem.components.UiRole
import tech.onova.offlinetranslator.core.designsystem.components.UiRoleKey
import tech.onova.offlinetranslator.core.designsystem.theme.PocketTheme
import tech.onova.offlinetranslator.core.domain.permissions.AppPermission
import tech.onova.offlinetranslator.core.domain.permissions.PermissionStatus
import tech.onova.offlinetranslator.support.AppUiTest
import tech.onova.offlinetranslator.tool.ToolScreen
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.assertTrue

/**
 * What stays on screen between a tool's modes keeps its place and size (owner, 2026-11-05). This walks every mode
 * selector of every tool, and in each mode taps every one-word toggle through its words (`docs/ui-rules.md`, T2), and
 * compares each element that appears in more than one of those states. New tools join through
 * [LayoutFitTest.TOOL_IDS]; the table of a run is in `androidApp/build/mode-stability/`. The owner's phone size fails
 * the build; a small phone and a small window only keep their tables, because there a dial may give up room that the
 * screen doesn't have, and what a mode adds can't always stay above the action.
 */
abstract class ModeStabilityTest(
    private val windowName: String,
    private val strict: Boolean,
) : AppUiTest() {
    private data class Box(
        val name: String,
        val left: Float,
        val top: Float,
        val width: Float,
        val height: Float,
    ) {
        /** By label, the whole box; by position, a text's width follows its words, so only its height and line. */
        fun differsFrom(
            other: Box,
            byPosition: Boolean,
        ): Boolean =
            abs(top - other.top) > TOLERANCE_PX ||
                abs(height - other.height) > TOLERANCE_PX ||
                (!byPosition && (abs(left - other.left) > TOLERANCE_PX || abs(width - other.width) > TOLERANCE_PX))

        override fun toString() =
            "x=${left.roundToInt()} y=${top.roundToInt()} ${width.roundToInt()}x${height.roundToInt()}"
    }

    private var current by mutableStateOf("")

    private fun label(node: SemanticsNode): String? {
        val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ")
        val description = node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ")
        // A toggle by its name, not by the word it shows: its place and size must not change with the word.
        if (node.config.getOrNull(UiRoleKey) == UiRole.Toggle) return description
        return (text ?: description)?.takeIf { it.isNotBlank() }
    }

    /** Visits the tree with each node's path, leaving out a sideways row: its chips scroll to the one chosen. */
    private fun visit(
        node: SemanticsNode,
        path: String,
        found: MutableList<Pair<String, SemanticsNode>>,
    ) {
        if (node.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange) != null) return
        node.children.forEachIndexed { index, child ->
            val childPath = "$path/$index"
            found += childPath to child
            visit(child, childPath, found)
        }
    }

    private fun descendants(node: SemanticsNode): List<SemanticsNode> =
        node.children.flatMap { listOf(it) + descendants(it) }

    private fun boxOf(
        name: String,
        node: SemanticsNode,
    ) = Box(name, node.boundsInRoot.left, node.boundsInRoot.top, node.boundsInRoot.width, node.boundsInRoot.height)

    /**
     * Every element now on screen, under two names: by its label, a repeated one told apart by its order; and, for a
     * reading whose text changes with the mode, such as "0 km/h" and "0 mph", by its position in a tree of the same
     * shape.
     */
    private fun snapshot(): Map<String, Box> {
        val nodes = mutableListOf<Pair<String, SemanticsNode>>()
        compose.onAllNodes(isRoot()).fetchSemanticsNodes().forEachIndexed {
            index,
            root,
            ->
            visit(root, "$index", nodes)
        }
        // Two forms with different fields (the QR creator's Wi-Fi and Contact) aren't the same tree, even when the
        // paths match: their fields are told apart by their labels.
        val fields = nodes.filter { it.second.config.getOrNull(SemanticsProperties.EditableText) != null }
        val shape =
            (
                nodes.joinToString(
                    ",",
                ) { it.first } + fields.joinToString(",") { label(it.second).orEmpty() }
            ).hashCode()
        val seen = mutableMapOf<String, Int>()
        val boxes = mutableMapOf<String, Box>()
        nodes.forEach { (path, node) ->
            val label = label(node)
            boxes["$BY_POSITION$shape $path"] = boxOf(label ?: "unlabelled element at $path", node)
            if (label != null) {
                val key = choiceSet(node)?.let { "$label in $it" } ?: label
                val order = seen.merge(key, 1, Int::plus) ?: 1
                boxes[if (order == 1) key else "$key #$order"] = boxOf(label, node)
            }
        }
        return boxes
    }

    /**
     * The choices beside a selector's option. Segments share their row's width, so an option is only the same element
     * among the same choices: the Tuner's "Standard" is half of the Cavaquinho's two tunings, a third of the Bass's.
     */
    private fun choiceSet(node: SemanticsNode): String? {
        if (node.config.getOrNull(SemanticsProperties.Role) !in SELECTOR_ROLES) return null
        val group = node.parent ?: return null
        return group.children.joinToString("|") { label(it) ?: firstLabel(it).orEmpty() }
    }

    /** The options of the tool's mode selectors: chips and segments. A list row may be selectable too, so by role. */
    private fun modeOptions(): List<String> =
        compose
            .onAllNodes(isSelectable() and hasClickAction())
            .fetchSemanticsNodes()
            .filter { it.config.getOrNull(SemanticsProperties.Role) in SELECTOR_ROLES }
            .filter { it.config.getOrNull(UiRoleKey) != UiRole.Menu }
            .mapNotNull { node -> label(node) ?: firstLabel(node) }

    private fun firstLabel(node: SemanticsNode): String? = descendants(node).firstNotNullOfOrNull(::label)

    /**
     * Taps each of [options] and keeps a snapshot of the mode it opens, with its toggles. A selector that only shows in
     * one mode (the QR creator's kinds, inside Create) is walked inside that mode, one level deep.
     */
    private fun walkOptions(
        options: List<String>,
        prefix: String,
        modes: MutableMap<String, Map<String, Box>>,
    ) {
        options.forEach { option ->
            if (!choose(option)) return@forEach
            val name = if (prefix.isEmpty()) option else "$prefix > $option"
            modes[name] = snapshot()
            walkToggles(name, modes)
            val inside = if (prefix.isEmpty()) modeOptions().filterNot { it in options }.distinct() else emptyList()
            if (inside.isNotEmpty()) walkOptions(inside, option, modes)
        }
    }

    /** Taps the selector [option], if this mode has it: one that belongs to one mode isn't there in the others. */
    private fun choose(option: String): Boolean {
        val selector = hasText(option) and isSelectable()
        if (compose.onAllNodes(selector).fetchSemanticsNodes().isEmpty()) return false
        // In a small window it may be past the edge of its sideways row, where a tap would miss.
        runCatching { compose.onAllNodes(selector).onFirst().performScrollTo() }
        compose.onAllNodes(selector).onFirst().performClick()
        compose.waitForIdle()
        return true
    }

    /**
     * Taps every one-word toggle on the screen through all its words, back to the first, and keeps a snapshot of each
     * (`docs/ui-rules.md`, T2): a toggle changes its word and what it computes, never the place of anything else.
     */
    private fun walkToggles(
        prefix: String,
        modes: MutableMap<String, Map<String, Box>>,
    ) {
        val isToggle = SemanticsMatcher.expectValue(UiRoleKey, UiRole.Toggle)
        val names =
            compose
                .onAllNodes(isToggle)
                .fetchSemanticsNodes()
                .mapNotNull { it.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull() }
                .distinct()
        names.forEach { name ->
            val toggle = isToggle and hasContentDescription(name)

            fun word() =
                compose
                    .onAllNodes(toggle)
                    .fetchSemanticsNodes()
                    .firstOrNull()
                    ?.config
                    ?.getOrNull(SemanticsProperties.StateDescription)
            val first = word() ?: return@forEach
            repeat(MAX_TOGGLE_WORDS) {
                runCatching { compose.onAllNodes(toggle).onFirst().performScrollTo() }
                compose.onAllNodes(toggle).onFirst().performClick()
                compose.waitForIdle()
                val now = word()
                if (now == null || now == first) return@forEach
                modes["$prefix, $name: $now"] = snapshot()
            }
        }
    }

    private fun compare(
        tool: String,
        modes: Map<String, Map<String, Box>>,
    ): List<String> {
        val names = modes.values.flatMap { it.keys }.toSet()
        return names
            .mapNotNull { name ->
                val places = modes.mapNotNull { (mode, boxes) -> boxes[name]?.let { mode to it } }
                val (firstMode, first) = places.firstOrNull() ?: return@mapNotNull null
                val byPosition = name.startsWith(BY_POSITION)
                val moved =
                    places.firstOrNull { (_, box) -> box.differsFrom(first, byPosition) } ?: return@mapNotNull null
                "$tool: '${first.name}': $firstMode $first, ${moved.first} '${moved.second.name}' ${moved.second}"
            }.distinct()
    }

    @Test
    fun whatStaysBetweenModesKeepsItsPlaceAndSize() {
        AppPermission.entries.forEach { fakes.permissions.statuses[it] = PermissionStatus.GRANTED }
        compose.setContent {
            PocketTheme(darkTheme = false, dynamicColor = false) {
                key(current) { if (current.isNotEmpty()) ToolScreen(toolId = current, onOpenSettings = {}) }
            }
        }
        val table = mutableListOf<String>()
        val moved = mutableListOf<String>()
        LayoutFitTest.TOOL_IDS.forEach { tool ->
            compose.runOnIdle { current = tool }
            compose.waitForIdle()
            val options = modeOptions()
            val modes = linkedMapOf<String, Map<String, Box>>()
            TOGGLED_MODES[tool]?.forEach { label ->
                modes[label] = snapshot()
                compose.onNodeWithText(label).performClick()
                compose.waitForIdle()
            }
            if (options.isEmpty() && modes.isEmpty()) {
                modes["default"] = snapshot()
                walkToggles("default", modes)
                if (modes.size == 1) {
                    table += "$tool: no modes"
                    return@forEach
                }
            }
            walkOptions(options, "", modes)
            val found = compare(tool, modes).filterNot { line -> ALLOWED.any { it in line } }
            val names = modes.keys.joinToString("/")
            table += "$tool: $names: ${if (found.isEmpty()) "stable" else "${found.size} moved"}"
            moved += found
        }
        File("build/mode-stability")
            .apply { mkdirs() }
            .resolve("$windowName.txt")
            .writeText((table + "" + moved).joinToString("\n"))
        assertTrue(
            !strict || moved.isEmpty(),
            "Elements that move between a tool's modes:\n" + moved.joinToString("\n"),
        )
    }

    private companion object {
        const val TOLERANCE_PX = 1.5f

        /** More words than any toggle has: the walk stops when the first word comes back. */
        const val MAX_TOGGLE_WORDS = 6
        const val BY_POSITION = "at "
        val SELECTOR_ROLES = setOf(Role.Checkbox, Role.RadioButton)

        /** Modes chosen by a one-word toggle: the word shows the current mode, and a tap moves to the next. */
        val TOGGLED_MODES = mapOf("calculator" to listOf("Basic", "Scientific", "Programmer"))

        /** Elements that may move, each with its reason. */
        val ALLOWED =
            listOf(
                // It starts on a round mark of each unit: 5 cm and 2 in aren't the same length.
                "ruler: 'End marker'",
                // Scientific adds three rows of keys, and every key shares the height of the screen (owner's choice).
                "calculator: ",
                // A beat's bar is centered with the others, and the signature is what changes their count (spec 025).
                "metronome: 'Beat ",
                // The strings share the row's width, and the instrument is what changes their count (spec 026).
                "pitch-tuner: 'String ",
                // Each shape's lengths come from the top, as many as it has: the shape is what changes them (spec 068).
                "area-volume: 'Height'",
                "area-volume: 'unlabelled element",
                // A conversation gives each language to its own half, beside its microphone (C1, spec 012).
                "translator: 'English'",
                "translator: 'Portuguese'",
                "translator: 'Spanish'",
            )
    }
}

@Config(qualifiers = "w411dp-h850dp-port-xhdpi")
class ModeStabilityPhoneTest : ModeStabilityTest("phone-411x850", strict = true)

@Config(qualifiers = "w240dp-h360dp-port-xhdpi")
class ModeStabilityWindowTest : ModeStabilityTest("window-240x360", strict = false)

@Config(qualifiers = "w360dp-h640dp-port-xhdpi")
class ModeStabilitySmallPhoneTest : ModeStabilityTest("small-phone-360x640", strict = false)
