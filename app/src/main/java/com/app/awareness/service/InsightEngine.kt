package com.app.awareness.service

import android.util.Log
import com.app.awareness.data.AppUsageEntity
import com.app.awareness.data.BenchmarkData
import com.app.awareness.data.DailySummaryEntity
import com.app.awareness.data.Insight
import com.app.awareness.data.Insight.Metric
import com.app.awareness.data.Insight.Sentiment
import com.app.awareness.data.Insight.Type
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Pure stateless engine that converts raw data into a list of [Insight] objects.
 *
 * **No Room access** — all data is passed in by the caller (DailyAggregator
 * or a ViewModel), making this fully unit-testable without a database.
 *
 * **Inputs:**
 * - [todayApps]  — [AppUsageEntity] list for today (used for app-level opens/minutes)
 * - [last7Days]  — [DailySummaryEntity] list ordered **newest first** (max 7 rows)
 * - [benchmark]  — [BenchmarkData] fetched by BenchmarkFetcher
 *
 * **Outputs:**
 * - [generate] returns a [List<Insight>] (may be empty; capped at 5 by the caller)
 *
 * Four insight types per Module 6 — BACKEND.md:
 *   1. Comparative — user stat vs world average
 *   2. Anomaly     — unusual spike in today's screen time
 *   3. Pattern     — recurring day-of-week high usage
 *   4. Streak      — N consecutive days under world average
 */
class InsightEngine {

    private val tag = "InsightEngine"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // ── Package names ─────────────────────────────────────────────────────────

