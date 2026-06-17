package ua.constitution

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ua.constitution.data.database.BookmarkEntity
import ua.constitution.data.database.ConstitutionDao
import ua.constitution.data.database.ConstitutionDatabase

/**
 * Characterizes the real Room DAO contract against an in-memory database. This is what
 * [FakeConstitutionDao] is built to mirror; pinning it here means a refactor that touches the
 * persistence layer can't silently change insert/ordering/no-op semantics.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BookmarkDaoTest {

    private lateinit var db: ConstitutionDatabase
    private lateinit var dao: ConstitutionDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ConstitutionDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.constitutionDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `inserted bookmark is returned by getAllBookmarks`() = runBlocking {
        dao.insertBookmark(BookmarkEntity(articleId = 1))
        assertEquals(listOf(1), dao.getAllBookmarks().first().map { it.articleId })
    }

    @Test
    fun `CHARACTERIZATION inserting the same articleId replaces the existing row`() = runBlocking {
        dao.insertBookmark(BookmarkEntity(articleId = 1, notes = "first"))
        dao.insertBookmark(BookmarkEntity(articleId = 1, notes = "second"))
        val all = dao.getAllBookmarks().first()
        assertEquals(1, all.size)
        assertEquals("second", all.single().notes)
    }

    @Test
    fun `getAllBookmarks is ordered by bookmarkedAt descending`() = runBlocking {
        dao.insertBookmark(BookmarkEntity(articleId = 1, bookmarkedAt = 100L))
        dao.insertBookmark(BookmarkEntity(articleId = 2, bookmarkedAt = 200L))
        assertEquals(listOf(2, 1), dao.getAllBookmarks().first().map { it.articleId })
    }

    @Test
    fun `deleteBookmarkByArticleId removes the row`() = runBlocking {
        dao.insertBookmark(BookmarkEntity(articleId = 1))
        dao.deleteBookmarkByArticleId(1)
        assertTrue(dao.getAllBookmarks().first().isEmpty())
    }

    @Test
    fun `CHARACTERIZATION deleting a missing row is a silent no-op`() = runBlocking {
        dao.deleteBookmarkByArticleId(99)
        assertTrue(dao.getAllBookmarks().first().isEmpty())
    }

    @Test
    fun `updateBookmarkNotes updates only the notes of an existing row`() = runBlocking {
        dao.insertBookmark(BookmarkEntity(articleId = 1, notes = "", editsJson = "keep"))
        dao.updateBookmarkNotes(1, "updated")
        val entity = dao.getBookmarkByArticle(1).first()
        assertEquals("updated", entity?.notes)
        assertEquals("keep", entity?.editsJson)
    }

    @Test
    fun `CHARACTERIZATION updating notes for a missing row inserts nothing`() = runBlocking {
        dao.updateBookmarkNotes(99, "x")
        assertTrue(dao.getAllBookmarks().first().isEmpty())
    }

    @Test
    fun `updateBookmarkEdits updates only the editsJson of an existing row`() = runBlocking {
        dao.insertBookmark(BookmarkEntity(articleId = 1, notes = "keep"))
        dao.updateBookmarkEdits(1, "{}")
        val entity = dao.getBookmarkByArticle(1).first()
        assertEquals("{}", entity?.editsJson)
        assertEquals("keep", entity?.notes)
    }

    @Test
    fun `getBookmarkByArticle returns null when absent and the entity when present`() = runBlocking {
        assertNull(dao.getBookmarkByArticle(1).first())
        dao.insertBookmark(BookmarkEntity(articleId = 1))
        assertEquals(1, dao.getBookmarkByArticle(1).first()?.articleId)
    }
}
