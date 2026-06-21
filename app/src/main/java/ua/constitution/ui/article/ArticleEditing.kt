package ua.constitution
import ua.constitution.ui.theme.HighlightPalette

import ua.constitution.utils.Constants

/**
 * All inputs ArticleCard needs for its in-card highlight editor, grouped into one parameter object
 * (Introduce Parameter Object) so the composable's signature stays small and cohesive. Defaults
 * reproduce a plain, non-editable card — the search/articles/home cards omit it entirely; only the
 * Bookmarks tab supplies a fully-wired instance.
 */
data class ArticleEditing(
    val isEditable: Boolean = false,
    val isCurrentlyEditing: Boolean = false,
    val isEditButtonEnabled: Boolean = true,
    val isPanelExpanded: Boolean = false,
    val activeTool: String = Constants.TOOL_MARKER,
    val selectedColorHex: String = HighlightPalette.DEFAULT_MARKER,
    val selectedMarkerColorHex: String = HighlightPalette.DEFAULT_MARKER,
    val selectedUnderlineColorHex: String = HighlightPalette.DEFAULT_UNDERLINE,
    val onSaveEdits: ((String) -> Unit)? = null,
    val onToggleEditing: (() -> Unit)? = null,
    val onPanelExpandedChange: ((Boolean) -> Unit)? = null,
    val onActiveToolChange: ((String) -> Unit)? = null,
    val onColorHexChange: ((String) -> Unit)? = null,
    val onDisabledEditClick: (() -> Unit)? = null,
)
