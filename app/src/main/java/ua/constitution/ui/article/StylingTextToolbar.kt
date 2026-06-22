package ua.constitution.ui.article

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import ua.constitution.utils.LogMessages

/** The copy / select-all callbacks the platform hands us when a selection toolbar is requested. */
class MenuCallbacks(
    val onCopy: (() -> Unit)?,
    val onSelectAll: (() -> Unit)?,
)

/**
 * A [TextToolbar] that suppresses the system selection menu and routes show/hide back into caller
 * state, so the reader can render its own [SelectionToolbarPopup] instead. [isShown] backs the
 * toolbar status, [onShowMenu] receives the anchor rect plus the platform copy/select-all callbacks
 * (wrapped in [MenuCallbacks]), and [onHide] clears the menu. Extracted verbatim from
 * SegmentedTextWithEdits; the menu state itself stays hoisted in the caller.
 */
@Composable
internal fun rememberStylingTextToolbar(
    isShown: () -> Boolean,
    onShowMenu: (Rect, MenuCallbacks) -> Unit,
    onHide: () -> Unit,
): TextToolbar = remember {
    object : TextToolbar {
        override val status: TextToolbarStatus
            get() = if (isShown()) TextToolbarStatus.Shown else TextToolbarStatus.Hidden

        override fun hide() {
            android.util.Log.d(LogMessages.TAG_SELECTION_BUG, LogMessages.TOOLBAR_HIDE_CALLED)
            onHide()
        }

        override fun showMenu(
            rect: Rect,
            onCopyRequested: (() -> Unit)?,
            onPasteRequested: (() -> Unit)?,
            onCutRequested: (() -> Unit)?,
            onSelectAllRequested: (() -> Unit)?,
        ) {
            android.util.Log.d(LogMessages.TAG_SELECTION_BUG, LogMessages.toolbarShowMenu(rect, rect.height, rect.width))
            onShowMenu(rect, MenuCallbacks(onCopyRequested, onSelectAllRequested))
        }
    }
}
