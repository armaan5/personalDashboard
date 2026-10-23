---
name: personalDashboard
description: One ruled planner sheet for today; ink on stock, four source stickers.
colors:
  stock: "#F7F9F8"
  ink: "#14324F"
  ink-2: "#546A7E"
  rule: "rgba(20,50,79,0.16)"
  tint: "rgba(20,50,79,0.07)"
  track: "rgba(20,50,79,0.10)"
  scrim: "rgba(20,50,79,0.32)"
  calendar: "#2A6DB8"
  calendar-wash: "rgba(42,109,184,0.14)"
  habits: "#3E9C55"
  todos: "#E8B92F"
  steps: "#E0573E"
  night: "#0E1C2B"
  chalk: "#E6EEF2"
  chalk-2: "#A9B3BA"
  night-rule: "rgba(230,238,242,0.16)"
  night-tint: "rgba(230,238,242,0.07)"
  night-track: "rgba(230,238,242,0.12)"
  night-scrim: "rgba(0,0,0,0.5)"
  night-calendar: "#6AA5EC"
  night-calendar-wash: "rgba(106,165,236,0.18)"
  night-habits: "#5DBB72"
  night-todos: "#F0C94A"
  night-steps: "#F07A5F"
typography:
  date:
    fontFamily: "Barlow Condensed"
    fontSize: "46sp"
    fontWeight: 700
    lineHeight: "46sp"
    fontFeature: "tnum"
  headline:
    fontFamily: "Barlow Condensed"
    fontSize: "30sp"
    fontWeight: 700
    lineHeight: "34sp"
    fontFeature: "tnum"
  title:
    fontFamily: "Barlow Condensed"
    fontSize: "26sp"
    fontWeight: 700
    lineHeight: "30sp"
    fontFeature: "tnum"
  section:
    fontFamily: "Barlow Condensed"
    fontSize: "19sp"
    fontWeight: 600
    lineHeight: "22sp"
    letterSpacing: "0.06em"
    fontFeature: "tnum"
  numeral-hero:
    fontFamily: "Barlow Condensed"
    fontSize: "48sp"
    fontWeight: 700
    lineHeight: "48sp"
    fontFeature: "tnum"
  numeral-next:
    fontFamily: "Barlow Condensed"
    fontSize: "38sp"
    fontWeight: 700
    lineHeight: "40sp"
    fontFeature: "tnum"
  numeral-streak:
    fontFamily: "Barlow Condensed"
    fontSize: "30sp"
    fontWeight: 700
    lineHeight: "32sp"
    fontFeature: "tnum"
  numeral-count:
    fontFamily: "Barlow Condensed"
    fontSize: "28sp"
    fontWeight: 700
    lineHeight: "30sp"
    fontFeature: "tnum"
  time:
    fontFamily: "Barlow Condensed"
    fontSize: "19sp"
    fontWeight: 600
    lineHeight: "22sp"
    fontFeature: "tnum"
  meta:
    fontFamily: "Barlow Condensed"
    fontSize: "15sp"
    fontWeight: 600
    lineHeight: "18sp"
    letterSpacing: "0.02em"
    fontFeature: "tnum"
  caps:
    fontFamily: "Barlow Condensed"
    fontSize: "12sp"
    fontWeight: 600
    lineHeight: "14sp"
    letterSpacing: "0.06em"
    fontFeature: "tnum"
  name:
    fontFamily: "Barlow"
    fontSize: "17sp"
    fontWeight: 500
    lineHeight: "22sp"
    fontFeature: "tnum"
  body-large:
    fontFamily: "Barlow"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "22sp"
    fontFeature: "tnum"
  body-medium:
    fontFamily: "Barlow"
    fontSize: "15sp"
    fontWeight: 400
    lineHeight: "21sp"
    fontFeature: "tnum"
  body-small:
    fontFamily: "Barlow"
    fontSize: "13sp"
    fontWeight: 400
    lineHeight: "18sp"
    fontFeature: "tnum"
  label-large:
    fontFamily: "Barlow"
    fontSize: "16sp"
    fontWeight: 600
    lineHeight: "20sp"
    fontFeature: "tnum"
rounded:
  bar: "2dp"
  tag: "3dp"
  sm: "4dp"
  fab: "8dp"
  sheet: "12dp"
  widget: "22dp"
spacing:
  hairline: "1dp"
  rule: "2dp"
  gutter: "16dp"
  section-gap: "20dp"
  list-tail: "32dp"
  fab-clearance: "96dp"
  target: "48dp"
  row-todo: "52dp"
  row-settings: "64dp"
  row-habit: "68dp"
