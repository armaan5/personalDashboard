# FEEDBACK — personalDashboard

Append-only inbox. Items are never deleted or reworded; only the `Status:` line changes.
Processed by `/feedback` (`.claude/skills/feedback/SKILL.md`).

---

## F1 — 2026-10-22 — owner (chat)
> proceed

Context: said right after the dev env setup, with `CLAUDE.md` Status naming the next job as
"scope v1 into `PRD.md`". Taken as the go-ahead for the v1 scoping session. Every answer in
that session is recorded verbatim in `PRD.md` §1.

Status: shipped 2026-10-23, bef9797

## F2 — 2026-10-23 — T9 verification (emulator, API 30)
> First launch shows the calendar permission dialog.

Found in T9 on `pd_api30` after clearing app data and launching with `am start` (no input):
`GrantPermissionsActivity` for `READ_CALENDAR` is focused over the dashboard. The code does what
SPEC §5.1 says ("a source's request fires the first time its card is composed on screen in
`NotAsked`", D7 "ask per card, on first view"). Calendar is the top card (D23), so its first view
*is* first launch. That contradicts PRD §8 step 1 ("First launch shows the dashboard with no
system dialog. The Calendar card says it needs calendar access and has **Grant**") and D7's own
"Nothing is requested at first launch". The documents disagree; the owner picks which one wins.
Steps follows the same rule (`Event.StepsShown`); the dashboard is a `LazyColumn`, so its request
fires whenever its card is composed, which on a tall screen may also be first launch (not checked:
`pd_api30` has no Health Connect, so Steps sat in `NeedsInstall`).

Status: open

## F3 — 2026-10-23 — owner (T9 pass, Galaxy S25, API 37)
> When I scroll down the content at the top of the app UI sits behind the status bar making
> making it difficult to read the time. It doesnt look great mainly.

Context: the dashboard is edge-to-edge with no status-bar scrim or inset padding while
scrolled, so the date head and section content scroll under the clock and icons. Already noted
in T7's finish review (`CLAUDE.md` "Still open") and `DESIGN.md` Known Gaps; also seen on
`pd_api30` in T9. Owner asked to file it for now, not fix it.

Status: triaged 2026-10-23 → D27 (Wave 2 U1, U4)

## F4 — 2026-10-23 — owner (chat)
> add dark mode theme option in settings

Status: triaged 2026-10-23 → D27 (Wave 2 U2–U4)
