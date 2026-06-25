package ua.constitution.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.constitution.R
import ua.constitution.data.settings.SettingsRepository
import ua.constitution.ui.theme.RichNavyText
import ua.constitution.ui.theme.SovereignBlue

/**
 * Bottom-sheet of app preferences. Currently the reader font-size control: a slider over
 * [SettingsRepository.MIN_FONT_SCALE]..[SettingsRepository.MAX_FONT_SCALE] with a live preview of the
 * reading text at the chosen scale. State mutation is delegated to [onFontScaleChange]; the sheet is
 * a dumb view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
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
                color = SovereignBlue,
            )
            Text(
                text = stringResource(R.string.settings_font_size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SovereignBlue,
            )
            // Live preview of the reading text at the chosen scale (base reader size is 16sp).
            Text(
                text = stringResource(R.string.settings_font_preview),
                fontSize = (16 * fontScale).sp,
                color = RichNavyText,
            )
            Slider(
                value = fontScale,
                onValueChange = onFontScaleChange,
                valueRange = SettingsRepository.MIN_FONT_SCALE..SettingsRepository.MAX_FONT_SCALE,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_font_slider"),
            )
        }
    }
}
