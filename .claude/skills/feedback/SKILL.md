---
name: feedback
description: Digest owner or user feedback on personalDashboard into decisions, SPEC patches, and a BACKLOG wave (PRD → SPEC → plan), then offer implementation. Logs raw feedback to FEEDBACK.md. User-invoked only — `/feedback <text>`, `/feedback log <text>`, or bare `/feedback` to digest the open inbox.
disable-model-invocation: true
---

# Feedback — personalDashboard

Turn raw feedback into a paper trail: the ask quoted verbatim, grilled into a decision table,
SPEC patches enumerated, a BACKLOG wave with runnable Accept lines, then implementation on the
owner's say-so. This skill **stops after the plan is committed.** Implementation is a separate
"go", in this session or a fresh one.

Ported 2026-10-14 from Quickmate's `/feedback`. House models to copy, not re-invent:
- Small tier: a `DECISIONS.md` D-entry with lettered rows (template in step 5). Once this repo
  has one, cite it here as the model.
- Large tier: `PRD-V<n>.md` + `PLAN-V<n>.md` (several asks, D-ids, staged plan with owner gates).
  Quickmate's `PRD-V2.md` / `PLAN-V2.md` (`~/Documents/Junkertown/Projects/quickmate/`) are the
  shape until this repo has its own.
- Tickets: `.claude/skills/to-tickets/SKILL.md` — slice rules and the ticket template. Use it
  as written; this skill does not restate it.

If `FEEDBACK.md`, `DECISIONS.md` or `BACKLOG.md` does not exist yet, create it with the shape
below on first use.

## Invocation

| Form | Does |
|---|---|
| `/feedback log <text>` | Append to `FEEDBACK.md` as `open`. Stop. No reading, no questions. |
| `/feedback <text>` | Append, then digest that item (and offer to fold in other `open` items). |
| `/feedback` | Digest every `open` item in `FEEDBACK.md`. None open → say so, stop. |

`<text>` may be a paste, a file path, or several asks at once. One ask per `F<n>` entry: split a
paste into separate asks when they would get separate decisions; keep the verbatim wording.

## FEEDBACK.md — the inbox

Append-only. Items are never deleted or reworded; only the `Status:` line changes.

```md
## F<n> — YYYY-MM-DD — owner | user (<where: chat, tester, email…>)
> verbatim text, quoted exactly, typos kept

Status: open
```

Status values, set by this skill and nothing else:
- `open`
- `triaged YYYY-MM-DD → D<n> (<wave> <labels>)` — became decisions and tickets
- `triaged YYYY-MM-DD → bug, <label>` — SPEC already pins the right behaviour
- `triaged YYYY-MM-DD → already shipped, <D-id or task>` / `→ duplicate of F<m>`
- `triaged YYYY-MM-DD → declined, <D-id or DECISIONS heading>` — owner chose not to reverse
- `shipped YYYY-MM-DD, <commit>` — set when the last ticket for it is DONE

`F<n>` numbers are stable. Next number = highest in the file + 1.

## Process

### 1. Intake

Append the item(s) to `FEEDBACK.md`. Source is `owner` unless the user says it came from someone
else. If `log`, commit nothing and stop here (the digest run commits it).

### 2. Read before asking

Read, in full where relevant, before a single question goes out:
`CLAUDE.md` (Load-bearing constraints, rejected list), `DECISIONS.md` (log, newest D-entries and
anything still open), every `PRD*.md` decision table, the `SPEC.md` sections the ask touches,
`BACKLOG.md` (status, last wave, parallelism notes), `PRODUCT.md` if copy or UI is involved,
`DESIGN.md` if visuals are.

Then read the code the ask would change. Write down, for yourself, **what the code already does**:
the file:line seams, tests that hard-code the current behaviour, lint or build checks that would
trip (`./gradlew lint`, detekt/ktlint if adopted), data-compat concerns (Room/DataStore schema
version and migrations, an installed older build on the owner's phone, persisted state across an
app update). This is what lets the grill ask real questions instead of generic ones.

