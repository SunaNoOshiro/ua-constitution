package ua.constitution.ui.screens
import ua.constitution.ui.theme.HighlightPalette

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ua.constitution.utils.Constants

/**
 * Hoisted UI state for the Bookmarks-tab highlight editor (which article is being edited, the active
 * tool, the marker/underline colours and whether the tool panel is expanded). It is remembered in
 * the dashboard and passed into BookmarksTabContent so the editing state survives tab switches
 * exactly as it did when these were five separate `var`s in MainAppDashboard.
 */
class BookmarkEditorState {
    var editingArticleId by mutableStateOf<Int?>(null)
    var activeTool by mutableStateOf(Constants.TOOL_NONE)
    var markerColorHex by mutableStateOf(HighlightPalette.DEFAULT_MARKER)
    var underlineColorHex by mutableStateOf(HighlightPalette.DEFAULT_UNDERLINE)
    var panelExpanded by mutableStateOf(false)
}
