package com.example

import android.content.Intent
import com.example.utils.Constants
import com.example.utils.LogMessages
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
import com.example.data.model.Paragraph
import com.example.data.model.Note
import com.example.data.model.Link
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.ConstitutionDatabase
import com.example.data.model.Article
import com.example.data.model.Chapter
import com.example.data.model.ConstitutionData
import com.example.data.repository.ConstitutionRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ConstitutionViewModel
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

enum class FullscreenSymbol {
    NONE,
    FLAG,
    COAT_OF_ARMS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Load official formatted articles from JSON assets
        ConstitutionData.initialize(this)
        
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        val database = ConstitutionDatabase.getDatabase(this)
        val repository = ConstitutionRepository(database.constitutionDao())

        val viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ConstitutionViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return ConstitutionViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        })[ConstitutionViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainAppDashboard(viewModel = viewModel)
            }
        }
    }
}

enum class DashboardTab {
    HOME,      // Головна (Державні символи та стаття дня)
    CHAPTERS,  // Вкладка 1: Розділи
    ARTICLES,  // Вкладка 2: Статті
    BOOKMARKS  // Вкладка 3: Закладки
}

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
                val chapterArticles = ConstitutionData.articles.filter { it.chapterId == targetArticle.chapterId }
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
                val chapterArticles = ConstitutionData.articles.filter { it.chapterId == currentSelectedChapterId }
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
            val chapterArticles = ConstitutionData.articles.filter { it.chapterId == targetArticle.chapterId }
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

            if (ConstitutionData.initializationError.isNotEmpty() || ConstitutionData.articles.isEmpty()) {
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
                            text = "Error: ${ConstitutionData.initializationError}. Articles count: ${ConstitutionData.articles.size}. Verified hash: ${ConstitutionData.computedHash}",
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
                                        onArticleClick = { target -> navigateToArticleWithOrigin(target, article) }
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
                                    items(ConstitutionData.chapters) { chapter ->
                                        val chapterArticles = remember(chapter.id) {
                                            ConstitutionData.articles.filter { it.chapterId == chapter.id }
                                        }
                                        val preambleStr = stringResource(R.string.preamble)
                                        val rangeText = if (chapterArticles.isNotEmpty()) {
                                            val firstId = formatArticleId(chapterArticles.first().id, chapter.id)
                                            val lastId = formatArticleId(chapterArticles.last().id, chapter.id)
                                            if (chapter.id == 0) {
                                                preambleStr
                                            } else if (chapter.id == 15) {
                                                if (firstId == lastId) {
                                                    stringResource(R.string.point_range_single, firstId)
                                                } else {
                                                    stringResource(R.string.point_range_multi, firstId, lastId)
                                                }
                                            } else if (firstId == lastId) {
                                                stringResource(R.string.article_range_single, firstId)
                                            } else {
                                                stringResource(R.string.article_range_multi, firstId, lastId)
                                            }
                                        } else {
                                            ""
                                        }

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val isNewChapter = chapter.id != currentSelectedChapterId
                                                    currentSelectedChapterId = chapter.id
                                                    activeTab = DashboardTab.ARTICLES 
                                                    if (isNewChapter) {
                                                        clickedArticleIndex = null
                                                        coroutineScope.launch {
                                                            articlesLazyListState.scrollToItem(0)
                                                        }
                                                    }
                                                }
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
                                                        onClick = {
                                                            try {
                                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(chapter.sourceUrl))
                                                                context.startActivity(intent)
                                                            } catch (e: Exception) {}
                                                        },
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

                        DashboardTab.ARTICLES -> {
                            val selectedChapter = ConstitutionData.chapters.find { it.id == currentSelectedChapterId }
                                ?: ConstitutionData.chapters.first()
                            val chapterArticles = ConstitutionData.articles.filter { it.chapterId == currentSelectedChapterId }

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
                                                        val chapterArticles = ConstitutionData.articles.filter { it.chapterId == targetArticle.chapterId }
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
                                                             fontSize = if (article.id > 1000 || (article.chapterId == 15 && article.id == 161)) 13.sp else 14.sp,
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
                                                    NoteCard(note = note, onArticleClick = navigateToArticle)
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
                                                onArticleClick = { target -> navigateToArticleWithOrigin(target, article) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        DashboardTab.BOOKMARKS -> {
                            val bookmarkedArticles = ConstitutionData.articles.filter { article ->
                                bookmarksList.any { it.articleId == article.bookmarkId }
                            }

                            val editingArticleObj = remember(bookmarkEditingArticleId) {
                                if (bookmarkEditingArticleId != null) {
                                    ConstitutionData.articles.find { it.bookmarkId == bookmarkEditingArticleId }
                                } else {
                                    null
                                }
                            }
                            
                            val editingArticleTitle = remember(editingArticleObj) {
                                editingArticleObj?.let { article ->
                                    val regex = """^(Стаття|Пункт)\s+(\d+(?:[\.\-]\d+)?)\.?\s*(.*)$""".toRegex()
                                    val match = regex.find(article.titleUa)
                                    if (match != null) {
                                        val label = match.groupValues[1]
                                        val rawNumber = match.groupValues[2]
                                        "$label $rawNumber"
                                    } else {
                                        article.titleUa
                                    }
                                }
                            }

                            val editingBookmarkEntity = remember(bookmarksList, bookmarkEditingArticleId) {
                                if (bookmarkEditingArticleId != null) {
                                    bookmarksList.find { it.articleId == bookmarkEditingArticleId }
                                } else {
                                    null
                                }
                            }
                            val activeBookmarkHasEdits = !editingBookmarkEntity?.editsJson.isNullOrBlank() && 
                                editingBookmarkEntity?.editsJson != "{}" && 
                                editingBookmarkEntity?.editsJson != "{\"paragraphEdits\":{}}"

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
                                                isEditable = isCurrentEditable,
                                                initialEditsJson = editsJson,
                                                onSaveEdits = onSaveCallback,
                                                 onDisabledEditClick = {
                                                     activeEditingWarningMessage = context.getString(R.string.save_edits_error)
                                                 },
                                                 isEditButtonEnabled = (bookmarkEditingArticleId == null || bookmarkEditingArticleId == article.bookmarkId),
                                                 isPanelExpanded = bookmarkPanelExpanded,
                                                 onPanelExpandedChange = { bookmarkPanelExpanded = it },
                                                 onActiveToolChange = { tool ->
                                                     bookmarkActiveTool = tool
                                                     val markerColors = Constants.MARKER_COLORS
                                                     val underlineColors = Constants.UNDERLINE_COLORS
                                                     if (false) {
                                                         val idx = -1
                                                         if (idx >= 0) {
                                                             // none
                                                         } else if (false) {
                                                             // none
                                                         }
                                                     } else if (false) {
                                                         val idx = -1
                                                         if (idx >= 0) {
                                                             // none
                                                         } else if (false) {
                                                             // none
                                                         }
                                                     }
                                                 },
                                                 onColorHexChange = { color ->
                                                      if (bookmarkActiveTool == Constants.TOOL_UNDERLINE) {
                                                          bookmarkSelectedUnderlineColorHex = color
                                                      } else {
                                                          bookmarkSelectedMarkerColorHex = color
                                                      }
                                                  },
                                                isCurrentlyEditing = (bookmarkEditingArticleId == article.bookmarkId),
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
                                                },
                                                activeTool = bookmarkActiveTool,
                                                selectedColorHex = if (bookmarkActiveTool == Constants.TOOL_UNDERLINE) bookmarkSelectedUnderlineColorHex else bookmarkSelectedMarkerColorHex,
                                                 selectedMarkerColorHex = bookmarkSelectedMarkerColorHex,
                                                 selectedUnderlineColorHex = bookmarkSelectedUnderlineColorHex
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
fun ArticleCard(
    article: Article,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
    onArticleClick: ((Article) -> Unit)? = null,
    isEditable: Boolean = false,
    initialEditsJson: String = "",
    onSaveEdits: ((String) -> Unit)? = null,
    isCurrentlyEditing: Boolean = false,
    onToggleEditing: (() -> Unit)? = null,
    activeTool: String = Constants.TOOL_MARKER,
    selectedColorHex: String = "#FFF59D",
    selectedMarkerColorHex: String = "#FFF59D",
    selectedUnderlineColorHex: String = "#F57F17",
    isEditButtonEnabled: Boolean = true,
    isPanelExpanded: Boolean = false,
    onPanelExpandedChange: ((Boolean) -> Unit)? = null,
    onActiveToolChange: ((String) -> Unit)? = null,
    onColorHexChange: ((String) -> Unit)? = null,
    onDisabledEditClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    
    // Split the title (e.g., "Стаття 20. Державні символи України") into number and title
    val titleParts = remember(article.id, article.titleUa) {
        val regex = """^(Стаття|Пункт)\s+(\d+(?:[\.\-]\d+)?)\.?\s*(.*)$""".toRegex()
        val match = regex.find(article.titleUa)
        if (match != null) {
            val label = match.groupValues[1]
            val rawNumber = match.groupValues[2]
            val formattedNumber = formatStringToSuperscript(rawNumber)
            val namePart = match.groupValues[3]
            Pair("$label $formattedNumber", namePart)
        } else {
            Pair(formatStringToSuperscript(article.titleUa), "")
        }
    }
    val articleNumber = titleParts.first
    val articleName = titleParts.second

    var localEdits by remember(isEditable) { mutableStateOf(emptyMap<Int, List<StyledRange>>()) }
    var lastSavedJson by remember { mutableStateOf("") }
    val paragraphRegistry = remember { mutableMapOf<Int, ParagraphRegistryEntry>() }
    
    var showRemoveConfirmDialog by remember { mutableStateOf(false) }
    val hasEdits = remember(initialEditsJson, localEdits) {
        BookmarkEditsParser.parse(initialEditsJson).isNotEmpty() || localEdits.isNotEmpty()
    }
    
    LaunchedEffect(initialEditsJson, isEditable) {
        if (isEditable) {
            if (lastSavedJson.isNotEmpty() && initialEditsJson == lastSavedJson) {
                lastSavedJson = ""
            } else {
                localEdits = BookmarkEditsParser.parse(initialEditsJson)
                lastSavedJson = ""
            }
        } else {
            localEdits = emptyMap()
            lastSavedJson = ""
        }
    }

    if (showRemoveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirmDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveConfirmDialog = false
                        onToggleBookmark()
                    }
                ) {
                    Text(
                        text = context.getString(R.string.dialog_remove_bookmark_confirm),
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRemoveConfirmDialog = false }
                ) {
                    Text(
                        text = context.getString(R.string.dialog_remove_bookmark_cancel),
                        color = Color(0xFF0D47A1),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            title = {
                Text(
                    text = context.getString(R.string.dialog_remove_bookmark_title),
                    color = Color(0xFF0D47A1),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = context.getString(R.string.dialog_remove_bookmark_message),
                    color = Color(0xFF0D47A1).copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            containerColor = Color(0xFFFFFDE7),
            tonalElevation = 6.dp,
            properties = DialogProperties(usePlatformDefaultWidth = true)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            width = 2.dp,
            color = Color(0xFF0D47A1)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = articleNumber,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0D47A1),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.radaUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {}
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = stringResource(R.string.read_source_btn),
                        tint = Color(0xFF0D47A1).copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        try {
                            val textToCopy = buildString {
                                append(articleNumber)
                                if (articleName.isNotEmpty()) {
                                    append(". ")
                                    append(articleName)
                                }
                                append("\n\n")
                                article.paragraphs.forEachIndexed { index, paragraph ->
                                    append(formatStringToSuperscript(paragraph.text))
                                    if (index < article.paragraphs.lastIndex) {
                                        append("\n\n")
                                    }
                                }
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(context.getString(R.string.tab_articles), textToCopy)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, context.getString(R.string.toast_article_copied), Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.toast_copy_error, e.message ?: ""), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(36.dp).testTag("copy_article_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.copy_article),
                        tint = Color(0xFF0D47A1).copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (isEditable) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (isCurrentlyEditing || isEditButtonEnabled) {
                                onToggleEditing?.invoke()
                            } else {
                                onDisabledEditClick?.invoke()
                            }
                        },
                        modifier = Modifier.size(36.dp).testTag("edit_article_button")
                    ) {
                         Icon(
                             imageVector = if (isCurrentlyEditing) Icons.Default.Check else Icons.Default.Edit,
                             contentDescription = if (isCurrentlyEditing) stringResource(R.string.close_editing) else stringResource(R.string.edit_highlights),
                             tint = if (isCurrentlyEditing) Color(0xFF2E7D32) else if (isEditButtonEnabled) Color(0xFF0D47A1) else Color(0xFF94A3B8),
                             modifier = Modifier.size(20.dp)
                         )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Bookmark toggle icon (becomes blue when active)
                IconButton(
                    onClick = {
                        if (isBookmarked && hasEdits) {
                            showRemoveConfirmDialog = true
                        } else {
                            onToggleBookmark()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = stringResource(R.string.save_bookmark),
                        tint = if (isBookmarked) Color(0xFF0D47A1) else Color(0xFF0D47A1).copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (articleName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = articleName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0D47A1),
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCurrentlyEditing) 4.dp else 14.dp))

            if (isCurrentlyEditing) {
                GlobalFormattingPanel(
                    editingArticleId = article.bookmarkId,
                    activeTool = activeTool,
                    onActiveToolChange = { onActiveToolChange?.invoke(it) },
                    selectedColorHex = selectedColorHex,
                    onColorHexChange = { onColorHexChange?.invoke(it) },
                    isPanelExpanded = isPanelExpanded,
                    onPanelExpandedChange = { onPanelExpandedChange?.invoke(it) },
                    onClearAllEdits = {
                        localEdits = emptyMap()
                        lastSavedJson = ""
                        onSaveEdits?.invoke("")
                    },
                    hasAnyEdits = localEdits.isNotEmpty(),
                    onDoneEditing = { onToggleEditing?.invoke() },
                    articleTitle = null
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Full legislation text (always expanded) - styled perfectly with paragraphs with integrated notes & links
            val fullArticleTextToCopy = remember(articleNumber, articleName, article.paragraphs) {
                buildString {
                    append(articleNumber)
                    if (articleName.isNotEmpty()) {
                        append(". ")
                        append(articleName)
                    }
                    append("\n\n")
                    article.paragraphs.forEachIndexed { index, paragraph ->
                        append(formatStringToSuperscript(paragraph.text))
                        if (index < article.paragraphs.lastIndex) {
                            append("\n\n")
                        }
                    }
                }
            }

            val combinedSegments = remember(article.paragraphs) {
                val result = mutableListOf<com.example.data.model.ContentSegment>()
                article.paragraphs.forEachIndexed { index, paragraph ->
                    result.addAll(paragraph.content)
                    if (index < article.paragraphs.lastIndex) {
                        result.add(com.example.data.model.ContentSegment(type = "text", value = "\n\n"))
                    }
                }
                result
            }

            val paragraphOffsets = remember(article.paragraphs) {
                val offsets = IntArray(article.paragraphs.size)
                var currentOffset = 0
                article.paragraphs.forEachIndexed { index, paragraph ->
                    offsets[index] = currentOffset
                    currentOffset += paragraph.text.length + 2 // 2 for "\n\n"
                }
                offsets
            }

            val combinedRanges = remember(localEdits, paragraphOffsets) {
                val result = mutableListOf<StyledRange>()
                localEdits.forEach { (pIdx, ranges) ->
                    val offset = paragraphOffsets.getOrNull(pIdx) ?: 0
                    ranges.forEach { range ->
                        result.add(
                            range.copy(
                                start = range.start + offset,
                                end = range.end + offset
                            )
                        )
                    }
                }
                result
            }

            val onUpdateCombinedRanges: (List<StyledRange>) -> Unit = { newCombinedRanges ->
                val newMap = mutableMapOf<Int, List<StyledRange>>()
                article.paragraphs.forEachIndexed { pIdx, paragraph ->
                    val offset = paragraphOffsets[pIdx]
                    val pLen = paragraph.text.length
                    val pStartInCombined = offset
                    val pEndInCombined = offset + pLen
                    
                    val pRanges = mutableListOf<StyledRange>()
                    newCombinedRanges.forEach { range ->
                        val intersectStart = maxOf(range.start, pStartInCombined)
                        val intersectEnd = minOf(range.end, pEndInCombined)
                        if (intersectStart < intersectEnd) {
                            pRanges.add(
                                range.copy(
                                    start = intersectStart - offset,
                                    end = intersectEnd - offset
                                )
                            )
                        }
                    }
                    if (pRanges.isNotEmpty()) {
                        newMap[pIdx] = pRanges
                    }
                }
                localEdits = newMap
                val json = BookmarkEditsParser.toJson(newMap)
                lastSavedJson = json
                onSaveEdits?.invoke(json)
            }

            val paragraphsContent = @Composable {
                if (isEditable) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SegmentedTextWithEdits(
                            segments = combinedSegments,
                            ranges = combinedRanges,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF0F172A),
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Medium,
                            onArticleClick = onArticleClick,
                            onUpdateRanges = onUpdateCombinedRanges,
                            selectedMarkerColorHex = selectedMarkerColorHex,
                            selectedUnderlineColorHex = selectedUnderlineColorHex,
                            onSelectedMarkerColorChange = { color ->
                                onActiveToolChange?.invoke(Constants.TOOL_MARKER)
                                onColorHexChange?.invoke(color)
                            },
                            onSelectedUnderlineColorChange = { color ->
                                onActiveToolChange?.invoke(Constants.TOOL_UNDERLINE)
                                onColorHexChange?.invoke(color)
                            },
                            activeTool = if (isCurrentlyEditing) activeTool else Constants.TOOL_NONE,
                            selectedColorHex = selectedColorHex,
                            fullArticleTextToCopy = fullArticleTextToCopy
                        )
                        val allNotes = remember(article.paragraphs) {
                            article.paragraphs.flatMap { it.notes }
                        }
                        if (allNotes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            allNotes.forEach { note ->
                                NoteCard(note = note, onArticleClick = onArticleClick)
                            }
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        article.paragraphs.forEachIndexed { pIdx, paragraph ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val paragraphRanges = remember(localEdits, pIdx) {
                                    localEdits[pIdx] ?: emptyList()
                                }
                                SegmentedTextWithEdits(
                                    segments = paragraph.content,
                                    ranges = paragraphRanges,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF0F172A),
                                    lineHeight = 24.sp,
                                    fontWeight = FontWeight.Medium,
                                    onArticleClick = onArticleClick,
                                    onUpdateRanges = null,
                                    selectedMarkerColorHex = selectedMarkerColorHex,
                                    selectedUnderlineColorHex = selectedUnderlineColorHex,
                                    onSelectedMarkerColorChange = null,
                                    onSelectedUnderlineColorChange = null,
                                    fullArticleTextToCopy = fullArticleTextToCopy
                                )
                                paragraph.notes.forEach { note ->
                                    NoteCard(note = note, onArticleClick = onArticleClick)
                                }
                            }
                        }
                    }
                }
            }

            if (isEditable) {
                // When isEditable is true, the text is rendered with BasicTextField (via SegmentedTextWithEdits)
                // which handles its own selection handles and toolbar. Wrapping in SelectionContainer
                // would cause selection handle conflicts or duplicate floating menus.
                paragraphsContent()
            } else {
                SelectionContainer {
                    paragraphsContent()
                }
            }
        }
    }
}

// Vector-based high-fidelity Coat of Arms of Ukraine (Герб України/Тризуб)
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

class ProceduralAnthemSynth {
    private var audioTrack: android.media.AudioTrack? = null
    private var isPlaying = false

    fun play(onComplete: () -> Unit) {
        if (isPlaying) return
        isPlaying = true
        Thread {
            try {
                val sampleRate = 22050
                val minBufferSize = android.media.AudioTrack.getMinBufferSize(
                    sampleRate,
                    android.media.AudioFormat.CHANNEL_OUT_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT
                )
                val track = android.media.AudioTrack(
                    android.media.AudioManager.STREAM_MUSIC,
                    sampleRate,
                    android.media.AudioFormat.CHANNEL_OUT_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize.coerceAtLeast(4096),
                    android.media.AudioTrack.MODE_STREAM
                )
                audioTrack = track
                track.play()

                // Melody of the anthem of Ukraine: frequency in Hz, duration in ms
                // BPM = 90. Quarter note = 667ms
                val quarter = 667
                val half = 1333
                val eighth = 333

                val melody = listOf(
                    // "Shche ne vmerla Ukrainy"
                    Pair(369.99f, eighth),  // F#4
                    Pair(392.00f, eighth),  // G4
                    Pair(440.00f, quarter), // A4
                    Pair(493.88f, eighth),  // B4
                    Pair(440.00f, quarter), // A4
                    Pair(392.00f, eighth),  // G4
                    Pair(369.99f, quarter), // F#4
                    Pair(329.63f, eighth),  // E4
                    Pair(293.66f, half),    // D4
                    
                    // "i slava, i volia"
                    Pair(369.99f, eighth),  // F#4
                    Pair(392.00f, eighth),  // G4
                    Pair(440.00f, quarter), // A4
                    Pair(493.88f, eighth),  // B4
                    Pair(440.00f, quarter), // A4
                    Pair(392.00f, eighth),  // G4
                    Pair(493.88f, half),    // B4
                    Pair(440.00f, half),    // A4
                    
                    // "Shche nam, brattia molodii"
                    Pair(369.99f, eighth),  // F#4
                    Pair(392.00f, eighth),  // G4
                    Pair(440.00f, quarter), // A4
                    Pair(493.88f, eighth),  // B4
                    Pair(440.00f, quarter), // A4
                    Pair(392.00f, eighth),  // G4
                    Pair(369.99f, quarter), // F#4
                    Pair(329.63f, eighth),  // E4
                    Pair(293.66f, half),    // D4
                    
                    // "usmiknetsia dolia."
                    Pair(369.99f, eighth),  // F#4
                    Pair(392.00f, eighth),  // G4
                    Pair(440.00f, quarter), // A4
                    Pair(329.63f, eighth),  // E4
                    Pair(369.99f, quarter), // F#4
                    Pair(329.63f, eighth),  // E4
                    Pair(293.66f, half),    // D4
                    
                    // Chorus: "Zhynut nashi vorizhenky" (chorus)
                    Pair(440.00f, quarter), // A4
                    Pair(440.00f, eighth),  // A4
                    Pair(493.88f, quarter), // B4
                    Pair(493.88f, eighth),  // B4
                    Pair(523.25f, quarter), // C5
                    Pair(523.25f, eighth),  // C5
                    Pair(493.88f, half),    // B4
                    
                    // "yak rosa na sontsi"
                    Pair(440.00f, eighth),  // A4
                    Pair(392.00f, eighth),  // G4
                    Pair(369.99f, quarter), // F#4
                    Pair(329.63f, eighth),  // E4
                    Pair(293.66f, quarter), // D4
                    Pair(329.63f, eighth),  // E4
                    Pair(369.99f, half),    // F#4
                    Pair(440.00f, half)     // A4
                )

                for (note in melody) {
                    if (!isPlaying) break
                    val freq = note.first
                    val dur = note.second
                    
                    val numSamples = (dur * sampleRate) / 1000
                    val samples = ShortArray(numSamples)
                    
                    for (i in 0 until numSamples) {
                        val t = i.toDouble() / sampleRate
                        var s = Math.sin(2.0 * Math.PI * freq * t)
                        
                        val envelope = when {
                            i < sampleRate * 0.05 -> i / (sampleRate * 0.05) // Attack (50ms)
                            i > numSamples - sampleRate * 0.05 -> (numSamples - i) / (sampleRate * 0.05) // Release (50ms)
                            else -> 1.0
                        }
                        
                        s += 0.3 * Math.sin(2.0 * Math.PI * (freq * 2.0) * t)
                        samples[i] = (s * 10000.0 * envelope).toInt().toShort()
                    }
                    track.write(samples, 0, numSamples)
                    
                    val gapSamples = (40 * sampleRate) / 1000
                    val gap = ShortArray(gapSamples)
                    track.write(gap, 0, gapSamples)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isPlaying = false
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (e: Exception) {}
                audioTrack = null
                onComplete()
            }
        }.start()
    }

    fun stop() {
        isPlaying = false
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {}
        audioTrack = null
    }
}

enum class AnthemVersion(
    val titleRes: Int,
    val performerRes: Int,
    val type: String,
    val source: String
) {
    OFFICIAL(R.string.anthem_title, R.string.orchestra_desc, "local", "anthem")
}

@Composable
fun YouTubePlayerWebView(
    videoId: String,
    isPlaying: Boolean,
    onStateChanged: (Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onError: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val currentOnStateChanged by rememberUpdatedState(onStateChanged)
    val currentOnError by rememberUpdatedState(onError)
    val webView = remember(context) { WebView(context) }
    
    // Create the HTML with full YouTube iFrame API controls and a state bridge
    val html = remember(videoId) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>body { margin: 0; padding: 0; background: transparent; overflow: hidden; width: 100%; height: 100%; }</style>
        </head>
        <body>
            <div id="player" style="width: 100%; height: 100%;"></div>
            <script>
                var player = null;
                var isReady = false;

                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('player', {
                        height: '100%',
                        width: '100%',
                        videoId: '$videoId',
                        playerVars: {
                            'autoplay': 1,
                            'controls': 0,
                            'modestbranding': 1,
                            'rel': 0,
                            'playsinline': 1,
                            'origin': 'https://www.youtube.com'
                        },
                        events: {
                            'onReady': onPlayerReady,
                            'onStateChange': onPlayerStateChange,
                            'onError': onPlayerError
                        }
                    });
                }

                function onPlayerReady(event) {
                    isReady = true;
                    AndroidBridge.onPageLoaded();
                    if ($isPlaying) {
                        event.target.playVideo();
                    }
                }

                function onPlayerStateChange(event) {
                    // States: -1 (unstarted), 0 (ended), 1 (playing), 2 (paused), 3 (buffering), 5 (video cued)
                    if (event.data === 1) {
                        AndroidBridge.onPlaying();
                    } else if (event.data === 2 || event.data === -1 || event.data === 5) {
                        AndroidBridge.onPaused();
                    } else if (event.data === 3) {
                        AndroidBridge.onBuffering();
                    } else if (event.data === 0) {
                        AndroidBridge.onEnded();
                    }
                }

                function onPlayerError() {
                    AndroidBridge.onPlayerPlaybackError();
                }

                function play() {
                    if (player && player.playVideo) { player.playVideo(); }
                }

                function pause() {
                    if (player && player.pauseVideo) { player.pauseVideo(); }
                }

                // Append the script tag only AFTER defining target listeners to avoid race conditions
                var tag = document.createElement('script');
                tag.src = "https://www.youtube.com/iframe_api";
                var firstScriptTag = document.getElementsByTagName('script')[0];
                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    var isWebPlayerReady by remember { mutableStateOf(false) }

    val bridge = remember {
        object {
            @android.webkit.JavascriptInterface
            fun onPageLoaded() {
                handler.post { isWebPlayerReady = true }
            }
            @android.webkit.JavascriptInterface
            fun onPlaying() {
                handler.post { currentOnStateChanged(true, false) }
            }
            @android.webkit.JavascriptInterface
            fun onPaused() {
                handler.post { currentOnStateChanged(false, false) }
            }
            @android.webkit.JavascriptInterface
            fun onBuffering() {
                handler.post { currentOnStateChanged(false, true) }
            }
            @android.webkit.JavascriptInterface
            fun onEnded() {
                handler.post { currentOnStateChanged(false, false) }
            }
            @android.webkit.JavascriptInterface
            fun onPlayerPlaybackError() {
                handler.post {
                    currentOnError?.invoke()
                }
            }
        }
    }

    AndroidView(
        factory = {
            webView.apply {
                settings.apply {
                    javaScriptEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    domStorageEnabled = true
                    userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
                }
                addJavascriptInterface(bridge, "AndroidBridge")
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
            }
        },
        modifier = modifier,
        update = {}
    )

    // Trigger Play/Pause reactively once the YouTube player is fully loaded and ready
    LaunchedEffect(isPlaying, isWebPlayerReady, videoId) {
        if (isWebPlayerReady) {
            try {
                if (isPlaying) {
                    webView.evaluateJavascript("play();", null)
                } else {
                    webView.evaluateJavascript("pause();", null)
                }
            } catch (e: Exception) {
                // Safe ignore if webview is in invalid state
            }
        }
    }

    // Load the HTML inside the web view
    LaunchedEffect(videoId, webView) {
        isWebPlayerReady = false
        try {
            webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            // Safe ignore
        }
    }

    DisposableEffect(webView) {
        onDispose {
            try {
                (webView.parent as? android.view.ViewGroup)?.removeView(webView)
                webView.stopLoading()
                webView.destroy()
            } catch (e: Exception) {
                // Safe ignore
            }
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
    bookmarksList: List<com.example.data.database.BookmarkEntity>,
    onNavigateToArticle: (Article, Article?) -> Unit,
    onOpenFullscreenSymbol: (FullscreenSymbol) -> Unit
) {
    val context = LocalContext.current
    
    // Choose the Article of the Day deterministically based on date seed with safe fallback
    val todayArticle = remember {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        if (ConstitutionData.articles.isNotEmpty()) {
            ConstitutionData.articles[dayOfYear % ConstitutionData.articles.size]
        } else {
            Article(1, 1, "${context.getString(R.string.article_label)} 1", listOf(Paragraph(listOf(com.example.data.model.ContentSegment("text", value = context.getString(R.string.article_1_fallback_content))), emptyList())))
        }
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
                        initialEditsJson = bookmarksList.find { it.articleId == todayArticle.bookmarkId }?.editsJson ?: ""
                    )
                }
            }
        }
    }

        // Old InvisibleYouTubePlayer replaced by UnifiedAnthemPlayer at the outer Box
    }

}

