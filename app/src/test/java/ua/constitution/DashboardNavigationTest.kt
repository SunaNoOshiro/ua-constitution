package ua.constitution

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
 * Characterizes the dashboard CHAPTERS-tab navigation BEFORE it is extracted into its own composable
 * (O3): tapping a chapter card leaves the chapter list and renders that chapter's article list. This
 * is the safety net the audit asked for ("add-test-first") around the dashboard tab decomposition.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "xxhdpi")
class DashboardNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var activity: MainActivity

    @Before
    fun setup() {
        activity = composeTestRule.activity
        composeTestRule.waitForIdle()
    }

    @Test
    fun tapping_a_chapter_card_opens_that_chapters_article_list() {
        // Go to the Chapters tab and confirm the chapter picker is shown.
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.tab_chapters), useUnmergedTree = true)
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(activity.getString(R.string.select_chapter_header), useUnmergedTree = true)
            .assertIsDisplayed()

        // Tap chapter 2's card.
        composeTestRule.onNodeWithTag("chapter_card_2", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        // We navigated off the chapter picker into the article list (article cards expose a copy button).
        composeTestRule.onAllNodesWithText(activity.getString(R.string.select_chapter_header), useUnmergedTree = true)
            .assertCountEquals(0)
        val copyButtons = composeTestRule.onAllNodesWithTag("copy_article_button", useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("expected the chapter's article list to render", copyButtons.isNotEmpty())
    }
}
