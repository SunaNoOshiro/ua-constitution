package ua.constitution.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide user preferences (reader font scale + [ThemeMode]), persisted in [SharedPreferences] and
 * exposed as [StateFlow]s so the UI recomposes reactively. The font scale is clamped to
 * [MIN_FONT_SCALE]..[MAX_FONT_SCALE] on both read and write; the theme mode defaults to
 * [ThemeMode.SYSTEM] (follow the device).
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

    private val _themeMode = MutableStateFlow(ThemeMode.fromName(prefs.getString(KEY_THEME_MODE, null)))
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setFontScale(scale: Float) {
        val clamped = scale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
        _fontScale.value = clamped
        prefs.edit().putFloat(KEY_FONT_SCALE, clamped).apply()
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    companion object {
        const val PREFS_NAME = "ua_constitution_settings"
        const val KEY_FONT_SCALE = "font_scale"
        const val KEY_THEME_MODE = "theme_mode"
        const val DEFAULT_FONT_SCALE = 1.0f
        const val MIN_FONT_SCALE = 0.85f
        const val MAX_FONT_SCALE = 1.5f
    }
}
