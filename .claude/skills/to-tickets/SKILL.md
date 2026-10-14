---
name: to-tickets
description: Break a plan, spec section, PRD decision, or the current conversation into tracer-bullet tasks in BACKLOG.md house format, each declaring what blocks it. User-invoked only.
disable-model-invocation: true
---

# To Tickets — personalDashboard

Break work into **tracer-bullet** tasks and land them in `BACKLOG.md`. The backlog is the tracker.
No GitHub issues, no `.scratch/`, no second list.

Adapted from `mattpocock/skills` → `engineering/to-tickets` via Quickmate (ported 2026-10-14).
Project specifics override the original.

## Process

### 1. Gather context

Work from the conversation. If given a reference (a PRD decision id like `D14`, a `SPEC.md`
section, a `BACKLOG.md` task id, a file path), read it in full first.

Always read `BACKLOG.md` before drafting (create it if absent: a Status line, a Build order line,
then waves): know the next free label, the wave the work belongs to, and whether an existing task
already owns part of it. Amend an existing task rather than create a duplicate; mark the
amendment `*amended <date>*` in its heading.

### 2. Explore the codebase

Confirm current state (`git log --oneline -10`, the files the task would touch). Titles and bodies
use `SPEC.md` vocabulary and respect `DECISIONS.md`. A ticket that reopens a rejected decision is
not a ticket; say so and stop.

Look for **prefactors**: a change that makes the real change easy (Quickmate's house example:
pull the rules into a pure reducer before adding moves). A prefactor is its own ticket, blocks
the rest, and is behaviour-neutral.

### 3. Draft vertical slices

<vertical-slice-rules>

- Each slice cuts a complete path: domain model → reducer case / pure function → data layer
  (repository, Room/DataStore, network) if any → ViewModel plumbing → Compose UI (if in scope)
  → test. Not one layer.
- A finished slice is verifiable on its own by a runnable check.
- Sized to one fresh context window. If it needs a "part 1 / part 2" split, split it.
- Prefactors first.

</vertical-slice-rules>

Each ticket declares **Blocked by**. Tickets that all edit the same reducer, the same database
schema, or the same Gradle build file are serial and block each other in order — say so
explicitly rather than relying on the wave note.

**Wide refactors** (package rename, Gradle module split, a domain-type retype across modules)
do not fit a tracer bullet. Sequence as expand → migrate (batched by module) → contract, each
its own ticket, contract blocked by every migrate.

**Schema changes** (Room entities, DataStore proto) carry their migration and a migration test
in the same ticket. Never "migration later".

### 4. Quiz the user

Show the breakdown as a numbered list: **label**, **title**, **blocked by**, **what it delivers**.

Then ask via `AskUserQuestion` (forced choice, recommended option first, per house preference —
never open prose):

- Granularity: right / too coarse / too fine
- Blocking edges: correct / list the wrong ones (multi-select of edges)
- Merge or split: multi-select of tickets to merge, tickets to split

Iterate until approved. Do not write to `BACKLOG.md` before approval.

### 5. Publish to `BACKLOG.md`

One `###` block per ticket, in the correct wave, in dependency order, house format below. Update
the **Build order** line near the top of `BACKLOG.md` if the new tickets change it. Do not touch
the status of existing tasks.

Labels: `T<n>` for product work (fractional `T6.5` when it must slot between existing numbers),
`O<n>` for ops, `S<n>` for side quests; later waves take a fresh capital-letter prefix (see
`/feedback` step 7). Existing numbers are stable; new work gets new labels.

<ticket-template>

### T<n> — <Title> — **high-care** (only if correctness-critical or niche API) — *added YYYY-MM-DD*
Files: `app/src/main/java/.../One.kt`, `.../Two.kt` (new), `app/src/test/java/.../OneTest.kt`.

<What to build, from the user's perspective on the phone or the data's. Two to six lines. Cite
`SPEC.md` § and PRD D-ids where the behaviour is pinned there. State the rule, not the
implementation.>

Blocked by: T<a>, T<b>. (or: none — can start now)

Accept: <a runnable check. `./gradlew test --tests '<Class>'` cases named by capability, a
`grep` that must match only X, a `connectedAndroidTest` case, a build that exits 0. Every claim
must be checkable by a command.>

Non-goals: <what an eager implementer would add and must not. Name where it lands instead.>

</ticket-template>

House conventions the template relies on:

- **Files** line lists what the task touches. Paths are allowed here (the original skill bans
  them; this backlog uses them and they are the fastest way to spot two tasks colliding on one
  file). Keep them to the Files line, not in the body.
- **Accept** is the first red test for `/tdd`. Write it so it can be transcribed into test names.
- **Non-goals** are mandatory. Every task in this backlog has them. They are how scope is held.
- A prototype snippet that encodes a decision (event sealed class, effect shape, state machine)
  may be inlined, trimmed to the decision-rich lines, marked "from prototype".

Work the **frontier**: any ticket whose blockers are all done. Announce the frontier after
publishing.
