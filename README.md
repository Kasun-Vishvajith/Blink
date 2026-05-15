# Blink
### *That's the one.*

---

## What is Blink?

Blink is a passive digital awareness app for Android. It watches how you use your phone, finds patterns in your behaviour, and tells you the truth — in plain English, every day.

No data entry. No setup. Just install, grant access once, and Blink does the rest.

---

## Why Blink Exists

The average person opens their phone 96 times a day without thinking. Most digital wellbeing apps show you a number and stop there. Blink goes further — it tells you what that number *means*, how it compares to the rest of the world, and gives you one honest second to reconsider before you open an app on autopilot.

That one second is everything.

---

## Features

### Passive Tracking
- App open and close times with exact duration
- Total daily screen time per app
- WhatsApp and VOIP call duration via microphone detection
- Spotify and YouTube background play time
- Notification count per app

### Daily Insights
- Plain English summary of your digital day
- Anomaly detection — flags unusual days automatically
- Pattern recognition — finds your consistent habits across weeks
- Streak tracking — celebrates your phone-free wins

### Blink Benchmarks
- Compare your habits against real global averages
- Sourced from published research and transparency reports
- Framed honestly — never shaming, always contextual

### Pause Overlay
- A 1–2 second breathing moment before any tracked app opens
- Shows your usage for that app today
- Backed by behavioural science research showing up to 60% reduction in mindless app opens

### Dynamic Pill
- Subtle real-time awareness at the top of your screen
- Appears when a call ends, a music session stops, or a tracked app opens
- Disappears in 3 seconds — never in the way

### Weekly Wrapped
- Swipeable story cards every week
- Your best day, worst day, top app, total hours
- One surprising insight always leads

### Soft App Limits
- Set a daily time budget per app
- Never a hard block — always your choice
- Overlay changes tone when you hit your limit

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Local Storage | Room (SQLite) |
| Background Services | WorkManager + ForegroundService |
| Charts | Vico |
| Animations | Lottie |
| Remote Benchmarks | Firebase Firestore |

---

## Permissions

Blink requires the following permissions, all granted once by the user:

| Permission | Purpose |
|---|---|
| `PACKAGE_USAGE_STATS` | App open/close tracking |
| `SYSTEM_ALERT_WINDOW` | Pause overlay and dynamic pill |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Notification count per app |
| `FOREGROUND_SERVICE` | Background tracking service |
| `INTERNET` | Fetching global benchmark data |

All data stays on your device. Blink never uploads your personal usage data anywhere.

---

## Privacy

- Zero personal data leaves your phone
- Global benchmarks are fetched as anonymous reference values only
- No accounts, no sign in, no tracking of who you are
- You can reset and delete all local data at any time from Settings

---

## Project Structure

```
com.app.blink/
├── data/          — Room entities, DAOs, database
├── service/       — UsageTracker, OverlayService, MediaTracker, InsightEngine
├── ui/            — Screens, composables, theme
├── viewmodel/     — HomeViewModel, WeeklyViewModel, AppDetailViewModel
└── util/          — Helpers, extensions
```

---

## Build Status

> Currently in active development. Phase 1 in progress.

| Phase | Description | Status |
|---|---|---|
| 1 | Core tracking + daily summary | 🔨 In progress |
| 2 | Benchmarking + insight engine | ⏳ Upcoming |
| 3 | Pause overlay + dynamic pill | ⏳ Upcoming |
| 4 | Weekly wrapped + polish | ⏳ Upcoming |

---

## The Science Behind the Pause

A 2018 behavioural study found that introducing a minimal intentional delay before opening a habitual app reduced compulsive usage by up to 60%. The pause does not block. It does not judge. It simply creates a moment of awareness between the impulse and the action.

That moment is Blink.

---

*Built with statistical rigour. Designed for real people.*