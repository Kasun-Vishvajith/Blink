# AGENT PROMPTING GUIDE

## How to Use This System

You have two reference files:
- `BACKEND.md` — all data, services, logic, Room DB, Firebase
- `FRONTEND.md` — all screens, composables, design system, ViewModels

Each prompt below targets ONE module only.
Attach only the relevant MD file to save tokens.
Do not ask the agent to build multiple modules in one session.

---

## GOLDEN RULES FOR EVERY PROMPT

- Always say: "Output only the file asked. No explanations."
- Always say: "Use the design system defined in FRONTEND.md exactly."
- Always say: "Do not create files not listed in this prompt."
- Always end with: "If anything is unclear, make a reasonable assumption and continue."

---

## PROMPT 1 — Project Setup

**Attach:** Neither file needed.

```
Create a new Android project with the following setup:
- Language: Kotlin
- UI: Jetpack Compose
- Min SDK: 26
- Package name: com.app.awareness

Add these dependencies to build.gradle:
- Room (latest stable)
- WorkManager (latest stable)
- Vico compose charts
- Lottie compose
- Firebase Firestore
- Compose Navigation
- ViewModel + StateFlow

Create the base package structure:
com.app.awareness/
├── data/        (Room DB, DAOs, entities)
├── service/     (background services)
├── ui/          (screens, composables)
├── viewmodel/   (ViewModels)
└── util/        (helpers)

Output only build.gradle and the empty package folders.
Do not create any screens yet.
If anything is unclear, make a reasonable assumption and continue.
```

---

## PROMPT 2 — Design System + Theme

**Attach:** `FRONTEND.md`

```
Read FRONTEND.md section "Design System" only.

Create these files:
- ui/theme/Color.kt — all color constants defined in the Design System section
- ui/theme/Type.kt — all typography styles defined in the Design System section
- ui/theme/Theme.kt — MaterialTheme dark theme using these colors and fonts

Use DM Sans from Google Fonts via Compose.
Output only these three files.
No screens, no composables, no other files.
If anything is unclear, make a reasonable assumption and continue.
```

---

## PROMPT 3 — Room Database Setup

**Attach:** `BACKEND.md`

```
Read BACKEND.md sections: Module 1, 2, 3, 4, 7 table definitions only.

Create these files:
- data/AppUsageEntity.kt — Room entity for table app_usage
- data/CallSessionEntity.kt — Room entity for table call_sessions
- data/MediaSessionEntity.kt — Room entity for table media_sessions
- data/NotificationEntity.kt — Room entity for table notifications
- data/DailySummaryEntity.kt — Room entity for table daily_summary
- data/AppDatabase.kt — Room database class connecting all entities
- data/AppUsageDao.kt — DAO with insert, queryByDate, queryByDateRange, getTopApps
- data/DailySummaryDao.kt — DAO with insert, getByDate, getLast7Days

Output only these files.
No services, no UI, no ViewModels.
If anything is unclear, make a reasonable assumption and continue.
```

---

## PROMPT 4 — UsageTracker Service

**Attach:** `BACKEND.md`

```
Read BACKEND.md Module 1 only.

Create these files:
- service/UsageTracker.kt
  - Query UsageStatsManager for all apps
  - Calculate open_count and total_minutes per app per day
  - Insert results into Room table app_usage via AppUsageDao
  - Assume AppUsageDao and AppUsageEntity already exist

- service/UsageWorker.kt
  - WorkManager PeriodicWorkRequest wrapper for UsageTracker
  - Runs every 15 minutes

Output only these two files.
No UI, no other services.
If anything is unclear, make a reasonable assumption and continue.
```

---

## PROMPT 5 — Notification Tracker

**Attach:** `BACKEND.md`

```
Read BACKEND.md Module 4 only.

Create this file:
- service/NotificationListenerServiceImpl.kt
  - Extends NotificationListenerService
  - On onNotificationPosted → insert or increment count in Room notifications table
  - Assume NotificationEntity and NotificationDao already exist

Also add to AndroidManifest.xml the required service declaration with BIND_NOTIFICATION_LISTENER_SERVICE.
Show the manifest addition as a comment block at the top of the file.

Output only this one file.
If anything is unclear, make a reasonable assumption and continue.
```

