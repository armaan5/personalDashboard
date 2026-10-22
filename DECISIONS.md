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

## Log

### 2026-10-22 — D1–D22: v1 scope

**Owner ask.** "proceed" (`FEEDBACK.md` F1), the go-ahead to scope v1 from an empty repo. Grilled
in six forced-choice rounds: content, surface, sources, audience, module rules, platform,
architecture, look, distribution, scope-out. Decisions D1–D22 and assumptions A1–A9 are in
`PRD.md` §3. Normative result in `SPEC.md` v1.

**Accepted.** No backup: a phone swap loses streaks (D9). Steps can lag 30 min on the widget (D8).
The Health Connect 28–33 path needs an older AVD or phone to verify. Losing the release keystore
forces every person to reinstall (D5).

Tasks: `BACKLOG.md` Wave 1, T0–T9 and O1.
