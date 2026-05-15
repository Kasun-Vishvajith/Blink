package com.app.awareness.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for table `daily_summary`.
 *
 * One row per calendar date — compiled nightly at 23:50 by DailyAggregator.
 * This is the primary source for HomeScreen and WeeklyScreen UI data.
 *
 * Table: daily_summary
 * Columns: date | total_screen_minutes | top_app | top_app_minutes |
 *          music_minutes | call_minutes | notification_count |
 *          screen_time_vs_benchmark_percent
 */
@Entity(
    tableName = "daily_summary",
    indices = [Index(value = ["date"], unique = true)]
)
data class DailySummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** ISO date string "YYYY-MM-DD" — unique per row */
    @ColumnInfo(name = "date")
    val date: String,

    /** Sum of total_minutes across all apps in app_usage for this date */
    @ColumnInfo(name = "total_screen_minutes")
    val totalScreenMinutes: Int,

    /** Package name of the app with the highest total_minutes */
    @ColumnInfo(name = "top_app")
    val topApp: String,

    /** total_minutes for the top app */
    @ColumnInfo(name = "top_app_minutes")
    val topAppMinutes: Int,

    /** Sum of play_minutes from media_sessions for this date */
    @ColumnInfo(name = "music_minutes")
    val musicMinutes: Int,

    /** Sum of duration_minutes from call_sessions for this date */
    @ColumnInfo(name = "call_minutes")
    val callMinutes: Int,

    /** Sum of count from notifications for this date */
    @ColumnInfo(name = "notification_count")
    val notificationCount: Int,

    /**
     * (total_screen_minutes / benchmark_daily_screen_time_minutes) * 100.
     * e.g. 85 means the user used 85% of the world average.
     * Populated using the cached BenchmarkFetcher value.
     */
    @ColumnInfo(name = "screen_time_vs_benchmark_percent")
    val screenTimeVsBenchmarkPercent: Int,
)
