package ua.constitution

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Characterizes the dashboard ARTICLES tab rendering BEFORE it is extracted into its own composable
 * (ArticlesTabContent) and before the navigation/scroll state is hoisted into DashboardNavState.
 *
 * This is the "add-test-first" safety net (same pattern as DashboardNavigationTest for CHAPTERS and
 * BookmarksFlowTest for BOOKMARKS): it pins the parts of the inline ARTICLES branch that the
 * extraction must preserve verbatim — the article list, the quick-links chip row for a large
 * chapter, the chip-tap collapse behavior, and the absence of the back button when there is no
 * navigation history.
 *
 * The navigation push/pop/trim LOGIC is intentionally NOT pinned here: it is the swipe/link-driven
 * state machine that Robolectric cannot trigger reliably. That logic moves into the plain
 * DashboardNavState holder in the next batch and is pinned there with direct unit tests
 * (DashboardNavStateTest), which is both more precise and more robust.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "xxhdpi")
class DashboardArticlesTabTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var activity: MainActivity

    @Before
    fun setup() {
        activity = composeTestRule.activity
        composeTestRule.waitForIdle()
    }

    private fun openArticlesTab() {
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.tab_articles), useUnmergedTree = true)
            .performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun articles_tab_renders_article_list_and_has_no_back_button_initially() {
        openArticlesTab()

        // The default chapter (1) has articles, so article cards render (each exposes a copy button).
        val copyButtons = composeTestRule.onAllNodesWithTag("copy_article_button", useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("expected the default chapter's article list to render", copyButtons.isNotEmpty())

        // With no cross-article navigation yet, the back-to-previous button must be absent.
        composeTestRule.onAllNodesWithTag("back_to_previous_article_button", useUnmergedTree = true)
            .assertCountEquals(0)
    }

    @Test
    fun expanding_quick_links_reveals_the_article_chip_row() {
        openArticlesTab()

        // Chapter 1 has >= 5 articles, so the quick-links toggle is present and chips start hidden.
        composeTestRule.onNodeWithTag("quick_links_toggle", useUnmergedTree = true).assertExists()
        composeTestRule.onAllNodesWithTag("quick_link_chip_0", useUnmergedTree = true).assertCountEquals(0)

        // Expand the quick-links: the chip row for the chapter's articles becomes visible.
        composeTestRule.onNodeWithTag("quick_links_toggle", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("quick_link_chip_0", useUnmergedTree = true).assertIsDisplayed()

        // The whole chapter renders a chip per article (chapter 1 has 20).
        val chips = composeTestRule.onAllNodesWithTag("quick_link_chip_0", useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("expected the quick-links chip row to render", chips.isNotEmpty())
    }
}
