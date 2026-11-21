package tech.onova.offlinetranslator.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing

/**
 * A tool's "How to use" text, so it reads in blocks instead of a wall of text (owner's phone pass, 2026-11-05). Every
 * line of [text] is a paragraph, and a line starting with "- " is a list item.
 */
@Composable
public fun HelpBody(
    text: String,
    modifier: Modifier = Modifier,
) {
    val blocks = helpBlocks(text)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(PocketSpacing.sm)) {
        blocks.forEach { block ->
            when (block) {
                is HelpBlock.Paragraph -> {
                    Text(text = block.text, style = MaterialTheme.typography.bodyLarge)
                }

                is HelpBlock.Items -> {
                    Column(verticalArrangement = Arrangement.spacedBy(PocketSpacing.xs)) {
                        block.items.forEach { item -> HelpItem(item) }
                    }
                }
            }
        }
    }
}

/** A list item: its dot is centered on the first line, and the lines that wrap start under the first one. */
@Composable
private fun HelpItem(text: String) {
    val style = MaterialTheme.typography.bodyLarge
    val firstLine = with(LocalDensity.current) { style.lineHeight.toDp() }
    Row(horizontalArrangement = Arrangement.spacedBy(PocketSpacing.sm)) {
        Box(modifier = Modifier.height(firstLine), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .size(PocketSizes.helpItemDot)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
        Text(text = text, style = style)
    }
}

internal sealed interface HelpBlock {
    data class Paragraph(
        val text: String,
    ) : HelpBlock

    data class Items(
        val items: List<String>,
    ) : HelpBlock
}

/** The blocks of a help text: list items in a row make one list, and blank lines are ignored. */
internal fun helpBlocks(text: String): List<HelpBlock> =
    buildList {
        var items = emptyList<String>()

        fun closeList() {
            if (items.isNotEmpty()) add(HelpBlock.Items(items))
            items = emptyList()
        }
        text.lines().map(String::trim).filter(String::isNotEmpty).forEach { line ->
            if (line.startsWith(ITEM_MARK)) {
                items = items + line.removePrefix(ITEM_MARK).trim()
            } else {
                closeList()
                add(HelpBlock.Paragraph(line))
            }
        }
        closeList()
    }

private const val ITEM_MARK = "- "