components:
  button-primary:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.stock}"
    typography: "{typography.label-large}"
    rounded: "{rounded.sm}"
    padding: "0 22dp"
    height: "48dp"
  button-text:
    textColor: "{colors.ink}"
    typography: "{typography.label-large}"
    height: "48dp"
  fab:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.stock}"
    typography: "{typography.label-large}"
    rounded: "{rounded.fab}"
    height: "56dp"
  week-tag:
    textColor: "{colors.ink}"
    typography: "{typography.meta}"
    rounded: "{rounded.tag}"
    padding: "4dp 7dp"
  now-stamp:
    backgroundColor: "{colors.calendar}"
    textColor: "{colors.stock}"
    rounded: "{rounded.tag}"
    padding: "4dp 6dp 3dp"
  all-day-bar:
    backgroundColor: "{colors.calendar-wash}"
    textColor: "{colors.ink}"
    rounded: "{rounded.sm}"
    padding: "7dp 10dp"
  day-chip:
    textColor: "{colors.ink}"
    rounded: "{rounded.sm}"
    height: "48dp"
  day-chip-on:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.stock}"
    rounded: "{rounded.sm}"
    height: "48dp"
  habit-tick-done:
    backgroundColor: "{colors.habits}"
    textColor: "{colors.stock}"
    size: "34dp"
  todo-check-done:
    backgroundColor: "{colors.todos}"
    textColor: "{colors.ink}"
    rounded: "{rounded.sm}"
    size: "24dp"
  text-field:
    textColor: "{colors.ink}"
    typography: "{typography.body-large}"
    rounded: "{rounded.sm}"
    height: "52dp"
  sheet:
    backgroundColor: "{colors.stock}"
    textColor: "{colors.ink}"
    rounded: "{rounded.sheet}"
    padding: "0 16dp 16dp"
  snackbar:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.stock}"
    rounded: "{rounded.sm}"
  widget:
    backgroundColor: "{colors.stock}"
    textColor: "{colors.ink}"
    rounded: "{rounded.widget}"
    padding: "12dp 12dp 10dp 14dp"
---

# Design System: personalDashboard

*Written 2026-10-23 (T9) from the shipped Compose code at `134e648`. The theme in
`app/src/main/kotlin/dev/armaan/pd/ui/theme/` is the source; this file describes it. Where the
code and `design/t3/comps.html` (D23) disagree, the code is recorded and the divergence noted.
Values are dp / sp; colour keys without `night-` are the light scheme, `night-*` its dark pair.*

## Overview

**Creative North Star: "The Wall Planner"**

A UK office wall planner crossed with a don't-break-the-chain calendar (D23). The app is one
ruled sheet of paper, not a stack of cards: each section opens with a heavy ink rule, rows are
divided by hairlines, and nothing floats except what Material must lift (FAB, snackbar, sheet,
dialog). Two values per scheme, a stock and an ink, carry every neutral and every action; four
sticker colours mean a source or an achievement and nothing else.

Condensed numerals do the talking. The date, the next event's start, the streak, the step count
and the head counts are set large in Barlow Condensed with tabular figures, so a glance reads
numbers first and prose second. Density is planner-like but not spreadsheet-like: one 16 dp
gutter, 20 dp between sections, every target at least 48 dp.

Light and dark follow the system. No Dynamic Color (D23, rejected). The rejected looks are the
stock Material 3 card stack ("generic Material template"), gamified/fitness-loud, and a dense
spreadsheet.

**Key Characteristics:**
- One ruled sheet: 2 dp ink rule opens each section, 1 dp `rule` hairline between rows, no cards.
- Ink is the only action colour; stickers mark source identity and done-ness.
- Condensed, tabular numerals at display sizes; Barlow for names and body.
- 4 dp corners almost everywhere.
- One signature motion: the habit tick pressed on like a sticker.

## Colors

Ink on stock, every neutral a fixed alpha of the ink, plus four planner stickers.

### Primary
- **Planner Ink** (`ink` / `chalk`): all text, all actions (filled buttons, FAB, selected day
  chip, switch track, date-picker selection), section rules, the today outline on step bars.
  Mapped onto every Material primary/secondary/tertiary role so stock components land in ink.

### Secondary (source stickers)
- **Calendar Blue** (`calendar` / `night-calendar`): Calendar's head dot, duration rules, the Now
  stamp, the text cursor. **Calendar Wash** (`calendar-wash`) fills all-day bars only.
