package ua.constitution

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Characterizes the pure superscript / index-mapping helpers in MainActivity.kt:
 *   formatStringToSuperscript, isSuperscriptEquivalent, mapOriginalToFormatted, mapFormattedToOriginal.
 *
 * These are plain JVM functions (no Android types), so this class needs no Robolectric runner.
 * Every assertion locks in CURRENT behavior, including a few counter-intuitive quirks that a future
 * SOLID refactor must preserve.
 */
class SuperscriptFormattingTest {

    // --- formatStringToSuperscript --------------------------------------------------------------

    @Test
    fun `dotted number gets its fractional part raised to superscript`() {
        assertEquals("16¹", formatStringToSuperscript("16.1"))
        assertEquals("16¹⁰", formatStringToSuperscript("16.10"))
        assertEquals("16¹²³", formatStringToSuperscript("16.123"))
    }

    @Test
    fun `hyphenated number gets its suffix raised to superscript`() {
        assertEquals("129¹", formatStringToSuperscript("129-1"))
        assertEquals("12³", formatStringToSuperscript("12-3"))
    }

    @Test
    fun `leading zeros in the suffix are preserved as superscripts`() {
        assertEquals("01⁰²", formatStringToSuperscript("01.02"))
    }

    @Test
    fun `text without a digit-separator-digit pattern is returned unchanged`() {
        assertEquals("Преамбула", formatStringToSuperscript("Преамбула"))
        assertEquals("1", formatStringToSuperscript("1"))
        assertEquals("", formatStringToSuperscript(""))
        // trailing/leading separator with no digit on one side does not match
        assertEquals("16.", formatStringToSuperscript("16."))
        assertEquals("-1", formatStringToSuperscript("-1"))
    }

    @Test
    fun `every dot pattern in the string is converted, not just the first`() {
        assertEquals("1² and 3⁴", formatStringToSuperscript("1.2 and 3.4"))
    }

    @Test
    fun `CHARACTERIZATION dots are applied before hyphens so a mixed token only raises the dot part`() {
        // "1.2-3": the dot pass turns "1.2" into "1²", leaving "1²-3". The hyphen pass then needs an
        // ASCII digit immediately before '-', but the char there is now the superscript '²', so it
        // does NOT match. Result keeps the literal "-3". (A naive reader might expect "1²³".)
        assertEquals("1²-3", formatStringToSuperscript("1.2-3"))
    }

    @Test
    fun `CHARACTERIZATION a second dot group after an already-converted token is left untouched`() {
        // Matches are non-overlapping over the original string: "1.2" converts, the trailing ".3"
        // has no digit before the dot left to consume, so it stays literal.
        assertEquals("1².3", formatStringToSuperscript("1.2.3"))
    }

    // --- isSuperscriptEquivalent ----------------------------------------------------------------

    @Test
    fun `digit maps to its matching unicode superscript`() {
        assertTrue(isSuperscriptEquivalent('0', '⁰'))
        assertTrue(isSuperscriptEquivalent('1', '¹'))
        assertTrue(isSuperscriptEquivalent('9', '⁹'))
    }

    @Test
    fun `digit does not match a different or non-superscript char`() {
        assertFalse(isSuperscriptEquivalent('1', '²'))
        assertFalse(isSuperscriptEquivalent('1', '1'))
    }

    @Test
    fun `non-digit normal char never matches`() {
        assertFalse(isSuperscriptEquivalent('a', '⁰'))
        assertFalse(isSuperscriptEquivalent('.', '¹'))
    }

    // --- mapOriginalToFormatted / mapFormattedToOriginal ----------------------------------------

    @Test
    fun `mapOriginalToFormatted produces an index for every original char plus a terminal`() {
        // "16.1" -> "16¹": indices 0,1 map straight through; the '.' maps to the current formatted
        // position WITHOUT consuming it; the trailing '1' maps onto the superscript '¹'.
        val mapping = mapOriginalToFormatted("16.1", "16¹")
        assertArrayEquals(intArrayOf(0, 1, 2, 2, 3), mapping)
        assertEquals("16.1".length + 1, mapping.size)
    }

    @Test
    fun `mapFormattedToOriginal skips the dot in the original when mapping back`() {
        // "16¹" back onto "16.1": the superscript '¹' maps to original index 3 (the trailing '1'),
        // having skipped the '.' at index 2.
        val mapping = mapFormattedToOriginal("16.1", "16¹")
        assertArrayEquals(intArrayOf(0, 1, 3, 4), mapping)
        assertEquals("16¹".length + 1, mapping.size)
    }

    @Test
    fun `empty strings yield a single terminal index`() {
        assertArrayEquals(intArrayOf(0), mapOriginalToFormatted("", ""))
        assertArrayEquals(intArrayOf(0), mapFormattedToOriginal("", ""))
    }
}
