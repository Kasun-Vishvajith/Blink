package com.app.awareness.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Single Room database for the Awareness app.
 *
 * Entities: AppUsageEntity, CallSessionEntity, MediaSessionEntity,
 *           NotificationEntity, DailySummaryEntity
 *
 * Increment [version] and provide a [Migration] whenever the schema changes.
 */
@Database(
    entities = [
        AppUsageEntity::class,
        CallSessionEntity::class,
        MediaSessionEntity::class,
        NotificationEntity::class,
        DailySummaryEntity::class,
    ],
    version = 1,
    exportSchema = true,            // keep schema JSON for migration diffs
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appUsageDao(): AppUsageDao
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun callSessionDao(): CallSessionDao
    abstract fun mediaSessionDao(): MediaSessionDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        private const val DB_NAME = "awareness.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME,
                )
                    .fallbackToDestructiveMigration() // TODO: replace with proper Migrations before release
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
