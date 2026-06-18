package ua.constitution

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.dialog_remove_bookmark_cancel),
                    color = Color(0xFF0D47A1),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.dialog_remove_bookmark_title),
                color = Color(0xFF0D47A1),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_remove_bookmark_message),
                color = Color(0xFF0D47A1).copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        containerColor = Color(0xFFFFFDE7),
        tonalElevation = 6.dp,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    )
}
