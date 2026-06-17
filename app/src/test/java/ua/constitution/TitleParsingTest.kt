package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ua.constitution.domain.text.formatStringToSuperscript
import ua.constitution.utils.Constants

/**
 * Characterizes the article-title parsing contract used by ArticleCard.
 *
 * The card splits a title like "Стаття 20. Державні символи" into a label+number and a name using
 * Constants.SPLIT_TITLE_REGEX_PATTERN (the inline regex at MainActivity.kt:1472 is byte-for-byte
 * identical to this constant), then runs the captured number through formatStringToSuperscript.
 * Both pieces are pure, so we pin the full pipeline here without touching the composable.
 */
class TitleParsingTest {

    private val regex = Constants.SPLIT_TITLE_REGEX_PATTERN.toRegex()

    @Test
    fun `splits a Стаття title into label, number and name`() {
        val m = regex.find("Стаття 20. Державні символи України")
        assertEquals("Стаття", m?.groupValues?.get(1))
        assertEquals("20", m?.groupValues?.get(2))
        assertEquals("Державні символи України", m?.groupValues?.get(3))
    }

    @Test
    fun `splits a Пункт title`() {
        val m = regex.find("Пункт 5. Назва")
        assertEquals("Пункт", m?.groupValues?.get(1))
        assertEquals("5", m?.groupValues?.get(2))
    }

    @Test
    fun `captures hyphenated and dotted article numbers verbatim`() {
        assertEquals("129-1", regex.find("Стаття 129-1. Судове рішення")?.groupValues?.get(2))
        assertEquals("16.1", regex.find("Стаття 16.1. Текст")?.groupValues?.get(2))
    }

    @Test
    fun `a number with no name yields an empty name group`() {
        val m = regex.find("Стаття 1.")
        assertEquals("1", m?.groupValues?.get(2))
        assertEquals("", m?.groupValues?.get(3))
    }

    @Test
    fun `the trailing dot after the number is optional`() {
        val m = regex.find("Стаття 2")
        assertEquals("2", m?.groupValues?.get(2))
        assertEquals("", m?.groupValues?.get(3))
    }

    @Test
    fun `titles that are not Стаття or Пункт do not match`() {
        assertNull(regex.find("Преамбула"))
        assertNull(regex.find("Загальні засади"))
    }

    @Test
    fun `full pipeline raises a fractional number to superscript`() {
        // What ArticleCard ultimately displays as the "number" portion.
        val number = regex.find("Стаття 16.1. Текст")!!.groupValues[2]
        assertEquals("16¹", formatStringToSuperscript(number))
    }

    @Test
    fun `non-matching title falls back to superscript-formatting the whole title unchanged`() {
        assertEquals("Преамбула", formatStringToSuperscript("Преамбула"))
    }
}
