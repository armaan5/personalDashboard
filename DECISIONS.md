# DECISIONS — personalDashboard

The *why*. `SPEC.md` is the *what* and wins on detail. Read the log before proposing anything.
Never re-propose a rejected option without an explicit reverse.

## Locked requirements

- On-device only: no `INTERNET` permission, no network code (D3).
- Logic in pure Kotlin (`:core`, no `android.*`), with time injected and effects returned (D10,
  PRD A2).
- Sideloaded: no Play Store work (D4, D5).

## Stack

Kotlin, Jetpack Compose, Gradle KTS, minSdk 28 (D6). Room + Preferences DataStore, manual DI
(D11). Glance + WorkManager for the widget (D2, D8). Health Connect for steps. CalendarContract
for events. Dependency allowlist: `PRD.md` A3.

## Rejected (do not re-propose)

| Option | Rejected by | Why |
|---|---|---|
| Weather card / any keyless API | D1, D3 | Owner: "calendar but no weather"; data is on-device only |
| Hilt / Dagger | D11 | Manual `AppContainer` is enough at this size |
| SQLDelight | D11 | Room chosen |
| Plain MVVM without the reducer | D10 | Architecture ratified as written |
| Auto Backup / JSON export | D9 | Owner chose no backup |
| Habits-only or to-dos-only | D14, D15 | Owner chose both |
| Ticking habits on the widget | D19 | Summary widget, read-only |
| Onboarding that asks all permissions up front | D7 | Ask per card |
| Firebase App Distribution, debug-APK sharing | D5 | Signed release APK |
| Notifications / reminders in v1 | D20 | Out of v1. A later wave may add them through a new D-entry, not a reversal |
| Configurable or 04:00 day rollover | D13 | Local midnight |
| N-per-week habits | D14 | Chosen weekdays |
| Reorder/hide cards | D18 | Fixed order |
| Dynamic Color (Material You) scheme | D23 | Own fixed palette chosen |
| Stock Material 3 card-stack look | D23 | Owner: "generic Material template" |

## Log

### 2026-10-23 — D25: Health Connect permission rule and test seam (T7)

**Owner ask.** T7 seam round (`/tdd`). Health Connect has no `shouldShowRequestPermissionRationale`;
after two refusals its dialog silently stops appearing. The Accept test must insert a
`StepsRecord` through `HealthConnectClient`, but test code runs in the app's process, so the
insert needs the app's own write grant.

**Decided.**
- **Repeat-refusal rule** (owner pick): a Health Connect request that comes back refused while
  `asked_health` was already true when it fired means `PermanentlyDenied`, for this process only.
  After a restart it reads `Denied` again; one **Grant access** tap re-requests, Health Connect
  returns at once, and the copy switches to "turn it on in Health Connect's settings". No new
  DataStore key. A grant clears it.
- **Debug-only `WRITE_STEPS`** (owner pick) in `app/src/debug/AndroidManifest.xml`. The release
  manifest stays read-only; app code never writes (D1, D16).
- The test grants through Health Connect's own per-app page (`ACTION_MANAGE_HEALTH_PERMISSIONS`,
  "Allow all", driven with the platform `UiAutomation`, no new test library), not `pm grant`.
  Seen on `pd_api37`: after `pm grant`, `readRecords` returned the record but `aggregate` returned
  null with no data origins. Only Health Connect's own grant path puts the app on its
  data-priority list, and `aggregate` ignores apps that aren't on it. Real step sources (a watch
  app, Fit) are granted through that path, so the app keeps `aggregate` as SPEC §5.3 says.
- The 7-day read uses whole local days, `[today − 6, tomorrow)`: with the range ending at `now`,
  `aggregateGroupByPeriod` dropped today's partial slice (seen on device: today's bar was empty
  while the ring showed the count).
- Other instrumented tests set `asked_health` (or grant read) up front, so Health Connect's
  request doesn't cover the dashboard, as D24 did for calendar.
- The Accept's grep for `SHOW_PERMISSIONS_RATIONALE\|VIEW_PERMISSION_USAGE` prints `3`, not `2`:
  the alias's guard, `android:permission="android.permission.START_VIEW_PERMISSION_USAGE"`, also
  matches. Health Connect's documented alias carries that guard (only the system may start it),
  so it stays. Both rationale entries are present.
- **For T8:** Health Connect only allows reads in the foreground unless the app holds
  `READ_HEALTH_DATA_IN_BACKGROUND`. The widget Worker's steps line will read nothing in the
  background. T8 decides (a dash, the last foreground value, or the background permission through
  a D-entry).

