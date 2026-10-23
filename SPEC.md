# SPEC — personalDashboard

*v1, 2026-10-22, from `PRD.md` D1–D22.* Normative. It describes what v1 does. A ticket that
ships behaviour different from this amends the section (*amended YYYY-MM-DD*) with a D-entry.
If this disagrees with `DECISIONS.md` on a detail, this file wins.

---

## 1. What it is

A personal Android dashboard for the owner and a few people, sideloaded (D4). One screen shows
four cards in a fixed order (D18): **Calendar**, **Habits**, **To-dos**, **Steps**. One
home-screen widget summarises them (D19). All data comes from the phone itself (D3).

## 2. Platform

- **minSdk 28**, compileSdk/targetSdk **37** (pinned in T0, PRD A5; *amended 2026-10-22*). Kotlin, Jetpack Compose,
  Gradle Kotlin DSL, version catalog `gradle/libs.versions.toml`.
- **Modules** (PRD A2):
  - `:core`: Kotlin/JVM. Domain types, the reducer, and every rule in §4, plus the pure halves
    of §5. Only `java.time` and `kotlinx.coroutines` are allowed. No Android.
  - `:app`: Android application. Activity, ViewModels, Room, DataStore, the CalendarContract and
    Health Connect readers, the Glance widget and the Worker. Plumbing only (D10).
- **Manifest:**
  - Permissions: `READ_CALENDAR` and `android.permission.health.READ_STEPS`. **No
    `INTERNET`**, no `POST_NOTIFICATIONS`, no `WRITE_CALENDAR`, no alarm permissions (D3, D20,
    D21).
  - `android:allowBackup="false"` (D9), plus `dataExtractionRules` and `fullBackupContent` that
    exclude every domain. On API 31+ `allowBackup="false"` alone doesn't stop device-to-device
    transfer (*clarified 2026-10-22*, T0).
  - A `<queries>` entry for the Health Connect package (needed on 28–33).
- **Time:** one `Clock` is created in `AppContainer`. Everything else receives it or an `Instant` /
  `ZoneId`. `ZoneId.systemDefault()` is read at the same wiring place, and re-read on
  `ACTION_TIMEZONE_CHANGED` and on foreground (D13).
- **Dependencies:** PRD A3 allowlist only.

## 3. Data

### 3.1 Room (`personal_dashboard.db`, schema version 1, schema JSON exported to `app/schemas/`)

| Table | Columns | Notes |
|---|---|---|
| `habit` | `id` INTEGER PK autoincrement · `name` TEXT not null · `weekdays` INTEGER not null (bitmask, Mon = bit 0 … Sun = bit 6; 127 = every day) · `created_on` TEXT not null (ISO `LocalDate`) · `sort_order` INTEGER not null | `weekdays` ≠ 0 (§4.2) |
| `habit_completion` | `habit_id` INTEGER FK → `habit.id` ON DELETE CASCADE · `date` TEXT not null (ISO `LocalDate`) | PK (`habit_id`, `date`) |
| `todo` | `id` INTEGER PK autoincrement · `title` TEXT not null · `due_date` TEXT null (ISO `LocalDate`) · `created_at` INTEGER not null (epoch ms) · `completed_on` TEXT null (ISO `LocalDate`, local date of the tick) | `completed_on` null = open |

Every later schema change comes with a `Migration` and a `MigrationTestHelper` test in the same
ticket. Destructive migration is never enabled.

### 3.2 DataStore (Preferences, `settings`)

| Key | Type | Default | Meaning |
|---|---|---|---|
| `calendar_ids` | `Set<String>` | absent | Absent = every calendar with `VISIBLE = 1`. Present = only these `Calendars._ID`s (§5.2) |
| `step_goal` | `Int` | 8000 | Range 1,000–50,000, step 500 (§5.3) |
| `asked_calendar` | `Boolean` | false | The calendar permission has been requested at least once (§5.1) |
| `asked_health` | `Boolean` | false | Health Connect permission requested at least once (§5.1) |

## 4. Rules (pure, `:core`)

### 4.1 Day (D13)

`today = now.atZone(zone).toLocalDate()`. A "day" is the local calendar date in the phone's
current zone. Every date stored or compared in §4 is a `LocalDate`. After a zone change the next
state recomputation uses the new zone. Stored dates are not rewritten.

### 4.2 Habits (D14, PRD A7)

- A habit has a non-blank `name` (trimmed, ≤ 60 chars) and a non-empty set of `weekdays`.
- **Scheduled on d** ⇔ `d.dayOfWeek ∈ weekdays` and `d ≥ created_on`.
- **Tick:** allowed only for `today`, and only if the habit is scheduled today. Toggling adds or
  removes the `(habit, today)` completion. Every other date is read-only.
- **Card count:** `done / scheduled`, over habits scheduled today. A habit not scheduled today is
  listed under "Not today" on the Habits screen and is left out of the card's count and list.
- **Streak:** walk back day by day starting at `today`:
  - If `today` is scheduled and not done, skip it. It doesn't count, and it doesn't break the streak.
  - Skip unscheduled days.
  - Scheduled and done: +1. Scheduled and not done: stop.
  - Stop at `created_on`.
  - Completions on unscheduled days (possible after a weekday change) are ignored.
  - The streak is computed against the *current* `weekdays`.
