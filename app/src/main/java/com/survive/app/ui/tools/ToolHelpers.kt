package com.survive.app.ui.tools

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/** Garde l'écran allumé tant que l'outil est affiché. */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

/** Force la luminosité maximale (écran utilisé comme lampe) tant que [enabled]. */
@Composable
fun MaxBrightness(enabled: Boolean) {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(enabled) {
        val window = activity.window
        val previous = window.attributes.screenBrightness
        if (enabled) {
            window.attributes = window.attributes.apply { screenBrightness = 1f }
        }
        onDispose {
            window.attributes = window.attributes.apply {
                screenBrightness = if (enabled) WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE else previous
            }
        }
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
