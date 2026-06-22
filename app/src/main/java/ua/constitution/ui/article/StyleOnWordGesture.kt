package ua.constitution.ui.article

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import ua.constitution.domain.text.GestureAxis
import ua.constitution.domain.text.getWordSnappedRange
import ua.constitution.domain.text.resolveGestureAxis

/**
 * Captures swipe-over-words styling gestures on the reader's edit overlay. Restarts when [keys]
 * changes (compared structurally). Per gesture it samples the active [activeTool] once, then on each
 * pointer move decides via [resolveGestureAxis] whether the user is scrolling (let the gesture pass)
 * or styling (consume the change and apply the tool to every word the finger touches); a release
 * before any decision is a single-word tap. [getLayout]/[selectedColorHex]/[applyToRanges] are read
 * live so the latest layout, colour and apply-function are used mid-gesture.
 *
 * Behaviour-preserving extraction of the inline pointerInput loop from `SegmentedTextWithEdits`
 * (its body is byte-identical, only the captured names are now parameters). The gesture path is not
 * Robolectric-triggerable, so this is verified by manual smoke test; the pure scroll-vs-style
 * decision it delegates to is unit-tested in `StylingGestureTest` (resolveGestureAxis). Its inherent
 * state-machine complexity is the same that was accepted inside SegmentedTextWithEdits and is
 * carried in the detekt baseline (relocated, not new).
 */
fun Modifier.styleOnWordGesture(
    keys: List<Any?>,
    getLayout: () -> TextLayoutResult?,
    formToOrigMapping: IntArray,
    originalText: String,
    activeTool: () -> String,
    selectedColorHex: () -> String,
    applyToRanges: (Collection<Pair<Int, Int>>, String, String) -> Unit,
): Modifier = pointerInput(keys) {
    val touchSlop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val tool = activeTool()
        val touchedWords = mutableSetOf<Pair<Int, Int>>()

        val startPosition = down.position
        val currentPointerId = down.id
        var hasDecidedGesture = false
        var isScrollingMode = false
        var isStylingMode = false

        // Applies the active tool to the word under [position] (once per
        // word in this gesture). Shared by the tap, the styling-down and
        // the drag paths, which were three verbatim copies of this block.
        val applyStyleAt: (Offset) -> Unit = { position ->
            getLayout()?.let { layoutResult ->
                val dragPos = layoutResult.getOffsetForPosition(position)
                val origDragPos = formToOrigMapping.getOrElse(dragPos) { dragPos }
                if (origDragPos in 0..originalText.length) {
                    getWordSnappedRange(originalText, origDragPos, origDragPos)?.let { snapped ->
                        if (touchedWords.add(snapped)) {
                            applyToRanges(touchedWords, tool, selectedColorHex())
                        }
                    }
                }
            }
        }

        while (true) {
            val event = awaitPointerEvent()
            val anyActive = event.changes.any { it.id == currentPointerId && it.pressed }
            if (!anyActive) {
                // User released finger
                if (!hasDecidedGesture) {
                    // Treated as a single tap!
                    applyStyleAt(startPosition)
                }
                break
            }

            val activeChange = event.changes.firstOrNull { it.id == currentPointerId }
            if (activeChange != null) {
                val currentPosition = activeChange.position
                val diffX = currentPosition.x - startPosition.x
                val diffY = currentPosition.y - startPosition.y

                if (!hasDecidedGesture) {
                    val axis = resolveGestureAxis(diffX, diffY, touchSlop)
                    if (axis != null) {
                        hasDecidedGesture = true
                        if (axis == GestureAxis.SCROLL) {
                            isScrollingMode = true
                            break
                        } else {
                            isStylingMode = true
                            activeChange.consume()
                            // Also apply styling to down position now that we know we are styling
                            applyStyleAt(startPosition)
                        }
                    }
                } else {
                    if (isStylingMode) {
                        activeChange.consume()
                        applyStyleAt(currentPosition)
                    }
                }
            }
        }
    }
}
