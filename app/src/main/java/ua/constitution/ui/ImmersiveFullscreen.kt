package ua.constitution.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** The hosting [Activity] for this context, found by walking the [ContextWrapper] chain, or null. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Drives immersive (system-bars-hidden) mode for the hosting window while [active], restoring the
 * bars otherwise. Extracted out of MainAppDashboard so window-chrome policy is no longer a
 * responsibility of the navigation shell. Driven purely via [WindowInsetsControllerCompat] (the
 * modern, cross-API hide/show; the deprecated FLAG_FULLSCREEN + statusBarColor it used to also set
 * were redundant with the controller and with the activity's edge-to-edge setup). Keyed on [active]
 * (the window toggle is idempotent across non-NONE fullscreen symbols).
 */
@Composable
fun ImmersiveFullscreenEffect(active: Boolean) {
    val context = LocalContext.current
    LaunchedEffect(active) {
        val win = context.findActivity()?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(win, win.decorView)
        if (active) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.isAppearanceLightStatusBars = true
        }
    }
}
