package ua.constitution
import ua.constitution.ui.theme.HighlightPalette

import ua.constitution.domain.text.StyledRange
import ua.constitution.utils.Constants

/**
 * The highlight-editing inputs for SegmentedTextWithEdits, grouped into one parameter object
 * (Introduce Parameter Object). Defaults reproduce the previous per-parameter defaults: a read-only
 * render with no edit callbacks. The editable Bookmarks path supplies a wired instance; the
 * read-only article path relies mostly on the defaults.
 */
data class SegmentEditing(
    val onUpdateRanges: ((List<StyledRange>) -> Unit)? = null,
    val selectedMarkerColorHex: String = HighlightPalette.DEFAULT_MARKER,
    val selectedUnderlineColorHex: String = HighlightPalette.DEFAULT_UNDERLINE,
    val onSelectedMarkerColorChange: ((String) -> Unit)? = null,
    val onSelectedUnderlineColorChange: ((String) -> Unit)? = null,
    val activeTool: String = Constants.TOOL_MARKER,
    val selectedColorHex: String = HighlightPalette.DEFAULT_MARKER,
    val fullArticleTextToCopy: String? = null,
)
