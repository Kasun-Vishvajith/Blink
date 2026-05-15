package com.app.awareness.data

/**
 * Immutable snapshot of global benchmark values fetched from Firebase Firestore
 * at path `benchmarks/global_averages`.
 *
 * All numeric fields represent world-average daily values.
 * Populated by [com.app.awareness.service.BenchmarkFetcher].
 *
 * Module 5 — BACKEND.md
 */
data class BenchmarkData(
    /** World average total daily screen time in minutes (e.g. 397) */
    val dailyScreenTimeMinutes: Int,

    /** World average number of times Instagram is opened per day (e.g. 23) */
    val instagramOpensDaily: Int,

    /** World average Spotify listening time per day in minutes (e.g. 82) */
    val spotifyMinutesDaily: Int,

    /** World average YouTube watch time per day in minutes (e.g. 95) */
    val youtubeMinutesDaily: Int,

    /** World average WhatsApp call duration per day in minutes (e.g. 18) */
    val whatsappCallMinutesDaily: Int,

    /** World average number of notifications received per day (e.g. 96) */
    val notificationsDaily: Int,

    /** Month string when the benchmark document was last updated (e.g. "2026-05") */
    val lastUpdated: String,
) {
    companion object {
        /**
         * Hard-coded fallback used when both the Firestore fetch and the
         * SharedPreferences cache are unavailable (first launch, no internet,
         * no cached data).
         *
         * Values mirror the example document in BACKEND.md Module 5.
         */
        val DEFAULT = BenchmarkData(
            dailyScreenTimeMinutes   = 397,
            instagramOpensDaily      = 23,
            spotifyMinutesDaily      = 82,
            youtubeMinutesDaily      = 95,
            whatsappCallMinutesDaily = 18,
            notificationsDaily       = 96,
            lastUpdated              = "2026-05",
        )
    }
}
