package tech.onova.offlinetranslator.screens

import android.os.Looper
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import org.robolectric.Shadows.shadowOf
import tech.onova.offlinetranslator.core.domain.permissions.AppPermission
import tech.onova.offlinetranslator.core.domain.permissions.PermissionStatus
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.PackStatus
import tech.onova.offlinetranslator.core.domain.ports.SpeechEvent
import tech.onova.offlinetranslator.support.FakeContentPacks
import tech.onova.offlinetranslator.support.KoinFakesRule

/**
 * A tool's mode with the most on its screen, which `LayoutFitTest`'s idle cases don't open (specs 057 to 061), apart so
 * that test stays a readable size. [prepare] sets the fakes, [after] opens the mode once the tool shows, given the
 * test's way to open a surface by its label, and [cleanup] puts the fakes back.
 */
internal class ModeSurface(
    val name: String,
    val toolId: String,
    val prepare: (KoinFakesRule) -> Unit,
    val cleanup: (KoinFakesRule) -> Unit,
    val after: (open: (String) -> Unit, compose: ComposeTestRule) -> Unit = { _, _ -> },
)

internal val modeSurfaces: List<ModeSurface> =
    listOf(
        // Spec 080: the language page, from the end of Type or speak's From list. Before the Conversation, since the
        // mode a case leaves is kept for the next one.
        ModeSurface(
            name = "translator-languages",
            toolId = "translator",
            prepare = {},
            cleanup = {},
            after = { open, compose ->
                // The language chips show once the settings are read, on another thread.
                compose.waitUntil(SETTINGS_MILLIS) {
                    shadowOf(Looper.getMainLooper()).idle()
                    compose.onAllNodes(hasClickAction() and fromChip).fetchSemanticsNodes().isNotEmpty()
                }
                open("From")
                open("Manage languages…")
            },
        ),
        // Spec 012: two facing halves, each with its language, its turns and its microphone.
        ModeSurface(
            name = "translator-conversation",
            toolId = "translator",
            prepare = {},
            cleanup = {},
            after = { open, _ -> open("Conversation") },
        ),
        // Spec 082 (owner, 2026-11-23): correcting what was heard, a popup in the middle (`PopupCard`, L5 and L7).
        ModeSurface(
            name = "translator-correction",
            toolId = "translator",
            prepare = { fakes -> Correction.prepare(fakes) },
            cleanup = { fakes -> Correction.cleanup(fakes) },
            after = { open, compose -> Correction.open(open, compose) },
        ),
    )

/** A turn said in the near half, then its own turn tapped: the correction opens. */
private object Correction {
    private val packs = listOf(PackId("translate_pt"), PackId("translate_es"))
    private var fakes: KoinFakesRule? = null
    private var before: List<PackStatus> = emptyList()

    /** A half's microphone, in English, Portuguese or Spanish ("Speak English", "Falar em inglês"). */
    private val microphone =
        SemanticsMatcher("a half's microphone") { node ->
            node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().any { description ->
                SPEAK.any { description.startsWith(it) }
            }
        }

    fun prepare(rule: KoinFakesRule) {
        fakes = rule
        before =
            rule.contentPacks.packs.value
                .filter { it.id in packs }
        packs.forEach { rule.contentPacks.put(FakeContentPacks.installed(it)) }
        rule.permissions.statuses[AppPermission.RECORD_AUDIO] = PermissionStatus.GRANTED
    }

    fun cleanup(rule: KoinFakesRule) {
        packs.forEach { id ->
            rule.contentPacks.put(before.firstOrNull { it.id == id } ?: FakeContentPacks.notInstalled(id))
        }
        rule.permissions.statuses.remove(AppPermission.RECORD_AUDIO)
        fakes = null
    }

    fun open(
        open: (String) -> Unit,
        compose: ComposeTestRule,
    ) {
        val rule = checkNotNull(fakes)
        // The case before leaves the Conversation open (a case's mode is kept), and tapping it again changes nothing.
        compose.waitUntil(SETTINGS_MILLIS) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodes(hasClickAction() and modeChoice).fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodes(microphone).fetchSemanticsNodes().isEmpty()) open("Conversation")
        compose.waitUntil(SETTINGS_MILLIS) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodes(microphone).fetchSemanticsNodes().size == 2
        }
        // The near half's is the lower one.
        val near =
            compose
                .onAllNodes(microphone)
                .fetchSemanticsNodes()
                .withIndex()
                .maxBy { it.value.boundsInRoot.top }
        val listened = rule.speechInput.listening
        compose.onAllNodes(microphone)[near.index].performClick()
        compose.waitUntil(SETTINGS_MILLIS) {
            shadowOf(Looper.getMainLooper()).idle()
            rule.speechInput.listening > listened
        }
        rule.speechInput.hear(SpeechEvent.Final(HEARD))
        // Translated, then read aloud; a turn is corrected once nothing else happens.
        compose.waitUntil(SETTINGS_MILLIS) {
            shadowOf(Looper.getMainLooper()).idle()
            rule.speechOutput.speaking
        }
        rule.speechOutput.done()
        val ownTurn = hasText(HEARD, substring = true) and hasClickAction() and isEnabled()
        compose.waitUntil(SETTINGS_MILLIS) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodes(ownTurn).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(ownTurn).performClick()
        compose.waitForIdle()
    }

    private const val HEARD = "Good morning"
    private val SPEAK = listOf("Speak ", "Falar em ", "Hablar en ")
}

private const val SETTINGS_MILLIS = 5_000L

/** The Translator's Conversation mode, in English, Portuguese and Spanish: the screen has shown. */
private val modeChoice: SemanticsMatcher =
    hasText("Conversation") or hasText("Conversa") or hasText("Conversación")

/** Type or speak's From chip, in English, Portuguese and Spanish. */
private val fromChip: SemanticsMatcher = hasContentDescription("From") or hasContentDescription("De")
