# CLAUDE.md — personalDashboard

## Status

**v1 in progress.** 2026-10-22: v1 grilled into `PRD.md` (D1–D22, assumptions A1–A9),
`SPEC.md` v1 written, `BACKLOG.md` Wave 1 (T0–T9, O1) approved, owner said go. T0 (scaffold), T1 (pure rules), T2 (Room + AppContainer) done.
Frontier: **T3** (owner design gate, blocks T4) and **O1** (release signing, needs the owner's keystore). `.claude/` ported from
Quickmate 2026-10-14. No `PRODUCT.md` (T3) or `DESIGN.md` (T9) yet.

- **App name: personalDashboard** (working name, 2026-10-14). Local directory
  `~/Documents/Junkertown/Projects/personalDashboard`. No remote yet.
- **Platform: Android, native.** Kotlin + Jetpack Compose, Gradle (Kotlin DSL) (2026-10-14).
- **This is a git repo**, no remote. Delivery mode: **commit to `main` directly**, solo project,
  same as Quickmate. Ask before adding a remote or pushing.

Do not add dependencies beyond what the current backlog task or a decision row names.

## What this is

A personal Android dashboard for the owner and a few people, sideloaded. One screen, four cards
in fixed order — **Calendar** (today + tomorrow, read-only), **Habits** (weekday-scheduled, streaks),
**To-dos** (optional due date), **Steps** (Health Connect, daily goal) — plus one read-only Glance
summary widget. On-device only: no `INTERNET` permission, no backup, no notifications. Detail:
`SPEC.md`; why: `PRD.md`.

## Read first

The Quickmate document set, one job each. Create each when its first content exists, not before.

- **`SPEC.md` — *what*.** Normative. Screens, data model, data sources, refresh/sync rules,
  every state and error. If it disagrees with `DECISIONS.md` on a detail, `SPEC.md` wins.
- **`DECISIONS.md` — *why*.** Locked requirements, the stack call, rejected options, the
  decisions log (`D<n>` entries, newest first). Read before proposing anything.
- **`PRD.md` / `PRD-V<n>.md` — *what changes*.** Owner asks grilled to a decision table, scope
  in/out, task map, SPEC patches required. `PRD.md` = v1 (D1–D22).
- **`PLAN-V<n>.md` — *how a large wave gets built*.** Seams (file:line), stages, lanes, owner
  gates. Self-contained; a fresh session executes it top to bottom without re-deriving seams.
- **`DESIGN.md` — *how it looks*.** Written by the impeccable documenter from the shipped UI,
  not ahead of it. Compose theme tokens are the source; the doc describes them. *Not yet written.*
- **`PRODUCT.md` — *who it is for*.** Users, positioning, hard UI constraints, voice (en-GB),
  accessibility bar. Read before touching copy. Impeccable's `init` writes it. *Not yet written.*
- **`FEEDBACK.md` — *what was asked*.** Append-only inbox, `F<n>` items with a status line.
  `/feedback` digests open items into a D-entry or PRD, SPEC patches, and a BACKLOG wave, then
  stops for "go".
- **`BACKLOG.md` — *in what order*.** Waves of tracer-bullet tickets (`/to-tickets` format):
  Files, Blocked by, runnable Accept, Non-goals.

## Workflow (ported from Quickmate)

1. Ask arrives → `/feedback <text>` logs it to `FEEDBACK.md`, triages, grills (forced-choice
   `AskUserQuestion`, recommended first, never open prose), records decisions (small tier: one
   `DECISIONS.md` D-entry; large tier: `PRD-V<n>.md` + `PLAN-V<n>.md`), enumerates SPEC patches,
   writes the BACKLOG wave via `/to-tickets`. Stops for "go".
2. "go" → work the frontier. Each ticket through `/tdd` (Accept line = first red test). UI
   tickets through `impeccable` (native/Android mode, `reference/android.md`) against `DESIGN.md`.
3. Each ticket applies its own SPEC patch (*amended YYYY-MM-DD*) and gets its DONE line
   (`— **DONE YYYY-MM-DD** (<hash>)`) when it merges.
4. Every wave with UI work ends in an **owner manual pass** ticket on a real device or emulator.
5. Wave closes → `CLAUDE.md` Status, `FEEDBACK.md` → `shipped YYYY-MM-DD, <commit>`.

House rules: no decision lives only in chat; no re-proposing a rejected option without an
explicit reverse/decline; no SPEC edit ahead of the ticket that ships it (clarifications
excepted); no BACKLOG write before the ticket quiz is approved.

Parallel work: worktree subagents on `model: "opus"`; main thread reviews and merges.

## Load-bearing constraints

*Ratified 2026-10-22 (D10). Enforced by module split: rules live in `:core` (Kotlin/JVM, cannot
import `android.*`), plumbing in `:app` (PRD A2).*

- **Logic lives in pure Kotlin, not in Android classes.** Every rule is a case in a pure
  reducer (`reduce(state, event, now) → state + effects`) or a pure function in a module with no
  `android.*` imports. ViewModels, Activities, Workers and repositories are I/O plumbing only.
  New behaviour = new reducer case + JVM unit test, never a ViewModel method with logic in it.
- **Time and randomness are arguments.** `Instant`/`Clock` injected; `System.currentTimeMillis()`
  / `Clock.systemUTC()` appear in one wiring place only. Coroutine dispatchers injected.
- **Effects are returned, not performed.** The reducer never calls the network, the database,
  or the notification manager; plumbing executes what it returns.

**Already evaluated and rejected on record — do not re-propose:** see `DECISIONS.md` "Rejected"
(weather/any network, Hilt, SQLDelight, plain MVVM, backup/export, widget actions, up-front
permission onboarding, reminders in v1, non-midnight rollover, N-per-week habits, card reordering).
Dependency allowlist: `PRD.md` A3.

## Dev environment

Set up 2026-10-22 (Apple Silicon, macOS 26.3):

- **Android Studio 2025.2** (`/Applications/Android Studio.app`). Its bundled JDK 21 is the
  only Java on the machine — no system JRE.
- **Android SDK** at `~/Library/Android/sdk`, installed headless via `cmdline-tools/latest`
  (21.0): `platform-tools` (adb 37.0.1), `emulator` (37.2), `build-tools;37.0.0`,
  `platforms;android-36`, `platforms;android-37.0`,
  `system-images;android-37.0;google_apis_playstore;arm64-v8a`. Licences accepted.
- **`~/.zshrc`** exports `JAVA_HOME` (Studio JBR), `ANDROID_HOME`, and puts `platform-tools`,
  `emulator`, `cmdline-tools/latest/bin` on `PATH`. Non-interactive shells (Claude's Bash) don't
  read it — run via `zsh -ic '…'` or export inline.
- **AVD `pd_api37`** (Pixel 8, API 37, Play image). Boot-tested headless:
  `emulator -avd pd_api37 -no-window -no-audio` → `adb wait-for-device`.
- Harmless noise: `sdkmanager`/`avdmanager` print `line 173: test: : integer expression expected`.
- compileSdk/minSdk not chosen — that's a PRD decision; both 36 and 37 platforms are present.
- No global `gradle` — always use the wrapper `./gradlew` once the scaffold exists.
- `git` 2.39, `gh` present (from Quickmate).

Gradle tasks (confirmed T0, 2026-10-22): `./gradlew test` (`:core:test` + `:app:testDebugUnitTest`),
`./gradlew lint` (`:app:lint`), `./gradlew assembleDebug`, `./gradlew assembleRelease`,
`./gradlew connectedAndroidTest` (instrumented, needs `pd_api37` booted). Full verification chain
for a wave's last code ticket: `test` · `lint` · `assembleDebug` · `connectedAndroidTest`.
Run inline: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
ANDROID_HOME="$HOME/Library/Android/sdk"` then `./gradlew …`. A cold `~/.gradle` cache took
~20 min for the first build; warm builds ~1 min.

Build pins (T0): AGP 9.4.1 (built-in Kotlin, no `kotlin-android` plugin), Gradle 9.8.2 (wrapper,
SHA-256 pinned), Kotlin 2.4.21, compileSdk/targetSdk 37, build-tools 37.0.0, Java 17 bytecode.
Package / applicationId `dev.armaan.pd`; sources under `src/*/kotlin/dev/armaan/pd/` in both
modules. `espresso-core` is pinned to 3.7.0 as a *constraint* only: compose ui-test pulls 3.5,
which crashes on API 37 (`NoSuchMethodException: InputManager.getInstance`). Room 2.8.5 via
KSP 2.3.12 (A3 names `room-ktx`, a Room 2 artifact; Room 3 is a separate `androidx.room3`
package), schema JSON in `app/schemas/`. kotlinx-coroutines 1.11.0.

**`connectedAndroidTest` can lie.** If the app process crashes before the runner starts (e.g. in
`PdApplication.onCreate`), Gradle exits 0 with **zero tests** (seen in T2). Every instrumented
Accept check also confirms the count:
`grep -o 'tests="[0-9]*"' app/build/outputs/androidTest-results/connected/debug/*.xml` must be
non-zero and include the named test class.

## Impeccable

Ported as-is from Quickmate (it is a third-party skill; do not edit its files). Use its native
path — `reference/android.md`, `adapt.native.md`, `audit.native.md`. Its design detector hook
(`.claude/settings.local.json`, PostToolUse + Stop) only scans web file types, so on Kotlin it
is effectively silent; the finish reviewer is the slop gate for native builds. `.impeccable/` is
local state and gitignored.

## Still open

- Visual direction and final card order — T3 (owner gate).
