package ua.constitution

/**
 * The interactive controls of the global formatting panel: the active highlight tool, the selected
 * colour and the panel's expansion, each with its change callback. Grouped into one parameter object
 * so GlobalFormattingPanel's signature stays focused on its remaining inputs (context + actions).
 */
data class FormattingPanelControls(
    val activeTool: String,
    val onActiveToolChange: (String) -> Unit,
    val selectedColorHex: String,
    val onColorHexChange: (String) -> Unit,
    val isPanelExpanded: Boolean,
    val onPanelExpandedChange: (Boolean) -> Unit,
)