- **Chain Green** (`habits` / `night-habits`): Habits' head dot, a done tick, done chain days.
- **Note Yellow** (`todos` / `night-todos`): To-dos' head dot, a done to-do box, the overdue
  pennant. Ink sits on it in both schemes (`onTodos` = light `ink`).
- **Tomato** (`steps` / `night-steps`): Steps' head dot, the ring's progress arc, bars that met the
  goal.

### Neutral
- **Stock** (`stock` / `night`): every background, including all Material surface containers,
  which are flattened to stock. Also `onInk`, the text on ink.
- **Ink 72 %** (`ink-2` / `chalk-2`): secondary text, the weekday over the date, unticked box and
  tick outlines, chevrons, meta beside a count (5.3:1 on light stock).
- **Rule** (`rule`, ink 16 %): row hairlines and Material `outlineVariant`.
- **Weekend Tint** (`tint`, ink 7 %): an unscheduled chain day; Material `surfaceVariant` /
  `secondaryContainer`.
- **Track** (`track`, ink 10 % light, 12 % dark): ring track and below-goal bars.
- **Scrim** (`scrim`): behind bottom sheets.

### Named Rules
**The Ink Acts Rule.** Every tappable control is drawn in ink. A sticker colour is never an
action colour.

**The Sticker Means Something Rule.** A sticker colour appears only as its source's identity or as
that source's achievement (done, met). Never decorative, never for state on another source.

**The Calm Miss Rule.** Overdue and missed are ink and ink-2 with a yellow pennant; no red alarm
treatment. Tomato means steps progress, not danger.

## Typography

**Display Font:** Barlow Condensed 500/600/700 (bundled `res/font`, OFL)
**Body Font:** Barlow 400/500/600 (bundled `res/font`, OFL)

**Character:** Condensed planner lettering for heads, times, dates and numerals; the plain
humanist Barlow for things people named (habits, to-dos, events) and for sentences. Every style
sets `tnum`, so counts and times never jitter.

### Hierarchy
- **Date** (700, 46/46): the masthead date only. Material `displaySmall`.
- **Numerals**: steps count (700, 48/48, auto-shrinks to 24 sp on one line rather than wrap);
  next event start (700, 38/40); streak (700, 30/32); head count (700, 28/30).
- **Headline** (700, 30/34): a screen's top-bar title.
- **Title** (700, 26/30): a sheet's title, About's sub-heads.
- **Section** (600, 19/22, +0.06 em, capitals): every section head. Material `titleMedium`.
- **Time** (600, 19/22): a timed event's start in its 52 dp column.
- **Meta** (600, 15/18, +0.02 em): "done", "left", "Goal 8,000" beside a head count; the week tag
  (+0.04 em).
- **Name** (Barlow 500, 17/22): habit names, settings row titles, calendar names.
- **Body** (Barlow 400, 16/22 large, 15/21 medium, 13/18 small): empty and blocked lines,
  "to go", settings hints. Blocked lines cap at 280 dp (about 34 characters); empty lines at
  340 dp.
- **Caps** (600, 12/14, +0.06 em, capitals): small labels that name what follows: weekday letters,
  "streak", field labels in sheets. Sibling small-capitals roles: sub-head "Tomorrow" (14, +0.07),
  "OVERDUE" (700, 14, +0.07), "ALL DAY" (13, +0.05), the Now stamp (700, 13, +0.08).
- **Label large** (Barlow 600, 16/20): button and FAB text.

### Named Rules
**The Numbers First Rule.** The figure a glance is for (date, next start, streak, steps) is the
largest thing in its section, condensed and tabular. Its unit or context sits beside it in meta,
in ink-2.

**The Named Things Are Barlow Rule.** Anything the person typed or a calendar supplied is set in
Barlow, never condensed and never in capitals.

## Layout

A single column, edge-to-edge (`enableEdgeToEdge`), insets applied as content padding. One 16 dp
gutter left and right; sections stack 20 dp apart. The masthead sits above the first section:
weekday in ink-2 over the date, the ISO week tag and Settings bottom-aligned to the right.
Dashboard section order is fixed: Calendar, Habits, To-dos, Steps (D18).

Rows: section head 48 dp minimum; habit row 68 dp (48 dp tick, 12 dp gap, name over chain,
56 dp streak column); to-do row 52 dp (48 dp box, 8 dp gap, title, due label at the end); timed
event 52 dp time column + 10 dp gap; settings and calendar rows 64 dp. Lists end 32 dp past the
bottom inset, or 96 dp on screens with a FAB. Habit chain: 14 dp marks, 6 dp apart, Monday
first, under a shared weekday-letter row with today underlined. Steps: 112 dp ring with a 12 dp
stroke beside the count, 20 dp apart; 92 dp bar chart of seven 24 dp bars, today last.

