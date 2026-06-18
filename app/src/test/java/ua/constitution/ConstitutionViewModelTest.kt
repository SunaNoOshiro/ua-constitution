package ua.constitution

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ua.constitution.ui.viewmodel.ConstitutionViewModel
import ua.constitution.data.repository.ConstitutionRepository

/**
 * Characterizes ConstitutionViewModel: search filtering, chapter/article selection and bookmarking.
 *
 * Pure JVM. Two details of the production design drive the test setup:
 *  - filteredArticles / bookmarks are stateIn(..., WhileSubscribed(5000)), so a collector must be
 *    active for the upstream flow to run. Tests that assert on filtering or bookmark state launch a
 *    collector in backgroundScope, and use runTest(testDispatcher) so that collector is hot
 *    synchronously (shares the UnconfinedTestDispatcher set as Main).
 *  - filteredArticles reads the injected content source, so each test supplies a FakeContentSource.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConstitutionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeDao: FakeConstitutionDao
    private lateinit var viewModel: ConstitutionViewModel

    // After the DIP refactor the ViewModel takes a content source, so we inject the shared
    // FakeContentSource double instead of seeding the ConstitutionData global.
    private val contentSource = FakeContentSource(
        articles = listOf(
            articleOf(id = 0, chapterId = 0, titleUa = "Преамбула"),
            articleOf(
                id = 1, chapterId = 1, titleUa = "Стаття 1",
                paragraphs = listOf(paragraphOf(textSegment("Україна є суверенна і незалежна держава")))
            ),
            articleOf(id = 2, chapterId = 1, titleUa = "Стаття 2"),
            articleOf(
                id = 20, chapterId = 2, titleUa = "Стаття 20",
                paragraphs = listOf(paragraphOf(textSegment("Державні символи України")))
            )
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeConstitutionDao()
        viewModel = ConstitutionViewModel(
            ConstitutionRepository(fakeDao), contentSource, contentSource, contentSource, contentSource
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- search & filtering ---------------------------------------------------------------------

    @Test
    fun `empty query returns all articles`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.filteredArticles.collect() }
        assertEquals(4, viewModel.filteredArticles.value.size)
    }

    @Test
    fun `empty query with a selected chapter returns only that chapter`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.filteredArticles.collect() }
        viewModel.selectChapter(1)
        assertEquals(listOf(1, 2), viewModel.filteredArticles.value.map { it.id })
    }

    @Test
    fun `CHARACTERIZATION a non-empty query performs a global search and ignores the chapter filter`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.filteredArticles.collect() }
        viewModel.selectChapter(2)        // would normally restrict to chapter 2
        viewModel.setSearchQuery("1")     // matches article 1 (chapter 1) by id and title
        val ids = viewModel.filteredArticles.value.map { it.id }
        assertTrue("global search should surface article 1 from chapter 1", ids.contains(1))
    }

    @Test
    fun `CHARACTERIZATION id match is exact OR substring so '2' matches both 2 and 20`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.filteredArticles.collect() }
        viewModel.setSearchQuery("2")
        val ids = viewModel.filteredArticles.value.map { it.id }
        assertTrue(ids.contains(2))
        assertTrue(ids.contains(20))
    }

    @Test
    fun `query '20' matches only article 20`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.filteredArticles.collect() }
        viewModel.setSearchQuery("20")
        assertEquals(listOf(20), viewModel.filteredArticles.value.map { it.id })
    }

    @Test
    fun `title search is case-insensitive including Ukrainian text`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.filteredArticles.collect() }
        viewModel.setSearchQuery("СТАТТЯ")
        val ids = viewModel.filteredArticles.value.map { it.id }
        assertEquals(listOf(1, 2, 20), ids) // preamble title has no "стаття"
    }

    @Test
    fun `body text search is case-insensitive`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.filteredArticles.collect() }
        viewModel.setSearchQuery("державні")
        assertEquals(listOf(20), viewModel.filteredArticles.value.map { it.id })
    }

    @Test
    fun `query matching nothing returns an empty list`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.filteredArticles.collect() }
        viewModel.setSearchQuery("zzzzz")
        assertTrue(viewModel.filteredArticles.value.isEmpty())
    }

    // --- chapter / article selection ------------------------------------------------------------

    @Test
    fun `CHARACTERIZATION selecting the same chapter twice toggles it back off`() {
        viewModel.selectChapter(1)
        assertEquals(1, viewModel.selectedChapterId.value)
        viewModel.selectChapter(1)
        assertNull(viewModel.selectedChapterId.value)
    }

    @Test
    fun `selecting a different chapter replaces the current one`() {
        viewModel.selectChapter(1)
        viewModel.selectChapter(2)
        assertEquals(2, viewModel.selectedChapterId.value)
    }

    @Test
    fun `selectArticle then clearSelectedArticle updates the selection`() {
        val article = articleOf(id = 1, chapterId = 1)
        viewModel.selectArticle(article)
        assertEquals(article, viewModel.selectedArticle.value)
        viewModel.clearSelectedArticle()
        assertNull(viewModel.selectedArticle.value)
    }

    // --- bookmarking ----------------------------------------------------------------------------

    @Test
    fun `toggling a bookmark adds it then removes it`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.bookmarks.collect() }
        viewModel.toggleBookmark(1)
        assertTrue(fakeDao.bookmarks.value.any { it.articleId == 1 })
        viewModel.toggleBookmark(1)
        assertFalse(fakeDao.bookmarks.value.any { it.articleId == 1 })
    }

    @Test
    fun `multiple distinct bookmarks coexist`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.bookmarks.collect() }
        viewModel.toggleBookmark(1)
        viewModel.toggleBookmark(2)
        assertEquals(setOf(1, 2), fakeDao.bookmarks.value.map { it.articleId }.toSet())
    }

    @Test
    fun `updateNotes is delegated to the repository for the bookmarked article`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.bookmarks.collect() }
        viewModel.toggleBookmark(1)
        viewModel.updateNotes(1, "моя нотатка")
        assertEquals("моя нотатка", fakeDao.bookmarks.value.first { it.articleId == 1 }.notes)
    }

    @Test
    fun `CHARACTERIZATION updateBookmarkEdits stores the raw string without validation`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.bookmarks.collect() }
        viewModel.toggleBookmark(1)
        viewModel.updateBookmarkEdits(1, "{not valid json")
        assertEquals("{not valid json", fakeDao.bookmarks.value.first { it.articleId == 1 }.editsJson)
    }
}
