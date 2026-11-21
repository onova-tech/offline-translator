package tech.onova.offlinetranslator.architecture

import tech.onova.offlinetranslator.architecture.ProjectSources.productionFiles
import tech.onova.offlinetranslator.architecture.ProjectSources.relativePath
import tech.onova.offlinetranslator.architecture.ProjectSources.violations
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Constitution Principle VII: visual values come only from `core/designsystem`, and no
 * user-visible text is hard-coded in UI code.
 */
class DesignSystemOnlyTest {
    private val uiFiles =
        productionFiles.filter { file ->
            val path = file.relativePath()
            path.startsWith("feature/") || path.startsWith("androidApp/src/main/")
        }

    private val rawVisualValues =
        Regex(
            listOf(
                """(?<![\w.])(?!0\.(dp|sp)\b)\d+(\.\d+)?\.(dp|sp)\b""",
                """\bColor\(\s*0x""",
                """\bFontWeight\(""",
                """\bRoundedCornerShape\(\s*\d""",
                """\btween\(\s*\d""",
                """\bfontSize\s*=""",
            ).joinToString("|"),
        )

    private val hardcodedText =
        Regex(
            listOf(
                """\bText\(\s*"""",
                """\btext\s*=\s*"""",
                """\bcontentDescription\s*=\s*"""",
                """\blabel\s*=\s*\{\s*Text\(\s*"""",
            ).joinToString("|"),
        )

    @Test
    fun uiCodeUsesOnlyDesignSystemTokens() {
        val offenders = uiFiles.flatMap { violations(it, rawVisualValues) }
        assertTrue(offenders.isEmpty(), "Raw visual values outside core/designsystem:\n" + offenders.joinToString("\n"))
    }

    /** Material's own field keeps 56 dp in a reduced window, so its text looks lost inside (owner, 2026-11-06). */
    @Test
    fun everyFieldIsTheDesignSystems() {
        val material = Regex("""(?<![\w.])OutlinedTextField\(""")
        val offenders = productionFiles.flatMap { violations(it, material) }
        assertTrue(offenders.isEmpty(), "Material's field instead of PocketTextField:\n" + offenders.joinToString("\n"))
    }

    /** Material's own messages wear its inverse colors: a white bar on a dark screen (owner, 2026-11-10). */
    @Test
    fun everyMessageHostIsTheDesignSystems() {
        val material = Regex("""(?<![\w.])SnackbarHost\(""")
        val offenders =
            productionFiles
                .filterNot { it.relativePath().endsWith("components/PocketSnackbarHost.kt") }
                .flatMap { violations(it, material) }
        assertTrue(
            offenders.isEmpty(),
            "Material's SnackbarHost, not PocketSnackbarHost:\n" + offenders.joinToString("\n"),
        )
    }

    /**
     * Text follows the window through its roles; overriding density would shrink touch targets too (spec 015 FR-010).
     */
    @Test
    fun nothingOverridesTheDensity() {
        val override = Regex("""LocalDensity\s+provides|(?<![\w.])Density\(""")
        val offenders = productionFiles.flatMap { violations(it, override) }
        assertTrue(offenders.isEmpty(), "LocalDensity overridden or a Density built:\n" + offenders.joinToString("\n"))
    }

    @Test
    fun uiCodeHasNoHardcodedUserVisibleText() {
        val designSystem = productionFiles.filter { it.relativePath().startsWith("core/designsystem/") }
        val offenders = (uiFiles + designSystem).flatMap { violations(it, hardcodedText) }
        assertTrue(
            offenders.isEmpty(),
            "Hard-coded user-visible text (use string resources):\n" + offenders.joinToString("\n"),
        )
    }
}
