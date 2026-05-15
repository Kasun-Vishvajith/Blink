package com.app.awareness.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomCartesianAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartCartesianAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.app.awareness.ui.theme.Accent
import com.app.awareness.ui.theme.Background
import com.app.awareness.ui.theme.BodyStyle
import com.app.awareness.ui.theme.CaptionStyle
import com.app.awareness.ui.theme.LabelStyle
import com.app.awareness.ui.theme.Positive
import com.app.awareness.ui.theme.Surface
import com.app.awareness.ui.theme.SurfaceVariant
import com.app.awareness.ui.theme.TextMuted
import com.app.awareness.ui.theme.TextPrimary
import com.app.awareness.ui.theme.TextSecondary
import com.app.awareness.ui.theme.TitleStyle
import com.app.awareness.ui.theme.Warning

// ── Mock data ─────────────────────────────────────────────────────────────────

private val mockChartData = mapOf(
    0 to listOf(12f, 8f, 20f, 5f, 14f, 10f, 18f),   // Today (hourly, 7 slots)
    1 to listOf(87f, 45f, 62f, 30f, 55f, 20f, 73f),   // This Week (daily)
    2 to listOf(320f, 280f, 410f, 250f, 380f, 300f, 430f), // This Month (weekly avg)
)

private val xLabels = mapOf(
    0 to listOf("10a", "11a", "12p", "1p", "2p", "3p", "4p"),
    1 to listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    2 to listOf("W1", "W2", "W3", "W4", "W5", "W6", "W7"),
)

// ── AppDetailSheet ────────────────────────────────────────────────────────────

/**
 * Screen 4 — App Detail Sheet — FRONTEND.md
 *
 * Modal bottom sheet triggered by tapping an app row on HomeScreen.
 * All data is hardcoded mock — connect AppDetailViewModel in the next pass.
 *
 * Sections:
 *   • App icon + name header
 *   • Today / This Week / This Month toggle (SegmentedButton)
 *   • Vico bar chart (daily usage for selected period)
 *   • Open count + avg session length stats
 *   • Benchmark comparison row
 *   • Time limit slider (0 = no limit, 1–180 min = soft overlay)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailSheet(
    appName: String  = "Instagram",
    appColor: Color  = Color(0xFFE1306C),
    onDismiss: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest   = onDismiss,
        sheetState         = sheetState,
        containerColor     = Surface,
        contentColor       = TextPrimary,
        dragHandle         = {
            // Custom drag pill
            Spacer(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(TextMuted),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            AppHeader(appName = appName, appColor = appColor)

            Spacer(modifier = Modifier.height(24.dp))

            // ── Period toggle ─────────────────────────────────────────────────
            var selectedPeriod by remember { mutableIntStateOf(0) }
            val periods = listOf("Today", "This Week", "This Month")

            PeriodToggle(
                options        = periods,
                selectedIndex  = selectedPeriod,
                onSelect       = { selectedPeriod = it },
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Vico bar chart ────────────────────────────────────────────────
            UsageBarChart(
                data     = mockChartData[selectedPeriod] ?: emptyList(),
                xLabels  = xLabels[selectedPeriod] ?: emptyList(),
                period   = selectedPeriod,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Stats row ─────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatBox(label = "Open count",    value = "47 times",   modifier = Modifier.weight(1f))
                StatBox(label = "Avg session",   value = "3m 12s",     modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Benchmark comparison ──────────────────────────────────────────
            BenchmarkComparisonRow(
                label      = "vs world avg",
                userValue  = "3h 22m / day",
                worldValue = "6h 37m / day",
                isBetter   = true,
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Time limit slider ─────────────────────────────────────────────
            var limitValue by remember { mutableFloatStateOf(0f) }
            TimeLimitSection(value = limitValue, onValueChange = { limitValue = it })
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun AppHeader(appName: String, appColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Colored circle placeholder (replace with real app icon via PackageManager)
        androidx.compose.foundation.layout.Box(
            modifier         = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(appColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = appName.first().toString(),
                style = TitleStyle,
                color = appColor,
            )
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(16.dp))

        Column {
            Text(text = appName, style = TitleStyle, color = TextPrimary)
            Text(text = "Today's usage: 3h 22m", style = CaptionStyle)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                shape    = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                selected = index == selectedIndex,
                onClick  = { onSelect(index) },
                colors   = SegmentedButtonDefaults.colors(
                    activeContainerColor       = Accent.copy(alpha = 0.15f),
                    activeContentColor         = Accent,
                    activeBorderColor          = Accent,
                    inactiveContainerColor     = SurfaceVariant,
                    inactiveContentColor       = TextSecondary,
                    inactiveBorderColor        = TextMuted,
                ),
            ) {
                Text(text = label, style = LabelStyle)
            }
        }
    }
}

@Composable
private fun UsageBarChart(
    data: List<Float>,
    xLabels: List<String>,
    period: Int,
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(period, data) {
        modelProducer.runTransaction {
            columnSeries { series(data) }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    rememberLineComponent(
                        color     = Accent,
                        thickness = 14.dp,
                        shape     = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                    )
                )
            ),
            startAxis  = rememberStartCartesianAxis(),
            bottomAxis = rememberBottomCartesianAxis(
                valueFormatter = { context, value, _ ->
                    xLabels.getOrElse(value.toInt()) { "" }
                }
            ),
        ),
        modelProducer = modelProducer,
        modifier      = Modifier
            .fillMaxWidth()
            .height(180.dp),
    )
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceVariant)
            .padding(16.dp),
    ) {
        Text(text = label, style = LabelStyle, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = value, style = TitleStyle.copy(fontSize = 20.sp), color = TextPrimary)
    }
}

@Composable
private fun BenchmarkComparisonRow(
    label: String,
    userValue: String,
    worldValue: String,
    isBetter: Boolean,
) {
    val valueColor = if (isBetter) Positive else Warning

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceVariant)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Column {
            Text(text = label, style = LabelStyle, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = userValue, style = BodyStyle.copy(fontWeight = FontWeight.SemiBold), color = valueColor)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = "world avg", style = LabelStyle, color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = worldValue, style = BodyStyle, color = TextSecondary)
        }
    }
}

@Composable
private fun TimeLimitSection(value: Float, onValueChange: (Float) -> Unit) {
    val limitText = when {
        value < 1f -> "No limit"
        value < 60f -> "${value.toInt()}m"
        else -> {
            val h = (value / 60).toInt()
            val m = (value % 60).toInt()
            if (m > 0) "${h}h ${m}m" else "${h}h"
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(text = "Daily limit",  style = TitleStyle, color = TextPrimary)
            Text(
                text  = limitText,
                style = TitleStyle.copy(fontSize = 18.sp),
                color = if (value < 1f) TextMuted else Accent,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text  = if (value < 1f) "No overlay — app opens freely"
                    else "Pause overlay shown when limit is reached",
            style = CaptionStyle,
            color = TextMuted,
        )

        Slider(
            value         = value,
            onValueChange = onValueChange,
            valueRange    = 0f..180f,
            modifier      = Modifier.fillMaxWidth(),
            colors        = SliderDefaults.colors(
                thumbColor            = Accent,
                activeTrackColor      = Accent,
                inactiveTrackColor    = SurfaceVariant,
            ),
        )

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Off",   style = CaptionStyle, color = TextMuted)
            Text(text = "3h",    style = CaptionStyle, color = TextMuted)
        }
    }
}
