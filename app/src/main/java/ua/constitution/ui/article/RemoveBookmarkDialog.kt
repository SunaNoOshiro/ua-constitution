package ua.constitution.ui.article

import ua.constitution.R
import ua.constitution.ui.theme.*

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Confirmation dialog shown before removing a bookmark that has edits. Extracted verbatim from the
 * inline AlertDialog in [ArticleCard] (O1); the parent keeps the `if (show)` guard and owns the
 * state, so this composable is dumb: it only renders and reports the two actions.
 */
@Composable
fun RemoveBookmarkDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.dialog_remove_bookmark_confirm),
                    color = ErrorRedStrong,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.dialog_remove_bookmark_cancel),
                    color = SovereignBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.dialog_remove_bookmark_title),
                color = SovereignBlue,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_remove_bookmark_message),
                color = SovereignBlue.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        containerColor = AppCanvasYellow,
        tonalElevation = 6.dp,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    )
}
