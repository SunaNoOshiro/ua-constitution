package ua.constitution

import android.content.Intent
import ua.constitution.utils.Constants
import ua.constitution.utils.LogMessages
import android.net.Uri
import android.os.Bundle
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import android.widget.Toast
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.horizontalScroll
import java.util.Calendar
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.constitution.data.model.Paragraph
import ua.constitution.data.model.Note
import ua.constitution.data.model.Link
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ua.constitution.data.model.Article
import ua.constitution.data.model.Chapter
import ua.constitution.data.repository.ConstitutionRepository
import ua.constitution.ui.theme.MyApplicationTheme
import ua.constitution.ui.viewmodel.ConstitutionViewModel
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import ua.constitution.audio.AnthemVersion
import ua.constitution.ui.model.DashboardTab
import ua.constitution.ui.model.FullscreenSymbol
import ua.constitution.domain.link.findArticleByLink
import ua.constitution.domain.title.parseArticleTitle
import ua.constitution.domain.bookmark.BookmarkEditsParser
import ua.constitution.domain.text.ArticleNumberFormatter
import ua.constitution.domain.text.StyledRange
import ua.constitution.domain.text.formatStringToSuperscript
import ua.constitution.domain.text.getWordRangeAtOffset
import ua.constitution.domain.text.getWordSnappedRange
import ua.constitution.domain.text.mapFormattedToOriginal
import ua.constitution.domain.text.mapOriginalToFormatted
import ua.constitution.domain.text.mergeAdjacentStyledRanges
import ua.constitution.ui.safeParseColor

@Composable
fun UkrainianCoatOfArms(
    modifier: Modifier = Modifier,
    useIsolated: Boolean = false
) {
    Image(
        painter = painterResource(id = if (useIsolated) R.drawable.ic_tryzub_isolated else R.drawable.ic_tryzub),
        contentDescription = stringResource(R.string.coat_of_arms_desc),
        modifier = modifier
    )
}

@Composable
fun AudioWaveformVisualizer(
    isPlaying: Boolean,
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val heights = listOf(
        0.3f, 0.5f, 0.7f, 0.4f, 0.8f, 0.6f, 0.9f, 0.5f, 
        0.7f, 1.0f, 0.4f, 0.6f, 0.8f, 0.5f, 0.9f, 0.3f,
        0.6f, 0.8f, 0.4f, 0.7f, 0.5f, 0.9f, 0.3f, 0.6f
    )
    
    var localWidth by remember { mutableStateOf(1) }

    Row(
        modifier = modifier
            .height(36.dp)
            .testTag("audio_waveform_visualizer")
            .onGloballyPositioned { coordinates ->
                localWidth = coordinates.size.width.coerceAtLeast(1)
            }
            .pointerInput(localWidth) {
                detectTapGestures { offset ->
                    val pct = (offset.x / localWidth).coerceIn(0f, 1f)
                    onSeek(pct)
                }
            }
            .pointerInput(localWidth) {
                detectDragGestures { change, _ ->
                    val pct = (change.position.x / localWidth).coerceIn(0f, 1f)
                    onSeek(pct)
                }
            },
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEachIndexed { index, baseHeight ->
            val animHeight by if (isPlaying) {
                infiniteTransition.animateFloat(
                    initialValue = baseHeight * 0.2f,
                    targetValue = baseHeight,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 350 + index * 45, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bar_$index"
                )
            } else {
                remember { mutableStateOf(baseHeight * 0.15f) }
            }
            
            val isPlayed = (index.toFloat() / heights.size) <= progress
            val barColor = if (isPlayed) Color(0xFFFFD500) else Color(0xFFFFD500).copy(alpha = 0.35f)

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(animHeight)
                    .background(barColor, shape = RoundedCornerShape(1.5.dp))
            )
        }
    }
}

@Composable
fun AutoScaleText(
    text: String,
    maxTextSize: Float = 17f,
    minTextSize: Float = 10f,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = Color.White,
    textAlign: TextAlign = TextAlign.Center,
    modifier: Modifier = Modifier
) {
    var textSize by remember(text) { mutableStateOf(maxTextSize) }
    Text(
        text = text,
        fontSize = textSize.sp,
        fontWeight = fontWeight,
        color = color,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Visible,
        modifier = modifier,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow && textSize > minTextSize) {
                textSize -= 0.5f
            }
        }
    )
}

// Stateful official State Symbols & Article of the Day home screen

