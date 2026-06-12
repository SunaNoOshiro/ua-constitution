package com.example.utils

object LogMessages {
    // Log TAGs
    const val TAG_CONSTITUTION_DATA = "ConstitutionData"
    const val TAG_ANTHEM_PLAYER = "AnthemPlayer"
    const val TAG_SELECTION_BUG = "SelectionBug"
    const val TAG_SEGMENTED_TEXT_EDITS = "SegmentedTextWithEdits"
    const val TAG_SEGMENTED_TEXT = "SegmentedText"

    // Log Messages for ConstitutionData
    fun integrityVerified(hash: String): String = "Cryptographic integrity verified! SHA-256: $hash"
    
    fun integrityMismatch(calculated: String, expected: String): String = 
        "WARNING: Cryptographic integrity mismatch! Calculated: $calculated, Expected: $expected. Loading anyway for resilience."
    
    const val INTEGRITY_COMPUTE_FAILED = "Failed to compute asset SHA-256"
    const val LOADED_FROM_RAW = "Loaded successfully from RAW resources."
    const val LOADED_FROM_ASSETS = "Loaded successfully from assets."
    
    fun loadFromRawFailed(errorMessage: String?): String = 
        "Fail reading from RAW resources, falling back to assets. Error: ${errorMessage ?: "unknown"}"
        
    fun initSuccess(articleCount: Int): String = 
        "Successfully initialized high-fidelity articles from nested JSON assets. Total articles: $articleCount"
        
    const val INIT_ERROR = "Error loading constitution_ua.json from assets."

    // Log Messages for AnthemPlayer
    const val PLAYER_RAW_CREATE_FAILED = "Failed to create MediaPlayer from raw resource"
    const val PLAYER_CREATE_ERROR = "Error creating MediaPlayer from raw resource"

    // Log Messages for Toolbar / Selection Bug / Custom Text Selection Menu
    const val TOOLBAR_HIDE_CALLED = "hide() called for customTextToolbar"
    
    fun toolbarShowMenu(rect: Any?, height: Float, width: Float): String = 
        "showMenu() called with rect: $rect, height=$height, width=$width"
        
    fun toolbarValueChange(newValueSelection: Any?, newValueSelectionCollapsed: Boolean): String = 
        "onValueChange: selection=$newValueSelection, selectionCollapsed=$newValueSelectionCollapsed"

    // Log Messages for Url handling
    fun openUrlFailed(url: String, errorMessage: String?): String = 
        "Failed to open url: $url. Error: ${errorMessage ?: "unknown"}"

    // Log Messages for Popup / Dismiss preserving selection
    const val POPUP_DISMISS_PRESERVE_SELECTION = "Popup onDismissRequest called - preserving selection so handles remain interactive!"
    const val PARAGRAPH_POPUP_DISMISS_PRESERVE_SELECTION = "Popup onDismissRequest called - preserving selection in InteractiveParagraphText!"
}
