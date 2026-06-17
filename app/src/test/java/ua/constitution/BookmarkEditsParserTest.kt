package ua.constitution

import ua.constitution.domain.bookmark.BookmarkEditsParser
import ua.constitution.domain.text.StyledRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Characterizes BookmarkEditsParser (MainActivity.kt) — JSON (de)serialization of per-paragraph
 * highlight/underline edits. Needs Robolectric because it uses org.json.JSONObject/JSONArray.
 *
 * Note: the two malformed-input tests below intentionally drive the parser's internal
 * `catch { e.printStackTrace() }`, so you will see JSONException stack traces in the console even
 * though the build/tests pass — that printed trace is the production code's existing behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BookmarkEditsParserTest {

    @Test
    fun `a simple edit map round-trips through toJson and parse`() {
        val edits = mapOf(0 to listOf(StyledRange(0, 5, "#FF0000", highlight = true, underscore = false)))
        val parsed = BookmarkEditsParser.parse(BookmarkEditsParser.toJson(edits))
        assertEquals(1, parsed.size)
        assertEquals(
            StyledRange(0, 5, "#FF0000", highlight = true, underscore = false),
            parsed[0]!!.single()
        )
    }

    @Test
    fun `custom highlight and underscore colors survive the round-trip`() {
        val range = StyledRange(0, 5, "#FF0000", highlight = true, underscore = true, "#00FF00", "#0000FF")
        val parsed = BookmarkEditsParser.parse(BookmarkEditsParser.toJson(mapOf(2 to listOf(range))))
        assertEquals(range, parsed[2]!!.single())
    }

    @Test
    fun `null blank and empty inputs parse to an empty map`() {
        assertTrue(BookmarkEditsParser.parse(null).isEmpty())
        assertTrue(BookmarkEditsParser.parse("").isEmpty())
        assertTrue(BookmarkEditsParser.parse("   ").isEmpty())
    }

    @Test
    fun `an empty edit map serializes to an empty paragraphEdits object`() {
        assertEquals("""{"paragraphEdits":{}}""", BookmarkEditsParser.toJson(emptyMap()))
    }

    @Test
    fun `CHARACTERIZATION a paragraph with an empty range list is dropped on round-trip`() {
        // toJson skips empty lists entirely, so the key disappears — {0:[]} does not survive.
        val parsed = BookmarkEditsParser.parse(BookmarkEditsParser.toJson(mapOf(0 to emptyList())))
        assertTrue(parsed.isEmpty())
    }

    @Test
    fun `CHARACTERIZATION malformed JSON yields an empty map rather than throwing`() {
        assertTrue(BookmarkEditsParser.parse("{not valid").isEmpty())
    }

    @Test
    fun `CHARACTERIZATION a range missing a required field discards the entire parse result`() {
        // The 'end' field is missing; getInt throws, the catch swallows it, and the whole map is
        // returned empty (not a partial result).
        val json = """{"paragraphEdits":{"0":[{"start":0}]}}"""
        assertTrue(BookmarkEditsParser.parse(json).isEmpty())
    }

    @Test
    fun `paragraphEdits that is not an object yields an empty map`() {
        assertTrue(BookmarkEditsParser.parse("""{"paragraphEdits":[]}""").isEmpty())
    }

    @Test
    fun `a non-integer paragraph key is skipped`() {
        assertTrue(BookmarkEditsParser.parse("""{"paragraphEdits":{"x":[]}}""").isEmpty())
    }

    @Test
    fun `multiple paragraphs are all parsed`() {
        val edits = mapOf(
            0 to listOf(StyledRange(0, 2, "#FFF59D", highlight = true, underscore = false)),
            5 to listOf(StyledRange(1, 4, "#F57F17", highlight = false, underscore = true))
        )
        val parsed = BookmarkEditsParser.parse(BookmarkEditsParser.toJson(edits))
        assertEquals(setOf(0, 5), parsed.keys)
    }
}
