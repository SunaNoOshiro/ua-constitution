package ua.constitution.ui.screens

import ua.constitution.R
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.constitution.data.model.Article
import ua.constitution.data.settings.SettingsRepository
import ua.constitution.ui.theme.*
import ua.constitution.ui.viewmodel.ConstitutionViewModel
import ua.constitution.ui.model.DashboardTab
import ua.constitution.ui.model.FullscreenSymbol
import ua.constitution.ui.ImmersiveFullscreenEffect
import ua.constitution.ui.openExternalUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppDashboard(viewModel: ConstitutionViewModel, settings: SettingsRepository) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(DashboardTab.HOME) } // Default to Home (Державні символи)
    var isSearchActive by remember { mutableStateOf(false) } // Controls immediate search bar drop

    var fullscreenSymbol by remember { mutableStateOf(FullscreenSymbol.NONE) }
    var showSettings by remember { mutableStateOf(false) }

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
            containerColor = LocalAppColors.current.canvas, // Yellow canvas (light) / navy (dark)
        bottomBar = {
            NavigationBar(
                containerColor = LocalAppColors.current.brandSurface, // yellow (light) / dark navy (dark)
                contentColor = LocalAppColors.current.onBrand,
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
                    label = { Text(stringResource(R.string.tab_chapters), fontWeight = FontWeight.Bold, color = LocalAppColors.current.onBrand, fontSize = 10.sp) },
                    icon = { Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.tab_chapters), tint = LocalAppColors.current.onBrand, modifier = Modifier.size(20.dp)) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = LocalAppColors.current.navIndicator
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
                    label = { Text(stringResource(R.string.tab_articles), fontWeight = FontWeight.Bold, color = LocalAppColors.current.onBrand, fontSize = 10.sp) },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.tab_articles), tint = LocalAppColors.current.onBrand, modifier = Modifier.size(20.dp)) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = LocalAppColors.current.navIndicator
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
                    label = { Text(stringResource(R.string.tab_search), fontWeight = FontWeight.Bold, color = LocalAppColors.current.onBrand, fontSize = 10.sp) },
                    icon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.tab_search), tint = LocalAppColors.current.onBrand, modifier = Modifier.size(20.dp)) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = LocalAppColors.current.navIndicator
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
                    label = { Text(stringResource(R.string.tab_bookmarks), fontWeight = FontWeight.Bold, color = LocalAppColors.current.onBrand, fontSize = 10.sp) },
                    icon = { 
                        Icon(
                            imageVector = if (activeTab == DashboardTab.BOOKMARKS && !isSearchActive) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, 
                            contentDescription = stringResource(R.string.tab_bookmarks),
                            tint = LocalAppColors.current.onBrand,
                            modifier = Modifier.size(20.dp)
                        ) 
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = LocalAppColors.current.navIndicator
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
                    .background(LocalAppColors.current.brandSurface) // yellow (light) / dark navy (dark)
                    .statusBarsPadding()
                    .padding(top = 0.dp, bottom = 2.dp, start = 20.dp, end = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            color = LocalAppColors.current.onBrand
                        )
                        val context = LocalContext.current
                        Text(
                            text = stringResource(R.string.official_source),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            fontWeight = FontWeight.Bold,
                            color = LocalAppColors.current.onBrand.copy(alpha = 0.75f),
                            modifier = Modifier
                                .clickable {
                                    openExternalUrl(context, Constants.DEFAULT_RADA_URL)
                                }
                        )
                    }

                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("open_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_open),
                            tint = LocalAppColors.current.onBrand,
                            modifier = Modifier.size(22.dp)
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
                        color = LocalAppColors.current.textHeading,
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
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.tab_search), tint = LocalAppColors.current.textHeading) },
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
                                    tint = LocalAppColors.current.textHeading
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
                            focusedBorderColor = LocalAppColors.current.textHeading,
                            unfocusedBorderColor = LocalAppColors.current.textHeading.copy(alpha = 0.5f),
                            focusedContainerColor = LocalAppColors.current.cardSurface,
                            unfocusedContainerColor = LocalAppColors.current.cardSurface,
                            focusedTextColor = LocalAppColors.current.textPrimary,
                            unfocusedTextColor = LocalAppColors.current.textPrimary
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

        if (showSettings) {
            SettingsSheet(
                fontScale = settings.fontScale.collectAsState().value,
                themeMode = settings.themeMode.collectAsState().value,
                onFontScaleChange = settings::setFontScale,
                onThemeModeChange = settings::setThemeMode,
                onDismiss = { showSettings = false }
            )
        }
    }

}