- **Week chain** (D23; *amended 2026-10-22*, T4): each habit row shows this week, Monday to
  Sunday. A day reads *done* (scheduled and done), *missed* (scheduled, before today, not done),
  *open* (today, scheduled, not done), *ahead* (scheduled, after today) or *off* (not scheduled,
  including days before `created_on`; a completion on an off day is ignored, as for the streak).
- **Order:** `sort_order` ascending. New habits go last. v1 has no reordering UI.
- **Delete:** removes the habit and its completions. A snackbar offers **Undo**, which restores
  both.

### 4.3 To-dos (D15, PRD A8)

- A to-do has a non-blank `title` (trimmed, ≤ 120 chars) and an optional `due_date`.
- **Buckets** relative to `today`:
  - `overdue`: open, `due_date < today`.
  - `today`: open, `due_date == today`.
  - `undated`: open, no `due_date`.
  - `upcoming`: open, `due_date > today`.
  - `doneToday`: `completed_on == today` (or later, after a westward zone change; *clarified
    2026-10-22*, T1).
  - `history`: `completed_on < today`.
- **Dashboard card:** `overdue` (oldest due first), then `today`, then `undated` (oldest
  `created_at` first), then `doneToday` struck through. `upcoming` and `history` are not on the
  card.
- **To-dos screen:** the card's sections plus `upcoming` (soonest first), then a collapsible
  **Done** section with `history` (most recent first) and a **Clear history** action. That action
  deletes all `history` items, with an Undo snackbar.
- **Tick:** sets `completed_on = today`. **Untick** (from `doneToday` or Undo) clears it. Items in
  `history` can't be unticked in v1.

### 4.4 Reducer

`reduce(state, event, now, zone) → (state, effects)`. State holds the screen back stack (PRD A4:
`[Dashboard]` at the root; Habits, To-dos and Settings are pushed; Back pops; Back at the root
finishes), the current domain snapshot, the permission/source states from §5, and pending undo.
Effects are data: `WriteHabit`, `DeleteHabit`, `RestoreHabit(habit, completions)`,
`ToggleCompletion(habit, date, done)`, `WriteTodo` (upsert by id), `DeleteTodos`,
`RequestPermission(kind)`, `OpenAppSettings`, `OpenHealthConnectInstall`, `RefreshWidget`,
`ShowSnackbar(message, undo)`, `Finish`. Plumbing executes them, in list order. The reducer performs
no I/O.

*Amended 2026-10-22 (T1):* the domain snapshot is whatever the repositories last delivered
(`HabitsLoaded`, `TodosLoaded`); the reducer never edits it optimistically. Pending undo holds only
the latest undoable action (habit delete, to-do tick, Clear history) and is cleared when the
snackbar is dismissed. Undo of a to-do tick reopens the current row, keeping any edit made
meanwhile. Opening Dashboard resets the back stack to `[Dashboard]`. `ToggleCompletion` carries the target `done` value rather than flipping, so
replaying it is harmless. `RestoreHabit` re-inserts a deleted habit with its old id and
completions. `RequestPermission` … `RefreshWidget` arrive with T6–T8.

## 5. Sources

### 5.1 Permissions (D7)

Per source, the state is one of: `Granted` · `NotAsked` · `Denied` (can ask again) ·
`PermanentlyDenied` · plus source-specific states from §5.3.

- Nothing is requested at launch. A source's request fires the first time its card is composed
  on screen in `NotAsked`. The matching `asked_*` flag is set when the request fires.
- `Denied`: the card shows its one-line reason and **Grant**, which requests again.
- `PermanentlyDenied` is reached when `asked_* = true`, the permission isn't granted, and the
  system will no longer show its dialog (for calendar, `shouldShowRequestPermissionRationale ==
  false` after a denial). **Grant** then opens the app's system settings page
  (`ACTION_APPLICATION_DETAILS_SETTINGS`).
- Permission state is re-read on every foreground (`ON_RESUME`).

### 5.2 Calendar (D17)

- Read `CalendarContract.Instances` (never `Events`, so recurrences expand) for
  `[startOfToday, startOfDayAfterTomorrow)` in the local zone. The calendars read are the ones in
  `calendar_ids`, or every calendar with `VISIBLE = 1` when the key is absent. Read-only.
- **All-day events** (`ALL_DAY = 1`) store `BEGIN`/`END` as UTC midnights. Convert them to a date
  range using UTC, never the local zone. An all-day event appears on each local date it covers
  (`END` exclusive).
- **Today:**
  - The all-day strip lists today's all-day events.
  - Timed events show if `end > now`, ordered by `begin`.
  - An event in progress is marked "Now".
- **Tomorrow:** an all-day strip, then every timed event that starts tomorrow. Events that span
  midnight from today show under today only.
- Times are formatted in the local zone, 24-hour, `HH:mm`.
- **Settings → Calendars:** every calendar on the device (display name, account name, colour
  swatch), each with a switch. Initial state follows the default above. The first toggle writes
  `calendar_ids` explicitly.
