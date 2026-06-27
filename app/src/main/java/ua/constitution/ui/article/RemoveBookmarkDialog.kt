package ua.constitution.ui.article

import ua.constitution.R
import ua.constitution.ui.theme.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Confirmation dialog shown before removing a bookmark that has edits. Originally an inline
 * [AlertDialog] (extracted at O1); redesigned to share the app's card vocabulary — a [Surface] with
 * the same rounded corners, 2dp [LocalAppColors.cardBorder] outline, shadow and Black-weight heading
 * as [ArticleCard] — so it reads as the same app in both light and dark mode. The parent keeps the
 * `if (show)` guard and owns the state; this composable only renders and reports the two actions.
 */
@Composable
fun RemoveBookmarkDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val appColors = LocalAppColors.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Surface(
            modifier = Modifier.shadow(8.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = appColors.cardSurface,
            border = BorderStroke(2.dp, appColors.cardBorder),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.dialog_remove_bookmark_title),
                    color = appColors.textHeading,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge,
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.dialog_remove_bookmark_message),
                    color = appColors.textPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                )

                Spacer(Modifier.height(24.dp))

                RemoveBookmarkActions(onConfirm = onConfirm, onDismiss = onDismiss)
            }
        }
    }
}

/**
 * Cancel (outlined, echoes the card border) + destructive confirm (filled danger). Laid out by
 * [AdaptiveButtonRow]: side-by-side equal halves when both labels fit, and only stacked full-width
 * when a large reader font would otherwise clip the single-word Ukrainian labels.
 */
@Composable
private fun RemoveBookmarkActions(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val appColors = LocalAppColors.current
    AdaptiveButtonRow(spacing = 12.dp, modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.heightIn(min = 48.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, appColors.textHeading),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = appColors.textHeading),
        ) {
            Text(
                text = stringResource(R.string.dialog_remove_bookmark_cancel),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        Button(
            onClick = onConfirm,
            modifier = Modifier.heightIn(min = 48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = appColors.danger,
                contentColor = Color.White,
            ),
        ) {
            Text(
                text = stringResource(R.string.dialog_remove_bookmark_confirm),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/**
 * Lays out exactly two buttons either as equal-width halves of one row, or — when those halves would
 * be narrower than the widest button's natural (single-line) width — stacked full-width. The decision
 * is driven by the children's intrinsic width, so it tracks the current reader font scale for free.
 */
@Composable
private fun AdaptiveButtonRow(
    spacing: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val maxW = constraints.maxWidth
        val n = measurables.size
        val widest = measurables.maxOf { it.maxIntrinsicWidth(constraints.maxHeight) }
        val stacked = n * widest + gap * (n - 1) > maxW
        val span = if (stacked) maxW else (maxW - gap * (n - 1)) / n
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = span, maxWidth = span)) }
        if (stacked) {
            layout(maxW, placeables.sumOf { it.height } + gap * (n - 1)) {
                var y = 0
                placeables.forEach { p -> p.placeRelative(0, y); y += p.height + gap }
            }
        } else {
            layout(maxW, placeables.maxOf { it.height }) {
                var x = 0
                placeables.forEach { p -> p.placeRelative(x, 0); x += p.width + gap }
            }
        }
    }
}
