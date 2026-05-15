package com.app.awareness.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySummaryDao {

    // ── Writes ────────────────────────────────────────────────────────────────

    /**
     * Insert or replace a daily summary row.
     * REPLACE handles re-runs of DailyAggregator (e.g. if it re-fires before midnight).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DailySummaryEntity)

    // ── Reads ─────────────────────────────────────────────────────────────────

    /**
     * Single summary row for a given date.
     * Returns null if DailyAggregator hasn't run yet for that date.
     * @param date ISO string "YYYY-MM-DD"
     */
    @Query("""
        SELECT * FROM daily_summary
        WHERE date = :date
        LIMIT 1
    """)
    fun getByDate(date: String): Flow<DailySummaryEntity?>

    /**
     * The 7 most recent daily summary rows, newest first.
     * Used by WeeklyScreen and InsightEngine for trend/streak analysis.
     */
    @Query("""
        SELECT * FROM daily_summary
        ORDER BY date DESC
        LIMIT 7
    """)
    fun getLast7Days(): Flow<List<DailySummaryEntity>>
}
