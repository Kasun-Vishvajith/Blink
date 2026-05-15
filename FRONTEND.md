# FRONTEND — Digital Awareness App

## Stack
- Language: Kotlin
- UI Framework: Jetpack Compose
- Navigation: Compose Navigation
- Charts: Vico (compose-charts)
- Animations: Lottie for Compose
- State: ViewModel + StateFlow
- Theme: Material3 with custom dark theme

---

## Design System

### Colors
```kotlin
Background     = #000000   // True black — OLED optimized
Surface        = #111111   // Cards, panels
SurfaceVariant = #1A1A1A   // Elevated cards
Accent         = #C8FF00   // Sharp lime — single accent color
TextPrimary    = #FFFFFF
TextSecondary  = #888888
TextMuted      = #444444
Positive       = #00E5A0   // Green for good stats
Warning        = #FF6B35   // Orange for high usage
```

### Typography
```kotlin
Font: "DM Sans" (Google Fonts)
Display  — 48sp, Weight 300 (thin) — used for big numbers
Title    — 22sp, Weight 600
Body     — 15sp, Weight 400
Caption  — 12sp, Weight 400, TextSecondary color
Label    — 11sp, Weight 500, uppercase, letter spacing 0.1em
```

### Spacing System
```
4dp base unit
Padding small  = 8dp
Padding medium = 16dp
Padding large  = 24dp
Card radius    = 20dp
Pill radius    = 100dp (full round)
```

---

## Screen 1 — Onboarding

**File:** `OnboardingScreen.kt`

**Pages:** 3 swipeable cards

**Page 1:**
- Title: "Your phone knows you."
- Subtitle: "We just tell you what it knows."
- Animation: Lottie — phone with subtle pulse

**Page 2:**
- Title: "No judgement."
- Subtitle: "Just honest numbers and what they mean."

**Page 3:**
- Permission grant screen
- List of permissions with plain explanations
- Single CTA button: "Give access — it stays on your phone"

**Logic:**
- After permissions granted → navigate to HomeScreen
- Never show again after first completion

---

## Screen 2 — Home (Daily Summary)

**File:** `HomeScreen.kt`

**Layout:** Single scroll, no tabs, no bottom nav

### Section A — Header
- Current time (large, thin weight)
- Date below in TextSecondary
- Greeting line: *"Good morning"* or *"Still up?"* based on time

### Section B — Today's Big Number
- Giant display number: total screen time in hours
- Below it: comparison line in Accent color
  - *"37 min less than yesterday"*
  - or *"Your lightest Tuesday in 3 weeks"*

### Section C — Insight Cards (Horizontal scroll)
- Each card: `InsightCard.kt` composable
- Card shows: icon + one sentence insight
- Color tint based on sentiment (positive=green tint, warning=orange tint)
- Minimum 2 cards, maximum 5 per day
- Swipe horizontally

**InsightCard composable props:**
```kotlin
data class InsightCardData(
    val message: String,
    val sentiment: String,  // "positive" | "neutral" | "warning"
    val metric: String
)
```

### Section D — App Breakdown
- Title: "Where your time went"
- List of top 5 apps today
- Each row: App icon + name + bar (proportional) + time label
- Bar uses Accent color, proportional to usage
- Tap any app → AppDetailSheet

### Section E — Benchmark Row
- Title: "You vs the world"
- 2-3 horizontal comparison pills
- Each pill: metric name + your value + world average
- Color coded: green if you're better, neutral if similar

---

## Screen 3 — Weekly Wrapped

**File:** `WeeklyScreen.kt`

**Layout:** Full screen vertical scroll of story cards

**Each card:** Full width, 200dp height, rounded 24dp

**Card 1 — Opening**
```
"This week in numbers"
[Week date range]
```

**Card 2 — Screen Time**
```
Big number: total weekly hours
"You spent X hours on your phone this week"
Subline: vs previous week delta
```

**Card 3 — Top App**
```
App icon (large)
"[AppName] was your most opened app"
"X times, X hours total"
```

**Card 4 — Best Day**
```
Day name large
"[Day] was your most phone-free day"
"Only X hours — your personal best this week"
```

**Card 5 — Calls**
```
Phone icon
"You spent ~X minutes on calls"
App breakdown if multiple VOIP apps used
```

**Card 6 — Music**
```
Waveform icon (Lottie)
"Spotify played for X hours"
"Mostly while you were doing other things" (if background play > 60%)
```

