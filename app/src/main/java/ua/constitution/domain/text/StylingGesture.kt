package ua.constitution.domain.text

/** Which way a styling-tool drag resolved once it passed the touch slop. */
enum class GestureAxis { SCROLL, STYLE }

/**
 * Decides, for a highlight/underline tool drag, whether the gesture is a vertical SCROLL or a
 * horizontal STYLE swipe — or null while the finger has not yet moved past [touchSlop]. Extracted
 * verbatim from the inline decision in SegmentedTextWithEdits' pointer loop: a move is committed only
 * once `dx² + dy² >= slop²`, and a vertical-dominant move (`|dy| > |dx|`) is a scroll, otherwise a
 * styling swipe. Pure, so the threshold/axis logic is unit-testable away from the pointer plumbing.
 */
fun resolveGestureAxis(diffX: Float, diffY: Float, touchSlop: Float): GestureAxis? {
    val distSq = diffX * diffX + diffY * diffY
    if (distSq < touchSlop * touchSlop) return null
    return if (kotlin.math.abs(diffY) > kotlin.math.abs(diffX)) GestureAxis.SCROLL else GestureAxis.STYLE
}
