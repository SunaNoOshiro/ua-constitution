package ua.constitution
import ua.constitution.ui.theme.HighlightPalette

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ua.constitution.ui.theme.MyApplicationTheme
import ua.constitution.utils.Constants

/**
 * Pins the global highlight-editor tool panel before its five copy-pasted tool-button scaffolds are
 * collapsed into a reusable ToolToggleButton. Asserts the buttons render and that the tool-select /
 * clear-all callbacks fire — the contract the extraction must preserve.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "xxhdpi")
class GlobalFormattingPanelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setPanel(
        activeTool: String = Constants.TOOL_NONE,
        hasAnyEdits: Boolean = true,
        onTool: (String) -> Unit = {},
        onClear: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            MyApplicationTheme {
                GlobalFormattingPanel(
                    editingArticleId = 1,
                    controls = FormattingPanelControls(
                        activeTool = activeTool,
                        onActiveToolChange = onTool,
                        selectedColorHex = HighlightPalette.DEFAULT_MARKER,
                        onColorHexChange = {},
                        isPanelExpanded = true,
                        onPanelExpandedChange = {},
                    ),
                    onClearAllEdits = onClear,
                    hasAnyEdits = hasAnyEdits,
                    onDoneEditing = {},
                    articleTitle = "Стаття 1",
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun renders_all_tool_buttons() {
        setPanel()
        composeTestRule.onNodeWithTag("tool_button_none", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("tool_button_marker", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("tool_button_underline", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("tool_button_eraser", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("tool_button_clear_all", useUnmergedTree = true).assertExists()
    }

    @Test
    fun tapping_marker_selects_the_marker_tool() {
        var picked: String? = null
        setPanel(onTool = { picked = it })
        composeTestRule.onNodeWithTag("tool_button_marker", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        assertEquals(Constants.TOOL_MARKER, picked)
    }

    @Test
    fun tapping_an_active_tool_toggles_it_back_to_none() {
        var picked: String? = null
        setPanel(activeTool = Constants.TOOL_UNDERLINE, onTool = { picked = it })
        composeTestRule.onNodeWithTag("tool_button_underline", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        assertEquals(Constants.TOOL_NONE, picked)
    }

    @Test
    fun tapping_clear_all_clears_when_there_are_edits() {
        var cleared = false
        setPanel(hasAnyEdits = true, onClear = { cleared = true })
        composeTestRule.onNodeWithTag("tool_button_clear_all", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        assertTrue(cleared)
    }
}