Phone portrait only; no tablet or foldable layout (PRODUCT.md).

## Elevation & Depth

Flat. The dashboard and every screen are one sheet; depth is drawn with rules, not shadows. The
section rule is 2 dp ink; row dividers are 1 dp `rule`. Only Material's floating parts lift,
with their default elevation: the extended FAB, the snackbar, the date-picker dialog. The bottom
sheet is stock over the `scrim`, its top edge opened by the same 2 dp ink rule as a section.

### Named Rules
**The No Cards Rule.** No container gets a background, border or shadow to separate it from the
page. A section is a rule and a head.

## Shapes

Gently squared: 4 dp corners on buttons, fields, chips, the to-do box, all-day bars, snackbar
(Material small/medium/extraSmall all 4 dp). 3 dp on the two inline tags (week tag, Now stamp);
2 dp on step bars and duration rules. 8 dp on the FAB (Material large), 12 dp on a sheet's top
(extraLarge). Circles only for things that are dots or rings: source dots (10 dp in heads, 8 dp
in the widget), the habit tick, chain marks, the steps ring. The widget card alone uses 22 dp to
sit with launcher widgets. The overdue pennant is a 10 × 9 dp yellow triangle.

## Components

### Buttons
- **Filled** (Grant access, Get Health Connect, Save): ink on 4 dp, 48 dp tall, 22 dp side
  padding. Disabled uses Material's ink-at-alpha defaults.
- **Text** (Add a habit, Add a to-do, Clear history, Delete, dialog OK/Cancel): ink, with a
  leading 24 dp icon; the dashboard's add actions are underlined like a planner note.
- **Icon** (Back, Settings, stepper −/+, clear date): 48 dp, ink; disabled ink at 38 %.
- **FAB**: extended, ink with stock text and Add icon, 8 dp corners, on Habits and To-dos screens.

### Section head
Optional 10 dp source dot, capitals title, optional meta (count + unit in ink-2), chevron in ink-2
when it opens a screen. Announced as one heading node carrying the section's summary.

### Habit tick (signature)
48 dp target around a 34 dp circle. Open: 2 dp ink-2 ring. Done: green disc with a 22 dp stock
check, resting at −3°. Ticking presses it on: scale 1.12 → 1 and tilt 0 → −3° over 200 ms,
LinearOutSlowIn; untick snaps. Instant when system animations are off. A habit not scheduled today
shows no tick and its name in ink-2.

### Habit chain
Seven 14 dp marks: done = green disc; missed = 1.5 dp ink-2 ring; unscheduled = tint square,
2 dp corners; today open = 1.5 dp ink ring with an ink centre dot; ahead = dashed ink-2 ring at
70 %. Decorative; the row's description carries the streak.

### To-do box
48 dp target around a 24 dp, 4 dp-cornered box. Open: 2 dp ink-2 outline. Done: yellow fill, ink
18 dp check, −3° (static). Done titles are struck through in ink-2. Overdue reads pennant +
"OVERDUE" in heavy ink capitals; other dates in meta-weight ink-2 ("Mon 26 Oct").

### Calendar rows
All-day: wash bar, 4 dp, name left, "ALL DAY" right. Next event: start in numeral-next, "until
HH:mm" in ink-2, Now stamp when in progress, title in Barlow 500 20/25, then a 3 dp calendar
duration rule (two hours fills the row) with a 2 × 9 dp ink tick at now. Later events: 19 sp time
column, Barlow title, 2 dp duration rule. "TOMORROW" sub-head in ink-2 capitals.

### Steps
Ring: `track` circle, tomato arc from 12 o'clock, butt caps. Bars: met = tomato; below goal =
track with a 1.5 dp ink-2 cap (WCAG 1.4.11); today = 1.5 dp ink outline, at least a 3 dp stub.
Dashed ink-2 goal line (4/3 dp) labelled at its right end, knocked out of any bar it crosses;
1 dp ink baseline; narrow weekday letters below, today in ink.

### Chips
Weekday chips in the habit sheet: seven equal 48 dp squares, 6 dp apart, 4 dp corners. Off:
1.5 dp ink-2 outline, ink letter. On: ink fill, stock letter.

### Inputs / Fields
- **Style:** Material outlined field, 4 dp, ink outline at rest and focused, ink-2 label,
  calendar-blue cursor; helper text and a character count below.
