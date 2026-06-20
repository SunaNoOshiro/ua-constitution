package ua.constitution

import ua.constitution.ui.theme.*

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * The shared text-styling inputs for the reader composables (SegmentedText / SegmentedTextWithEdits),
 * grouped into one parameter object (Introduce Parameter Object) to shorten their parameter lists.
 * Defaults reproduce the previous per-parameter defaults; [style] has no default here because the
 * composables supply MaterialTheme.typography.bodyMedium in composable context via their default arg.
 */
data class SegmentTextStyle(
    val style: TextStyle,
    val color: Color = RichNavyText,
    val lineHeight: TextUnit = 24.sp,
    val fontWeight: FontWeight = FontWeight.Medium,
    val fontStyle: FontStyle? = null,
)
