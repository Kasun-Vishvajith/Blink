package com.app.awareness.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.awareness.ui.theme.Accent
import com.app.awareness.ui.theme.BodyStyle
import com.app.awareness.ui.theme.LabelStyle
import com.app.awareness.ui.theme.Positive
import com.app.awareness.ui.theme.SurfaceVariant
import com.app.awareness.ui.theme.TextMuted
import com.app.awareness.ui.theme.TextPrimary
import com.app.awareness.ui.theme.TextSecondary
import com.app.awareness.ui.theme.Warning

/**
 * UI model passed to [InsightCard].
 * Maps from [com.app.awareness.data.Insight] in the ViewModel layer.
 */
data class InsightCardData(
    /** Plain English insight sentence shown to the user. */
    val message: String,
    /** "positive" | "neutral" | "warning" */
    val sentiment: String,
    /** Which metric this insight is about — drives the icon. */
    val metric: String,
)

/**
 * Single insight card — 280 × 120 dp card with a left sentiment bar.
 *
 * Layout (FRONTEND.md — Composable InsightCard):
 * ┌──┬────────────────────────────────┐
 * │  │ METRIC LABEL (uppercase)       │
 * │  │ Insight message body text      │
 * │  │                           icon │
 * └──┴────────────────────────────────┘
 * 4dp color bar | 16dp padding content area
 *
 * Sentiment → bar color:
 *   positive → Positive (#00E5A0)
 *   warning  → Warning  (#FF6B35)
 *   neutral  → TextMuted (#444444)
 */
@Composable
fun InsightCard(
    data: InsightCardData,
    modifier: Modifier = Modifier,
) {
    val barColor = when (data.sentiment) {
        "positive" -> Positive
        "warning"  -> Warning
        else       -> TextMuted
    }

    Box(
        modifier = modifier
            .width(280.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceVariant),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {

            // ── Left sentinel bar ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(barColor),
            )

            // ── Content area ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // Top: uppercase metric label
                    Text(
                        text  = data.metric.replace("_", " ").uppercase(),
                        style = LabelStyle,
                        color = TextSecondary,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Middle: insight message
                    Text(
                        text     = data.message,
                        style    = BodyStyle,
                        color    = TextPrimary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Bottom-right: subtle metric icon
                Icon(
                    imageVector      = metricIcon(data.metric),
                    contentDescription = null,
                    tint             = barColor.copy(alpha = 0.35f),
                    modifier         = Modifier
                        .size(18.dp)
                        .align(Alignment.BottomEnd),
                )
            }
        }
    }
}

// ── Helper ────────────────────────────────────────────────────────────────────

private fun metricIcon(metric: String): ImageVector = when (metric) {
    "instagram_opens"   -> TrendingUp
    "screen_time"       -> Icons.Outlined.Timer
    "spotify_minutes"   -> Icons.Outlined.MusicNote
    "notifications"     -> Icons.Outlined.Notifications
    "call_minutes"      -> Icons.Outlined.PhoneInTalk
    "best_day"          -> Icons.Outlined.BarChart
    else                -> Icons.Outlined.TrendingUp
}

private val TrendingUp   get() = Icons.Outlined.TrendingUp
