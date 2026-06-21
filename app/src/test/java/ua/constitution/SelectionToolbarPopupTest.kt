package ua.constitution

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ua.constitution.ui.theme.MyApplicationTheme

/**
 * Behavior net for the SelectionToolbarPopup composable extracted from SegmentedTextWithEdits (K1).
 * Renders the dumb toolbar directly with stub callbacks and asserts each button is wired to the
 * right action (the native selection gesture that shows the toolbar can't be triggered under
 * Robolectric, so this pins the rendering + click->callback surface the extraction could break).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "xxhdpi")
class SelectionToolbarPopupTest {

    @get:Rule
    val rule = createComposeRule()

    private val markerColors = listOf("#FFF59D", "#FFCC80")
    private val underlineColors = listOf("#F57F17", "#1565C0")

    private val noPosition = object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowSize: IntSize,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize
        ): IntOffset = IntOffset.Zero
    }

    private fun str(id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun setToolbar(
        showCopy: Boolean = true,
        showEraser: Boolean = true,
        onCopy: () -> Unit = {},
        onSelectAll: () -> Unit = {},
        onApplyMarker: (String) -> Unit = {},
        onApplyUnderline: (String) -> Unit = {},
        onApplyEraser: () -> Unit = {}
    ) {
        rule.setContent {
            MyApplicationTheme {
                SelectionToolbarPopup(
                    positionProvider = noPosition,
                    onDismissRequest = {},
                    selectionKey = "key",
                    showCopy = showCopy,
                    onCopy = onCopy,
                    onSelectAll = onSelectAll,
                    formatting = FormattingTools(
                        showFormatting = true,
                        markerColors = markerColors,
                        underlineColors = underlineColors,
                        lastMarkerColor = "#FFF59D",
                        lastUnderlineColor = "#F57F17",
                        eraserIcon = Icons.Default.Clear,
                        showEraser = showEraser,
                        onApplyMarker = onApplyMarker,
                        onApplyUnderline = onApplyUnderline,
                        onApplyEraser = onApplyEraser
                    )
                )
            }
        }
    }

    @Test
    fun copy_button_invokes_onCopy() {
        var copied = 0
        setToolbar(onCopy = { copied++ })
        rule.onNodeWithContentDescription(str(R.string.btn_copy)).performClick()
        assertEquals(1, copied)
    }

    @Test
    fun select_all_button_invokes_onSelectAll() {
        var count = 0
        setToolbar(onSelectAll = { count++ })
        rule.onNodeWithContentDescription(str(R.string.btn_select_all)).performClick()
        assertEquals(1, count)
    }

    @Test
    fun marker_quick_button_applies_the_last_marker_color() {
        var applied: String? = null
        setToolbar(onApplyMarker = { applied = it })
        rule.onNodeWithText(str(R.string.tool_marker)).performClick()
        assertEquals("#FFF59D", applied)
    }

    @Test
    fun underline_quick_button_applies_the_last_underline_color() {
        var applied: String? = null
        setToolbar(onApplyUnderline = { applied = it })
        rule.onNodeWithText(str(R.string.tool_underline)).performClick()
        assertEquals("#F57F17", applied)
    }

    @Test
    fun eraser_button_invokes_onApplyEraser() {
        var erased = 0
        setToolbar(showEraser = true, onApplyEraser = { erased++ })
        rule.onNodeWithText(str(R.string.tool_eraser)).performClick()
        assertEquals(1, erased)
    }

    @Test
    fun eraser_button_is_hidden_without_edits() {
        setToolbar(showEraser = false)
        rule.onAllNodesWithText(str(R.string.tool_eraser)).assertCountEquals(0)
    }

    @Test
    fun copy_button_is_hidden_when_showCopy_is_false() {
        setToolbar(showCopy = false)
        rule.onAllNodesWithContentDescription(str(R.string.btn_copy)).assertCountEquals(0)
    }

    @Test
    fun marker_picker_swatch_applies_that_swatch_color() {
        var applied: String? = null
        setToolbar(onApplyMarker = { applied = it })
        rule.onNodeWithTag("toolbar_marker_picker").performClick()
        rule.onAllNodesWithTag("toolbar_marker_swatch").onFirst().performClick()
        assertEquals(markerColors.first(), applied)
    }
}
