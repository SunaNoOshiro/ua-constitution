package ua.constitution

import ua.constitution.utils.Constants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.constitution.data.model.Article
import ua.constitution.data.model.Chapter
import ua.constitution.ui.theme.*
import ua.constitution.ui.viewmodel.ConstitutionViewModel
import ua.constitution.ui.model.DashboardTab
import ua.constitution.ui.model.FullscreenSymbol
import ua.constitution.domain.content.ChapterRangeKind
import ua.constitution.domain.content.chapterRangeKind
import ua.constitution.ui.ImmersiveFullscreenEffect
import ua.constitution.ui.openExternalUrl
import ua.constitution.ui.isBookmarked
import ua.constitution.ui.editsJsonFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppDashboard(viewModel: ConstitutionViewModel) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(DashboardTab.HOME) } // Default to Home (Державні символи)
    var isSearchActive by remember { mutableStateOf(false) } // Controls immediate search bar drop

    var fullscreenSymbol by remember { mutableStateOf(FullscreenSymbol.NONE) }

    // Immersive (system-bars-hidden) window mode while a symbol is shown full-screen.
    ImmersiveFullscreenEffect(active = fullscreenSymbol != FullscreenSymbol.NONE)

    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredArticles by viewModel.filteredArticles.collectAsState()
    val bookmarksList by viewModel.bookmarks.collectAsState(initial = emptyList())

    val focusRequester = remember { FocusRequester() }

    val articlesLazyListState = rememberLazyListState()
    val homeScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val navState = remember(articlesLazyListState, coroutineScope) {
        DashboardNavState(articlesLazyListState, coroutineScope, viewModel::articlesForChapter)
    }

    val bookmarkEditor = remember { BookmarkEditorState() }
    var activeEditingWarningMessage by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = navState.navigationHistory.isNotEmpty()) {
        val targetArticle = navState.popBack()
        if (targetArticle != null) {
            activeTab = DashboardTab.ARTICLES
            navState.popToArticle(targetArticle)
        }
    }

    val navigateToArticleWithOrigin: (Article, Article?) -> Unit = { targetArticle, originArticle ->
        navState.pushOrigin(originArticle, targetArticle, activeTab == DashboardTab.ARTICLES)
        activeTab = DashboardTab.ARTICLES
        navState.popToArticle(targetArticle)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
            containerColor = AppCanvasYellow, // Radiant high-fidelity soft yellow canvas
        bottomBar = {
            NavigationBar(
                containerColor = SunflowerYellow, // Dynamic flag-colored yellow
                contentColor = SovereignBlue,   // Sovereign Ukrainian corporate blue
                tonalElevation = 8.dp,
                modifier = Modifier
                    .shadow(16.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                // Item 1: Chapters
                NavigationBarItem(
                    selected = activeTab == DashboardTab.CHAPTERS && !isSearchActive,
                    onClick = { 
                        navState.navigationHistory.clear()
                        activeTab = DashboardTab.CHAPTERS
                        isSearchActive = false
                    },
                    label = { Text(stringResource(R.string.tab_chapters), fontWeight = FontWeight.Bold, color = SovereignBlue, fontSize = 10.sp) },
                    icon = { Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.tab_chapters), tint = SovereignBlue, modifier = Modifier.size(20.dp)) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = NationalYellowBg
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
                            navState.navigationHistory.clear()
                            activeTab = DashboardTab.ARTICLES
                            isSearchActive = false
                        }
                    },
                    label = { Text(stringResource(R.string.tab_articles), fontWeight = FontWeight.Bold, color = SovereignBlue, fontSize = 10.sp) },
                    icon = { Icon(Icons.Default.List, contentDescription = stringResource(R.string.tab_articles), tint = SovereignBlue, modifier = Modifier.size(20.dp)) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = NationalYellowBg
                    )
                )

                // Item 3 (Center): Standard NavigationBarItem styled to look like a prominent circular button
                val homeSelected = activeTab == DashboardTab.HOME && !isSearchActive
                NavigationBarItem(
                    selected = homeSelected,
                    onClick = { 
                        navState.navigationHistory.clear()
                        activeTab = DashboardTab.HOME
                        isSearchActive = false
                    },
                    icon = {
                        CoatOfArmsBadge(
                            badgeSize = 44.dp,
                            shadowElevation = if (homeSelected) 6.dp else 2.dp,
                            gradientColors = if (homeSelected) {
                                listOf(BrightBlue, SovereignBlue)
                            } else {
                                listOf(BrightBlue.copy(alpha = 0.85f), SovereignBlue.copy(alpha = 0.82f))
                            },
                            coatSize = 28.dp
                        )
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
                    label = { Text(stringResource(R.string.tab_search), fontWeight = FontWeight.Bold, color = SovereignBlue, fontSize = 10.sp) },
                    icon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.tab_search), tint = SovereignBlue, modifier = Modifier.size(20.dp)) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = NationalYellowBg
                    )
                )

                // Item 5: Bookmarks
                NavigationBarItem(
                    selected = activeTab == DashboardTab.BOOKMARKS && !isSearchActive,
                    onClick = { 
                        navState.navigationHistory.clear()
                        activeTab = DashboardTab.BOOKMARKS
                        isSearchActive = false
                    },
                    label = { Text(stringResource(R.string.tab_bookmarks), fontWeight = FontWeight.Bold, color = SovereignBlue, fontSize = 10.sp) },
                    icon = { 
                        Icon(
                            imageVector = if (activeTab == DashboardTab.BOOKMARKS && !isSearchActive) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, 
                            contentDescription = stringResource(R.string.tab_bookmarks), 
                            tint = SovereignBlue,
                            modifier = Modifier.size(20.dp)
                        ) 
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = NationalYellowBg
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
                    .background(SunflowerYellow) // Solid flag-colored yellow
                    .statusBarsPadding()
                    .padding(top = 0.dp, bottom = 2.dp, start = 20.dp, end = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Coat of arms (Герб України) inside a beautiful circular badge to prevent shadow bleed-through
                    CoatOfArmsBadge(
                        badgeSize = 46.dp,
                        shadowElevation = 3.dp,
                        gradientColors = listOf(BrightBlue, SovereignBlue),
                        coatSize = 30.dp
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.app_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 22.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Black,
                            color = SovereignBlue
                        )
                        val context = LocalContext.current
                        Text(
                            text = stringResource(R.string.official_source),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            fontWeight = FontWeight.Bold,
                            color = SovereignBlue.copy(alpha = 0.75f),
                            modifier = Modifier
                                .clickable {
                                    openExternalUrl(context, Constants.DEFAULT_RADA_URL)
                                }
                        )
                    }
                }
            }

            if (viewModel.initializationError.isNotEmpty() || viewModel.articlesEmpty) {
                Surface(
                    color = ErrorBg,
                    contentColor = ErrorText,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ErrorBorder)
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
                        color = SovereignBlue,
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
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.tab_search), tint = SovereignBlue) },
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
                                    tint = SovereignBlue
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
                            focusedBorderColor = SovereignBlue,
                            unfocusedBorderColor = SovereignBlue.copy(alpha = 0.5f),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = SovereignBlue,
                            unfocusedTextColor = SovereignBlue
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
                    SearchResultsContent(
                        viewModel = viewModel,
                        filteredArticles = filteredArticles,
                        searchQuery = searchQuery,
                        bookmarksList = bookmarksList,
                        onNavigateToArticle = navigateToArticleWithOrigin
                    )
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
                                    val isNewChapter = selectedId != navState.currentSelectedChapterId
                                    navState.currentSelectedChapterId = selectedId
                                    activeTab = DashboardTab.ARTICLES
                                    if (isNewChapter) {
                                        navState.clickedArticleIndex = null
                                        coroutineScope.launch {
                                            articlesLazyListState.scrollToItem(0)
                                        }
                                    }
                                },
                                onOpenSourceUrl = { url ->
                                    openExternalUrl(context, url)
                                }
                            )
                        }

                        DashboardTab.ARTICLES -> {
                            ArticlesTabContent(
                                viewModel = viewModel,
                                navState = navState,
                                bookmarksList = bookmarksList,
                                onNavigateToArticle = navigateToArticleWithOrigin,
                                onNavigateToArticlesTab = { activeTab = DashboardTab.ARTICLES }
                            )
                        }

                        DashboardTab.BOOKMARKS -> {
                            BookmarksTabContent(
                                viewModel = viewModel,
                                bookmarksList = bookmarksList,
                                editor = bookmarkEditor,
                                onNavigateToArticle = navigateToArticleWithOrigin,
                                onShowEditWarning = { activeEditingWarningMessage = it }
                            )
                        }
                    }
                }
            }
        }
    }

        if (fullscreenSymbol != FullscreenSymbol.NONE) {
            FullscreenSymbolOverlay(
                symbol = fullscreenSymbol,
                onDismiss = { fullscreenSymbol = FullscreenSymbol.NONE }
            )
        }

        activeEditingWarningMessage?.let { msg ->
            EditingWarningToast(message = msg, onDismiss = { activeEditingWarningMessage = null })
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
            color = SovereignBlue,
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
                    val firstId = formatArticleId(chapterArticles.first().id)
                    val lastId = formatArticleId(chapterArticles.last().id)
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
                    border = BorderStroke(1.5.dp, SovereignBlue)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SovereignBlue),
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
                                    color = SovereignBlue
                                )
                                if (rangeText.isNotEmpty()) {
                                    Text(
                                        text = "($rangeText)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                        fontWeight = FontWeight.Black,
                                        color = SovereignBlue.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Text(
                                text = chapter.titleUa,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SovereignBlue
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
                                    tint = SovereignBlue.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                 )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = stringResource(R.string.open_chapter_articles),
                            tint = SovereignBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarksTabContent(
    viewModel: ConstitutionViewModel,
    bookmarksList: List<ua.constitution.data.database.BookmarkEntity>,
    editor: BookmarkEditorState,
    onNavigateToArticle: (Article, Article?) -> Unit,
    onShowEditWarning: (String) -> Unit
) {
    val context = LocalContext.current
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
            color = SovereignBlue,
            modifier = Modifier.padding(vertical = 10.dp)
        )

        LaunchedEffect(editor.editingArticleId) {
            editor.panelExpanded = false
        }

         val bookmarksListState = rememberLazyListState()
         val coroutineScope = rememberCoroutineScope()

         LaunchedEffect(bookmarksListState.isScrollInProgress) {
             if (bookmarksListState.isScrollInProgress && editor.panelExpanded) {
                 editor.panelExpanded = false
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
                                tint = SovereignBlue.copy(alpha = 0.2f),
                                modifier = Modifier.size(72.dp)
                            )
                            Text(
                                text = stringResource(R.string.no_bookmarks_msg),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SovereignBlue.copy(alpha = 0.6f)
                            )
                            Text(
                                text = stringResource(R.string.bookmarks_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SovereignBlue.copy(alpha = 0.5f),
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
                     val editsJson = bookmarksList.editsJsonFor(article)
                     val isCurrentEditable = true
                     val onSaveCallback: (String) -> Unit = { newJson ->
                         viewModel.updateBookmarkEdits(article.bookmarkId, newJson)
                     }
                    ArticleCard(
                        article = article,
                        isBookmarked = true,
                        onToggleBookmark = {
                            if (editor.editingArticleId == article.bookmarkId) {
                                editor.editingArticleId = null
                            }
                            viewModel.toggleBookmark(article.bookmarkId)
                        },
                        onArticleClick = { target -> onNavigateToArticle(target, article) },
                        resolveArticleLink = viewModel::resolveLink,
                        initialEditsJson = editsJson,
                        editing = ArticleEditing(
                            isEditable = isCurrentEditable,
                            isCurrentlyEditing = (editor.editingArticleId == article.bookmarkId),
                            isEditButtonEnabled = (editor.editingArticleId == null || editor.editingArticleId == article.bookmarkId),
                            isPanelExpanded = editor.panelExpanded,
                            activeTool = editor.activeTool,
                            selectedColorHex = if (editor.activeTool == Constants.TOOL_UNDERLINE) editor.underlineColorHex else editor.markerColorHex,
                            selectedMarkerColorHex = editor.markerColorHex,
                            selectedUnderlineColorHex = editor.underlineColorHex,
                            onSaveEdits = onSaveCallback,
                            onDisabledEditClick = {
                                onShowEditWarning(context.getString(R.string.save_edits_error))
                            },
                            onPanelExpandedChange = { editor.panelExpanded = it },
                            onActiveToolChange = { tool ->
                                editor.activeTool = tool
                            },
                            onColorHexChange = { color ->
                                if (editor.activeTool == Constants.TOOL_UNDERLINE) {
                                    editor.underlineColorHex = color
                                } else {
                                    editor.markerColorHex = color
                                }
                            },
                            onToggleEditing = {
                                if (editor.editingArticleId == article.bookmarkId) {
                                    editor.editingArticleId = null
                                } else {
                                    editor.editingArticleId = article.bookmarkId
                                    editor.panelExpanded = false
                                    editor.activeTool = Constants.TOOL_NONE
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