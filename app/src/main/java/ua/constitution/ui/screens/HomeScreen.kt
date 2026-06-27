package ua.constitution.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import java.util.Calendar
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.constitution.R
import ua.constitution.data.model.Paragraph
import ua.constitution.data.model.Article
import ua.constitution.utils.Constants
import ua.constitution.ui.theme.*
import ua.constitution.ui.article.ArticleCard
import ua.constitution.ui.isBookmarked
import ua.constitution.ui.editsJsonFor
import ua.constitution.ui.viewmodel.ConstitutionViewModel
import ua.constitution.audio.computeWaveformBarStates
import ua.constitution.ui.formatMillisToMinutesSeconds
import ua.constitution.ui.model.FullscreenSymbol

/** Aspect ratio (width / height) of the Tryzub coat-of-arms vector, used to size it consistently. */
internal val TRYZUB_ASPECT_RATIO = 165f / 230.5f

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

/**
 * The Ukrainian state flag as two equal stripes. Vertical (default) is blue-over-gold; [horizontal]
 * is the 90°-rotated form, gold-left / blue-right. Shared by the Home national-symbols preview and
 * the fullscreen flag view, which were two separate inline Column/Row implementations.
 */
@Composable
fun UkrainianFlag(modifier: Modifier = Modifier, horizontal: Boolean = false) {
    if (horizontal) {
        Row(modifier = modifier) {
            Box(modifier = Modifier.fillMaxHeight().weight(1f).background(FlagGold))
            Box(modifier = Modifier.fillMaxHeight().weight(1f).background(FlagBlue))
        }
    } else {
        Column(modifier = modifier) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(FlagBlue))
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(FlagGold))
        }
    }
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
        val barStates = computeWaveformBarStates(heights.size, progress)
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
            
            val isPlayed = barStates[index]
            val barColor = if (isPlayed) SunflowerYellow else SunflowerYellow.copy(alpha = 0.35f)

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
            ?: Article(1, 1, "${context.getString(R.string.article_label)} 1", listOf(Paragraph(listOf(ua.constitution.data.model.ContentSegment(Constants.TYPE_TEXT, value = context.getString(R.string.article_1_fallback_content))), emptyList())))
    }
    val isTodayBookmarked = bookmarksList.isBookmarked(todayArticle)

    // The Hymn of Ukraine player lifecycle lives in its own holder (SRP).
    val anthem = rememberAnthemPlayerState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
        item {
            NationalSymbolsCard(onOpenFullscreenSymbol = onOpenFullscreenSymbol)
        }

        // Anthem block
        item {
            AnthemCard(
                isPlaying = anthem.isPlaying,
                isBuffering = anthem.isBuffering,
                playerPosition = anthem.position,
                playerDuration = anthem.duration,
                onTogglePlay = anthem.onTogglePlay,
                onSeek = anthem.onSeek
            )
        }

        // Article of the day block
        item {
            ArticleOfTheDayCard(
                article = todayArticle,
                isBookmarked = isTodayBookmarked,
                initialEditsJson = bookmarksList.editsJsonFor(todayArticle),
                onToggleBookmark = { viewModel.toggleBookmark(todayArticle.bookmarkId) },
                onArticleClick = { target -> onNavigateToArticle(target, todayArticle) },
                resolveArticleLink = viewModel::resolveLink,
                onOpen = { onNavigateToArticle(todayArticle, null) }
            )
        }
    }

        // Old InvisibleYouTubePlayer replaced by UnifiedAnthemPlayer at the outer Box
    }

}


@Composable
fun NationalSymbolsCard(onOpenFullscreenSymbol: (FullscreenSymbol) -> Unit) {
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.cardSurface),
        border = BorderStroke(2.dp, appColors.cardBorder)
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
                color = appColors.textHeading,
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
                UkrainianFlag(
                    modifier = Modifier
                        .size(width = 130.dp, height = 86.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenFullscreenSymbol(FullscreenSymbol.FLAG) }
                )

                Spacer(modifier = Modifier.width(28.dp))

                // 2. Coat of Arms (Tryzub) - Clean, high-fidelity shield logo matching the visual weight of the Flag
                UkrainianCoatOfArms(
                    modifier = Modifier
                        .height(96.dp)
                        .aspectRatio(TRYZUB_ASPECT_RATIO)
                        .clickable { onOpenFullscreenSymbol(FullscreenSymbol.COAT_OF_ARMS) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.flag_and_coat_desc),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = appColors.textHeading
            )
        }
    }
}

@Composable
fun AnthemCard(
    isPlaying: Boolean,
    isBuffering: Boolean,
    playerPosition: Int,
    playerDuration: Int,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SovereignBlue),
        border = BorderStroke(1.5.dp, SunflowerYellow)
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
                    tint = SunflowerYellow,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.national_anthem_header),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = SunflowerYellow
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
                    .background(DeepBlueVariant)
                    .border(1.dp, SunflowerYellow.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { onTogglePlay() },
                    modifier = Modifier
                        .size(34.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(SunflowerYellow)
                        .testTag("play_button_unified")
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            color = SovereignBlue,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) stringResource(R.string.btn_stop) else stringResource(R.string.btn_play),
                            tint = SovereignBlue,
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
                    text = if (playerDuration > 0) "${formatMillisToMinutesSeconds(playerPosition)} / ${formatMillisToMinutesSeconds(playerDuration)}" else "00:00 / 01:24",
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

@Composable
fun ArticleOfTheDayCard(
    article: Article,
    isBookmarked: Boolean,
    initialEditsJson: String,
    onToggleBookmark: () -> Unit,
    onArticleClick: (Article) -> Unit,
    resolveArticleLink: (String) -> Article?,
    onOpen: () -> Unit
) {
    val appColors = LocalAppColors.current
    Column {
        Text(
            text = stringResource(R.string.article_of_the_day),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = appColors.textHeading,
            modifier = Modifier.padding(vertical = 10.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .clickable { onOpen() }
        ) {
            ArticleCard(
                article = article,
                isBookmarked = isBookmarked,
                onToggleBookmark = onToggleBookmark,
                onArticleClick = onArticleClick,
                resolveArticleLink = resolveArticleLink,
                initialEditsJson = initialEditsJson
            )
        }
    }
}