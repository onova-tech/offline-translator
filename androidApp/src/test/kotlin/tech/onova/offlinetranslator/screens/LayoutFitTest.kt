package tech.onova.offlinetranslator.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.espresso.IdlingPolicies
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.dsl.module
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import tech.onova.offlinetranslator.core.designsystem.theme.PocketTheme
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes
import tech.onova.offlinetranslator.core.domain.permissions.AppPermission
import tech.onova.offlinetranslator.core.domain.permissions.PermissionStatus
import tech.onova.offlinetranslator.core.domain.ports.DeviceCapabilities
import tech.onova.offlinetranslator.feature.settings.SettingsScreen
import tech.onova.offlinetranslator.OfflineTranslatorApp
import tech.onova.offlinetranslator.support.AppUiTest
import tech.onova.offlinetranslator.support.FakeCapabilities
import tech.onova.offlinetranslator.support.LayoutFit
import tech.onova.offlinetranslator.tool.ToolScreen
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.koin.core.context.GlobalContext.get as getKoin

/** What a window's run must prove. */
enum class FitRule {
    /** Nothing scrolls but [LayoutFitTest.SCROLLS_BY_DESIGN] and sideways selector rows: phones and tablets. */
    FITS,

    /**
     * May scroll, but everything can be reached: nothing cut, no squeezed scroller, no crash or hang (spec 015
     * FR-014).
     */
    REACHABLE,
}

enum class FitLanguage { ENGLISH, PORTUGUESE, SPANISH }

/**
 * Every tool fits its window (owner, 2026-10-31: tools work at different screen sizes and resolutions mostly
 * without scrolling), and in a reduced window all of it can still be reached (owner, 2026-11-05). Each subclass renders
 * every screen and surface at one window size and font scale and reads the result from the semantics tree (see
 * [LayoutFit]). The rule of each window is its [FitRule], and every run writes its table to
 * `androidApp/build/layout-fit/<name>.txt`. A window with [enforced] false only tracks its table.
 */
