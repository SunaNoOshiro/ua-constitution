package ua.constitution

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ua.constitution.data.database.DatabaseProvider

/**
 * Characterizes the dashboard BOOKMARKS tab BEFORE it is extracted into its own composable:
 * bookmarking an article makes it appear in the Bookmarks tab (the empty-state message disappears).
 * The empty rendering is already pinned by MainActivityTest; this pins the populated path that the
 * BookmarksTabContent extraction must preserve.
 *
 * The app uses a process-wide singleton Room DB, so this test clears the bookmarks table before and
 * after itself to avoid leaking state into MainActivityTest (which asserts an empty Bookmarks tab).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "xxhdpi")
class BookmarksFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var activity: MainActivity

    @Before
    fun setup() {
        clearBookmarks()
        activity = composeTestRule.activity
        composeTestRule.waitForIdle()
    }

    @After
    fun tearDown() {
        clearBookmarks()
    }

    private fun clearBookmarks() = runBlocking {
        val dao = DatabaseProvider.getDatabase(ApplicationProvider.getApplicationContext<Context>()).constitutionDao()
        dao.getAllBookmarks().first().forEach { dao.deleteBookmarkByArticleId(it.articleId) }
    }

    @Test
    fun bookmarking_an_article_populates_the_bookmarks_tab() {
        // Bookmarks tab starts empty.
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.tab_bookmarks), useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText(activity.getString(R.string.no_bookmarks_msg), useUnmergedTree = true)
            .assertCountEquals(1)

        // Open the Articles tab and bookmark the first article.
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.tab_articles), useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithContentDescription(activity.getString(R.string.save_bookmark), useUnmergedTree = true)
            .onFirst().performClick()
        composeTestRule.waitForIdle()

        // Back to the Bookmarks tab: the empty message must be gone (the list is populated).
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.tab_bookmarks), useUnmergedTree = true).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(activity.getString(R.string.no_bookmarks_msg), useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }
}
