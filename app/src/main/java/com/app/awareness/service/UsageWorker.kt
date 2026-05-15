package com.app.awareness.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * WorkManager [CoroutineWorker] that runs [UsageTracker.track] on a
 * 15-minute repeating schedule.
 *
 * Schedule it once at app startup (or after boot) via [schedule].
 * WorkManager guarantees the work survives process death and device restarts.
 *
 * Minimum repeat interval enforced by WorkManager is exactly 15 minutes,
 * which matches the spec in BACKEND.md Module 1.
 */
class UsageWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            UsageTracker(applicationContext).track()
            Log.d(TAG, "UsageTracker completed successfully")
            Result.success()
        } catch (e: SecurityException) {
            // PACKAGE_USAGE_STATS permission not granted — do not retry,
            // the user must grant it manually in Settings.
            Log.e(TAG, "PACKAGE_USAGE_STATS permission missing", e)
            Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "UsageTracker failed — will retry next interval", e)
            Result.retry()
        }
    }

    // ── Static helpers ────────────────────────────────────────────────────────

    companion object {
        private const val TAG           = "UsageWorker"
        private const val WORK_NAME     = "com.app.awareness.UsageWorker"
        private const val INTERVAL_MINS = 15L

        /**
         * Enqueues a unique periodic work request.
         * Call this from Application.onCreate() and from a BOOT_COMPLETED receiver.
         *
         * [ExistingPeriodicWorkPolicy.KEEP] ensures re-scheduling on boot
         * does not reset the 15-minute timer if the worker is already queued.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UsageWorker>(
                INTERVAL_MINS, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        // Allow on low battery — usage data is lightweight
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )

            Log.d(TAG, "Scheduled — repeating every ${INTERVAL_MINS}m")
        }

        /**
         * Cancels the periodic work. Call only when the user revokes
         * PACKAGE_USAGE_STATS permission or resets all data.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Cancelled")
        }
    }
}
