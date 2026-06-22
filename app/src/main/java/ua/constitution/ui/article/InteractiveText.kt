package ua.constitution.ui.article

import android.content.Intent
import ua.constitution.R
import ua.constitution.utils.Constants
import ua.constitution.utils.LogMessages
import android.net.Uri
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.constitution.data.model.Article
import ua.constitution.ui.theme.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import ua.constitution.domain.text.StyledRange
import ua.constitution.domain.text.RangeStyler
import ua.constitution.domain.text.formatStringToSuperscript
import ua.constitution.domain.text.mapFormattedToOriginal
import ua.constitution.domain.text.mapOriginalToFormatted
import ua.constitution.domain.content.mergeAdjacentLinkSegments
import ua.constitution.domain.link.handleLinkAnnotationTap
import ua.constitution.ui.createEraserIcon
import ua.constitution.ui.safeParseColor
import ua.constitution.ui.copyToClipboardWithToast
import ua.constitution.ui.selectionToolbarOffset

/**
 * Handles a tapped URL annotation in a reader: resolves an internal article cross-reference via
 * [resolveArticleLink]/[onArticleClick], otherwise launches the external URL; failures are logged
 * under [logTag]. Shared by the three reader tap sites (BasicTextField edit-mode, BasicTextField
 * read-only, and the read-only ClickableText), which were byte-identical apart from the log tag.
 * Deliberately NOT routed through ui.openExternalUrl — that helper is silent, this one logs.
 */
private fun openAnnotatedLink(
    item: String,
    context: Context,
    resolveArticleLink: ((String) -> Article?)?,
    onArticleClick: ((Article) -> Unit)?,
    logTag: String,
) {
    try {
        handleLinkAnnotationTap(
            item,
            Constants.DEFAULT_RADA_URL,
            resolveArticleLink,
            onArticleClick,
        ) { finalUrl ->
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)))
        }
    } catch (e: Exception) {
        android.util.Log.e(logTag, LogMessages.openUrlFailed(item, e.message), e)
    }
}

