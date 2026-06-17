package ua.constitution

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ua.constitution.data.repository.ConstitutionRepository

/**
 * Characterizes ConstitutionRepository's thin delegation to the DAO, using the in-memory fake.
 * Pure JVM. (The fake's behavior is validated against the real Room DAO in BookmarkDaoTest.)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConstitutionRepositoryTest {

    private lateinit var fakeDao: FakeConstitutionDao
    private lateinit var repository: ConstitutionRepository

    @Before
    fun setup() {
        fakeDao = FakeConstitutionDao()
        repository = ConstitutionRepository(fakeDao)
    }

    @Test
    fun `addBookmark defaults notes to empty string`() = runTest {
        repository.addBookmark(1)
        val entity = fakeDao.bookmarks.value.first { it.articleId == 1 }
        assertEquals("", entity.notes)
    }

    @Test
    fun `addBookmark stores supplied notes`() = runTest {
        repository.addBookmark(2, "hi")
        assertEquals("hi", fakeDao.bookmarks.value.first { it.articleId == 2 }.notes)
    }

    @Test
    fun `CHARACTERIZATION adding the same article again replaces the previous bookmark`() = runTest {
        repository.addBookmark(1, "first")
        repository.addBookmark(1, "second")
        val matches = fakeDao.bookmarks.value.filter { it.articleId == 1 }
        assertEquals(1, matches.size)
        assertEquals("second", matches.single().notes)
    }

    @Test
    fun `removeBookmark deletes an existing bookmark`() = runTest {
        repository.addBookmark(1)
        repository.removeBookmark(1)
        assertTrue(fakeDao.bookmarks.value.none { it.articleId == 1 })
    }

    @Test
    fun `CHARACTERIZATION removing a non-existent bookmark is a silent no-op`() = runTest {
        repository.removeBookmark(99) // nothing stored
        assertTrue(fakeDao.bookmarks.value.isEmpty())
    }

    @Test
    fun `updateBookmarkNotes updates an existing bookmark`() = runTest {
        repository.addBookmark(1)
        repository.updateBookmarkNotes(1, "note")
        assertEquals("note", fakeDao.bookmarks.value.first { it.articleId == 1 }.notes)
    }

    @Test
    fun `CHARACTERIZATION updating notes for a missing article inserts nothing`() = runTest {
        repository.updateBookmarkNotes(99, "note")
        assertTrue(fakeDao.bookmarks.value.isEmpty())
    }

    @Test
    fun `CHARACTERIZATION updateBookmarkEdits persists arbitrary strings unchanged`() = runTest {
        repository.addBookmark(1)
        repository.updateBookmarkEdits(1, "{malformed")
        assertEquals("{malformed", fakeDao.bookmarks.value.first { it.articleId == 1 }.editsJson)
    }

    @Test
    fun `getBookmarkByArticle reflects presence and absence`() = runTest {
        assertNull(repository.getBookmarkByArticle(1).first())
        repository.addBookmark(1)
        assertEquals(1, repository.getBookmarkByArticle(1).first()?.articleId)
    }

    @Test
    fun `allBookmarks exposes the current set`() = runTest {
        repository.addBookmark(1)
        repository.addBookmark(2)
        assertEquals(setOf(1, 2), repository.allBookmarks.first().map { it.articleId }.toSet())
    }
}