- **Date field:** 52 dp row, 1 dp ink outline, 4 dp, event icon in ink-2, date or "No date" (ink-2),
  clear button when set. Opens the Material date picker on stock, selected day in ink.
- **Switch** (Settings → Calendars): on = ink track, stock thumb; off = stock track, ink-2 thumb
  and outline. The whole 64 dp row toggles.

### Navigation
No bar or drawer. The dashboard masthead holds Settings; section heads open their screens. Each
screen has a 64 dp top bar: Back, then the title in headline type as a heading.

### Sheet
Modal bottom sheet, skip-partially-expanded, 12 dp top corners, stock on scrim. Top edge: 2 dp
ink rule, then the drag handle in ink-2. Title in title type, 18 dp below; actions right-aligned
26 dp below the last field.

### Snackbar
Material snackbar on the inverse roles: ink container, stock text, 4 dp; Undo action in stock.

### Widget (Glance)
2×2 minimum, resizable, read-only; tap opens the dashboard. Stock card with 22 dp corners (a
drawable, so it rounds below API 31). Header "Thu 22" bold 19 sp over "Oct" 15 sp ink-2, a 2 dp
ink rule, then three equal lines (next event, steps, habits) split by 1 dp hairlines: 8 dp source
dot, value bold 18 sp, optional unit 15 sp ink-2, optional "Open to set up" hint 12 sp ink-2 on
the same line.

## Do's and Don'ts

### Do:
- **Do** open every new section or sheet with the 2 dp ink rule and a capitals section head.
- **Do** draw every action in ink, 48 dp minimum, 4 dp corners.
- **Do** set figures in Barlow Condensed with `tnum`; let a hero figure shrink to fit on one line
  rather than wrap.
- **Do** give every empty or blocked state one Barlow line (max 280–340 dp) and its one ink action.
- **Do** take colours from `Pd.colors` and type from `Pd.type` / `MaterialTheme.typography`; a
  new Material component inherits the world through the ink/stock role mapping.

### Don't:
- **Don't** put content in Material cards, tonal containers or shadows (D23: "generic Material
  template").
- **Don't** use a sticker colour for an action, a warning, or another source's state.
- **Don't** use red or alarm styling for overdue, missed or short; the pennant and ink capitals
  are the whole treatment.
- **Don't** enable Dynamic Color or derive colours from the wallpaper (D23, rejected).
- **Don't** add an icon library; icons are Material outlines drawn as paths in `PdIcons`
  (24 dp grid, A3).

## Known Gaps

Recorded for the T9 owner pass; not fixed here.

- **Edge-to-edge status bar.** The dashboard scrolls under the status bar with no scrim, so
  content collides with the system icons.
- **Calendar time at 200 % font.** A timed row's start wraps inside its 52 dp column ("09:" /
  "00").
- **Widget picker preview.** The picker shows Glance's loading layout, not a preview: no
  `previewImage` or `previewLayout` in `dashboard_widget_info.xml`.
- **Widget at 200 % font.** At 2×2 every line ellipsises ("13:00 L…"); readable once resized.
- **Widget type is a fallback.** RemoteViews can't load bundled fonts, so the widget sets
  `sans-serif-condensed` (system) where the comp has Barlow Condensed. A platform limit, not a
  type choice; don't copy it into the app.
- **`surfaceTint` unmapped.** `Theme.kt` copies Material's baseline scheme without overriding
  `surfaceTint`, so a tonally elevated stock surface (e.g. the date-picker dialog) may take
  Material's baseline tint. Unverified on device.

### Divergences from the T3 comp (code recorded above)
- Habit chain "today, open": comp is an ink ring inside a stock gap and outer ink ring; code is an
  ink ring with a filled ink centre dot. "Ahead": comp dotted, code dashed.
- Step bars: code adds a 1.5 dp ink-2 cap to below-goal bars and a 3 dp stub for today at zero;
  the goal label is 12 sp untracked (comp 11 sp, +0.05 em) and knocked out of bars.
- Text fields: comp has a 1.5 dp ink border with the label inside in caps; code uses Material's
  outlined field (1 dp at rest, 2 dp focused) with a Barlow floating label.
- Snackbar action: comp underlines Undo; code uses Material's plain action text.
- Widget: comp puts the set-up hint on a second line under the dash; code puts it beside the dash
  so three lines fit a 2×2. Widget type as above.
- Streak label "STREAK": comp 500 weight, code 600 (caps role).
