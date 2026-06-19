package ua.constitution

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The formatting half of the selection toolbar (marker / underline / eraser): its visibility,
 * colour palettes, last-used colours, eraser icon and the apply callbacks. Grouped into one
 * parameter object so SelectionToolbarPopup's signature stays small and the copy/select-all controls
 * read clearly alongside it.
 */
data class FormattingTools(
    val showFormatting: Boolean,
    val markerColors: List<String>,
    val underlineColors: List<String>,
    val lastMarkerColor: String,
    val lastUnderlineColor: String,
    val eraserIcon: ImageVector,
    val showEraser: Boolean,
    val onApplyMarker: (String) -> Unit,
    val onApplyUnderline: (String) -> Unit,
    val onApplyEraser: () -> Unit,
)
