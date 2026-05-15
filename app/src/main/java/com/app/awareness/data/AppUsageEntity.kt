package com.app.awareness.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for table `app_usage`.
 *
 * One row per app per calendar date.
 * Populated by UsageTracker (WorkManager, every 15 min).
 *
 * Table: app_usage
 * Columns: id | package_name | date | total_minutes | open_count | last_opened
 */
@Entity(
    tableName = "app_usage",
    indices = [
        Index(value = ["package_name", "date"], unique = true)
    ]
)
data class AppUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** e.g. "com.instagram.android" */
    @ColumnInfo(name = "package_name")
    val packageName: String,

    /** ISO date string "YYYY-MM-DD" */
    @ColumnInfo(name = "date")
    val date: String,

    /** Cumulative foreground minutes for this app on this date */
    @ColumnInfo(name = "total_minutes")
    val totalMinutes: Int,

    /** Number of times the app was brought to foreground */
    @ColumnInfo(name = "open_count")
    val openCount: Int,

    /** Epoch millis of the most recent foreground event */
    @ColumnInfo(name = "last_opened")
    val lastOpened: Long,
)