@Composable
fun SegmentedTextWithEdits(
    segments: List<ua.constitution.data.model.ContentSegment>,
    ranges: List<StyledRange>,
    modifier: Modifier = Modifier,
    textStyle: SegmentTextStyle = SegmentTextStyle(MaterialTheme.typography.bodyMedium),
    onArticleClick: ((Article) -> Unit)? = null,
    resolveArticleLink: ((String) -> Article?)? = null,
    editing: SegmentEditing = SegmentEditing()
) {
    val context = LocalContext.current
    // Unpack the grouped editing object into the names the body already uses.
    val onUpdateRanges = editing.onUpdateRanges
    val selectedMarkerColorHex = editing.selectedMarkerColorHex
    val selectedUnderlineColorHex = editing.selectedUnderlineColorHex
    val onSelectedMarkerColorChange = editing.onSelectedMarkerColorChange
    val onSelectedUnderlineColorChange = editing.onSelectedUnderlineColorChange
    val activeTool = editing.activeTool
    val selectedColorHex = editing.selectedColorHex
    val fullArticleTextToCopy = editing.fullArticleTextToCopy
    var textLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }

    val mergedSegments = remember(segments) { mergeAdjacentLinkSegments(segments) }

    val originalText = remember(mergedSegments) {
        mergedSegments.joinToString("") { it.displayText }
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
        buildSegmentedAnnotatedString(mergedSegments, formattedText, origToFormMapping)
    }

    val markerColors = HighlightPalette.MARKER_COLORS
    val underlineColors = HighlightPalette.UNDERLINE_COLORS

    val lastMarkerColor = selectedMarkerColorHex
    val lastUnderlineColor = selectedUnderlineColorHex

    val eraserIcon = remember { createEraserIcon(Color.White) }

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

    val customTextToolbar = rememberStylingTextToolbar(
        isShown = { menuRect != null },
        onShowMenu = { rect, callbacks ->
            menuRect = rect
            menuCallbacks = callbacks
        },
        onHide = {
            menuRect = null
            menuCallbacks = null
        }
    )

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
                                    openAnnotatedLink(annotation.item, context, resolveArticleLink, onArticleClick, LogMessages.TAG_SEGMENTED_TEXT_EDITS)
                                }
                        }
                    },
                    readOnly = true,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.Transparent),
                    textStyle = textStyle.toTextStyle(),
                    onTextLayout = { textLayoutResult = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .styledOverlay({ textLayoutResult }, ranges, originalText.length, origToFormMapping)
                )

                // High Z-index interactive overlay solely to capture and handle styling gestures (swiping over words)
                if (isStylingToolActive) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .styleOnWordGesture(
                                keys = listOf(annotatedString, activeTool, selectedColorHex),
                                getLayout = { textLayoutResult },
                                formToOrigMapping = formToOrigMapping,
                                originalText = originalText,
                                activeTool = { currentActiveTool },
                                selectedColorHex = { currentSelectedColorHex },
                                applyToRanges = { words, tool, colorHex ->
                                    currentApplyStyleToRanges(words, tool, colorHex)
                                }
                            )
                    )
                }
            }
        }
    } else {
        androidx.compose.material3.Text(
            text = annotatedString,
            style = textStyle.toTextStyle(),
            onTextLayout = { textLayoutResult = it },
            modifier = modifier
                .styledOverlay({ textLayoutResult }, ranges, originalText.length, origToFormMapping)
                .then(
                    Modifier.pointerInput(annotatedString) {
                        detectTapGestures(
                            onTap = { offset ->
                                textLayoutResult?.let { layoutResult ->
                                    val position = layoutResult.getOffsetForPosition(offset)
                                    if (position in 0..annotatedString.length) {
                                        annotatedString.getStringAnnotations(tag = Constants.ANNOTATION_TAG_URL, start = position, end = position)
                                            .firstOrNull()?.let { annotation ->
                                                openAnnotatedLink(annotation.item, context, resolveArticleLink, onArticleClick, LogMessages.TAG_SEGMENTED_TEXT_EDITS)
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
        val density = LocalDensity.current
        val popupPositionProvider = remember(currentMenuRect) {
            object : androidx.compose.ui.window.PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: androidx.compose.ui.unit.IntRect,
                    windowSize: androidx.compose.ui.unit.IntSize,
                    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                    popupContentSize: androidx.compose.ui.unit.IntSize
                ): androidx.compose.ui.unit.IntOffset = selectionToolbarOffset(
                    selectionRect = currentMenuRect,
                    anchorBounds = anchorBounds,
                    windowSize = windowSize,
                    popupContentSize = popupContentSize,
                    gapPx = with(density) { 8.dp.toPx() },
                    bufferPx = with(density) { 60.dp.toPx() }
                )
            }
        }

        // Action lambdas keep the selection/menu/text-field state mutation in this composable;
        // SelectionToolbarPopup is a dumb view that just invokes them (moved verbatim).
        val applyMarker: (String) -> Unit = { colorHex ->
            textFieldValue = textFieldValue.copy(selection = androidx.compose.ui.text.TextRange.Zero)
            menuRect = null
            menuCallbacks = null
            applyStyleToRange(mappedStart, mappedEnd, Constants.TOOL_MARKER, colorHex)
            onSelectedMarkerColorChange?.invoke(colorHex)
        }
        val applyUnderline: (String) -> Unit = { colorHex ->
            textFieldValue = textFieldValue.copy(selection = androidx.compose.ui.text.TextRange.Zero)
            menuRect = null
            menuCallbacks = null
            applyStyleToRange(mappedStart, mappedEnd, Constants.TOOL_UNDERLINE, colorHex)
            onSelectedUnderlineColorChange?.invoke(colorHex)
        }
        val applyEraser: () -> Unit = {
            textFieldValue = textFieldValue.copy(selection = androidx.compose.ui.text.TextRange.Zero)
            menuRect = null
            menuCallbacks = null
            applyStyleToRange(mappedStart, mappedEnd, Constants.TOOL_ERASER, "#FFFFFF")
        }
        val performCopy: () -> Unit = {
            if (fullArticleTextToCopy != null && selStart == 0 && selEnd == textFieldValue.text.length) {
                copyToClipboardWithToast(context, context.getString(R.string.article_label), fullArticleTextToCopy)
            } else {
                menuCallbacks?.onCopy?.invoke()
            }
            menuRect = null
            menuCallbacks = null
            textFieldValue = textFieldValue.copy(selection = androidx.compose.ui.text.TextRange.Zero)
        }
        val performSelectAll: () -> Unit = {
            val onSelectAll = menuCallbacks?.onSelectAll
            if (onSelectAll != null) {
                onSelectAll()
            } else {
                textFieldValue = textFieldValue.copy(
                    selection = androidx.compose.ui.text.TextRange(0, textFieldValue.text.length)
                )
            }
        }

        SelectionToolbarPopup(
            positionProvider = popupPositionProvider,
            onDismissRequest = {
                android.util.Log.d(LogMessages.TAG_SELECTION_BUG, LogMessages.POPUP_DISMISS_PRESERVE_SELECTION)
                menuRect = null
                menuCallbacks = null
            },
            selectionKey = currentMenuRect,
            showCopy = menuCallbacks?.onCopy != null,
            onCopy = performCopy,
            onSelectAll = performSelectAll,
            formatting = FormattingTools(
                showFormatting = onUpdateRanges != null,
                markerColors = markerColors,
                underlineColors = underlineColors,
                lastMarkerColor = lastMarkerColor,
                lastUnderlineColor = lastUnderlineColor,
                eraserIcon = eraserIcon,
                showEraser = hasEditsInSelection,
                onApplyMarker = applyMarker,
                onApplyUnderline = applyUnderline,
                onApplyEraser = applyEraser
            )
        )
    }
}

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
    textStyle: SegmentTextStyle = SegmentTextStyle(MaterialTheme.typography.bodyMedium),
    onArticleClick: ((Article) -> Unit)? = null,
    resolveArticleLink: ((String) -> Article?)? = null
) {
    val context = LocalContext.current

    // Merge adjacent link segments that share the same URL to prevent split link issues (e.g., 149-1)
    val mergedSegments = remember(segments) { mergeAdjacentLinkSegments(segments) }

    val annotatedString = remember(mergedSegments) {
        buildAnnotatedString {
            mergedSegments.forEach { segment ->
                if (segment.type == Constants.TYPE_LINK) {
                    pushStringAnnotation(tag = Constants.ANNOTATION_TAG_URL, annotation = "${segment.url}|${segment.text}")
                    withStyle(
                        style = SpanStyle(
                            color = SovereignBlue,
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
        style = textStyle.toTextStyle(),
        modifier = modifier,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = Constants.ANNOTATION_TAG_URL, start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    openAnnotatedLink(annotation.item, context, resolveArticleLink, onArticleClick, LogMessages.TAG_SEGMENTED_TEXT)
                }
        }
    )
}

/**
 * One color-picker sub-row of the selection toolbar (a back arrow + divider + tappable swatches).
 * Shared verbatim by the MARKER and UNDERLINE pickers, which differ only in their swatch list,
 * selected color, default-parse color, swatch testTag and pick handler. The [swatchTestTag] is kept
 * identical to the inlined originals so SelectionToolbarPopupTest's pins still resolve.
 */
@Composable
private fun ColorPickerRow(
    colors: List<String>,
    selectedColorHex: String,
    defaultColor: Color,
    swatchTestTag: String,
    onBack: () -> Unit,
    onPick: (String) -> Unit,
) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.btn_back),
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(16.dp)
                .background(SlateText) // slate-600
        )
        colors.forEach { colorHex ->
            val colorVal = safeParseColor(colorHex, defaultColor)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(colorVal)
                    .border(
                        width = if (selectedColorHex == colorHex) 2.dp else 1.dp,
                        color = if (selectedColorHex == colorHex) Color.White else Color.Gray.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
                    .testTag(swatchTestTag)
                    .clickable { onPick(colorHex) }
            )
        }
    }
}

/**
 * The floating selection toolbar (copy / select-all / marker / underline / eraser, with color
 * pickers). A "dumb" presentational composable: all state-mutating actions are supplied as lambdas
 * by the caller (SegmentedTextWithEdits), so this can be rendered and tested in isolation. Only the
 * marker/underline color-picker sub-mode is its own internal state, reset per [selectionKey].
 */
@Composable
internal fun SelectionToolbarPopup(
    positionProvider: androidx.compose.ui.window.PopupPositionProvider,
    onDismissRequest: () -> Unit,
    selectionKey: Any?,
    showCopy: Boolean,
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    formatting: FormattingTools
) {
    val showFormatting = formatting.showFormatting
    val markerColors = formatting.markerColors
    val underlineColors = formatting.underlineColors
    val lastMarkerColor = formatting.lastMarkerColor
    val lastUnderlineColor = formatting.lastUnderlineColor
    val eraserIcon = formatting.eraserIcon
    val showEraser = formatting.showEraser
    val onApplyMarker = formatting.onApplyMarker
    val onApplyUnderline = formatting.onApplyUnderline
    val onApplyEraser = formatting.onApplyEraser
    var showedColorPickerMode by remember(selectionKey) { mutableStateOf<String?>(null) }

    androidx.compose.ui.window.Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SlateDark, // slate-800
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, SlateDarker), // slate-700
            modifier = Modifier.padding(2.dp)
        ) {
            if (showedColorPickerMode == Constants.TOOL_MARKER) {
                ColorPickerRow(
                    colors = markerColors,
                    selectedColorHex = lastMarkerColor,
                    defaultColor = Color.Yellow,
                    swatchTestTag = "toolbar_marker_swatch",
                    onBack = { showedColorPickerMode = null },
                    onPick = {
                        onApplyMarker(it)
                        showedColorPickerMode = null
                    }
                )
            } else if (showedColorPickerMode == Constants.TOOL_UNDERLINE) {
                ColorPickerRow(
                    colors = underlineColors,
                    selectedColorHex = lastUnderlineColor,
                    defaultColor = Color.Red,
                    swatchTestTag = "toolbar_underline_swatch",
                    onBack = { showedColorPickerMode = null },
                    onPick = {
                        onApplyUnderline(it)
                        showedColorPickerMode = null
                    }
                )
            } else {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (showCopy) {
                        IconButton(
                            onClick = onCopy,
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
                        onClick = onSelectAll,
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
                    if (showFormatting) {
                        // Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(SlateText) // slate-600
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
                                        .testTag("toolbar_marker_picker")
                                        .clickable { showedColorPickerMode = Constants.TOOL_MARKER }
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
                                onClick = { onApplyMarker(lastMarkerColor) },
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
                                .background(SlateText) // slate-600
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
                                        .testTag("toolbar_underline_picker")
                                        .clickable { showedColorPickerMode = Constants.TOOL_UNDERLINE }
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
                                onClick = { onApplyUnderline(lastUnderlineColor) },
                                iconSize = 15.dp,
                                fontSize = 12.sp,
                                paddingHorizontal = 2.dp,
                                paddingVertical = 4.dp
                            )
                        }

                        if (showEraser) {
                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(16.dp)
                                    .background(SlateText) // slate-600
                            )

                            TextButtonWithIcon(
                                icon = eraserIcon,
                                iconTint = ErrorRed, // red-500
                                text = stringResource(R.string.tool_eraser),
                                onClick = onApplyEraser,
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

/**
 * One tool button in the [GlobalFormattingPanel] row (select / marker / underline / eraser). Shared
 * verbatim by those four buttons, which differ only in icon, accent colour, active state, the small
 * bottom indicator colour, testTag and click action. The clear-all button is intentionally NOT this
 * (it is enabled-gated and not a tool toggle).
 */
@Composable
private fun RowScope.ToolToggleButton(
    testTag: String,
    icon: ImageVector,
    contentDescription: String,
    accent: Color,
    isActive: Boolean,
    bottomIndicatorColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(54.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isActive) 2.dp else 1.dp,
            color = if (isActive) accent else SlateBorder
        ),
        color = if (isActive) accent.copy(alpha = 0.08f) else Color.White
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isActive) accent else SlateText,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(bottomIndicatorColor)
            )
        }
    }
}

@Composable
fun GlobalFormattingPanel(
    editingArticleId: Int?,
    controls: FormattingPanelControls,
    onClearAllEdits: () -> Unit,
    hasAnyEdits: Boolean,
    onDoneEditing: () -> Unit,
    articleTitle: String?
) {
    val activeTool = controls.activeTool
    val onActiveToolChange = controls.onActiveToolChange
    val selectedColorHex = controls.selectedColorHex
    val onColorHexChange = controls.onColorHexChange
    val isPanelExpanded = controls.isPanelExpanded
    val onPanelExpandedChange = controls.onPanelExpandedChange
    val density = androidx.compose.ui.platform.LocalDensity.current
    var lastMarkerColor by remember { mutableStateOf(HighlightPalette.DEFAULT_MARKER) }
    var lastUnderlineColor by remember { mutableStateOf(HighlightPalette.DEFAULT_UNDERLINE) }

    androidx.compose.runtime.LaunchedEffect(activeTool, selectedColorHex) {
        if (activeTool == Constants.TOOL_MARKER) {
            lastMarkerColor = selectedColorHex
        } else if (activeTool == Constants.TOOL_UNDERLINE) {
            lastUnderlineColor = selectedColorHex
        }
    }

    val eraserIcon = remember { createEraserIcon(Color.Black) }

    val markerColors = HighlightPalette.MARKER_COLORS
    val underlineColors = HighlightPalette.UNDERLINE_COLORS

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("global_formatting_panel"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(
            color = SlateBorder,
            thickness = 1.2.dp,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Button 0: SELECT / CURSOR ---
            ToolToggleButton(
                testTag = "tool_button_none",
                icon = Icons.Default.TouchApp,
                contentDescription = stringResource(R.string.tool_selection),
                accent = SovereignBlue,
                isActive = activeTool == Constants.TOOL_NONE,
                bottomIndicatorColor = Color.Transparent,
                onClick = { onActiveToolChange(Constants.TOOL_NONE) }
            )

            // Divider 0-1
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(SlateBorder)
            )

            // --- Button 1: MARKER ---
            ToolToggleButton(
                testTag = "tool_button_marker",
                icon = Icons.Default.Brush,
                contentDescription = stringResource(R.string.tool_marker),
                accent = SovereignBlue,
                isActive = activeTool == Constants.TOOL_MARKER,
                bottomIndicatorColor = safeParseColor(lastMarkerColor, Color.Yellow),
                onClick = {
                    if (activeTool == Constants.TOOL_MARKER) {
                        onActiveToolChange(Constants.TOOL_NONE)
                    } else {
                        onActiveToolChange(Constants.TOOL_MARKER)
                    }
                }
            )

            // Divider 1-2
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(SlateBorder)
            )

            // --- Button 2: UNDERLINE (Line) ---
            ToolToggleButton(
                testTag = "tool_button_underline",
                icon = Icons.Default.FormatUnderlined,
                contentDescription = stringResource(R.string.tool_underline),
                accent = SovereignBlue,
                isActive = activeTool == Constants.TOOL_UNDERLINE,
                bottomIndicatorColor = safeParseColor(lastUnderlineColor, Color.Red),
                onClick = {
                    if (activeTool == Constants.TOOL_UNDERLINE) {
                        onActiveToolChange(Constants.TOOL_NONE)
                    } else {
                        onActiveToolChange(Constants.TOOL_UNDERLINE)
                    }
                }
            )

            // Divider 2-3
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(SlateBorder)
            )

            // --- Button 3: ERASER (Гумка) ---
            ToolToggleButton(
                testTag = "tool_button_eraser",
                icon = eraserIcon,
                contentDescription = stringResource(R.string.tool_eraser),
                accent = ErrorRed,
                isActive = activeTool == Constants.TOOL_ERASER,
                bottomIndicatorColor = Color.Transparent,
                onClick = {
                    if (activeTool == Constants.TOOL_ERASER) {
                        onActiveToolChange(Constants.TOOL_NONE)
                    } else {
                        onActiveToolChange(Constants.TOOL_ERASER)
                    }
                }
            )

            // Divider 3-4
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(SlateBorder)
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
                    color = if (hasAnyEdits) SlateBorder else SlateBorder.copy(alpha = 0.5f)
                ),
                color = if (hasAnyEdits) Color.White else SlateBgLight.copy(alpha = 0.5f),
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
                        tint = if (hasAnyEdits) ErrorRed else SlateMuted,
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
        if (activeTool == Constants.TOOL_MARKER || activeTool == Constants.TOOL_UNDERLINE) {
            val activeColors = if (activeTool == Constants.TOOL_MARKER) markerColors else underlineColors
            val currentColor = if (activeTool == Constants.TOOL_MARKER) lastMarkerColor else lastUnderlineColor

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateBgLight, RoundedCornerShape(12.dp))
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
                                color = if (isSelected) SovereignBlue else SlateDivider,
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
                                tint = if (activeTool == Constants.TOOL_MARKER) SlateDark else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            color = SlateBorder,
            thickness = 1.2.dp,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}
