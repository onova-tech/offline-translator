package tech.onova.offlinetranslator.core.designsystem.components

import kotlin.test.Test
import kotlin.test.assertEquals

class HelpBlocksTest {
    @Test
    fun aLineIsAParagraph() {
        assertEquals(
            listOf(HelpBlock.Paragraph("One."), HelpBlock.Paragraph("Two.")),
            helpBlocks("One.\nTwo."),
        )
    }

    @Test
    fun itemsInARowMakeOneListBetweenParagraphs() {
        assertEquals(
            listOf(
                HelpBlock.Paragraph("Shows:"),
                HelpBlock.Items(listOf("A.", "B.")),
                HelpBlock.Paragraph("Then:"),
                HelpBlock.Items(listOf("C.")),
            ),
            helpBlocks("Shows:\n- A.\n- B.\nThen:\n- C."),
        )
    }

    @Test
    fun aTextMayStartAndEndWithAList() {
        assertEquals(
            listOf(HelpBlock.Items(listOf("A.")), HelpBlock.Paragraph("End."), HelpBlock.Items(listOf("B."))),
            helpBlocks("- A.\nEnd.\n- B."),
        )
    }

    @Test
    fun blankLinesAndSpacesAreIgnored() {
        assertEquals(
            listOf(HelpBlock.Paragraph("One."), HelpBlock.Items(listOf("A."))),
            helpBlocks("  One. \n\n  - A.  \n"),
        )
    }

    @Test
    fun aHyphenInsideALineIsNotAnItem() {
        val text = "A 30-second trace - or less."
        assertEquals(listOf(HelpBlock.Paragraph(text)), helpBlocks(text))
    }

    @Test
    fun aTextWithNoLinesHasNoBlocks() {
        assertEquals(emptyList(), helpBlocks(""))
    }
}
