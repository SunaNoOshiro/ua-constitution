package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Characterizes the pure text-selection / range-merging helpers in MainActivity.kt:
 *   getWordRangeAtOffset, getWordSnappedRange, mergeAdjacentStyledRanges, plus StyledRange defaults.
 *
 * Pure JVM — no Android runtime needed.
 */
class TextSelectionLogicTest {

    // --- StyledRange defaults -------------------------------------------------------------------

    @Test
    fun `StyledRange highlight and underscore colors default to the base color`() {
        val r = StyledRange(0, 5, "#FF0000", highlight = true, underscore = false)
        assertEquals("#FF0000", r.highlightColorHex)
        assertEquals("#FF0000", r.underscoreColorHex)
    }

    // --- getWordRangeAtOffset -------------------------------------------------------------------

    @Test
    fun `offset inside a word returns the whole word bounds`() {
        // Ukrainian Cyrillic letters are treated as letters; "Україна" spans [0,7).
        assertEquals(0 to 7, getWordRangeAtOffset("Україна суверенна", 3))
        assertEquals(0 to 5, getWordRangeAtOffset("hello world", 0))
        assertEquals(6 to 11, getWordRangeAtOffset("hello world", 6))
    }

    @Test
    fun `offset on whitespace or out of bounds returns null`() {
        assertNull(getWordRangeAtOffset("hello world", 5)) // the space
        assertNull(getWordRangeAtOffset("", 0))            // empty text, offset out of bounds
        assertNull(getWordRangeAtOffset("   ", 1))         // only whitespace
        assertNull(getWordRangeAtOffset("hello", -1))      // negative
        assertNull(getWordRangeAtOffset("hello", 5))       // == length
    }

    @Test
    fun `leading and trailing punctuation is trimmed from the returned word`() {
        assertEquals(1 to 6, getWordRangeAtOffset("(hello)", 0))
    }

    @Test
    fun `CHARACTERIZATION punctuation inside a word is kept (apostrophe is not a boundary)`() {
        // Only whitespace ends the forward/backward scan, so "don't" is one word and the inner
        // apostrophe is never trimmed.
        assertEquals(0 to 5, getWordRangeAtOffset("don't", 3))
    }

    // --- getWordSnappedRange --------------------------------------------------------------------

    @Test
    fun `snapping spans from the word at the lower offset to the word at the upper offset`() {
        assertEquals(0 to 11, getWordSnappedRange("hello world", 0, 10))
    }

    @Test
    fun `out-of-bounds offsets are coerced into the text before snapping`() {
        assertEquals(0 to 5, getWordSnappedRange("hello", 100, 200))
    }

    @Test
    fun `CHARACTERIZATION collapsed selection on a gap between two words returns null`() {
        // Both offsets land on the single space at index 5. The start search scans forward to the
        // next word (start=6) and the end search scans backward to the previous word (end=5), which
        // inverts the range, so the function returns null rather than snapping to either word.
        assertNull(getWordSnappedRange("hello world", 5, 5))
    }

    @Test
    fun `snapping over empty text returns null`() {
        assertNull(getWordSnappedRange("", 0, 0))
    }

    // --- mergeAdjacentStyledRanges --------------------------------------------------------------

    @Test
    fun `empty or single-element lists are returned as-is`() {
        assertEquals(emptyList<StyledRange>(), mergeAdjacentStyledRanges("abc", emptyList()))
        val one = listOf(StyledRange(0, 3, "#FF0000", highlight = true, underscore = false))
        assertEquals(one, mergeAdjacentStyledRanges("abc", one))
    }

    @Test
    fun `adjacent ranges with the same spec merge into one`() {
        val merged = mergeAdjacentStyledRanges(
            "HelloWorld",
            listOf(
                StyledRange(0, 5, "#FF0000", highlight = true, underscore = false),
                StyledRange(5, 10, "#FF0000", highlight = true, underscore = false)
            )
        )
        assertEquals(1, merged.size)
        assertEquals(0, merged[0].start)
        assertEquals(10, merged[0].end)
    }

    @Test
    fun `a gap made only of whitespace or punctuation is bridged`() {
        val merged = mergeAdjacentStyledRanges(
            "abc   def",
            listOf(
                StyledRange(0, 3, "#FF0000", highlight = true, underscore = false),
                StyledRange(6, 9, "#FF0000", highlight = true, underscore = false)
            )
        )
        assertEquals(1, merged.size)
        assertEquals(0 to 9, merged[0].start to merged[0].end)
    }

    @Test
    fun `a gap containing letters or digits is NOT bridged`() {
        val merged = mergeAdjacentStyledRanges(
            "abc123def",
            listOf(
                StyledRange(0, 3, "#FF0000", highlight = true, underscore = false),
                StyledRange(6, 9, "#FF0000", highlight = true, underscore = false)
            )
        )
        assertEquals(2, merged.size)
    }

    @Test
    fun `ranges with differing highlight or underscore flags stay separate`() {
        val merged = mergeAdjacentStyledRanges(
            "HelloWorld",
            listOf(
                StyledRange(0, 5, "#FF0000", highlight = true, underscore = false),
                StyledRange(5, 10, "#FF0000", highlight = false, underscore = false)
            )
        )
        assertEquals(2, merged.size)
    }

    @Test
    fun `CHARACTERIZATION color comparison is case-insensitive and the merged range keeps the first color's case`() {
        val merged = mergeAdjacentStyledRanges(
            "HelloWorld",
            listOf(
                StyledRange(0, 5, "#FF0000", highlight = true, underscore = false),
                StyledRange(5, 10, "#ff0000", highlight = true, underscore = false)
            )
        )
        assertEquals(1, merged.size)
        // They merge (case-insensitive equality) but the result is built from `last` (the already-
        // accumulated FIRST range), so it keeps that range's original color text — not the later one.
        assertEquals("#FF0000", merged[0].colorHex)
    }
}
