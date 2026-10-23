# BACKLOG — personalDashboard

**Status:** Wave 1 (v1, D1–D26) **closed 2026-10-23**. T0–T6 done 2026-10-22, T7–T9 and O1 done 2026-10-23. Wave 2 (D27: status-bar band, theme setting) added 2026-10-23, U1–U4. U1–U3 done. Frontier: **U4** (owner pass). Open feedback: F2.

**Build order:** T0 → T1 → T2 → (T3 owner gate) → T4 → T5 → T6 → T7 → T8 → T9. Wave 2: (U1 ∥ U2) → U3 → U4. T3 runs beside
T0–T2. O1 runs any time after T0.

Ticket format: `.claude/skills/to-tickets/SKILL.md`. DONE line: `— **DONE YYYY-MM-DD** (<hash>)`.
Paths below use `core/…/` for `core/src/main/kotlin/<pkg>/`, `core/test/…/` for
`core/src/test/kotlin/<pkg>/`, and `app/…/` / `app/test/…/` / `app/androidTest/…/` likewise. `<pkg>`
is fixed in T0.

---

## Wave 1 — v1 — *added 2026-10-22, see `PRD.md` D1–D22*

T0 sets up the build. T1 and T2 lay the pure rules and the storage under them. T3 is the
owner's design gate and runs in parallel with T0–T2. T4–T8 are serial, because each one extends
the same reducer state, dashboard ViewModel and (from T6) Settings screen. O1 is independent
after T0. T9 closes the wave on a real device.

Dependencies: the `PRD.md` A3 allowlist only, with each library added by the first ticket that
uses it. No others.

