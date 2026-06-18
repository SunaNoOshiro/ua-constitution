package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.constitution.domain.text.BackNavigationTarget
import ua.constitution.domain.text.backNavigationTarget

/**
 * Pure tests for the back-navigation branch extracted from the getBackNavigationText composable in
 * N2. The rendered strings stay pinned by ArticleIdComposeTest; this pins which message applies.
 */
class BackNavigationTest {

    @Test
    fun `chapter 15 maps to the punkt message`() {
        assertEquals(BackNavigationTarget.CHAPTER_15, backNavigationTarget(15))
    }

    @Test
    fun `chapter 0 maps to the preamble message`() {
        assertEquals(BackNavigationTarget.PREAMBLE, backNavigationTarget(0))
    }

    @Test
    fun `any other chapter maps to the generic chapter-article message`() {
        assertEquals(BackNavigationTarget.OTHER, backNavigationTarget(1))
        assertEquals(BackNavigationTarget.OTHER, backNavigationTarget(14))
        assertEquals(BackNavigationTarget.OTHER, backNavigationTarget(2))
    }
}
