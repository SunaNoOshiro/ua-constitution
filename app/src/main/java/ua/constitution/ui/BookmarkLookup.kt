package ua.constitution.ui

import ua.constitution.data.database.BookmarkEntity
import ua.constitution.data.model.Article

/** True if [article] is bookmarked in this list (matched by its derived bookmarkId). */
fun List<BookmarkEntity>.isBookmarked(article: Article): Boolean =
    any { it.articleId == article.bookmarkId }

/** The saved highlight-edits JSON for [article], or "" when it isn't bookmarked. */
fun List<BookmarkEntity>.editsJsonFor(article: Article): String =
    find { it.articleId == article.bookmarkId }?.editsJson ?: ""

/** The saved personal study note for [article], or "" when it isn't bookmarked. */
fun List<BookmarkEntity>.notesFor(article: Article): String =
    find { it.articleId == article.bookmarkId }?.notes ?: ""
