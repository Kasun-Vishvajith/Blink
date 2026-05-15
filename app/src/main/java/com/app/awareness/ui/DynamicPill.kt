package com.app.awareness.ui

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.app.awareness.ui.theme.BlinkTheme
import com.app.awareness.ui.theme.BodyStyle
import com.app.awareness.ui.theme.Positive
import com.app.awareness.ui.theme.SurfaceVariant
import com.app.awareness.ui.theme.TextPrimary
import com.app.awareness.ui.theme.Warning
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages a top-center Dynamic Pill overlay rendered via [WindowManager].
 *
 * Spec — Composable Dynamic Pill — FRONTEND.md:
 *   - Height 36dp, width wrap_content (max 280dp)
 *   - Background: SurfaceVariant (#1A1A1A), border: 0.5dp #333333
 *   - Left colored dot (green=positive, orange=warning)
 *   - Text: 13sp, single line
 *   - Enter: scale 0.3→1.0 + fade in, 250ms spring
 *   - Hold 3 seconds, then exit: scale 1.0→0.3 + fade out, 200ms
 */
class DynamicPillManager(private val context: Context) {

    private val tag = "DynamicPillManager"

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var pillView: ComposeView? = null

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Show the dynamic pill with [message] and [sentiment].
     * If a pill is already showing it is replaced immediately.
     *
     * @param message   Single-line text shown in the pill.
     * @param sentiment "positive" → green dot, "warning" → orange dot, else neutral.
     */
    fun show(message: String, sentiment: String) {
        dismissNow()    // replace any existing pill

        val view = buildComposeView(message, sentiment)
        pillView = view

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 52          // pixels below status bar
        }

        try {
            windowManager.addView(view, params)
            Log.d(tag, "Pill shown: $message")
        } catch (e: Exception) {
            Log.e(tag, "Failed to add pill view", e)
            pillView = null
        }
    }

    fun dismissNow() {
        pillView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        pillView = null
    }

    // ── View building ─────────────────────────────────────────────────────────

    private fun buildComposeView(message: String, sentiment: String): ComposeView {
        val lifecycleOwner = PillLifecycleOwner().also { it.start() }

        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                BlinkTheme {
                    DynamicPillContent(
                        message   = message,
                        sentiment = sentiment,
                        onDismiss = { dismissNow() },
                    )
                }
            }
        }
    }
}

// ── Pill composable ───────────────────────────────────────────────────────────

/**
 * @param message   Text to display in the pill.
 * @param sentiment "positive" | "warning" | anything else → neutral dot.
 * @param onDismiss Called after exit animation completes.
 */
@Composable
fun DynamicPillContent(
    message: String,
    sentiment: String,
    onDismiss: () -> Unit = {},
) {
    val dotColor = when (sentiment) {
        "positive" -> Positive
        "warning"  -> Warning
        else       -> Color(0xFF888888)
    }

    // Animation state
    val scale = remember { Animatable(0.3f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // ── Enter: spring scale + fade (250ms) ────────────────────────────────
        launch {
            scale.animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMedium,
                ),
            )
        }
        launch { alpha.animateTo(1f, tween(250)) }

        // ── Hold 3 seconds ────────────────────────────────────────────────────
        delay(3_000L)

        // ── Exit: scale + fade out (200ms) ────────────────────────────────────
        launch { scale.animateTo(0.3f, tween(200)) }
        alpha.animateTo(0f, tween(200))

        onDismiss()
    }

    Row(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .height(36.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            }
            .clip(RoundedCornerShape(100.dp))
            .background(SurfaceVariant)
            .border(0.5.dp, Color(0xFF333333), RoundedCornerShape(100.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Sentiment dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Message text
        Text(
            text     = message,
            style    = BodyStyle.copy(fontSize = 13.sp),
            color    = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Lifecycle shim ────────────────────────────────────────────────────────────

private class PillLifecycleOwner :
    LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val registry   = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this).also { it.performAttach() }

    override val lifecycle: Lifecycle           get() = registry
    override val viewModelStore: ViewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

    fun start() {
        controller.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }
}
