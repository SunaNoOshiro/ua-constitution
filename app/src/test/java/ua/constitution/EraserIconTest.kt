package ua.constitution

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import ua.constitution.ui.createEraserIcon

/**
 * Pins the extracted eraser icon (N7): name, 24dp/24f viewport, and that the fill color is the only
 * parameter (both former call sites differed only in fill). The icon's rendering stays exercised by
 * the toolbar/panel composables.
 */
class EraserIconTest {

    @Test
    fun `eraser icon has the expected name and 24 viewport`() {
        val icon = createEraserIcon(Color.White)
        assertEquals("Eraser", icon.name)
        assertEquals(24.dp, icon.defaultWidth)
        assertEquals(24.dp, icon.defaultHeight)
        assertEquals(24f, icon.viewportWidth)
        assertEquals(24f, icon.viewportHeight)
    }

    @Test
    fun `both fill colors build the same-shaped icon`() {
        assertEquals("Eraser", createEraserIcon(Color.White).name)
        assertEquals("Eraser", createEraserIcon(Color.Black).name)
    }
}
