package com.app.awareness.service

/*
 * ════════════════════════════════════════════════════════════════════════════
 * MANIFEST ADDITION REQUIRED
 * Add the following <service> block inside <application> in AndroidManifest.xml:
 *
 *     <service
 *         android:name=".service.NotificationListenerServiceImpl"
 *         android:label="@string/app_name"
 *         android:exported="true"
 *         android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
 *         <intent-filter>
 *             <action android:name="android.service.notification.NotificationListenerService" />
 *         </intent-filter>
 *     </service>
 *
 * The user must also grant Notification Access manually via:
 *   Settings → Apps → Special app access → Notification access
 *
 * Check status at runtime with:
 *   NotificationManagerCompat.getEnabledListenerPackages(context)
 *       .contains(context.packageName)
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Expected NotificationDao interface (already assumed to exist):
 *
 *   @Dao interface NotificationDao {
 *       @Insert(onConflict = OnConflictStrategy.IGNORE)
 *       suspend fun insert(entity: NotificationEntity): Long
 *
 *       @Query("UPDATE notifications SET count = count + 1 WHERE package_name = :pkg AND date = :date")
 *       suspend fun increment(pkg: String, date: String): Int   // returns rows affected
 *   }
 * ════════════════════════════════════════════════════════════════════════════
 */

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.app.awareness.data.AppDatabase
import com.app.awareness.data.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Listens for every posted notification via the system's
 * [NotificationListenerService] API and increments (or seeds) a per-app
 * daily counter in the Room `notifications` table.
 *
 * Lifecycle is fully managed by the OS — do NOT start/stop this manually.
 * WorkManager or any activity should never bind to this service directly.
 *
 * Module 4 — BACKEND.md
 */
class NotificationListenerServiceImpl : NotificationListenerService() {

    private val tag = "NotifListener"

    // Coroutine scope tied to the service lifetime — cancelled in onDestroy.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val dao by lazy {
        AppDatabase.getInstance(applicationContext).notificationDao()
    }

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // ── Service lifecycle ─────────────────────────────────────────────────────

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(tag, "Notification listener connected")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(tag, "Notification listener destroyed — scope cancelled")
    }

    // ── Core callback ─────────────────────────────────────────────────────────

    /**
     * Called by the OS on the main thread for every new notification.
     * The Room write is dispatched to [Dispatchers.IO] to avoid blocking UI.
     */
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        val packageName = sbn.packageName ?: return
        val date        = dateFormatter.format(Date())

        serviceScope.launch {
            incrementOrInsert(packageName, date)
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Atomic increment-or-insert for the (package_name, date) pair:
     *
     * 1. Try `UPDATE notifications SET count = count + 1 WHERE ...` — O(1), no read.
     * 2. If no row exists yet (rowsAffected == 0), INSERT a fresh row with count = 1.
     *
     * This pattern avoids a read-modify-write race and keeps the hot path to
     * a single statement for apps that spam many notifications.
     */
    private suspend fun incrementOrInsert(packageName: String, date: String) {
        try {
            val rowsUpdated = dao.increment(packageName, date)
            if (rowsUpdated == 0) {
                // First notification from this app today — seed the row
                dao.insert(
                    NotificationEntity(
                        packageName = packageName,
                        date        = date,
                        count       = 1,
                    )
                )
                Log.d(tag, "Seeded: $packageName on $date")
            } else {
                Log.d(tag, "Incremented: $packageName on $date (+1)")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to record notification for $packageName", e)
        }
    }
}
