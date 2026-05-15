package com.app.awareness.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.awareness.data.AppDatabase
import com.app.awareness.data.BenchmarkData
import com.app.awareness.service.BenchmarkFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ── UI model ──────────────────────────────────────────────────────────────────

/** Typed data for WeeklyScreen cards — matches the 7-card spec in FRONTEND.md. */
data class WeeklyCardData(
    val weekRange: String,
    val weekTotalHours: Int,
    val weekTotalMins: Int,
    val prevWeekDeltaHours: Int,
    val deltaIsWorse: Boolean,
    val topAppName: String,
    val topAppOpenCount: Int,
    val topAppHours: Int,
    val bestDayName: String,
    val bestDayHours: Int,
    val bestDayMins: Int,
    val callMinutes: Int,
    val callApps: List<String>,
    val musicAppName: String,
    val musicHours: Int,
    val musicBgPercent: Int,
    val benchmarkSentiment: String,
    val benchmarkComparisons: List<BenchmarkStat>,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class WeeklyViewModel(application: Application) : AndroidViewModel(application) {

    private val ctx = application.applicationContext
    private val db  = AppDatabase.getInstance(ctx)
    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispFmt = SimpleDateFormat("MMM d", Locale.getDefault())
    private val dayFmt  = SimpleDateFormat("EEEE", Locale.getDefault())

    private val _benchmark = MutableStateFlow(BenchmarkData.DEFAULT)
    init {
        viewModelScope.launch(Dispatchers.IO) {
            _benchmark.value = BenchmarkFetcher(ctx).fetchBenchmarks()
        }
    }

    // ── StateFlows (FRONTEND.md ViewModel Structure) ───────────────────────────

    /** Aggregated weekly totals in minutes. */
    val weekTotalMinutes: StateFlow<Int> = db.dailySummaryDao()
        .getLast7Days()
        .map { days -> days.sumOf { it.totalScreenMinutes } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Day name with the lowest screen time this week. */
    val bestDay: StateFlow<String> = db.dailySummaryDao()
        .getLast7Days()
        .map { days ->
            days.minByOrNull { it.totalScreenMinutes }?.let { summary ->
                runCatching { dayFmt.format(fmt.parse(summary.date)!!) }.getOrElse { "—" }
            } ?: "—"
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "—")

    /** Top app of the week by total minutes across all app_usage rows. */
    val topApp: StateFlow<AppUsageStat> = db.appUsageDao()
        .queryByDateRange(sevenDaysAgo(), today())
        .map { entities ->
            entities.groupBy { it.packageName }
                .mapValues { (_, rows) -> rows.sumOf { it.totalMinutes } }
                .maxByOrNull { it.value }
                ?.let { (pkg, mins) ->
                    AppUsageStat(pkg, label(pkg), mins, 0)
                } ?: AppUsageStat("", "—", 0, 0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUsageStat("", "—", 0, 0))

    /** Full weekly card data combining all the above for WeeklyScreen. */
    val weeklyCards: StateFlow<List<WeeklyCardData>> = combine(
        db.dailySummaryDao().getLast7Days(),
        topApp,
        _benchmark,
    ) { days, top, bm ->
        if (days.isEmpty()) return@combine emptyList()

        val totalMins   = days.sumOf { it.totalScreenMinutes }
        val totalHours  = totalMins / 60
        val totalMinRem = totalMins % 60
        val bmWeeklyAvg = bm.dailyScreenTimeMinutes * 7

        val bestDaySummary = days.minByOrNull { it.totalScreenMinutes }
        val bestDayStr = bestDaySummary?.let {
            runCatching { dayFmt.format(fmt.parse(it.date)!!) }.getOrElse { "—" }
        } ?: "—"
        val bestDayMins   = bestDaySummary?.totalScreenMinutes ?: 0

        val weekRange = buildWeekRange()

        listOf(
            WeeklyCardData(
                weekRange            = weekRange,
                weekTotalHours       = totalHours,
                weekTotalMins        = totalMinRem,
                prevWeekDeltaHours   = 0,          // TODO: compare with prev week
                deltaIsWorse         = false,
                topAppName           = top.appName,
                topAppOpenCount      = top.openCount,
                topAppHours          = top.totalMinutes / 60,
                bestDayName          = bestDayStr,
                bestDayHours         = bestDayMins / 60,
                bestDayMins          = bestDayMins % 60,
                callMinutes          = days.sumOf { it.callMinutes },
                callApps             = listOf("WhatsApp"),
                musicAppName         = "Spotify",
                musicHours           = days.sumOf { it.musicMinutes } / 60,
                musicBgPercent       = 65,
                benchmarkSentiment   = if (totalMins < bmWeeklyAvg) "You're using your phone more mindfully than most." else "You're above the world average this week.",
                benchmarkComparisons = listOf(
                    BenchmarkStat("Weekly screen time", fmtMin(totalMins), fmtMin(bmWeeklyAvg), totalMins < bmWeeklyAvg),
                    BenchmarkStat("Daily notifs", "—", "${bm.notificationsDaily}", false),
                ),
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun today() = fmt.format(Date())
    private fun sevenDaysAgo() = fmt.format(
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }.time
    )
    private fun buildWeekRange(): String {
        val end   = dispFmt.format(Date())
        val start = dispFmt.format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }.time)
        return "$start – $end"
    }
    private fun label(pkg: String): String = try {
        val info = ctx.packageManager.getApplicationInfo(pkg, 0)
        ctx.packageManager.getApplicationLabel(info).toString()
    } catch (_: Exception) { pkg.substringAfterLast('.') }
    private fun fmtMin(m: Int): String {
        val h = m / 60; val min = m % 60; return if (h > 0) "${h}h ${min}m" else "${min}m"
    }
}
