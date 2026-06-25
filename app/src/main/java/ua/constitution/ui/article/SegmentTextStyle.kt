package ua.constitution.ui.article

import ua.constitution.ui.theme.*

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
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

/**
 * The effective [TextStyle] for rendering: the base [SegmentTextStyle.style] with the grouped
 * overrides applied and Start alignment. The reader composables built this identically inline in
 * three places (editable BasicTextField, read-only Text, read-only ClickableText).
 */
fun SegmentTextStyle.toTextStyle(): TextStyle =
    style.copy(
        color = color,
        lineHeight = lineHeight,
        fontWeight = fontWeight,
        fontStyle = fontStyle ?: style.fontStyle,
        textAlign = TextAlign.Start,
    )

/**
 * [toTextStyle] with the reader's [LocalFontScale] applied to font size and line height (only the
 * reading text scales; returns the unscaled style at scale 1.0). Specified-unit guarded so an
 * inherited/Unspecified size is left untouched.
 */
@Composable
fun SegmentTextStyle.toScaledTextStyle(): TextStyle {
    val scale = LocalFontScale.current
    val base = toTextStyle()
    if (scale == 1f) return base
    return base.copy(
        fontSize = if (base.fontSize.isSpecified) base.fontSize * scale else base.fontSize,
        lineHeight = if (base.lineHeight.isSpecified) base.lineHeight * scale else base.lineHeight,
    )
}
