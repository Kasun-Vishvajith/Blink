package com.app.awareness.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.awareness.ui.theme.Accent
import com.app.awareness.ui.theme.Background
import com.app.awareness.ui.theme.BodyStyle
import com.app.awareness.ui.theme.CaptionStyle
import com.app.awareness.ui.theme.DisplayStyle
import com.app.awareness.ui.theme.DmSansFontFamily
import com.app.awareness.ui.theme.LabelStyle
import com.app.awareness.ui.theme.Positive
import com.app.awareness.ui.theme.Surface
import com.app.awareness.ui.theme.SurfaceVariant
import com.app.awareness.ui.theme.TextMuted
import com.app.awareness.ui.theme.TextPrimary
import com.app.awareness.ui.theme.TextSecondary
import com.app.awareness.ui.theme.TitleStyle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.WindowInsets
import androidx.compose.foundation.layout.statusBars

// ── Mock data models (private to this file) ───────────────────────────────────

private data class AppRowData(
    val name: String,
    val minutes: Int,
    val dotColor: Color,
)

private data class BenchmarkItem(
    val label: String,
    val userValue: String,
    val worldValue: String,
    val isBetter: Boolean,
)

// ── Mock data ─────────────────────────────────────────────────────────────────

private val mockInsights = listOf(
    InsightCardData(
        message   = "You open Instagram 5 times a day. Most people open it 23 times. You're remarkably intentional.",
        sentiment = "positive",
        metric    = "instagram_opens",
    ),
    InsightCardData(
        message   = "Today was unusual — your screen time was 52% higher than your weekly average.",
        sentiment = "warning",
        metric    = "screen_time",
    ),
    InsightCardData(
        message   = "You spent 18 minutes on Spotify today. The average is 82 minutes.",
        sentiment = "positive",
        metric    = "spotify_minutes",
    ),
    InsightCardData(
        message   = "Your Mondays are consistently your highest screen time day.",
        sentiment = "neutral",
        metric    = "screen_time",
    ),
)

private val mockApps = listOf(
    AppRowData("Instagram", 87,  Color(0xFFE1306C)),
    AppRowData("YouTube",   65,  Color(0xFFFF0000)),
    AppRowData("WhatsApp",  43,  Color(0xFF25D366)),
    AppRowData("Spotify",   38,  Color(0xFF1DB954)),
    AppRowData("Chrome",    22,  Color(0xFF4285F4)),
)

private val mockBenchmarks = listOf(
    BenchmarkItem("Screen time", "3h 22m", "6h 37m", isBetter = true),
    BenchmarkItem("App opens",   "47",     "72",     isBetter = true),
    BenchmarkItem("Notifications","43",    "96",     isBetter = true),
)

// ── HomeScreen ────────────────────────────────────────────────────────────────

/**
 * Screen 2 — Home (Daily Summary) — FRONTEND.md
 *
 * Single vertical scroll. Five sections:
 *   A – Header (time, date, greeting)
 *   B – Today's Big Number (total screen time)
 *   C – Insight Cards (horizontal scroll)
 *   D – App Breakdown (top 5 apps)
 *   E – Benchmark Row ("You vs the world")
 *
 * All data is hardcoded mock — connect a ViewModel in the next pass.
 */
@Composable
fun HomeScreen() {
    val scrollState = rememberScrollState()

    // Derive time-based values once per composition
    val calendar = remember { Calendar.getInstance() }
    val timeText = remember {
        SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())
    }
    val amPm = remember {
        SimpleDateFormat("a", Locale.getDefault()).format(Date())
    }
    val dateText = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }
    val hour = remember { calendar.get(Calendar.HOUR_OF_DAY) }
    val greeting = when {
        hour < 6  -> "Still up?"
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        hour < 21 -> "Good evening"
        else      -> "Still up?"
    }

    // Mock totals
    val totalMinutes   = 202
    val comparisonText = "37 min less than yesterday"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(scrollState),
    ) {

        // ── Section A — Header ────────────────────────────────────────────────
        SectionHeader(timeText, amPm, dateText, greeting)

        Spacer(modifier = Modifier.height(32.dp))

        // ── Section B — Today's Big Number ────────────────────────────────────
        BigNumberSection(totalMinutes, comparisonText)

        Spacer(modifier = Modifier.height(36.dp))

        // ── Section C — Insight Cards ─────────────────────────────────────────
        InsightCardsSection(mockInsights)

        Spacer(modifier = Modifier.height(36.dp))

        // ── Section D — App Breakdown ─────────────────────────────────────────
        AppBreakdownSection(mockApps)

        Spacer(modifier = Modifier.height(36.dp))

        // ── Section E — Benchmark Row ─────────────────────────────────────────
        BenchmarkSection(mockBenchmarks)

        Spacer(modifier = Modifier.height(48.dp))
    }
}

