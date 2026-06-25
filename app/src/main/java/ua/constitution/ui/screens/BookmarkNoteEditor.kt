package ua.constitution.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ua.constitution.R

/**
 * A personal study-note editor for a bookmarked article: an outlined text field pre-filled with the
 * [initialNote], and a Save action enabled only while the (trimmed) draft differs from what's saved.
 *
 * The draft is keyed on [initialNote] (remember(initialNote)) so it resets when the saved note
 * actually changes — after a save, or when a different bookmark's note is shown — but is preserved
 * across unrelated recompositions (e.g. while the bookmarks Flow re-emits for a highlight edit).
 * Persisting is delegated to [onSaveNote] (ConstitutionViewModel.updateNotes -> Room).
 */
@Composable
fun BookmarkNoteEditor(
    initialNote: String,
    onSaveNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember(initialNote) { mutableStateOf(initialNote) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            label = { Text(stringResource(R.string.note_label)) },
            placeholder = { Text(stringResource(R.string.note_hint)) },
            minLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bookmark_note_field"),
        )
        TextButton(
            onClick = { onSaveNote(draft.trim()) },
            enabled = draft.trim() != initialNote,
            modifier = Modifier
                .wrapContentWidth(Alignment.End)
                .testTag("bookmark_note_save"),
        ) {
            Text(stringResource(R.string.note_save))
        }
    }
}
