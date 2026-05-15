# BACKEND — Digital Awareness App

## Stack
- Language: Kotlin (Android Services)
- Local DB: Room (SQLite)
- Remote: Firebase Firestore (free tier)
- Background: Foreground Service + WorkManager

---

## Permissions Required (Declare in AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" tools:ignore="ProtectedPermissions"/>
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"/>
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
```

---

## Module 1 — UsageTracker

**File:** `UsageTracker.kt`

**Purpose:** Collect app open/close times and duration using UsageStatsManager.

**Key API:** `UsageStatsManager.queryUsageStats()`

**Logic:**
- Query every 15 minutes via WorkManager
- Store per-app: package name, open timestamp, close timestamp, duration
- Aggregate daily totals per app
- Store in Room: table `app_usage`

**Room Table: app_usage**
```
id | package_name | date | total_minutes | open_count | last_opened
```

**Task schedule:**
```
WorkManager — PeriodicWorkRequest every 15 min
```

---

## Module 2 — MicrophoneCallDetector

**File:** `MicrophoneCallDetector.kt`

**Purpose:** Detect WhatsApp/Telegram/any VOIP call duration using microphone access logs.

**Key API:** `AppOpsManager.getPackagesForOps(OPSTR_RECORD_AUDIO)`

**Logic:**
- Poll every 5 minutes
- If WhatsApp + microphone active → mark as call session
- On microphone release → calculate duration
- Store in Room: table `call_sessions`

**Room Table: call_sessions**
```
id | package_name | start_time | end_time | duration_minutes | date
```

---

## Module 3 — MediaSessionTracker

**File:** `MediaSessionTracker.kt`

**Purpose:** Track Spotify, YouTube, and any media app background play time.

**Key API:** `MediaSessionManager.getActiveSessions()`

**Logic:**
- Register `MediaController.Callback` for each active session
- On `onPlaybackStateChanged` → track play/pause events
- Calculate net playing time (exclude paused time)
- Store in Room: table `media_sessions`

**Room Table: media_sessions**
```
id | package_name | date | play_minutes | pause_minutes | background_play_minutes
```

**Background detection:**
- Cross-reference with UsageStats
- If media playing + app NOT in foreground → mark as background play

---

## Module 4 — NotificationTracker

**File:** `NotificationListenerServiceImpl.kt`

**Purpose:** Count notifications received per app.

**Key API:** Extend `NotificationListenerService`

**Logic:**
- On `onNotificationPosted` → increment count for package
- Store in Room: table `notifications`

**Room Table: notifications**
```
id | package_name | date | count
```

---

## Module 5 — BenchmarkFetcher

**File:** `BenchmarkFetcher.kt`

**Purpose:** Fetch global average usage stats from Firebase once per day.

**Firebase Document Path:** `benchmarks/global_averages`

**Document Structure:**
```json
{
  "daily_screen_time_minutes": 397,
  "instagram_opens_daily": 23,
  "spotify_minutes_daily": 82,
  "youtube_minutes_daily": 95,
  "whatsapp_call_minutes_daily": 18,
  "notifications_daily": 96,
  "last_updated": "2026-05"
}
```

**Logic:**
- Fetch once per day via WorkManager
- Cache locally in SharedPreferences
- Use cached value if fetch fails

---

## Module 6 — InsightEngine

**File:** `InsightEngine.kt`

**Purpose:** Generate plain English insight strings from raw data. This is the statistical brain.

**Input:** Daily aggregated data from Room DB + benchmark values

**Insight Types:**

### Comparative Insight
```
if user.instagram_opens < benchmark.instagram_opens * 0.5:
    → "You open Instagram {user} times a day. Most people open it {benchmark} times. You're remarkably intentional."
```

### Anomaly Insight
```
if today.screen_time > user_7day_average * 1.5:
    → "Today was unusual — your screen time was 50% higher than your weekly average."
```

### Pattern Insight
```
if monday.screen_time consistently > other_days * 1.2:
    → "Your Mondays are consistently your highest screen time day."
```

### Streak Insight
```
if last_5_days.screen_time < benchmark.daily_screen_time:
    → "5 days in a row under the world average. That's rare."
```

**Output:** List of `Insight` data class objects
```kotlin
data class Insight(
    val type: String,       // "comparative" | "anomaly" | "pattern" | "streak"
    val message: String,    // Plain English text shown to user
    val metric: String,     // Which metric this is about
    val sentiment: String   // "positive" | "neutral" | "warning"
)
```

---

## Module 7 — DailyAggregator

**File:** `DailyAggregator.kt`

**Purpose:** Compile end-of-day summary from all tables.

**Runs:** Every night at 23:50 via WorkManager

**Output stored in Room: table `daily_summary`**
```
date | total_screen_minutes | top_app | top_app_minutes |
music_minutes | call_minutes | notification_count |
screen_time_vs_benchmark_percent
```

---

## Module 8 — OverlayService

**File:** `OverlayService.kt`

**Purpose:** Show pause overlay when a tracked app is opened.

**Key API:** `WindowManager` + `SYSTEM_ALERT_WINDOW`

**Logic:**
- Monitor foreground app every 1 second via UsageStatsManager
- On app change detected → check if new app is in user's tracked list
- If yes → show overlay window for configured delay (1 or 2 seconds)
- Overlay auto-dismisses → app becomes usable

**Overlay data shown:**
- App name
- Today's usage so far for that app
- Breathing animation (handled in frontend)

**Runs as:** Persistent ForegroundService with a silent notification

---

## Module 9 — PillNotificationService

**File:** `PillNotificationService.kt`

**Purpose:** Show dynamic island-style pill at top of screen on key events.

**Triggers:**
- App opened → show today's time for that app
- Call ended → show call duration
- Music session ends → show total music time today

**Display:** Small `WindowManager` overlay, top-center, pill shape, 3 second auto-dismiss

---

## Data Flow Summary

```
Phone usage happens
      ↓
UsageTracker + MicrophoneDetector + MediaSessionTracker + NotificationTracker
      ↓
Room Database (local, private)
      ↓
DailyAggregator (nightly)
      ↓
InsightEngine (generates insight strings)
      ↓
UI Layer reads from Room and displays
      ↑
BenchmarkFetcher (Firebase, once daily) feeds InsightEngine
```

---

## Implementation Order

1. `UsageTracker` — foundation of everything
2. `Room DB setup` — all tables
3. `DailyAggregator` — compile daily data
4. `BenchmarkFetcher` — Firebase connection
5. `InsightEngine` — insight generation logic
6. `NotificationTracker` — NotificationListenerService
7. `MicrophoneCallDetector` — call detection
8. `MediaSessionTracker` — music/video tracking
9. `OverlayService` — pause overlay
10. `PillNotificationService` — dynamic pill