// ── Section A — Header ────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    timeText: String,
    amPm: String,
    dateText: String,
    greeting: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp),
    ) {
        // Large time display
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text  = timeText,
                style = DisplayStyle,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text     = amPm,
                style    = TitleStyle,
                color    = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Date
        Text(
            text  = dateText,
            style = BodyStyle,
            color = TextSecondary,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Greeting
        Text(
            text  = greeting,
            style = CaptionStyle,
            color = TextMuted,
        )
    }
}

// ── Section B — Big Number ────────────────────────────────────────────────────

@Composable
private fun BigNumberSection(totalMinutes: Int, comparisonText: String) {
    val hours   = totalMinutes / 60
    val minutes = totalMinutes % 60

    Column(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        // Giant screen time number
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text  = "$hours",
                style = DisplayStyle.copy(fontSize = 80.sp, fontWeight = FontWeight.W200),
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text     = "h",
                style    = TitleStyle,
                color    = TextSecondary,
                modifier = Modifier.padding(bottom = 14.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text  = "$minutes",
                style = DisplayStyle.copy(fontSize = 80.sp, fontWeight = FontWeight.W200),
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text     = "m",
                style    = TitleStyle,
                color    = TextSecondary,
                modifier = Modifier.padding(bottom = 14.dp),
            )
        }

        // Comparison line in Accent
        Text(
            text  = comparisonText,
            style = BodyStyle,
            color = Accent,
        )
    }
}

// ── Section C — Insight Cards ─────────────────────────────────────────────────

@Composable
private fun InsightCardsSection(insights: List<InsightCardData>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text     = "Insights",
            style    = LabelStyle,
            color    = TextSecondary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding    = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(insights.take(5)) { insight ->
                InsightCard(data = insight)
            }
        }
    }
}

// ── Section D — App Breakdown ─────────────────────────────────────────────────

@Composable
private fun AppBreakdownSection(apps: List<AppRowData>) {
    val maxMinutes = apps.maxOfOrNull { it.minutes }?.toFloat() ?: 1f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        Text(
            text  = "Where your time went",
            style = TitleStyle,
            color = TextPrimary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        apps.forEach { app ->
            AppRow(app = app, maxMinutes = maxMinutes)
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun AppRow(app: AppRowData, maxMinutes: Float) {
    val barFraction = (app.minutes / maxMinutes).coerceIn(0f, 1f)

    Row(
        modifier        = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // App colour dot with initial
        Box(
            modifier          = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(app.dotColor.copy(alpha = 0.2f))
                .border(1.dp, app.dotColor.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = app.name.first().toString(),
                style = LabelStyle,
                color = app.dotColor,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // App name
            Text(
                text  = app.name,
                style = BodyStyle,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Proportional bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(SurfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(barFraction)
                        .height(4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Accent),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Time label
        Text(
            text      = formatMinutes(app.minutes),
            style     = CaptionStyle,
            color     = TextSecondary,
            textAlign = TextAlign.End,
        )
    }
}

// ── Section E — Benchmark Row ─────────────────────────────────────────────────

@Composable
private fun BenchmarkSection(benchmarks: List<BenchmarkItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        Text(
            text  = "You vs the world",
            style = TitleStyle,
            color = TextPrimary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            benchmarks.forEach { item ->
                BenchmarkPill(item = item, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BenchmarkPill(item: BenchmarkItem, modifier: Modifier = Modifier) {
    val valueColor = if (item.isBetter) Positive else TextSecondary

    Column(
        modifier          = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text      = item.label,
            style     = LabelStyle,
            color     = TextMuted,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text      = item.userValue,
            style     = TitleStyle.copy(fontSize = 18.sp),
            color     = valueColor,
            textAlign = TextAlign.Center,
        )

        Text(
            text      = "avg ${item.worldValue}",
            style     = CaptionStyle,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Utilities ─────────────────────────────────────────────────────────────────

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0          -> "${h}h"
        else           -> "${m}m"
    }
}
