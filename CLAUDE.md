# CLAUDE.md — personalDashboard

## Status

**Scoping. Nothing built.** Directory created 2026-10-14; `.claude/` (skills `feedback`, `to-tickets`,
`tdd`, `impeccable`; the four impeccable agents; the impeccable hook) ported from Quickmate the
same day, adapted for Android. No `SPEC.md`, `PRD.md`, `PRODUCT.md`, `DESIGN.md` or `BACKLOG.md`
yet. First job: scope v1 into `PRD.md` (decisions D1…), then `SPEC.md`, then a BACKLOG wave.
Scaffold (Gradle project) is gated on an explicit ask.

- **App name: personalDashboard** (working name, 2026-10-14). Local directory
  `~/Documents/Junkertown/Projects/personalDashboard`. No remote yet.
- **Platform: Android, native.** Kotlin + Jetpack Compose, Gradle (Kotlin DSL) (2026-10-14).
- **This is a git repo**, no remote. Delivery mode: **commit to `main` directly**, solo project,
  same as Quickmate. Ask before adding a remote or pushing.

Do not add dependencies beyond what the current backlog task or a decision row names.

## What this is

A personal dashboard app for Android. *What it shows, for whom, and from which sources is
undecided — that is the first PRD.* Do not invent scope here; fill this section from the PRD.

## Read first

The Quickmate document set, one job each. Create each when its first content exists, not before.

- **`SPEC.md` — *what*.** Normative. Screens, data model, data sources, refresh/sync rules,
  every state and error. If it disagrees with `DECISIONS.md` on a detail, `SPEC.md` wins.
  *Not yet written.*
- **`DECISIONS.md` — *why*.** Locked requirements, the stack call, rejected options, the
  decisions log (`D<n>` entries, newest first). Read before proposing anything.
- **`PRD.md` / `PRD-V<n>.md` — *what changes*.** Owner asks grilled to a decision table, scope
  in/out, task map, SPEC patches required. *Not yet written.*
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
  Files, Blocked by, runnable Accept, Non-goals. *Not yet written.*

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

*Proposed 2026-10-14 at port time, carried over from Quickmate's architecture. Ratify or
reverse in the first PRD.*

- **Logic lives in pure Kotlin, not in Android classes.** Every rule is a case in a pure
  reducer (`reduce(state, event, now) → state + effects`) or a pure function in a module with no
  `android.*` imports. ViewModels, Activities, Workers and repositories are I/O plumbing only.
  New behaviour = new reducer case + JVM unit test, never a ViewModel method with logic in it.
- **Time and randomness are arguments.** `Instant`/`Clock` injected; `System.currentTimeMillis()`
  / `Clock.systemUTC()` appear in one wiring place only. Coroutine dispatchers injected.
- **Effects are returned, not performed.** The reducer never calls the network, the database,
  or the notification manager; plumbing executes what it returns.

**Already evaluated and rejected on record — do not re-propose:** *(none yet)*

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

Expected Gradle tasks once scaffolded (confirm in the scaffold ticket, then fix this list):
`./gradlew assembleDebug`, `./gradlew test` (JVM unit tests), `./gradlew lint`,
`./gradlew connectedAndroidTest` (instrumented, needs device/emulator). Full verification chain
for a wave's last code ticket: `test` · `lint` · `assembleDebug` · `connectedAndroidTest`.

## Impeccable

Ported as-is from Quickmate (it is a third-party skill; do not edit its files). Use its native
path — `reference/android.md`, `adapt.native.md`, `audit.native.md`. Its design detector hook
(`.claude/settings.local.json`, PostToolUse + Stop) only scans web file types, so on Kotlin it
is effectively silent; the finish reviewer is the slop gate for native builds. `.impeccable/` is
local state and gitignored.

## Still open

Everything product-side: what the dashboard shows, data sources (on-device vs. APIs, auth),
widgets vs. app, offline/refresh model, min SDK, distribution (sideload vs. Play). First
`/feedback` or a scoping session turns these into D-rows.