### T0 — Gradle scaffold — *added 2026-10-22* — **DONE 2026-10-22** (dfa8c1e)
Files: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/*`,
`gradlew`, `core/build.gradle.kts`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`,
`app/…/MainActivity.kt`, `app/…/ui/theme/Theme.kt`, `core/test/…/SmokeTest.kt`,
`app/androidTest/…/LaunchTest.kt`, `.gitignore`, `CLAUDE.md` (Dev environment task list).

A two-module project (SPEC §2, PRD A2). `:core` is Kotlin/JVM with no Android plugin. `:app` is
an Android app with Compose, minSdk 28 and compileSdk/targetSdk at the highest stable API the
chosen AGP supports (PRD A5). At this stage it is one Activity with a placeholder Material 3
theme and a "personalDashboard" text. `applicationId` / package is put to the owner as a
forced choice at the start of this ticket (it can't change later without a reinstall). The
manifest has no `INTERNET` and `allowBackup="false"` (D3, D9). Only the libraries this ticket
uses go into the catalog.

Blocked by: none — can start now.

Accept: with `JAVA_HOME` set per `CLAUDE.md`:
- `./gradlew test lint assembleDebug` exits 0.
- `./gradlew connectedAndroidTest` on `pd_api37` passes `LaunchTest` (the activity starts and
  the text is displayed).
- `grep -c INTERNET app/src/main/AndroidManifest.xml` prints `0`.
- `grep 'allowBackup="false"' app/src/main/AndroidManifest.xml` matches.
- `grep -rn 'import android' core/` matches nothing.
- `CLAUDE.md` Dev environment lists the confirmed task names.

Non-goals: no Room, DataStore, Glance, WorkManager or Health Connect yet (each comes with its
ticket). No real theme (T3/T4). No release signing (O1). No CI.

### T1 — Pure rules and reducer skeleton — **high-care** — *added 2026-10-22* — **DONE 2026-10-22** (35ba61a)
Files: `core/…/time/Day.kt`, `core/…/habit/Habit.kt`, `core/…/habit/Streak.kt`,
`core/…/todo/Todo.kt`, `core/…/todo/Buckets.kt`, `core/…/app/State.kt`, `core/…/app/Event.kt`,
`core/…/app/Effect.kt`, `core/…/app/Reduce.kt`, `core/test/…/time/DayTest.kt`,
`core/test/…/habit/StreakTest.kt`, `core/test/…/todo/BucketsTest.kt`, `core/test/…/app/ReduceTest.kt`.

Every rule in SPEC §4, as pure Kotlin in `:core`:
- Local-date day boundary (§4.1).
- Habit scheduling, tick eligibility, done/scheduled count and streak (§4.2, D14, PRD A7).
- To-do validation and the six buckets with their ordering (§4.3, D15, PRD A8).
- The reducer with its back stack (§4.4, PRD A4), and the habit/to-do events emitting the §4.4
  effects, including undo.

Blocked by: T0.

Accept: `./gradlew :core:test` passes, with cases named by capability:
- `DayTest`: tick at 23:59 and 00:01 land on different dates; a zone change moves `today`.
- `StreakTest`:
  - Today not done doesn't break.
  - Unscheduled days are skipped.
  - A missed scheduled day stops the count.
  - The count stops at `created_on`.
  - Completions on unscheduled days are ignored.
  - A weekday change recomputes.
- `BucketsTest`: each bucket boundary at `today ± 1`; `doneToday` moves to `history` after
  midnight; ordering within each bucket.
- `ReduceTest`:
  - Tick is refused on an unscheduled day and on a past date.
  - Delete emits `DeleteHabit` + `ShowSnackbar` with undo, and undo restores.
  - Back pops, and Back at the root emits `Finish`.
  - Clear history emits `DeleteTodos` + undo.

Non-goals: no persistence, no Android, no calendar/steps rules (T6/T7 add their pure halves), no
UI.

### T2 — Room v1, repositories, AppContainer — *added 2026-10-22* — **DONE 2026-10-22** (5c8fdcf)
Files: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/schemas/` (generated),
`app/…/data/Db.kt`, `app/…/data/HabitDao.kt`, `app/…/data/TodoDao.kt`, `app/…/data/Entities.kt`,
`app/…/data/HabitRepository.kt`, `app/…/data/TodoRepository.kt`, `app/…/AppContainer.kt`,
`app/…/PdApplication.kt`, `app/androidTest/…/data/DaoTest.kt`.

The SPEC §3.1 schema at version 1, with JSON exported, in Room via KSP (D11). Repositories expose
`Flow`s of `:core` domain types and execute the T1 effects. `AppContainer` is the single wiring
place for `Clock.systemDefaultZone()`, `ZoneId`, dispatchers, the database and the repositories
(D10). It is created in `PdApplication`.

Blocked by: T1.

Accept:
- `./gradlew connectedAndroidTest --tests '*DaoTest*'` passes:
  - Insert/read habit with weekday bitmask round-trip.
  - Completion PK rejects duplicates.
  - Deleting a habit cascades its completions.
  - A to-do with a null due date round-trips.
- `app/schemas/…/1.json` exists.
- `grep -rn 'System.currentTimeMillis\|Clock.system' app/src/main` matches only
  `AppContainer.kt`.

Non-goals: no DataStore (T6), no UI, no migrations (none exist at v1), and no
`fallbackToDestructiveMigration` ever.

### T3 — Design direction round — **OWNER gate** — *added 2026-10-22* — **DONE 2026-10-22** (a83a7a7)
Files: `PRODUCT.md` (new), `.impeccable/` (local, gitignored), comps as the skill emits them.

Run `impeccable` in native/Android mode (`reference/android.md`) for the dashboard (four cards,
D18 order as the starting point), one Habits screen and the 2×2 widget (D19), in light and dark.
`init` writes `PRODUCT.md`: users (owner + a few people, D4), positioning, the PRD A1 bar, and the
en-GB voice (SPEC §7). The owner picks a direction and approves the comps. Card order may change
here (D18). If it does, record it as a D-entry and amend SPEC §6.1.

Blocked by: none — can start now.

Accept:
- `PRODUCT.md` exists and states the A1 bar and the voice.
- The owner's approval is recorded as a D-entry in `DECISIONS.md` naming the chosen direction and
  the comp files.

Non-goals: no Kotlin. No `DESIGN.md` (written from the shipped UI after T8). No icon set beyond
what the comps need.

### T4 — Theme, dashboard shell, Habits — *added 2026-10-22* — **DONE 2026-10-22** (86146b3)
Files: `app/…/ui/theme/*`, `app/…/ui/MainScreen.kt`, `app/…/ui/dashboard/DashboardScreen.kt`,
`app/…/ui/dashboard/HabitsCard.kt`, `app/…/ui/habits/HabitsScreen.kt`,
`app/…/ui/habits/HabitSheet.kt`, `app/…/MainViewModel.kt`, `app/src/main/res/values/strings.xml`,
`app/androidTest/…/ui/HabitsFlowTest.kt`.

The Compose theme from T3's tokens, in light and dark. The dashboard has the four cards in
order. Calendar, To-dos and Steps are present as titled placeholders that later tickets fill. The
Habits card shows done/scheduled and today's habits with tick and streak (SPEC §4.2). The Habits
screen has "Not today", the add/edit sheet with weekday chips (default all), and delete with an
Undo snackbar. Navigation is the reducer back stack + `BackHandler` (PRD A4).
`MainViewModel` only maps the reducer to the UI and runs effects.

Blocked by: T2, T3.

Accept:
- `./gradlew test` passes.
- `./gradlew connectedAndroidTest --tests '*HabitsFlowTest*'` passes:
  - Add "Stretch" Mon–Fri, tick it, and the card shows 1 / 1 with streak 1.
  - Delete → Undo restores it with its streak.
  - System Back from Habits returns to Dashboard.
  - Every card and control has a content description.
- `grep -rn '"[A-Z][a-z]' app/src/main/kotlin --include=*.kt | grep -v 'R.string\|Preview\|Log\.'`
  finds no hard-coded UI copy.

Non-goals: no habit reordering, no editing past days, no to-do/calendar/steps content, no
animations beyond what the comps specify.

### T5 — To-dos — *added 2026-10-22* — **DONE 2026-10-22** (fdde0b9)
Files: `app/…/ui/dashboard/TodosCard.kt`, `app/…/ui/todos/TodosScreen.kt`,
`app/…/ui/todos/TodoSheet.kt`, `app/…/MainViewModel.kt`, `app/…/ui/dashboard/DashboardScreen.kt`,
`app/src/main/res/values/strings.xml`, `app/androidTest/…/ui/TodosFlowTest.kt`.

The To-dos card replaces its placeholder (SPEC §4.3, D15): overdue (marked), today, undated,
then done-today struck through. The To-dos screen adds upcoming and the collapsible Done history
with **Clear history** + Undo. The add/edit sheet takes a title and an optional date from the
Material date picker.

Blocked by: T4.

Accept:
- `./gradlew connectedAndroidTest --tests '*TodosFlowTest*'` passes:
  - An undated item and one due yesterday both show, overdue first.
  - Tick → struck through and still on the card.
  - Clear history → Undo restores it.
  - A future-dated item appears only on the To-dos screen.

Non-goals: no due times, no reminders (D20), no unticking from history, no search or tags.

### T6 — Calendar card and Settings — **high-care** — *added 2026-10-22* — **DONE 2026-10-22** (5248381)
Files: `core/…/calendar/Agenda.kt`, `core/test/…/calendar/AgendaTest.kt`, `core/…/app/*`,
`app/…/source/CalendarReader.kt`, `app/…/data/Settings.kt`,
`app/…/ui/dashboard/CalendarCard.kt`, `app/…/ui/settings/SettingsScreen.kt`,
`app/…/ui/settings/CalendarsScreen.kt`, `app/src/main/AndroidManifest.xml`, `app/…/MainViewModel.kt`,
`app/…/AppContainer.kt`, `gradle/libs.versions.toml`, `app/build.gradle.kts`,
`app/src/main/res/values/strings.xml`, `app/androidTest/…/ui/CalendarCardTest.kt`.

The Calendar card per SPEC §5.2 and D17:
- Instances read for today and tomorrow, all-day strip, past timed events hidden, "Now" marker.
- The permission flow per §5.1 and D7: request on first view, explain + Grant, then app settings
  after a permanent deny.
- Settings screen created with a Calendars entry: a per-calendar switch, stored in DataStore
  `calendar_ids` (SPEC §3.2, the first DataStore use).

The pure half (row → agenda, UTC all-day conversion, span rules) lives in `:core`.

Blocked by: T5.

Accept:
- `./gradlew :core:test --tests '*AgendaTest*'` passes:
  - An all-day event stored at UTC midnight shows on the right local date in UTC−8 and UTC+13.
  - A multi-day all-day event covers each date, end exclusive.
  - An event that ended today is hidden; an in-progress event is "Now".
  - An event spanning midnight shows under today only.
- `./gradlew connectedAndroidTest --tests '*CalendarCardTest*'` passes: with the permission
  revoked, the card shows the reason + Grant; with it granted and an event inserted via
  `CalendarContract` in test setup, the event title appears.
- `grep WRITE_CALENDAR app/src/main/AndroidManifest.xml` matches nothing.

Non-goals: no event editing or detail screen (D21), no days beyond tomorrow, no colour-coding
beyond the Settings swatch unless the comps ask for it.

### T7 — Steps and Health Connect — **high-care** — *added 2026-10-22* — **DONE 2026-10-23** (c663e2a)
Files: `core/…/steps/Steps.kt`, `core/test/…/steps/StepsTest.kt`, `core/…/app/*`,
`app/…/source/StepsReader.kt`, `app/…/data/Settings.kt`, `app/…/ui/dashboard/StepsCard.kt`,
`app/…/ui/settings/SettingsScreen.kt`, `app/…/ui/settings/AboutScreen.kt`,
`app/…/PrivacyRationaleActivity.kt`, `app/src/main/AndroidManifest.xml`, `app/…/MainViewModel.kt`,
`app/…/AppContainer.kt`, `gradle/libs.versions.toml`, `app/build.gradle.kts`,
`app/src/main/res/values/strings.xml`, `app/androidTest/…/ui/StepsCardTest.kt`.

The Steps card per SPEC §5.3, D6 and D16:
- Every state, in check order: `Unavailable` → `NeedsInstall` → permission states → `NoData` →
  `Steps`.
- Today via `aggregate`, the last 7 days via `aggregateGroupByPeriod`.
- A `Canvas` ring for today / goal, and 7 `Canvas` bars.
- Settings → Step goal stepper (1,000–50,000, step 500, DataStore `step_goal`).
- The About screen, doubling as the Health Connect rationale activity, with both the 28–33
  intent filter and the 34+ alias (PRD A9).

Blocked by: T6.

Accept:
- `./gradlew :core:test --tests '*StepsTest*'` passes:
  - State precedence when several conditions hold.
  - The ring fraction is capped at 1 and the label isn't.
  - The 7-day series is ordered today-last and zero-fills days with no records.
  - The goal is clamped to range.
- `./gradlew connectedAndroidTest --tests '*StepsCardTest*'` on `pd_api37` passes: with
  permission granted and a `StepsRecord` inserted via `HealthConnectClient`, the count shows with
  a thousands separator.
- Both rationale entries appear: `grep -c 'SHOW_PERMISSIONS_RATIONALE\|VIEW_PERMISSION_USAGE'
  app/src/main/AndroidManifest.xml` prints `2`.

Non-goals: no other Health Connect record types (D1), no writing to Health Connect, no history
beyond 7 days, no chart library (A3).

### T8 — Glance widget and refresh — **high-care** — *added 2026-10-22* — **DONE 2026-10-23** (c689283)
Files: `core/…/widget/Summary.kt`, `core/test/…/widget/SummaryTest.kt`,
`app/…/widget/DashboardWidget.kt`, `app/…/widget/DashboardWidgetReceiver.kt`,
`app/…/widget/RefreshWorker.kt`, `app/src/main/res/xml/dashboard_widget_info.xml`,
`app/src/main/AndroidManifest.xml`, `app/…/MainViewModel.kt`, `app/…/AppContainer.kt`,
`gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/res/values/strings.xml`,
`app/androidTest/…/widget/RefreshWorkerTest.kt`.

One Glance widget per SPEC §6.2, D19 and the T3 widget comp:
- Lines: next event, steps / goal, habits done / scheduled. A dash plus "Open to set up" for a
  source that isn't ready.
- Tap opens the Dashboard.
- Unique periodic work every 30 min (`KEEP`), enqueued at app start and on widget add.
- An immediate `updateAll` after each habit/to-do write and on foreground (D8).
- The summary text is a pure `:core` function over the same snapshots the app uses.

Blocked by: T7.

Accept:
- `./gradlew :core:test --tests '*SummaryTest*'` passes:
  - Next event picks an in-progress event over a later one.
  - Tomorrow is prefixed "Tomorrow".
  - Nothing left gives "Nothing scheduled".
  - A missing source gives a dash.
- `./gradlew connectedAndroidTest --tests '*RefreshWorkerTest*'` passes (`TestListenableWorkerBuilder`
  returns success; unique work is enqueued once after two app starts).

Non-goals: no widget actions (D19), no second widget (D2), no configuration activity, no
separate widget cache.

### O1 — Release signing — *added 2026-10-22* — **DONE 2026-10-23** (2c16afe)
Files: `app/build.gradle.kts`, `CLAUDE.md` (Release section), `.gitignore`.

`release` signing config per SPEC §8 (D5): reads `PD_STORE_FILE`, `PD_STORE_PASSWORD`,
`PD_KEY_ALIAS` and `PD_KEY_PASSWORD` from `~/.gradle/gradle.properties`, and fails clearly
naming any missing key. `isMinifyEnabled = false` (A6). The owner creates the keystore
(`keytool`, outside the repo). `CLAUDE.md` records where it lives, how it is backed up, and the
`versionCode` bump rule.

Blocked by: T0.

Accept:
- With the properties set, `./gradlew assembleRelease` exits 0, and `apksigner verify
  --print-certs app/build/outputs/apk/release/app-release.apk` shows the owner's cert.
- With them unset, the build fails with a message naming `PD_STORE_FILE`.
- `git ls-files | grep -iE '\.(jks|keystore)$'` matches nothing.

Non-goals: no Play App Signing, no CI signing, no Firebase (D5).

### T9 — Verification chain and owner manual pass — **OWNER gate** — *added 2026-10-22* — **DONE 2026-10-23** (bef9797)
*Result:* the full chain exits 0 (22 instrumented tests). On API 30, `NeedsInstall` opens Health
Connect's Play listing. The owner passed PRD §8 on a Galaxy S25 (API 37), except: step 1 shows the
calendar dialog (F2); step 5 was seen only as `NoData`, because Samsung Health had written no
steps; the Sat → Mon streak and the midnight rollover are covered by tests only. Owner also filed
F3 (status-bar overlap). The owner closed the ticket with these gaps noted.
Files: `BACKLOG.md`, `FEEDBACK.md`, `CLAUDE.md` (Status), `DESIGN.md` (via impeccable
documenter), `.impeccable/design.json` sidecar if the documenter emits it.

Run the full chain, then install the signed release APK on the owner's phone and walk `PRD.md`
§8 steps 1–8. Also boot an API 30 AVD (`system-images;android-30;google_apis_playstore;arm64-v8a`)
to check the Health Connect `NeedsInstall` path (PRD §7). Then the impeccable documenter writes
`DESIGN.md` from the shipped UI. Close the wave: `CLAUDE.md` Status and "What this is", and
`FEEDBACK.md` F1 → shipped.

Blocked by: T8, O1.

Accept:
- `./gradlew test lint assembleDebug connectedAndroidTest assembleRelease` exits 0.
- The owner confirms PRD §8 steps 1–8 on a device; failures become `FEEDBACK.md` items.
- `DESIGN.md` exists.

Non-goals: fixing anything found here inside this ticket. Each finding becomes an F-item and a
new ticket.

---

## Wave 2 — Status-bar band and theme setting — *added 2026-10-23, see `DECISIONS.md` "D27"*

U1 (F3) and U2 (F4) touch different files and run in parallel worktrees. U3 follows U2 because it
reads U2's setting. U3 is the wave's last code ticket and runs the full chain. U4 is the owner
pass on the S25. No new dependency.

### U1 — Status-bar band on the Dashboard (F3) — *added 2026-10-23* — **DONE 2026-10-23** (2e70cd9)
Files: `app/…/ui/dashboard/DashboardScreen.kt`, `app/androidTest/…/ui/DashboardInsetsTest.kt` (new),
`SPEC.md` §6.1.

Scrolling the Dashboard no longer puts rows under the clock and status icons (D27 a). The list
is clipped at the bottom of the status-bar inset, and the inset itself is a solid band in
`Pd.colors.stock`, in both schemes. At rest the masthead sits where it does now. The other screens
keep their fixed `TopBar`. SPEC §6.1 *amended*: "Edge-to-edge; on the Dashboard the status-bar
inset is a solid stock band and the list is clipped below it."

Blocked by: none — can start now.

Accept:
- `./gradlew connectedAndroidTest` on `pd_api37` passes `DashboardInsetsTest`:
  - `scrolledRowsStayBelowTheStatusBar`: after scrolling to the Steps card, no visible list
    node's clipped top is above the status-bar inset.
  - `bandFillsTheInsetInStock`: a pixel inside the inset is `Pd.colors.stock`.
- The count check (`CLAUDE.md`) includes `DashboardInsetsTest`.

Non-goals: no gradient or fading scrim (D27 g). No change to the `TopBar` screens or the
navigation-bar inset. No collapsing masthead.

### U2 — Theme setting: System / Light / Dark (F4) — *added 2026-10-23* — **DONE 2026-10-23** (c67bdb1)
Files: `core/…/app/{Settings,Event,Effect,Reduce}.kt`, `core/…/app/Theme.kt` (new),
`core/test/…/app/ThemeTest.kt` (new), `app/…/data/Settings.kt`, `app/…/MainViewModel.kt`,
`app/…/MainActivity.kt`, `app/…/PrivacyRationaleActivity.kt`, `app/…/ui/theme/Theme.kt`,
`app/…/ui/MainScreen.kt`, `app/…/ui/settings/SettingsScreen.kt`, `app/src/main/res/values/strings.xml`,
`app/androidTest/…/ui/SettingsThemeTest.kt` (new), `SPEC.md` §3.2 §4.4 §6.1, `PRODUCT.md`.

Settings gains a "Theme" row with three chips (System · Light · Dark) between Step goal and
About (D27 c, e). Picking one applies at once to every app screen, the privacy screen, and the
status- and navigation-bar icons, and survives a restart. Default System, so v1 behaviour is unchanged until
someone picks. Rules in `:core`: `ThemeChoice { System, Light, Dark }`, `Settings.theme`,
`Event.SetTheme(choice)` → `Effect.WriteTheme(choice)`, which joins the writes that append
`RefreshWidget` (D26). There's also a pure `isDark(choice, systemDark)`. DataStore key `theme`, a string, absent =
System. SPEC patches: §3.2 row `theme`; §4.4 the `SetTheme` case; §6.1 Settings lists "Calendars,
Step goal, Theme, About", and the A1 line reads "light/dark following the Theme setting (default
System)". `PRODUCT.md`: "Dark theme follows the system by default; Settings → Theme can force
Light or Dark (D27)."

Blocked by: none — can start now.

Accept:
- `./gradlew :core:test --tests '*ThemeTest*'` passes:
  - `isDarkFollowsSystemOnlyForSystem`: the three choices × system dark on/off.
  - `setThemeWritesAndRefreshesWidget`: the effects are `[WriteTheme(Dark), RefreshWidget]`.
  - `defaultSettingsThemeIsSystem`.
- `./gradlew connectedAndroidTest` passes `SettingsThemeTest`:
  - `pickingDarkRecoloursTheApp`: with the system light, tapping **Dark** marks that chip
    selected, and the Settings background pixel is `DarkColors.stock`.
  - `choiceSurvivesRecreate`: the same after `ActivityScenario.recreate()`.
- `grep -rn isSystemInDarkTheme app/src/main` matches one wiring line only.
- `grep -n "no in-app toggle" PRODUCT.md` matches nothing.

Non-goals: no widget change (U3). No pure-black palette, scheduled switching or accent choice
(D27 g). No change to the window theme or splash (D27 assumptions). No Dynamic Color (D23).

### U3 — Widget follows the theme choice — *added 2026-10-23* — **DONE 2026-10-23** (71c8f5f)
Files: `core/…/app/Theme.kt`, `core/test/…/app/ThemeTest.kt`, `app/…/widget/DashboardWidget.kt`,
`SPEC.md` §6.2.

The widget uses the app's choice (D27 d). Light or Dark forces that palette whatever the system
mode, and System keeps the day/night pair. Changing the setting refreshes the widget (U2's
`RefreshWidget`). The palette pick is a pure `:core` function, `widgetScheme(choice)` →
`Light | Dark | DayNight`. SPEC §6.2 *amended*: "Colours follow the Theme setting: forced
Light/Dark, or day/night on System."

Blocked by: U2.

Accept:
- `./gradlew :core:test --tests '*ThemeTest*'` passes `widgetSchemeMatchesChoice` (the three
  choices).
- Full chain: `./gradlew test lint assembleDebug connectedAndroidTest` exits 0 on `pd_api37`, and
  the count check includes `DashboardInsetsTest` and `SettingsThemeTest`.

Non-goals: no widget preview fix (`previewLayout`), no 200 % font fix (`DESIGN.md` Known Gaps).
No widget-only theme setting.

### U4 — Owner pass, v0.2.0 — **OWNER gate** — *added 2026-10-23*
Files: `app/build.gradle.kts` (`versionCode` 2, `versionName` "0.2.0"), `BACKLOG.md`, `FEEDBACK.md`,
`CLAUDE.md` (Status), `DESIGN.md` (via the impeccable documenter, for the band and the Theme row).

Bump the version. Build the signed release, verify the cert against `CLAUDE.md` Release, and
install it over v0.1.0 on the owner's S25 with `adb install -r`, so data is kept (D27 f). The
owner walks:
1. Habits and to-dos from v0.1.0 are still there.
2. Scroll the Dashboard: nothing passes under the clock, in light and dark.
3. Settings → Theme → Dark with the phone in light mode: the app, the bar icons and the widget go
   dark. Light with the phone in dark does the reverse. System follows the phone again.
4. Kill and reopen the app: the choice holds.

Blocked by: U1, U3.

Accept:
- `apksigner verify --print-certs` shows the `CLAUDE.md` cert, and `dumpsys package dev.armaan.pd`
  shows `versionCode=2`.
- The owner confirms steps 1–4; failures become `FEEDBACK.md` items.
- `DESIGN.md` records the band and the Theme row.

Non-goals: fixing anything found here inside this ticket.
