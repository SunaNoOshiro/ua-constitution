package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ua.constitution.domain.bookmark.reconcileEditsOnInput

/**
 * Pins ArticleCard's edits-reconciliation decision (extracted from a LaunchedEffect into a pure
 * function). Needs Robolectric only because the parse path uses org.json (same as
 * BookmarkEditsParserTest); the decision logic itself is pure.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditsReconcilerTest {

    private val json = """{"paragraphEdits":{"0":[{"start":0,"end":3,"colorHex":"#FFEB3B","highlight":true,"underscore":false}]}}"""

    @Test
    fun not_editable_shows_no_edits() {
        assertEquals(emptyMap<Int, Any>(), reconcileEditsOnInput(json, isEditable = false, lastSavedJson = "anything"))
    }

    @Test
    fun editable_parses_the_incoming_json() {
        val result = reconcileEditsOnInput(json, isEditable = true, lastSavedJson = "")
        assertTrue("expected the incoming edits to be parsed", result != null && result.isNotEmpty())
    }

    @Test
    fun editable_keeps_current_edits_when_incoming_matches_what_was_just_saved() {
        // A save round-trip: the incoming JSON equals lastSavedJson -> keep current local edits.
        assertNull(reconcileEditsOnInput(json, isEditable = true, lastSavedJson = json))
    }

    @Test
    fun editable_reparses_when_incoming_differs_from_last_saved() {
        val result = reconcileEditsOnInput(json, isEditable = true, lastSavedJson = "{}")
        assertTrue(result != null && result.isNotEmpty())
    }
}
