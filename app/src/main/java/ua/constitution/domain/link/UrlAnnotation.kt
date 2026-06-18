package ua.constitution.domain.link

import ua.constitution.data.model.Article

/**
 * Pure helpers for handling a tap on a "url|text" link annotation in the reader. Extracted verbatim
 * from the three identical inline tap handlers in InteractiveText (SegmentedTextWithEdits' text field
 * and tap-gesture paths, and SegmentedText's ClickableText). The Android Intent launch and logging
 * stay at the call sites; everything here is Android-free and unit-testable.
 */

/** Splits a "url|text" annotation payload into (url, text), defaulting missing parts to "". */
fun parseUrlAnnotation(item: String): Pair<String, String> {
    val parts = item.split("|")
    return (parts.getOrNull(0) ?: "") to (parts.getOrNull(1) ?: "")
}

/** True when a link should be treated as an internal article cross-reference (an anchor "#..." or
 *  any non-http(s) URL) rather than an external web link. */
fun isInternalArticleLink(url: String): Boolean =
    url.startsWith("#") || (!url.startsWith("http://") && !url.startsWith("https://"))

/** The browser URL to open for [clickedUrl]: anchors are resolved against [baseUrl]; otherwise the
 *  URL is used as-is. */
fun resolveExternalUrl(clickedUrl: String, baseUrl: String): String =
    if (clickedUrl.startsWith("#")) "$baseUrl$clickedUrl" else clickedUrl

/**
 * Handles a tap on a "url|text" annotation: for an internal link it resolves the target article and
 * invokes [onArticleClick]; otherwise (or if resolution fails) it asks [openExternalUrl] to open the
 * base-resolved web URL (when non-empty). Behavior mirrors the former inline handlers verbatim; the
 * caller supplies [openExternalUrl] so the Android Intent launch stays out of this pure function.
 */
fun handleLinkAnnotationTap(
    annotationItem: String,
    baseUrl: String,
    resolveArticleLink: ((String) -> Article?)?,
    onArticleClick: ((Article) -> Unit)?,
    openExternalUrl: (String) -> Unit,
) {
    val (clickedUrl, segmentText) = parseUrlAnnotation(annotationItem)

    var articleNavigated = false
    if (isInternalArticleLink(clickedUrl)) {
        val targetArticle = resolveArticleLink?.invoke(segmentText)
        if (targetArticle != null && onArticleClick != null) {
            onArticleClick(targetArticle)
            articleNavigated = true
        }
    }

    if (!articleNavigated) {
        val finalUrl = resolveExternalUrl(clickedUrl, baseUrl)
        if (finalUrl.isNotEmpty()) {
            openExternalUrl(finalUrl)
        }
    }
}
