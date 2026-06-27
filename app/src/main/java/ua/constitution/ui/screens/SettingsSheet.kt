package ua.constitution.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ua.constitution.R
import ua.constitution.data.settings.SettingsRepository
import ua.constitution.data.settings.ThemeMode
import ua.constitution.ui.theme.AppColors
import ua.constitution.ui.theme.LocalAppColors

/**
 * Bottom-sheet of app preferences: a reader font-size slider and a theme-mode selector (Auto / Light
 * / Dark; Auto follows the device). State mutation is delegated to the callbacks; the sheet is a
 * dumb, theme-aware view (its text/icons use [LocalAppColors] so they read correctly in both modes).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    fontScale: Float,
    themeMode: ThemeMode,
    onFontScaleChange: (Float) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary,
            )

            SettingRowLabel(Icons.Default.FormatSize, stringResource(R.string.settings_font_size), colors.textPrimary)
            Slider(
                value = fontScale,
                onValueChange = onFontScaleChange,
                valueRange = SettingsRepository.MIN_FONT_SCALE..SettingsRepository.MAX_FONT_SCALE,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_font_slider"),
            )

            SettingRowLabel(Icons.Default.Contrast, stringResource(R.string.settings_theme), colors.textPrimary)
            ThemeModeSelector(selected = themeMode, onSelect = onThemeModeChange, colors = colors)
        }
    }
}

/** An icon + bold label, used as the leading content of each settings row. */
@Composable
private fun SettingRowLabel(icon: ImageVector, label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

/**
 * A bordered three-segment toggle (Auto / Light / Dark) themed via [AppColors], so it stays on-brand
 * in both light and dark mode. The selected segment gets a soft heading-tinted fill; the others are
 * transparent. Built custom (rather than [androidx.compose.material3.SegmentedButton]) to control the
 * palette and guarantee equal-width segments.
 */
@Composable
private fun ThemeModeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    colors: AppColors,
) {
    val options = listOf(
        ThemeMode.SYSTEM to stringResource(R.string.settings_theme_auto),
        ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
        ThemeMode.DARK to stringResource(R.string.settings_theme_dark),
    )
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.5.dp, colors.cardBorder, shape)
            .testTag("settings_theme_selector"),
    ) {
        options.forEach { (mode, label) ->
            val isActive = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(mode) }
                    .background(if (isActive) colors.textHeading.copy(alpha = 0.16f) else Color.Transparent)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (isActive) colors.textHeading else colors.textSecondary,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}
