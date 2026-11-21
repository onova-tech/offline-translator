package tech.onova.offlinetranslator.feature.toolstranslate

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringArrayResource
import org.jetbrains.compose.resources.StringResource
import tech.onova.offlinetranslator.core.designsystem.icons.pocketIcon
import tech.onova.offlinetranslator.core.domain.catalog.ToolCategory
import tech.onova.offlinetranslator.core.domain.catalog.ToolDescriptor
import tech.onova.offlinetranslator.core.domain.catalog.ToolId
import tech.onova.offlinetranslator.core.toolapi.ToolEntry
import tech.onova.offlinetranslator.core.toolapi.ToolHelp
import tech.onova.offlinetranslator.feature.toolstranslate.presentation.TranslatorContent
import tech.onova.offlinetranslator.feature.toolstranslate.resources.Res
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_help
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_keywords
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_name

/**
 * The Translator (spec 012): Type or speak, and Conversation. Its id is permanent once shipped. No hardware is
 * required: without a microphone or an on-device recognizer, typing still works (FR-026).
 */
internal class TranslatorEntry : ToolEntry {
    override val descriptor =
        ToolDescriptor(
            id = ToolId(ID),
            category = ToolCategory.UTILITIES,
            requiredHardware = emptySet(),
        )
    override val name: StringResource = Res.string.translator_name
    override val keywords: StringArrayResource = Res.array.translator_keywords

    // Lucide's "languages" (ISC, listed on the Open-source licenses page).
    override val icon: ImageVector =
        pocketIcon("Translator", "M5 8l6 6M4 14l6-6 2-3M2 5h12M7 2h1M22 22l-5-10-5 10M14 18h6")

    @Composable
    override fun HelpContent() = ToolHelp(Res.string.translator_help)

    @Composable
    override fun Content() = TranslatorContent()

    companion object {
        const val ID = "translator"
    }
}
