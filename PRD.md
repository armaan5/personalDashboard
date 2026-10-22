# PRD — personalDashboard v1

Written 2026-10-22. First scoping session, from an empty repo, grilled to closure in six
forced-choice rounds and recorded here as decisions D1–D22. Nothing below is a guess. Where a
question was not put to the owner, the assumption is marked.

`SPEC.md` is the normative description of v1 and is written from this PRD. Wave 1 in
`BACKLOG.md` builds it. No code exists yet, so there are no seams and no `PLAN-V1.md`. The
backlog tickets carry the order.

---

## 1. The asks, as answered

The owner did not bring a written ask. The v1 shape is the owner's answers to the grill,
verbatim where the owner typed them:

- Content: **"Tasks / habits, Health / activity, calendar but no weather"**.
- Surface: app + one home-screen widget.
- Data: on-device only.
- Audience: "Me + a few people, sideloaded".
- Everything else: the option the owner picked in §3.

## 2. What already exists

- Nothing is built. The repo holds `CLAUDE.md`, the ported `.claude/` workflow, and this
  document set.
- The dev env is ready (2026-10-22): Android SDK with platforms 36 and 37.0, build-tools
  37.0.0, emulator, and AVD `pd_api37` (API 37, Play image, arm64). See `CLAUDE.md` Dev
  environment.
- `CLAUDE.md` proposed three load-bearing constraints at port time (pure reducer, injected
  time, returned effects). D10 ratifies them.

## 3. Decisions (2026-10-22)

| # | Question | Decision | Lands in |
|---|---|---|---|
| D1 | What v1 shows | **Three modules: Calendar, Habits + to-dos, Steps.** No weather. Health is steps only (D16) | SPEC §1, T4–T7 |
| D2 | Surface | **Compose app + one Glance home-screen widget** | SPEC §6, T8 |
| D3 | Data sources | **On-device only.** CalendarContract, Health Connect, and the app's own Room database. The manifest declares **no `INTERNET` permission**, and there is no network code. Health Connect is IPC, not network | SPEC §2, T0 |
| D4 | Audience | **The owner plus a few people, sideloaded.** Copy, empty states and permission prompts have to make sense to someone who isn't the owner. No Play listing, no data-safety form | SPEC §7 |
| D5 | Distribution | **Signed release APK, shared directly.** The release keystore and its passwords stay outside git. Every release uses the same key, so updates install over the top | SPEC §8, O1 |
| D6 | minSdk | **28 (Android 9).** Health Connect is built in on 34+. On 28–33 it is the Play app, so the steps card has a "needs Health Connect" state | SPEC §5.3, T0, T7 |
| D7 | Permission UX | **Ask per card, on first view; explain + Grant on deny.** Nothing is requested at first launch. A card whose permission is missing stays in place, says in one line why it is empty, and offers **Grant**. Once Android stops showing the system dialog (permanent deny), **Grant** opens the app's system settings page | SPEC §5.1, T6, T7 |
| D8 | Widget freshness | **Periodic + on change.** A WorkManager periodic job every 30 min, with no constraints. On top of that, an immediate widget update after any habit or to-do write and whenever the app comes to the foreground. Steps on the widget may lag up to 30 min | SPEC §6.2, T8 |
| D9 | Backup | **None.** `android:allowBackup="false"`. Uninstalling or moving to a new phone loses habits, to-dos and settings | SPEC §2, T0 |
| D10 | Architecture | **Ratified as written in `CLAUDE.md`.** Pure reducer `reduce(state, event, now) → state + effects`, injected `Clock`, effects returned rather than performed. ViewModels, Activities, Workers and repositories are plumbing | `CLAUDE.md`, all tickets |
| D11 | Persistence + DI | **Room for habits, completions and to-dos (schema exported from v1). Preferences DataStore for settings. Hand-written `AppContainer`, no Hilt.** Room uses KSP | SPEC §3, T0, T2 |
| D12 | Look | **Impeccable direction round before the first UI ticket.** Native/Android mode, light and dark. It writes `PRODUCT.md` and approves comps for the dashboard and widget. `DESIGN.md` is written afterwards from the shipped UI | T3 (owner gate) |
| D13 | Habit day boundary | **Local midnight in the phone's current time zone.** Completions are stored as `LocalDate`. A tick at 00:30 counts for the new day | SPEC §4.1, T1 |
| D14 | Habit schedule | **Chosen weekdays, defaulting to all seven.** The streak counts consecutive *scheduled* days that were done. Unscheduled days neither add to it nor break it. A scheduled day that isn't done breaks the streak only once that day has ended | SPEC §4.2, T1 |
| D15 | To-dos on the dashboard | **Overdue, due today and undated.** Future-dated to-dos appear only in the full list. A to-do ticked today stays on the dashboard, struck through, until local midnight, then moves to Done history. History can be cleared | SPEC §4.3, T1, T5 |
| D16 | Steps | **Daily goal (default 8,000, set in Settings) + 7-day bars.** The dashboard shows today against the goal as a ring, plus the last 7 days as bars. The widget shows today / goal | SPEC §5.3, T7 |
| D17 | Calendar | **Today's remaining events + tomorrow's, read-only. Calendars chosen in Settings, default all visible. All-day events in a strip above timed events. Timed events that have already ended today are hidden** | SPEC §5.2, T6 |
| D18 | Dashboard order | **Fixed: Calendar → Habits → To-dos → Steps.** The direction round (T3) may reorder them. There is no user reordering or hiding | SPEC §6.1, T4 |
| D19 | Widget content | **Summary card: next event, steps today / goal, habits done / scheduled today.** Tapping it opens the app. Read-only, with no actions on the widget | SPEC §6.2, T8 |
| D20 | Reminders | **None in v1.** No `POST_NOTIFICATIONS`, no alarms | SPEC §9 |
| D21 | Explicitly out | **Editing calendar events (no `WRITE_CALENDAR`), tablet/foldable layouts, crash reporting/analytics, translations** | SPEC §9 |
| D22 | Paperwork tier | **Large tier, v1.** This `PRD.md` + `SPEC.md` + `BACKLOG.md` Wave 1. No `PLAN-V1.md`, because there are no seams to record before the scaffold exists | — |

