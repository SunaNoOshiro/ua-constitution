package ua.constitution

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ua.constitution.data.source.ConstitutionJsonParser

/**
 * Characterizes the extracted ConstitutionJsonParser on crafted JSON — parse() in isolation, plus a
 * smoke check of computeIntegrity. Needs Robolectric for Context / org.json / string resources.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConstitutionJsonParserTest {

    private lateinit var parser: ConstitutionJsonParser

    @Before
    fun setup() {
        parser = ConstitutionJsonParser(ApplicationProvider.getApplicationContext<Context>())
    }

    @Test
    fun `parse reads the preamble, nested articles and fractional ids in sorted order`() {
        val json = """
            {
              "preamble": { "titleUa": "Преамбула", "paragraphs": [] },
              "chapters": [
                {
                  "id": 1, "titleUa": "Загальні засади",
                  "articles": [
                    { "id": 1, "chapterId": 1, "titleUa": "Стаття 1", "paragraphs": [] },
                    { "id": 16.1, "chapterId": 1, "titleUa": "Стаття 16.1", "paragraphs": [] }
                  ]
                }
              ]
            }
        """.trimIndent()

        val parsed = parser.parse(json)

        // preamble (id 0) + article 1 + fractional 16.1 -> 161, sorted ascending
        assertEquals(listOf(0, 1, 161), parsed.articles.map { it.id })
        assertEquals(0, parsed.articles.first { it.id == 0 }.chapterId)
        // preamble chapter (0) + chapter 1
        assertEquals(listOf(0, 1), parsed.chapters.map { it.id })
    }

    @Test
    fun `CHARACTERIZATION when the preamble key is absent a fallback preamble article is synthesized`() {
        val json = """{ "chapters": [ { "id": 1, "titleUa": "X", "articles": [] } ] }"""

        val parsed = parser.parse(json)

        val preamble = parsed.articles.single { it.id == 0 }
        assertEquals("Преамбула", preamble.titleUa) // R.string.preamble
        assertTrue(preamble.textUa.isNotEmpty())    // fallback text paragraph
        assertEquals(listOf(0, 1), parsed.chapters.map { it.id })
    }

    @Test
    fun `computeIntegrity returns a hash and always reports pass`() {
        val result = parser.computeIntegrity()
        assertTrue(result.verificationPass) // set true even on mismatch (preserved quirk)
        assertTrue(result.computedHash.isNotEmpty())
    }
}
