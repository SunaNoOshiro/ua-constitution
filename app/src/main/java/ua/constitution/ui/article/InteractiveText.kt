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
import ua.constitution.audio.ProceduralAnthemSynth
import ua.constitution.ui.model.DashboardTab
import ua.constitution.ui.model.FullscreenSymbol
import ua.constitution.domain.title.parseArticleTitle
import ua.constitution.domain.bookmark.BookmarkEditsParser
import ua.constitution.domain.text.ArticleNumberFormatter
import ua.constitution.domain.text.StyledRange
import ua.constitution.domain.text.RangeStyler
import ua.constitution.domain.text.formatStringToSuperscript
import ua.constitution.domain.text.getWordRangeAtOffset
import ua.constitution.domain.text.getWordSnappedRange
import ua.constitution.domain.text.mapFormattedToOriginal
import ua.constitution.domain.text.mapOriginalToFormatted
import ua.constitution.domain.text.mergeAdjacentStyledRanges
import ua.constitution.domain.content.mergeAdjacentLinkSegments
import ua.constitution.ui.safeParseColor

@Composable
fun SegmentedTextWithEdits(
    segments: List<ua.constitution.data.model.ContentSegment>,
    ranges: List<StyledRange>,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color(0xFF0F172A),
    lineHeight: androidx.compose.ui.unit.TextUnit = 24.sp,
    fontWeight: FontWeight = FontWeight.Medium,
    fontStyle: FontStyle? = null,
    onArticleClick: ((Article) -> Unit)? = null,
    resolveArticleLink: ((String) -> Article?)? = null,
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
    
    val mergedSegments = remember(segments) { mergeAdjacentLinkSegments(segments) }

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

    // Highlight/underline overlay shared by the editable and read-only render paths.
    val drawStyledOverlay: androidx.compose.ui.graphics.drawscope.ContentDrawScope.() -> Unit = {
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
            onUpdateRanges?.invoke(RangeStyler.applyStyle(originalText, ranges, wordRanges, tool, colorHex))
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
                                            val targetArticle = resolveArticleLink?.invoke(segmentText)
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
                        .drawWithContent(drawStyledOverlay)
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
                .drawWithContent(drawStyledOverlay)
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
                                                        val targetArticle = resolveArticleLink?.invoke(segmentText)
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
fun SegmentedText(
    segments: List<ua.constitution.data.model.ContentSegment>,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color(0xFF0F172A),
    lineHeight: androidx.compose.ui.unit.TextUnit = 24.sp,
    fontWeight: FontWeight = FontWeight.Medium,
    fontStyle: FontStyle? = null,
    onArticleClick: ((Article) -> Unit)? = null,
    resolveArticleLink: ((String) -> Article?)? = null
) {
    val context = LocalContext.current

    // Merge adjacent link segments that share the same URL to prevent split link issues (e.g., 149-1)
    val mergedSegments = remember(segments) { mergeAdjacentLinkSegments(segments) }

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
                            val targetArticle = resolveArticleLink?.invoke(segmentText)
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
