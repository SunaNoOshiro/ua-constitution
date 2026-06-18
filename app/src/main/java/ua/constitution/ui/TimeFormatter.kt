package ua.constitution.ui

/**
 * Formats a millisecond duration as "MM:SS" (zero-padded). Minutes are NOT wrapped at 60 — a long
 * duration renders as e.g. "61:01" — preserving the exact behavior of the former inline formatTime
 * in HomeScreen's anthem player.
 */
fun formatMillisToMinutesSeconds(ms: Int): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%02d:%02d", mins, secs)
}