---

## PROMPT 6 — Media Session Tracker

**Attach:** `BACKEND.md`

```
Read BACKEND.md Module 3 only.

Create this file:
- service/MediaSessionTracker.kt
  - Use MediaSessionManager to get active sessions
  - Register MediaController.Callback for each session
  - Track play vs pause state changes
  - Detect background play by cross referencing with UsageStatsManager
  - Insert results into Room table media_sessions via MediaSessionDao
  - Assume MediaSessionEntity and MediaSessionDao already exist

Output only this one file.
No UI, no other services.
If anything is unclear, make a reasonable assumption and continue.
```

---

## PROMPT 7 — Microphone Call Detector

**Attach:** `BACKEND.md`

```
Read BACKEND.md Module 2 only.

Create this file:
- service/MicrophoneCallDetector.kt
  - Use AppOpsManager to query OPSTR_RECORD_AUDIO per package
  - Detect if WhatsApp or Telegram is actively using microphone
  - Calculate session duration on microphone release
  - Insert into Room call_sessions table via CallSessionDao
  - Assume CallSessionEntity and CallSessionDao already exist

Output only this one file.
No UI, no other services.
If anything is unclear, make a reasonable assumption and continue.
```

---

## PROMPT 8 — Benchmark Fetcher

**Attach:** `BACKEND.md`

```
Read BACKEND.md Module 5 only.

Create this file:
- service/BenchmarkFetcher.kt
  - Fetch document from Firebase Firestore path: benchmarks/global_averages
  - Parse all fields defined in Module 5
  - Cache result in SharedPreferences
  - Return cached value if fetch fails
  - Expose a suspend function: fetchBenchmarks(): BenchmarkData

Also create:
- data/BenchmarkData.kt — data class with all fields from Module 5 document structure

Output only these two files.
If anything is unclear, make a reasonable assumption and continue.
```

---

## PROMPT 9 — Insight Engine

**Attach:** `BACKEND.md`

```
Read BACKEND.md Module 6 only.

Create these files:
- data/Insight.kt — data class defined in Module 6
- service/InsightEngine.kt
  - Takes daily aggregated Room data + BenchmarkData as input
  - Implements all 4 insight types: Comparative, Anomaly, Pattern, Streak
  - Returns List<Insight>
  - Use exact message templates from Module 6 as base, parameterize the values
  - Assume all Room DAOs and BenchmarkData already exist

Output only these two files.
No UI, no other services.
If anything is unclear, make a reasonable assumption and continue.
```

---

## PROMPT 10 — Home Screen UI

**Attach:** `FRONTEND.md`

```
Read FRONTEND.md Screen 2 (Home) and Design System sections only.

Create these files:
- ui/HomeScreen.kt — full screen as described in Screen 2
  - Use mock/hardcoded data for now (no ViewModel connection yet)
  - Implement all 5 sections: Header, Big Number, Insight Cards, App Breakdown, Benchmark Row
  - Use exact colors and typography from Design System

- ui/InsightCard.kt — composable as described in Composable — InsightCard section

Output only these two files.
No ViewModels, no navigation, no other screens.
If anything is unclear, make a reasonable assumption and continue.
```

---

## PROMPT 11 — Weekly Screen UI

**Attach:** `FRONTEND.md`

```
Read FRONTEND.md Screen 3 (Weekly Wrapped) and Design System sections only.

Create this file:
- ui/WeeklyScreen.kt
  - Implement all 7 cards defined in Screen 3
  - Use mock/hardcoded data for now
  - Full screen vertical scroll
  - Each card full width, 200dp height, rounded 24dp
  - Use exact colors and typography from Design System

Output only this one file.
No ViewModels, no navigation connections.
If anything is unclear, make a reasonable assumption and continue.
```

---

## PROMPT 12 — App Detail Sheet

**Attach:** `FRONTEND.md`

```
Read FRONTEND.md Screen 4 (App Detail Sheet) and Design System sections only.

Create this file:
- ui/AppDetailSheet.kt
  - Bottom sheet modal composable
  - Today/Week/Month toggle using SegmentedButton
  - Bar chart using Vico BarChart composable
  - Open count and average session length stats
  - Benchmark comparison row
  - Time limit slider (0 to 180 minutes)
  - Use mock data for now

Output only this one file.
If anything is unclear, make a reasonable assumption and continue.
```

