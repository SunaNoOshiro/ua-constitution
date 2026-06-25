package ua.constitution.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The reader body-text font-size multiplier (1.0 = default), provided at the app root from the
 * user's settings. Only the constitution reading text scales by this — the app chrome (nav, headers,
 * buttons) keeps its fixed sizes so layouts stay stable.
 */
val LocalFontScale = staticCompositionLocalOf { 1.0f }
