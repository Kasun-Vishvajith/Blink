package com.app.awareness.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageDao {

    // ── Writes ────────────────────────────────────────────────────────────────

    /**
     * Insert or replace a usage row.
     * REPLACE strategy handles the case where UsageTracker re-processes
     * the same (package_name, date) pair on subsequent 15-min polls.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AppUsageEntity)

    // ── Reads ─────────────────────────────────────────────────────────────────

    /**
     * All app usage rows for a single date, ordered by total_minutes descending.
     * @param date ISO string "YYYY-MM-DD"
     */
    @Query("""
        SELECT * FROM app_usage
        WHERE date = :date
        ORDER BY total_minutes DESC
    """)
    fun queryByDate(date: String): Flow<List<AppUsageEntity>>

    /**
     * All app usage rows within an inclusive date range.
     * Useful for weekly screen breakdown.
     * @param from ISO string "YYYY-MM-DD" (inclusive)
     * @param to   ISO string "YYYY-MM-DD" (inclusive)
     */
    @Query("""
        SELECT * FROM app_usage
        WHERE date >= :from AND date <= :to
        ORDER BY date ASC, total_minutes DESC
    """)
    fun queryByDateRange(from: String, to: String): Flow<List<AppUsageEntity>>

    /**
     * Top N apps by total usage minutes for a given date.
     * Used by HomeScreen "Where your time went" section.
     * @param date  ISO string "YYYY-MM-DD"
     * @param limit number of apps to return (default 5)
     */
    @Query("""
        SELECT * FROM app_usage
        WHERE date = :date
        ORDER BY total_minutes DESC
        LIMIT :limit
    """)
    fun getTopApps(date: String, limit: Int = 5): Flow<List<AppUsageEntity>>

    /**
     * Non-flow version for one-off lookups (e.g. by PillNotificationService).
     */
    @Query("SELECT total_minutes FROM app_usage WHERE package_name=:pkg AND date=:date LIMIT 1")
    suspend fun queryByDateOnce(pkg: String, date: String): Int?
}
