package com.app.awareness.service

import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import com.app.awareness.data.AppDatabase
import com.app.awareness.data.MediaSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tracks play/pause state for every active [MediaController] session and
 * calculates net play time, pause time, and background play time per app per day.
 *
 * **How it works:**
 * 1. Calls [MediaSessionManager.getActiveSessions] (requires the
 *    [NotificationListenerServiceImpl] to be connected — it provides the
 *    [ComponentName] used for permission gating).
 * 2. Attaches a [MediaController.Callback] to each controller.
 * 3. On every [PlaybackState] transition, updates in-memory [SessionState].
 * 4. On pause/stop/session-destroy, flushes accumulated ms to Room via [MediaSessionDao].
 * 5. Background play is detected by cross-referencing the play segment with
 *    [UsageStatsManager] — if the app had zero foreground time during the segment,
 *    the entire play duration is counted as background.
 *
 * **Lifecycle:** Call [start] once (e.g. from [NotificationListenerServiceImpl.onListenerConnected])
 * and [stop] in the service's [onDestroy].
 *
 * Module 3 — BACKEND.md
 */
class MediaSessionTracker(private val context: Context) {

    private val tag = "MediaSessionTracker"

    // ── System services ───────────────────────────────────────────────────────

    private val mediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    // Requires NotificationListenerService to be connected
    private val listenerComponent =
        ComponentName(context, NotificationListenerServiceImpl::class.java)

    private val dao by lazy {
        AppDatabase.getInstance(context).mediaSessionDao()
    }

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Scope for all Room writes — cancelled in stop()
    private val trackerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── In-memory per-session state ───────────────────────────────────────────

    /**
     * Mutable accumulator for one app's playback during the current day.
     * All timestamps are epoch millis; accumulated values are ms.
     */
    private data class SessionState(
        val packageName: String,
        var isPlaying: Boolean   = false,
        var playStartMs: Long    = 0L,
        var pauseStartMs: Long   = 0L,
        var accPlayMs: Long      = 0L,
        var accPauseMs: Long     = 0L,
        var accBackgroundMs: Long = 0L,
    )

    private val sessionStates   = mutableMapOf<String, SessionState>()
    private val controllers     = mutableMapOf<String, MediaController>()
    private val callbacks       = mutableMapOf<String, MediaController.Callback>()

    // ── Session-changed listener ──────────────────────────────────────────────

    private val activeSessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { newControllers ->
            val activePkgs = newControllers?.map { it.packageName }?.toSet() ?: emptySet()

            // Attach any newly-appeared sessions
            newControllers?.forEach { attachController(it) }

            // Flush + detach sessions that have disappeared
            controllers.keys
                .filter { it !in activePkgs }
                .toList()           // snapshot — detachController mutates the map
                .forEach { detachController(it) }
        }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Attaches to all currently-active media sessions and registers the
     * sessions-changed listener. Safe to call multiple times — duplicate
     * [attachController] calls are no-ops.
     */
    fun start() {
        try {
            mediaSessionManager.getActiveSessions(listenerComponent)
                .forEach { attachController(it) }

            mediaSessionManager.addOnActiveSessionsChangedListener(
                activeSessionsChangedListener,
                listenerComponent,
            )
            Log.d(tag, "Started — watching ${controllers.size} session(s)")
        } catch (e: SecurityException) {
            Log.e(tag, "Notification listener not connected — cannot get sessions", e)
        }
    }

    /**
     * Flushes all active sessions to Room, unregisters all callbacks,
     * and cancels the coroutine scope. Call from the host service's onDestroy.
     */
    fun stop() {
        mediaSessionManager.removeOnActiveSessionsChangedListener(activeSessionsChangedListener)
        controllers.keys.toList().forEach { detachController(it) }
        trackerScope.cancel()
        Log.d(tag, "Stopped")
    }

    // ── Controller management ─────────────────────────────────────────────────

    private fun attachController(controller: MediaController) {
        val pkg = controller.packageName
        if (pkg in controllers) return          // already tracking this app

        val state = SessionState(packageName = pkg)
        sessionStates[pkg] = state

        val cb = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(playbackState: PlaybackState?) {
                playbackState ?: return
                handleStateChange(pkg, playbackState.state)
            }

            override fun onSessionDestroyed() {
                Log.d(tag, "Session destroyed: $pkg")
                detachController(pkg)
            }
        }

