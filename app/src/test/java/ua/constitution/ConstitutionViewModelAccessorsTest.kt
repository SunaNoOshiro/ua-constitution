package ua.constitution

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ua.constitution.data.database.BookmarkEntity
import ua.constitution.data.model.Chapter
import ua.constitution.data.repository.ConstitutionRepository
import ua.constitution.ui.viewmodel.ConstitutionViewModel

/**
 * Pins the plain content read accessors added to the ViewModel in F3, so the UI can stop reading
 * the ConstitutionData global. They delegate to the injected segregated sources and move former
 * inline UI logic verbatim — these tests guard that logic before F4-F6 depend on it.
 *
 * Pure JVM. Dispatchers.setMain is needed only because constructing the ViewModel touches
 * viewModelScope (the bookmarks stateIn); the accessors themselves are synchronous.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConstitutionViewModelAccessorsTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val content = FakeContentSource(
        articles = listOf(
            articleOf(id = 0, chapterId = 0, titleUa = "Преамбула"),
            articleOf(id = 1, chapterId = 1, titleUa = "Стаття 1"),
            articleOf(id = 2, chapterId = 1, titleUa = "Стаття 2"),
            articleOf(id = 20, chapterId = 2, titleUa = "Стаття 20")
        ),
        chapters = listOf(Chapter(0, "Преамбула"), Chapter(1, "Розділ I"), Chapter(2, "Розділ II")),
        computedHash = "abc123"
    )

    private lateinit var viewModel: ConstitutionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ConstitutionViewModel(
            ConstitutionRepository(FakeConstitutionDao()), content, content, content, content
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `chapters delegates to the chapter source`() {
        assertEquals(listOf(0, 1, 2), viewModel.chapters.map { it.id })
    }

    @Test
    fun `articlesForChapter delegates to the article lookup`() {
        assertEquals(listOf(1, 2), viewModel.articlesForChapter(1).map { it.id })
        assertEquals(listOf(20), viewModel.articlesForChapter(2).map { it.id })
        assertTrue(viewModel.articlesForChapter(99).isEmpty())
    }

    @Test
    fun `bookmarkedArticles returns bookmarked articles in article order`() {
        val a1 = content.articles.first { it.id == 1 }
        val a20 = content.articles.first { it.id == 20 }
        // Bookmarks intentionally out of article order; result must follow article order.
        val bookmarks = listOf(
            BookmarkEntity(articleId = a20.bookmarkId),
            BookmarkEntity(articleId = a1.bookmarkId)
        )
        assertEquals(listOf(1, 20), viewModel.bookmarkedArticles(bookmarks).map { it.id })
    }

    @Test
    fun `articleOfDay selects by day-of-year modulo article count`() {
        assertEquals(content.articles[3 % 4], viewModel.articleOfDay(3))
        assertEquals(content.articles[5 % 4], viewModel.articleOfDay(5)) // wraps around
    }

    @Test
    fun `articleOfDay returns null when there is no content`() {
        val empty = FakeContentSource()
        val vm = ConstitutionViewModel(
            ConstitutionRepository(FakeConstitutionDao()), empty, empty, empty, empty
        )
        assertNull(vm.articleOfDay(42))
    }

    @Test
    fun `resolveLink delegates to findArticleByLink over the content list`() {
        assertEquals(20, viewModel.resolveLink("20")?.id)
        assertNull(viewModel.resolveLink("nothing-numeric"))
    }

    @Test
    fun `integrity accessors expose the status fields`() {
        assertEquals("abc123", viewModel.computedHash)
        assertEquals("", viewModel.initializationError)
        assertFalse(viewModel.articlesEmpty)
        assertEquals(4, viewModel.articlesCount)
    }
}
