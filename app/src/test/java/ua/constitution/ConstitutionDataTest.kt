package ua.constitution

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ua.constitution.data.model.ConstitutionData

/**
 * Characterizes the ConstitutionData singleton: its accessors (seeded via initializeForTests) and a
 * single end-to-end load of the real bundled JSON via initialize(context).
 *
 * Robolectric is required for the real-asset load (Context, assets, org.json, SHA-256 logging) and
 * for the default-chapter fallback (string resources).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConstitutionDataTest {

    /**
     * ConstitutionData is a process-wide object and Robolectric shares it across this class's test
     * methods, so an earlier test's initializeForTests leaves the `isInitialized` guard set. Reset
     * it before each test so the real-asset test performs a genuine fresh initialize().
     */
    @Before
    fun resetInitGuard() {
        ConstitutionData::class.java.getDeclaredField("isInitialized")
            .apply { isAccessible = true }
            .setBoolean(ConstitutionData, false)
    }

    @Test
    fun `initializeForTests seeds the article list`() {
        val seeded = listOf(articleOf(id = 1, chapterId = 1), articleOf(id = 2, chapterId = 1))
        ConstitutionData.initializeForTests(seeded)
        assertEquals(seeded, ConstitutionData.articles)
    }

    @Test
    fun `getArticlesForChapter returns only that chapter's articles`() {
        ConstitutionData.initializeForTests(
            listOf(
                articleOf(id = 1, chapterId = 1),
                articleOf(id = 2, chapterId = 1),
                articleOf(id = 20, chapterId = 2)
            )
        )
        assertEquals(listOf(1, 2), ConstitutionData.getArticlesForChapter(1).map { it.id })
        assertEquals(listOf(20), ConstitutionData.getArticlesForChapter(2).map { it.id })
        assertTrue(ConstitutionData.getArticlesForChapter(99).isEmpty())
    }

    @Test
    fun `getArticleById returns the matching article`() {
        ConstitutionData.initializeForTests(listOf(articleOf(id = 0, chapterId = 0), articleOf(id = 20, chapterId = 2)))
        assertEquals(20, ConstitutionData.getArticleById(20)?.id)
    }

    @Test
    fun `CHARACTERIZATION getArticleById returns the first article when the id is unknown`() {
        // The production fallback is `?: articles.firstOrNull()`, so an unknown id silently returns
        // the first article (here the preamble) rather than null.
        ConstitutionData.initializeForTests(listOf(articleOf(id = 0, chapterId = 0), articleOf(id = 20, chapterId = 2)))
        assertEquals(0, ConstitutionData.getArticleById(99999)?.id)
    }

    @Test
    fun `getArticleById on an empty data set returns null`() {
        ConstitutionData.initializeForTests(emptyList())
        assertNull(ConstitutionData.getArticleById(1))
    }

    @Test
    fun `getRandomArticle on a single-article set returns that article`() {
        val only = articleOf(id = 42, chapterId = 3)
        ConstitutionData.initializeForTests(listOf(only))
        assertEquals(only, ConstitutionData.getRandomArticle())
    }

    @Test
    fun `chapters falls back to the default list when none were parsed`() {
        ConstitutionData.initializeForTests(listOf(articleOf(id = 1, chapterId = 1)), emptyList())
        val chapters = ConstitutionData.chapters
        assertEquals(15, chapters.size)
        assertTrue(chapters.any { it.id == 0 })   // preamble
        assertTrue(chapters.any { it.id == 15 })  // transitional provisions
        assertTrue(chapters.none { it.id == 7 })  // chapter 7 is intentionally skipped
    }

    @Test
    fun `real asset initialize loads and parses the bundled constitution`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ConstitutionData.initialize(context)

        assertFalse("a successful parse must not flag the fallback", ConstitutionData.usedFallback)
        assertTrue(ConstitutionData.integrityVerificationPass) // set true even on hash mismatch (quirk)
        assertTrue(ConstitutionData.computedHash.isNotEmpty())

        val articles = ConstitutionData.articles
        assertTrue("expected the full constitution to load", articles.size >= 150)

        // Preamble is article id 0 in chapter 0.
        assertEquals(0, articles.first { it.id == 0 }.chapterId)

        // Fractional article "16.1" is parsed to id 161 (Math.round(16.1 * 10)).
        assertTrue(articles.any { it.id == 161 })

        // Chapters include the preamble and the final transitional-provisions chapter.
        assertTrue(ConstitutionData.chapters.any { it.id == 0 })
        assertTrue(ConstitutionData.chapters.any { it.id == 15 })
    }
}
