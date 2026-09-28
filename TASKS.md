# Osyster - Tasks

> **Project:** Osyster
>
> **Version:** 0.2.9
>
> **Last Updated:** 2026-09-28

Planned work, grouped by version. Items carried from the old app are marked **Parity**: the redesign keeps every interaction people relied on, except features dropped on purpose (listed at the end).

---

## 1. v0.2.9: Parity with the old app

- [x] **PAR-0001 - Live facts on Home rows**
  - **Location:** `feature/home`, `core/device/domain`, `core/device/data`
  - **Current:** Each device page row shows a fixed description such as "Health, charge, temperature".
  - **Change:** Show a live one-line fact per page, as the old dashboard cards did: battery level and charging state, RAM in use, Android version and patch, resolution and refresh rate, camera and sensor counts, active connection. Pinned Data usage, Screen usage, and Stay awake rows show today's total or time left.

- [x] **PAR-0002 - CPU and RAM trend sparklines**
  - **Location:** `core/ui` (`HeroCard`), `feature/home`
  - **Current:** The hero card shows single readings.
  - **Change:** Draw a short sparkline of recent CPU load and RAM use under each reading, kept in memory while Home is visible.

- [x] **PAR-0003 - Drag to reorder Home**
  - **Location:** `feature/home`
  - **Current:** Rows move one step at a time from the long-press menu.
  - **Change:** An Edit Home mode with drag handles to reorder pages and pinned items, plus show and hide switches, as the old dashboard's edit mode had.

## 2. v0.2.9: Outside the roadmap

- [x] **FEAT-0001 - Widgets page**
  - **Location:** `feature/musicwidgets`, `feature/datausage`, new widgets page
  - **Current:** The Widgets tool opens a placeholder.
  - **Change:** List every Osyster widget with a preview and an Add button that asks the launcher to pin it.

- [x] **FEAT-0002 - Glance stack widget**
  - **Location:** new widget
  - **Current:** Widget stack is "coming soon".
  - **Change:** One home-screen widget that swipes between battery, memory, data today, and screen time cards.

- [x] **FEAT-0003 - Color picker tile**
  - **Location:** new feature
  - **Current:** Deferred.
  - **Change:** A tile that captures the screen once (with Android's own screen capture prompt), shows it full screen with a magnifier, and copies the picked color as hex.

- [x] **FEAT-0004 - Two panes on wide screens**
  - **Location:** `app` shell
  - **Current:** Every screen is one column at any width.
  - **Change:** On tablets and unfolded foldables, keep the tab list on the left and open pages on the right.

- [x] **FEAT-0005 - Back up power tool lists**
  - **Location:** `app/preferences/AppSettingsBackup.kt`
  - **Current:** Settings export leaves out the Shut-Up! and Freeze lists.
  - **Change:** Include them, and keep reading older export files.

- [x] **FEAT-0006 - Stay awake grant through Shizuku or root**
  - **Location:** `feature/stayawake`
  - **Current:** While charging mode only offers the ADB command.
  - **Change:** Offer the one-tap grant when Shizuku or root is set up.

- [x] **PAR-0004 - Settings the old app had**
  - **Location:** `feature/settings`, `core/device`
  - **Change:** Temperature unit, live refresh speed, and block screenshots.

- [x] **PAR-0005 - Data usage history and search**
  - **Location:** `feature/datausage`, `core/usage`
  - **Change:** Previous and next period navigation, and search in the app list.

- [x] **PAR-0006 - App stopper actions**
  - **Location:** `feature/appstopper`
  - **Change:** Open app, App info, store listing or reinstall, and status counts.

## 3. v0.3.0: Device testing

- [ ] **TEST-0001 - Full device pass**
  - **Location:** whole app
  - **Current:** v0.2.6 to v0.2.9 have only unit and architecture tests. Riskiest on a device: the color picker capture, the glance stack widget, two panes on a tablet, Edit Home dragging, and the power tools.
  - **Change:** Test every screen, tile, widget, shortcut, and power tool on a phone, with and without Shizuku and root, and fix what the pass finds.

- [ ] **TEST-0002 - Screenshots**
  - **Location:** `docs/Screenshots`, README, website
  - **Current:** No screenshots yet; the website has no phone showcase and the README has no screenshot table.
  - **Change:** Capture real screens, convert them to lossless WebP, add the README table, and put the phone showcase back in the website hero.

- [ ] **TEST-0003 - Feature graphic**
  - **Location:** `docs/assets`
  - **Current:** The website's `og:image` is the app icon.
  - **Change:** Add a 1024 by 500 `Osyster-feature.png` and use it for `og:image`.

---

## Dropped on purpose

These were in the old app or the reference app and are left out by decision:

- **Process list:** Android blocks other apps' processes since Android 7; the old list showed only Osyster's own.
- **Screen time daily goal.**
- **Bento dashboard:** replaced by the hero card and grouped list with live facts.
- **Bluetooth batteries, per-app volume, Data SIM tile:** need hidden system interfaces.
- **Night light per app, screen-off widget, button remap:** need an AccessibilityService or constant app watching.
