package com.app.awareness.ui

import androidx.compose.foundation.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.app.awareness.ui.theme.LabelStyle
import com.app.awareness.ui.theme.Positive
import com.app.awareness.ui.theme.Surface
import com.app.awareness.ui.theme.SurfaceVariant
import com.app.awareness.ui.theme.TextMuted
import com.app.awareness.ui.theme.TextPrimary
import com.app.awareness.ui.theme.TextSecondary
import com.app.awareness.ui.theme.TitleStyle
import com.app.awareness.ui.theme.Warning
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ── Mock data ─────────────────────────────────────────────────────────────────

private data class ComparisonLine(
    val label: String,
    val userValue: String,
    val worldAvg: String,
    val isBetter: Boolean,
)

private val mockComparisons = listOf(
    ComparisonLine("Weekly screen time", "23h 15m", "46h 23m", isBetter = true),
    ComparisonLine("Daily app opens",    "47",      "72",      isBetter = true),
    ComparisonLine("Notifications",      "302",     "672",     isBetter = true),
)

// ── WeeklyScreen ──────────────────────────────────────────────────────────────

/**
 * Screen 3 — Weekly Wrapped — FRONTEND.md
 *
 * Vertical scroll of 7 full-width story cards (200dp × full-width, radius 24dp).
 * All data is hardcoded mock — connect WeeklyViewModel in the next pass.
 *
 * Card order:
 *   1 – Opening ("This week in numbers")
 *   2 – Screen Time (big number)
 *   3 – Top App
 *   4 – Best Day
 *   5 – Calls
 *   6 – Music (Lottie placeholder)
 *   7 – Closing Benchmark
 */
@Composable
fun WeeklyScreen() {
    val scrollState = rememberScrollState()

    // Derive week date range from today
    val cal   = remember { Calendar.getInstance() }
    val fmt   = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val endDateStr   = remember { fmt.format(cal.time) }
    val startDateStr = remember {
        val c = cal.clone() as Calendar
        c.add(Calendar.DAY_OF_YEAR, -6)
        fmt.format(c.time)
    }
    val weekRange = "$startDateStr – $endDateStr"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Page title
        Text(
            text     = "Weekly Wrapped",
            style    = TitleStyle,
            color    = TextSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )

        Card1Opening(weekRange)
        Card2ScreenTime(totalHours = 23, totalMins = 15, deltaHours = 2, deltaIsWorse = true)
        Card3TopApp(appName = "Instagram", openCount = 143, totalHours = 9)
        Card4BestDay(day = "Saturday", hours = 1, minutes = 23)
        Card5Calls(totalMinutes = 47, apps = listOf("WhatsApp", "Telegram"))
        Card6Music(appName = "Spotify", hours = 9, backgroundPercent = 72)
        Card7Benchmark(comparisons = mockComparisons, sentiment = "You're using your phone more mindfully than most.")
    }
}

// ── Card 1 — Opening ──────────────────────────────────────────────────────────

@Composable
private fun Card1Opening(weekRange: String) {
    StoryCard(
        modifier = Modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF0A0A0A), Color(0xFF1A1F00)),
            ),
            shape = RoundedCornerShape(24.dp),
        )
    ) {
        Column(
            modifier          = Modifier.fillMaxSize(),
            verticalArrangement   = Arrangement.Center,
        ) {
            Text(
                text  = "This week",
                style = DisplayStyle.copy(fontSize = 40.sp),
                color = TextPrimary,
            )
            Text(
                text  = "in numbers",
                style = DisplayStyle.copy(fontSize = 40.sp, fontWeight = FontWeight.W200),
                color = Accent,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text  = weekRange,
                style = BodyStyle,
                color = TextSecondary,
            )
        }

        // Corner label
        Text(
            text     = "BLINK",
            style    = LabelStyle,
            color    = TextMuted,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

// ── Card 2 — Screen Time ──────────────────────────────────────────────────────

@Composable
private fun Card2ScreenTime(
    totalHours: Int,
    totalMins: Int,
    deltaHours: Int,
    deltaIsWorse: Boolean,
) {
    val deltaColor = if (deltaIsWorse) Warning else Positive
    val deltaSign  = if (deltaIsWorse) "+" else "−"
    val deltaText  = "${deltaSign}${deltaHours}h vs last week"

    StoryCard {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                    text  = "SCREEN TIME",
                    style = LabelStyle,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Big number
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text  = "$totalHours",
                        style = DisplayStyle.copy(fontSize = 72.sp, fontWeight = FontWeight.W200),
                        color = TextPrimary,
                    )
                    Text(
                        text     = "h ${totalMins}m",
                        style    = TitleStyle,
                        color    = TextSecondary,
                        modifier = Modifier.padding(bottom = 10.dp, start = 6.dp),
                    )
                }

                Text(
                    text  = "You spent $totalHours hours on your phone this week",
                    style = BodyStyle,
                    color = TextPrimary,
                )
            }

            Text(
                text  = deltaText,
                style = BodyStyle.copy(fontWeight = FontWeight.SemiBold),
                color = deltaColor,
            )
        }
    }
}

