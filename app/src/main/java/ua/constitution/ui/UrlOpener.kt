package ua.constitution.ui

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens [url] in an external viewer (browser), swallowing the failure when no activity can handle it.
 *
 * Centralizes the formerly-inlined `try { startActivity(Intent(ACTION_VIEW, Uri.parse(url))) } catch {}`
 * blocks that were copy-pasted across the dashboard header, the chapter/article source buttons, so the
 * Intent construction and the (intentionally silent) error handling live in exactly one place. The
 * reader link-tap handlers in InteractiveText are deliberately NOT routed here: they log on failure
 * via their own surrounding catch, which this helper would suppress.
 */
fun openExternalUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        // No activity able to handle the link — match the prior inline empty-catch behavior.
    }
}
