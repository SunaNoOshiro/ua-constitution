package ua.constitution.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.constitution.R
import ua.constitution.data.database.BookmarkEntity
import ua.constitution.data.model.Article
import ua.constitution.ui.article.ArticleCard
import ua.constitution.ui.editsJsonFor
import ua.constitution.ui.isBookmarked
import ua.constitution.ui.model.FullscreenSymbol
import ua.constitution.ui.theme.*
import ua.constitution.ui.viewmodel.ConstitutionViewModel

/**
 * The circular emblem badge (radial-gradient disc + golden border + isolated tryzub) used by the
 * dashboard header and the center nav button. Parameterised over the few things that differ.
 */
@Composable
fun CoatOfArmsBadge(badgeSize: Dp, shadowElevation: Dp, gradientColors: List<Color>, coatSize: Dp) {
    Box(
        modifier = Modifier
            .size(badgeSize)
            .shadow(shadowElevation, CircleShape)
            .clip(CircleShape)
            .background(Brush.radialGradient(gradientColors))
            .border(1.5.dp, SunflowerYellow, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        UkrainianCoatOfArms(
            useIsolated = true,
            modifier = Modifier
                .size(coatSize)
                .padding(1.dp)
        )
    }
}

/**
 * The full-screen national-symbol viewer (flag or coat of arms). Owns its own view state — the
 * tap-to-rotate-then-dismiss gesture and the auto-hiding instruction badge — so the dashboard only
 * decides WHICH symbol (or none) is shown. Extracted verbatim from MainAppDashboard.
 */
@Composable
fun FullscreenSymbolOverlay(symbol: FullscreenSymbol, onDismiss: () -> Unit) {
    var isRotated by remember(symbol) { mutableStateOf(false) }
    var showBadge by remember { mutableStateOf(true) }
    LaunchedEffect(symbol) {
        showBadge = true
        kotlinx.coroutines.delay(2000L)
        showBadge = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(symbol.overlayBackground)
            .clickable {
                if (!isRotated) isRotated = true else onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        if (symbol == FullscreenSymbol.FLAG) {
            // Rotated 90° shows the gold-left / blue-right form.
            UkrainianFlag(modifier = Modifier.fillMaxSize(), horizontal = isRotated)
        } else if (symbol == FullscreenSymbol.COAT_OF_ARMS) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val tryzubRatio = TRYZUB_ASPECT_RATIO
                val sizeFraction = if (isRotated) {
                    val targetHeight = maxWidth * 0.85f
                    minOf(targetHeight, maxHeight * 0.85f / tryzubRatio)
                } else {
                    val targetHeight = maxHeight * 0.7f
                    val targetWidth = maxWidth * 0.85f
                    minOf(targetHeight, targetWidth / tryzubRatio)
                }

                Box(
                    modifier = Modifier
                        .size(sizeFraction)
                        .graphicsLayer {
                            rotationZ = if (isRotated) 90f else 0f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    UkrainianCoatOfArms(
                        modifier = Modifier
                            .size(width = sizeFraction * tryzubRatio, height = sizeFraction)
                    )
                }
            }
        }

        // Floating instruction badge at the bottom
        if (showBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 50.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(R.string.symbol_view_hint),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * The floating editing-warning toast: shows [message], auto-dismisses after 4s or on the close
 * button (both via [onDismiss]). Extracted verbatim from MainAppDashboard; the auto-dismiss is keyed
 * on [message], so a newer message cancels the previous timer (no stale dismiss).
 */
@Composable
fun EditingWarningToast(message: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, bottom = 96.dp), // floats above bottom navigation bar
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SlateDark.copy(alpha = 0.95f),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, SlateText),
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = SunflowerYellow,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = message,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close_text),
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
    LaunchedEffect(message) {
        kotlinx.coroutines.delay(4000L)
        onDismiss()
    }
}

/**
 * The in-place search results list (or the prompt/no-results message). Extracted verbatim from the
 * `isSearchActive` branch of MainAppDashboard.
 */
@Composable
fun SearchResultsContent(
    viewModel: ConstitutionViewModel,
    filteredArticles: List<Article>,
    searchQuery: String,
    bookmarksList: List<BookmarkEntity>,
    onNavigateToArticle: (Article, Article?) -> Unit,
) {
    val appColors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (filteredArticles.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) stringResource(R.string.search_prompt_input) else stringResource(R.string.search_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = appColors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredArticles) { article ->
                    ArticleCard(
                        article = article,
                        isBookmarked = bookmarksList.isBookmarked(article),
                        initialEditsJson = bookmarksList.editsJsonFor(article),
                        onToggleBookmark = { viewModel.toggleBookmark(article.bookmarkId) },
                        onArticleClick = { target -> onNavigateToArticle(target, article) },
                        resolveArticleLink = viewModel::resolveLink
                    )
                }
            }
        }
    }
}