@Composable
fun HomeTabContent(
    viewModel: ConstitutionViewModel,
    bookmarksList: List<ua.constitution.data.database.BookmarkEntity>,
    onNavigateToArticle: (Article, Article?) -> Unit,
    onOpenFullscreenSymbol: (FullscreenSymbol) -> Unit
) {
    val context = LocalContext.current
    
    // Choose the Article of the Day deterministically based on date seed with safe fallback
    val todayArticle = remember {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        viewModel.articleOfDay(dayOfYear)
            ?: Article(1, 1, "${context.getString(R.string.article_label)} 1", listOf(Paragraph(listOf(ua.constitution.data.model.ContentSegment("text", value = context.getString(R.string.article_1_fallback_content))), emptyList())))
    }
    val isTodayBookmarked = bookmarksList.any { it.articleId == todayArticle.bookmarkId }

    // Unified local player state for the Hymn of Ukraine
    val selectedVersion = AnthemVersion.OFFICIAL
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var nativeMediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playerPosition by remember { mutableStateOf(0) }
    var playerDuration by remember { mutableStateOf(0) }

    fun formatTime(ms: Int): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%02d:%02d", mins, secs)
    }

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

    // Toggle Play function
    fun togglePlayAnthem() {
        isPlaying = !isPlaying
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.dp, Color(0xFF0D47A1))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.national_symbols_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = Color(0xFF0D47A1),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // 1. National Flag
                        Column(
                            modifier = Modifier
                                .size(width = 130.dp, height = 86.dp)
                                .shadow(4.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenFullscreenSymbol(FullscreenSymbol.FLAG) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(Color(0xFF0057B7)) // Sovereign blue
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(Color(0xFFFFD700)) // Golden yellow
                            )
                        }

                        Spacer(modifier = Modifier.width(28.dp))

                        // 2. Coat of Arms (Tryzub) - Clean, high-fidelity shield logo matching the visual weight of the Flag
                        UkrainianCoatOfArms(
                            modifier = Modifier
                                .height(96.dp)
                                .aspectRatio(165f / 230.5f)
                                .clickable { onOpenFullscreenSymbol(FullscreenSymbol.COAT_OF_ARMS) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(R.string.flag_and_coat_desc),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0D47A1)
                    )
                }
            }
        }

        // Anthem block
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D47A1)),
                border = BorderStroke(1.5.dp, Color(0xFFFFD500))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFFFFD500),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.national_anthem_header),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = Color(0xFFFFD500)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val lines = listOf(
                        stringResource(R.string.anthem_line_1),
                        stringResource(R.string.anthem_line_2),
                        stringResource(R.string.anthem_line_3),
                        stringResource(R.string.anthem_line_4),
                        "",
                        stringResource(R.string.anthem_line_5),
                        stringResource(R.string.anthem_line_6)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        lines.forEach { line ->
                            if (line.isEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                            } else {
                                AutoScaleText(
                                    text = line,
                                    maxTextSize = 13.5f,
                                    minTextSize = 9.5f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.orchestra_recording_info),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Slimmed down, centered high-fidelity player capsule
                    Row(
                        modifier = Modifier
                            .widthIn(max = 290.dp)
                            .fillMaxWidth(0.9f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF092C66))
                            .border(1.dp, Color(0xFFFFD500).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { togglePlayAnthem() },
                            modifier = Modifier
                                .size(34.dp)
                                .shadow(2.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color(0xFFFFD500))
                                .testTag("play_button_unified")
                        ) {
                            if (isBuffering) {
                                CircularProgressIndicator(
                                    color = Color(0xFF0D47A1),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) stringResource(R.string.btn_stop) else stringResource(R.string.btn_play),
                                    tint = Color(0xFF0D47A1),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        val progress = if (playerDuration > 0) playerPosition.toFloat() / playerDuration else 0f

                        AudioWaveformVisualizer(
                            isPlaying = isPlaying,
                            progress = progress,
                            onSeek = onSeek,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = if (playerDuration > 0) "${formatTime(playerPosition)} / ${formatTime(playerDuration)}" else "00:00 / 01:24",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            color = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
        }

        // Article of the day block
        item {
            Column {
                Text(
                    text = stringResource(R.string.article_of_the_day),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0D47A1),
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToArticle(todayArticle, null) }
                ) {
                    ArticleCard(
                        article = todayArticle,
                        isBookmarked = isTodayBookmarked,
                        onToggleBookmark = { viewModel.toggleBookmark(todayArticle.bookmarkId) },
                        onArticleClick = { target -> onNavigateToArticle(target, todayArticle) },
                        resolveArticleLink = viewModel::resolveLink,
                        initialEditsJson = bookmarksList.find { it.articleId == todayArticle.bookmarkId }?.editsJson ?: ""
                    )
                }
            }
        }
    }

        // Old InvisibleYouTubePlayer replaced by UnifiedAnthemPlayer at the outer Box
    }

}
