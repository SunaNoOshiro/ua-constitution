package ua.constitution.domain.text

/**
 * Builds the plain-text form of an article for the clipboard "copy" action: the article number
 * (optionally followed by ". " + name), a blank line, then each paragraph's superscript-formatted
 * text separated by blank lines. Extracted verbatim from the two identical buildString blocks in
 * ArticleCard (the copy button and the fullArticleTextToCopy memo) to remove the duplication.
 */
fun formatArticleForCopy(articleNumber: String, articleName: String, paragraphTexts: List<String>): String =
    buildString {
        append(articleNumber)
        if (articleName.isNotEmpty()) {
            append(". ")
            append(articleName)
        }
        append("\n\n")
        paragraphTexts.forEachIndexed { index, text ->
            append(formatStringToSuperscript(text))
            if (index < paragraphTexts.lastIndex) {
                append("\n\n")
            }
        }
    }