- **States:** permission states (§5.1) · `Empty` ("Nothing else today" / "Nothing tomorrow",
  per section) · `Events`.

### 5.3 Steps (D6, D16)

- Source: Health Connect `StepsRecord`, read with `aggregate` (`COUNT_TOTAL`) for today
  `[startOfToday, now)`, and with `aggregateGroupByPeriod` (1 day) for the last 7 local days,
  today included.
- **States** (in check order):
  - `Unavailable`: `getSdkStatus == SDK_UNAVAILABLE` (Health Connect can't run on this device).
    Copy: "Step counts aren't available on this phone." No action.
  - `NeedsInstall`: `SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED` (28–33: missing or old Health
    Connect app). Action **Get Health Connect** opens its Play listing, falling back to the browser
    intent if Play is absent. This is a user-initiated intent, not app network use (D3).
  - The permission states from §5.1. Health Connect's own permission contract replaces the
    runtime dialog. `PermanentlyDenied` → **Grant** opens Health Connect's settings for the app.
  - `NoData`: granted, and the 7-day total is 0. Copy explains that steps come from apps that
    write to Health Connect (a watch app, Fit, Samsung Health).
  - `Steps(today, last7)`.
- **Card:** a ring for `today / step_goal` (capped visually at 100 %, with the number shown
  uncapped), the number with thousands separators (en-GB: `8,000`), and 7 bars labelled with
  single-letter weekdays, today last.
- **Settings → Step goal:** a stepper from 1,000 to 50,000 in steps of 500.
- **Privacy rationale:** the About screen (PRD A9) is also the activity Health Connect launches.
  On 28–33 it is registered via an intent filter for
  `androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE`; on 34+ via an `activity-alias` for
  `android.intent.action.VIEW_PERMISSION_USAGE` with category
  `android.intent.category.HEALTH_PERMISSIONS`.

## 6. UI

### 6.1 App

- One Activity, edge-to-edge. Four screens:
  - **Dashboard:** the four cards in the D18 order, as T3 approves it. Each card's header opens
    its screen (Calendar and Steps open Settings).
  - **Habits:** today's scheduled habits with tick and streak, then "Not today". Add/edit sheet
    with name and weekday chips. Delete from the edit sheet.
  - **To-dos:** §4.3, an add/edit sheet with title and an optional date (Material date picker).
  - **Settings:** Calendars, Step goal, About.
- The look comes from the T3 direction and the Compose theme tokens it produces. `DESIGN.md`
  describes the shipped theme afterwards.
- PRD A1 bar: light/dark following the system, WCAG 2.2 AA contrast, 48 dp targets, TalkBack
  labels (every card announces its summary, e.g. "Habits, 2 of 3 done"), works at 360 dp and
  200 % font.

### 6.2 Widget (D2, D8, D19)

- One Glance widget, resizable, minimum 2×2 cells. Content:
  - Next event: the first timed event still to start or in progress, today or tomorrow, as
    "HH:mm Title" ("Tomorrow HH:mm Title" for tomorrow). If none: "Nothing scheduled".
  - Steps: "today / goal", or a dash when not `Steps`.
  - Habits: "done / scheduled", or "No habits today".
- Tap anywhere: opens the app on the Dashboard. No other actions.
- If a source isn't granted or available, its line shows a dash and the hint "Open to set up".
- **Refresh:**
  - Unique periodic WorkManager work, 30 min, no constraints, `KEEP` policy, enqueued at app
    start and when the widget is added.
  - Plus an immediate `updateAll` after every habit/to-do write and on app foreground.
  - The widget reads from the same sources and `:core` rules as the app. There is no separate cache.

## 7. Copy

- en-GB, plain, short, second person. No exclamation marks.
- Every empty or blocked state says what is missing and what will fix it, in one line.
- Every string lives in `strings.xml` (D21: translations are out, but strings are not hard-coded).
  `PRODUCT.md` (T3) sets the voice and refines the strings named here.

## 8. Build and release (D5, PRD A6)

- `./gradlew test` (JVM, `:core` and `:app` unit) · `./gradlew lint` · `./gradlew assembleDebug`
  · `./gradlew connectedAndroidTest` · `./gradlew assembleRelease`.
- Release signing reads `storeFile`, `storePassword`, `keyAlias` and `keyPassword` from
  `~/.gradle/gradle.properties` (keys prefixed `PD_`). Nothing secret lives in the repo. When the
  properties are absent, `assembleRelease` fails with a message naming the missing keys.
  `isMinifyEnabled = false`.
- `versionCode` increments on every shared APK. The `versionName` is shown in About.

## 9. Non-goals (v1)

Weather. Health metrics other than steps. Any network access. Backup, export or sync.
Reminders and notifications. Creating, editing or RSVPing to calendar events. Tablet, foldable
or two-pane layouts. Crash reporting and analytics. Translations. Reordering or hiding cards.
Widget actions. More than one widget. Weekly or N-per-week habits. A rollover hour other than
midnight. Editing past habit days. Unticking a to-do already in history.
