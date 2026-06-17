package ua.constitution

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Characterizes the @Composable article-id formatters (formatArticleId, getBackNavigationText,
 * ArticleIdText). These are prime candidates for extraction into pure helpers during a future
 * refactor, so pinning their rendered output now protects that change.
 *
 * Each @Test gets a fresh compose rule, so each calls setContent exactly once.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ArticleIdComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun formatId(id: Int, chapterId: Int): String {
        var result = ""
        composeTestRule.setContent { result = formatArticleId(id, chapterId) }
        composeTestRule.waitForIdle()
        return result
    }

    private fun backText(article: ua.constitution.data.model.Article): String {
        var result = ""
        composeTestRule.setContent { result = getBackNavigationText(article) }
        composeTestRule.waitForIdle()
        return result
    }

    // --- formatArticleId ------------------------------------------------------------------------

    @Test
    fun `formatArticleId returns the preamble string for id 0`() {
        assertEquals("Преамбула", formatId(0, 0))
    }

    @Test
    fun `CHARACTERIZATION formatArticleId special-cases article 16-1 in chapter 15`() {
        assertEquals("16¹", formatId(161, 15))
    }

    @Test
    fun `formatArticleId renders ids over 1000 as base plus superscript`() {
        assertEquals("100¹", formatId(1001, 0))
    }

    @Test
    fun `formatArticleId returns a plain number for a normal article`() {
        assertEquals("20", formatId(20, 1))
    }

    @Test
    fun `formatArticleId leaves exactly 1000 as a plain number`() {
        assertEquals("1000", formatId(1000, 0))
    }

    // --- getBackNavigationText ------------------------------------------------------------------

    @Test
    fun `getBackNavigationText for the preamble`() {
        assertEquals("Назад до Преамбули", backText(articleOf(id = 0, chapterId = 0)))
    }

    @Test
    fun `getBackNavigationText for a chapter 15 punkt`() {
        assertEquals("Назад до Розділу 15, п. 16¹", backText(articleOf(id = 161, chapterId = 15)))
    }

    @Test
    fun `getBackNavigationText for a normal chapter article`() {
        assertEquals("Назад до Розділу 1, ст. 1", backText(articleOf(id = 1, chapterId = 1)))
    }

    // --- ArticleIdText (rendered text) ----------------------------------------------------------

    @Test
    fun `ArticleIdText renders the preamble label for id 0`() {
        composeTestRule.setContent {
            MaterialTheme {
                ArticleIdText(id = 0, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        composeTestRule.onNodeWithText("Преамбула").assertIsDisplayed()
    }

    @Test
    fun `ArticleIdText renders base plus suffix as one string for a fractional id`() {
        composeTestRule.setContent {
            MaterialTheme {
                ArticleIdText(id = 161, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold, chapterId = 15)
            }
        }
        composeTestRule.onNodeWithText("161").assertIsDisplayed()
    }

    @Test
    fun `ArticleIdText renders a plain number for a normal article`() {
        composeTestRule.setContent {
            MaterialTheme {
                ArticleIdText(id = 20, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        composeTestRule.onNodeWithText("20").assertIsDisplayed()
    }
}
