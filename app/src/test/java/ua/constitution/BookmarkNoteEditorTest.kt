package ua.constitution

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ua.constitution.ui.screens.BookmarkNoteEditor
import ua.constitution.ui.theme.MyApplicationTheme

/**
 * Pins the BookmarkNoteEditor in isolation (the bookmark Flow that drives it end-to-end is covered
 * by BookmarksFlowTest): the Save action is disabled until the draft differs from the saved note,
 * and saving delivers the trimmed text to the callback.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "xxhdpi")
class BookmarkNoteEditorTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun save_is_disabled_until_the_draft_changes_then_saves_the_trimmed_note() {
        var saved: String? = null
        rule.setContent {
            MyApplicationTheme {
                BookmarkNoteEditor(initialNote = "old", onSaveNote = { saved = it })
            }
        }

        // Pre-filled with the saved note, so Save starts disabled (no change yet).
        rule.onNodeWithTag("bookmark_note_save").assertIsNotEnabled()

        rule.onNodeWithTag("bookmark_note_field").performTextReplacement("  my note  ")
        rule.onNodeWithTag("bookmark_note_save").assertIsEnabled()
        rule.onNodeWithTag("bookmark_note_save").performClick()

        assertEquals("my note", saved)
    }
}