// ── Card 3 — Top App ─────────────────────────────────────────────────────────

@Composable
private fun Card3TopApp(appName: String, openCount: Int, totalHours: Int) {
    StoryCard {
        Row(
            modifier        = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Large app placeholder circle
            Box(
                modifier          = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E0814)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = appName.first().toString(),
                    style = DisplayStyle.copy(fontSize = 36.sp),
                    color = Color(0xFFE1306C),
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "TOP APP",
                    style = LabelStyle,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text  = "$appName was your most opened app",
                    style = TitleStyle,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text  = "$openCount times  ·  ${totalHours}h total",
                    style = BodyStyle,
                    color = TextSecondary,
                )
            }
        }
    }
}

// ── Card 4 — Best Day ─────────────────────────────────────────────────────────

@Composable
private fun Card4BestDay(day: String, hours: Int, minutes: Int) {
    StoryCard {
        Column(
            modifier          = Modifier.fillMaxSize(),
            verticalArrangement   = Arrangement.Center,
        ) {
            Text(
                text  = "BEST DAY",
                style = LabelStyle,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Large day name
            Text(
                text  = day,
                style = DisplayStyle.copy(fontSize = 52.sp, fontWeight = FontWeight.W200),
                color = Positive,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text  = "$day was your most phone-free day",
                style = BodyStyle,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = "Only ${hours}h ${minutes}m — your personal best this week",
                style = CaptionStyle,
            )
        }
    }
}

// ── Card 5 — Calls ────────────────────────────────────────────────────────────

@Composable
private fun Card5Calls(totalMinutes: Int, apps: List<String>) {
    StoryCard {
        Row(
            modifier        = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Phone icon
            Box(
                modifier          = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector      = Icons.Outlined.Phone,
                    contentDescription = null,
                    tint             = Accent,
                    modifier         = Modifier.size(32.dp),
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "CALLS",
                    style = LabelStyle,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text  = "You spent ~$totalMinutes minutes on calls",
                    style = TitleStyle,
                    color = TextPrimary,
                )
                if (apps.size > 1) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text  = apps.joinToString(" + "),
                        style = CaptionStyle,
                    )
                }
            }
        }
    }
}

// ── Card 6 — Music ────────────────────────────────────────────────────────────

@Composable
private fun Card6Music(appName: String, hours: Int, backgroundPercent: Int) {
    val showBackgroundLine = backgroundPercent > 60

    StoryCard {
        Row(
            modifier        = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Music icon (TODO: replace with Lottie waveform animation)
            Box(
                modifier          = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0D1A0D)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector      = Icons.Outlined.MusicNote,
                    contentDescription = null,
                    tint             = Positive,
                    modifier         = Modifier.size(32.dp),
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "MUSIC",
                    style = LabelStyle,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text  = "$appName played for $hours hours",
                    style = TitleStyle,
                    color = TextPrimary,
                )
                if (showBackgroundLine) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text  = "Mostly while you were doing other things",
                        style = CaptionStyle,
                    )
                }
            }
        }
    }
}

// ── Card 7 — Closing Benchmark ────────────────────────────────────────────────

@Composable
private fun Card7Benchmark(
    comparisons: List<ComparisonLine>,
    sentiment: String,
) {
    StoryCard(
        modifier = Modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF0A0A0A), Color(0xFF001A14)),
            ),
            shape = RoundedCornerShape(24.dp),
        )
    ) {
        Column(
            modifier              = Modifier.fillMaxSize(),
            verticalArrangement   = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text  = "Compared to the world...",
                    style = TitleStyle,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(16.dp))

                comparisons.forEach { line ->
                    ComparisonRow(line)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Sentiment footer
            Text(
                text  = sentiment,
                style = BodyStyle.copy(fontWeight = FontWeight.SemiBold),
                color = Positive,
            )
        }
    }
}

@Composable
private fun ComparisonRow(line: ComparisonLine) {
    val valueColor = if (line.isBetter) Positive else Warning
    val indicator  = if (line.isBetter) "↓" else "↑"

    Row(
        modifier        = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text  = line.label,
            style = CaptionStyle,
            color = TextSecondary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text  = line.userValue,
                style = BodyStyle.copy(fontWeight = FontWeight.SemiBold),
                color = valueColor,
            )
            Text(
                text  = "  $indicator  avg ${line.worldAvg}",
                style = CaptionStyle,
                color = TextMuted,
            )
        }
    }
}

// ── Shared card wrapper ───────────────────────────────────────────────────────

/**
 * Base story card shell — full width, 200dp height, 24dp radius, SurfaceVariant bg.
 * Override [modifier] with a custom `.background()` for gradient cards (Card 1 & 7).
 */
@Composable
private fun StoryCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceVariant)
            .then(modifier)
            .padding(24.dp),
        content = content,
    )
}