If the ask is about behaviour on the device, check the device (`adb logcat`, `adb shell
dumpsys`, a screenshot via `adb exec-out screencap -p`) rather than guessing.

### 3. Triage — one AskUserQuestion round

Classify each item:

- **Bug** — `SPEC.md` already says what should happen and the app does otherwise. No decision
  needed. Goes straight to a ticket (step 6), fixed with `/tdd`, regression test first.
- **Already shipped / duplicate** — cite where. Close it.
- **Collides with a rejected or locked decision** — cite the D-id or `CLAUDE.md` line and the
  reason it was locked. Never quietly re-propose a rejected option. Offer the owner an explicit
  **reverse** or **decline**. A reversal is legitimate; it is recorded as "reverses <id>" in the
  heading.
- **New ask** — goes to the grill.

Propose the tier alongside the triage:

- **Small** — one ask (or a few tightly related), ≤ ~5 tickets, no design-direction work.
  Record: one `DECISIONS.md` entry `D<next>` with lettered rows, plus a BACKLOG wave.
- **Large** — several unrelated asks, a redesign, a new data source, or anything needing an
  impeccable direction round. Record: `PRD-V<next>.md` (one D-id per decision) +
  `PLAN-V<next>.md` + a short `DECISIONS.md` log entry pointing at both, plus a BACKLOG wave.

One AskUserQuestion: the triage per item (single-select, your classification first as
Recommended) and the tier. Owner may say "bug, but I want it different" — that promotes it to a
new ask.

### 4. Grill

Forced-choice only (`AskUserQuestion`, single- or multi-select, recommended option first, never
open prose — house rule). Up to four questions a round, several rounds fine. Batch by theme:
data/sources, domain/logic, UI, copy, permissions/privacy, battery/background work, process.

- Never ask what the docs or code already answer. Say the answer in the option description instead.
- Every option names its consequence (what changes, what it costs, what it rules out).
- Surface the uncomfortable questions the owner did not raise: runtime permissions and what
  happens on deny, background-work limits (WorkManager constraints, Doze, exact alarms),
  battery and network cost, API keys/secrets on device, offline and stale-data display, schema
  migration for an installed build, small-phone fit (360 dp wide) and large fonts, dark theme,
  WCAG 2.2 AA / TalkBack, en-GB voice, min SDK.
- Close with a **paperwork** question if the tier is borderline, and a **scope-out** multi-select
  (what an eager implementer would add; the owner ticks what is explicitly out).
- Anything you decide without asking is written down as **Assumption (not put to the owner)**.

Stop grilling when every ticket you can foresee has a rule to cite.

### 5. Record the decisions

D-ids: next after the highest in `DECISIONS.md`, `PRD.md`, `PRD-V*.md`. Check all of them.

**Small tier** — insert directly above the current newest `D<n>` entry in `DECISIONS.md`'s log:

```md
### YYYY-MM-DD — D<n>: <plain title> (reverses <id>, if any)

**Owner ask.** <verbatim quote or tight paraphrase with the quote>. From `FEEDBACK.md` F<n>.
<What was checked first — device, code — in one or two sentences.>

| # | Question | Decision |
|---|---|---|
| a | … | **Bold answer.** Detail |

**Why these answers.** <per-row reasons, only where not obvious; cite the row letter>

**Accepted.** <costs the owner knowingly took>

**SPEC.** <each § and the change, or "no change">

Tasks: `BACKLOG.md` Wave <n>, <labels>.
```

**Large tier** — `PRD-V<n>.md` with sections: 1 asks verbatim · 2 what the code already does ·
3 decisions table (D-id, question, decision, lands in) · 4 scope in/out · 5 task map · 6 SPEC
patches required · 7 risks recorded · 8 done, restated as a user walkthrough on the phone. Plus a
`DECISIONS.md` log entry: heading, two-line summary, pointer to the PRD.