abstract class LayoutFitTest(
    private val windowName: String,
    private val fontScale: Float,
    private val rule: FitRule = FitRule.REACHABLE,
    private val enforced: Boolean = true,
    private val textLanguage: FitLanguage = FitLanguage.ENGLISH,
) : AppUiTest() {
    private class Case(
        val name: String,
        val prepare: () -> Unit = {},
        val after: () -> Unit = {},
        val cleanup: () -> Unit = {},
        val content: @Composable () -> Unit,
    )

    private var current by mutableStateOf("")

    /** A hang is a violation of that screen, found quickly; later classes in this JVM keep the usual limit. */
    @Before
    fun shortenTheIdleLimit() = IdlingPolicies.setMasterPolicyTimeout(HANG_SECONDS, TimeUnit.SECONDS)

    @After
    fun restoreTheIdleLimit() = IdlingPolicies.setMasterPolicyTimeout(USUAL_IDLE_SECONDS, TimeUnit.SECONDS)

    @Test
    fun everyScreenFitsItsWindow() {
        RuntimeEnvironment.setFontScale(fontScale)
        AppPermission.entries.forEach { fakes.permissions.statuses[it] = PermissionStatus.GRANTED }
        val cases = buildCases()
        val screens = cases.associateBy { it.name }
        var density = Density(1f)
        var touchTarget = 0.dp
        compose.setContent {
            density = LocalDensity.current
            PocketTheme(darkTheme = false, dynamicColor = false) {
                touchTarget = PocketSizes.touchTarget
                key(current) { screens[current]?.content?.invoke() }
            }
        }
        val table = mutableListOf<String>()
        val violations = mutableListOf<String>()
        cases.forEach { case ->
            val outcome =
                runCatching {
                    case.prepare()
                    compose.runOnIdle { current = case.name }
                    compose.waitForIdle()
                    case.after()
                    compose.waitForIdle()
                    readScreen(density, touchTarget)
                }
            val report = outcome.getOrNull()
            if (report == null) {
                val problem = outcome.exceptionOrNull()
                val line =
                    problem
                        ?.message
                        ?.lineSequence()
                        ?.firstOrNull()
                        ?.take(MAX_PROBLEM)
                val kind = if (problem is OpenerMissed) "not opened" else "crash"
                table += "${case.name}: $kind: ${problem?.javaClass?.simpleName} $line"
                violations += "[$windowName] ${case.name} $kind: ${problem?.stackTraceToString()?.take(MAX_STACK)}"
            } else {
                table += "${case.name}: ${if (report.fits) "fits" else report.lines().joinToString(" | ")}"
                violationsOf(case, report).forEach { violations += "[$windowName] ${case.name} $it" }
            }
            runCatching { case.cleanup() }
            File("build/layout-fit").apply { mkdirs() }.resolve("$windowName.txt").writeText(table.joinToString("\n"))
        }
        assertTrue(!enforced || violations.isEmpty(), "\n" + violations.joinToString("\n"))
    }

    private fun violationsOf(
        case: Case,
        report: LayoutFit.Report,
    ): List<String> {
        val sideways = case.name in SCROLLS_SIDEWAYS_BY_DESIGN
        return report.lines().filterNot { line ->
            when {
                // A row of selectors may scroll sideways (CLAUDE.md: a long list of categories stays one row).
                line.startsWith("scrolls: sideways, not a selector row") -> sideways

                line.startsWith("scrolls: sideways") -> true

                line.startsWith("target: touch areas") && case.name in TOUCH_AREAS_ALLOWED -> true

                !line.startsWith("scrolls") -> false

                rule == FitRule.REACHABLE -> true

                else -> case.name in SCROLLS_BY_DESIGN
            }
        }
    }

    /** The screen at rest and then, for each lazy list, every row of it: rows off-screen aren't composed at rest. */
    private fun readScreen(
        density: Density,
        touchTarget: Dp,
    ): LayoutFit.Report {
        fun read() =
            LayoutFit.inspect(
                compose.onAllNodes(isRoot()).fetchSemanticsNodes(),
                compose.onAllNodes(isRoot(), useUnmergedTree = true).fetchSemanticsNodes(),
                density,
                touchTarget,
            )
        var report = read()
        val lists = compose.onAllNodes(hasScrollToIndexAction())
        lists.fetchSemanticsNodes().indices.forEach { index ->
            var step = 0
            while (step++ < MAX_LIST_STEPS && scrollDown(lists[index].fetchSemanticsNode())) {
                report = report.merge(read())
            }
        }
        return report
    }

    /** Moves a list by most of its viewport; false when it has reached its end. */
    private fun scrollDown(list: SemanticsNode): Boolean {
        val range = list.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
        val scrollBy = list.config[SemanticsActions.ScrollBy].action
        val canMove = range != null && range.value() < range.maxValue() - 1f && scrollBy != null
        if (canMove) {
            compose.runOnUiThread { scrollBy?.invoke(0f, list.size.height * LIST_STEP) }
            compose.waitForIdle()
        }
        return canMove
    }

    /** Opens a surface: scrolls its target into view, clicks it, and proves that the screen changed. */
    private fun open(label: String) {
        val shown = label.inLanguage()

        fun screen() =
            compose
                .onAllNodes(isRoot(), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .flatMap { it.textsBelow() }
        val target = hasText(shown).or(hasContentDescription(shown))
        val tappable = target.and(hasClickAction())
        // In a lazy list, a row past the viewport isn't composed yet.
        val lists = compose.onAllNodes(hasScrollToNodeAction())
        if (compose.onAllNodes(tappable).fetchSemanticsNodes().isEmpty() && lists.fetchSemanticsNodes().isNotEmpty()) {
            runCatching { lists.onFirst().performScrollToNode(tappable) }
            compose.waitForIdle()
        }
        if (compose.onAllNodes(tappable).fetchSemanticsNodes().isEmpty()) {
            throw OpenerMissed("nothing to tap called '$shown'")
        }
        val node = compose.onAllNodes(tappable).onFirst()
        runCatching { node.performScrollTo() }
        val before = screen()
        node.performClick()
        compose.waitForIdle()
        if (screen() == before) throw OpenerMissed("tapping '$shown' changed nothing")
    }

    private class OpenerMissed(
        message: String,
    ) : IllegalStateException(message)

    private fun SemanticsNode.textsBelow(): List<String> =
        config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
            config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() +
            children.flatMap { it.textsBelow() }

    private fun String.inLanguage(): String =
        when (textLanguage) {
            FitLanguage.ENGLISH -> this
            FitLanguage.PORTUGUESE -> checkNotNull(LABELS[this]) { this }.first
            FitLanguage.SPANISH -> checkNotNull(LABELS[this]) { this }.second
        }

    /** The Translator at rest, its surfaces and modes, then Settings and its dialog (spec 081). */
    private fun buildCases(): List<Case> = idleCases() + hostCases() + surfaces() + modeSurfaces()

    /** Dialogs and sheets, which a screen at rest doesn't show (spec 015 FR-014). Their rows start with "surface ". */
    private fun surfaces(): List<Case> =
        listOf(
            surface("help-sheet", "translator", "How to use"),
            Case(
                "surface settings-language-dialog",
                after = { open("App language") },
            ) { SettingsScreen(onBack = {}) },
        )

    private fun capabilities(fake: DeviceCapabilities) {
        getKoin().loadModules(listOf(module { single<DeviceCapabilities> { fake } }), allowOverride = true)
    }

    private fun surface(
        name: String,
        tool: String,
        opener: String,
    ): Case = Case("surface $name", after = { open(opener) }) { ToolScreen(toolId = tool, onOpenSettings = {}) }

    /** A tool's mode with the most on its screen, which the idle cases don't open (`ModeSurface.kt`). */
    private fun modeSurfaces(): List<Case> =
        modeSurfaces.map { surface ->
            Case(
                "surface ${surface.name}",
                prepare = { surface.prepare(fakes) },
                after = { surface.after(::open, compose) },
                cleanup = { surface.cleanup(fakes) },
            ) { ToolScreen(toolId = surface.toolId, onOpenSettings = {}) }
        }

    private fun idleCases(): List<Case> = TOOL_IDS.map { id -> Case(id) { ToolScreen(toolId = id, onOpenSettings = {}) } }

    private fun hostCases(): List<Case> =
        listOf(
            Case("settings") { SettingsScreen(onBack = {}) },
            Case("app") { OfflineTranslatorApp() },
        )

    companion object {
        private const val HANG_SECONDS = 15L
        private const val USUAL_IDLE_SECONDS = 60L
        private const val MAX_LIST_STEPS = 40
        private const val LIST_STEP = 0.9f
        private const val MAX_PROBLEM = 120
        private const val MAX_STACK = 800

        /** What each surface's opener is called, in Portuguese and Spanish. */
        private val LABELS =
            mapOf(
                "Conversation" to ("Conversa" to "Conversación"),
                "How to use" to ("Como usar" to "Cómo usar"),
                "App language" to ("Idioma do app" to "Idioma de la app"),
                "From" to ("De" to "De"),
                "Manage languages…" to ("Gerenciar idiomas…" to "Gestionar idiomas…"),
            )

        /** No screen may have touch areas that meet. */
        private val TOUCH_AREAS_ALLOWED = emptySet<String>()

        /** No screen scrolls sideways but its selector rows. */
        private val SCROLLS_SIDEWAYS_BY_DESIGN = emptySet<String>()

        val TOOL_IDS = listOf("translator")

        /**
         * The screens that may scroll (owner's rule, CLAUDE.md "Screen layout rules"): lists and long text, and
         * the Ruler's calibration, whose card outline is as long as a real card whatever the screen, and the
         * export page and the import review, each a list of every part of the data file (spec 010).
         */
        val SCROLLS_BY_DESIGN: Set<String> =
            setOf(
                "settings",
                // Spec 080: the language page is a list of 54 languages.
                "surface translator-languages",
                // A dialog over Settings, which is a list.
                "surface settings-language-dialog",
            )
    }
}

@Config(qualifiers = "w411dp-h850dp-port-xhdpi")
class LayoutFitPhoneTest : LayoutFitTest("phone-411x850-f1.0", 1f, FitRule.FITS)

@Config(qualifiers = "w800dp-h1200dp-port-xhdpi")
class LayoutFitTabletTest : LayoutFitTest("tablet-800x1200-f1.0", 1f, FitRule.FITS)

@Config(qualifiers = "w1280dp-h740dp-land-xhdpi")
class LayoutFitTabletLandscapeTest : LayoutFitTest("tablet-landscape-1280x740-f1.0", 1f, FitRule.FITS)

@Config(qualifiers = "w411dp-h850dp-port-xhdpi")
class LayoutFitPhoneLargerTextTest : LayoutFitTest("phone-411x850-f1.3", 1.3f)

@Config(qualifiers = "w411dp-h850dp-port-xhdpi")
class LayoutFitPhoneLargestTextTest : LayoutFitTest("phone-411x850-f2.0", 2f)

@Config(qualifiers = "w360dp-h640dp-port-xhdpi")
class LayoutFitSupportedMinimumPortraitTest : LayoutFitTest("supported-minimum-360x640-f1.0", 1f)

@Config(qualifiers = "w640dp-h360dp-land-xhdpi")
class LayoutFitSupportedMinimumLandscapeTest : LayoutFitTest("supported-minimum-640x360-f1.0", 1f)

@Config(qualifiers = "w360dp-h592dp-port-xhdpi")
class LayoutFitCompactPhoneTest : LayoutFitTest("compact-phone-360x592-f1.0", 1f)

@Config(qualifiers = "w411dp-h400dp-port-xhdpi")
class LayoutFitSplitScreenTest : LayoutFitTest("split-screen-411x400-f1.0", 1f)

@Config(qualifiers = "w850dp-h380dp-land-xhdpi")
class LayoutFitLandscapeTest : LayoutFitTest("landscape-850x380-f1.0", 1f)

@Config(qualifiers = "w320dp-h520dp-port-xhdpi")
class LayoutFitSmallPhoneTest : LayoutFitTest("small-phone-320x520-f1.0", 1f)

@Config(qualifiers = "w320dp-h520dp-port-xhdpi")
class LayoutFitSmallPhoneLargerTextTest : LayoutFitTest("small-phone-320x520-f1.3", 1.3f)

@Config(qualifiers = "w320dp-h520dp-port-xhdpi")
class LayoutFitSmallPhoneLargestTextTest : LayoutFitTest("small-phone-320x520-f2.0", 2f)

@Config(qualifiers = "w280dp-h420dp-port-xhdpi")
class LayoutFitWindow280Test : LayoutFitTest("window-280x420-f1.0", 1f)

/** The owner's phone in One UI's pop-up window, as measured on 2026-11-06. */
@Config(qualifiers = "w257dp-h498dp-port-xhdpi")
class LayoutFitPopUpWindowTest : LayoutFitTest("pop-up-257x498-f1.0", 1f)

@Config(qualifiers = "w240dp-h360dp-port-xhdpi")
class LayoutFitWindow240Test : LayoutFitTest("window-240x360-f1.0", 1f)

@Config(qualifiers = "pt-rBR-w240dp-h360dp-port-xhdpi")
class LayoutFitWindow240PortugueseTest :
    LayoutFitTest("window-240x360-pt-f1.0", 1f, textLanguage = FitLanguage.PORTUGUESE)

@Config(qualifiers = "es-w240dp-h360dp-port-xhdpi")
class LayoutFitWindow240SpanishTest : LayoutFitTest("window-240x360-es-f1.0", 1f, textLanguage = FitLanguage.SPANISH)

@Config(qualifiers = "w240dp-h360dp-port-xhdpi")
class LayoutFitWindow240LargestTextTest : LayoutFitTest("window-240x360-f2.0", 2f, enforced = false)

@Config(qualifiers = "w220dp-h300dp-port-xhdpi")
class LayoutFitWindow220Test : LayoutFitTest("window-220x300-f1.0", 1f)

@Config(qualifiers = "w220dp-h220dp-port-xhdpi")
class LayoutFitWindowFloorTest : LayoutFitTest("window-220x220-f1.0", 1f)

@Config(qualifiers = "w420dp-h240dp-land-xhdpi")
class LayoutFitWindowShortTest : LayoutFitTest("window-420x240-f1.0", 1f)

/** The NDEF type of a link record. */

/** When the histories' rows were made (spec 044). */
