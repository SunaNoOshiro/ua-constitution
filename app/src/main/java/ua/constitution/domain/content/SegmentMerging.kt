package ua.constitution.domain.content

import ua.constitution.data.model.ContentSegment

/**
 * Merges consecutive "link" segments that share the same URL into a single segment (concatenating
 * their text), leaving non-link segments untouched. This prevents a cross-reference link that the
 * parser split across several segments (e.g. "149-1") from rendering as separate tappable pieces.
 *
 * Extracted verbatim from the reader composables (SegmentedTextWithEdits / SegmentedText) to remove
 * duplication and make the logic unit-testable.
 */
fun mergeAdjacentLinkSegments(segments: List<ContentSegment>): List<ContentSegment> {
    val result = mutableListOf<ContentSegment>()
    var currentLink: ContentSegment? = null
    for (segment in segments) {
        if (segment.type == "link") {
            if (currentLink != null && currentLink.url == segment.url) {
                currentLink = currentLink.copy(text = currentLink.text + segment.text)
            } else {
                if (currentLink != null) {
                    result.add(currentLink)
                }
                currentLink = segment
            }
        } else {
            if (currentLink != null) {
                result.add(currentLink)
                currentLink = null
            }
            result.add(segment)
        }
    }
    if (currentLink != null) {
        result.add(currentLink)
    }
    return result
}
