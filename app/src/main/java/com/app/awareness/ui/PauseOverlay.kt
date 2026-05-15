package com.app.awareness.ui

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.app.awareness.ui.theme.Accent
import com.app.awareness.ui.theme.TextSecondary
import com.app.awareness.ui.theme.TitleStyle
import com.app.awareness.ui.theme.AwarenessTheme
import com.app.awareness.ui.theme.BodyStyle
import com.app.awareness.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages a full-screen pause overlay rendered via [WindowManager] — not Compose Navigation.
 *
 * Call [show] to display the overlay for a given app. The overlay auto-dismisses
 * after [dismissDelayMs] + 200ms fade-out with no user interaction required.
 *
 * Composable — Pause Overlay — FRONTEND.md
 */
class PauseOverlayManager(private val context: Context) {

    private val tag = "PauseOverlayManager"

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: ComposeView? = null

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Show the pause overlay for [appName] with today's usage.
     * Safe to call from any thread — posts to main if needed.
     *
     * @param appName        Display name of the opened app.
     * @param todayMinutes   How many minutes this app has been used today.
     * @param dismissDelayMs How long (ms) to hold before fading out (1000 or 2000).
     */
    fun show(appName: String, todayMinutes: Int, dismissDelayMs: Long = 1_500L) {
        if (overlayView != null) return     // already showing

        val view = buildComposeView(appName, todayMinutes, dismissDelayMs)
        overlayView = view

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            PixelFormat.TRANSLUCENT,
        )

        try {
            windowManager.addView(view, params)
            Log.d(tag, "Overlay shown for $appName")
        } catch (e: Exception) {
            Log.e(tag, "Failed to add overlay view", e)
            overlayView = null
        }
    }

    /** Remove the overlay view from WindowManager. Called by the composable on dismiss. */
    fun dismiss() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
                Log.d(tag, "Overlay dismissed")
            } catch (e: Exception) {
                Log.e(tag, "Failed to remove overlay view", e)
            }
        }
        overlayView = null
    }

    // ── View building ─────────────────────────────────────────────────────────

    private fun buildComposeView(
        appName: String,
        todayMinutes: Int,
        dismissDelayMs: Long,
    ): ComposeView {
        // WindowManager-hosted ComposeView needs its own lifecycle owners
        val lifecycleOwner = OverlayLifecycleOwner().also { it.start() }

        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                AwarenessTheme {
                    PauseOverlayContent(
                        appName        = appName,
                        todayMinutes   = todayMinutes,
                        dismissDelayMs = dismissDelayMs,
                        onDismiss      = { dismiss() },
                    )
                }
            }
        }
    }
}

// ── Overlay composable ────────────────────────────────────────────────────────

/**
 * Full-screen black overlay with an expanding circle animation.
 *
 * Animation sequence (FRONTEND.md spec):
 *  1. Circle expands 60dp → 120dp radius over 1 000ms
 *  2. Text fades in simultaneously over 1 000ms
 *  3. Hold for [dismissDelayMs]
 *  4. Entire overlay fades out over 200ms → [onDismiss]
 */
@Composable
private fun PauseOverlayContent(
    appName: String,
    todayMinutes: Int,
    dismissDelayMs: Long,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current

    // Animation state
    val startRadiusPx = with(density) { 60.dp.toPx() }
    val endRadiusPx   = with(density) { 120.dp.toPx() }
    val circleRadius  = remember { Animatable(startRadiusPx) }
    val textAlpha     = remember { Animatable(0f) }
    val overlayAlpha  = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Phase 1: expand circle + fade in text (1 000ms)
        launch { circleRadius.animateTo(endRadiusPx, tween(1_000)) }
        launch { textAlpha.animateTo(1f, tween(1_000)) }

        // Phase 2: hold for configured delay
        delay(dismissDelayMs)

        // Phase 3: fade out entire overlay (200ms)
        overlayAlpha.animateTo(0f, tween(200))
        onDismiss()
    }

    Box(
        modifier          = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha.value }
            .background(Color(0xFF000000).copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Expanding circle (Canvas)
            Canvas(modifier = Modifier.size(260.dp)) {
                // Outer glow ring
                drawCircle(
                    color  = Accent.copy(alpha = 0.08f),
                    radius = circleRadius.value * 1.5f,
                )
                // Mid ring
                drawCircle(
                    color  = Accent.copy(alpha = 0.18f),
                    radius = circleRadius.value * 1.15f,
                )
                // Core circle
                drawCircle(
                    color  = Accent.copy(alpha = 0.30f),
                    radius = circleRadius.value,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App name
            Text(
                text     = appName,
                style    = TitleStyle,
                color    = TextPrimary.copy(alpha = textAlpha.value),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Today's usage
            Text(
                text     = "${todayMinutes}m today",
                style    = BodyStyle,
                color    = TextSecondary.copy(alpha = textAlpha.value),
            )
        }
    }
}

// ── Lifecycle shim for WindowManager-hosted ComposeView ───────────────────────

/**
 * Minimal [LifecycleOwner] / [ViewModelStoreOwner] / [SavedStateRegistryOwner]
 * required by ComposeView when hosted outside a Fragment or Activity.
 */
private class OverlayLifecycleOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val registry   = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this).also { it.performAttach() }

    override val lifecycle: Lifecycle             get() = registry
    override val viewModelStore: ViewModelStore   = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

    fun start() {
        controller.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    fun stop() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }
}
