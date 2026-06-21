package ua.constitution

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ua.constitution.data.source.ConstitutionLoader

/**
 * Characterizes ConstitutionLoader: a real end-to-end load of the bundled JSON and the default
 * chapter fallback list. Robolectric is required for Context, assets, org.json, SHA-256 and string
 * resources. The loader is stateless, so each test gets a fresh load (no reflection reset needed).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConstitutionLoaderTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `load parses the bundled constitution`() {
        val content = ConstitutionLoader.load(context)

        assertFalse("a successful parse must not flag the fallback", content.usedFallback)
        assertTrue(content.integrityVerificationPass) // set true even on hash mismatch (quirk)
        assertTrue(content.computedHash.isNotEmpty())

        val articles = content.articles
        assertTrue("expected the full constitution to load", articles.size >= 150)

        // Preamble is article id 0 in chapter 0.
        assertEquals(0, articles.first { it.id == 0 }.chapterId)

        // Fractional article "16.1" is parsed to id 16001 (N*1000+M encoding); the real Article 161
        // keeps id 161 — the two no longer collide.
        assertTrue(articles.any { it.id == 16001 })
        assertTrue(articles.any { it.id == 161 })

        // Chapters include the preamble and the final transitional-provisions chapter.
        assertTrue(content.chapters.any { it.id == 0 })
        assertTrue(content.chapters.any { it.id == 15 })
    }

    @Test
    fun `defaultChapters lists 15 chapters including preamble and skipping chapter 7`() {
        val chapters = ConstitutionLoader.defaultChapters(context)
        assertEquals(15, chapters.size)
        assertTrue(chapters.any { it.id == 0 })   // preamble
        assertTrue(chapters.any { it.id == 15 })  // transitional provisions
        assertTrue(chapters.none { it.id == 7 })  // chapter 7 is intentionally skipped
    }
}
