package com.app.awareness.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.app.awareness.data.AppDatabase
import com.app.awareness.ui.DynamicPillManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lightweight service that shows a [DynamicPillManager] notification for three events:
 *
 * 1. **App opened** → "X minutes on [AppName] today"
 * 2. **Call ended** → "~[N] min call on [AppName]"
 * 3. **Music session ended** → "[AppName] played for [N] min today"
 *
 * Called by other services via [notifyAppOpened], [notifyCallEnded], [notifyMusicEnded].
 * Reads Room DAOs to build the message so the calling service only needs to supply
 * a package name (no data coupling between services).
 *
 * Module 9 — BACKEND.md
 */
class PillNotificationService : Service() {

    private val tag = "PillNotifService"

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val pillManager by lazy { DynamicPillManager(applicationContext) }

    private val db by lazy { AppDatabase.getInstance(applicationContext) }

    private val today: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // ── Service lifecycle ─────────────────────────────────────────────────────

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return START_NOT_STICKY

        when (intent.getStringExtra(EXTRA_TRIGGER)) {

            TRIGGER_APP_OPENED -> {
                val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: return START_NOT_STICKY
                handleAppOpened(pkg)
            }

            TRIGGER_CALL_ENDED -> {
                val pkg      = intent.getStringExtra(EXTRA_PACKAGE) ?: return START_NOT_STICKY
                val duration = intent.getIntExtra(EXTRA_DURATION, 0)
                handleCallEnded(pkg, duration)
            }

            TRIGGER_MUSIC_ENDED -> {
                val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: return START_NOT_STICKY
                handleMusicEnded(pkg)
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    // ── Trigger handlers ──────────────────────────────────────────────────────

    /**
     * Trigger 1: App opened.
     * Reads today's usage from [AppUsageDao] and shows:
     *   "[X] min on [AppName] today"
     */
    private fun handleAppOpened(packageName: String) {
        serviceScope.launch {
            val minutes = db.appUsageDao()
                .queryByDateOnce(packageName, today) ?: 0

            val appLabel = getAppLabel(packageName)
            val sentiment = when {
                minutes > 120 -> "warning"
                minutes < 30  -> "positive"
                else          -> "neutral"
            }

            val message = when {
                minutes == 0 -> "Opening $appLabel"
                else         -> "${minutes}m on $appLabel today"
            }

            showPill(message, sentiment)
            Log.d(tag, "App opened pill: $message")
        }
    }

    /**
     * Trigger 2: Call ended.
     * Duration is supplied by the caller (MicrophoneCallDetector already computed it).
     * Shows: "~[N] min call on [AppName]"
     */
    private fun handleCallEnded(packageName: String, durationMinutes: Int) {
        serviceScope.launch {
            val appLabel = getAppLabel(packageName)
            val message  = "~${durationMinutes} min call on $appLabel"
            showPill(message, "neutral")
            Log.d(tag, "Call ended pill: $message")
        }
    }

    /**
     * Trigger 3: Music session ended.
     * Reads today's total play minutes from [MediaSessionDao].
     * Shows: "[AppName] played for [N] min today"
     */
    private fun handleMusicEnded(packageName: String) {
        serviceScope.launch {
            val playMinutes = db.mediaSessionDao()
                .getPlayMinutesToday(packageName, today) ?: 0

            if (playMinutes == 0) return@launch     // nothing to report

            val appLabel = getAppLabel(packageName)
            val message  = "$appLabel played for ${playMinutes}m today"
            showPill(message, "positive")
            Log.d(tag, "Music ended pill: $message")
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun showPill(message: String, sentiment: String) {
        withContext(Dispatchers.Main) {
            pillManager.show(message, sentiment)
        }
    }

    /** Returns the human-readable app label, falling back to the package name. */
    private fun getAppLabel(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }
    }

    // ── Static entry points ───────────────────────────────────────────────────

    companion object {
        private const val EXTRA_TRIGGER  = "trigger"
        private const val EXTRA_PACKAGE  = "package"
        private const val EXTRA_DURATION = "duration"

        const val TRIGGER_APP_OPENED  = "APP_OPENED"
        const val TRIGGER_CALL_ENDED  = "CALL_ENDED"
        const val TRIGGER_MUSIC_ENDED = "MUSIC_ENDED"

        /**
         * Call from [OverlayService] when a tracked app enters the foreground.
         * The service will query Room for today's usage minutes.
         */
        fun notifyAppOpened(context: Context, packageName: String) {
            context.startService(
                Intent(context, PillNotificationService::class.java).apply {
                    putExtra(EXTRA_TRIGGER, TRIGGER_APP_OPENED)
                    putExtra(EXTRA_PACKAGE, packageName)
                }
            )
        }

        /**
         * Call from [MicrophoneCallDetector] when a call session is closed.
         * Pass the pre-computed [durationMinutes] so no extra Room query is needed.
         */
        fun notifyCallEnded(context: Context, packageName: String, durationMinutes: Int) {
            context.startService(
                Intent(context, PillNotificationService::class.java).apply {
                    putExtra(EXTRA_TRIGGER, TRIGGER_CALL_ENDED)
                    putExtra(EXTRA_PACKAGE, packageName)
                    putExtra(EXTRA_DURATION, durationMinutes)
                }
            )
        }

        /**
         * Call from [MediaSessionTracker] when a media session transitions to stopped/paused.
         * The service queries Room for today's total play minutes.
         */
        fun notifyMusicEnded(context: Context, packageName: String) {
            context.startService(
                Intent(context, PillNotificationService::class.java).apply {
                    putExtra(EXTRA_TRIGGER, TRIGGER_MUSIC_ENDED)
                    putExtra(EXTRA_PACKAGE, packageName)
                }
            )
        }
    }
}
