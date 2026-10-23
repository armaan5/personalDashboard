# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

- **The owner**, daily. One phone, sideloaded release APK.
- **A few tech-savvy friends** (D4), on their own phones. They are comfortable with sideloading,
  runtime permissions and Health Connect, so copy can name those things plainly. They have
  never seen this app before, so every empty or blocked state still has to say what is missing
  and what fixes it without the owner standing next to them.

Job: know what today holds and keep small daily commitments, in seconds. See the next event, tick
the habit just done, clear a to-do, check steps against the goal, close the app.

## Product Purpose

One screen that answers "what's on, what's left, how am I doing today" from data already on the
phone: calendar events, the app's own habits and to-dos, and Health Connect steps. Plus one
read-only home-screen widget so most looks don't need the app at all.

Success: the owner opens it many times a day without friction, habits get ticked the moment they
are done, and nothing on screen ever looks broken when a source is missing or empty.

## Positioning

A private, single-screen daily dashboard that combines four on-device sources, with no account,
no network permission and no backup. Nothing leaves the phone, because the app physically cannot
send it (no `INTERNET` permission, D3).

## Operating Context

- **Quick glances through the day.** Open, scan, tick, close: seconds per visit. The widget
  carries most passive looking (next event, steps / goal, habits done / scheduled).
- One-handed, on the move, often between other tasks. The first screenful must answer the
  question without scrolling for the top card, and the tick targets must be easy to hit.
- Permissions are asked per card on first view (D7), never up front.
- Day boundary is local midnight (D13). Streaks count scheduled weekdays only (D14).

## Capabilities and Constraints

- Four cards in a fixed order: Calendar, Habits, To-dos, Steps (D18; T3 may reorder once). No user
  reordering or hiding.
- Calendar: today's remaining events and tomorrow's, read-only, all-day strip on top (D17).
- Habits: chosen weekdays, tick today only, streaks over scheduled days (D14, PRD A7).
- To-dos: optional due date; dashboard shows overdue, today, undated, and done-today struck
  through (D15).
- Steps: today against a daily goal (default 8,000) as a ring, plus 7-day bars, drawn with
  Compose `Canvas`, no chart library (D16, A3).
- One Glance widget, 2×2 minimum, read-only, tap opens the app (D19).
- Out of v1: weather, network of any kind, backup/export, notifications and reminders, editing
  calendar events, tablet/foldable layouts, analytics, translations (PRD §4).
- Terminology: **habit**, **to-do**, **streak**, **step goal**, **Not today**, **Done**.

## Brand Commitments

- **Name:** personalDashboard (working name).
- **Voice (SPEC §7):** en-GB spelling and conventions (`8,000`, 24-hour `HH:mm`). Plain, short,
  second person. No exclamation marks. Every empty or blocked state is one line: what is missing,
  then what fixes it.
- **Calm, no nagging.** A missed day, a broken streak or a short step count is stated neutrally or
  not at all. No guilt copy, no red alarm treatment for ordinary misses, no pleading.
- **Streaks are motivating.** Streak counts and goal progress are prominent and feel rewarding when
  they grow. Reward lives in what was achieved, never in shaming what wasn't.

## Evidence on Hand

None. No logo, icon, screenshots or user quotes exist. Do not invent testimonials or usage
figures. Example content in comps is placeholder and must read as such.

## Product Principles

1. **Answer at a glance.** Each card states its summary in its first line; detail is secondary.
2. **Never look broken.** Missing permission, missing Health Connect and empty data each get their
   own explained state, in place, with the one action that fixes it.
3. **Reward progress, don't punish misses.** Celebrate the streak and the closed ring; let misses
   pass quietly.
4. **Native first.** Material 3 behaviour, system back, system dark mode, TalkBack. It should feel
   like it belongs on Android, not like a web page in a frame.
5. **Private by construction.** On-device only is a fact of the build, stated once in About, not a
   marketing banner.

## Accessibility & Inclusion

PRD A1 bar:

- WCAG 2.2 AA contrast, in light and dark.
- TalkBack labels on every control and card; each card announces its summary (e.g. "Habits, 2 of
  3 done").
- 48 dp minimum touch targets.
- Layout holds at 360 dp width and 200 % font scale.
- Dark theme follows the system, with no in-app toggle.
- Portrait and landscape both work (landscape stretches; no tablet layouts, D21).
