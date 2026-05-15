package com.app.awareness.service

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.app.awareness.data.AppDatabase
import com.app.awareness.data.CallSessionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Detects active VOIP/microphone call sessions for known calling apps by
 * polling [AppOpsManager] for [AppOpsManager.OPSTR_RECORD_AUDIO] activity.
 *
 * **Polling contract:**
 * - Called externally every 5 minutes (via WorkManager or a PeriodicWorkRequest).
 * - Each [poll] call compares current mic state against open (endTime=null) rows
 *   in the `call_sessions` Room table and either opens or closes sessions.
 *
 * **Detection strategy by API level:**
 * - API 29+ : [AppOpsManager.isOperationActive] — exact real-time check.
 * - API 26–28: [AppOpsManager.checkOpNoThrow] — permission-level check only
 *   (no "currently running" signal). On these versions detection is approximate:
 *   a session is "active" if the permission is allowed. Transitions are still
 *   inferred correctly once recording stops (op reverts to a non-running mode
 *   after the call ends).
 *
 * **Assumptions about [CallSessionDao] (already exists):**
 *   @Insert(onConflict = IGNORE) suspend fun insert(entity: CallSessionEntity): Long
 *   @Query("SELECT * FROM call_sessions WHERE package_name=:pkg AND date=:date AND end_time IS NULL LIMIT 1")
 *   suspend fun getOpenSession(pkg: String, date: String): CallSessionEntity?
 *   @Query("UPDATE call_sessions SET end_time=:endTime, duration_minutes=:mins WHERE id=:id")
 *   suspend fun closeSession(id: Long, endTime: Long, mins: Int)
 *
 * Module 2 — BACKEND.md
 */
class MicrophoneCallDetector(private val context: Context) {

    private val tag = "MicCallDetector"

    private val appOpsManager =
        context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

    private val dao by lazy {
        AppDatabase.getInstance(context).callSessionDao()
    }

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // ── Target packages ───────────────────────────────────────────────────────

    /**
     * VOIP apps whose microphone usage is interpreted as a call session.
     * Extend this list as needed (e.g. Signal, Viber, Teams).
     */
    private val voipPackages = listOf(
        "com.whatsapp",
        "org.telegram.messenger",
        "com.viber.voip",
        "com.skype.raider",
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Single poll cycle — intended to be called by a WorkManager
     * [PeriodicWorkRequest] every 5 minutes.
     *
     * For each tracked VOIP package:
     * - **Mic active, no open session** → insert a new [CallSessionEntity]
     *   with [CallSessionEntity.endTime] = null.
     * - **Mic inactive, open session exists** → close the session with
     *   [CallSessionEntity.endTime] and calculated [CallSessionEntity.durationMinutes].
     * - **Mic active + session open** or **Mic inactive + no session** → no-op.
     */
    suspend fun poll() {
        val today = dateFormatter.format(Date())
        val now   = System.currentTimeMillis()

        for (pkg in voipPackages) {
            try {
                pollPackage(pkg, today, now)
            } catch (e: Exception) {
                Log.e(tag, "Error polling $pkg", e)
            }
        }
    }

    // ── Per-package poll ──────────────────────────────────────────────────────

    private suspend fun pollPackage(packageName: String, today: String, now: Long) {
        val isActive   = isRecordingAudio(packageName)
        val openSession = dao.getOpenSession(packageName, today)

        when {
            // ── Session START detected ────────────────────────────────────────
            isActive && openSession == null -> {
                val entity = CallSessionEntity(
                    packageName     = packageName,
                    startTime       = now,
                    endTime         = null,
                    durationMinutes = null,
                    date            = today,
                )
                dao.insert(entity)
                Log.d(tag, "Call started: $packageName at $now")
            }

            // ── Session END detected ──────────────────────────────────────────
            !isActive && openSession != null -> {
                val durationMs      = now - openSession.startTime
                val durationMinutes = (durationMs / 60_000L).toInt()

                dao.closeSession(
                    id      = openSession.id,
                    endTime = now,
                    mins    = durationMinutes,
                )
                Log.d(tag, "Call ended: $packageName — ${durationMinutes}m")
            }

            // ── No change ─────────────────────────────────────────────────────
            else -> {
                Log.v(tag, "No transition: $packageName (active=$isActive, open=${openSession != null})")
            }
        }
    }

    // ── Microphone activity detection ─────────────────────────────────────────

    /**
     * Returns true if [packageName] is currently recording audio.
     *
     * On API 29+, uses [AppOpsManager.isOperationActive] — a real-time check.
     * On API 26–28, falls back to [AppOpsManager.checkOpNoThrow] — checks only
     * whether the permission is granted (no "currently running" signal available
     * via public API on those versions).
     *
     * Returns false if the app is not installed or any security exception occurs.
     */
    @Suppress("DEPRECATION")
    private fun isRecordingAudio(packageName: String): Boolean {
        return try {
            val uid = context.packageManager
                .getApplicationInfo(packageName, 0).uid

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                isOperationActiveCompat(packageName, uid)
            } else {
                // API 26–28: best-effort — checkOpNoThrow returns MODE_ALLOWED
                // while the app has (and is using) the permission.
                appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_RECORD_AUDIO, uid, packageName
                ) == AppOpsManager.MODE_ALLOWED
            }
        } catch (e: Exception) {
            // App not installed or UID resolution failed — treat as inactive
            Log.d(tag, "$packageName not installed or inaccessible: ${e.message}")
            false
        }
    }

    /**
     * Real-time check — only compiled/called on API 29+.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun isOperationActiveCompat(packageName: String, uid: Int): Boolean {
        return appOpsManager.isOperationActive(
            AppOpsManager.OPSTR_RECORD_AUDIO,
            uid,
            packageName,
        )
    }
}
