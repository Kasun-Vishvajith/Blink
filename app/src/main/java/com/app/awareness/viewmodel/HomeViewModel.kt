package com.app.awareness.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.awareness.data.AppDatabase
import com.app.awareness.data.BenchmarkData
import com.app.awareness.service.BenchmarkFetcher
import com.app.awareness.service.InsightEngine
import com.app.awareness.ui.InsightCardData
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

// ── Shared UI models ──────────────────────────────────────────────────────────

data class AppUsageStat(
    val packageName: String,
    val appName: String,
    val totalMinutes: Int,
    val openCount: Int,
)

data class BenchmarkStat(
    val label: String,
    val userValue: String,
    val worldValue: String,
    val isBetter: Boolean,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val ctx = application.applicationContext
    private val db  = AppDatabase.getInstance(ctx)
    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val today: String     = fmt.format(Date())
    private val yesterday: String = fmt.format(
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
    )

    // Benchmark fetched once on init; falls back to DEFAULT if offline
    private val _benchmark = MutableStateFlow(BenchmarkData.DEFAULT)
    init {
        viewModelScope.launch(Dispatchers.IO) {
            _benchmark.value = BenchmarkFetcher(ctx).fetchBenchmarks()
        }
    }

    // ── StateFlows (FRONTEND.md ViewModel Structure) ───────────────────────────

    /** Total screen time today in minutes. */
    val todayScreenTime: StateFlow<Int> = db.dailySummaryDao()
        .getByDate(today)
        .map { it?.totalScreenMinutes ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Top 5 apps by usage today. */
    val topApps: StateFlow<List<AppUsageStat>> = db.appUsageDao()
        .getTopApps(today, 5)
        .map { list -> list.map { e -> AppUsageStat(e.packageName, label(e.packageName), e.totalMinutes, e.openCount) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** InsightEngine output mapped to UI cards. */
    val insights: StateFlow<List<InsightCardData>> = combine(
        db.appUsageDao().queryByDate(today),
        db.dailySummaryDao().getLast7Days(),
        _benchmark,
    ) { todayApps, last7, benchmark ->
        InsightEngine().generate(todayApps, last7, benchmark)
            .take(5)
            .map { InsightCardData(it.message, it.sentiment, it.metric) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Benchmark comparison pills for Section E. */
    val benchmarkComparisons: StateFlow<List<BenchmarkStat>> = combine(
        db.dailySummaryDao().getByDate(today),
        _benchmark,
    ) { summary, bm ->
        val mins = summary?.totalScreenMinutes ?: 0
        listOf(
            BenchmarkStat("Screen time", fmtMin(mins),           fmtMin(bm.dailyScreenTimeMinutes),   mins < bm.dailyScreenTimeMinutes),
            BenchmarkStat("Spotify",     fmtMin(0),              fmtMin(bm.spotifyMinutesDaily),      false),
            BenchmarkStat("Notifs",      "—",                    "${bm.notificationsDaily}",           false),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Positive = used less than yesterday; negative = used more. */
    val yesterdayDelta: StateFlow<Int> = combine(
        db.dailySummaryDao().getByDate(today),
        db.dailySummaryDao().getByDate(yesterday),
    ) { todaySummary, yestSummary ->
        (yestSummary?.totalScreenMinutes ?: 0) - (todaySummary?.totalScreenMinutes ?: 0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun label(pkg: String): String = try {
        val info = ctx.packageManager.getApplicationInfo(pkg, 0)
        ctx.packageManager.getApplicationLabel(info).toString()
    } catch (_: Exception) { pkg.substringAfterLast('.') }

    private fun fmtMin(m: Int): String {
        val h = m / 60; val min = m % 60
        return if (h > 0) "${h}h ${min}m" else "${min}m"
    }
}
