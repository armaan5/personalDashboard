---
name: tdd
description: Test-driven development for personalDashboard (Kotlin, Android). Use when building a backlog task or fixing a bug test-first, when the user says "red-green", "tdd", or "write the test first", or when a task's Accept line names a `./gradlew test` case.
---

# TDD — personalDashboard

Red → green, one slice at a time. This file makes the loop produce tests worth keeping in this
repo: where the seams are, what counts as an independent expected value, what the runner allows,
and what is banned.

Adapted from `mattpocock/skills` → `engineering/tdd` via Quickmate (ported 2026-10-14).
Project specifics override the original. **The seam table below is provisional** until the
scaffold ticket lands; that ticket replaces the placeholder paths with real ones.

## Read first

- `SPEC.md` — the source of truth for every expected value. A test's expected output comes from
  a section of `SPEC.md`, not from running the code and pasting what it printed.
- `BACKLOG.md` — the task's **Accept** line is the first red test. Start there.
- `DECISIONS.md` — what has been rejected. Don't write a test that assumes a rejected design.

Test names use `SPEC.md` vocabulary, whatever it settles on. Not synonyms.

## Seams in this repo

Tests live at public boundaries. In order of preference:

| Seam | Driven by | Runner |
|---|---|---|
| Pure domain module (models, formatters, parsers, calculations) — no `android.*` imports | direct call | JVM unit test, `./gradlew test` |
| `reduce(state, event, now)` | build a `State`, call `reduce`, assert `{ state, effects }` | JVM unit test |
| Data-source parsers / mappers (API JSON → domain) | fixture string → domain object | JVM unit test |
| Repository + Room DAO / DataStore | in-memory Room DB, real DAO | Robolectric or `connectedAndroidTest` |
| ViewModel plumbing | `runTest` + fake repository, assert `StateFlow` emissions | JVM unit test with `kotlinx-coroutines-test` |
| Compose screen | `createComposeRule()`, semantics finders (`onNodeWithText`, `onNodeWithContentDescription`) | `connectedAndroidTest` (or Robolectric if adopted) |

**Test at the reducer, not at the ViewModel.** Everything that can be asserted on `reduce` or a
pure function is asserted there. ViewModel tests cover only wiring: the event reaches `reduce`,
the state reaches the `StateFlow`, effects reach the repository. Compose tests cover only what
the logic layer cannot see: the right thing on screen, semantics for TalkBack, click → event.

**Confirm seams before writing.** Before the first red test of a task, list the seams under test
and the cases planned at each, and confirm via `AskUserQuestion` (multi-select of the proposed
cases, recommended set first). One round. No test lands at an unconfirmed seam.

## Runner rules

- JUnit + `kotlin.test` assertions (`assertEquals`, `assertIs`). Coroutines: `runTest` from
  `kotlinx-coroutines-test`, `StandardTestDispatcher` injected. No other test libraries unless a
  decision row names one (MockK, Turbine, Truth, Kotest are *not* adopted by default).
- JVM tests under `src/test/`, instrumented under `src/androidTest/`. Anything that can run on
  the JVM runs on the JVM.
- Fixtures: a `base()` factory per test file returning a `State` with defaults; use `copy(...)`
  for overrides. Do not share a mutable fixture across tests.
- Time is an argument. `now: Instant` (or an injected `Clock`) is passed explicitly. Tests never
  read the system clock and never sleep; delays use `advanceTimeBy` on the test dispatcher.
- Randomness (IDs) goes through an injected generator; tests assert shape, not value.
- API fixtures are checked-in files (`src/test/resources/`), captured once from the real source
  and trimmed. No live network in tests.

## What a good test is here

See [tests.md](tests.md) for worked examples and [mocking.md](mocking.md) for the boundary
rules. Short form:

- **Asserts on state or effects**, never on internals. `effects` is the contract with I/O;
  `state` is what the UI renders. Both are public by spec.
- **Expected value from `SPEC.md`.** A literal you could check by hand, not the output of the
  same code path.
- **One capability per test**, named after it with backticks:
  `` fun `refresh - stale cache shows last value with stale badge`() ``.
- **Order is part of the assertion** where `SPEC.md` orders it. Assert the `effects` list in
  order, not `contains`.

## Anti-patterns

- **Implementation-coupled**: verifying calls on a mock, reading private fields via reflection,
  counting invocations. Tell: refactor breaks the test, behaviour didn't change.
- **Tautological**: expected value computed the way the code computes it.
- **Horizontal slicing**: all tests for a task first, then all implementation. Write one, make it
  pass, write the next. The second test responds to what the first taught you.
- **Snapshot dumping**: `assertEquals(expectedWholeState, out.state)` when one field changed.
  Assert the field; assert the *rest* unchanged only where the spec says so.
- **Instrumented when JVM would do**: a `connectedAndroidTest` for logic that has no Android
  dependency. Move the logic out and test it on the JVM.

## Rules of the loop

1. **Red first.** Write the failing test. Run `./gradlew test --tests '<Class>'`. See it fail for
   the right reason (assertion, not compile error).
2. **Green with the least code.** No speculative branches for the next test.
3. **Repeat.** Next case from the Accept line, then edge cases the spec names.
4. **Refactor is a separate step**, after the task's Accept line is fully green. Use
   `/simplify` or `/code-review`, not the red-green cycle.
5. **`./gradlew lint` before commit**, and `connectedAndroidTest` if the ticket touched UI or
   the database.

## Per-task checklist

- [ ] Accept line from `BACKLOG.md` transcribed into test names
- [ ] Seams confirmed (one `AskUserQuestion` round)
- [ ] Each expected value traceable to a `SPEC.md` section (cite in a comment when not obvious)
- [ ] System clock read in one wiring place only (grep `System.currentTimeMillis`, `Clock.system`,
      `Instant.now()`)
- [ ] `./gradlew test` and `./gradlew lint` clean
