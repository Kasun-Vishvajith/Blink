package com.app.awareness.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import com.app.awareness.data.AppDatabase
import com.app.awareness.data.AppUsageEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Queries [UsageStatsManager] for today's per-app foreground usage,
 * aggregates open counts via [UsageEvents], and upserts the results
 * into the Room `app_usage` table through [AppUsageDao].
 *
 * Called by [UsageWorker] every 15 minutes.
 * Requires the PACKAGE_USAGE_STATS permission (granted by the user in Settings).
 */
class UsageTracker(private val context: Context) {

    private val tag = "UsageTracker"

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private val dao = AppDatabase.getInstance(context).appUsageDao()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Main entry point — called from [UsageWorker.doWork].
     * Suspend so Room inserts run on the worker's coroutine dispatcher.
     */
    suspend fun track() {
        val (startOfDay, now) = dayWindow()
        val today = dateFormatter.format(Date())

        // 1. Per-app foreground time (ms) via INTERVAL_DAILY query
        val statsMap = queryForegroundStats(startOfDay, now)
        if (statsMap.isEmpty()) {
            Log.w(tag, "queryUsageStats returned empty — permission may be missing")
            return
        }

        // 2. Per-app open count + last-opened epoch via event stream
        val (openCountMap, lastOpenedMap) = queryEventCounts(startOfDay, now)

        // 3. Upsert one row per app into Room
        statsMap.forEach { (packageName, totalMs) ->
            val totalMinutes = (totalMs / 1000L / 60L).toInt()
            if (totalMinutes <= 0) return@forEach   // skip system noise

            val entity = AppUsageEntity(
                packageName  = packageName,
                date         = today,
                totalMinutes = totalMinutes,
                openCount    = openCountMap[packageName] ?: 0,
                lastOpened   = lastOpenedMap[packageName] ?: now,
            )
            dao.insert(entity)  // OnConflictStrategy.REPLACE handles re-runs
            Log.d(tag, "Upserted: $packageName → ${totalMinutes}m")
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Returns (startOfToday epoch ms, current epoch ms).
     */
    private fun dayWindow(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis to System.currentTimeMillis()
    }

    /**
     * Queries [UsageStatsManager.INTERVAL_DAILY] and returns a map of
     * packageName → totalTimeInForeground (ms).
     * Only includes entries with foreground time > 0.
     */
    private fun queryForegroundStats(start: Long, end: Long): Map<String, Long> {
        return usageStatsManager
            .queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            .orEmpty()
            .filter { it.totalTimeInForeground > 0 }
            .associate { it.packageName to it.totalTimeInForeground }
    }

    /**
     * Walks the raw [UsageEvents] stream from [start] to [end].
     * Counts every [UsageEvents.Event.ACTIVITY_RESUMED] as one "open".
     * Also tracks the latest resume timestamp per package as [lastOpened].
     *
     * Returns a pair:
     *   - openCountMap:  packageName → number of foreground appearances
     *   - lastOpenedMap: packageName → epoch ms of most recent open
     */
    private fun queryEventCounts(
        start: Long,
        end: Long,
    ): Pair<Map<String, Int>, Map<String, Long>> {
        val openCountMap  = mutableMapOf<String, Int>()
        val lastOpenedMap = mutableMapOf<String, Long>()

        val events = usageStatsManager.queryEvents(start, end)
        val event  = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                val pkg = event.packageName
                openCountMap[pkg] = (openCountMap[pkg] ?: 0) + 1
                if (event.timeStamp > (lastOpenedMap[pkg] ?: 0L)) {
                    lastOpenedMap[pkg] = event.timeStamp
                }
            }
        }

        return openCountMap to lastOpenedMap
    }
}
