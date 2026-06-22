package ua.constitution.ui.article

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import ua.constitution.data.model.ContentSegment
import ua.constitution.ui.theme.SovereignBlue
import ua.constitution.utils.Constants

/**
 * Builds the reader's display string from already-merged content segments.
 *
 * Each segment's [ContentSegment.displayText] is appended in order, remapped from original to
 * superscript-formatted coordinates via [origToFormMapping] (so the appended text comes from
 * [formattedText]). Link segments are wrapped in a blue / bold / underlined [SpanStyle] and carry a
 * [Constants.ANNOTATION_TAG_URL] string annotation of the form "url|text" (consumed by the reader's
 * tap handler). Extracted verbatim from `SegmentedTextWithEdits` so the mapping is unit-testable.
 */
fun buildSegmentedAnnotatedString(
    mergedSegments: List<ContentSegment>,
    formattedText: String,
    origToFormMapping: IntArray
): AnnotatedString = buildAnnotatedString {
    var originalOffset = 0
    mergedSegments.forEach { segment ->
        val segLen = segment.displayText.length
        val startOrig = originalOffset
        val endOrig = originalOffset + segLen

        val startForm = origToFormMapping.getOrElse(startOrig) { startOrig }.coerceIn(0, formattedText.length)
        val endForm = origToFormMapping.getOrElse(endOrig) { endOrig }.coerceIn(0, formattedText.length)

        val textToAppend = formattedText.substring(startForm, endForm)

        if (segment.type == Constants.TYPE_LINK) {
            pushStringAnnotation(tag = Constants.ANNOTATION_TAG_URL, annotation = "${segment.url}|${segment.text}")
            withStyle(
                style = SpanStyle(
                    color = SovereignBlue,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(textToAppend)
            }
            pop()
        } else {
            append(textToAppend)
        }
        originalOffset = endOrig
    }
}
