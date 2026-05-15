package com.app.awareness.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.app.awareness.ui.PauseOverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Persistent foreground service that monitors the active foreground app every second
 * and shows [PauseOverlayManager] when the user opens a tracked app.
 *
 * **Lifecycle:** Started from Application.onCreate() + a BOOT_COMPLETED receiver.
 * Never needs to be restarted manually — returns [START_STICKY].
 *
 * **Detection:** Uses [UsageStatsManager.queryEvents] over a rolling 3-second window
 * and looks for the most recent [UsageEvents.Event.ACTIVITY_RESUMED] event.
 *
 * **Tracked apps:** Hardcoded list below. TODO: replace with user-configured Room table.
 *
 * Module 8 — BACKEND.md / Composable — Pause Overlay — FRONTEND.md
 */
class OverlayService : Service() {

    private val tag = "OverlayService"

    // ── System services ───────────────────────────────────────────────────────

    private val usageStatsManager by lazy {
        getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    private val overlayManager by lazy { PauseOverlayManager(applicationContext) }

    private val notificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }

    // ── Coroutine scope ───────────────────────────────────────────────────────

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null

    // ── Tracked apps ──────────────────────────────────────────────────────────

    /**
     * Apps that trigger the pause overlay on open.
     * TODO: replace with user-configured list stored in Room / SharedPreferences.
     */
    private val trackedPackages = setOf(
        "com.instagram.android",
        "com.facebook.katana",
        "com.twitter.android",
        "com.zhiliaoapp.musically",   // TikTok
        "com.reddit.frontpage",
        "com.snapchat.android",
        "com.youtube.android",
        "com.google.android.youtube",
    )

    // ── Settings ──────────────────────────────────────────────────────────────

    private val prefs by lazy {
        getSharedPreferences("awareness_settings", Context.MODE_PRIVATE)
    }

    /** Returns the overlay hold duration in ms. 0 = overlay disabled. */
    private val overlayDelayMs: Long
        get() = when (prefs.getString("overlay_delay", "1")) {
            "1"  -> 1_000L
            "2"  -> 2_000L
            else -> 0L      // "Off" — don't show overlay
        }

    // ── Service lifecycle ─────────────────────────────────────────────────────

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        Log.d(tag, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startMonitoring()
        return START_STICKY     // OS restarts the service if killed
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        serviceScope.launch(Dispatchers.Main) { overlayManager.dismiss() }
        super.onDestroy()
        Log.d(tag, "Service destroyed")
    }

    // ── Foreground app monitoring ─────────────────────────────────────────────

    private fun startMonitoring() {
        if (monitorJob?.isActive == true) return

        monitorJob = serviceScope.launch {
            var lastForegroundPackage = ""

            while (isActive) {
                val currentPackage = getForegroundPackage()

                if (!currentPackage.isNullOrEmpty() &&
                    currentPackage != lastForegroundPackage &&
                    currentPackage != packageName           // don't trigger on our own app
                ) {
                    lastForegroundPackage = currentPackage

                    val delay = overlayDelayMs
                    if (delay > 0L && currentPackage in trackedPackages) {
                        val appName      = getAppLabel(currentPackage)
                        val todayMinutes = getTodayMinutes(currentPackage)

                        withContext(Dispatchers.Main) {
                            overlayManager.show(
                                appName        = appName,
                                todayMinutes   = todayMinutes,
                                dismissDelayMs = delay,
                            )
                        }
                        Log.d(tag, "Overlay triggered: $currentPackage")
                    }
                }

                delay(1_000L)   // poll every 1 second
            }
        }
    }

    // ── UsageStats helpers ────────────────────────────────────────────────────

    /**
     * Queries the most recent [UsageEvents.Event.ACTIVITY_RESUMED] in the last 3 seconds.
     * Returns the package name or null if PACKAGE_USAGE_STATS permission is missing.
     */
    private fun getForegroundPackage(): String? {
        return try {
            val end   = System.currentTimeMillis()
            val start = end - 3_000L

            val events = usageStatsManager.queryEvents(start, end)
            val event  = UsageEvents.Event()
            var lastPkg: String? = null

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastPkg = event.packageName
                }
            }
            lastPkg
        } catch (e: SecurityException) {
            Log.w(tag, "PACKAGE_USAGE_STATS permission missing")
            null
        }
    }

    /**
     * Total foreground minutes for [packageName] today, via INTERVAL_DAILY query.
     */
    private fun getTodayMinutes(packageName: String): Int {
        return try {
            val now   = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - 24 * 60 * 60 * 1_000L,
                now,
            )
            val ms = stats.find { it.packageName == packageName }?.totalTimeInForeground ?: 0L
            (ms / 60_000L).toInt()
        } catch (e: Exception) {
            0
        }
    }

    /** Returns the human-readable app label, falling back to [packageName]. */
    private fun getAppLabel(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    // ── Foreground notification ───────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Awareness Monitor",
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Awareness")
            .setContentText("Monitoring app usage")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setOngoing(true)
            .build()

    companion object {
        private const val CHANNEL_ID       = "awareness_monitor"
        private const val NOTIFICATION_ID  = 1001

        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }
}