### 6. SPEC patches

`SPEC.md` describes shipped behaviour and stays normative, so it is **patched by the ticket that
ships the behaviour**, not ahead of it. At this stage:

- Enumerate every patch: section, old rule, new rule. Precise enough that the ticket's
  implementer does not decide anything.
- Put each patch on the owning ticket's Files line (`SPEC.md` §x) and make its Accept check it.
- Exception: a pure clarification with no behaviour change may land in the docs commit, marked
  *amended YYYY-MM-DD*.
- Reversing a SPEC non-goal is itself a patch; name it.

### 7. Plan — BACKLOG wave

Follow `to-tickets` steps 2–5 exactly: explore, prefactors first, vertical slices, quiz
(granularity / blocking edges / merge-split), write only after approval.

Wave shape: `## Wave <n> — <title> — *added YYYY-MM-DD, see DECISIONS.md "D<n>"*`, then a
paragraph with order, what runs in parallel, owner gates, and "No new dependency" or the one
dependency the decision allowed.

Labels: pick the next capital letter not already used as a label prefix in `BACKLOG.md` (grep
`^### [A-Z]+[0-9]`); number from 0 or 1 to match the last wave. Propose it in the quiz round.

Every wave with UI work ends in an **owner manual pass** ticket (install the debug build on the
owner's phone or an emulator, walk the PRD's "done" section). Every wave's last code ticket
Accept includes the full chain: `./gradlew test` · `./gradlew lint` · `./gradlew assembleDebug`
· `./gradlew connectedAndroidTest` (on an emulator or device). Fix this list from `CLAUDE.md`
Dev environment once the scaffold pins it.

**Large tier also writes `PLAN-V<n>.md`**: context · seams found (file:line, "do not
re-derive") · stages with M / W / OWNER legend · owner input points table · risks and handling.
Self-contained: a fresh session after `/clear` executes it top to bottom.

### 8. Close the digest

1. `FEEDBACK.md` status lines → `triaged … → D<n> (Wave <n> <labels>)` etc.
2. `BACKLOG.md` Status line and `CLAUDE.md` Status: one sentence naming the D-id and wave.
   Large tier: add the new PRD/PLAN to `CLAUDE.md` "Read first".
3. Memory: update the project-state memory file if the wave changes what a fresh session must
   know; otherwise leave it.
4. Commit to `main`: `docs: scope <thing> (D<n>, Wave <n>)`. **Do not push.** Ask first.
5. Report: the decisions in one table, the frontier (tickets whose blockers are done), and one
   forced-choice question: **implement now** / **fresh session later** (give the one-line
   kickoff prompt, e.g. "Execute BACKLOG.md Wave 2 from the frontier") / **stop here**.

## Implementation — only on "go"

- Work the frontier. Each ticket through `/tdd` (its Accept line is the first red test, seams
  confirmed in one round). UI tickets through `impeccable` (native path, `reference/android.md`)
  against `DESIGN.md`.
- Serial vs parallel per the wave paragraph. Worktree subagents on `model: "opus"`; main thread
  reviews and merges.
- Each ticket applies its own SPEC patch, marked *amended YYYY-MM-DD*, and gets its DONE line
  (`— **DONE YYYY-MM-DD** (<hash>)`) when it merges.
- Push only after asking. Release builds / Play uploads only after asking.
- When the wave closes: DECISIONS "shipped" note if the wave was large, `CLAUDE.md` Status,
  `FEEDBACK.md` → `shipped YYYY-MM-DD, <commit>`.

## Don'ts

- No decision lives only in chat. If the owner answered it, it is in a table row the same day.
- No re-proposing rejected options (`CLAUDE.md` list) without the explicit reverse/decline choice.
- No SPEC edit ahead of the ticket that ships it (clarifications excepted).
- No BACKLOG write before the quiz is approved.
- No dependency beyond what a decision row names.
- No push without asking.
