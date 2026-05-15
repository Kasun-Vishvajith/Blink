package com.app.awareness.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for table `notifications`.
 *
 * One row per app per calendar date.
 * Populated by NotificationListenerServiceImpl on each onNotificationPosted event.
 *
 * Table: notifications
 * Columns: id | package_name | date | count
 */
@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["package_name", "date"], unique = true)
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** e.g. "com.instagram.android" */
    @ColumnInfo(name = "package_name")
    val packageName: String,

    /** ISO date string "YYYY-MM-DD" */
    @ColumnInfo(name = "date")
    val date: String,

    /** Running total of notifications received from this app on this date */
    @ColumnInfo(name = "count")
    val count: Int,
)
