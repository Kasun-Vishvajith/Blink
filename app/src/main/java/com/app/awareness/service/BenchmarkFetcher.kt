package com.app.awareness.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.app.awareness.data.BenchmarkData
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Fetches global benchmark averages from Firebase Firestore once per day,
 * caches the result in [SharedPreferences], and returns the cached values
 * on subsequent calls or if the fetch fails.
 *
 * **Firestore path:** `benchmarks/global_averages`
 *
 * **Priority chain:**
 * 1. Live Firestore fetch — if successful, update cache and return.
 * 2. SharedPreferences cache — if fetch fails but cache exists, return cache.
 * 3. [BenchmarkData.DEFAULT] — hard-coded fallback (first launch, no network, no cache).
 *
 * **Usage:** Call [fetchBenchmarks] from a WorkManager Worker scheduled once per day.
 * The cache lives in SharedPreferences so subsequent reads are instant and offline-safe.
 *
 * Module 5 — BACKEND.md
 */
class BenchmarkFetcher(private val context: Context) {

    private val tag = "BenchmarkFetcher"

    // ── Firestore ─────────────────────────────────────────────────────────────

    private val firestore = Firebase.firestore

    private companion object {
        const val COLLECTION = "benchmarks"
        const val DOCUMENT   = "global_averages"

        // SharedPreferences keys
        const val PREFS_NAME                    = "benchmark_cache"
        const val KEY_DAILY_SCREEN_TIME         = "daily_screen_time_minutes"
        const val KEY_INSTAGRAM_OPENS           = "instagram_opens_daily"
        const val KEY_SPOTIFY_MINUTES           = "spotify_minutes_daily"
        const val KEY_YOUTUBE_MINUTES           = "youtube_minutes_daily"
        const val KEY_WHATSAPP_CALL_MINUTES     = "whatsapp_call_minutes_daily"
        const val KEY_NOTIFICATIONS_DAILY       = "notifications_daily"
        const val KEY_LAST_UPDATED              = "last_updated"
        const val KEY_CACHE_EXISTS              = "cache_exists"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Fetches the latest benchmark document from Firestore.
     * Falls back to [SharedPreferences] cache or [BenchmarkData.DEFAULT] on failure.
     *
     * Suspend — run from a coroutine or WorkManager's [doWork].
     */
    suspend fun fetchBenchmarks(): BenchmarkData {
        return try {
            val snapshot = firestore
                .collection(COLLECTION)
                .document(DOCUMENT)
                .get()
                .await()

            val fresh = parseSnapshot(snapshot)
            if (fresh != null) {
                saveToCache(fresh)
                Log.d(tag, "Fetched fresh benchmarks (updated: ${fresh.lastUpdated})")
                fresh
            } else {
                Log.w(tag, "Firestore document empty or malformed — using cache")
                getCached()
            }
        } catch (e: Exception) {
            Log.e(tag, "Firestore fetch failed — falling back to cache", e)
            getCached()
        }
    }

    // ── Parsing ───────────────────────────────────────────────────────────────

    /**
     * Converts a [DocumentSnapshot] into a [BenchmarkData].
     * Returns null if any required numeric field is missing.
     */
    private fun parseSnapshot(doc: DocumentSnapshot): BenchmarkData? {
        if (!doc.exists()) return null

        return try {
            BenchmarkData(
                dailyScreenTimeMinutes   = doc.getLong(KEY_DAILY_SCREEN_TIME)?.toInt()   ?: return null,
                instagramOpensDaily      = doc.getLong(KEY_INSTAGRAM_OPENS)?.toInt()     ?: return null,
                spotifyMinutesDaily      = doc.getLong(KEY_SPOTIFY_MINUTES)?.toInt()     ?: return null,
                youtubeMinutesDaily      = doc.getLong(KEY_YOUTUBE_MINUTES)?.toInt()     ?: return null,
                whatsappCallMinutesDaily = doc.getLong(KEY_WHATSAPP_CALL_MINUTES)?.toInt() ?: return null,
                notificationsDaily       = doc.getLong(KEY_NOTIFICATIONS_DAILY)?.toInt() ?: return null,
                lastUpdated              = doc.getString(KEY_LAST_UPDATED) ?: "unknown",
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse Firestore document", e)
            null
        }
    }

    // ── SharedPreferences cache ───────────────────────────────────────────────

    /**
     * Returns the cached [BenchmarkData] from SharedPreferences, or
     * [BenchmarkData.DEFAULT] if no cache has been written yet.
     */
    private fun getCached(): BenchmarkData {
        if (!prefs.getBoolean(KEY_CACHE_EXISTS, false)) {
            Log.d(tag, "No cache found — returning DEFAULT benchmarks")
            return BenchmarkData.DEFAULT
        }

        return BenchmarkData(
            dailyScreenTimeMinutes   = prefs.getInt(KEY_DAILY_SCREEN_TIME,     BenchmarkData.DEFAULT.dailyScreenTimeMinutes),
            instagramOpensDaily      = prefs.getInt(KEY_INSTAGRAM_OPENS,       BenchmarkData.DEFAULT.instagramOpensDaily),
            spotifyMinutesDaily      = prefs.getInt(KEY_SPOTIFY_MINUTES,       BenchmarkData.DEFAULT.spotifyMinutesDaily),
            youtubeMinutesDaily      = prefs.getInt(KEY_YOUTUBE_MINUTES,       BenchmarkData.DEFAULT.youtubeMinutesDaily),
            whatsappCallMinutesDaily = prefs.getInt(KEY_WHATSAPP_CALL_MINUTES, BenchmarkData.DEFAULT.whatsappCallMinutesDaily),
            notificationsDaily       = prefs.getInt(KEY_NOTIFICATIONS_DAILY,   BenchmarkData.DEFAULT.notificationsDaily),
            lastUpdated              = prefs.getString(KEY_LAST_UPDATED,       BenchmarkData.DEFAULT.lastUpdated) ?: BenchmarkData.DEFAULT.lastUpdated,
        ).also {
            Log.d(tag, "Returning cached benchmarks (updated: ${it.lastUpdated})")
        }
    }

    /**
     * Persists a [BenchmarkData] snapshot to SharedPreferences atomically.
     */
    private fun saveToCache(data: BenchmarkData) {
        prefs.edit().apply {
            putInt(KEY_DAILY_SCREEN_TIME,     data.dailyScreenTimeMinutes)
            putInt(KEY_INSTAGRAM_OPENS,       data.instagramOpensDaily)
            putInt(KEY_SPOTIFY_MINUTES,       data.spotifyMinutesDaily)
            putInt(KEY_YOUTUBE_MINUTES,       data.youtubeMinutesDaily)
            putInt(KEY_WHATSAPP_CALL_MINUTES, data.whatsappCallMinutesDaily)
            putInt(KEY_NOTIFICATIONS_DAILY,   data.notificationsDaily)
            putString(KEY_LAST_UPDATED,       data.lastUpdated)
            putBoolean(KEY_CACHE_EXISTS, true)
            apply()
        }
        Log.d(tag, "Cache updated")
    }
}