data class StyledRange(
    val start: Int,
    val end: Int,
    val colorHex: String,
    val highlight: Boolean,
    val underscore: Boolean,
    val highlightColorHex: String = colorHex,
    val underscoreColorHex: String = colorHex
)

class ParagraphRegistryEntry(
    val paragraphIndex: Int,
    val paragraphText: String,
    val formToOrigMapping: IntArray,
    val getLayoutCoordinates: () -> androidx.compose.ui.layout.LayoutCoordinates?,
    val getTextLayoutResult: () -> androidx.compose.ui.text.TextLayoutResult?,
    val applyStyleToRanges: (Collection<Pair<Int, Int>>, String, String) -> Unit
)

object BookmarkEditsParser {
    fun parse(jsonStr: String?): Map<Int, List<StyledRange>> {
        val result = mutableMapOf<Int, List<StyledRange>>()
        if (jsonStr.isNullOrBlank()) return result
        try {
            val root = org.json.JSONObject(jsonStr)
            val paragraphEdits = root.optJSONObject("paragraphEdits") ?: return result
            val keys = paragraphEdits.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val paragraphIndex = key.toIntOrNull() ?: continue
                val arr = paragraphEdits.optJSONArray(key) ?: continue
                val ranges = mutableListOf<StyledRange>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val start = obj.getInt("start")
                    val end = obj.getInt("end")
                    val colorHex = obj.getString("colorHex")
                    val highlight = obj.getBoolean("highlight")
                    val underscore = obj.getBoolean("underscore")
                    val highlightColorHex = obj.optString("highlightColorHex", colorHex)
                    val underscoreColorHex = obj.optString("underscoreColorHex", colorHex)
                    ranges.add(
                        StyledRange(
                            start = start,
                            end = end,
                            colorHex = colorHex,
                            highlight = highlight,
                            underscore = underscore,
                            highlightColorHex = highlightColorHex,
                            underscoreColorHex = underscoreColorHex
                        )
                    )
                }
                result[paragraphIndex] = ranges
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun toJson(edits: Map<Int, List<StyledRange>>): String {
        try {
            val root = org.json.JSONObject()
            val paragraphEdits = org.json.JSONObject()
            for ((paragraphIndex, ranges) in edits) {
                if (ranges.isEmpty()) continue
                val arr = org.json.JSONArray()
                for (range in ranges) {
                    val obj = org.json.JSONObject()
                    obj.put("start", range.start)
                    obj.put("end", range.end)
                    obj.put("colorHex", range.colorHex)
                    obj.put("highlight", range.highlight)
                    obj.put("underscore", range.underscore)
                    obj.put("highlightColorHex", range.highlightColorHex)
                    obj.put("underscoreColorHex", range.underscoreColorHex)
                    arr.put(obj)
                }
                paragraphEdits.put(paragraphIndex.toString(), arr)
            }
            root.put("paragraphEdits", paragraphEdits)
            return root.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
}

fun getWordRangeAtOffset(text: String, offset: Int): Pair<Int, Int>? {
    if (offset < 0 || offset >= text.length) return null
    if (text[offset].isWhitespace()) return null
    
    var start = offset
    while (start > 0 && !text[start - 1].isWhitespace()) {
        start--
    }
    
    var end = offset
    while (end < text.length && !text[end].isWhitespace()) {
        end++
    }
    
    // Trim leading punctuation
    while (start < end && !text[start].isLetterOrDigit()) {
        start++
    }
    
    // Trim trailing punctuation
    while (end > start && !text[end - 1].isLetterOrDigit()) {
        end--
    }
    
    return if (start < end) Pair(start, end) else null
}

fun getWordSnappedRange(text: String, offset1: Int, offset2: Int): Pair<Int, Int>? {
    val o1 = offset1.coerceIn(0, (text.length - 1).coerceAtLeast(0))
    val o2 = offset2.coerceIn(0, (text.length - 1).coerceAtLeast(0))
    val minO = minOf(o1, o2)
    val maxO = maxOf(o1, o2)
    
    var finalStart = minO
    val startWord = getWordRangeAtOffset(text, minO)
    if (startWord != null) {
        finalStart = startWord.first
    } else {
        var found = false
        for (i in minO until text.length) {
            val word = getWordRangeAtOffset(text, i)
            if (word != null) {
                finalStart = word.first
                found = true
                break
            }
        }
        if (!found) {
            for (i in minO downTo 0) {
                val word = getWordRangeAtOffset(text, i)
                if (word != null) {
                    finalStart = word.first
                    break
                }
            }
        }
    }
    
    var finalEnd = maxO
    val endWord = getWordRangeAtOffset(text, maxO)
    if (endWord != null) {
        finalEnd = endWord.second
    } else {
        var found = false
        for (i in maxO downTo 0) {
            val word = getWordRangeAtOffset(text, i)
            if (word != null) {
                finalEnd = word.second
                found = true
                break
            }
        }
        if (!found) {
            for (i in maxO until text.length) {
                val word = getWordRangeAtOffset(text, i)
                if (word != null) {
                    finalEnd = word.second
                    break
                }
            }
        }
    }
    
    if (finalStart < finalEnd) {
        return Pair(finalStart, finalEnd)
    }
    return null
}

fun mergeAdjacentStyledRanges(text: String, ranges: List<StyledRange>): List<StyledRange> {
    if (ranges.size <= 1) return ranges
    
    val sorted = ranges.sortedBy { it.start }
    val result = mutableListOf<StyledRange>()
    
    for (range in sorted) {
        if (result.isEmpty()) {
            result.add(range)
        } else {
            val last = result.last()
            val sameSpec = last.highlight == range.highlight && 
                           last.underscore == range.underscore && 
                           last.highlightColorHex.lowercase() == range.highlightColorHex.lowercase() && 
                           last.underscoreColorHex.lowercase() == range.underscoreColorHex.lowercase() && 
                           last.colorHex.lowercase() == range.colorHex.lowercase()
            
            if (sameSpec) {
                val canMerge = if (range.start <= last.end) {
                    true
                } else {
                    val intermediateText = text.substring(last.end, range.start)
                    intermediateText.all { it.isWhitespace() || !it.isLetterOrDigit() }
                }
                
                if (canMerge) {
                    result[result.size - 1] = last.copy(
                        start = last.start,
                        end = maxOf(last.end, range.end)
                    )
                } else {
                    result.add(range)
                }
            } else {
                result.add(range)
            }
        }
    }
    return result
}

@Composable
fun SegmentedTextWithEdits(
    segments: List<com.example.data.model.ContentSegment>,
    ranges: List<StyledRange>,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color(0xFF0F172A),
    lineHeight: androidx.compose.ui.unit.TextUnit = 24.sp,
    fontWeight: FontWeight = FontWeight.Medium,
    fontStyle: FontStyle? = null,
    onArticleClick: ((Article) -> Unit)? = null,
    onUpdateRanges: ((List<StyledRange>) -> Unit)? = null,
    selectedMarkerColorHex: String = "#FFF59D",
    selectedUnderlineColorHex: String = "#F57F17",
    onSelectedMarkerColorChange: ((String) -> Unit)? = null,
    onSelectedUnderlineColorChange: ((String) -> Unit)? = null,
    activeTool: String = Constants.TOOL_MARKER,
    selectedColorHex: String = "#FFF59D",
    fullArticleTextToCopy: String? = null
) {
    val context = LocalContext.current
    var textLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    
    val mergedSegments = remember(segments) {
        val result = mutableListOf<com.example.data.model.ContentSegment>()
        var currentLink: com.example.data.model.ContentSegment? = null
        for (segment in segments) {
            if (segment.type == "link") {
                if (currentLink != null && currentLink.url == segment.url) {
                    currentLink = currentLink.copy(text = currentLink.text + segment.text)
                } else {
                    if (currentLink != null) {
                        result.add(currentLink)
                    }
                    currentLink = segment
                }
            } else {
                if (currentLink != null) {
                    result.add(currentLink)
                    currentLink = null
                }
                result.add(segment)
            }
        }
        if (currentLink != null) {
            result.add(currentLink)
        }
        result
    }

    val originalText = remember(mergedSegments) {
        mergedSegments.joinToString("") { if (it.type == "link") it.text else it.value }
    }

    val formattedText = remember(originalText) {
        formatStringToSuperscript(originalText)
    }

    val origToFormMapping = remember(originalText, formattedText) {
        mapOriginalToFormatted(originalText, formattedText)
    }

    val formToOrigMapping = remember(originalText, formattedText) {
        mapFormattedToOriginal(originalText, formattedText)
    }

    val annotatedString = remember(mergedSegments, formattedText, origToFormMapping) {
        buildAnnotatedString {
            var originalOffset = 0
            mergedSegments.forEach { segment ->
                val segLen = (if (segment.type == "link") segment.text else segment.value).length
                val startOrig = originalOffset
                val endOrig = originalOffset + segLen
                
                val startForm = origToFormMapping.getOrElse(startOrig) { startOrig }.coerceIn(0, formattedText.length)
                val endForm = origToFormMapping.getOrElse(endOrig) { endOrig }.coerceIn(0, formattedText.length)
                
                val textToAppend = formattedText.substring(startForm, endForm)
                
                if (segment.type == "link") {
                    pushStringAnnotation(tag = "URL", annotation = "${segment.url}|${segment.text}")
                    withStyle(
                        style = SpanStyle(
                            color = Color(0xFF0D47A1),
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(textToAppend)
                    }
                    pop()
                } else {
                    append(textToAppend)
                }
                originalOffset = endOrig
            }
        }
    }

    val markerColors = Constants.MARKER_COLORS
    val underlineColors = Constants.UNDERLINE_COLORS

    val lastMarkerColor = selectedMarkerColorHex
    val lastUnderlineColor = selectedUnderlineColorHex

    val eraserIcon = remember {
        androidx.compose.ui.graphics.vector.ImageVector.Builder(
            name = "Eraser",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = androidx.compose.ui.graphics.SolidColor(Color.White),
                stroke = null,
                strokeLineWidth = 1f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Butt,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Miter,
                strokeLineMiter = 1f
            ) {
                moveTo(16.24f, 3.56f)
                lineTo(21.19f, 8.51f)
                curveTo(21.97f, 9.29f, 21.97f, 10.56f, 21.19f, 11.34f)
                lineTo(14.12f, 18.41f)
                lineTo(9.17f, 13.46f)
                lineTo(16.24f, 3.56f)
                close()
                moveTo(7.76f, 14.88f)
                lineTo(12.71f, 19.83f)
                lineTo(5.64f, 21.0f)
                lineTo(2.0f, 21.0f)
                lineTo(7.76f, 14.88f)
                close()
            }
        }.build()
    }

    val applyStyleToRanges = remember(originalText, ranges, onUpdateRanges) {
        { wordRanges: Collection<Pair<Int, Int>>, tool: String, colorHex: String ->
            val len = originalText.length
            val charHighlight = BooleanArray(len)
            val charHighlightColor = Array(len) { "#FFFFFF" }
            val charUnderscore = BooleanArray(len)
            val charUnderscoreColor = Array(len) { "#FFFFFF" }
            
            // Populate arrays with existing ranges
            ranges.forEach { r ->
                for (i in r.start until r.end) {
                    if (i in 0 until len) {
                        if (r.highlight) {
                            charHighlight[i] = true
                            charHighlightColor[i] = r.highlightColorHex
                        }
                        if (r.underscore) {
                            charUnderscore[i] = true
                            charUnderscoreColor[i] = r.underscoreColorHex
                        }
                    }
                }
            }
            
            // Modify all specified ranges together!
            wordRanges.forEach { (start, end) ->
                for (i in start until end) {
                    if (i in 0 until len) {
                        when (tool) {
                            Constants.TOOL_MARKER -> {
                                charHighlight[i] = true
                                charHighlightColor[i] = colorHex
                            }
                            Constants.TOOL_UNDERLINE -> {
                                charUnderscore[i] = true
                                charUnderscoreColor[i] = colorHex
                            }
                            Constants.TOOL_ERASER -> {
                                charHighlight[i] = false
                                charUnderscore[i] = false
                            }
                        }
                    }
                }
            }

            // Post-process to fill styling gaps (spaces/punctuation between adjacent words styled with identical colors)
            var lastHighlightIdx = -1
            for (i in 0 until len) {
                if (charHighlight[i]) {
                    if (lastHighlightIdx != -1 && lastHighlightIdx < i - 1) {
                        val color1 = charHighlightColor[lastHighlightIdx].lowercase()
                        val color2 = charHighlightColor[i].lowercase()
                        if (color1 == color2) {
                            var onlyGapChars = true
                            for (j in (lastHighlightIdx + 1) until i) {
                                val char = originalText[j]
                                if (char.isLetterOrDigit()) {
                                    onlyGapChars = false
                                    break
                                }
                            }
                            if (onlyGapChars) {
                                for (j in (lastHighlightIdx + 1) until i) {
                                    charHighlight[j] = true
                                    charHighlightColor[j] = charHighlightColor[lastHighlightIdx]
                                }
                            }
                        }
                    }
                    lastHighlightIdx = i
                }
            }

            var lastUnderscoreIdx = -1
            for (i in 0 until len) {
                if (charUnderscore[i]) {
                    if (lastUnderscoreIdx != -1 && lastUnderscoreIdx < i - 1) {
                        val color1 = charUnderscoreColor[lastUnderscoreIdx].lowercase()
                        val color2 = charUnderscoreColor[i].lowercase()
                        if (color1 == color2) {
                            var onlyGapChars = true
                            for (j in (lastUnderscoreIdx + 1) until i) {
                                val char = originalText[j]
                                if (char.isLetterOrDigit()) {
                                    onlyGapChars = false
                                    break
                                }
                            }
                            if (onlyGapChars) {
                                for (j in (lastUnderscoreIdx + 1) until i) {
                                    charUnderscore[j] = true
                                    charUnderscoreColor[j] = charUnderscoreColor[lastUnderscoreIdx]
                                }
                            }
                        }
                    }
                    lastUnderscoreIdx = i
                }
            }
            
            // Convert back to structured StyledRanges with trimmer
            val newRanges = mutableListOf<StyledRange>()
            fun addRangeWithCheck(s: Int, e: Int, highlight: Boolean, hc: String, underscore: Boolean, uc: String) {
                if (s < e) {
                    val sub = originalText.substring(s, e)
                    if (sub.any { !it.isWhitespace() }) {
                        newRanges.add(
                            StyledRange(
                                start = s,
                                end = e,
                                colorHex = if (highlight) hc else uc,
                                highlight = highlight,
                                underscore = underscore,
                                highlightColorHex = hc,
                                underscoreColorHex = uc
                            )
                        )
                    }
                }
            }

            var currentStart = -1
            var currentHighlight = false
            var currentHighlightColor = "#FFFFFF"
            var currentUnderscore = false
            var currentUnderscoreColor = "#FFFFFF"

            for (i in 0 until len) {
                val h = charHighlight[i]
                val hc = charHighlightColor[i]
                val u = charUnderscore[i]
                val uc = charUnderscoreColor[i]

                val hasStyle = h || u
                val matchesCurrent = currentStart != -1 && 
                                     currentHighlight == h && 
                                     currentHighlightColor.lowercase() == hc.lowercase() && 
                                     currentUnderscore == u && 
                                     currentUnderscoreColor.lowercase() == uc.lowercase()

                if (hasStyle) {
                    if (currentStart == -1) {
                        currentStart = i
                        currentHighlight = h
                        currentHighlightColor = hc
                        currentUnderscore = u
                        currentUnderscoreColor = uc
                    } else if (!matchesCurrent) {
                        addRangeWithCheck(
                            currentStart,
                            i,
                            currentHighlight,
                            currentHighlightColor,
                            currentUnderscore,
                            currentUnderscoreColor
                        )
                        currentStart = i
                        currentHighlight = h
                        currentHighlightColor = hc
                        currentUnderscore = u
                        currentUnderscoreColor = uc
                    }
                } else {
                    if (currentStart != -1) {
                        addRangeWithCheck(
                            currentStart,
                            i,
                            currentHighlight,
                            currentHighlightColor,
                            currentUnderscore,
                            currentUnderscoreColor
                        )
                        currentStart = -1
                    }
                }
            }
            if (currentStart != -1) {
                addRangeWithCheck(
                    currentStart,
                    len,
                    currentHighlight,
                    currentHighlightColor,
                    currentUnderscore,
                    currentUnderscoreColor
                )
            }
            onUpdateRanges?.invoke(mergeAdjacentStyledRanges(originalText, newRanges))
        }
    }

    val applyStyleToRange = remember(applyStyleToRanges) {
        { start: Int, end: Int, tool: String, colorHex: String ->
            applyStyleToRanges(listOf(Pair(start, end)), tool, colorHex)
        }
    }

    var menuRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var menuCallbacks by remember { mutableStateOf<MenuCallbacks?>(null) }

    val customTextToolbar = remember {
        object : androidx.compose.ui.platform.TextToolbar {
            override val status: androidx.compose.ui.platform.TextToolbarStatus
                get() = if (menuRect != null) androidx.compose.ui.platform.TextToolbarStatus.Shown else androidx.compose.ui.platform.TextToolbarStatus.Hidden

            override fun hide() {
                android.util.Log.d(LogMessages.TAG_SELECTION_BUG, LogMessages.TOOLBAR_HIDE_CALLED)
                menuRect = null
                menuCallbacks = null
            }

            override fun showMenu(
                rect: androidx.compose.ui.geometry.Rect,
                onCopyRequested: (() -> Unit)?,
                onPasteRequested: (() -> Unit)?,
                onCutRequested: (() -> Unit)?,
                onSelectAllRequested: (() -> Unit)?
            ) {
                android.util.Log.d(LogMessages.TAG_SELECTION_BUG, LogMessages.toolbarShowMenu(rect, rect.height, rect.width))
                menuRect = rect
                menuCallbacks = MenuCallbacks(onCopyRequested, onSelectAllRequested)
            }
        }
    }

    val isStylingToolActive = activeTool == Constants.TOOL_MARKER || activeTool == Constants.TOOL_UNDERLINE || activeTool == Constants.TOOL_ERASER

    val focusRequester = remember { FocusRequester() }

    var textFieldValue by remember {
        mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(annotatedString))
    }

    androidx.compose.runtime.LaunchedEffect(annotatedString) {
        textFieldValue = textFieldValue.copy(annotatedString = annotatedString)
    }

    val currentActiveTool by androidx.compose.runtime.rememberUpdatedState(activeTool)
    val currentSelectedColorHex by androidx.compose.runtime.rememberUpdatedState(selectedColorHex)
    val currentApplyStyleToRanges by androidx.compose.runtime.rememberUpdatedState(applyStyleToRanges)

    val currentLocalTextToolbar = androidx.compose.ui.platform.LocalTextToolbar.current
    val finalLocalTextToolbar = if (onUpdateRanges != null) customTextToolbar else currentLocalTextToolbar

    if (onUpdateRanges != null) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalTextToolbar provides finalLocalTextToolbar
        ) {
            Box(modifier = modifier) {
                androidx.compose.foundation.text.BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        android.util.Log.d(LogMessages.TAG_SELECTION_BUG, LogMessages.toolbarValueChange(newValue.selection, newValue.selection.collapsed))
                        textFieldValue = newValue.copy(annotatedString = annotatedString)
                        if (newValue.selection.collapsed) {
                            val clickedOffset = newValue.selection.start
                            annotatedString.getStringAnnotations(tag = Constants.ANNOTATION_TAG_URL, start = clickedOffset, end = clickedOffset)
                                .firstOrNull()?.let { annotation ->
                                    try {
                                        val parts = annotation.item.split("|")
                                        val clickedUrl = parts.getOrNull(0) ?: ""
                                        val segmentText = parts.getOrNull(1) ?: ""

                                        var articleNavigated = false
                                        if (clickedUrl.startsWith("#") || (!clickedUrl.startsWith("http://") && !clickedUrl.startsWith("https://"))) {
                                            val targetArticle = findArticleByLink(segmentText)
                                            if (targetArticle != null && onArticleClick != null) {
                                                onArticleClick(targetArticle)
                                                articleNavigated = true
                                            }
                                        }

                                        if (!articleNavigated) {
                                            val finalUrl = if (clickedUrl.startsWith("#")) {
                                                "${Constants.DEFAULT_RADA_URL}$clickedUrl"
                                            } else {
                                                clickedUrl
                                            }
                                            if (finalUrl.isNotEmpty()) {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                                                context.startActivity(intent)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e(LogMessages.TAG_SEGMENTED_TEXT_EDITS, LogMessages.openUrlFailed(annotation.item, e.message), e)
                                    }
                                }
                        }
                    },
                    readOnly = true,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.Transparent),
                    textStyle = style.copy(
                        color = color,
                        lineHeight = lineHeight,
                        fontWeight = fontWeight,
                        fontStyle = fontStyle ?: style.fontStyle,
                        textAlign = TextAlign.Start
                    ),
                    onTextLayout = { textLayoutResult = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .drawWithContent {
                            // Draw highlights first behind the text
                            try {
                                textLayoutResult?.let { layoutResult ->
                                    ranges.forEach { range ->
                                        if (range.highlight) {
                                            val colorVal = safeParseColor(range.highlightColorHex, Color.Yellow).copy(alpha = 0.85f)
                                            val safeStart = range.start.coerceIn(0, originalText.length)
                                            val safeEnd = range.end.coerceIn(0, originalText.length)
                                            val rawMappedStart = origToFormMapping.getOrElse(safeStart) { safeStart }
                                            val rawMappedEnd = origToFormMapping.getOrElse(safeEnd) { safeEnd }
                                            val textLen = layoutResult.layoutInput.text.length
                                            val mappedSelectStart = rawMappedStart.coerceIn(0, textLen)
                                            val mappedSelectEnd = rawMappedEnd.coerceIn(0, textLen)
                                            if (mappedSelectStart < mappedSelectEnd) {
                                                val startLine = layoutResult.getLineForOffset(mappedSelectStart)
                                                val endLine = layoutResult.getLineForOffset(maxOf(0, mappedSelectEnd - 1))
                                                for (line in startLine..endLine) {
                                                    val lineStart = layoutResult.getLineStart(line)
                                                    val lineEnd = layoutResult.getLineEnd(line)
                                                    val segmentStart = maxOf(mappedSelectStart, lineStart)
                                                    val segmentEnd = minOf(mappedSelectEnd, lineEnd)
                                                    if (segmentStart < segmentEnd) {
                                                        val left = if (segmentStart == lineStart) {
                                                            layoutResult.getLineLeft(line)
                                                        } else {
                                                            layoutResult.getHorizontalPosition(segmentStart, usePrimaryDirection = true)
                                                        }
                                                        val right = if (segmentEnd == lineEnd) {
                                                            layoutResult.getLineRight(line)
                                                        } else {
                                                            layoutResult.getHorizontalPosition(segmentEnd, usePrimaryDirection = true)
                                                        }
                                                        val topHeight = layoutResult.getLineTop(line)
                                                        val bottomHeight = layoutResult.getLineBottom(line)
                                                        drawRect(
                                                            color = colorVal,
                                                            topLeft = androidx.compose.ui.geometry.Offset(minOf(left, right), topHeight),
                                                            size = androidx.compose.ui.geometry.Size(kotlin.math.abs(right - left), bottomHeight - topHeight)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // ignore
                            }

                            drawContent()

                            // Draw underlines on top of the text
                            try {
                                textLayoutResult?.let { layoutResult ->
                                    ranges.forEach { range ->
                                        if (range.underscore) {
                                            val colorVal = safeParseColor(range.underscoreColorHex, Color.Red)
                                            val safeStart = range.start.coerceIn(0, originalText.length)
                                            val safeEnd = range.end.coerceIn(0, originalText.length)
                                            val rawMappedStart = origToFormMapping.getOrElse(safeStart) { safeStart }
                                            val rawMappedEnd = origToFormMapping.getOrElse(safeEnd) { safeEnd }
                                            val textLen = layoutResult.layoutInput.text.length
                                            val mappedSelectStart = rawMappedStart.coerceIn(0, textLen)
                                            val mappedSelectEnd = rawMappedEnd.coerceIn(0, textLen)
                                            if (mappedSelectStart < mappedSelectEnd) {
                                                val startLine = layoutResult.getLineForOffset(mappedSelectStart)
                                                val endLine = layoutResult.getLineForOffset(maxOf(0, mappedSelectEnd - 1))
                                                for (line in startLine..endLine) {
                                                    val lineStart = layoutResult.getLineStart(line)
                                                    val lineEnd = layoutResult.getLineEnd(line)
                                                    val segmentStart = maxOf(mappedSelectStart, lineStart)
                                                    val segmentEnd = minOf(mappedSelectEnd, lineEnd)
                                                    if (segmentStart < segmentEnd) {
                                                        val left = if (segmentStart == lineStart) {
                                                            layoutResult.getLineLeft(line)
                                                        } else {
                                                            layoutResult.getHorizontalPosition(segmentStart, usePrimaryDirection = true)
                                                        }
                                                        val right = if (segmentEnd == lineEnd) {
                                                            layoutResult.getLineRight(line)
                                                        } else {
                                                            layoutResult.getHorizontalPosition(segmentEnd, usePrimaryDirection = true)
                                                        }
                                                        val bottomHeight = layoutResult.getLineBottom(line)
                                                        val lineY = bottomHeight - 2.dp.toPx()
                                                        drawLine(
                                                            color = colorVal,
                                                            start = androidx.compose.ui.geometry.Offset(minOf(left, right), lineY),
                                                            end = androidx.compose.ui.geometry.Offset(maxOf(left, right), lineY),
                                                            strokeWidth = 2.dp.toPx()
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // ignore drawing errors to avoid crashing the Compose drawing thread
                            }
                        }
                )

                // High Z-index interactive overlay solely to capture and handle styling gestures (swiping over words)
                if (isStylingToolActive) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .pointerInput(annotatedString, activeTool, selectedColorHex) {
                                val touchSlop = viewConfiguration.touchSlop
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val tool = currentActiveTool
                                    val touchedWords = mutableSetOf<Pair<Int, Int>>()
                                    
                                    val startPosition = down.position
                                    val currentPointerId = down.id
                                    var hasDecidedGesture = false
                                    var isScrollingMode = false
                                    var isStylingMode = false

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val anyActive = event.changes.any { it.id == currentPointerId && it.pressed }
                                        if (!anyActive) {
                                            // User released finger
                                            if (!hasDecidedGesture) {
                                                // Treated as a single tap!
                                                textLayoutResult?.let { layoutResult ->
                                                    val dragPos = layoutResult.getOffsetForPosition(startPosition)
                                                    val origDragPos = formToOrigMapping.getOrElse(dragPos) { dragPos }
                                                    if (origDragPos in 0..originalText.length) {
                                                        getWordSnappedRange(originalText, origDragPos, origDragPos)?.let { snapped ->
                                                            if (touchedWords.add(snapped)) {
                                                                currentApplyStyleToRanges(touchedWords, tool, currentSelectedColorHex)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            break
                                        }

                                        val activeChange = event.changes.firstOrNull { it.id == currentPointerId }
                                        if (activeChange != null) {
                                            val currentPosition = activeChange.position
                                            val diffX = currentPosition.x - startPosition.x
                                            val diffY = currentPosition.y - startPosition.y

                                            if (!hasDecidedGesture) {
                                                val distSq = diffX * diffX + diffY * diffY
                                                if (distSq >= touchSlop * touchSlop) {
                                                    hasDecidedGesture = true
                                                    if (kotlin.math.abs(diffY) > kotlin.math.abs(diffX)) {
                                                        isScrollingMode = true
                                                        break
                                                    } else {
                                                        isStylingMode = true
                                                        activeChange.consume()

                                                        // Also apply styling to down position now that we know we are styling
                                                        textLayoutResult?.let { layoutResult ->
                                                            val dragPos = layoutResult.getOffsetForPosition(startPosition)
                                                            val origDragPos = formToOrigMapping.getOrElse(dragPos) { dragPos }
                                                            if (origDragPos in 0..originalText.length) {
                                                                getWordSnappedRange(originalText, origDragPos, origDragPos)?.let { snapped ->
                                                                    if (touchedWords.add(snapped)) {
                                                                        currentApplyStyleToRanges(touchedWords, tool, currentSelectedColorHex)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                if (isStylingMode) {
                                                    activeChange.consume()
                                                    textLayoutResult?.let { layoutResult ->
                                                        val dragPos = layoutResult.getOffsetForPosition(currentPosition)
                                                        val origDragPos = formToOrigMapping.getOrElse(dragPos) { dragPos }
                                                        if (origDragPos in 0..originalText.length) {
                                                            getWordSnappedRange(originalText, origDragPos, origDragPos)?.let { snapped ->
                                                                if (touchedWords.add(snapped)) {
                                                                    currentApplyStyleToRanges(touchedWords, tool, currentSelectedColorHex)
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
                    )
                }
            }
        }
    } else {
        androidx.compose.material3.Text(
            text = annotatedString,
            style = style.copy(
                color = color,
                lineHeight = lineHeight,
                fontWeight = fontWeight,
                fontStyle = fontStyle ?: style.fontStyle,
                textAlign = TextAlign.Start
            ),
            onTextLayout = { textLayoutResult = it },
            modifier = modifier
                .drawWithContent {
                    // Draw highlights first behind the text
                    try {
                        textLayoutResult?.let { layoutResult ->
                            ranges.forEach { range ->
                                if (range.highlight) {
                                    val colorVal = safeParseColor(range.highlightColorHex, Color.Yellow).copy(alpha = 0.85f)
                                    val safeStart = range.start.coerceIn(0, originalText.length)
                                    val safeEnd = range.end.coerceIn(0, originalText.length)
                                    val rawMappedStart = origToFormMapping.getOrElse(safeStart) { safeStart }
                                    val rawMappedEnd = origToFormMapping.getOrElse(safeEnd) { safeEnd }
                                    val textLen = layoutResult.layoutInput.text.length
                                    val mappedSelectStart = rawMappedStart.coerceIn(0, textLen)
                                    val mappedSelectEnd = rawMappedEnd.coerceIn(0, textLen)
                                    if (mappedSelectStart < mappedSelectEnd) {
                                        val startLine = layoutResult.getLineForOffset(mappedSelectStart)
                                        val endLine = layoutResult.getLineForOffset(maxOf(0, mappedSelectEnd - 1))
                                        for (line in startLine..endLine) {
                                            val lineStart = layoutResult.getLineStart(line)
                                            val lineEnd = layoutResult.getLineEnd(line)
                                            val segmentStart = maxOf(mappedSelectStart, lineStart)
                                            val segmentEnd = minOf(mappedSelectEnd, lineEnd)
                                            if (segmentStart < segmentEnd) {
                                                val left = if (segmentStart == lineStart) {
                                                    layoutResult.getLineLeft(line)
                                                } else {
                                                    layoutResult.getHorizontalPosition(segmentStart, usePrimaryDirection = true)
                                                }
                                                val right = if (segmentEnd == lineEnd) {
                                                    layoutResult.getLineRight(line)
                                                } else {
                                                    layoutResult.getHorizontalPosition(segmentEnd, usePrimaryDirection = true)
                                                }
                                                val topHeight = layoutResult.getLineTop(line)
                                                val bottomHeight = layoutResult.getLineBottom(line)
                                                drawRect(
                                                    color = colorVal,
                                                    topLeft = androidx.compose.ui.geometry.Offset(minOf(left, right), topHeight),
                                                    size = androidx.compose.ui.geometry.Size(kotlin.math.abs(right - left), bottomHeight - topHeight)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // ignore
                    }

                    drawContent()

                    // Draw underlines on top of the text
                    try {
                        textLayoutResult?.let { layoutResult ->
                            ranges.forEach { range ->
                                if (range.underscore) {
                                    val colorVal = safeParseColor(range.underscoreColorHex, Color.Red)
                                    val safeStart = range.start.coerceIn(0, originalText.length)
                                    val safeEnd = range.end.coerceIn(0, originalText.length)
                                    val rawMappedStart = origToFormMapping.getOrElse(safeStart) { safeStart }
                                    val rawMappedEnd = origToFormMapping.getOrElse(safeEnd) { safeEnd }
                                    val textLen = layoutResult.layoutInput.text.length
                                    val mappedSelectStart = rawMappedStart.coerceIn(0, textLen)
                                    val mappedSelectEnd = rawMappedEnd.coerceIn(0, textLen)
                                    if (mappedSelectStart < mappedSelectEnd) {
                                        val startLine = layoutResult.getLineForOffset(mappedSelectStart)
                                        val endLine = layoutResult.getLineForOffset(maxOf(0, mappedSelectEnd - 1))
                                        for (line in startLine..endLine) {
                                            val lineStart = layoutResult.getLineStart(line)
                                            val lineEnd = layoutResult.getLineEnd(line)
                                            val segmentStart = maxOf(mappedSelectStart, lineStart)
                                            val segmentEnd = minOf(mappedSelectEnd, lineEnd)
                                            if (segmentStart < segmentEnd) {
                                                val left = if (segmentStart == lineStart) {
                                                    layoutResult.getLineLeft(line)
                                                } else {
                                                    layoutResult.getHorizontalPosition(segmentStart, usePrimaryDirection = true)
                                                }
                                                val right = if (segmentEnd == lineEnd) {
                                                    layoutResult.getLineRight(line)
                                                } else {
                                                    layoutResult.getHorizontalPosition(segmentEnd, usePrimaryDirection = true)
                                                }
                                                val bottomHeight = layoutResult.getLineBottom(line)
                                                val lineY = bottomHeight - 2.dp.toPx()
                                                drawLine(
                                                    color = colorVal,
                                                    start = androidx.compose.ui.geometry.Offset(minOf(left, right), lineY),
                                                    end = androidx.compose.ui.geometry.Offset(maxOf(left, right), lineY),
                                                    strokeWidth = 2.dp.toPx()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // ignore drawing errors to avoid crashing the Compose drawing thread
                    }
                }
                .then(
                    Modifier.pointerInput(annotatedString) {
                        detectTapGestures(
                            onTap = { offset ->
                                textLayoutResult?.let { layoutResult ->
                                    val position = layoutResult.getOffsetForPosition(offset)
                                    if (position in 0..annotatedString.length) {
                                        annotatedString.getStringAnnotations(tag = "URL", start = position, end = position)
                                            .firstOrNull()?.let { annotation ->
                                                try {
                                                    val parts = annotation.item.split("|")
                                                    val clickedUrl = parts.getOrNull(0) ?: ""
                                                    val segmentText = parts.getOrNull(1) ?: ""

                                                    var articleNavigated = false
                                                    if (clickedUrl.startsWith("#") || (!clickedUrl.startsWith("http://") && !clickedUrl.startsWith("https://"))) {
                                                        val targetArticle = findArticleByLink(segmentText)
                                                        if (targetArticle != null && onArticleClick != null) {
                                                            onArticleClick(targetArticle)
                                                            articleNavigated = true
                                                        }
                                                    }

                                                    if (!articleNavigated) {
                                                        val finalUrl = if (clickedUrl.startsWith("#")) {
                                                            "${Constants.DEFAULT_RADA_URL}$clickedUrl"
                                                        } else {
                                                            clickedUrl
                                                        }
                                                        if (finalUrl.isNotEmpty()) {
                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                                                            context.startActivity(intent)
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    android.util.Log.e(LogMessages.TAG_SEGMENTED_TEXT_EDITS, LogMessages.openUrlFailed(annotation.item, e.message), e)
                                                }
                                            }
                                    }
                                }
                            }
                        )
                    }
                )
        )
    }

    val currentMenuRect = menuRect
    if (currentMenuRect != null && !textFieldValue.selection.collapsed) {
        val selStart = minOf(textFieldValue.selection.start, textFieldValue.selection.end)
        val selEnd = maxOf(textFieldValue.selection.start, textFieldValue.selection.end)
        
        val mappedStart = formToOrigMapping.getOrElse(selStart) { selStart }
        val mappedEnd = formToOrigMapping.getOrElse(selEnd) { selEnd }
        
        val hasEditsInSelection = remember(ranges, mappedStart, mappedEnd) {
            ranges.any { range -> range.start < mappedEnd && range.end > mappedStart }
        }
        var showedColorPickerMode by remember(currentMenuRect) { mutableStateOf<String?>(null) }

        val density = LocalDensity.current
        val popupPositionProvider = remember(currentMenuRect) {
            object : androidx.compose.ui.window.PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: androidx.compose.ui.unit.IntRect,
                    windowSize: androidx.compose.ui.unit.IntSize,
                    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                    popupContentSize: androidx.compose.ui.unit.IntSize
                ): androidx.compose.ui.unit.IntOffset {
                    val left = currentMenuRect.left
                    val right = currentMenuRect.right
                    val top = currentMenuRect.top
                    val bottom = currentMenuRect.bottom

                    val safeLeft = if (left.isNaN() || left.isInfinite()) 0f else left
                    val safeRight = if (right.isNaN() || right.isInfinite()) 0f else right
                    val safeTop = if (top.isNaN() || top.isInfinite()) 0f else top
                    val safeBottom = if (bottom.isNaN() || bottom.isInfinite()) 0f else bottom

                    val x = anchorBounds.left + (safeLeft + safeRight) / 2 - popupContentSize.width / 2
                    val buffer = with(density) { 60.dp.toPx() }
                    var y = anchorBounds.top + safeTop - popupContentSize.height - with(density) { 8.dp.toPx() }
                    
                    if (y < buffer) {
                        y = anchorBounds.top + safeBottom + with(density) { 8.dp.toPx() }
                    }
                    
                    val maxX = (windowSize.width - popupContentSize.width - 8).toFloat()
                    val finalX = if (8f >= maxX) 8f else x.coerceIn(8f, maxX)
                    
                    val maxY = (windowSize.height - popupContentSize.height - 8).toFloat()
                    val finalY = if (8f >= maxY) 8f else y.coerceIn(8f, maxY)
                    
                    return androidx.compose.ui.unit.IntOffset(finalX.toInt(), finalY.toInt())
                }
            }
        }

        androidx.compose.ui.window.Popup(
            popupPositionProvider = popupPositionProvider,
            onDismissRequest = {
                android.util.Log.d(LogMessages.TAG_SELECTION_BUG, LogMessages.POPUP_DISMISS_PRESERVE_SELECTION)
                menuRect = null
                menuCallbacks = null
            }
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E293B), // slate-800
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFF334155)), // slate-700
                modifier = Modifier.padding(2.dp)
            ) {
                if (showedColorPickerMode == Constants.TOOL_MARKER) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { showedColorPickerMode = null },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = stringResource(R.string.btn_back),
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(Color(0xFF475569)) // slate-600
                        )
                        markerColors.forEach { colorHex ->
                            val colorVal = safeParseColor(colorHex, Color.Yellow)
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(colorVal)
                                    .border(
                                        width = if (lastMarkerColor == colorHex) 2.dp else 1.dp,
                                        color = if (lastMarkerColor == colorHex) Color.White else Color.Gray.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        textFieldValue = textFieldValue.copy(selection = androidx.compose.ui.text.TextRange.Zero)
                                        menuRect = null
                                        menuCallbacks = null
                                        applyStyleToRange(mappedStart, mappedEnd, "MARKER", colorHex)
                                        onSelectedMarkerColorChange?.invoke(colorHex)
                                        showedColorPickerMode = null
                                    }
                            )
                        }
                    }
                } else if (showedColorPickerMode == "UNDERLINE") {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { showedColorPickerMode = null },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = stringResource(R.string.btn_back),
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(Color(0xFF475569)) // slate-600
                        )
                        underlineColors.forEach { colorHex ->
                            val colorVal = safeParseColor(colorHex, Color.Red)
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(colorVal)
                                    .border(
                                        width = if (lastUnderlineColor == colorHex) 2.dp else 1.dp,
                                        color = if (lastUnderlineColor == colorHex) Color.White else Color.Gray.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        textFieldValue = textFieldValue.copy(selection = androidx.compose.ui.text.TextRange.Zero)
                                        menuRect = null
                                        menuCallbacks = null
                                        applyStyleToRange(mappedStart, mappedEnd, "UNDERLINE", colorHex)
                                        onSelectedUnderlineColorChange?.invoke(colorHex)
                                        showedColorPickerMode = null
                                    }
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        menuCallbacks?.onCopy?.let { onCopy ->
                            IconButton(
                                onClick = {
                                    if (fullArticleTextToCopy != null && selStart == 0 && selEnd == textFieldValue.text.length) {
                                        try {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText(context.getString(R.string.article_label), fullArticleTextToCopy)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, context.getString(R.string.toast_article_copied), Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, context.getString(R.string.toast_copy_error, e.message ?: ""), Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        onCopy()
                                    }
                                    menuRect = null
                                    menuCallbacks = null
                                    textFieldValue = textFieldValue.copy(selection = androidx.compose.ui.text.TextRange.Zero)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.btn_copy),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val onSelectAll = menuCallbacks?.onSelectAll
                            if (onSelectAll != null) {
                                onSelectAll()
                            } else {
                                textFieldValue = textFieldValue.copy(
                                    selection = androidx.compose.ui.text.TextRange(0, textFieldValue.text.length)
                                )
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelectAll,
                            contentDescription = stringResource(R.string.btn_select_all),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    // Only show formatting options if edit callbacks are provided
                    if (onUpdateRanges != null) {
                        // Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(Color(0xFF475569)) // slate-600
                        )

                        // --- MARKER GROUP ---
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Marker Color Indicator dropdown slot
                            Box(contentAlignment = Alignment.Center) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { showedColorPickerMode = "MARKER" }
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                  ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(safeParseColor(lastMarkerColor, Color.Yellow))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            // Marker quick application text button
                            TextButtonWithIcon(
                                icon = Icons.Default.Brush,
                                iconTint = safeParseColor(lastMarkerColor, Color.Yellow),
                                text = stringResource(R.string.tool_marker),
                                onClick = {
                                    val targetStart = mappedStart
                                    val targetEnd = mappedEnd
                                    textFieldValue = textFieldValue.copy(selection = androidx.compose.ui.text.TextRange.Zero)
                                    menuRect = null
                                    menuCallbacks = null
                                    applyStyleToRange(targetStart, targetEnd, "MARKER", lastMarkerColor)
                                    onSelectedMarkerColorChange?.invoke(lastMarkerColor)
                                },
                                iconSize = 15.dp,
                                fontSize = 12.sp,
                                paddingHorizontal = 2.dp,
                                paddingVertical = 4.dp
                            )
                        }

                        // Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(Color(0xFF475569)) // slate-600
                        )

                        // --- UNDERLINE (LINE) GROUP ---
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Underline Color Indicator dropdown slot
                            Box(contentAlignment = Alignment.Center) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { showedColorPickerMode = "UNDERLINE" }
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(safeParseColor(lastUnderlineColor, Color.Red))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            // Underline quick application text button
                            TextButtonWithIcon(
                                icon = Icons.Default.FormatUnderlined,
                                iconTint = safeParseColor(lastUnderlineColor, Color.Red),
                                text = stringResource(R.string.tool_underline),
                                onClick = {
                                    val targetStart = mappedStart
                                    val targetEnd = mappedEnd
                                    textFieldValue = textFieldValue.copy(selection = androidx.compose.ui.text.TextRange.Zero)
                                    menuRect = null
                                    menuCallbacks = null
                                    applyStyleToRange(targetStart, targetEnd, "UNDERLINE", lastUnderlineColor)
                                    onSelectedUnderlineColorChange?.invoke(lastUnderlineColor)
                                },
                                iconSize = 15.dp,
                                fontSize = 12.sp,
                                paddingHorizontal = 2.dp,
                                paddingVertical = 4.dp
                            )
                        }

                        if (hasEditsInSelection) {
                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(16.dp)
                                    .background(Color(0xFF475569)) // slate-600
                            )

                            TextButtonWithIcon(
                                icon = eraserIcon,
                                iconTint = Color(0xFFEF4444), // red-500
                                text = stringResource(R.string.tool_eraser),
                                onClick = {
                                    val targetStart = mappedStart
                                    val targetEnd = mappedEnd
                                    textFieldValue = textFieldValue.copy(selection = androidx.compose.ui.text.TextRange.Zero)
                                    menuRect = null
                                    menuCallbacks = null
                                    applyStyleToRange(targetStart, targetEnd, "ERASER", "#FFFFFF")
                                },
                                iconSize = 15.dp,
                                fontSize = 12.sp,
                                paddingHorizontal = 2.dp,
                                paddingVertical = 4.dp
                            )
                        }
                    }
                    }
                }
            }
        }
    }
}

class MenuCallbacks(
    val onCopy: (() -> Unit)?,
    val onSelectAll: (() -> Unit)?
)

@Composable
fun TextButtonWithIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    text: String,
    onClick: () -> Unit,
    iconSize: androidx.compose.ui.unit.Dp = 16.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    paddingHorizontal: androidx.compose.ui.unit.Dp = 8.dp,
    paddingVertical: androidx.compose.ui.unit.Dp = 6.dp
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = paddingHorizontal, vertical = paddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = fontSize
            )
        )
    }
}

@Composable
fun InteractiveParagraphText(
    paragraphText: String,
    paragraphIndex: Int,
    ranges: List<StyledRange>,
    activeTool: String,
    selectedColorHex: String,
    onUpdateRanges: (List<StyledRange>) -> Unit,
    selectedMarkerColorHex: String = "#FFF59D",
    selectedUnderlineColorHex: String = "#F57F17",
    onSelectedMarkerColorChange: ((String) -> Unit)? = null,
    onSelectedUnderlineColorChange: ((String) -> Unit)? = null,
    allEdits: Map<Int, List<StyledRange>> = emptyMap(),
    paragraphRegistry: MutableMap<Int, ParagraphRegistryEntry> = remember { mutableMapOf() }
) {
    var textLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    val currentLayoutCoordinates = remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    var activeColorPickerMenu by remember { mutableStateOf<String?>(null) }
    
    val annotatedString = remember(paragraphText) {
        buildAnnotatedString {
            append(formatStringToSuperscript(paragraphText))
        }
    }

    val origToFormMapping = remember(paragraphText, annotatedString) {
        mapOriginalToFormatted(paragraphText, annotatedString.text)
    }

    val formToOrigMapping = remember(paragraphText, annotatedString) {
        mapFormattedToOriginal(paragraphText, annotatedString.text)
    }

    val markerColors = Constants.MARKER_COLORS
    val underlineColors = Constants.UNDERLINE_COLORS

    val lastMarkerColor = selectedMarkerColorHex
    val lastUnderlineColor = selectedUnderlineColorHex

    val eraserIcon = remember {
        androidx.compose.ui.graphics.vector.ImageVector.Builder(
            name = "Eraser",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = androidx.compose.ui.graphics.SolidColor(Color.Black),
                strokeLineWidth = 0f
            ) {
                moveTo(16.24f, 3.56f)
                lineTo(21.19f, 8.51f)
                curveTo(21.97f, 9.29f, 21.97f, 10.56f, 21.19f, 11.34f)
                lineTo(14.12f, 18.41f)
                lineTo(9.17f, 13.46f)
                lineTo(16.24f, 3.56f)
                close()
                moveTo(7.76f, 14.88f)
                lineTo(12.71f, 19.83f)
                lineTo(5.64f, 21.0f)
                lineTo(2.0f, 21.0f)
                lineTo(7.76f, 14.88f)
                close()
            }
        }.build()
    }

    val applyStyleToRanges = remember(paragraphText, ranges, activeTool, selectedColorHex, onUpdateRanges, allEdits) {
        { wordRanges: Collection<Pair<Int, Int>>, tool: String, colorHex: String ->
            val len = paragraphText.length
            val charHighlight = BooleanArray(len)
            val charHighlightColor = Array(len) { "#FFFFFF" }
            val charUnderscore = BooleanArray(len)
            val charUnderscoreColor = Array(len) { "#FFFFFF" }
            
            // Populate arrays with existing ranges
            ranges.forEach { r ->
                for (i in r.start until r.end) {
                    if (i in 0 until len) {
                        if (r.highlight) {
                            charHighlight[i] = true
                            charHighlightColor[i] = r.highlightColorHex
                        }
                        if (r.underscore) {
                            charUnderscore[i] = true
                            charUnderscoreColor[i] = r.underscoreColorHex
                        }
                    }
                }
            }
            
            // Modify all specified ranges together!
            wordRanges.forEach { (start, end) ->
                for (i in start until end) {
                    if (i in 0 until len) {
                        when (tool) {
                            Constants.TOOL_MARKER -> {
                                charHighlight[i] = true
                                charHighlightColor[i] = colorHex
                            }
                            Constants.TOOL_UNDERLINE -> {
                                charUnderscore[i] = true
                                charUnderscoreColor[i] = colorHex
                            }
                            Constants.TOOL_ERASER -> {
                                charHighlight[i] = false
                                charUnderscore[i] = false
                            }
                        }
                    }
                }
            }

            // Post-process to fill styling gaps (spaces/punctuation between adjacent words styled with identical colors)
            var lastHighlightIdx = -1
            for (i in 0 until len) {
                if (charHighlight[i]) {
                    if (lastHighlightIdx != -1 && lastHighlightIdx < i - 1) {
                        val color1 = charHighlightColor[lastHighlightIdx].lowercase()
                        val color2 = charHighlightColor[i].lowercase()
                        if (color1 == color2) {
                            var onlyGapChars = true
                            for (j in (lastHighlightIdx + 1) until i) {
                                val char = paragraphText[j]
                                if (char.isLetterOrDigit()) {
                                    onlyGapChars = false
                                    break
                                }
                            }
                            if (onlyGapChars) {
                                for (j in (lastHighlightIdx + 1) until i) {
                                    charHighlight[j] = true
                                    charHighlightColor[j] = charHighlightColor[lastHighlightIdx]
                                }
                            }
                        }
                    }
                    lastHighlightIdx = i
                }
            }

            var lastUnderscoreIdx = -1
            for (i in 0 until len) {
                if (charUnderscore[i]) {
                    if (lastUnderscoreIdx != -1 && lastUnderscoreIdx < i - 1) {
                        val color1 = charUnderscoreColor[lastUnderscoreIdx].lowercase()
                        val color2 = charUnderscoreColor[i].lowercase()
                        if (color1 == color2) {
                            var onlyGapChars = true
                            for (j in (lastUnderscoreIdx + 1) until i) {
                                val char = paragraphText[j]
                                if (char.isLetterOrDigit()) {
                                    onlyGapChars = false
                                    break
                                }
                            }
                            if (onlyGapChars) {
                                for (j in (lastUnderscoreIdx + 1) until i) {
                                    charUnderscore[j] = true
                                    charUnderscoreColor[j] = charUnderscoreColor[lastUnderscoreIdx]
                                }
                            }
                        }
                    }
                    lastUnderscoreIdx = i
                }
            }
            
            // Convert back to structured StyledRanges with trimmer
            val newRanges = mutableListOf<StyledRange>()
            fun addRangeWithCheck(s: Int, e: Int, highlight: Boolean, hc: String, underscore: Boolean, uc: String) {
                if (s < e) {
                    val sub = paragraphText.substring(s, e)
                    if (sub.any { !it.isWhitespace() }) {
                        newRanges.add(
                            StyledRange(
                                start = s,
                                end = e,
                                colorHex = if (highlight) hc else uc,
                                highlight = highlight,
                                underscore = underscore,
                                highlightColorHex = hc,
                                underscoreColorHex = uc
                            )
                        )
                    }
                }
            }

            var currentStart = -1
            var currentHighlight = false
            var currentHighlightColor = "#FFFFFF"
            var currentUnderscore = false
            var currentUnderscoreColor = "#FFFFFF"

            for (i in 0 until len) {
                val h = charHighlight[i]
                val hc = charHighlightColor[i]
                val u = charUnderscore[i]
                val uc = charUnderscoreColor[i]

                val hasStyle = h || u
                val matchesCurrent = currentStart != -1 && 
                                     currentHighlight == h && 
                                     currentHighlightColor.lowercase() == hc.lowercase() && 
                                     currentUnderscore == u && 
                                     currentUnderscoreColor.lowercase() == uc.lowercase()

                if (hasStyle) {
                    if (currentStart == -1) {
                        currentStart = i
                        currentHighlight = h
                        currentHighlightColor = hc
                        currentUnderscore = u
                        currentUnderscoreColor = uc
                    } else if (!matchesCurrent) {
                        addRangeWithCheck(
                            currentStart,
                            i,
                            currentHighlight,
                            currentHighlightColor,
                            currentUnderscore,
                            currentUnderscoreColor
                        )
                        currentStart = i
                        currentHighlight = h
                        currentHighlightColor = hc
                        currentUnderscore = u
                        currentUnderscoreColor = uc
                    }
                } else {
                    if (currentStart != -1) {
                        addRangeWithCheck(
                            currentStart,
                            i,
                            currentHighlight,
                            currentHighlightColor,
                            currentUnderscore,
                            currentUnderscoreColor
                        )
                        currentStart = -1
                    }
                }
            }
            if (currentStart != -1) {
                addRangeWithCheck(
                    currentStart,
                    len,
                    currentHighlight,
                    currentHighlightColor,
                    currentUnderscore,
                    currentUnderscoreColor
                )
            }
            onUpdateRanges(mergeAdjacentStyledRanges(paragraphText, newRanges))
        }
    }

    val applyStyleToRange = remember(applyStyleToRanges) {
        { start: Int, end: Int, tool: String, colorHex: String ->
            applyStyleToRanges(listOf(Pair(start, end)), tool, colorHex)
        }
    }

    val currentActiveTool by androidx.compose.runtime.rememberUpdatedState(activeTool)
    val currentSelectedColorHex by androidx.compose.runtime.rememberUpdatedState(selectedColorHex)
    val currentApplyStyleToRanges by androidx.compose.runtime.rememberUpdatedState(applyStyleToRanges)

    DisposableEffect(paragraphIndex, paragraphText, formToOrigMapping, applyStyleToRanges, paragraphRegistry) {
        val entry = ParagraphRegistryEntry(
            paragraphIndex = paragraphIndex,
            paragraphText = paragraphText,
            formToOrigMapping = formToOrigMapping,
            getLayoutCoordinates = { currentLayoutCoordinates.value },
            getTextLayoutResult = { textLayoutResult },
            applyStyleToRanges = applyStyleToRanges
        )
        paragraphRegistry[paragraphIndex] = entry
        onDispose {
            paragraphRegistry.remove(paragraphIndex)
        }
    }

    var menuRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var menuCallbacks by remember { mutableStateOf<MenuCallbacks?>(null) }

    val customTextToolbar = remember {
        object : androidx.compose.ui.platform.TextToolbar {
            override val status: androidx.compose.ui.platform.TextToolbarStatus
                get() = if (menuRect != null) androidx.compose.ui.platform.TextToolbarStatus.Shown else androidx.compose.ui.platform.TextToolbarStatus.Hidden

            override fun hide() {
                menuRect = null
                menuCallbacks = null
            }

            override fun showMenu(
                rect: androidx.compose.ui.geometry.Rect,
                onCopyRequested: (() -> Unit)?,
                onPasteRequested: (() -> Unit)?,
                onCutRequested: (() -> Unit)?,
                onSelectAllRequested: (() -> Unit)?
            ) {
                menuRect = rect
                menuCallbacks = MenuCallbacks(onCopyRequested, onSelectAllRequested)
            }
        }
    }

    val focusRequester = remember { FocusRequester() }

    var textFieldValue by remember {
        mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(annotatedString))
    }

    androidx.compose.runtime.LaunchedEffect(annotatedString) {
        textFieldValue = textFieldValue.copy(annotatedString = annotatedString)
    }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalTextToolbar provides customTextToolbar
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue.copy(annotatedString = annotatedString)
            },
            readOnly = true,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.Transparent),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF0F172A),
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium
            ),
            onTextLayout = { textLayoutResult = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onGloballyPositioned { currentLayoutCoordinates.value = it }
                .then(
                    if (activeTool == "MARKER" || activeTool == "UNDERLINE" || activeTool == "ERASER") {
                        Modifier.pointerInput(paragraphText, paragraphRegistry, activeTool, textFieldValue.selection.collapsed) {
                            if (!textFieldValue.selection.collapsed) {
                                return@pointerInput
                            }
                            val touchSlop = viewConfiguration.touchSlop
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val tool = currentActiveTool
                                if (tool != "MARKER" && tool != "UNDERLINE" && tool != "ERASER") {
                                    return@awaitEachGesture
                                }
                                
                                val startPosition = down.position
                                val currentPointerId = down.id
                                var hasDecidedGesture = false
                                var isScrollingMode = false
                                var isStylingMode = false
                                val touchedWordsMap = mutableMapOf<Int, MutableSet<Pair<Int, Int>>>()

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val anyActive = event.changes.any { it.id == currentPointerId && it.pressed }
                                    if (!anyActive) {
                                        // User released finger
                                        if (!hasDecidedGesture) {
                                            // Treated as a single tap!
                                            focusRequester.requestFocus()
                                            currentLayoutCoordinates.value?.let { myCoords ->
                                                if (myCoords.isAttached) {
                                                    val windowOffset = myCoords.localToWindow(startPosition)
                                                    
                                                    // Find which paragraph is at this touch position
                                                    for ((idx, entry) in paragraphRegistry) {
                                                        val otherCoords = entry.getLayoutCoordinates() ?: continue
                                                        if (!otherCoords.isAttached) continue
                                                        val otherLayoutResult = entry.getTextLayoutResult() ?: continue
                                                        val otherLocalPos = otherCoords.windowToLocal(windowOffset)
                                                        val height = otherCoords.size.height
                                                        
                                                        if (otherLocalPos.y in 0f..height.toFloat()) {
                                                            val dragPos = otherLayoutResult.getOffsetForPosition(otherLocalPos)
                                                            val origDragPos = entry.formToOrigMapping.getOrElse(dragPos) { dragPos }
                                                            if (origDragPos in 0..entry.paragraphText.length) {
                                                                getWordSnappedRange(entry.paragraphText, origDragPos, origDragPos)?.let { snapped ->
                                                                    val set = touchedWordsMap.getOrPut(idx) { mutableSetOf() }
                                                                    if (set.add(snapped)) {
                                                                        entry.applyStyleToRanges(set, currentActiveTool, currentSelectedColorHex)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        break
                                    }

                                    val activeChange = event.changes.firstOrNull { it.id == currentPointerId }
                                    if (activeChange != null) {
                                        val currentPosition = activeChange.position
                                        val diffX = currentPosition.x - startPosition.x
                                        val diffY = currentPosition.y - startPosition.y

                                        if (!hasDecidedGesture) {
                                            val distSq = diffX * diffX + diffY * diffY
                                            if (distSq >= touchSlop * touchSlop) {
                                                hasDecidedGesture = true
                                                if (kotlin.math.abs(diffY) > kotlin.math.abs(diffX)) {
                                                    isScrollingMode = true
                                                    break
                                                    break
                                                } else {
                                                    isStylingMode = true
                                                    focusRequester.requestFocus()
                                                    activeChange.consume()

                                                    // Also apply styling to down position now that we know we are styling
                                                    currentLayoutCoordinates.value?.let { myCoords ->
                                                        if (myCoords.isAttached) {
                                                            val windowOffset = myCoords.localToWindow(startPosition)
                                                            for ((idx, entry) in paragraphRegistry) {
                                                                val otherCoords = entry.getLayoutCoordinates() ?: continue
                                                                if (!otherCoords.isAttached) continue
                                                                val otherLayoutResult = entry.getTextLayoutResult() ?: continue
                                                                val otherLocalPos = otherCoords.windowToLocal(windowOffset)
                                                                val height = otherCoords.size.height
                                                                
                                                                if (otherLocalPos.y in 0f..height.toFloat()) {
                                                                    val dragPos = otherLayoutResult.getOffsetForPosition(otherLocalPos)
                                                                    val origDragPos = entry.formToOrigMapping.getOrElse(dragPos) { dragPos }
                                                                    if (origDragPos in 0..entry.paragraphText.length) {
                                                                        getWordSnappedRange(entry.paragraphText, origDragPos, origDragPos)?.let { snapped ->
                                                                            val set = touchedWordsMap.getOrPut(idx) { mutableSetOf() }
                                                                            if (set.add(snapped)) {
                                                                                entry.applyStyleToRanges(set, currentActiveTool, currentSelectedColorHex)
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            if (isStylingMode) {
                                                activeChange.consume()
                                                currentLayoutCoordinates.value?.let { myCoords ->
                                                    if (myCoords.isAttached) {
                                                        val windowOffset = myCoords.localToWindow(currentPosition)
                                                        
                                                        // Iterate through all active paragraphs to find which one is hovered
                                                        for ((idx, entry) in paragraphRegistry) {
                                                            val otherCoords = entry.getLayoutCoordinates() ?: continue
                                                            if (!otherCoords.isAttached) continue
                                                            val otherLayoutResult = entry.getTextLayoutResult() ?: continue
                                                            val otherLocalOffset = otherCoords.windowToLocal(windowOffset)
                                                            val height = otherCoords.size.height
                                                            
                                                            if (otherLocalOffset.y in 0f..height.toFloat()) {
                                                                val dragPos = otherLayoutResult.getOffsetForPosition(otherLocalOffset)
                                                                val origDragPos = entry.formToOrigMapping.getOrElse(dragPos) { dragPos }
                                                                if (origDragPos in 0..entry.paragraphText.length) {
                                                                    getWordSnappedRange(entry.paragraphText, origDragPos, origDragPos)?.let { snapped ->
                                                                        val set = touchedWordsMap.getOrPut(idx) { mutableSetOf() }
                                                                        if (set.add(snapped)) {
                                                                            entry.applyStyleToRanges(set, currentActiveTool, currentSelectedColorHex)
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
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                )
                .drawWithContent {
                    // Draw highlights first behind the text
                    try {
                        textLayoutResult?.let { layoutResult ->
                            ranges.forEach { range ->
                                if (range.highlight) {
                                    val colorVal = safeParseColor(range.highlightColorHex, Color.Yellow).copy(alpha = 0.85f)
                                    val safeStart = range.start.coerceIn(0, paragraphText.length)
                                    val safeEnd = range.end.coerceIn(0, paragraphText.length)
                                    val rawMappedStart = origToFormMapping.getOrElse(safeStart) { safeStart }
                                    val rawMappedEnd = origToFormMapping.getOrElse(safeEnd) { safeEnd }
                                    val textLen = layoutResult.layoutInput.text.length
                                    val mappedSelectStart = rawMappedStart.coerceIn(0, textLen)
                                    val mappedSelectEnd = rawMappedEnd.coerceIn(0, textLen)
                                    if (mappedSelectStart < mappedSelectEnd) {
                                        val startLine = layoutResult.getLineForOffset(mappedSelectStart)
                                        val endLine = layoutResult.getLineForOffset(maxOf(0, mappedSelectEnd - 1))
                                        for (line in startLine..endLine) {
                                            val lineStart = layoutResult.getLineStart(line)
                                            val lineEnd = layoutResult.getLineEnd(line)
                                            val segmentStart = maxOf(mappedSelectStart, lineStart)
                                            val segmentEnd = minOf(mappedSelectEnd, lineEnd)
                                            if (segmentStart < segmentEnd) {
                                                val left = if (segmentStart == lineStart) {
                                                    layoutResult.getLineLeft(line)
                                                } else {
                                                    layoutResult.getHorizontalPosition(segmentStart, usePrimaryDirection = true)
                                                }
                                                val right = if (segmentEnd == lineEnd) {
                                                    layoutResult.getLineRight(line)
                                                } else {
                                                    layoutResult.getHorizontalPosition(segmentEnd, usePrimaryDirection = true)
                                                }
                                                val topHeight = layoutResult.getLineTop(line)
                                                val bottomHeight = layoutResult.getLineBottom(line)
                                                drawRect(
                                                    color = colorVal,
                                                    topLeft = androidx.compose.ui.geometry.Offset(minOf(left, right), topHeight),
                                                    size = androidx.compose.ui.geometry.Size(kotlin.math.abs(right - left), bottomHeight - topHeight)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // ignore
                    }

                    drawContent()

                    // Draw underlines on top of the text
                    try {
                        textLayoutResult?.let { layoutResult ->
                            ranges.forEach { range ->
                                if (range.underscore) {
                                    val colorVal = safeParseColor(range.underscoreColorHex, Color.Red)
                                    val safeStart = range.start.coerceIn(0, paragraphText.length)
                                    val safeEnd = range.end.coerceIn(0, paragraphText.length)
                                    val rawMappedStart = origToFormMapping.getOrElse(safeStart) { safeStart }
                                    val rawMappedEnd = origToFormMapping.getOrElse(safeEnd) { safeEnd }
                                    val textLen = layoutResult.layoutInput.text.length
                                    val mappedSelectStart = rawMappedStart.coerceIn(0, textLen)
                                    val mappedSelectEnd = rawMappedEnd.coerceIn(0, textLen)
                                    if (mappedSelectStart < mappedSelectEnd) {
                                        val startLine = layoutResult.getLineForOffset(mappedSelectStart)
                                        val endLine = layoutResult.getLineForOffset(maxOf(0, mappedSelectEnd - 1))
                                        for (line in startLine..endLine) {
                                            val lineStart = layoutResult.getLineStart(line)
                                            val lineEnd = layoutResult.getLineEnd(line)
                                            val segmentStart = maxOf(mappedSelectStart, lineStart)
                                            val segmentEnd = minOf(mappedSelectEnd, lineEnd)
                                            if (segmentStart < segmentEnd) {
                                                val left = if (segmentStart == lineStart) {
                                                    layoutResult.getLineLeft(line)
                                                } else {
                                                    layoutResult.getHorizontalPosition(segmentStart, usePrimaryDirection = true)
                                                }
                                                val right = if (segmentEnd == lineEnd) {
                                                    layoutResult.getLineRight(line)
                                                } else {
                                                    layoutResult.getHorizontalPosition(segmentEnd, usePrimaryDirection = true)
                                                }
                                                val bottomHeight = layoutResult.getLineBottom(line)
                                                val lineY = bottomHeight - 2.dp.toPx()
                                                drawLine(
                                                    color = colorVal,
                                                    start = androidx.compose.ui.geometry.Offset(minOf(left, right), lineY),
                                                    end = androidx.compose.ui.geometry.Offset(maxOf(left, right), lineY),
                                                    strokeWidth = 2.dp.toPx()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // ignore drawing errors to avoid crashing the Compose drawing thread
                    }
                }
                .padding(vertical = 4.dp)
        )
    }

    val currentMenuRect = menuRect
    if (currentMenuRect != null && !textFieldValue.selection.collapsed) {
        val selStart = minOf(textFieldValue.selection.start, textFieldValue.selection.end)
        val selEnd = maxOf(textFieldValue.selection.start, textFieldValue.selection.end)
        
        val mappedStart = formToOrigMapping.getOrElse(selStart) { selStart }
        val mappedEnd = formToOrigMapping.getOrElse(selEnd) { selEnd }
        
        val hasEditsInSelection = remember(ranges, mappedStart, mappedEnd) {
            ranges.any { range -> range.start < mappedEnd && range.end > mappedStart }
        }
        var showedColorPickerMode by remember(currentMenuRect) { mutableStateOf<String?>(null) }

        val density = LocalDensity.current
        val popupPositionProvider = remember(currentMenuRect) {
            object : androidx.compose.ui.window.PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: androidx.compose.ui.unit.IntRect,
                    windowSize: androidx.compose.ui.unit.IntSize,
                    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                    popupContentSize: androidx.compose.ui.unit.IntSize
                ): androidx.compose.ui.unit.IntOffset {
                    val left = currentMenuRect.left
                    val right = currentMenuRect.right
                    val top = currentMenuRect.top
                    val bottom = currentMenuRect.bottom

                    val safeLeft = if (left.isNaN() || left.isInfinite()) 0f else left
                    val safeRight = if (right.isNaN() || right.isInfinite()) 0f else right
                    val safeTop = if (top.isNaN() || top.isInfinite()) 0f else top
                    val safeBottom = if (bottom.isNaN() || bottom.isInfinite()) 0f else bottom

                    val x = anchorBounds.left + (safeLeft + safeRight) / 2 - popupContentSize.width / 2
                    val buffer = with(density) { 60.dp.toPx() }
                    var y = anchorBounds.top + safeTop - popupContentSize.height - with(density) { 8.dp.toPx() }
                    
                    if (y < buffer) {
                        y = anchorBounds.top + safeBottom + with(density) { 8.dp.toPx() }
                    }
                    
                    val maxX = (windowSize.width - popupContentSize.width - 8).toFloat()
                    val finalX = if (8f >= maxX) 8f else x.coerceIn(8f, maxX)
                    
                    val maxY = (windowSize.height - popupContentSize.height - 8).toFloat()
                    val finalY = if (8f >= maxY) 8f else y.coerceIn(8f, maxY)
                    
                    return androidx.compose.ui.unit.IntOffset(finalX.toInt(), finalY.toInt())
                }
            }
        }

        androidx.compose.ui.window.Popup(
            popupPositionProvider = popupPositionProvider,
            onDismissRequest = {
                android.util.Log.d(LogMessages.TAG_SELECTION_BUG, LogMessages.PARAGRAPH_POPUP_DISMISS_PRESERVE_SELECTION)
                menuRect = null
                menuCallbacks = null
            }
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E293B), // slate-800
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFF334155)), // slate-700
                modifier = Modifier.padding(2.dp)
            ) {
                if (showedColorPickerMode == Constants.TOOL_MARKER) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { showedColorPickerMode = null },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = stringResource(R.string.btn_back),
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(Color(0xFF475569)) // slate-600
                        )
                        markerColors.forEach { colorHex ->
                            val colorVal = safeParseColor(colorHex, Color.Yellow)
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(colorVal)
                                    .border(
                                        width = if (lastMarkerColor == colorHex) 2.dp else 1.dp,
                                        color = if (lastMarkerColor == colorHex) Color.White else Color.Gray.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        textFieldValue = textFieldValue.copy(selection = androidx.compose.ui.text.TextRange.Zero)
                                        menuRect = null
                                        menuCallbacks = null
                                        applyStyleToRange(mappedStart, mappedEnd, "MARKER", colorHex)
                                        onSelectedMarkerColorChange?.invoke(colorHex)
                                        showedColorPickerMode = null
                                    }
                            )
                        }
                    }
                } else if (showedColorPickerMode == "UNDERLINE") {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { showedColorPickerMode = null },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = stringResource(R.string.btn_back),
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(Color(0xFF475569)) // slate-600
                        )
                        underlineColors.forEach { colorHex ->
                            val colorVal = safeParseColor(colorHex, Color.Red)
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(colorVal)
                                    .border(
                                        width = if (lastUnderlineColor == colorHex) 2.dp else 1.dp,
                                        color = if (lastUnderlineColor == colorHex) Color.White else Color.Gray.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        textFieldValue = textFieldValue.copy(selection = androidx.compose.ui.text.TextRange.Zero)
                                        menuRect = null
                                        menuCallbacks = null
                                        applyStyleToRange(mappedStart, mappedEnd, "UNDERLINE", colorHex)
                                        onSelectedUnderlineColorChange?.invoke(colorHex)
                                        showedColorPickerMode = null
                                    }
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        menuCallbacks?.onCopy?.let { onCopy ->
                            IconButton(
                                onClick = {
                                    onCopy()
                                    menuRect = null
                                    menuCallbacks = null
                                    textFieldValue = textFieldValue.copy(selection = androidx.compose.ui.text.TextRange.Zero)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.btn_copy),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        menuCallbacks?.onSelectAll?.let { onSelectAll ->
                            IconButton(
                                onClick = {
                                    onSelectAll()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = stringResource(R.string.btn_select_all),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        
                        // Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(Color(0xFF475569)) // slate-600
                        )

                        // --- MARKER GROUP ---
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Marker Color Indicator dropdown slot
                            Box(contentAlignment = Alignment.Center) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { showedColorPickerMode = "MARKER" }
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(safeParseColor(lastMarkerColor, Color.Yellow))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            // Marker quick application text button
                            TextButtonWithIcon(
                                icon = Icons.Default.Brush,
                                iconTint = safeParseColor(lastMarkerColor, Color.Yellow),
                                text = stringResource(R.string.tool_marker),
                                onClick = {
                                    val targetStart = mappedStart
                                    val targetEnd = mappedEnd
                                    textFieldValue = textFieldValue.copy(selection = androidx.compose.ui.text.TextRange.Zero)
                                    menuRect = null
                                    menuCallbacks = null
                                    applyStyleToRange(targetStart, targetEnd, "MARKER", lastMarkerColor)
                                    onSelectedMarkerColorChange?.invoke(lastMarkerColor)
                                },
                                iconSize = 15.dp,
                                fontSize = 12.sp,
                                paddingHorizontal = 2.dp,
                                paddingVertical = 4.dp
                            )
                        }

                        // Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(Color(0xFF475569)) // slate-600
                        )

                        // --- UNDERLINE (LINE) GROUP ---
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Underline Color Indicator dropdown slot
                            Box(contentAlignment = Alignment.Center) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { showedColorPickerMode = "UNDERLINE" }
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(safeParseColor(lastUnderlineColor, Color.Red))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            // Underline quick application text button
                            TextButtonWithIcon(
                                icon = Icons.Default.FormatUnderlined,
                                iconTint = safeParseColor(lastUnderlineColor, Color.Red),
                                text = stringResource(R.string.tool_underline),
                                onClick = {
                                    val targetStart = mappedStart
                                    val targetEnd = mappedEnd
                                    textFieldValue = textFieldValue.copy(selection = androidx.compose.ui.text.TextRange.Zero)
                                    menuRect = null
                                    menuCallbacks = null
                                    applyStyleToRange(targetStart, targetEnd, "UNDERLINE", lastUnderlineColor)
                                    onSelectedUnderlineColorChange?.invoke(lastUnderlineColor)
                                },
                                iconSize = 15.dp,
                                fontSize = 12.sp,
                                paddingHorizontal = 2.dp,
                                paddingVertical = 4.dp
                            )
                        }

                        if (hasEditsInSelection) {
                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(16.dp)
                                    .background(Color(0xFF475569)) // slate-600
                            )

                            TextButtonWithIcon(
                                icon = eraserIcon,
                                iconTint = Color(0xFFEF4444), // red-500
                                text = stringResource(R.string.tool_eraser),
                                onClick = {
                                    val targetStart = mappedStart
                                    val targetEnd = mappedEnd
                                    // 1. Clear selection first to dismiss selection context safely before triggering parent recompositions
                                    textFieldValue = textFieldValue.copy(selection = androidx.compose.ui.text.TextRange.Zero)
                                    menuRect = null
                                    menuCallbacks = null
                                    // 2. Apply styling changes
                                    applyStyleToRange(targetStart, targetEnd, "ERASER", "#FFFFFF")
                                },
                                iconSize = 15.dp,
                                fontSize = 12.sp,
                                paddingHorizontal = 2.dp,
                                paddingVertical = 4.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

fun safeParseColor(hex: String?, default: Color): Color {
    if (hex.isNullOrBlank()) return default
    return try {
        val normalized = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(normalized))
    } catch (e: Exception) {
        default
    }
}

fun isSuperscriptEquivalent(normal: Char, superChar: Char): Boolean {
    val map = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹'
    )
    return map[normal] == superChar
}

fun mapOriginalToFormatted(original: String, formatted: String): IntArray {
    val origToForm = IntArray(original.length + 1) { formatted.length }
    var formIdx = 0
    for (origIdx in 0..original.length) {
        if (origIdx == original.length) {
            origToForm[origIdx] = formatted.length
            break
        }
        val origChar = original[origIdx]
        if (formIdx < formatted.length) {
            val formChar = formatted[formIdx]
            if (origChar == formChar || isSuperscriptEquivalent(origChar, formChar)) {
                origToForm[origIdx] = formIdx
                formIdx++
            } else if (origChar == '.' || origChar == '-') {
                origToForm[origIdx] = formIdx
            } else {
                origToForm[origIdx] = formIdx
                formIdx++
            }
        } else {
            origToForm[origIdx] = formatted.length
        }
    }
    return origToForm
}

fun mapFormattedToOriginal(original: String, formatted: String): IntArray {
    val formToOrig = IntArray(formatted.length + 1) { original.length }
    var origIdx = 0
    for (formIdx in 0..formatted.length) {
        if (formIdx == formatted.length) {
            formToOrig[formIdx] = original.length
            break
        }
        val formChar = formatted[formIdx]
        var assigned = false
        while (origIdx < original.length) {
            val origChar = original[origIdx]
            if (origChar == formChar || isSuperscriptEquivalent(origChar, formChar)) {
                formToOrig[formIdx] = origIdx
                origIdx++
                assigned = true
                break
            } else if (origChar == '.' || origChar == '-') {
                origIdx++
            } else {
                formToOrig[formIdx] = origIdx
                origIdx++
                assigned = true
                break
            }
        }
        if (!assigned) {
            formToOrig[formIdx] = original.length
        }
    }
    return formToOrig
}

fun formatStringToSuperscript(input: String): String {
    var result = input
    val regexDots = """(\d+)\.(\d+)""".toRegex()
    result = regexDots.replace(result) { matchResult ->
        val base = matchResult.groupValues[1]
        val suffix = matchResult.groupValues[2]
        val sup = suffix.map { char ->
            when (char) {
                '0' -> '⁰'
                '1' -> '¹'
                '2' -> '²'
                '3' -> '³'
                '4' -> '⁴'
                '5' -> '⁵'
                '6' -> '⁶'
                '7' -> '⁷'
                '8' -> '⁸'
                '9' -> '⁹'
                else -> char
            }
        }.joinToString("")
        "$base$sup"
    }
    val regexHyphens = """(\d+)-(\d+)""".toRegex()
    result = regexHyphens.replace(result) { matchResult ->
        val base = matchResult.groupValues[1]
        val suffix = matchResult.groupValues[2]
        val sup = suffix.map { char ->
            when (char) {
                '0' -> '⁰'
                '1' -> '¹'
                '2' -> '²'
                '3' -> '³'
                '4' -> '⁴'
                '5' -> '⁵'
                '6' -> '⁶'
                '7' -> '⁷'
                '8' -> '⁸'
                '9' -> '⁹'
                else -> char
            }
        }.joinToString("")
        "$base$sup"
    }
    return result
}

@Composable
fun formatArticleId(id: Int, chapterId: Int = 0): String {
    if (id == 0) return stringResource(R.string.preamble)
    if (chapterId == 15 && id == 161) {
        return "16¹"
    }
    if (id > 1000) {
        val base = id / 10
        val suffix = id % 10
        val superscript = when (suffix) {
            0 -> "⁰"
            1 -> "¹"
            2 -> "²"
            3 -> "³"
            4 -> "⁴"
            5 -> "⁵"
            6 -> "⁶"
            7 -> "⁷"
            8 -> "⁸"
            9 -> "⁹"
            else -> suffix.toString()
        }
        return "$base$superscript"
    }
    return id.toString()
}

@Composable
fun getBackNavigationText(article: Article): String {
    val artLabel = formatArticleId(article.id, article.chapterId)
    return if (article.chapterId == 15) {
        stringResource(R.string.back_to_chapter_15, artLabel)
    } else if (article.chapterId == 0) {
        stringResource(R.string.back_to_preamble)
    } else {
        stringResource(R.string.back_to_chapter_article, article.chapterId, artLabel)
    }
}

@Composable
fun ArticleIdText(
    id: Int,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
    chapterId: Int = 0
) {
    val preambleText = stringResource(R.string.preamble)
    val text = remember(id, chapterId, preambleText) {
        if (id == 0) {
            buildAnnotatedString {
                append(preambleText)
            }
        } else if (id > 1000 || (chapterId == 15 && id == 161)) {
            val base = if (chapterId == 15 && id == 161) "16" else (id / 10).toString()
            val suffix = if (chapterId == 15 && id == 161) "1" else (id % 10).toString()
            buildAnnotatedString {
                append(base)
                withStyle(
                    SpanStyle(
                        baselineShift = BaselineShift.Superscript,
                        fontSize = (fontSize.value * 0.7f).sp
                    )
                ) {
                    append(suffix)
                }
            }
        } else {
            buildAnnotatedString {
                append(id.toString())
            }
        }
    }
    val isFractional = id > 1000 || (chapterId == 15 && id == 161)
    Text(
        text = text,
        color = color,
        fontSize = if (isFractional) (fontSize.value * 0.85f).sp else fontSize,
        fontWeight = fontWeight,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
    )
}

fun findArticleByLink(text: String): Article? {
    // Normalise text
    val normalized = text
        .replace("¹", ".1")
        .replace("²", ".2")
        .replace("³", ".3")
        .replace("⁴", ".4")
        .replace("⁵", ".5")
        .replace("⁶", ".6")
        .replace("⁷", ".7")
        .replace("⁸", ".8")
        .replace("⁹", ".9")
        .replace("⁰", ".0")
        .replace("-", ".") // "16-1" -> "16.1"
        .replace("–", ".") // en-dash
        .replace("—", ".") // em-dash
    
    // Check if the link specifically refers to a "punkt" / "п"
    val lower = text.lowercase()
    val isPunkt = lower.contains(Constants.LINK_PUNKT_FULL) || 
                  lower.contains(Constants.LINK_P_DOT) || 
                  lower.startsWith(Constants.LINK_P_SPACE_START) || 
                  lower.contains(Constants.LINK_P_SPACE_MID)
    
    // Find decimal or integer number (e.g. "125" or "16.1")
    val regex = """\d+(?:\.\d+)?""".toRegex()
    val match = regex.find(normalized)
    if (match != null) {
        val numberStr = match.value
        val dVal = numberStr.toDoubleOrNull()
        if (dVal != null) {
            val targetId = if (dVal % 1.0 != 0.0) {
                Math.round(dVal * 10).toInt()
            } else {
                dVal.toInt()
            }
            // If it's labeled as a "punkt" (point) or chapter 15 reference, look in chapter 15 first
            val article = if (isPunkt) {
                ConstitutionData.articles.find { it.id == targetId && it.chapterId == 15 }
                    ?: ConstitutionData.articles.find { it.id == targetId }
            } else {
                ConstitutionData.articles.find { it.id == targetId && it.chapterId != 15 }
                    ?: ConstitutionData.articles.find { it.id == targetId }
            }
            if (article != null) return article
        }
    }
    return null
}

@Composable
fun SegmentedText(
    segments: List<com.example.data.model.ContentSegment>,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color(0xFF0F172A),
    lineHeight: androidx.compose.ui.unit.TextUnit = 24.sp,
    fontWeight: FontWeight = FontWeight.Medium,
    fontStyle: FontStyle? = null,
    onArticleClick: ((Article) -> Unit)? = null
) {
    val context = LocalContext.current
    
    // Merge adjacent link segments that share the same URL to prevent split link issues (e.g., 149-1)
    val mergedSegments = remember(segments) {
        val result = mutableListOf<com.example.data.model.ContentSegment>()
        var currentLink: com.example.data.model.ContentSegment? = null
        for (segment in segments) {
            if (segment.type == "link") {
                if (currentLink != null && currentLink.url == segment.url) {
                    currentLink = currentLink.copy(text = currentLink.text + segment.text)
                } else {
                    if (currentLink != null) {
                        result.add(currentLink)
                    }
                    currentLink = segment
                }
            } else {
                if (currentLink != null) {
                    result.add(currentLink)
                    currentLink = null
                }
                result.add(segment)
            }
        }
        if (currentLink != null) {
            result.add(currentLink)
        }
        result
    }

    val annotatedString = remember(mergedSegments) {
        buildAnnotatedString {
            mergedSegments.forEach { segment ->
                if (segment.type == "link") {
                    pushStringAnnotation(tag = "URL", annotation = "${segment.url}|${segment.text}")
                    withStyle(
                        style = SpanStyle(
                            color = Color(0xFF0D47A1),
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(formatStringToSuperscript(segment.text))
                    }
                    pop()
                } else {
                    append(formatStringToSuperscript(segment.value))
                }
            }
        }
    }

    androidx.compose.foundation.text.ClickableText(
        text = annotatedString,
        style = style.copy(
            color = color,
            lineHeight = lineHeight,
            fontWeight = fontWeight,
            fontStyle = fontStyle ?: style.fontStyle,
            textAlign = TextAlign.Start
        ),
        modifier = modifier,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        val parts = annotation.item.split("|")
                        val clickedUrl = parts.getOrNull(0) ?: ""
                        val segmentText = parts.getOrNull(1) ?: ""
                        
                        var articleNavigated = false
                        if (clickedUrl.startsWith("#") || (!clickedUrl.startsWith("http://") && !clickedUrl.startsWith("https://"))) {
                            val targetArticle = findArticleByLink(segmentText)
                            if (targetArticle != null && onArticleClick != null) {
                                onArticleClick(targetArticle)
                                articleNavigated = true
                            }
                        }
                        
                        if (!articleNavigated) {
                            val finalUrl = if (clickedUrl.startsWith("#")) {
                                "${Constants.DEFAULT_RADA_URL}$clickedUrl"
                            } else {
                                clickedUrl
                            }
                            if (finalUrl.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                                context.startActivity(intent)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e(LogMessages.TAG_SEGMENTED_TEXT, LogMessages.openUrlFailed(annotation.item, e.message), e)
                    }
                }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteCard(note: Note, modifier: Modifier = Modifier, onArticleClick: ((Article) -> Unit)? = null) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF1F5F9),
        border = BorderStroke(0.5.dp, Color(0xFFCBD5E1))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SegmentedText(
                segments = note.content,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = Color(0xFF475569),
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                onArticleClick = onArticleClick
            )
        }
    }
}

@Composable
fun GlobalFormattingPanel(
    editingArticleId: Int?,
    activeTool: String,
    onActiveToolChange: (String) -> Unit,
    selectedColorHex: String,
    onColorHexChange: (String) -> Unit,
    isPanelExpanded: Boolean,
    onPanelExpandedChange: (Boolean) -> Unit,
    onClearAllEdits: () -> Unit,
    hasAnyEdits: Boolean,
    onDoneEditing: () -> Unit,
    articleTitle: String?
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    var lastMarkerColor by remember { mutableStateOf(Constants.COLOR_DEFAULT_MARKER) }
    var lastUnderlineColor by remember { mutableStateOf(Constants.COLOR_DEFAULT_UNDERLINE) }

    androidx.compose.runtime.LaunchedEffect(activeTool, selectedColorHex) {
        if (activeTool == Constants.TOOL_MARKER) {
            lastMarkerColor = selectedColorHex
        } else if (activeTool == Constants.TOOL_UNDERLINE) {
            lastUnderlineColor = selectedColorHex
        }
    }

    val eraserIcon = remember {
        androidx.compose.ui.graphics.vector.ImageVector.Builder(
            name = "Eraser",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = androidx.compose.ui.graphics.SolidColor(Color.Black),
                strokeLineWidth = 0f
            ) {
                moveTo(16.24f, 3.56f)
                lineTo(21.19f, 8.51f)
                curveTo(21.97f, 9.29f, 21.97f, 10.56f, 21.19f, 11.34f)
                lineTo(14.12f, 18.41f)
                lineTo(9.17f, 13.46f)
                lineTo(16.24f, 3.56f)
                close()
                moveTo(7.76f, 14.88f)
                lineTo(12.71f, 19.83f)
                lineTo(5.64f, 21.0f)
                lineTo(2.0f, 21.0f)
                lineTo(7.76f, 14.88f)
                close()
            }
        }.build()
    }

    val markerColors = Constants.MARKER_COLORS
    val underlineColors = Constants.UNDERLINE_COLORS

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("global_formatting_panel"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(
            color = Color(0xFFE2E8F0),
            thickness = 1.2.dp,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Button 0: SELECT / CURSOR ---
            Surface(
                onClick = {
                    onActiveToolChange(Constants.TOOL_NONE)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("tool_button_none"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (activeTool == Constants.TOOL_NONE) 2.dp else 1.dp,
                    color = if (activeTool == Constants.TOOL_NONE) Color(0xFF0D47A1) else Color(0xFFE2E8F0)
                ),
                color = if (activeTool == Constants.TOOL_NONE) Color(0xFF0D47A1).copy(alpha = 0.08f) else Color.White
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = stringResource(R.string.tool_selection),
                        tint = if (activeTool == Constants.TOOL_NONE) Color(0xFF0D47A1) else Color(0xFF475569),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.Transparent)
                    )
                }
            }

            // Divider 0-1
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(Color(0xFFE2E8F0))
            )

            // --- Button 1: MARKER ---
            Surface(
                onClick = {
                    if (activeTool == Constants.TOOL_MARKER) {
                        onActiveToolChange(Constants.TOOL_NONE)
                    } else {
                        onActiveToolChange(Constants.TOOL_MARKER)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("tool_button_marker"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (activeTool == Constants.TOOL_MARKER) 2.dp else 1.dp,
                    color = if (activeTool == Constants.TOOL_MARKER) Color(0xFF0D47A1) else Color(0xFFE2E8F0)
                ),
                color = if (activeTool == Constants.TOOL_MARKER) Color(0xFF0D47A1).copy(alpha = 0.08f) else Color.White
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Brush,
                        contentDescription = stringResource(R.string.tool_marker),
                        tint = if (activeTool == Constants.TOOL_MARKER) Color(0xFF0D47A1) else Color(0xFF475569),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(safeParseColor(lastMarkerColor, Color.Yellow))
                    )
                }
            }

            // Divider 1-2
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(Color(0xFFE2E8F0))
            )

            // --- Button 2: UNDERLINE (Line) ---
            Surface(
                onClick = {
                    if (activeTool == Constants.TOOL_UNDERLINE) {
                        onActiveToolChange(Constants.TOOL_NONE)
                    } else {
                        onActiveToolChange(Constants.TOOL_UNDERLINE)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("tool_button_underline"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (activeTool == Constants.TOOL_UNDERLINE) 2.dp else 1.dp,
                    color = if (activeTool == Constants.TOOL_UNDERLINE) Color(0xFF0D47A1) else Color(0xFFE2E8F0)
                ),
                color = if (activeTool == Constants.TOOL_UNDERLINE) Color(0xFF0D47A1).copy(alpha = 0.08f) else Color.White
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatUnderlined,
                        contentDescription = stringResource(R.string.tool_underline),
                        tint = if (activeTool == Constants.TOOL_UNDERLINE) Color(0xFF0D47A1) else Color(0xFF475569),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(safeParseColor(lastUnderlineColor, Color.Red))
                    )
                }
            }

            // Divider 2-3
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(Color(0xFFE2E8F0))
            )

            // --- Button 3: ERASER (Гумка) ---
            Surface(
                onClick = {
                    if (activeTool == Constants.TOOL_ERASER) {
                        onActiveToolChange(Constants.TOOL_NONE)
                    } else {
                        onActiveToolChange(Constants.TOOL_ERASER)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("tool_button_eraser"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (activeTool == Constants.TOOL_ERASER) 2.dp else 1.dp,
                    color = if (activeTool == Constants.TOOL_ERASER) Color(0xFFEF4444) else Color(0xFFE2E8F0)
                ),
                color = if (activeTool == Constants.TOOL_ERASER) Color(0xFFEF4444).copy(alpha = 0.08f) else Color.White
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = eraserIcon,
                        contentDescription = stringResource(R.string.tool_eraser),
                        tint = if (activeTool == Constants.TOOL_ERASER) Color(0xFFEF4444) else Color(0xFF475569),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.Transparent)
                    )
                }
            }

            // Divider 3-4
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(Color(0xFFE2E8F0))
            )

            // --- Button 4: CLEAR ALL (Очистити все) ---
            Surface(
                onClick = {
                    if (hasAnyEdits) {
                        onClearAllEdits()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("tool_button_clear_all"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = 1.0.dp,
                    color = if (hasAnyEdits) Color(0xFFE2E8F0) else Color(0xFFE2E8F0).copy(alpha = 0.5f)
                ),
                color = if (hasAnyEdits) Color.White else Color(0xFFF8FAFC).copy(alpha = 0.5f),
                enabled = hasAnyEdits
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatColorReset,
                        contentDescription = stringResource(R.string.tool_clear_all),
                        tint = if (hasAnyEdits) Color(0xFFEF4444) else Color(0xFF94A3B8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.Transparent)
                    )
                }
            }
        }

        // --- Active Color Palette ---
        if (activeTool == "MARKER" || activeTool == "UNDERLINE") {
            val activeColors = if (activeTool == "MARKER") markerColors else underlineColors
            val currentColor = if (activeTool == "MARKER") lastMarkerColor else lastUnderlineColor

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                activeColors.forEach { colorHex ->
                    val isSelected = currentColor.equals(colorHex, ignoreCase = true)
                    val colorVal = safeParseColor(colorHex, Color.Gray)

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colorVal)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color(0xFF0D47A1) else Color(0xFFCBD5E1),
                                shape = CircleShape
                            )
                            .clickable {
                                onColorHexChange(colorHex)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.state_selected),
                                tint = if (activeTool == "MARKER") Color(0xFF1E293B) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            color = Color(0xFFE2E8F0),
            thickness = 1.2.dp,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}