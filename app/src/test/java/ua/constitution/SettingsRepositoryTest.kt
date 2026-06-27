package ua.constitution

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ua.constitution.data.settings.SettingsRepository
import ua.constitution.data.settings.ThemeMode

/**
 * Pins SettingsRepository: defaults, font-scale clamping, and persistence across instances (a fresh
 * repo reads what a prior one wrote to SharedPreferences).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsRepositoryTest {

    private val prefs = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("settings_test", Context.MODE_PRIVATE)

    @Before
    fun clear() {
        prefs.edit().clear().commit()
    }

    @Test
    fun defaults_are_1x_font_and_system_theme() {
        val repo = SettingsRepository(prefs)
        assertEquals(SettingsRepository.DEFAULT_FONT_SCALE, repo.fontScale.value, 0.0001f)
        assertEquals(ThemeMode.SYSTEM, repo.themeMode.value)
    }

    @Test
    fun setFontScale_clamps_to_the_allowed_range_and_persists() {
        SettingsRepository(prefs).setFontScale(5.0f)
        assertEquals(SettingsRepository.MAX_FONT_SCALE, SettingsRepository(prefs).fontScale.value, 0.0001f)

        SettingsRepository(prefs).setFontScale(0.1f)
        assertEquals(SettingsRepository.MIN_FONT_SCALE, SettingsRepository(prefs).fontScale.value, 0.0001f)

        SettingsRepository(prefs).setFontScale(1.2f)
        assertEquals(1.2f, SettingsRepository(prefs).fontScale.value, 0.0001f)
    }

    @Test
    fun setThemeMode_persists() {
        SettingsRepository(prefs).setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, SettingsRepository(prefs).themeMode.value)

        SettingsRepository(prefs).setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, SettingsRepository(prefs).themeMode.value)
    }
}