### Assumptions (not put to the owner)

Each of these may be overruled. Overruling one becomes a D-entry.

- **A1 — Product bar.** en-GB voice. WCAG 2.2 AA. TalkBack labels on every control and card. 48 dp
  minimum touch targets. Layout holds at 360 dp width and 200 % font scale. Dark theme follows the
  system, with no in-app toggle. Portrait and landscape both work (landscape just stretches, D21).
  T3 writes these into `PRODUCT.md`.
- **A2 — Module split enforces D10.** Two Gradle modules: `:core` (Kotlin/JVM, so it *cannot*
  import `android.*`) holds every rule; `:app` (Android) holds plumbing and UI. This makes the
  "no android imports" rule a compile error rather than a review item.
- **A3 — Dependency allowlist for v1.** AGP, Kotlin, the Compose BOM (material3, ui,
  ui-tooling-preview), activity-compose, lifecycle-viewmodel-compose / runtime-compose, Room
  (runtime, ktx, compiler via KSP, testing), DataStore Preferences, WorkManager (runtime-ktx),
  Glance (appwidget, material3), Health Connect client, kotlinx-coroutines (core, test), JUnit 4,
  and the AndroidX test runner, junit-ext and compose-ui-test. **Not allowed:** Hilt/Dagger,
  Navigation, a chart library, an image loader, a network client. The 7-day bars and the ring are
  drawn with Compose `Canvas`. The scaffold ticket picks current stable versions into
  `gradle/libs.versions.toml`.
- **A4 — Navigation is hand-rolled.** There are four screens (Dashboard, Habits, To-dos,
  Settings), and the back stack is a list in the pure reducer, driven by `BackHandler`. No
  Navigation library (A3).
- **A5 — compileSdk / targetSdk** = the highest stable API the chosen AGP supports (≥ 36), pinned
  in T0.
- **A6 — Release build is not minified** in v1 (`isMinifyEnabled = false`). This avoids R8 keep-rule
  surprises with Room, Glance and Health Connect on a sideloaded app, where APK size doesn't
  matter.
- **A7 — Habit details.** A habit can only be ticked for *today*, and only on a day it is scheduled.
  Today's tick can be undone. Past days can't be edited. Changing a habit's weekdays recomputes its
  streak against the new schedule. Deleting a habit deletes its completions, with an undo
  snackbar and no confirmation dialog.
- **A8 — To-do details.** The due date is a date, not a time (no reminders, D20). Ticking can be
  undone from the snackbar and by unticking while it's still on the dashboard. "Clear history" uses
  an undo snackbar, not a dialog.