---

## PROMPT 13 — Pause Overlay

**Attach:** `FRONTEND.md`

```
Read FRONTEND.md Composable — Pause Overlay section and Design System only.

Create these files:
- service/OverlayService.kt
  - Foreground service
  - Monitors foreground app every 1 second using UsageStatsManager
  - On app change: calls PauseOverlayManager to show overlay
  - Auto dismiss after delay from SharedPreferences setting

- ui/PauseOverlay.kt
  - Rendered via WindowManager
  - Full screen black background
  - Expanding circle animation using Canvas
  - App name and today's usage text
  - Auto dismisses — no button
  - Fade out animation on dismiss

Output only these two files.
No navigation, no screen connections.
If anything is unclear, make a reasonable assumption and continue.
```

---

## PROMPT 14 — Dynamic Pill

**Attach:** `FRONTEND.md`

```
Read FRONTEND.md Composable — Dynamic Pill section and Design System only.

Create these files:
- ui/DynamicPill.kt
  - Rendered via WindowManager
  - Top-center position
  - Pill shape layout as described
  - Spring enter animation, fade exit after 3 seconds
  - Accepts: message string and sentiment string as parameters

- service/PillNotificationService.kt
  - Triggers DynamicPill on: app open, call end, music session end
  - Reads data from Room to build message string

Output only these two files.
If anything is unclear, make a reasonable assumption and continue.
```

---

## PROMPT 15 — ViewModels + Navigation

**Attach:** `FRONTEND.md`

```
Read FRONTEND.md ViewModel Structure and Navigation Structure sections only.

Create these files:
- viewmodel/HomeViewModel.kt — all StateFlows defined in ViewModel Structure, connected to Room DAOs
- viewmodel/WeeklyViewModel.kt — all StateFlows defined, connected to Room DAOs
- viewmodel/AppDetailViewModel.kt — all StateFlows + setLimit function
- ui/Navigation.kt — NavHost with all routes defined in Navigation Structure section

Connect HomeScreen, WeeklyScreen, AppDetailSheet, SettingsScreen to their ViewModels.
Replace all mock data with real StateFlow collection.

Output only these four files.
If anything is unclear, make a reasonable assumption and continue.
```

---

## PROMPT 16 — Settings Screen

**Attach:** `FRONTEND.md`

```
Read FRONTEND.md Screen 5 (Settings) and Design System sections only.

Create this file:
- ui/SettingsScreen.kt
  - All settings items defined in Screen 5
  - Overlay delay SegmentedButton (1 sec / 2 sec / Off)
  - Show dynamic pill Toggle
  - Weekly report day dropdown
  - Permissions status list with indicators
  - Reset all data button with confirmation AlertDialog
  - Save all values to SharedPreferences

Output only this one file.
If anything is unclear, make a reasonable assumption and continue.
```

---

## PROMPT 17 — Onboarding Screen

**Attach:** `FRONTEND.md`

```
Read FRONTEND.md Screen 1 (Onboarding) and Design System sections only.

Create this file:
- ui/OnboardingScreen.kt
  - 3 swipeable pages using HorizontalPager
  - Pages as defined in Screen 1
  - Permission request flow on page 3
  - Requests: PACKAGE_USAGE_STATS, SYSTEM_ALERT_WINDOW, BIND_NOTIFICATION_LISTENER_SERVICE
  - On all permissions granted → navigate to HomeScreen
  - Save completion flag to SharedPreferences so it never shows again

Output only this one file.
If anything is unclear, make a reasonable assumption and continue.
```

---

## BUILD ORDER

Follow prompts in this exact sequence:

```
1  → Project Setup
2  → Design System
3  → Room Database
4  → UsageTracker
5  → NotificationTracker
6  → MediaSessionTracker
7  → MicrophoneCallDetector
8  → BenchmarkFetcher
9  → InsightEngine
10 → HomeScreen UI
11 → WeeklyScreen UI
12 → AppDetailSheet UI
13 → PauseOverlay
14 → DynamicPill
15 → ViewModels + Navigation
16 → SettingsScreen
17 → OnboardingScreen
```

Test each prompt output compiles before moving to the next.
