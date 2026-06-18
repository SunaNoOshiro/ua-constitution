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
import ua.constitution.domain.title.parseArticleTitle
import ua.constitution.domain.bookmark.BookmarkEditsParser
import ua.constitution.domain.text.ArticleNumberFormatter
import ua.constitution.domain.text.BackNavigationTarget
import ua.constitution.domain.text.backNavigationTarget
import ua.constitution.domain.text.formatArticleForCopy
import ua.constitution.domain.text.StyledRange
import ua.constitution.domain.text.ParagraphRangeMapping
import ua.constitution.domain.text.getWordRangeAtOffset
import ua.constitution.domain.text.getWordSnappedRange
import ua.constitution.domain.text.mapFormattedToOriginal
import ua.constitution.domain.text.mapOriginalToFormatted
import ua.constitution.domain.text.mergeAdjacentStyledRanges
import ua.constitution.ui.safeParseColor

@Composable
fun ArticleCard(
    article: Article,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
    onArticleClick: ((Article) -> Unit)? = null,
    resolveArticleLink: (String) -> Article?,
    isEditable: Boolean = false,
    initialEditsJson: String = "",
    onSaveEdits: ((String) -> Unit)? = null,
    isCurrentlyEditing: Boolean = false,
    onToggleEditing: (() -> Unit)? = null,
    activeTool: String = Constants.TOOL_MARKER,
    selectedColorHex: String = Constants.COLOR_DEFAULT_MARKER,
    selectedMarkerColorHex: String = Constants.COLOR_DEFAULT_MARKER,
    selectedUnderlineColorHex: String = Constants.COLOR_DEFAULT_UNDERLINE,
    isEditButtonEnabled: Boolean = true,
    isPanelExpanded: Boolean = false,
    onPanelExpandedChange: ((Boolean) -> Unit)? = null,
    onActiveToolChange: ((String) -> Unit)? = null,
    onColorHexChange: ((String) -> Unit)? = null,
    onDisabledEditClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    
    // Split the title (e.g., "Стаття 20. Державні символи України") into number and title
    val titleParts = remember(article.id, article.titleUa) { parseArticleTitle(article.titleUa) }
    val articleNumber = titleParts.display
    val articleName = titleParts.name

    var localEdits by remember(isEditable) { mutableStateOf(emptyMap<Int, List<StyledRange>>()) }
    var lastSavedJson by remember { mutableStateOf("") }
    
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
                            val textToCopy = formatArticleForCopy(articleNumber, articleName, article.paragraphs.map { it.text })
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
                formatArticleForCopy(articleNumber, articleName, article.paragraphs.map { it.text })
            }

            val combinedSegments = remember(article.paragraphs) {
                ParagraphRangeMapping.flattenToSegments(article.paragraphs)
            }

            val paragraphOffsets = remember(article.paragraphs) {
                ParagraphRangeMapping.paragraphOffsets(article.paragraphs)
            }

            val combinedRanges = remember(localEdits, paragraphOffsets) {
                ParagraphRangeMapping.toCombined(localEdits, paragraphOffsets)
            }

            val onUpdateCombinedRanges: (List<StyledRange>) -> Unit = { newCombinedRanges ->
                val newMap = ParagraphRangeMapping.toPerParagraph(
                    newCombinedRanges, article.paragraphs, paragraphOffsets
                )
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
                            resolveArticleLink = resolveArticleLink,
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
                                NoteCard(note = note, onArticleClick = onArticleClick, resolveArticleLink = resolveArticleLink)
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
                                    resolveArticleLink = resolveArticleLink,
                                    onUpdateRanges = null,
                                    selectedMarkerColorHex = selectedMarkerColorHex,
                                    selectedUnderlineColorHex = selectedUnderlineColorHex,
                                    onSelectedMarkerColorChange = null,
                                    onSelectedUnderlineColorChange = null,
                                    fullArticleTextToCopy = fullArticleTextToCopy
                                )
                                paragraph.notes.forEach { note ->
                                    NoteCard(note = note, onArticleClick = onArticleClick, resolveArticleLink = resolveArticleLink)
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
fun formatArticleId(id: Int, chapterId: Int = 0): String =
    ArticleNumberFormatter.formatWithPreamble(id, chapterId, stringResource(R.string.preamble))

@Composable
fun getBackNavigationText(article: Article): String {
    val artLabel = formatArticleId(article.id, article.chapterId)
    return when (backNavigationTarget(article.chapterId)) {
        BackNavigationTarget.CHAPTER_15 -> stringResource(R.string.back_to_chapter_15, artLabel)
        BackNavigationTarget.PREAMBLE -> stringResource(R.string.back_to_preamble)
        BackNavigationTarget.OTHER -> stringResource(R.string.back_to_chapter_article, article.chapterId, artLabel)
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
        } else if (ArticleNumberFormatter.isFractional(id, chapterId)) {
            val (base, suffix) = ArticleNumberFormatter.fractionalParts(id, chapterId)
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
    val isFractional = ArticleNumberFormatter.isFractional(id, chapterId)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteCard(note: Note, resolveArticleLink: (String) -> Article?, modifier: Modifier = Modifier, onArticleClick: ((Article) -> Unit)? = null) {
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
                onArticleClick = onArticleClick,
                resolveArticleLink = resolveArticleLink
            )
        }
    }
}
