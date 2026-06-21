package ua.constitution.domain.bookmark

import ua.constitution.domain.text.StyledRange

/**
 * Decides the per-card highlight edits to show when the incoming saved JSON or the editable flag
 * changes. Extracted verbatim from ArticleCard's reconciliation `LaunchedEffect`:
 *  - not editable        -> show nothing (empty map)
 *  - editable, and the incoming JSON is exactly what this card just saved -> keep the current local
 *    edits (return null = "no change"), so a save round-trip does not clobber in-progress edits
 *  - editable, otherwise -> parse and show the incoming JSON
 *
 * In every case the caller also clears its `lastSavedJson` marker. Pure (no Compose/Android), so the
 * branch is unit-testable; the previous inline form was not.
 */
fun reconcileEditsOnInput(
    initialEditsJson: String,
    isEditable: Boolean,
    lastSavedJson: String,
): Map<Int, List<StyledRange>>? {
    if (!isEditable) return emptyMap()
    if (lastSavedJson.isNotEmpty() && initialEditsJson == lastSavedJson) return null
    return BookmarkEditsParser.parse(initialEditsJson)
}
