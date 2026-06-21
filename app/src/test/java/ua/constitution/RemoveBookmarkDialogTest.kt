package ua.constitution

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ua.constitution.ui.article.RemoveBookmarkDialog
import ua.constitution.ui.theme.MyApplicationTheme

/**
 * Behavior net for the RemoveBookmarkDialog composable extracted from ArticleCard (O1). Renders the
 * dumb dialog directly with stub callbacks and asserts each button maps to the right action.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "xxhdpi")
class RemoveBookmarkDialogTest {

    @get:Rule
    val rule = createComposeRule()

    private fun str(id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun setDialog(onConfirm: () -> Unit = {}, onDismiss: () -> Unit = {}) {
        rule.setContent {
            MyApplicationTheme {
                RemoveBookmarkDialog(onConfirm = onConfirm, onDismiss = onDismiss)
            }
        }
    }

    @Test
    fun shows_title_and_message() {
        setDialog()
        rule.onNodeWithText(str(R.string.dialog_remove_bookmark_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.dialog_remove_bookmark_message)).assertIsDisplayed()
    }

    @Test
    fun confirm_button_invokes_onConfirm_only() {
        var confirmed = 0
        var dismissed = 0
        setDialog(onConfirm = { confirmed++ }, onDismiss = { dismissed++ })
        rule.onNodeWithText(str(R.string.dialog_remove_bookmark_confirm)).performClick()
        assertEquals(1, confirmed)
        assertEquals(0, dismissed)
    }

    @Test
    fun cancel_button_invokes_onDismiss_only() {
        var confirmed = 0
        var dismissed = 0
        setDialog(onConfirm = { confirmed++ }, onDismiss = { dismissed++ })
        rule.onNodeWithText(str(R.string.dialog_remove_bookmark_cancel)).performClick()
        assertEquals(0, confirmed)
        assertEquals(1, dismissed)
    }
}