        controller.registerCallback(cb)
        controllers[pkg] = controller
        callbacks[pkg]   = cb

        // Sync to whatever state the session is already in
        controller.playbackState?.let { handleStateChange(pkg, it.state) }
        Log.d(tag, "Attached callback: $pkg")
    }

    private fun detachController(packageName: String) {
        flushSession(packageName)               // persist before removing

        val cb = callbacks.remove(packageName)
        controllers.remove(packageName)?.let { ctrl ->
            cb?.let { ctrl.unregisterCallback(it) }
        }
        sessionStates.remove(packageName)
        Log.d(tag, "Detached: $packageName")
    }

    // ── Playback state machine ────────────────────────────────────────────────

    /**
     * Called on every [PlaybackState] change for a given package.
     *
     * State machine:
     *   → PLAYING  : close any open pause segment; open a play segment.
     *   → not-PLAYING: close the play segment; check background; open pause if PAUSED.
     *
     * On every transition *away* from PLAYING the session is flushed to Room so
     * data is never lost if the process is killed between flush points.
     */
    private fun handleStateChange(packageName: String, newState: Int) {
        val s   = sessionStates[packageName] ?: return
        val now = System.currentTimeMillis()

        val transitToPlaying  = newState == PlaybackState.STATE_PLAYING && !s.isPlaying
        val transitFromPlaying = newState != PlaybackState.STATE_PLAYING &&  s.isPlaying

        when {
            transitToPlaying -> {
                // Close any open pause segment
                if (s.pauseStartMs > 0L) {
                    s.accPauseMs += now - s.pauseStartMs
                    s.pauseStartMs = 0L
                }
                s.playStartMs = now
                s.isPlaying   = true
            }

            transitFromPlaying -> {
                // Close the open play segment
                if (s.playStartMs > 0L) {
                    val playDurationMs = now - s.playStartMs
                    s.accPlayMs += playDurationMs

                    // Attribute to background if not in foreground during that segment
                    if (!wasInForeground(packageName, s.playStartMs, now)) {
                        s.accBackgroundMs += playDurationMs
                    }
                    s.playStartMs = 0L
                }

                // Open a pause segment if specifically PAUSED (not STOPPED/IDLE)
                if (newState == PlaybackState.STATE_PAUSED) {
                    s.pauseStartMs = now
                }
                s.isPlaying = false

                // Flush on every pause/stop so data survives process death
                flushSession(packageName)
            }
        }
    }

    // ── Background detection ──────────────────────────────────────────────────

    /**
     * Returns true if [packageName] had any foreground time between [startMs] and [endMs].
     *
     * Uses [UsageStatsManager.INTERVAL_BEST] to get the finest available granularity
     * for short play segments.
     */
    private fun wasInForeground(packageName: String, startMs: Long, endMs: Long): Boolean {
        return try {
            usageStatsManager
                .queryUsageStats(UsageStatsManager.INTERVAL_BEST, startMs, endMs)
                .any { it.packageName == packageName && it.totalTimeInForeground > 0L }
        } catch (e: Exception) {
            Log.w(tag, "Could not query UsageStats for $packageName — assuming foreground", e)
            true    // conservative: don't mis-classify as background on error
        }
    }

    // ── Room flush ────────────────────────────────────────────────────────────

    /**
     * Converts accumulated ms to minutes and upserts into the `media_sessions` table.
     * No-op if the session has accumulated zero play time (avoids polluting the DB).
     */
    private fun flushSession(packageName: String) {
        val s    = sessionStates[packageName] ?: return
        if (s.accPlayMs <= 0L) return

        val date = dateFormatter.format(Date())

        trackerScope.launch {
            try {
                val entity = MediaSessionEntity(
                    packageName          = packageName,
                    date                 = date,
                    playMinutes          = (s.accPlayMs       / 60_000L).toInt(),
                    pauseMinutes         = (s.accPauseMs      / 60_000L).toInt(),
                    backgroundPlayMinutes = (s.accBackgroundMs / 60_000L).toInt(),
                )
                dao.insert(entity)  // assumed OnConflictStrategy.REPLACE
                Log.d(tag, "Flushed $packageName: play=${entity.playMinutes}m " +
                        "pause=${entity.pauseMinutes}m bg=${entity.backgroundPlayMinutes}m")
            } catch (e: Exception) {
                Log.e(tag, "Failed to flush $packageName", e)
            }
        }
    }
}
