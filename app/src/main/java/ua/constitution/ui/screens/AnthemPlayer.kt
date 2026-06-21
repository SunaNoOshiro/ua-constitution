package ua.constitution

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import ua.constitution.utils.LogMessages

/**
 * Read-only snapshot of the anthem player plus its controls, handed to AnthemCard. A plain value
 * object (rebuilt each recomposition from the live state); the lifecycle lives in
 * [rememberAnthemPlayerState].
 */
@Stable
class AnthemPlayerState(
    val isPlaying: Boolean,
    val isBuffering: Boolean,
    val position: Int,
    val duration: Int,
    val onTogglePlay: () -> Unit,
    val onSeek: (Float) -> Unit,
)

/**
 * Owns the raw-asset MediaPlayer lifecycle for the national anthem: create/start/pause on the
 * play toggle, position polling while playing, seek, and release on dispose. Extracted verbatim
 * from HomeTabContent so that composable is responsible only for layout (SRP) and no longer depends
 * directly on android.media.MediaPlayer. Behavior — including the swallowed exceptions and the
 * completion-resets-to-zero handler — is preserved exactly.
 */
@Composable
fun rememberAnthemPlayerState(): AnthemPlayerState {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var nativeMediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playerPosition by remember { mutableStateOf(0) }
    var playerDuration by remember { mutableStateOf(0) }

    LaunchedEffect(isPlaying, nativeMediaPlayer) {
        if (isPlaying && nativeMediaPlayer != null) {
            while (isPlaying) {
                try {
                    nativeMediaPlayer?.let { mp ->
                        if (mp.isPlaying) {
                            playerPosition = mp.currentPosition
                            playerDuration = mp.duration
                        }
                    }
                } catch (e: Exception) {}
                kotlinx.coroutines.delay(200)
            }
        }
    }

    val onSeek: (Float) -> Unit = { pct ->
        nativeMediaPlayer?.let { mp ->
            try {
                val targetMs = (pct * mp.duration).toInt()
                mp.seekTo(targetMs)
                playerPosition = targetMs
            } catch (e: Exception) {}
        }
    }

    // Control Local MediaPlayer reactively for raw audio asset playback
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            if (nativeMediaPlayer == null) {
                isBuffering = true
                try {
                    val mp = MediaPlayer.create(context, R.raw.anthem).apply {
                        setOnCompletionListener {
                            isPlaying = false
                            playerPosition = 0
                        }
                    }
                    if (mp != null) {
                        nativeMediaPlayer = mp
                        playerDuration = mp.duration
                        playerPosition = mp.currentPosition
                        mp.start()
                    } else {
                        android.util.Log.e(LogMessages.TAG_ANTHEM_PLAYER, LogMessages.PLAYER_RAW_CREATE_FAILED)
                        isPlaying = false
                    }
                } catch (e: Exception) {
                    android.util.Log.e(LogMessages.TAG_ANTHEM_PLAYER, LogMessages.PLAYER_CREATE_ERROR, e)
                    isPlaying = false
                } finally {
                    isBuffering = false
                }
            } else {
                try {
                    nativeMediaPlayer?.start()
                } catch (e: Exception) {
                    isPlaying = false
                }
            }
        } else {
            try {
                if (nativeMediaPlayer?.isPlaying == true) {
                    nativeMediaPlayer?.pause()
                }
            } catch (e: Exception) {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            nativeMediaPlayer?.let {
                try {
                    if (it.isPlaying) it.stop()
                } catch (e: Exception) {}
                try {
                    it.release()
                } catch (e: Exception) {}
            }
            nativeMediaPlayer = null
        }
    }

    return AnthemPlayerState(
        isPlaying = isPlaying,
        isBuffering = isBuffering,
        position = playerPosition,
        duration = playerDuration,
        onTogglePlay = { isPlaying = !isPlaying },
        onSeek = onSeek,
    )
}
