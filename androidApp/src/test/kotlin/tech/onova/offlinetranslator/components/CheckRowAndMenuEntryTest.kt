package tech.onova.offlinetranslator.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Test
import tech.onova.offlinetranslator.core.designsystem.components.CheckRow
import tech.onova.offlinetranslator.core.designsystem.components.ChoiceOption
import tech.onova.offlinetranslator.core.designsystem.components.MenuChoice
import tech.onova.offlinetranslator.core.designsystem.components.MenuEntry
import tech.onova.offlinetranslator.support.AppUiTest
import kotlin.test.assertTrue

/** Spec 080 (T015, T016): a whole-row check box, and an action at the end of a menu's list. */
class CheckRowAndMenuEntryTest : AppUiTest() {
    @Test
    fun theWholeRowIsOneCheckBoxTarget() {
        show {
            var checked by remember { mutableStateOf(false) }
            CheckRow(title = "Japanese", checked = checked, onCheckedChange = { checked = it }, supporting = "日本語")
        }
        val row =
            compose
                .onNodeWithText(
                    "Japanese",
                ).assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
        row.assertIsOff().performClick()
        row.assertIsOn()
        val bounds = row.getUnclippedBoundsInRoot()
        assertTrue(bounds.bottom - bounds.top >= 48.dp, "a whole touch target")
    }

    @Test
    fun aDisabledRowKeepsItsState() {
        show { CheckRow(title = "English", checked = true, onCheckedChange = {}, enabled = false) }
        compose.onNodeWithText("English").assertIsNotEnabled().assertIsOn()
    }

    @Test
    fun theLastEntryClosesTheListAndActs() {
        var opened = false
        show {
            MenuChoice(
                options = listOf(ChoiceOption("en", "English"), ChoiceOption("pt", "Portuguese")),
                selected = "en",
                onSelect = {},
                name = "From",
                last = MenuEntry("Other languages…") { opened = true },
            )
        }
        compose.onNodeWithContentDescription("From").performClick()
        compose.onNode(hasText("Other languages…") and hasAnyAncestor(isPopup())).performClick()
        compose.waitForIdle()
        assertTrue(opened)
        compose.onNode(hasText("Portuguese") and hasAnyAncestor(isPopup())).assertDoesNotExist()
    }
}
