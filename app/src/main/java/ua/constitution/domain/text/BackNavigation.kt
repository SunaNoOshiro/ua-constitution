package ua.constitution.domain.text

/**
 * Which "back to ..." message applies when navigating back to an article, decided purely from its
 * chapter id. Extracted from the getBackNavigationText composable so the branch is unit-testable;
 * the UI maps each case to its localized string resource (and arguments).
 */
enum class BackNavigationTarget { CHAPTER_15, PREAMBLE, OTHER }

/** Mirrors the former inline `if (chapterId == 15) ... else if (chapterId == 0) ... else ...`. */
fun backNavigationTarget(chapterId: Int): BackNavigationTarget = when (chapterId) {
    15 -> BackNavigationTarget.CHAPTER_15
    0 -> BackNavigationTarget.PREAMBLE
    else -> BackNavigationTarget.OTHER
}
