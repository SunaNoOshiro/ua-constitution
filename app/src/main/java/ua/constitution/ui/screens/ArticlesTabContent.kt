package ua.constitution

import ua.constitution.ui.theme.*

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.constitution.data.database.BookmarkEntity
import ua.constitution.data.model.Article
import ua.constitution.domain.text.ArticleNumberFormatter
import ua.constitution.ui.viewmodel.ConstitutionViewModel

/**
 * The ARTICLES tab: the chapter header, the collapsible quick-links chip row and the article list,
 * plus the cross-article back button. Extracted verbatim from the inline `DashboardTab.ARTICLES`
 * branch of [MainAppDashboard] (the last and largest of the four dashboard tabs to be split out,
 * after Home/Chapters/Bookmarks). All navigation/scroll state lives in [navState]; this composable
 * owns only the local quick-links collapse/auto-collapse-timer state, exactly as before.
 */
@Composable
fun ArticlesTabContent(
    viewModel: ConstitutionViewModel,
    navState: DashboardNavState,
    bookmarksList: List<BookmarkEntity>,
    onNavigateToArticle: (Article, Article?) -> Unit,
    onNavigateToArticlesTab: () -> Unit,
) {
    val navigateToArticle: (Article) -> Unit = { onNavigateToArticle(it, null) }

    val selectedChapter = viewModel.chapters.find { it.id == navState.currentSelectedChapterId }
        ?: viewModel.chapters.first()
    val chapterArticles = viewModel.articlesForChapter(navState.currentSelectedChapterId)

    val context = LocalContext.current
    val activeArticleIndex by remember {
        derivedStateOf {
            navState.clickedArticleIndex ?: navState.articlesListState.firstVisibleItemIndex
        }
    }

    LaunchedEffect(navState.currentSelectedChapterId, activeArticleIndex) {
        navState.trimHistoryAt(chapterArticles.getOrNull(activeArticleIndex))
    }

    var isQuickLinksCollapsed by remember(navState.currentSelectedChapterId) { mutableStateOf(true) }
    var lastScrollStartTime by remember { mutableStateOf(0L) }

    LaunchedEffect(navState.articlesListState.isScrollInProgress) {
        if (navState.articlesListState.isScrollInProgress) {
            if (!navState.ignoreScrollActiveIndexSetting) {
                navState.clickedArticleIndex = null
            }
            lastScrollStartTime = System.currentTimeMillis()
        } else {
            navState.ignoreScrollActiveIndexSetting = false
        }
    }

    LaunchedEffect(lastScrollStartTime) {
        if (lastScrollStartTime > 0L && !isQuickLinksCollapsed) {
            kotlinx.coroutines.delay(2000)
            isQuickLinksCollapsed = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        if (navState.navigationHistory.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                val lastArticle = navState.navigationHistory.last()
                Surface(
                    onClick = {
                        val targetArticle = navState.popBack()
                        if (targetArticle != null) {
                            onNavigateToArticlesTab()
                            navState.popToArticle(targetArticle)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = SlateBg,
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.testTag("back_to_previous_article_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = getBackNavigationText(lastArticle),
                            tint = SovereignBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = getBackNavigationText(lastArticle),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = SovereignBlue
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 0.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = SovereignBlue,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = chapterArticles.size >= 5) {
                            isQuickLinksCollapsed = !isQuickLinksCollapsed
                        }
                        .testTag("quick_links_toggle")
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(SunflowerYellow),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${selectedChapter.id}",
                            fontWeight = FontWeight.Black,
                            color = SovereignBlue,
                            fontSize = 12.sp
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${stringResource(R.string.chapter_singular)} ${selectedChapter.id}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = SunflowerYellow
                        )
                        Text(
                            text = selectedChapter.titleUa,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (selectedChapter.sourceUrl.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(selectedChapter.sourceUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {}
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = stringResource(R.string.read_chapter_source),
                                tint = SunflowerYellow,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (chapterArticles.size >= 5) {
                        Icon(
                            imageVector = if (isQuickLinksCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = if (isQuickLinksCollapsed) stringResource(R.string.expand_quick_links) else stringResource(R.string.collapse_quick_links),
                            tint = SunflowerYellow,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = chapterArticles.size >= 5 && !isQuickLinksCollapsed,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                    HorizontalDivider(
                        color = SunflowerYellow.copy(alpha = 0.25f),
                        thickness = 1.dp
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        chapterArticles.forEachIndexed { index, article ->
                            val isActive = index == activeArticleIndex
                            val bgColor = if (isActive) SunflowerYellow else Color.White
                            val borderStroke = if (isActive) {
                                BorderStroke(2.5.dp, Color.White)
                            } else {
                                BorderStroke(1.5.dp, SunflowerYellow)
                            }

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(bgColor)
                                    .border(borderStroke, CircleShape)
                                    .testTag("quick_link_chip_$index")
                                    .clickable {
                                        navState.jumpToArticleIndex(index)
                                        isQuickLinksCollapsed = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                ArticleIdText(
                                     id = article.id,
                                     fontSize = if (ArticleNumberFormatter.isFractional(article.id, article.chapterId)) 13.sp else 14.sp,
                                     fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Black,
                                     color = SovereignBlue,
                                     chapterId = article.chapterId
                                 )
                            }
                        }
                    }
                    }
                }
            }
        }

        LazyColumn(
            state = navState.articlesListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (chapterArticles.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (selectedChapter.info.isNotEmpty()) {
                            Text(
                                text = selectedChapter.info,
                                style = MaterialTheme.typography.bodyMedium,
                                color = RichNavyText,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        selectedChapter.excludedNote?.let { note ->
                            NoteCard(note = note, onArticleClick = navigateToArticle, resolveArticleLink = viewModel::resolveLink)
                        }
                        if (selectedChapter.info.isEmpty() && selectedChapter.excludedNote == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.empty_chapter_msg),
                                    color = SovereignBlue
                                )
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(chapterArticles) { index, article ->
                    val isBookmarked = bookmarksList.any { it.articleId == article.bookmarkId }
                    val editsJson = bookmarksList.find { it.articleId == article.bookmarkId }?.editsJson ?: ""
                    ArticleCard(
                        article = article,
                        isBookmarked = isBookmarked,
                        onToggleBookmark = { viewModel.toggleBookmark(article.bookmarkId) },
                        initialEditsJson = editsJson,
                        onArticleClick = { target -> onNavigateToArticle(target, article) },
                        resolveArticleLink = viewModel::resolveLink
                    )
                }
            }
        }
    }
}