**SPEC patches.** §2, §4.4, §5.1, §5.3, §6.1 (*amended 2026-10-23*, T7).

### 2026-10-22 — D24: calendar permission details and test seam (T6)

**Owner ask.** T6 seam round (`/tdd`). Revoking a runtime permission kills the process under
test, so the Accept's "permission revoked" case can't be driven for real.

**Decided.**
- `AppContainer` takes an optional `calendarGranted: () -> Boolean` (default: the real
  `checkSelfPermission`). Instrumented tests fake the blocked state through it; the granted case
  grants `READ_CALENDAR` for real and inserts events through `CalendarContract` under the shell's
  identity (`adoptShellPermissionIdentity`), so the app never declares `WRITE_CALENDAR` (D21).
- While the system dialog is up, `asked_calendar` is already set but nothing is answered, so the
  state reads `Denied`, not `PermanentlyDenied`, until the next check (seen on device: the card
  showed the permanent-deny copy behind the dialog).
- The calendar provider is observed only while access is granted: registering the observer
  without `READ_CALENDAR` throws (seen on device as a first-launch crash).
- The `Instances` query window is padded a day either side; `:core` drops what falls outside
  today and tomorrow, so all-day rows never depend on the provider's zone handling.
- Settings → Calendars is its own back-stack screen (`Screen.Calendars`) under Settings (PRD A4).

**SPEC patches.** §4.4, §5.1, §5.2, §6.1 (*amended 2026-10-22*, T6).

### 2026-10-22 — D23: visual direction "The Wall Planner" (T3)

**Owner ask.** T3 design gate (D12). Impeccable native/Android round, seed `b0779a3e`, Operate
mode. Owner answers: users are the owner plus tech-savvy friends; use is quick glances through
the day; brand commitments "calm, no nagging" and "streaks are motivating" (`PRODUCT.md`); an own
fixed palette, not Dynamic Color; to feel wrong it would be a generic Material template,
gamified/fitness-loud, or a dense spreadsheet.

**Decided.** The owner picked the rolled direction **The Wall Planner** (a UK office wall planner
crossed with a don't-break-the-chain calendar) over the Desk Appliance pick, the Ticket Wallet
challenger and the Material 3 standard, and approved the comps as drawn:

- Comp files: `design/t3/comps.html` (source of truth for tokens) and the captures
  `design/t3/{light,dark}-{dash,first,habits,sheet,widget}.png`, `design/t3/sheet.png`.
- Two values per scheme (light stock `#F7F9F8` / ink `#14324F`; dark ground `#0E1C2B` / chalk
  `#E6EEF2`), every neutral a fixed tint of the ink. Four sticker colours, one per source, used
  only for source identity and achievement. Every action is drawn in ink.
- No cards, no elevation: one ruled sheet, 4 dp corners.
- Type: Barlow Condensed + Barlow, bundled as `res/font` in T4 (resources, not a library; no
  downloadable-fonts provider).
- Habits show this week's Mon–Sun chain under a shared letter row.
- Signature tick: a sticker pressed on, instant when animations are off.
- **Card order: D18 kept** (Calendar → Habits → To-dos → Steps). SPEC §6.1 is unchanged.

**Rejected.** Dynamic Color / wallpaper-derived scheme (owner chose an own palette). The stock
Material 3 card stack (owner: "generic Material template").

**Accepted.** `DESIGN.md` is still written from the shipped UI in T9. The direction contract
lives in the local impeccable surface brief (`.impeccable/surfaces/`, gitignored). The comp
sheet loads Barlow from Google Fonts in a browser only; the app never does (D3).

### 2026-10-22 — D1–D22: v1 scope

**Owner ask.** "proceed" (`FEEDBACK.md` F1), the go-ahead to scope v1 from an empty repo. Grilled
in six forced-choice rounds: content, surface, sources, audience, module rules, platform,
architecture, look, distribution, scope-out. Decisions D1–D22 and assumptions A1–A9 are in
`PRD.md` §3. Normative result in `SPEC.md` v1.

**Accepted.** No backup: a phone swap loses streaks (D9). Steps can lag 30 min on the widget (D8).
The Health Connect 28–33 path needs an older AVD or phone to verify. Losing the release keystore
forces every person to reinstall (D5).

Tasks: `BACKLOG.md` Wave 1, T0–T9 and O1.