    private companion object {
        const val PKG_INSTAGRAM = "com.instagram.android"
        const val PKG_SPOTIFY   = "com.spotify.music"

        // Thresholds from BACKEND.md Module 6
        const val COMPARATIVE_THRESHOLD = 0.5   // user < benchmark * 0.5 → insight fires
        const val ANOMALY_THRESHOLD     = 1.5   // today > 7day_avg * 1.5 → insight fires
        const val PATTERN_THRESHOLD     = 1.2   // day_avg > others_avg * 1.2 → insight fires
        const val STREAK_MIN_DAYS       = 2     // at least 2 consecutive days for a streak
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Runs all four insight checks and returns every triggered [Insight].
     *
     * @param todayApps  All [AppUsageEntity] rows for today's date.
     * @param last7Days  Up to 7 [DailySummaryEntity] rows, **newest first**.
     * @param benchmark  Current [BenchmarkData] from Firestore / cache.
     * @return           List of triggered insights (may be empty).
     */
    fun generate(
        todayApps: List<AppUsageEntity>,
        last7Days: List<DailySummaryEntity>,
        benchmark: BenchmarkData,
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        insights += comparative(todayApps, benchmark)
        insights += anomaly(last7Days)
        insights += pattern(last7Days)
        insights += streak(last7Days, benchmark)

        Log.d(tag, "Generated ${insights.size} insight(s)")
        return insights
    }

    // ── 1. Comparative ────────────────────────────────────────────────────────

    /**
     * Fires when a user metric is less than [COMPARATIVE_THRESHOLD] × the world average.
     *
     * Template (Instagram example from spec):
     *   "You open Instagram {user} times a day. Most people open it {benchmark} times.
     *    You're remarkably intentional."
     *
     * Extended: also checks Spotify and total notification count.
     */
    private fun comparative(
        todayApps: List<AppUsageEntity>,
        benchmark: BenchmarkData,
    ): List<Insight> {
        val results = mutableListOf<Insight>()

        // ── Instagram opens ───────────────────────────────────────────────────
        val userInstagramOpens = todayApps
            .find { it.packageName == PKG_INSTAGRAM }?.openCount ?: 0

        if (userInstagramOpens < benchmark.instagramOpensDaily * COMPARATIVE_THRESHOLD) {
            results += Insight(
                type      = Type.COMPARATIVE,
                message   = "You open Instagram $userInstagramOpens times a day. " +
                            "Most people open it ${benchmark.instagramOpensDaily} times. " +
                            "You're remarkably intentional.",
                metric    = Metric.INSTAGRAM_OPENS,
                sentiment = Sentiment.POSITIVE,
            )
        }

        // ── Spotify minutes ───────────────────────────────────────────────────
        val userSpotifyMinutes = todayApps
            .find { it.packageName == PKG_SPOTIFY }?.totalMinutes ?: 0

        if (userSpotifyMinutes < benchmark.spotifyMinutesDaily * COMPARATIVE_THRESHOLD) {
            results += Insight(
                type      = Type.COMPARATIVE,
                message   = "You spent $userSpotifyMinutes minutes on Spotify today. " +
                            "The average is ${benchmark.spotifyMinutesDaily} minutes. " +
                            "Looks like you're in charge of your listening time.",
                metric    = Metric.SPOTIFY_MINUTES,
                sentiment = Sentiment.POSITIVE,
            )
        }

        return results
    }

    // ── 2. Anomaly ────────────────────────────────────────────────────────────

    /**
     * Fires when today's screen time exceeds [ANOMALY_THRESHOLD] × the 6-day average.
     *
     * Template from spec:
     *   "Today was unusual — your screen time was 50% higher than your weekly average."
     */
    private fun anomaly(last7Days: List<DailySummaryEntity>): List<Insight> {
        if (last7Days.size < 2) return emptyList()

        val today   = last7Days.first()
        val past6   = last7Days.drop(1)

        val avgPast = past6.map { it.totalScreenMinutes }.average()
        if (avgPast <= 0) return emptyList()

        val ratio = today.totalScreenMinutes / avgPast

        return if (ratio >= ANOMALY_THRESHOLD) {
            val percentOver = ((ratio - 1) * 100).toInt()
            listOf(
                Insight(
                    type      = Type.ANOMALY,
                    message   = "Today was unusual — your screen time was " +
                                "${percentOver}% higher than your weekly average.",
                    metric    = Metric.SCREEN_TIME,
                    sentiment = Sentiment.WARNING,
                )
            )
        } else emptyList()
    }

    // ── 3. Pattern ────────────────────────────────────────────────────────────

    /**
     * Detects if a single day-of-week in [last7Days] consistently averages
     * more than [PATTERN_THRESHOLD] × the mean of all other days.
     *
     * With 7 days each day-of-week appears at most once, so "consistent" is
     * approximated: the highest-usage day must exceed [PATTERN_THRESHOLD] of
     * the average of all remaining days.
     *
     * Template from spec:
     *   "Your Mondays are consistently your highest screen time day."
     */
    private fun pattern(last7Days: List<DailySummaryEntity>): List<Insight> {
        if (last7Days.size < 4) return emptyList()  // need enough data for comparison

        // Map date string → day-of-week name
        val dayEntries = last7Days.mapNotNull { summary ->
            val date = runCatching { dateFormat.parse(summary.date) }.getOrNull() ?: return@mapNotNull null
            val cal  = Calendar.getInstance().apply { time = date }
            val dayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
                ?: return@mapNotNull null
            Pair(dayName, summary.totalScreenMinutes)
        }

        if (dayEntries.size < 4) return emptyList()

        val highestDay    = dayEntries.maxByOrNull { it.second } ?: return emptyList()
        val othersAverage = dayEntries
            .filter { it.first != highestDay.first }
            .map { it.second }
            .average()

        return if (othersAverage > 0 && highestDay.second > othersAverage * PATTERN_THRESHOLD) {
            listOf(
                Insight(
                    type      = Type.PATTERN,
                    message   = "Your ${highestDay.first}s are consistently your " +
                                "highest screen time day.",
                    metric    = Metric.SCREEN_TIME,
                    sentiment = Sentiment.NEUTRAL,
                )
            )
        } else emptyList()
    }

    // ── 4. Streak ─────────────────────────────────────────────────────────────

    /**
     * Counts how many consecutive recent days (newest first) the user's total
     * screen time was below [BenchmarkData.dailyScreenTimeMinutes].
     *
     * Fires when streak ≥ [STREAK_MIN_DAYS].
     *
     * Template from spec:
     *   "5 days in a row under the world average. That's rare."
     */
    private fun streak(
        last7Days: List<DailySummaryEntity>,
        benchmark: BenchmarkData,
    ): List<Insight> {
        var streakDays = 0

        for (day in last7Days) {  // newest first — break on first day above benchmark
            if (day.totalScreenMinutes < benchmark.dailyScreenTimeMinutes) {
                streakDays++
            } else {
                break
            }
        }

        return if (streakDays >= STREAK_MIN_DAYS) {
            listOf(
                Insight(
                    type      = Type.STREAK,
                    message   = "$streakDays days in a row under the world average. That's rare.",
                    metric    = Metric.SCREEN_TIME,
                    sentiment = Sentiment.POSITIVE,
                )
            )
        } else emptyList()
    }
}
