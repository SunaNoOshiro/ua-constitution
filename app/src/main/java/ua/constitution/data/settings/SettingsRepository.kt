package ua.constitution.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide user preferences (reader font scale + dark theme), persisted in [SharedPreferences] and
 * exposed as [StateFlow]s so the UI recomposes reactively. The font scale is clamped to
 * [MIN_FONT_SCALE]..[MAX_FONT_SCALE] on both read and write.
 *
 * Deliberately a plain class over SharedPreferences (no DataStore dependency for two scalar prefs);
 * constructed once in MainActivity (composition root) and read by the theme + the settings sheet.
 */
class SettingsRepository(private val prefs: SharedPreferences) {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    private val _fontScale = MutableStateFlow(
        prefs.getFloat(KEY_FONT_SCALE, DEFAULT_FONT_SCALE).coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
    )
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    private val _darkTheme = MutableStateFlow(prefs.getBoolean(KEY_DARK_THEME, DEFAULT_DARK_THEME))
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    fun setFontScale(scale: Float) {
        val clamped = scale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
        _fontScale.value = clamped
        prefs.edit().putFloat(KEY_FONT_SCALE, clamped).apply()
    }

    fun setDarkTheme(enabled: Boolean) {
        _darkTheme.value = enabled
        prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
    }

    companion object {
        const val PREFS_NAME = "ua_constitution_settings"
        const val KEY_FONT_SCALE = "font_scale"
        const val KEY_DARK_THEME = "dark_theme"
        const val DEFAULT_FONT_SCALE = 1.0f
        const val MIN_FONT_SCALE = 0.85f
        const val MAX_FONT_SCALE = 1.5f
        const val DEFAULT_DARK_THEME = false
    }
}
