package ua.constitution.ui.model

import androidx.compose.ui.graphics.Color
import ua.constitution.ui.theme.CoatOfArmsBlue

/**
 * A national symbol shown full-screen. Each declares its own immersive backdrop so adding a symbol
 * is a one-line change here rather than editing the overlay's color expression.
 */
enum class FullscreenSymbol(val overlayBackground: Color) {
    NONE(Color.Black),
    FLAG(Color.Black),
    COAT_OF_ARMS(CoatOfArmsBlue)
}
