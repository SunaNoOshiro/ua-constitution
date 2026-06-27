package ua.constitution.data.settings

/**
 * The user's theme preference. [SYSTEM] (the default) follows the device's light/dark setting;
 * [LIGHT] and [DARK] force a fixed theme regardless of the system. Persisted by name via
 * [SettingsRepository]; an unknown/absent stored value resolves back to [SYSTEM].
 */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun fromName(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
