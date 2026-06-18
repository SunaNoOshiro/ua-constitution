package ua.constitution

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "xxhdpi")
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var activity: MainActivity

    @Before
    fun setup() {
        // MainActivity loads its own content in onCreate via ConstitutionLoader; just wait for it.
        activity = composeTestRule.activity
        composeTestRule.waitForIdle()
    }

    @Test
    fun testTabNavigation() {
        composeTestRule.onNodeWithText(activity.getString(R.string.national_symbols_header), substring = true, useUnmergedTree = true).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.tab_chapters), useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.select_chapter_header), useUnmergedTree = true).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.tab_bookmarks), useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.no_bookmarks_msg), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun testSearchIntegration() {
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.tab_search), useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("search_field", useUnmergedTree = true).performTextInput("20")
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("20", substring = true, useUnmergedTree = true).onFirst().assertIsDisplayed()
    }
}
