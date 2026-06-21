package ua.constitution.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import ua.constitution.R

/**
 * Copies [text] to the clipboard under [label] and shows the standard "copied" / "copy error" toast.
 * Centralizes the identical clipboard+toast block that lived in both ArticleCard's copy button and
 * the reader's selection-toolbar copy action (they differed only in the clip label).
 */
fun copyToClipboardWithToast(context: Context, label: String, text: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, context.getString(R.string.toast_article_copied), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.toast_copy_error, e.message ?: ""), Toast.LENGTH_SHORT).show()
    }
}
