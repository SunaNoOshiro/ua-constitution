package ua.constitution.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ua.constitution.R
import ua.constitution.data.model.Article
import ua.constitution.ui.article.ArticleCard
import ua.constitution.ui.article.ArticleEditing
import ua.constitution.ui.editsJsonFor
import ua.constitution.ui.notesFor
import ua.constitution.ui.theme.LocalAppColors
import ua.constitution.ui.theme.SovereignBlue
import ua.constitution.ui.viewmodel.ConstitutionViewModel
import ua.constitution.utils.Constants

@Composable
fun BookmarksTabContent(
    viewModel: ConstitutionViewModel,
    bookmarksList: List<ua.constitution.data.database.BookmarkEntity>,
    editor: BookmarkEditorState,
    onNavigateToArticle: (Article, Article?) -> Unit,
    onShowEditWarning: (String) -> Unit
) {
    val context = LocalContext.current
    val appColors = LocalAppColors.current
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
            color = appColors.textHeading,
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
                                color = appColors.textSecondary
                            )
                            Text(
                                text = stringResource(R.string.bookmarks_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = appColors.textSecondary,
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
                     val onSaveCallback: (String) -> Unit = { newJson ->
                         viewModel.updateBookmarkEdits(article.bookmarkId, newJson)
                     }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            isEditable = true,
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
                    if (editor.editingArticleId == article.bookmarkId) {
                        BookmarkNoteEditor(
                            initialNote = bookmarksList.notesFor(article),
                            onSaveNote = { note -> viewModel.updateNotes(article.bookmarkId, note) },
                        )
                    }
                    }
                }
            }
        }
    }
}
