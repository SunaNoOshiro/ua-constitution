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
import ua.constitution.domain.content.ChapterRangeKind
import ua.constitution.domain.content.chapterRangeKind
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppDashboard(viewModel: ConstitutionViewModel) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(DashboardTab.HOME) } // Default to Home (Державні символи)
    var isSearchActive by remember { mutableStateOf(false) } // Controls immediate search bar drop
    var currentSelectedChapterId by remember { mutableStateOf(1) } // Default to Chapter 1

    var fullscreenSymbol by remember { mutableStateOf(FullscreenSymbol.NONE) }
    var isRotated by remember { mutableStateOf(false) }
    var showBadge by remember { mutableStateOf(true) }

    LaunchedEffect(fullscreenSymbol) {
        isRotated = false
        
        // Find hosting activity to toggle immersive system bar states
        var currentContext = context
        var activity: android.app.Activity? = null
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is android.app.Activity) {
                activity = currentContext
                break
            }
            currentContext = currentContext.baseContext
        }
        
        activity?.window?.let { win ->
            val controller = androidx.core.view.WindowCompat.getInsetsController(win, win.decorView)
            if (fullscreenSymbol != FullscreenSymbol.NONE) {
                // Completely hide status and navigation bars via both flag and insets controller
                win.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
                controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                // Restore system bars and flags
                win.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                win.statusBarColor = android.graphics.Color.TRANSPARENT
                controller.isAppearanceLightStatusBars = true
            }
        }

        if (fullscreenSymbol != FullscreenSymbol.NONE) {
            showBadge = true
            kotlinx.coroutines.delay(2000L)
            showBadge = false
        }
    }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredArticles by viewModel.filteredArticles.collectAsState()
    val bookmarksList by viewModel.bookmarks.collectAsState(initial = emptyList())

    val focusRequester = remember { FocusRequester() }

    val articlesLazyListState = rememberLazyListState()
    val homeScrollState = rememberScrollState()
    var clickedArticleIndex by remember { mutableStateOf<Int?>(null) }
    var ignoreScrollActiveIndexSetting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val navigationHistory = remember { mutableStateListOf<Article>() }

    var bookmarkEditingArticleId by remember { mutableStateOf<Int?>(null) }
    var bookmarkActiveTool by remember { mutableStateOf(Constants.TOOL_NONE) } 
    var bookmarkSelectedMarkerColorHex by remember { mutableStateOf(Constants.COLOR_DEFAULT_MARKER) }
    var bookmarkSelectedUnderlineColorHex by remember { mutableStateOf(Constants.COLOR_DEFAULT_UNDERLINE) }
    var bookmarkPanelExpanded by remember { mutableStateOf(false) }
    var activeEditingWarningMessage by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = navigationHistory.isNotEmpty()) {
        val targetArticle = navigationHistory.removeLastOrNull()
        if (targetArticle != null) {
            currentSelectedChapterId = targetArticle.chapterId
            activeTab = DashboardTab.ARTICLES
            coroutineScope.launch {
                val chapterArticles = viewModel.articlesForChapter(targetArticle.chapterId)
                val index = chapterArticles.indexOfFirst { it.bookmarkId == targetArticle.bookmarkId }
                if (index >= 0) {
                    ignoreScrollActiveIndexSetting = true
                    clickedArticleIndex = index
                    articlesLazyListState.animateScrollToItem(index)
                }
            }
        }
    }

    val navigateToArticleWithOrigin: (Article, Article?) -> Unit = { targetArticle, originArticle ->
        val currentArticle = originArticle ?: run {
            if (activeTab == DashboardTab.ARTICLES) {
                val chapterArticles = viewModel.articlesForChapter(currentSelectedChapterId)
                val activeArticleIndexVal = articlesLazyListState.firstVisibleItemIndex
                chapterArticles.getOrNull(activeArticleIndexVal)
            } else {
                null
            }
        }
        if (currentArticle != null && currentArticle.bookmarkId != targetArticle.bookmarkId) {
            if (navigationHistory.isEmpty() || navigationHistory.last().bookmarkId != currentArticle.bookmarkId) {
                navigationHistory.add(currentArticle)
            }
        }
        currentSelectedChapterId = targetArticle.chapterId
        activeTab = DashboardTab.ARTICLES
        coroutineScope.launch {
            val chapterArticles = viewModel.articlesForChapter(targetArticle.chapterId)
            val index = chapterArticles.indexOfFirst { it.bookmarkId == targetArticle.bookmarkId }
            if (index >= 0) {
                ignoreScrollActiveIndexSetting = true
                clickedArticleIndex = index
                articlesLazyListState.animateScrollToItem(index)
            }
        }
    }

    val navigateToArticle: (Article) -> Unit = { targetArticle ->
        navigateToArticleWithOrigin(targetArticle, null)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
            containerColor = Color(0xFFFFFDE7), // Radiant high-fidelity soft yellow canvas
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFFFD500), // Dynamic flag-colored yellow
                contentColor = Color(0xFF0D47A1),   // Sovereign Ukrainian corporate blue
                tonalElevation = 8.dp,
                modifier = Modifier
                    .shadow(16.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                // Item 1: Chapters
                NavigationBarItem(
                    selected = activeTab == DashboardTab.CHAPTERS && !isSearchActive,
                    onClick = { 
                        navigationHistory.clear()
                        activeTab = DashboardTab.CHAPTERS
                        isSearchActive = false
                    },
                    label = { Text(stringResource(R.string.tab_chapters), fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1), fontSize = 10.sp) },
                    icon = { Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.tab_chapters), tint = Color(0xFF0D47A1), modifier = Modifier.size(20.dp)) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color(0xFFFFF9C4)
                    )
                )

                // Item 2: Articles
                NavigationBarItem(
                    selected = activeTab == DashboardTab.ARTICLES && !isSearchActive,
                    onClick = { 
                        if (activeTab == DashboardTab.ARTICLES && !isSearchActive) {
                            coroutineScope.launch {
                                articlesLazyListState.animateScrollToItem(0)
                            }
                        } else {
                            navigationHistory.clear()
                            activeTab = DashboardTab.ARTICLES
                            isSearchActive = false
                        }
                    },
                    label = { Text(stringResource(R.string.tab_articles), fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1), fontSize = 10.sp) },
                    icon = { Icon(Icons.Default.List, contentDescription = stringResource(R.string.tab_articles), tint = Color(0xFF0D47A1), modifier = Modifier.size(20.dp)) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color(0xFFFFF9C4)
                    )
                )

                // Item 3 (Center): Standard NavigationBarItem styled to look like a prominent circular button
                val homeSelected = activeTab == DashboardTab.HOME && !isSearchActive
                NavigationBarItem(
                    selected = homeSelected,
                    onClick = { 
                        navigationHistory.clear()
                        activeTab = DashboardTab.HOME
                        isSearchActive = false
                    },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(if (homeSelected) 6.dp else 2.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = if (homeSelected) {
                                            listOf(Color(0xFF1E88E5), Color(0xFF0D47A1))
                                        } else {
                                            listOf(Color(0xFF1E88E5).copy(alpha = 0.85f), Color(0xFF0D47A1).copy(alpha = 0.82f))
                                        }
                                    )
                                )
                                .border(1.5.dp, Color(0xFFFFD500), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            UkrainianCoatOfArms(
                                useIsolated = true,
                                modifier = Modifier
                                    .size(28.dp)
                                    .padding(1.dp)
                            )
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent // Clean circular feel
                    )
                )

                // Item 4: Search
                NavigationBarItem(
                    selected = isSearchActive,
                    onClick = { 
                        isSearchActive = !isSearchActive 
                    },
                    label = { Text(stringResource(R.string.tab_search), fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1), fontSize = 10.sp) },
                    icon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.tab_search), tint = Color(0xFF0D47A1), modifier = Modifier.size(20.dp)) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color(0xFFFFF9C4)
                    )
                )

                // Item 5: Bookmarks
                NavigationBarItem(
                    selected = activeTab == DashboardTab.BOOKMARKS && !isSearchActive,
                    onClick = { 
                        navigationHistory.clear()
                        activeTab = DashboardTab.BOOKMARKS
                        isSearchActive = false
                    },
                    label = { Text(stringResource(R.string.tab_bookmarks), fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1), fontSize = 10.sp) },
                    icon = { 
                        Icon(
                            imageVector = if (activeTab == DashboardTab.BOOKMARKS && !isSearchActive) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, 
                            contentDescription = stringResource(R.string.tab_bookmarks), 
                            tint = Color(0xFF0D47A1),
                            modifier = Modifier.size(20.dp)
                        ) 
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color(0xFFFFF9C4)
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // High-fidelity Ukrainian Sovereign Header with Emblem (Hamburger/Menu button removed)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFD500)) // Solid flag-colored yellow
                    .statusBarsPadding()
                    .padding(top = 0.dp, bottom = 2.dp, start = 20.dp, end = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Coat of arms (Герб України) inside a beautiful circular badge to prevent shadow bleed-through
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(3.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF1E88E5), // Bright center blue
                                        Color(0xFF0D47A1)  // Regal sovereign blue
                                    )
                                )
                            )
                            .border(1.5.dp, Color(0xFFFFD500), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        UkrainianCoatOfArms(
                            useIsolated = true,
                            modifier = Modifier
                                .size(30.dp)
                                .padding(1.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.app_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 22.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0D47A1)
                        )
                        val context = LocalContext.current
                        Text(
                            text = stringResource(R.string.official_source),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D47A1).copy(alpha = 0.75f),
                            modifier = Modifier
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://zakon.rada.gov.ua/laws/show/254%D0%BA/96-%D0%B2%D1%80"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {}
                                }
                        )
                    }
                }
            }

            if (viewModel.initializationError.isNotEmpty() || viewModel.articlesEmpty) {
                Surface(
                    color = Color(0xFFF8D7DA),
                    contentColor = Color(0xFF721C24),
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFF5C6CB))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.error_loading_constitution),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Error: ${viewModel.initializationError}. Articles count: ${viewModel.articlesCount}. Verified hash: ${viewModel.computedHash}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Immediately focus on search bar if active
            LaunchedEffect(isSearchActive) {
                if (isSearchActive) {
                    focusRequester.requestFocus()
                }
            }

            // Expanded Animated top search field container
            AnimatedVisibility(
                visible = isSearchActive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.quick_search_header),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0D47A1),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { 
                            Text(
                                stringResource(R.string.search_placeholder),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.tab_search), tint = Color(0xFF0D47A1)) },
                        trailingIcon = {
                            IconButton(
                                onClick = { 
                                    isSearchActive = false
                                    viewModel.setSearchQuery("")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.search_clear_desc),
                                    tint = Color(0xFF0D47A1)
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .testTag("search_field"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0D47A1),
                            unfocusedBorderColor = Color(0xFF0D47A1).copy(alpha = 0.5f),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color(0xFF0D47A1),
                            unfocusedTextColor = Color(0xFF0D47A1)
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isSearchActive) {
                    // Universal in-place search results list 
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
                                            color = Color(0xFF0D47A1).copy(alpha = 0.7f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                items(filteredArticles) { article ->
                                    val isBookmarked = bookmarksList.any { it.articleId == article.bookmarkId }
                                    val editsJson = bookmarksList.find { it.articleId == article.bookmarkId }?.editsJson ?: ""
                                    ArticleCard(
                                        article = article,
                                        isBookmarked = isBookmarked,
                                        initialEditsJson = editsJson,
                                        onToggleBookmark = { viewModel.toggleBookmark(article.bookmarkId) },
                                        onArticleClick = { target -> navigateToArticleWithOrigin(target, article) },
                                        resolveArticleLink = viewModel::resolveLink
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Normal app tabs (Chapters, Articles list, or Bookmarks)
                    when (activeTab) {
                        DashboardTab.HOME -> {
                            HomeTabContent(
                                viewModel = viewModel,
                                bookmarksList = bookmarksList,
                                onNavigateToArticle = navigateToArticleWithOrigin,
                                onOpenFullscreenSymbol = { symbol ->
                                    fullscreenSymbol = symbol
                                }
                            )
                        }
                        DashboardTab.CHAPTERS -> {
                            ChaptersTabContent(
                                chapters = viewModel.chapters,
                                articlesForChapter = viewModel::articlesForChapter,
                                onSelectChapter = { selectedId ->
                                    val isNewChapter = selectedId != currentSelectedChapterId
                                    currentSelectedChapterId = selectedId
                                    activeTab = DashboardTab.ARTICLES
                                    if (isNewChapter) {
                                        clickedArticleIndex = null
                                        coroutineScope.launch {
                                            articlesLazyListState.scrollToItem(0)
                                        }
                                    }
                                },
                                onOpenSourceUrl = { url ->
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    } catch (e: Exception) {}
                                }
                            )
                        }

                        DashboardTab.ARTICLES -> {
                            val selectedChapter = viewModel.chapters.find { it.id == currentSelectedChapterId }
                                ?: viewModel.chapters.first()
                            val chapterArticles = viewModel.articlesForChapter(currentSelectedChapterId)

                            val context = LocalContext.current
                            val activeArticleIndex by remember {
                                derivedStateOf {
                                    clickedArticleIndex ?: articlesLazyListState.firstVisibleItemIndex
                                }
                            }

                            LaunchedEffect(currentSelectedChapterId, activeArticleIndex) {
                                val currentArticle = chapterArticles.getOrNull(activeArticleIndex)
                                if (currentArticle != null && navigationHistory.isNotEmpty()) {
                                    val indexInHistory = navigationHistory.indexOfFirst { it.bookmarkId == currentArticle.bookmarkId }
                                    if (indexInHistory >= 0) {
                                        while (navigationHistory.size > indexInHistory) {
                                            navigationHistory.removeLastOrNull()
                                        }
                                    }
                                }
                            }

                            var isQuickLinksCollapsed by remember(currentSelectedChapterId) { mutableStateOf(true) }
                            var lastScrollStartTime by remember { mutableStateOf(0L) }

                            LaunchedEffect(articlesLazyListState.isScrollInProgress) {
                                if (articlesLazyListState.isScrollInProgress) {
                                    if (!ignoreScrollActiveIndexSetting) {
                                        clickedArticleIndex = null
                                    }
                                    lastScrollStartTime = System.currentTimeMillis()
                                } else {
                                    ignoreScrollActiveIndexSetting = false
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
                                if (navigationHistory.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        val lastArticle = navigationHistory.last()
                                        Surface(
                                            onClick = {
                                                val targetArticle = navigationHistory.removeLastOrNull()
                                                if (targetArticle != null) {
                                                    currentSelectedChapterId = targetArticle.chapterId
                                                    activeTab = DashboardTab.ARTICLES
                                                    coroutineScope.launch {
                                                        val chapterArticles = viewModel.articlesForChapter(targetArticle.chapterId)
                                                        val index = chapterArticles.indexOfFirst { it.bookmarkId == targetArticle.bookmarkId }
                                                        if (index >= 0) {
                                                            ignoreScrollActiveIndexSetting = true
                                                            clickedArticleIndex = index
                                                            articlesLazyListState.animateScrollToItem(index)
                                                        }
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFF1F5F9),
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                                                    tint = Color(0xFF0D47A1),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = getBackNavigationText(lastArticle),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0D47A1)
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
                                    color = Color(0xFF0D47A1),
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
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFFD500)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${selectedChapter.id}",
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF0D47A1),
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${stringResource(R.string.chapter_singular)} ${selectedChapter.id}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFFFFD500)
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
                                                        tint = Color(0xFFFFD500),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            if (chapterArticles.size >= 5) {
                                                Icon(
                                                    imageVector = if (isQuickLinksCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                                    contentDescription = if (isQuickLinksCollapsed) stringResource(R.string.expand_quick_links) else stringResource(R.string.collapse_quick_links),
                                                    tint = Color(0xFFFFD500),
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
                                                color = Color(0xFFFFD500).copy(alpha = 0.25f),
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
                                                    val bgColor = if (isActive) Color(0xFFFFD500) else Color.White
                                                    val borderStroke = if (isActive) {
                                                        BorderStroke(2.5.dp, Color.White)
                                                    } else {
                                                        BorderStroke(1.5.dp, Color(0xFFFFD500))
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .size(38.dp)
                                                            .clip(CircleShape)
                                                            .background(bgColor)
                                                            .border(borderStroke, CircleShape)
                                                            .clickable {
                                                                ignoreScrollActiveIndexSetting = true
                                                                clickedArticleIndex = index
                                                                coroutineScope.launch {
                                                                    articlesLazyListState.animateScrollToItem(index)
                                                                }
                                                                isQuickLinksCollapsed = true
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        ArticleIdText(
                                                             id = article.id,
                                                             fontSize = if (ArticleNumberFormatter.isFractional(article.id, article.chapterId)) 13.sp else 14.sp,
                                                             fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Black,
                                                             color = Color(0xFF0D47A1),
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
                                    state = articlesLazyListState,
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
                                                        color = Color(0xFF0F172A),
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
                                                            color = Color(0xFF0D47A1)
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
                                                onArticleClick = { target -> navigateToArticleWithOrigin(target, article) },
                                                resolveArticleLink = viewModel::resolveLink
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        DashboardTab.BOOKMARKS -> {
                            val bookmarkedArticles = viewModel.bookmarkedArticles(bookmarksList)

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.saved_bookmarks_header, bookmarkedArticles.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0D47A1),
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )

                                LaunchedEffect(bookmarkEditingArticleId) {
                                    bookmarkPanelExpanded = false
                                }

                                 val bookmarksListState = rememberLazyListState()
                                 val coroutineScope = rememberCoroutineScope()

                                 LaunchedEffect(bookmarksListState.isScrollInProgress) {
                                     if (bookmarksListState.isScrollInProgress && bookmarkPanelExpanded) {
                                         bookmarkPanelExpanded = false
                                     }
                                 }

                                LazyColumn(
                                    state = bookmarksListState,
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (bookmarkedArticles.isEmpty()) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 60.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Bookmark,
                                                        contentDescription = null,
                                                        tint = Color(0xFF0D47A1).copy(alpha = 0.2f),
                                                        modifier = Modifier.size(72.dp)
                                                    )
                                                    Text(
                                                        text = stringResource(R.string.no_bookmarks_msg),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0D47A1).copy(alpha = 0.6f)
                                                    )
                                                    Text(
                                                        text = stringResource(R.string.bookmarks_hint),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = Color(0xFF0D47A1).copy(alpha = 0.5f),
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.padding(horizontal = 20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        itemsIndexed(
                                            items = bookmarkedArticles,
                                            key = { _, article -> article.bookmarkId }
                                        ) { index, article ->
                                             val bookmarkEntity = bookmarksList.find { it.articleId == article.bookmarkId }
                                             val editsJson = bookmarkEntity?.editsJson ?: ""
                                             val isCurrentEditable = true
                                             val onSaveCallback: (String) -> Unit = { newJson ->
                                                 viewModel.updateBookmarkEdits(article.bookmarkId, newJson)
                                             }
                                            ArticleCard(
                                                article = article,
                                                isBookmarked = true,
                                                onToggleBookmark = {
                                                    if (bookmarkEditingArticleId == article.bookmarkId) {
                                                        bookmarkEditingArticleId = null
                                                    }
                                                    viewModel.toggleBookmark(article.bookmarkId)
                                                },
                                                onArticleClick = { target -> navigateToArticleWithOrigin(target, article) },
                                                resolveArticleLink = viewModel::resolveLink,
                                                initialEditsJson = editsJson,
                                                editing = ArticleEditing(
                                                    isEditable = isCurrentEditable,
                                                    isCurrentlyEditing = (bookmarkEditingArticleId == article.bookmarkId),
                                                    isEditButtonEnabled = (bookmarkEditingArticleId == null || bookmarkEditingArticleId == article.bookmarkId),
                                                    isPanelExpanded = bookmarkPanelExpanded,
                                                    activeTool = bookmarkActiveTool,
                                                    selectedColorHex = if (bookmarkActiveTool == Constants.TOOL_UNDERLINE) bookmarkSelectedUnderlineColorHex else bookmarkSelectedMarkerColorHex,
                                                    selectedMarkerColorHex = bookmarkSelectedMarkerColorHex,
                                                    selectedUnderlineColorHex = bookmarkSelectedUnderlineColorHex,
                                                    onSaveEdits = onSaveCallback,
                                                    onDisabledEditClick = {
                                                        activeEditingWarningMessage = context.getString(R.string.save_edits_error)
                                                    },
                                                    onPanelExpandedChange = { bookmarkPanelExpanded = it },
                                                    onActiveToolChange = { tool ->
                                                        bookmarkActiveTool = tool
                                                    },
                                                    onColorHexChange = { color ->
                                                        if (bookmarkActiveTool == Constants.TOOL_UNDERLINE) {
                                                            bookmarkSelectedUnderlineColorHex = color
                                                        } else {
                                                            bookmarkSelectedMarkerColorHex = color
                                                        }
                                                    },
                                                    onToggleEditing = {
                                                        if (bookmarkEditingArticleId == article.bookmarkId) {
                                                            bookmarkEditingArticleId = null
                                                        } else {
                                                            bookmarkEditingArticleId = article.bookmarkId
                                                            bookmarkPanelExpanded = false
                                                            bookmarkActiveTool = Constants.TOOL_NONE
                                                            coroutineScope.launch {
                                                                bookmarksListState.animateScrollToItem(index)
                                                            }
                                                        }
                                                    }
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        if (fullscreenSymbol != FullscreenSymbol.NONE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (fullscreenSymbol == FullscreenSymbol.COAT_OF_ARMS) Color(0xFF005BBB) else Color.Black)
                    .clickable {
                        if (!isRotated) {
                            isRotated = true
                        } else {
                            fullscreenSymbol = FullscreenSymbol.NONE
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (fullscreenSymbol == FullscreenSymbol.FLAG) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (!isRotated) {
                            Column(modifier = Modifier.fillMaxSize()) {
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
                        } else {
                            // Rotated 90°: Yellow on Left, Blue on Right
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f)
                                        .background(Color(0xFFFFD700)) // Golden yellow on left
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f)
                                        .background(Color(0xFF0057B7)) // Sovereign blue on right
                                )
                            }
                        }
                    }
                } else if (fullscreenSymbol == FullscreenSymbol.COAT_OF_ARMS) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val tryzubRatio = 165f / 230.5f
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

        // Floating custom banner/toast overlay
        activeEditingWarningMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, bottom = 96.dp), // floats above bottom navigation bar
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.95f),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, Color(0xFF475569)),
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
                            tint = Color(0xFFFFD500),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = msg,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { activeEditingWarningMessage = null },
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
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(4000L)
                if (activeEditingWarningMessage == msg) {
                    activeEditingWarningMessage = null
                }
            }
        }
    }

}

// Gorgeous always-expanded article card with interactive title and integrated bookmarks


@Composable
fun ChaptersTabContent(
    chapters: List<Chapter>,
    articlesForChapter: (Int) -> List<Article>,
    onSelectChapter: (Int) -> Unit,
    onOpenSourceUrl: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = stringResource(R.string.select_chapter_header),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = Color(0xFF0D47A1),
            modifier = Modifier.padding(vertical = 10.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(chapters) { chapter ->
                val chapterArticles = remember(chapter.id) {
                    articlesForChapter(chapter.id)
                }
                val preambleStr = stringResource(R.string.preamble)
                val rangeText = if (chapterArticles.isNotEmpty()) {
                    val firstId = formatArticleId(chapterArticles.first().id, chapter.id)
                    val lastId = formatArticleId(chapterArticles.last().id, chapter.id)
                    when (chapterRangeKind(chapter.id, firstId == lastId)) {
                        ChapterRangeKind.PREAMBLE -> preambleStr
                        ChapterRangeKind.POINT_SINGLE -> stringResource(R.string.point_range_single, firstId)
                        ChapterRangeKind.POINT_MULTI -> stringResource(R.string.point_range_multi, firstId, lastId)
                        ChapterRangeKind.ARTICLE_SINGLE -> stringResource(R.string.article_range_single, firstId)
                        ChapterRangeKind.ARTICLE_MULTI -> stringResource(R.string.article_range_multi, firstId, lastId)
                    }
                } else {
                    ""
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chapter_card_${chapter.id}")
                        .clickable { onSelectChapter(chapter.id) }
                        .shadow(2.dp, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.5.dp, Color(0xFF0D47A1))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0D47A1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${chapter.id}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${stringResource(R.string.chapter_singular)} ${chapter.id}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0D47A1)
                                )
                                if (rangeText.isNotEmpty()) {
                                    Text(
                                        text = "($rangeText)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0D47A1).copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Text(
                                text = chapter.titleUa,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0D47A1)
                            )
                        }

                        if (chapter.sourceUrl.isNotEmpty()) {
                            IconButton(
                                onClick = { onOpenSourceUrl(chapter.sourceUrl) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = stringResource(R.string.read_chapter_source),
                                    tint = Color(0xFF0D47A1).copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                 )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = stringResource(R.string.open_chapter_articles),
                            tint = Color(0xFF0D47A1)
                        )
                    }
                }
            }
        }
    }
}