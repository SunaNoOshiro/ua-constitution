package ua.constitution.ui.article

import ua.constitution.R
import ua.constitution.utils.Constants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.constitution.data.model.Note
import ua.constitution.data.model.Article
import ua.constitution.ui.theme.*
import androidx.compose.foundation.text.selection.SelectionContainer
import ua.constitution.domain.title.parseArticleTitle
import ua.constitution.domain.bookmark.BookmarkEditsParser
import ua.constitution.domain.bookmark.reconcileEditsOnInput
import ua.constitution.domain.text.ArticleNumberFormatter
import ua.constitution.domain.text.BackNavigationTarget
import ua.constitution.domain.text.backNavigationTarget
import ua.constitution.domain.text.formatArticleForCopy
import ua.constitution.domain.text.StyledRange
import ua.constitution.domain.text.ParagraphRangeMapping
import ua.constitution.ui.openExternalUrl
import ua.constitution.ui.copyToClipboardWithToast

@Composable
fun ArticleCard(
    article: Article,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    resolveArticleLink: (String) -> Article?,
    modifier: Modifier = Modifier,
    onArticleClick: ((Article) -> Unit)? = null,
    initialEditsJson: String = "",
    editing: ArticleEditing = ArticleEditing()
) {
    val context = LocalContext.current

    // Unpack the grouped editing parameter object into the names the body already uses, so the
    // long parameter list collapses to one cohesive object without touching the rendering code.
    val isEditable = editing.isEditable
    val isCurrentlyEditing = editing.isCurrentlyEditing
    val isEditButtonEnabled = editing.isEditButtonEnabled
    val isPanelExpanded = editing.isPanelExpanded
    val activeTool = editing.activeTool
    val selectedColorHex = editing.selectedColorHex
    val selectedMarkerColorHex = editing.selectedMarkerColorHex
    val selectedUnderlineColorHex = editing.selectedUnderlineColorHex
    val onSaveEdits = editing.onSaveEdits
    val onToggleEditing = editing.onToggleEditing
    val onPanelExpandedChange = editing.onPanelExpandedChange
    val onActiveToolChange = editing.onActiveToolChange
    val onColorHexChange = editing.onColorHexChange
    val onDisabledEditClick = editing.onDisabledEditClick

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
        reconcileEditsOnInput(initialEditsJson, isEditable, lastSavedJson)?.let { localEdits = it }
        lastSavedJson = ""
    }

    if (showRemoveConfirmDialog) {
        RemoveBookmarkDialog(
            onConfirm = {
                showRemoveConfirmDialog = false
                onToggleBookmark()
            },
            onDismiss = { showRemoveConfirmDialog = false }
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
            color = SovereignBlue
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
                    color = SovereignBlue,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { openExternalUrl(context, article.radaUrl) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = stringResource(R.string.read_source_btn),
                        tint = SovereignBlue.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        copyToClipboardWithToast(
                            context,
                            context.getString(R.string.tab_articles),
                            formatArticleForCopy(articleNumber, articleName, article.paragraphs.map { it.text })
                        )
                    },
                    modifier = Modifier.size(36.dp).testTag("copy_article_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.copy_article),
                        tint = SovereignBlue.copy(alpha = 0.85f),
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
                             tint = if (isCurrentlyEditing) SuccessGreen else if (isEditButtonEnabled) SovereignBlue else SlateMuted,
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
                        tint = if (isBookmarked) SovereignBlue else SovereignBlue.copy(alpha = 0.4f),
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
                        color = SovereignBlue,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCurrentlyEditing) 4.dp else 14.dp))

            if (isCurrentlyEditing) {
                GlobalFormattingPanel(
                    editingArticleId = article.bookmarkId,
                    controls = FormattingPanelControls(
                        activeTool = activeTool,
                        onActiveToolChange = { onActiveToolChange?.invoke(it) },
                        selectedColorHex = selectedColorHex,
                        onColorHexChange = { onColorHexChange?.invoke(it) },
                        isPanelExpanded = isPanelExpanded,
                        onPanelExpandedChange = { onPanelExpandedChange?.invoke(it) }
                    ),
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
                            textStyle = SegmentTextStyle(MaterialTheme.typography.bodyMedium),
                            onArticleClick = onArticleClick,
                            resolveArticleLink = resolveArticleLink,
                            editing = SegmentEditing(
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
                                    textStyle = SegmentTextStyle(MaterialTheme.typography.bodyMedium),
                                    onArticleClick = onArticleClick,
                                    resolveArticleLink = resolveArticleLink,
                                    editing = SegmentEditing(
                                        onUpdateRanges = null,
                                        selectedMarkerColorHex = selectedMarkerColorHex,
                                        selectedUnderlineColorHex = selectedUnderlineColorHex,
                                        onSelectedMarkerColorChange = null,
                                        onSelectedUnderlineColorChange = null,
                                        fullArticleTextToCopy = fullArticleTextToCopy
                                    )
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

@Composable
fun formatArticleId(id: Int): String =
    ArticleNumberFormatter.formatWithPreamble(id, stringResource(R.string.preamble))

@Composable
fun getBackNavigationText(article: Article): String {
    val artLabel = formatArticleId(article.id)
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
    modifier: Modifier = Modifier
) {
    val preambleText = stringResource(R.string.preamble)
    val text = remember(id, preambleText) {
        if (id == 0) {
            buildAnnotatedString {
                append(preambleText)
            }
        } else if (ArticleNumberFormatter.isFractional(id)) {
            val (base, suffix) = ArticleNumberFormatter.fractionalParts(id)
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
    val isFractional = ArticleNumberFormatter.isFractional(id)
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
        color = SlateBg,
        border = BorderStroke(0.5.dp, SlateDivider)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SegmentedText(
                segments = note.content,
                textStyle = SegmentTextStyle(
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateText,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic
                ),
                onArticleClick = onArticleClick,
                resolveArticleLink = resolveArticleLink
            )
        }
    }
}
