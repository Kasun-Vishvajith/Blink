package com.app.awareness.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for table `call_sessions`.
 *
 * One row per detected VOIP/microphone call session.
 * Populated by MicrophoneCallDetector (polled every 5 min).
 *
 * Table: call_sessions
 * Columns: id | package_name | start_time | end_time | duration_minutes | date
 */
@Entity(
    tableName = "call_sessions",
    indices = [Index(value = ["date"])]
)
data class CallSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** e.g. "com.whatsapp", "org.telegram.messenger" */
    @ColumnInfo(name = "package_name")
    val packageName: String,

    /** Epoch millis when microphone access began */
    @ColumnInfo(name = "start_time")
    val startTime: Long,

    /** Epoch millis when microphone was released; null if still active */
    @ColumnInfo(name = "end_time")
    val endTime: Long?,

    /** Calculated from (end_time - start_time); null while session is active */
    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int?,

    /** ISO date string "YYYY-MM-DD" matching the session start date */
    @ColumnInfo(name = "date")
    val date: String,
)