- **A9 — Settings has an About row** that shows the version and a one-screen privacy statement
  ("Nothing leaves this phone"). Health Connect requires that screen as its permission-rationale
  activity (SPEC §5.3).

## 4. Scope

**In:** SPEC §1–§8, built by `BACKLOG.md` Wave 1.

**Out:** weather; any health metric other than steps; network access of any kind; backup,
export and sync (D9); reminders and notifications (D20); editing calendar events, tablet/foldable
layouts, crash reporting/analytics, translations (D21); user reordering or hiding of cards (D18);
widget actions such as ticking from the widget (D19); more than one widget (D2); weekly or
N-per-week habits (D14); a habit rollover hour other than midnight (D13).

## 5. Task map

| Ticket | Delivers | Decisions |
|---|---|---|
| T0 | Gradle scaffold: `:core` + `:app`, version catalog, empty themed screen, no `INTERNET`, no backup | D3, D6, D9, D11, A2, A3, A5 |
| T1 | Pure rules in `:core`: day boundary, habit schedule and streaks, to-do bucketing, the reducer skeleton | D10, D13–D15, A7, A8 |
| T2 | Room schema v1, repositories, `AppContainer`, the single `Clock` wiring place | D10, D11 |
| T3 | **OWNER** impeccable direction round, `PRODUCT.md`, approved comps | D12, D18, A1 |
| T4 | Theme + dashboard shell + Habits (card and manage screen) | D14, D18, A4, A7 |
| T5 | To-dos (card, full list, history) | D15, A8 |
| T6 | Calendar card, permission flow, Settings screen with the calendar picker | D7, D17 |
| T7 | Steps card, Health Connect states, goal setting, privacy screen | D6, D7, D16, A9 |
| T8 | Glance widget + WorkManager refresh | D2, D8, D19 |
| O1 | Release signing, documented in `CLAUDE.md` | D5, A6 |
| T9 | Full verification chain + **OWNER** manual pass on a device | all |

## 6. SPEC patches required

None. `SPEC.md` is written new from this PRD (*v1, 2026-10-22*). Each Wave 1 ticket confirms
that its sections match what shipped, and amends them (*amended YYYY-MM-DD*) if the build forced a
change, which also needs a D-entry.

## 7. Risks recorded

- **Health Connect on Android 9–13** means the Play-installed app, its version checks, and a
  separate permission-rationale intent filter. There are many states (SPEC §5.3), and the emulator
  image is API 37, so the 28–33 path needs an older AVD or a real older phone in T9.
- **Steps may be empty.** Health Connect only has steps if some app writes them (a watch app,
  Fit, Samsung Health). The empty state has to explain this, not look broken.
- **All-day events** are stored in UTC in CalendarContract. Recurring events must be read from
  `Instances`, not `Events`. Both are classic bugs, so T6 is high-care and its rules are pure
  functions with tests.
- **No backup (D9).** A phone swap loses the owner's streaks. The owner accepted this knowingly.
- **Glance refresh.** System throttling and an OEM battery saver can delay the 30-min job. The
  on-change push is the real freshness path.
- **Shared release key (D5).** Losing the keystore means every person has to uninstall and lose
  their data. O1 documents where the key lives and how it is backed up, outside the repo.

## 8. Done, as a walkthrough

On a phone with the release APK installed:

1. First launch shows the dashboard with no system dialog. The Calendar card says it needs
   calendar access and has **Grant**. The Steps card does the same for Health Connect.
2. Grant calendar. Today's remaining events appear under an all-day strip, then tomorrow's. In
   Settings, turn off a holiday calendar, and its events disappear.
3. Add a habit "Stretch" for Mon–Fri. On a weekday it shows on the Habits card as 0 / 1. Tick it,
   and it reads 1 / 1 with streak 1. On Saturday it isn't counted, and Monday's streak carries on.
4. Add a to-do with no date and another due yesterday. Both show, the overdue one first and
   marked overdue. Tick one, and it stays struck through until midnight, then sits in Done
   history.
5. Grant Health Connect. Today's steps show against 8,000 as a ring, with 7 bars underneath.
   Change the goal to 10,000 in Settings, and the ring updates.
6. Add the widget. It shows the next event, steps / goal, and habits done / scheduled. Tick a habit
   in the app and the widget updates within seconds. Tap the widget and the app opens.
7. Deny calendar twice. The card's **Grant** now opens the system settings page for the app.
8. The phone is in airplane mode throughout, and nothing changes.
