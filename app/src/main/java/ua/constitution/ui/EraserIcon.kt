package ua.constitution.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The eraser tool icon as an [ImageVector], parameterized by [fillColor]. Extracted from the two
 * identical inline builders in InteractiveText (white in the selection toolbar, black in the global
 * formatting panel). Both had stroke = null, so their formerly-differing stroke params were no-ops;
 * only the fill color differed, which is the sole parameter here. Path coordinates are verbatim.
 */
fun createEraserIcon(fillColor: Color): ImageVector =
    ImageVector.Builder(
        name = "Eraser",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(fillColor)) {
            moveTo(16.24f, 3.56f)
            lineTo(21.19f, 8.51f)
            curveTo(21.97f, 9.29f, 21.97f, 10.56f, 21.19f, 11.34f)
            lineTo(14.12f, 18.41f)
            lineTo(9.17f, 13.46f)
            lineTo(16.24f, 3.56f)
            close()
            moveTo(7.76f, 14.88f)
            lineTo(12.71f, 19.83f)
            lineTo(5.64f, 21.0f)
            lineTo(2.0f, 21.0f)
            lineTo(7.76f, 14.88f)
            close()
        }
    }.build()