**Card 7 — Closing Benchmark**
```
"Compared to the world..."
3 comparison lines
Sentiment line at bottom
```

---

## Screen 4 — App Detail Sheet

**File:** `AppDetailSheet.kt`

**Trigger:** Tap any app in Home breakdown list

**Type:** Bottom sheet (modal)

**Content:**
- App icon + name header
- Today / This Week / This Month toggle
- Bar chart: daily usage for selected period (Vico BarChart)
- Open count stat
- Average session length stat
- Benchmark comparison if available
- Time limit setter: slider from 0 to 180 minutes
  - 0 = no limit
  - Any value = soft overlay limit enabled

---

## Screen 5 — Settings

**File:** `SettingsScreen.kt`

**Minimal list:**

- Overlay delay: 1 sec / 2 sec / Off (SegmentedButton)
- Show dynamic pill: Toggle
- Weekly report day: dropdown (default Sunday)
- Permissions status: each permission with green/red indicator
- Reset all data: destructive button (confirmation dialog)

---

## Composable — Pause Overlay

**File:** `PauseOverlay.kt`

**Type:** Rendered via WindowManager (not Compose Navigation)

**Layout:**
- Full screen, black background #000000, alpha 0.95
- Center: expanding/contracting circle animation (Lottie or Canvas)
- Below circle: App name in Title style
- Below name: *"X minutes today"* in TextSecondary
- Auto dismiss after configured delay — no button

**Animation:**
- Circle expands slowly from 60dp to 120dp over 1 second
- Simultaneously fades in text
- On dismiss: entire overlay fades out in 200ms

---

## Composable — Dynamic Pill

**File:** `DynamicPill.kt`

**Type:** Rendered via WindowManager, top-center position

**Layout:**
- Pill shape: height 36dp, width wrap_content, max 280dp
- Background: #1A1A1A
- Left: small colored dot (green=positive, orange=warning)
- Text: single line, 13sp
- Border: 0.5dp stroke #333333

**Animations:**
- Enter: scale from 0.3 to 1.0 + fade in, 250ms spring
- Exit: scale to 0.3 + fade out, 200ms, after 3 seconds

---

## Composable — InsightCard

**File:** `InsightCard.kt`

**Size:** 280dp wide, 120dp tall

**Layout:**
- Background: SurfaceVariant (#1A1A1A)
- Radius: 20dp
- Left color bar: 4dp wide, full height, color by sentiment
- Padding: 16dp
- Top: metric label (Caption style, uppercase)
- Middle: insight message (Body style)
- Bottom-right: subtle icon

---

## Navigation Structure

```
NavHost
├── OnboardingScreen   (shown once)
├── HomeScreen         (default destination)
├── WeeklyScreen
├── SettingsScreen
└── AppDetailSheet     (bottom sheet overlay)
```

No bottom navigation bar. 
Home → Weekly via swipe up gesture.
Home → Settings via gear icon top-right.
Any screen → back via system gesture.

---

## ViewModel Structure

**HomeViewModel.kt**
```kotlin
// StateFlows exposed to UI:
val todayScreenTime: StateFlow<Int>        // minutes
val topApps: StateFlow<List<AppUsageStat>>
val insights: StateFlow<List<InsightCardData>>
val benchmarkComparisons: StateFlow<List<BenchmarkStat>>
val yesterdayDelta: StateFlow<Int>         // minutes diff
```

**WeeklyViewModel.kt**
```kotlin
val weeklyCards: StateFlow<List<WeeklyCard>>
val weekTotalMinutes: StateFlow<Int>
val bestDay: StateFlow<String>
val topApp: StateFlow<AppUsageStat>
```

**AppDetailViewModel.kt**
```kotlin
val selectedApp: StateFlow<String>
val dailyUsage: StateFlow<List<DailyUsageStat>>
val currentLimit: StateFlow<Int>
fun setLimit(minutes: Int)
```

---

## Implementation Order

1. Design system setup (colors, typography, theme)
2. OnboardingScreen + permission flow
3. HomeScreen layout (static/mock data first)
4. InsightCard composable
5. AppDetailSheet
6. WeeklyScreen with card layout
7. SettingsScreen
8. Connect ViewModels to Room DB
9. PauseOverlay (WindowManager)
10. DynamicPill (WindowManager)
11. Polish animations
