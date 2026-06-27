package ua.constitution.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.constitution.R
import ua.constitution.data.settings.SettingsRepository
import ua.constitution.ui.theme.LocalAppColors

/**
 * Bottom-sheet of app preferences: a reader font-size slider and a dark-theme toggle, each with a
 * leading icon. State mutation is delegated to the callbacks; the sheet is a dumb, theme-aware view
 * (its text/icons use [LocalAppColors] so they read correctly in both light and dark mode).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    fontScale: Float,
    darkTheme: Boolean,
    onFontScaleChange: (Float) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SettingRowLabel(Icons.Default.DarkMode, stringResource(R.string.settings_dark_mode), colors.textPrimary)
                Switch(
                    checked = darkTheme,
                    onCheckedChange = onDarkThemeChange,
                    modifier = Modifier.testTag("settings_dark_switch"),
                )
            }
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
