package com.app.awareness.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for table `media_sessions`.
 *
 * One row per app per calendar date.
 * Populated by MediaSessionTracker (MediaController.Callback).
 *
 * Table: media_sessions
 * Columns: id | package_name | date | play_minutes | pause_minutes | background_play_minutes
 */
@Entity(
    tableName = "media_sessions",
    indices = [
        Index(value = ["package_name", "date"], unique = true)
    ]
)
data class MediaSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** e.g. "com.spotify.music", "com.google.android.youtube" */
    @ColumnInfo(name = "package_name")
    val packageName: String,

    /** ISO date string "YYYY-MM-DD" */
    @ColumnInfo(name = "date")
    val date: String,

    /** Cumulative minutes in PLAYING state */
    @ColumnInfo(name = "play_minutes")
    val playMinutes: Int,

    /** Cumulative minutes in PAUSED state */
    @ColumnInfo(name = "pause_minutes")
    val pauseMinutes: Int,

    /**
     * Minutes where media was playing but the app was NOT in the foreground.
     * Cross-referenced with UsageStats by MediaSessionTracker.
     */
    @ColumnInfo(name = "background_play_minutes")
    val backgroundPlayMinutes: Int,
)
