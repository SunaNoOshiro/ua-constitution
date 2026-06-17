package ua.constitution.audio

import ua.constitution.R

enum class AnthemVersion(
    val titleRes: Int,
    val performerRes: Int,
    val type: String,
    val source: String
) {
    OFFICIAL(R.string.anthem_title, R.string.orchestra_desc, "local", "anthem")
}
