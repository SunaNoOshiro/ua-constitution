package ua.constitution

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test
import ua.constitution.ui.article.SegmentTextStyle
import ua.constitution.ui.article.toTextStyle

/**
 * Pins SegmentTextStyle.toTextStyle — the single home for the effective reader TextStyle that three
 * reader render paths previously built identically inline.
 */
class SegmentTextStyleTest {

    @Test
    fun `toTextStyle applies the grouped overrides and forces Start alignment`() {
        val result = SegmentTextStyle(
            style = TextStyle(fontStyle = FontStyle.Italic),
            color = Color.Red,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Normal,
        ).toTextStyle()

        assertEquals(Color.Red, result.color)
        assertEquals(30.sp, result.lineHeight)
        assertEquals(FontWeight.Bold, result.fontWeight)
        assertEquals(FontStyle.Normal, result.fontStyle)
        assertEquals(TextAlign.Start, result.textAlign)
    }

    @Test
    fun `toTextStyle falls back to the base style fontStyle when the override is null`() {
        val result = SegmentTextStyle(
            style = TextStyle(fontStyle = FontStyle.Italic),
            fontStyle = null,
        ).toTextStyle()

        assertEquals(FontStyle.Italic, result.fontStyle)
    }
}
